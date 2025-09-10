package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.Member;
import com.rudrainfotech.milkdiary.entity.MemberSaving;
import com.rudrainfotech.milkdiary.entity.Outlet;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Placeholder service for managing saving periods. In a real
 * application this would persist data and perform business logic.
 */
public class SavingPeriodService {

    /** Simple DTO representing a saving period. */
    public static class SavingPeriod {
        private LocalDate startDate;
        private LocalDate endDate;

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    /** Return the current active period. */
    public SavingPeriod getCurrentPeriod(Outlet outlet) {
        return Tx.tx(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod p = s.createQuery(
                            "from SavingPeriod p where p.endDate is null",
                            com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                    .setMaxResults(1)
                    .uniqueResult();
            if (p == null) return null;
            SavingPeriod sp = new SavingPeriod();
            sp.setStartDate(p.getStartDate());
            sp.setEndDate(p.getEndDate());
            return sp;
        });
    }

    /** Update period dates for the given outlet. */
    public void updatePeriod(Outlet outlet, LocalDate start, LocalDate end) {
        Tx.txVoid(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod p = getActivePeriod(s);
            p.setStartDate(start);
            p.setEndDate(end);
        });
    }

    /** Close the current period for the outlet. */
    public void closePeriod(Outlet outlet) {
        Tx.txVoid(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod p = s.createQuery(
                            "from SavingPeriod p where p.endDate is null",
                            com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                    .setMaxResults(1)
                    .uniqueResult();
            if (p != null && p.getEndDate() == null) {
                p.setEndDate(LocalDate.now());
            }
        });
    }

    /** Initialize the next period for the outlet. */
    public void initNextPeriod(Outlet outlet) {
        Tx.txVoid(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod last = s.createQuery(
                            "from SavingPeriod p order by p.startDate desc",
                            com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                    .setMaxResults(1)
                    .uniqueResult();
            if (last != null && last.getEndDate() == null) return; // still active
            com.rudrainfotech.milkdiary.entity.SavingPeriod next = new com.rudrainfotech.milkdiary.entity.SavingPeriod();
            LocalDate start = (last != null && last.getEndDate() != null)
                    ? last.getEndDate().plusDays(1)
                    : LocalDate.now();
            next.setStartDate(start);
            s.persist(next);
        });
    }


    public void addSaving(Member member, BigDecimal delta) {
        if (delta == null || delta.signum() == 0) return;
        Tx.txVoid(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod period = getActivePeriod(s);
            MemberSaving ms = s.createQuery(
                            "from MemberSaving ms where ms.member = :m and ms.period = :p",
                            MemberSaving.class)
                    .setParameter("m", member)
                    .setParameter("p", period)
                    .setMaxResults(1)
                    .uniqueResult();
            if (ms == null) {
                ms = new MemberSaving();
                ms.setMember(member);
                ms.setPeriod(period);
                ms.setAccumulatedAmount(BigDecimal.ZERO);
                ms.setPaid(false);
            }
            ms.setAccumulatedAmount(ms.getAccumulatedAmount().add(delta));
            s.merge(ms);
        });
    }

    private com.rudrainfotech.milkdiary.entity.SavingPeriod getActivePeriod(Session s) {
        com.rudrainfotech.milkdiary.entity.SavingPeriod p = s.createQuery("from SavingPeriod p where p.endDate is null", com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                .setMaxResults(1)
                .uniqueResult();
        if (p == null) {
            p = new com.rudrainfotech.milkdiary.entity.SavingPeriod();
            p.setStartDate(LocalDate.now());
            s.persist(p);
        }
        return p;
    }

}