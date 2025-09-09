package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.db.HibernateUtil;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * SQLite-safe backups using VACUUM INTO.
 * Creates ./<backupDir>/milkdiary-YYYYMMDD-HHmmss.db
 * Keeps only the N most recent backups (BACKUPS_KEEP).
 * Supports restore-at-restart via pending file.
 */
public class BackupService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path DB_PATH = Path.of("data", "milkdiary.db");
    private static final Path RESTORE_PENDING = Path.of("data", "restore-pending.db");

    public File backupNow() throws Exception {
        AppSettingsService s = new AppSettingsService();
        String dirName = s.getString(AppSettingsService.BACKUP_DIR, "backups");
        int keep = s.getInt(AppSettingsService.BACKUPS_KEEP, 14);

        Path dir = Path.of(dirName);
        Files.createDirectories(dir);

        String fname = "milkdiary-" + TS.format(LocalDateTime.now()) + ".db";
        Path target = dir.resolve(fname).toAbsolutePath();

        // Use Hibernate session doWork to run native VACUUM INTO (must not be inside a tx)
        HibernateUtil.sf().openSession().doWork(conn -> {
            try (var st = conn.createStatement()) {
                // Ensure autoCommit true
                if (!conn.getAutoCommit()) conn.setAutoCommit(true);
                // SQLite requires single quotes; escape ' in path if any
                String sql = "VACUUM INTO '" + target.toString().replace("'", "''") + "'";
                st.execute(sql);
            }
        });

        // Retention
        pruneOldBackups(dir, keep);

        // Audit
        new AuditService().log("backup_now", null, null, null, null, null, target.getFileName().toString());

        return target.toFile();
    }

    public List<File> listBackups() throws IOException {
        AppSettingsService s = new AppSettingsService();
        Path dir = Path.of(s.getString(AppSettingsService.BACKUP_DIR, "backups"));
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> st = Files.list(dir)) {
            return st.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".db"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    public void queueRestore(File backupFile) throws IOException {
        if (backupFile == null || !backupFile.exists())
            throw new IOException("Backup file not found.");
        // Copy to data/restore-pending.db (atomic replace)
        Files.createDirectories(DB_PATH.getParent());
        Files.copy(backupFile.toPath(), RESTORE_PENDING, StandardCopyOption.REPLACE_EXISTING);

        // Audit
        new AuditService().log("restore_queued", null, null, null, null, null, backupFile.getName());
    }

    /** Called by Bootstrap before DB init */
    public static void applyPendingRestoreIfAny() throws IOException {
        if (Files.exists(RESTORE_PENDING)) {
            // close SF if somehow already touched (defensive)
            HibernateUtil.closeIfInitialized();

            if (Files.exists(DB_PATH)) {
                Path bak = DB_PATH.resolveSibling("milkdiary.db.bak");
                Files.move(DB_PATH, bak, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(RESTORE_PENDING, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void pruneOldBackups(Path dir, int keep) throws IOException {
        try (Stream<Path> st = Files.list(dir)) {
            var files = st.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".db"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
            for (int i = keep; i < files.size(); i++) {
                Files.deleteIfExists(files.get(i));
            }
        }
    }

    public Path backupNow(Path dataDir, Path backupDir, int keepCount) throws IOException {
        if (!Files.exists(backupDir)) Files.createDirectories(backupDir);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path zipPath = backupDir.resolve("milkdiary-" + ts + ".zip");
        zipDirectory(dataDir, zipPath);
        rotate(backupDir, keepCount);
        return zipPath;
    }

    private void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                for (Path p : (Iterable<Path>) walk::iterator) {
                    if (Files.isDirectory(p)) continue;
                    String entryName = sourceDir.relativize(p).toString().replace('\\','/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(p, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private void rotate(Path backupDir, int keepCount) throws IOException {
        try (Stream<Path> files = Files.list(backupDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".zip"))
                 .sorted(Comparator.<Path>comparingLong(p -> p.toFile().lastModified()).reversed())
                 .skip(keepCount)
                 .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
    }
}
