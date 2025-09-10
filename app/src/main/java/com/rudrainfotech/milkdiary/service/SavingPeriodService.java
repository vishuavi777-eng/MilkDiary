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

    /** Return the current period for an outlet (stub data). */
    public SavingPeriod getCurrentPeriod(Outlet outlet) {
        SavingPeriod p = new SavingPeriod();
        p.setStartDate(LocalDate.now().withDayOfMonth(1));
        p.setEndDate(LocalDate.now());
        return p;
    }

    /** Update period dates for the given outlet. */
    public void updatePeriod(Outlet outlet, LocalDate start, LocalDate end) {
        // persistence logic would go here
    }

    /** Close the current period for the outlet. */
    public void closePeriod(Outlet outlet) {
        // closing logic would go here
    }

    /** Initialize the next period for the outlet. */
    public void initNextPeriod(Outlet outlet) {
        // initialization logic would go here
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