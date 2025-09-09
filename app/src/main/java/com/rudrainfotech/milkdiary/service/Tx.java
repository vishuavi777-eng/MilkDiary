package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.db.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

public final class Tx {
    private Tx() {}

    /** Read-write transaction that returns a value. */
    public static <T> T tx(Function<Session, T> work) {
        try (Session session = HibernateUtil.sf().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = work.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException ex) {
                if (tx != null && tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    /** Read-write transaction for void operations. */
    public static void txVoid(Consumer<Session> work) {
        tx(s -> { work.accept(s); return null; });
    }

    /** Optional: read-only convenience (no commit). */
    public static <T> T txReadOnly(Function<Session, T> work) {
        try (Session session = HibernateUtil.sf().openSession()) {
            // If you want, you can also set session.setDefaultReadOnly(true);
            return work.apply(session);
        }
    }
}

//package com.rudrainfotech.milkdiary.service;
//
//import com.rudrainfotech.milkdiary.db.HibernateUtil;
//import org.hibernate.Session;
//import org.hibernate.Transaction;
//
//import java.util.function.Function;
//
//public final class Tx {
//    private Tx() {}
//    public static <T> T tx(Function<Session, T> work) {
//        try (Session s = HibernateUtil.sf().openSession()) {
//            Transaction tx = s.beginTransaction();
//            try {
//                T res = work.apply(s);
//                tx.commit();
//                return res;
//            } catch (RuntimeException ex) {
//                tx.rollback();
//                throw ex;
//            }
//        }
//    }
//}
