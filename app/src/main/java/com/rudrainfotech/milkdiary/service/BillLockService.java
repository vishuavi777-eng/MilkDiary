package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.CapLock;
import com.rudrainfotech.milkdiary.entity.Outlet;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

public class BillLockService {

    /** Cap window helper */
    public static record CapWindow(LocalDate start, LocalDate end) {}

    public static CapWindow window(int year, int month, int capNo) {
        YearMonth ym = YearMonth.of(year, month);
        int d1 = capNo==1?1:capNo==2?11:21;
        int d2 = capNo==1?10:capNo==2?20:ym.lengthOfMonth();
        return new CapWindow(ym.atDay(d1), ym.atDay(d2));
    }

    public static int capNoFor(LocalDate date) {
        int d = date.getDayOfMonth();
        return (d<=10)?1:(d<=20?2:3);
    }

    public boolean isCapLocked(Long outletId, int year, int month, int capNo) {
        return Tx.tx(s -> {
            String hql = "select count(c) from CapLock c where c.outlet.id=:oid and c.year=:y and c.month=:m and c.capNo=:c";
            Long n = s.createQuery(hql, Long.class)
                      .setParameter("oid", outletId).setParameter("y", year).setParameter("m", month).setParameter("c", capNo)
                      .uniqueResult();
            return n != null && n > 0;
        });
    }

    /** Check lock by date */
    public boolean isDateLocked(Long outletId, LocalDate date) {
        int cap = capNoFor(date);
        return isCapLocked(outletId, date.getYear(), date.getMonthValue(), cap);
    }

    public void lockCap(Outlet outlet, int year, int month, int capNo, String by) {
        Tx.tx(s -> {
            if (!isCapLocked(outlet.getId(), year, month, capNo)) {
                CapLock cl = new CapLock(outlet, year, month, capNo, by, LocalDateTime.now());
                s.persist(cl);
            }
            return null;
        });
        new AuditService().log("lock_cap", outlet, null, year, month, capNo, "by="+by);
    }

    public void unlockCap(Outlet outlet, int year, int month, int capNo, String by) {
        Tx.tx(s -> {
            String hql = "from CapLock c where c.outlet.id=:oid and c.year=:y and c.month=:m and c.capNo=:c";
            var cl = s.createQuery(hql, CapLock.class)
                      .setParameter("oid", outlet.getId()).setParameter("y", year).setParameter("m", month).setParameter("c", capNo)
                      .uniqueResult();
            if (cl != null) s.remove(cl);
            return null;
        });
        new AuditService().log("unlock_cap", outlet, null, year, month, capNo, "by="+by);
    }

    /** Throw if locked and override=false */
    public void guardEditable(Outlet outlet, LocalDate date, boolean override) {
        if (!override && isDateLocked(outlet.getId(), date)) {
            int cap = capNoFor(date);
            throw new IllegalStateException("Cap is locked for "+date.getYear()+"-"+date.getMonthValue()+" cap "+cap);
        }
    }
}
