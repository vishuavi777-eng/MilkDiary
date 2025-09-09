package com.rudrainfotech.milkdiary.entity;

import jakarta.persistence.*;

@Entity @Table(name="app_settings")
public class AppSetting {
    @Id @Column(name="key", length=64)
    private String key;
    @Column(name="value", length=400)
    private String value;

    public AppSetting() {}
    public AppSetting(String key, String value){ this.key=key; this.value=value; }
    public String getKey(){ return key; }
    public void setKey(String key){ this.key = key; }
    public String getValue(){ return value; }
    public void setValue(String value){ this.value = value; }
}
