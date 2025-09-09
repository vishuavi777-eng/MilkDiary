package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;
import java.time.LocalDateTime;

public class AuditService {
//    public void logOld(String action, Outlet outlet, Member member,
//                    Integer year, Integer month, Integer capNo, String details) {
//        Tx.txVoid(s -> s.persist(new AuditLog(
//            LocalDateTime.now(),
//            action,
//            new AppSettingsService().getString(AppSettingsService.PRINTED_BY,
//                System.getProperty("user.name","user")),
//            outlet, member, year, month, capNo, details
//        )));
//    }

    public void log(String action, Outlet outlet, Member member,
                    Integer year, Integer month, Integer capNo, String details) {
        Tx.txVoid(s -> {
            AuditLog a = new AuditLog();
            a.setTs(java.time.LocalDateTime.now().toString()); // ISO-8601
            String who = new AppSettingsService()
                    .getString(AppSettingsService.PRINTED_BY,
                            System.getProperty("user.name","user"));
            a.setUser(who);
            a.setAction(action);
            a.setOutlet(outlet);
            a.setMember(member);
            a.setYear(year);
            a.setMonth(month);
            a.setCapNo(capNo);
            a.setDetails(details);
            s.persist(a);
        });
    }
}
