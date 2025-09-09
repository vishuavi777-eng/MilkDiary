package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_entries",
       indexes = {
         @Index(name="idx_entry_outlet_date", columnList="outlet_id,date"),
         @Index(name="idx_entry_member_date", columnList="member_id,date")
       })
public class DailyMilkEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="outlet_id", nullable=false)
    private Outlet outlet;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="member_id", nullable=false)
    private Member member;

    @Column(nullable=false) private LocalDate date;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private SessionType session;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Species species;

    @Column(name = "qty_litre", nullable = false, precision = 10, scale = 3)
    private BigDecimal qtyLitre;

    @Column(name = "fat_pct", precision = 5, scale = 2)
    private BigDecimal fatPct;

    @Column(name = "snf_pct", precision = 5, scale = 2)
    private BigDecimal snfPct;

    @Column(name = "rate_applied", precision = 10, scale = 3)
    private BigDecimal rateApplied;

    @Column(precision=12, scale=2) private BigDecimal amount;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="rate_item_id")
    private RateItem matchedRateItem;

    private String notes;

    // getters/setters
    public Long getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public Member getMember() { return member; }
    public LocalDate getDate() { return date; }
    public SessionType getSession() { return session; }
    public Species getSpecies() { return species; }
    public BigDecimal getQtyLitre() { return qtyLitre; }
    public BigDecimal getFatPct() { return fatPct; }
    public BigDecimal getSnfPct() { return snfPct; }
    public BigDecimal getRateApplied() { return rateApplied; }
    public BigDecimal getAmount() { return amount; }
    public RateItem getMatchedRateItem() { return matchedRateItem; }
    public String getNotes() { return notes; }

    public void setId(long id) { this.id = id; }
    public void setOutlet(Outlet outlet) { this.outlet = outlet; }
    public void setMember(Member member) { this.member = member; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setSession(SessionType session) { this.session = session; }
    public void setSpecies(Species species) { this.species = species; }
    public void setQtyLitre(BigDecimal qtyLitre) { this.qtyLitre = qtyLitre; }
    public void setFatPct(BigDecimal fatPct) { this.fatPct = fatPct; }
    public void setSnfPct(BigDecimal snfPct) { this.snfPct = snfPct; }
    public void setRateApplied(BigDecimal rateApplied) { this.rateApplied = rateApplied; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setMatchedRateItem(RateItem matchedRateItem) { this.matchedRateItem = matchedRateItem; }
    public void setNotes(String notes) { this.notes = notes; }
}
