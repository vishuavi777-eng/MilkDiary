package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

public class RateResolver {
    private final RatePlanService planSvc = new RatePlanService();

    public static final class Match {
        public final BigDecimal rate; public final RateItem item; public final RatePlan plan;
        public Match(BigDecimal r, RateItem i, RatePlan p){ this.rate=r; this.item=i; this.plan=p; }
    }

    public Match resolve(Outlet outlet, LocalDate date, Species species, BigDecimal fat, BigDecimal snf) {
        RatePlan p = planSvc.findActivePlanWithItems(outlet, date);  // ← use initialized plan
        if (p == null) throw new IllegalStateException("No active plan on " + date);

        // slabs first
        var items = p.getItems().stream()
                .filter(it -> it.getSpecies()==species)
                .sorted(java.util.Comparator.comparing(it -> it.getSortOrder()==null?0:it.getSortOrder()))
                .toList();

        for (RateItem it : items) {
            if (it.getType()!=RateType.SLAB) continue;
            if (fat!=null && snf!=null &&
                it.getMinFat()!=null && it.getMaxFat()!=null &&
                it.getMinSnf()!=null && it.getMaxSnf()!=null &&
                fat.compareTo(it.getMinFat())>=0 && fat.compareTo(it.getMaxFat())<=0 &&
                snf.compareTo(it.getMinSnf())>=0 && snf.compareTo(it.getMaxSnf())<=0) {
                return new Match(it.getRatePerLitre(), it, p);
            }
        }
        // formulas
        for (RateItem it : items) {
            if (it.getType()!=RateType.FORMULA) continue;
            BigDecimal f = (fat==null?BigDecimal.ZERO:fat);
            BigDecimal s = (snf==null?BigDecimal.ZERO:snf);
            BigDecimal r = nz(it.getBase()).add(f.multiply(nz(it.getPerFat()))).add(s.multiply(nz(it.getPerSnf())));
            return new Match(r, it, p);
        }
        throw new IllegalStateException("No rate rule matched for " + species + " at Fat " + fat + ", SNF " + snf);
    }

    private static BigDecimal nz(BigDecimal x){ return x==null?BigDecimal.ZERO:x; }
}
