package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "outlets")
public class Outlet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String name;

    @Column(length=120) private String owner;
    @Column(length=200) private String address;
    @Column(length=20)  private String phone;
    @Column(length=20)  private String gstin;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getGstin() { return gstin; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGstin(String gstin) { this.gstin = gstin; }
}
