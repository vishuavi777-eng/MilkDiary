package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

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

    @Column(name="cow_saving_pl", precision=10, scale=2)
    private BigDecimal cowSavingPerLitre;

    @Column(name="buffalo_saving_pl", precision=10, scale=2)
    private BigDecimal buffaloSavingPerLitre;


    public Long getId() { return id; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getGstin() { return gstin; }
    public BigDecimal getCowSavingPerLitre() { return cowSavingPerLitre; }
    public BigDecimal getBuffaloSavingPerLitre() { return buffaloSavingPerLitre; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public void setCowSavingPerLitre(BigDecimal cowSavingPerLitre) { this.cowSavingPerLitre = cowSavingPerLitre; }
    public void setBuffaloSavingPerLitre(BigDecimal buffaloSavingPerLitre) { this.buffaloSavingPerLitre = buffaloSavingPerLitre; }
}
