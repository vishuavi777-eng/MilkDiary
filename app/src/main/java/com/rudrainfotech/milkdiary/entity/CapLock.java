package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "CapLock")
@Table(name = "cap_locks",
  uniqueConstraints = @UniqueConstraint(columnNames = {"outlet_id","year","month","cap_no"}))
public class CapLock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="outlet_id", nullable=false)
    private Outlet outlet;

    @Column(nullable=false) private int year;
    @Column(nullable=false) private int month; // 1..12
    @Column(name="cap_no", nullable=false) private int capNo; // 1..3

    @Column(name="locked_by") private String lockedBy;
    @Column(name="locked_at") private LocalDateTime lockedAt;

    public CapLock() {}
    public CapLock(Outlet outlet, int year, int month, int capNo, String by, LocalDateTime at) {
        this.outlet = outlet; this.year = year; this.month = month; this.capNo = capNo;
        this.lockedBy = by; this.lockedAt = at;
    }

    public Long getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getCapNo() { return capNo; }
    public String getLockedBy() { return lockedBy; }
    public LocalDateTime getLockedAt() { return lockedAt; }
}
