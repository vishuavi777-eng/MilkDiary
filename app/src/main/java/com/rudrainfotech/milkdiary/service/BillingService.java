package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class BillingService {

    public List<MonthlyBill> listByOutletAndPeriod(Outlet outlet, int year, int month) {
        return Tx.tx((Session s) ->
                s.createQuery("""
            select b
            from MonthlyBill b
            join fetch b.member m
            where b.outlet = :o and b.year = :y and b.month = :m
            order by m.code
            """, MonthlyBill.class)
                        .setParameter("o", outlet)
                        .setParameter("y", year)
                        .setParameter("m", month)
                        .getResultList()
        );
    }

    public MonthlyBill findBill(Outlet outlet, Member member, int year, int month) {
        return Tx.tx((Session s) ->
                s.createQuery("""
                from MonthlyBill b where b.outlet=:o and b.member=:m and b.year=:y and b.month=:mo
                """, MonthlyBill.class)
                        .setParameter("o", outlet)
                        .setParameter("m", member)
                        .setParameter("y", year)
                        .setParameter("mo", month)
                        .setMaxResults(1).uniqueResult()
        );
    }

    public void lockBill(MonthlyBill bill, boolean lock) {
        Tx.tx((Session s) -> {
            MonthlyBill b = s.find(MonthlyBill.class, bill.getId());
            if (b != null) b.setLocked(lock);
            return null;
        });
    }

    public MonthlyBill upsertBill(Outlet outlet, Member member, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        // fetch all entries for member and month
        List<DailyMilkEntry> entries = Tx.tx((Session s) ->
                s.createQuery("""
                from DailyMilkEntry e
                where e.outlet = :o and e.member = :m and e.date >= :sd and e.date <= :ed
                """, DailyMilkEntry.class)
                        .setParameter("o", outlet)
                        .setParameter("m", member)
                        .setParameter("sd", start)
                        .setParameter("ed", end)
                        .getResultList()
        );

        // Recompute daily rate/amount in-place so billing reflects latest plan
        DailyEntryService des = new DailyEntryService();
        for (DailyMilkEntry e : entries) {
            des.computeRateAndAmount(outlet, e);  // ← recompute
            // No explicit save needed; Hibernate will flush managed entities on commit
        }

        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal cowQty = BigDecimal.ZERO;
        BigDecimal buffaloQty = BigDecimal.ZERO;
        BigDecimal fatQty = BigDecimal.ZERO;
        BigDecimal snfQty = BigDecimal.ZERO;
        BigDecimal gross = BigDecimal.ZERO;

        for (DailyMilkEntry e : entries) {
            BigDecimal q = nz(e.getQtyLitre());
            qty = qty.add(q);
            if (e.getSpecies() == Species.COW) cowQty = cowQty.add(q);
            if (e.getSpecies() == Species.BUFFALO) buffaloQty = buffaloQty.add(q);
            if (e.getFatPct()!=null) fatQty = fatQty.add(e.getFatPct().multiply(q));
            if (e.getSnfPct()!=null) snfQty = snfQty.add(e.getSnfPct().multiply(q));
            gross = gross.add(nz(e.getAmount()));
        }

        BigDecimal avgFat = qty.signum()==0 ? null : fatQty.divide(qty, 2, RoundingMode.HALF_UP);
        BigDecimal avgSnf = qty.signum()==0 ? null : snfQty.divide(qty, 2, RoundingMode.HALF_UP);
        gross = gross.setScale(2, RoundingMode.HALF_UP);

        // find or create bill
        MonthlyBill bill = findBill(outlet, member, year, month);
        if (bill == null) {
            bill = new MonthlyBill();
            bill.setOutlet(outlet);
            bill.setMember(member);
            bill.setYear(year);
            bill.setMonth(month);
        }
        if (bill.isLocked()) return bill; // respect lock

        bill.setTotalLitre(qty.setScale(3, RoundingMode.HALF_UP));
        bill.setAvgFat(avgFat);
        bill.setAvgSnf(avgSnf);
        bill.setGrossAmount(gross);

        BigDecimal saving = cowQty.multiply(nz(outlet.getCowSavingPerLitre()))
                .add(buffaloQty.multiply(nz(outlet.getBuffaloSavingPerLitre())));
        saving = saving.setScale(2, RoundingMode.HALF_UP);
        bill.setSavingsAmount(saving);

        // adjustments sum
        MonthlyBill finalBill1 = bill;
        BigDecimal adjTotal = (bill.getId() == null)
                ? BigDecimal.ZERO
                : Tx.tx(s -> {
            BigDecimal sum = s.createQuery(
                            "select coalesce(sum(a.amount), 0) " +
                                    "from BillAdjustment a where a.bill.id = :bid",
                            BigDecimal.class)
                    .setParameter("bid", finalBill1.getId())
                    .getSingleResult();
            return sum == null ? BigDecimal.ZERO : sum;
        });

        bill.setAdjustmentsTotal(adjTotal.setScale(2, RoundingMode.HALF_UP));

        BigDecimal preRound = gross.subtract(saving).add(bill.getAdjustmentsTotal());
        BigDecimal rounded = preRound.setScale(0, RoundingMode.HALF_UP); // nearest rupee
        BigDecimal roundOff = rounded.subtract(preRound).setScale(2, RoundingMode.HALF_UP);
        bill.setRoundOff(roundOff);
        bill.setNetAmount(preRound.add(roundOff));

        // persist
        MonthlyBill finalBill = bill;
        return Tx.tx((Session s) -> s.merge(finalBill));
    }

    public void generateForAllMembers(Outlet outlet, int year, int month, boolean skipLocked) {
        List<Member> members = new MemberService().listByOutlet(outlet);
        for (Member m : members) {
            MonthlyBill existing = findBill(outlet, m, year, month);
            if (skipLocked && existing != null && existing.isLocked()) continue;
            upsertBill(outlet, m, year, month);
        }
    }

    public void addOrUpdateAdjustment(MonthlyBill bill, AdjustmentType type, BigDecimal amount, String remark) {
        Tx.tx((Session s) -> {
            MonthlyBill b = s.find(MonthlyBill.class, bill.getId());
            if (b == null || b.isLocked()) return null;
            BillAdjustment aj = new BillAdjustment();
            aj.setBill(b);
            aj.setType(type);
            aj.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            aj.setRemark(remark);
            s.persist(aj);
            // re-tally
            BigDecimal adjTotal = b.getAdjustments().stream()
                    .map(a -> a.getAmount()==null?BigDecimal.ZERO:a.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            b.setAdjustmentsTotal(adjTotal);
            BigDecimal preRound = nz(b.getGrossAmount())
                    .subtract(nz(b.getSavingsAmount()))
                    .add(adjTotal);
            BigDecimal rounded = preRound.setScale(0, RoundingMode.HALF_UP);
            BigDecimal roundOff = rounded.subtract(preRound).setScale(2, RoundingMode.HALF_UP);
            b.setRoundOff(roundOff);
            b.setNetAmount(preRound.add(roundOff));
            return null;
        });
    }

    private static BigDecimal nz(BigDecimal x){ return x==null?BigDecimal.ZERO:x; }
}
