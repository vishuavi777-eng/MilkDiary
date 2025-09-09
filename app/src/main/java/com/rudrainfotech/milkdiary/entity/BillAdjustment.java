package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="bill_adjustments")
public class BillAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="bill_id", nullable=false)
    private MonthlyBill bill;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private AdjustmentType type;

    @Column(nullable=false, precision=14, scale=2)
    private BigDecimal amount;

    private String remark;

    // getters/setters
    public Long getId() { return id; }
    public MonthlyBill getBill() { return bill; }
    public AdjustmentType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getRemark() { return remark; }
    public void setBill(MonthlyBill bill) { this.bill = bill; }
    public void setType(AdjustmentType type) { this.type = type; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setRemark(String remark) { this.remark = remark; }
}
