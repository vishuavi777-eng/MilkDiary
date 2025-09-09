package com.rudrainfotech.milkdiary.service;

import java.math.BigDecimal;

public class OutletSummary {
    public BigDecimal totalLitre;
    public BigDecimal cowLitre;
    public BigDecimal buffaloLitre;

    public BigDecimal avgFat;   // weighted
    public BigDecimal avgSnf;   // weighted

    public BigDecimal grossTotal;
    public BigDecimal adjustmentsTotal;
    public BigDecimal roundOffTotal;
    public BigDecimal netTotal;

    public int billsCount;
    public int lockedBillsCount;
    public int membersWithEntries;

    public BigDecimal amLitre;
    public BigDecimal pmLitre;
}
