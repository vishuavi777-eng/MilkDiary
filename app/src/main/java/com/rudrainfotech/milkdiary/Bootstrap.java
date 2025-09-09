package com.rudrainfotech.milkdiary;

import com.rudrainfotech.milkdiary.db.HibernateUtil;
import com.rudrainfotech.milkdiary.db.Props;
import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.service.AppSettingsService;
import com.rudrainfotech.milkdiary.service.BackupService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;

public class Bootstrap {
    private static volatile boolean done = false;

    public static synchronized void runOnce() {
        if (done) return;
        done = true;

        try {
            // Ensure ./data directory exists (for SQLite db file)
            Files.createDirectories(Path.of("data"));

            // NEW: apply pending restore (if any) before touching DB
            BackupService.applyPendingRestoreIfAny();

            // Configure Flyway
            Flyway flyway = Flyway.configure()
                    .dataSource(Props.jdbcUrl(), null, null)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true) // safe for dev/local; remove if you prefer stricter prod policy
                    .load();

            // Migrate; if validation fails (checksum mismatch), repair then migrate again
            try {
                flyway.migrate();
            } catch (FlywayValidateException vex) {
                System.err.println("Flyway validation failed; attempting repair...\n" + vex.getMessage());
                flyway.repair();
                flyway.migrate();
            }

            // Seed one demo outlet only if the table is empty
            try (Session s = HibernateUtil.sf().openSession()) {
                Transaction tx = s.beginTransaction();
                Long cnt = s.createQuery("select count(o) from Outlet o", Long.class).uniqueResult();
                if (cnt == null || cnt == 0L) {
                    Outlet o = new Outlet();
                    o.setName("Demo Outlet");
                    o.setOwner("Owner Name");
                    s.persist(o);
                    System.out.println("Inserted Demo Outlet.");
                }
                tx.commit();
            }

            new com.rudrainfotech.milkdiary.service.AppSettingsService().ensureDefaults();

            // Ensure active outlet setting points to a real outlet
            Outlet active = new AppSettingsService().ensureActiveOutletExists();
            System.out.println("Active outlet: " + active.getName() + " (id=" + active.getId() + ")");

            System.out.println("Bootstrap complete.");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Bootstrap failed", ex);
        }
    }
}
