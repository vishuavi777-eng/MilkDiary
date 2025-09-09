package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "rate_items")
public class RateItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private RatePlan plan;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RateType type;

    @Column(name = "min_fat", precision = 5, scale = 2)
    private BigDecimal minFat;

    @Column(name = "max_fat", precision = 5, scale = 2)
    private BigDecimal maxFat;

    @Column(name = "min_snf", precision = 5, scale = 2)
    private BigDecimal minSnf;

    @Column(name = "max_snf", precision = 5, scale = 2)
    private BigDecimal maxSnf;

    @Column(name = "rate_per_litre", precision = 10, scale = 3)
    private BigDecimal ratePerLitre;

    @Column(precision = 10, scale = 3)
    private BigDecimal base;

    @Column(name = "per_fat", precision = 10, scale = 3)
    private BigDecimal perFat;

    @Column(name = "per_snf", precision = 10, scale = 3)
    private BigDecimal perSnf;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // getters/setters
    public Long getId() { return id; }
    public RatePlan getPlan() { return plan; }
    public Species getSpecies() { return species; }
    public RateType getType() { return type; }
    public BigDecimal getMinFat() { return minFat; }
    public BigDecimal getMaxFat() { return maxFat; }
    public BigDecimal getMinSnf() { return minSnf; }
    public BigDecimal getMaxSnf() { return maxSnf; }
    public BigDecimal getRatePerLitre() { return ratePerLitre; }
    public BigDecimal getBase() { return base; }
    public BigDecimal getPerFat() { return perFat; }
    public BigDecimal getPerSnf() { return perSnf; }
    public Integer getSortOrder() { return sortOrder; }

    public void setId(Long id){ this.id=id; }
    public void setPlan(RatePlan plan) { this.plan = plan; }
    public void setSpecies(Species species) { this.species = species; }
    public void setType(RateType type) { this.type = type; }
    public void setMinFat(BigDecimal minFat) { this.minFat = minFat; }
    public void setMaxFat(BigDecimal maxFat) { this.maxFat = maxFat; }
    public void setMinSnf(BigDecimal minSnf) { this.minSnf = minSnf; }
    public void setMaxSnf(BigDecimal maxSnf) { this.maxSnf = maxSnf; }
    public void setRatePerLitre(BigDecimal ratePerLitre) { this.ratePerLitre = ratePerLitre; }
    public void setBase(BigDecimal base) { this.base = base; }
    public void setPerFat(BigDecimal perFat) { this.perFat = perFat; }
    public void setPerSnf(BigDecimal perSnf) { this.perSnf = perSnf; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
