package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "members",
       indexes = {
           @Index(name = "idx_member_outlet", columnList = "outlet_id"),
           @Index(name = "uk_member_code_outlet", columnList = "code,outlet_id", unique = true)
       })
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false)
    private Species species = Species.COW; // sensible default

    public Species getSpecies() { return species; }
    public void setSpecies(Species species) { this.species = species; }

    @Column(length = 200) private String address;
    @Column(length = 20)  private String phone;
    @Column(nullable = false) private boolean active = true;

    // getters/setters
    public Long getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public void setOutlet(Outlet outlet) { this.outlet = outlet; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setActive(boolean active) { this.active = active; }
    public void setId(Long id) { this.id = id; }

}
