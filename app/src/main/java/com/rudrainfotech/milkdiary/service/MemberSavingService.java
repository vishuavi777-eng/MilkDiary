package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.Member;
import com.rudrainfotech.milkdiary.entity.Outlet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder service dealing with member savings. Real implementation
 * would load and persist balances from the database.
 */
public class MemberSavingService {

    /** Simple DTO representing saving data for a member. */
    public static class MemberSaving {
        private Member member;
        private double balance;
        private double initialAmount;

        public MemberSaving(Member member, double balance, double initialAmount) {
            this.member = member;
            this.balance = balance;
            this.initialAmount = initialAmount;
        }

        public Member getMember() { return member; }
        public void setMember(Member member) { this.member = member; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
        public double getInitialAmount() { return initialAmount; }
        public void setInitialAmount(double initialAmount) { this.initialAmount = initialAmount; }
    }

    /** List current balances for the given outlet. */
    public List<MemberSaving> listBalances(Outlet outlet) {
        return Tx.tx(s -> {
            List<Member> members = s.createQuery(
                            "from Member m where m.outlet = :out order by m.code asc",
                            Member.class)
                    .setParameter("out", outlet)
                    .getResultList();

            com.rudrainfotech.milkdiary.entity.SavingPeriod period = s.createQuery(
                            "from SavingPeriod p where p.endDate is null",
                            com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                    .setMaxResults(1)
                    .uniqueResult();

            List<MemberSaving> result = new ArrayList<>();
            for (Member m : members) {
                double bal = 0d;
                if (period != null) {
                    com.rudrainfotech.milkdiary.entity.MemberSaving db = s.createQuery(
                                    "from MemberSaving ms where ms.member = :m and ms.period = :p",
                                    com.rudrainfotech.milkdiary.entity.MemberSaving.class)
                            .setParameter("m", m)
                            .setParameter("p", period)
                            .setMaxResults(1)
                            .uniqueResult();
                    if (db != null && db.getAccumulatedAmount() != null) {
                        bal = db.getAccumulatedAmount().doubleValue();
                    }
                }
                result.add(new MemberSaving(m, bal, 0d));
            }
            return result;
        });
    }

    /** Update the initial amount for a member. */
    public void updateInitialAmount(MemberSaving ms, double amount) {
        Tx.txVoid(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod period = s.createQuery(
                            "from SavingPeriod p where p.endDate is null",
                            com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                    .setMaxResults(1)
                    .uniqueResult();

            com.rudrainfotech.milkdiary.entity.MemberSaving db = s.createQuery(
                            "from MemberSaving ms where ms.member = :m and ms.period = :p",
                            com.rudrainfotech.milkdiary.entity.MemberSaving.class)
                    .setParameter("m", ms.getMember())
                    .setParameter("p", period)
                    .setMaxResults(1)
                    .uniqueResult();

            if (db == null) {
                db = new com.rudrainfotech.milkdiary.entity.MemberSaving();
                db.setMember(ms.getMember());
                db.setPeriod(period);
                db.setAccumulatedAmount(BigDecimal.ZERO);
                db.setPaid(false);
            }

            db.setAccumulatedAmount(BigDecimal.valueOf(amount));
            s.merge(db);

            ms.setInitialAmount(amount);
            ms.setBalance(amount);
        });
    }

    /** Disburse savings for all members in the outlet. */
    public void disburseForOutlet(Outlet outlet) {
        Tx.txVoid(s -> {
            com.rudrainfotech.milkdiary.entity.SavingPeriod period = s.createQuery(
                            "from SavingPeriod p where p.endDate is null",
                            com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                    .setMaxResults(1)
                    .uniqueResult();
            if (period == null) return;

            List<com.rudrainfotech.milkdiary.entity.MemberSaving> list = s.createQuery(
                            "select ms from MemberSaving ms join ms.member m where m.outlet = :o and ms.period = :p",
                            com.rudrainfotech.milkdiary.entity.MemberSaving.class)
                    .setParameter("o", outlet)
                    .setParameter("p", period)
                    .getResultList();

            for (com.rudrainfotech.milkdiary.entity.MemberSaving m : list) {
                m.setPaid(true);
                s.merge(m);
            }
        });
    }
}