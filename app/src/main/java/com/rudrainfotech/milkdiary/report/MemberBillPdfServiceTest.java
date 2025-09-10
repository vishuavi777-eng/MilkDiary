package com.rudrainfotech.milkdiary.report;

import com.rudrainfotech.milkdiary.entity.*;
import com.rudrainfotech.milkdiary.service.DailyEntryService;
//import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

//import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberBillPdfServiceTest {

//    @Test
    void capDataUsesRecomputedAmounts() throws Exception {
        Outlet outlet = new Outlet();

        DailyMilkEntry e1 = new DailyMilkEntry();
        e1.setOutlet(outlet);
        e1.setDate(LocalDate.of(2023, 1, 1));
        e1.setSession(SessionType.AM);
        e1.setQtyLitre(new BigDecimal("5"));

        DailyMilkEntry e2 = new DailyMilkEntry();
        e2.setOutlet(outlet);
        e2.setDate(LocalDate.of(2023, 1, 2));
        e2.setSession(SessionType.PM);
        e2.setQtyLitre(new BigDecimal("3"));

        List<DailyMilkEntry> entries = Arrays.asList(e1, e2);

        // Stub computeRateAndAmount to avoid DB access
        DailyEntryService stub = new DailyEntryService() {
            @Override
            public void computeRateAndAmount(Outlet o, DailyMilkEntry e) {
                BigDecimal rate = new BigDecimal("7.25");
                e.setRateApplied(rate);
                e.setAmount(e.getQtyLitre().multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }
        };

        // Simulate billing recomputation
        for (DailyMilkEntry e : entries) {
            stub.computeRateAndAmount(outlet, e);
        }
        BigDecimal expectedGross = entries.stream()
                .map(DailyMilkEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Reset amounts to mimic stale stored values
        entries.forEach(e -> e.setAmount(BigDecimal.ZERO));

        MemberBillPdfService svc = new MemberBillPdfService();
        Field f = MemberBillPdfService.class.getDeclaredField("dailySvc");
        f.setAccessible(true);
        f.set(svc, stub);

        Method m = MemberBillPdfService.class.getDeclaredMethod(
                "buildMemberCapData", List.class, YearMonth.class, int.class, int.class, boolean.class);
        m.setAccessible(true);
        Object capData = m.invoke(svc, entries, YearMonth.of(2023, 1), 1, 31, false);

        Field grossF = capData.getClass().getDeclaredField("gross");
        grossF.setAccessible(true);
        BigDecimal gross = (BigDecimal) grossF.get(capData);

//        assertEquals(expectedGross, gross, "gross amount should match recomputed billing");
    }
}