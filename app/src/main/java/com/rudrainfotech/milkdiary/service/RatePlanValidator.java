package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RatePlanValidator {
    private RatePlanValidator(){}

    public static void ensureNoOverlappingSlabs(RatePlan plan) {
        for (Species sp : Species.values()) {
            List<String> errs = findOverlaps(plan, sp);
            if (!errs.isEmpty()) throw new IllegalArgumentException(String.join("\n", errs));
        }
    }

    public static List<String> findOverlaps(RatePlan plan, Species species) {
        List<RateItem> slabs = plan.getItems().stream()
            .filter(it -> it.getSpecies()==species && it.getType()==RateType.SLAB)
            .toList();
        List<String> errs = new ArrayList<>();
        for (int i=0;i<slabs.size();i++) {
            for (int j=i+1;j<slabs.size();j++) {
                RateItem a = slabs.get(i), b = slabs.get(j);
                if (rectOverlap(a.getMinFat(), a.getMaxFat(), a.getMinSnf(), a.getMaxSnf(),
                                b.getMinFat(), b.getMaxFat(), b.getMinSnf(), b.getMaxSnf())) {
                    errs.add(species + " slab overlap between rows sort#" + a.getSortOrder() + " and sort#" + b.getSortOrder());
                }
            }
        }
        return errs;
    }

    private static boolean rectOverlap(BigDecimal a1, BigDecimal a2, BigDecimal b1, BigDecimal b2,
                                       BigDecimal c1, BigDecimal c2, BigDecimal d1, BigDecimal d2) {
        if (a1==null||a2==null||b1==null||b2==null||c1==null||c2==null||d1==null||d2==null) return false;
        boolean fatOverlap = a1.compareTo(c2) <= 0 && a2.compareTo(c1) >= 0;
        boolean snfOverlap = b1.compareTo(d2) <= 0 && b2.compareTo(d1) >= 0;
        return fatOverlap && snfOverlap;
    }
}
