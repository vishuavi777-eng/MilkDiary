package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "member_savings", uniqueConstraints =
@UniqueConstraint(name = "uk_member_period", columnNames = {"member_id", "period_id"}))
public class MemberSaving {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private SavingPeriod period;

    @Column(name = "accumulated_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal accumulatedAmount = BigDecimal.ZERO;

    @Column(name = "paid", nullable = false)
    private boolean paid = false;

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public SavingPeriod getPeriod() { return period; }
    public BigDecimal getAccumulatedAmount() { return accumulatedAmount; }
    public boolean isPaid() { return paid; }

    public void setId(Long id) { this.id = id; }
    public void setMember(Member member) { this.member = member; }
    public void setPeriod(SavingPeriod period) { this.period = period; }
    public void setAccumulatedAmount(BigDecimal accumulatedAmount) { this.accumulatedAmount = accumulatedAmount; }
    public void setPaid(boolean paid) { this.paid = paid; }
}