package com.rudrainfotech.milkdiary.db;

import com.rudrainfotech.milkdiary.entity.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import javax.sql.DataSource;
import java.util.Properties;

public final class HibernateUtil {
    private static final SessionFactory SF = build();

    private static SessionFactory build() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(Props.jdbcUrl());
        cfg.setMaximumPoolSize(Props.poolSize());
        DataSource ds = new HikariDataSource(cfg);

        Properties p = new Properties();
        p.put("hibernate.dialect", Props.dialect());
        p.put("hibernate.hbm2ddl.auto", Props.hbm2ddl());
        p.put("hibernate.show_sql", String.valueOf(Props.showSql()));
        p.put("hibernate.format_sql", String.valueOf(Props.formatSql()));
        p.put("hibernate.connection.datasource", ds);

        var registry = new StandardServiceRegistryBuilder().applySettings(p).build();

        return new MetadataSources(registry)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.Outlet.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.Member.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.RatePlan.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.RateItem.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.DailyMilkEntry.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.MonthlyBill.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.BillAdjustment.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.AppSetting.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.CapLock.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.AuditLog.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.MemberSaving.class)
                .addAnnotatedClass(com.rudrainfotech.milkdiary.entity.SavingPeriod.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    public static SessionFactory sf() { return SF; }

    /** Close SF if already initialized (for restore-at-restart safety) */
    public static void closeIfInitialized() {
        if (SF != null && !SF.isClosed()) {
            try { SF.close(); } catch (Exception ignore) {}
        }
    }
}
