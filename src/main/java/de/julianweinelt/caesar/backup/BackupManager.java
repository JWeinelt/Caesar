package de.julianweinelt.caesar.backup;

import de.julianweinelt.caesar.storage.Configuration;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private static final Logger log = LoggerFactory.getLogger(BackupManager.class);
    private final File backupFolder;
    private ScheduledExecutorService backupService = Executors.newScheduledThreadPool(1);

    private int interval;
    private ChronoUnit intervalUnit;
    private boolean enableAutoBackup;
    private Configuration.BackupType backupType;
    private Configuration.AfterBackupAction afterBackupAction;
    private Configuration.BackupCompressType compressType;

    private LocalDateTime lastBackupTime;
    private boolean lastBackupSuccessful;

    private String userName;
    private String password;
    private String databaseName;

    private File backupProcessFolder;

    public BackupManager() {
        backupFolder = new File("backups");
        if (backupFolder.mkdir()) log.info("Created backup folder.");
    }

    public void configure(Configuration c) {
        this.interval = c.getInterval();
        this.intervalUnit = c.getIntervalType();
        this.backupType = c.getBackupType();
        this.afterBackupAction = c.getAfterBackupAction();
        this.enableAutoBackup = c.isDoAutoBackups();
        this.compressType = c.getCompressType();

        userName = c.getDatabaseUser();
        password = c.getDatabasePassword();
        databaseName = c.getDatabaseName();

        if (enableAutoBackup) startBackupTask();
    }

    public void startBackupTask() {
        if (backupService == null || backupService.isShutdown()) backupService = Executors.newScheduledThreadPool(1);
        backupService.scheduleAtFixedRate(this::runBackup, interval, interval, toTimeUnit(intervalUnit));
    }

    public void stopBackupService() {
        try {
            boolean stopped = backupService.awaitTermination(30, TimeUnit.SECONDS);
            if (!stopped) backupService.shutdownNow();
        } catch (InterruptedException e) {
            log.debug(e.getMessage());
            log.warn("Backup service could not be stopped.");
        }
    }

    public void runBackup() {
        switch (backupType) {
            case FULL -> {
                File bF = createBackupFolder();
                boolean dumpSuccess = MySQLDumper.dumpDatabase(userName, password, databaseName, new File(bF, "dump.sql"));
                if (dumpSuccess) log.debug("SQL Dump has been created.");
                Path[] toSave =  {
                    Path.of(URI.create("data")),
                    Path.of(URI.create("config.json")),
                        new File(bF, "dump.sql").toPath()
                };

                Date c = Date.from(Instant.now());
                String name = String.format("%s_%s_%s__%s_%s_%s", c.getDay(), c.getMonth(), c.getYear(), c.getHours(), c.getMinutes(), "1");

                try {
                    switch (compressType) {
                        case TAR -> tarGzFiles(new File(backupFolder, name + ".tar.gz").toPath(), toSave);
                        case ZIP -> zipFiles(new File(backupFolder, name + ".tar.gz").toPath(), toSave);
                    }
                    bF.delete();
                    lastBackupSuccessful = true;
                    lastBackupTime = LocalDateTime.now();
                } catch (IOException e) {
                    log.debug("Could not compress backup data.");
                    log.debug(e.getMessage());
                    lastBackupSuccessful = false;
                    lastBackupTime = LocalDateTime.now();
                }
            } case LOCAL_ONLY -> {
                File bF = createBackupFolder();

                Path[] toSave =  {
                        Path.of(URI.create("data")),
                        Path.of(URI.create("config.json"))
                };

                Date c = Date.from(Instant.now());
                String name = String.format("%s_%s_%s__%s_%s_%s", c.getDay(), c.getMonth(), c.getYear(), c.getHours(), c.getMinutes(), "1");

                try {
                    switch (compressType) {
                        case TAR -> tarGzFiles(new File(backupFolder, name + ".tar.gz").toPath(), toSave);
                        case ZIP -> zipFiles(new File(backupFolder, name + ".tar.gz").toPath(), toSave);
                    }
                    bF.delete();
                    lastBackupSuccessful = true;
                    lastBackupTime = LocalDateTime.now();
                } catch (IOException e) {
                    log.debug("Could not compress backup data.");
                    log.debug(e.getMessage());
                    lastBackupSuccessful = false;
                    lastBackupTime = LocalDateTime.now();
                }
            }
        }
    }

    private File createBackupFolder() {
        Date c = Date.from(Instant.now());
        String name = String.format("%s_%s_%s__%s_%s_%s", c.getDay(), c.getMonth(), c.getYear(), c.getHours(), c.getMinutes(), "1");
        backupProcessFolder = new File(backupFolder, name);
        return backupProcessFolder;
    }

    private void zipFiles(Path outputZip, Path... sources) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputZip))) {
            for (Path source : sources) {
                Files.walk(source)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry entry = new ZipEntry(source.relativize(path).toString());
                            try {
                                zos.putNextEntry(entry);
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                log.debug(e.getMessage());
                            }
                        });
            }
        }
    }

    private void tarGzFiles(Path output, Path... sources) throws IOException {
        try (OutputStream fOut = Files.newOutputStream(output);
             GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(fOut);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)) {

            for (Path source : sources) {
                Files.walk(source).forEach(path -> {
                    try {
                        Path relPath = source.getParent() != null ? source.getParent().relativize(path) : path;
                        TarArchiveEntry entry = new TarArchiveEntry(path.toFile(), relPath.toString());
                        tarOut.putArchiveEntry(entry);
                        if (!Files.isDirectory(path)) {
                            Files.copy(path, tarOut);
                        }
                        tarOut.closeArchiveEntry();
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                });
            }
            tarOut.finish();
        }
    }

    private TimeUnit toTimeUnit(ChronoUnit chronoUnit) {
        return switch (chronoUnit) {
            case NANOS    -> TimeUnit.NANOSECONDS;
            case MICROS   -> TimeUnit.MICROSECONDS;
            case MILLIS   -> TimeUnit.MILLISECONDS;
            case SECONDS  -> TimeUnit.SECONDS;
            case MINUTES  -> TimeUnit.MINUTES;
            case HOURS    -> TimeUnit.HOURS;
            case DAYS     -> TimeUnit.DAYS;
            default       -> throw new IllegalArgumentException("ChronoUnit " + chronoUnit + " cannot be converted to TimeUnit");
        };
    }
}
