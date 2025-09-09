package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.Member;
import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.entity.Species;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;

public class MemberService {

    public List<Member> listByOutlet(Outlet outlet) {
        return Tx.tx((Session s) ->
            s.createQuery("from Member m where m.outlet = :outlet order by m.code asc", Member.class)
             .setParameter("outlet", outlet)
             .getResultList()
        );
    }

    public Member save(Member m) {
        return Tx.tx((Session s) -> {
            if (m.getId() == null) {
                s.persist(m);
                return m;
            } else {
                return s.merge(m);
            }
        });
    }

    // MemberService.java
    public Member save(Outlet outlet, Member m) {
        return Tx.tx(s -> {
            boolean isNew = (m.getId() == null);

            if (isNew) {
                if (m.getSpecies() == null) m.setSpecies(Species.COW);
                m.setOutlet(outlet);
                s.persist(m);
                return m;
            } else {
                Member db = s.find(Member.class, m.getId());
                if (db == null) throw new IllegalArgumentException("Member not found");

                // If species is changing, block when data exists
                if (db.getSpecies() != m.getSpecies()) {
                    Long eCnt = s.createQuery(
                                    "select count(e) from DailyMilkEntry e where e.member.id=:mid", Long.class)
                            .setParameter("mid", db.getId())
                            .uniqueResult();

                    Long bCnt = s.createQuery(
                                    "select count(b) from MonthlyBill b where b.member.id=:mid", Long.class)
                            .setParameter("mid", db.getId())
                            .uniqueResult();

                    if ((eCnt != null && eCnt > 0) || (bCnt != null && bCnt > 0)) {
                        throw new IllegalStateException(
                                "Cannot change species for a member that already has entries/bills. " +
                                        "Create a new member for the other species.");
                    }
                }

                // Update fields
                db.setCode(m.getCode());
                db.setName(m.getName());
                db.setActive(m.isActive());
                db.setSpecies(m.getSpecies());
                return db;
            }
        });
    }


    public void delete(Member m) {
        Tx.tx((Session s) -> {
            Member attached = s.find(Member.class, m.getId());
            if (attached != null) s.remove(attached);
            return null;
        });
    }
}
