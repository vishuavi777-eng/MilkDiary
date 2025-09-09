package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "rate_plans")
public class RatePlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Column(nullable = false) private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<RateItem> items = new ArrayList<>();

    // getters/setters
    public Long getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<RateItem> getItems() { return items; }

    public void setId(Long id){ this.id=id; }
    public void setOutlet(Outlet outlet) { this.outlet = outlet; }
    public void setName(String name) { this.name = name; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setItems(List<RateItem> rateItems) { items = rateItems; }
}
