package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OutletSummaryService {

    public OutletSummary compute(Outlet outlet, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        OutletSummary S = new OutletSummary();
        S.totalLitre = bd0(); S.cowLitre = bd0(); S.buffaloLitre = bd0();
        S.avgFat = null; S.avgSnf = null;
        S.grossTotal = bd0(); S.adjustmentsTotal = bd0(); S.roundOffTotal = bd0(); S.netTotal = bd0();
        S.billsCount = 0; S.lockedBillsCount = 0; S.membersWithEntries = 0;
        S.amLitre = bd0(); S.pmLitre = bd0();

        // 1) Read all bills for period (money/totals + data for weighted avgs)
        List<MonthlyBill> bills = Tx.tx((Session s) ->
            s.createQuery("""
                from MonthlyBill b
                where b.outlet=:o and b.year=:y and b.month=:m
                """, MonthlyBill.class)
            .setParameter("o", outlet)
            .setParameter("y", year)
            .setParameter("m", month)
            .getResultList()
        );

        BigDecimal fatTimesQty = bd0();
        BigDecimal snfTimesQty = bd0();

        for (MonthlyBill b : bills) {
            BigDecimal qty = nz(b.getTotalLitre());
            BigDecimal fat = b.getAvgFat();
            BigDecimal snf = b.getAvgSnf();

            S.totalLitre = S.totalLitre.add(qty);
            S.grossTotal = S.grossTotal.add(nz(b.getGrossAmount()));
            S.adjustmentsTotal = S.adjustmentsTotal.add(nz(b.getAdjustmentsTotal()));
            S.roundOffTotal = S.roundOffTotal.add(nz(b.getRoundOff()));
            S.netTotal = S.netTotal.add(nz(b.getNetAmount()));
            if (Boolean.TRUE.equals(b.isLocked())) S.lockedBillsCount++;
            S.billsCount++;

            if (qty.signum() > 0) {
                if (fat != null) fatTimesQty = fatTimesQty.add(fat.multiply(qty));
                if (snf != null) snfTimesQty = snfTimesQty.add(snf.multiply(qty));
                S.membersWithEntries++;
            }
        }

        if (S.totalLitre.signum() > 0) {
            S.avgFat = fatTimesQty.divide(S.totalLitre, 2, RoundingMode.HALF_UP);
            S.avgSnf = snfTimesQty.divide(S.totalLitre, 2, RoundingMode.HALF_UP);
        }

        // 2) From daily_entries: species & session splits (independent of bills)
        Map<Species, BigDecimal> bySpecies = new EnumMap<>(Species.class);
        Map<SessionType, BigDecimal> bySession = new EnumMap<>(SessionType.class);
        bySpecies.put(Species.COW, bd0()); bySpecies.put(Species.BUFFALO, bd0());
        bySession.put(SessionType.AM, bd0()); bySession.put(SessionType.PM, bd0());

        Tx.tx((Session s) -> {
            List<Object[]> sp = s.createQuery("""
                select e.species, sum(e.qtyLitre)
                from DailyMilkEntry e
                where e.outlet=:o and e.date>=:sd and e.date<=:ed
                group by e.species
                """, Object[].class)
                .setParameter("o", outlet)
                .setParameter("sd", start)
                .setParameter("ed", end)
                .getResultList();
            for (Object[] row : sp) {
                Species spc = (Species) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                bySpecies.put(spc, nz(sum));
            }

            List<Object[]> sess = s.createQuery("""
                select e.session, sum(e.qtyLitre)
                from DailyMilkEntry e
                where e.outlet=:o and e.date>=:sd and e.date<=:ed
                group by e.session
                """, Object[].class)
                .setParameter("o", outlet)
                .setParameter("sd", start)
                .setParameter("ed", end)
                .getResultList();
            for (Object[] row : sess) {
                SessionType st = (SessionType) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                bySession.put(st, nz(sum));
            }
            return null;
        });

        S.cowLitre = bySpecies.getOrDefault(Species.COW, bd0());
        S.buffaloLitre = bySpecies.getOrDefault(Species.BUFFALO, bd0());
        S.amLitre = bySession.getOrDefault(SessionType.AM, bd0());
        S.pmLitre = bySession.getOrDefault(SessionType.PM, bd0());

        // Scale money to 2 decimals; litres to 3
        S.totalLitre = S.totalLitre.setScale(3, RoundingMode.HALF_UP);
        S.cowLitre = S.cowLitre.setScale(3, RoundingMode.HALF_UP);
        S.buffaloLitre = S.buffaloLitre.setScale(3, RoundingMode.HALF_UP);
        S.amLitre = S.amLitre.setScale(3, RoundingMode.HALF_UP);
        S.pmLitre = S.pmLitre.setScale(3, RoundingMode.HALF_UP);

        S.grossTotal = S.grossTotal.setScale(2, RoundingMode.HALF_UP);
        S.adjustmentsTotal = S.adjustmentsTotal.setScale(2, RoundingMode.HALF_UP);
        S.roundOffTotal = S.roundOffTotal.setScale(2, RoundingMode.HALF_UP);
        S.netTotal = S.netTotal.setScale(2, RoundingMode.HALF_UP);

        return S;
    }

    // CSV exporting (simple, no extra deps)
    public void exportCsv(java.io.File file, OutletSummary S, int year, int month, Outlet outlet) throws Exception {
        try (var w = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            w.println("Outlet Summary");
            w.printf("Outlet,%s%n", outlet.getName());
            w.printf("Period,%4d-%02d%n", year, month);
            w.println();
            w.println("Metric,Value");
            w.printf("Total Litre,%.3f%n", S.totalLitre);
            w.printf("Cow Litre,%.3f%n", S.cowLitre);
            w.printf("Buffalo Litre,%.3f%n", S.buffaloLitre);
            w.printf("AM Litre,%.3f%n", S.amLitre);
            w.printf("PM Litre,%.3f%n", S.pmLitre);
            w.printf("Avg Fat,%s%n", S.avgFat==null?"":S.avgFat.toPlainString());
            w.printf("Avg SNF,%s%n", S.avgSnf==null?"":S.avgSnf.toPlainString());
            w.printf("Gross Amount,%.2f%n", S.grossTotal);
            w.printf("Adjustments,%.2f%n", S.adjustmentsTotal);
            w.printf("Round Off,%.2f%n", S.roundOffTotal);
            w.printf("Net Amount,%.2f%n", S.netTotal);
            w.printf("Bills Count,%d%n", S.billsCount);
            w.printf("Locked Bills,%d%n", S.lockedBillsCount);
            w.printf("Members With Entries,%d%n", S.membersWithEntries);
        }
    }

    private static BigDecimal bd0(){ return BigDecimal.ZERO; }
    private static BigDecimal nz(BigDecimal x){ return x==null?BigDecimal.ZERO:x; }
}
