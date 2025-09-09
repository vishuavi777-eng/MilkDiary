package com.rudrainfotech.milkdiary.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillDayRow {
    public LocalDate date;
    public BigDecimal qty;     // Litre (3)
    public BigDecimal rate;    // Rate/L (3), avg for day = amount/qty
    public BigDecimal amount;  // (2)
    public BigDecimal fat;     // weighted avg fat (2) - not shown in table to save space
    public BigDecimal snf;     // weighted avg snf (2)

    public BillDayRow(LocalDate date) { this.date = date; }
}
