package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="monthly_bills",
       uniqueConstraints = @UniqueConstraint(name="uk_bill_member_month",
         columnNames={"outlet_id","member_id","month","year"}))
public class MonthlyBill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="outlet_id", nullable=false)
    private Outlet outlet;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="member_id", nullable=false)
    private Member member;

    @Column(nullable=false) private int month;
    @Column(nullable=false) private int year;

    @Column(name = "total_litre", precision=12, scale=3)
    private BigDecimal totalLitre;

    @Column(name = "avg_fat", precision=5, scale=2)
    private BigDecimal avgFat;

    @Column(name = "avg_snf", precision=5, scale=2)
    private BigDecimal avgSnf;

    @Column(name = "gross_amount", precision=14, scale=2)
    private BigDecimal grossAmount;

    @Column(name = "adjustments_total", precision=14, scale=2)
    private BigDecimal adjustmentsTotal;

    @Column(name = "savings_amount", precision=14, scale=2)
    private BigDecimal savingsAmount;

    @Column(name = "net_amount", precision=14, scale=2)
    private BigDecimal netAmount;

    @Column(name = "round_off", precision=14, scale=2)
    private BigDecimal roundOff;

    @Column(nullable=false) private boolean locked = false;

    @Column(name = "bill_no", length=40)
    private String billNo;

    @Column(name = "generated_on")
    private java.time.LocalDateTime generatedOn;

    @OneToMany(mappedBy="bill", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<BillAdjustment> adjustments = new ArrayList<>();

    // getters/setters
    public Long getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public Member getMember() { return member; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public BigDecimal getTotalLitre() { return totalLitre; }
    public BigDecimal getAvgFat() { return avgFat; }
    public BigDecimal getAvgSnf() { return avgSnf; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getAdjustmentsTotal() { return adjustmentsTotal; }
    public BigDecimal getSavingsAmount() { return savingsAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getRoundOff() { return roundOff; }
    public boolean isLocked() { return locked; }
    public String getBillNo() { return billNo; }
    public LocalDateTime getGeneratedOn() { return generatedOn; }
    public List<BillAdjustment> getAdjustments() { return adjustments; }

    public void setOutlet(Outlet outlet) { this.outlet = outlet; }
    public void setMember(Member member) { this.member = member; }
    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }
    public void setTotalLitre(BigDecimal totalLitre) { this.totalLitre = totalLitre; }
    public void setAvgFat(BigDecimal avgFat) { this.avgFat = avgFat; }
    public void setAvgSnf(BigDecimal avgSnf) { this.avgSnf = avgSnf; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public void setAdjustmentsTotal(BigDecimal adjustmentsTotal) { this.adjustmentsTotal = adjustmentsTotal; }
    public void setSavingsAmount(BigDecimal savingsAmount) { this.savingsAmount = savingsAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public void setRoundOff(BigDecimal roundOff) { this.roundOff = roundOff; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public void setGeneratedOn(LocalDateTime generatedOn) { this.generatedOn = generatedOn; }
}
