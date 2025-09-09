package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;

@Entity(name = "AuditLog")
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_ts", columnList = "ts")
})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Store as ISO-8601 string to avoid SQLite datetime quirks
    @Column(name = "ts", nullable = false)
    private String ts;

    @Column(name = "user")
    private String user;

    @Column(name = "action", nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id")
    private Outlet outlet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "cap_no")
    private Integer capNo;

    @Column(name = "details")
    private String details;

    // Getters & setters
    public Long getId() { return id; }
    public String getTs() { return ts; }
    public void setTs(String ts) { this.ts = ts; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Outlet getOutlet() { return outlet; }
    public void setOutlet(Outlet outlet) { this.outlet = outlet; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Integer getCapNo() { return capNo; }
    public void setCapNo(Integer capNo) { this.capNo = capNo; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}


//package com.rudrainfotech.milkdiary.entity;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity(name = "AuditLog")
//@Table(name="audit_log")
//public class AuditLog {
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable=false) private LocalDateTime ts;
//    @Column(nullable=false) private String action;
//    private String user;
//
//    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="outlet_id")
//    private Outlet outlet;
//
//    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="member_id")
//    private Member member;
//
//    private Integer year;   // nullable
//    private Integer month;
//    @Column(name="cap_no") private Integer capNo;
//
//    @Column(length=4000) private String details;
//
//    public AuditLog() {}
//    public AuditLog(LocalDateTime ts, String action, String user,
//                    Outlet outlet, Member member,
//                    Integer year, Integer month, Integer capNo, String details) {
//        this.ts = ts; this.action = action; this.user = user;
//        this.outlet = outlet; this.member = member;
//        this.year = year; this.month = month; this.capNo = capNo; this.details = details;
//    }
//}
