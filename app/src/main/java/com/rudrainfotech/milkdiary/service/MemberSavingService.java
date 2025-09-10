package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.Member;
import com.rudrainfotech.milkdiary.entity.Outlet;

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
        return new ArrayList<>();
    }

    /** Update the initial amount for a member. */
    public void updateInitialAmount(MemberSaving ms, double amount) {
        ms.setInitialAmount(amount);
    }

    /** Disburse savings for all members in the outlet. */
    public void disburseForOutlet(Outlet outlet) {
        // business logic would go here
    }
}