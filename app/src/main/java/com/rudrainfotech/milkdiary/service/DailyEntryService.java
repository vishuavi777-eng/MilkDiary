package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DailyEntryService {
    private final RateResolver resolver = new RateResolver();

    public List<DailyMilkEntry> list(Outlet outlet, LocalDate date, SessionType session) {
        return Tx.tx(s -> s.createQuery("""
        from DailyMilkEntry e
        join fetch e.member m
        where e.outlet=:o and e.date=:d and e.session=:ss
        order by m.code
    """, DailyMilkEntry.class)
                .setParameter("o", outlet)
                .setParameter("d", date)
                .setParameter("ss", session)
                .getResultList());
    }

    // Optional filtered version
    public List<DailyMilkEntry> list(Outlet outlet, LocalDate date, SessionType session, Species filter) {
        if (filter == null) return list(outlet, date, session);
        return Tx.tx(s -> s.createQuery("""
        from DailyMilkEntry e
        join fetch e.member m
        where e.outlet=:o and e.date=:d and e.session=:ss and m.species=:sp
        order by m.code
    """, DailyMilkEntry.class)
                .setParameter("o", outlet)
                .setParameter("d", date)
                .setParameter("ss", session)
                .setParameter("sp", filter)
                .getResultList());
    }


    public DailyMilkEntry findOne(Outlet outlet, Member member, LocalDate date, SessionType session, Species species) {
        return Tx.tx((Session s) ->
                s.createQuery("""
            select e
            from DailyMilkEntry e
            join fetch e.member m
            where e.outlet=:o and e.member=:m and e.date=:d and e.session=:sess and e.species=:sp
            """, DailyMilkEntry.class)
                        .setParameter("o", outlet)
                        .setParameter("m", member)
                        .setParameter("d", date)
                        .setParameter("sess", session)
                        .setParameter("sp", species)
                        .setMaxResults(1)
                        .uniqueResult()
        );
    }

    public void computeRateAndAmount(Outlet outlet, DailyMilkEntry e) {
        var fat = e.getFatPct();
        var snf = e.getSnfPct();
        var match = resolver.resolve(outlet, e.getDate(), e.getSpecies(), fat, snf);
        BigDecimal rate = match.rate == null ? BigDecimal.ZERO : match.rate;
        // recommended rounding policies
        rate = rate.setScale(3, RoundingMode.HALF_UP);
        e.setRateApplied(rate);
        BigDecimal amount = (e.getQtyLitre()==null ? BigDecimal.ZERO : e.getQtyLitre()).multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
        e.setAmount(amount);
        e.setMatchedRateItem(match.item);
    }

    /** Save one row with lock guard. */
    public DailyMilkEntry saveEntry(Outlet outlet, DailyMilkEntry e, boolean overrideLock) {
        new BillLockService().guardEditable(outlet, e.getDate(), overrideLock);
        e.setSpecies(e.getMember().getSpecies()); // ← enforce
        computeRateAndAmount(outlet, e);
        return Tx.tx((Session s) -> {
            if (e.getId() == null) { s.persist(e); return e; }
            else return s.merge(e);
        });
    }

    /** Delete one row with lock guard. */
    public void deleteEntry(Outlet outlet, DailyMilkEntry e, boolean overrideLock) {
        new BillLockService().guardEditable(outlet, e.getDate(), overrideLock);
        Tx.txVoid((Session s) -> {
            DailyMilkEntry m = s.find(DailyMilkEntry.class, e.getId());
            if (m != null) s.remove(m);
        });
        new AuditService().log("daily_entry_delete", outlet, e.getMember(),
                e.getDate().getYear(), e.getDate().getMonthValue(), BillLockService.capNoFor(e.getDate()),
                "id="+e.getId());
    }


    /**
     * Bulk UPSERT for a whole (date, session, species).
     * - Guard by cap lock once.
     * - qty null/zero -> delete if exists.
     * - recompute rate/amount before persist/merge.
     */
    public void upsertAll(Outlet outlet, LocalDate date, SessionType session,
                          List<DailyMilkEntry> rows, boolean overrideLock) {
        new BillLockService().guardEditable(outlet, date, overrideLock);

        Tx.txVoid((Session s) -> {
            // Load existing rows for the key (date,session,species)
            List<DailyMilkEntry> existing = s.createQuery("""
                from DailyMilkEntry e
                where e.outlet=:o and e.date=:d and e.session=:ss
            """, DailyMilkEntry.class)
                    .setParameter("o", outlet)
                    .setParameter("d", date)
                    .setParameter("ss", session)
//                    .setParameter("sp", species)
                    .getResultList();

            Map<Long, DailyMilkEntry> exByMember = existing.stream()
                    .filter(e -> e.getMember()!=null && e.getMember().getId()!=null)
                    .collect(Collectors.toMap(e -> e.getMember().getId(), e -> e));

            for (DailyMilkEntry r : rows) {
                // normalize keys (caller already sets these, but ensure)
                r.setOutlet(outlet);
                r.setDate(date);
                r.setSession(session);
                r.setSpecies(r.getMember().getSpecies());

                BigDecimal q = r.getQtyLitre();
                boolean empty = (q == null || q.signum() == 0);

                DailyMilkEntry prev = (r.getMember()!=null) ? exByMember.get(r.getMember().getId()) : null;

                if (empty) {
                    if (prev != null) s.remove(prev); // delete
                    continue;
                }

                computeRateAndAmount(outlet, r);
                if (prev == null) {
                    s.persist(r);
                } else {
                    // keep prev.id for merge consistency
                    r.setId(prev.getId());
                    s.merge(r);
                }
            }
        });

        new AuditService().log("daily_entries_save_all", outlet, null,
                date.getYear(), date.getMonthValue(), BillLockService.capNoFor(date),
                "count="+rows.size()+", session="+session);
    }
}
