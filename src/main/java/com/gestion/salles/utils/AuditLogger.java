package com.gestion.salles.utils;

/******************************************************************************
 * AuditLogger.java
 *
 * Thread-safe singleton that persists security-relevant events to the
 * 'audit_log' table with HMAC-SHA256 checksums for tamper detection.
 * Writes are dispatched on a dedicated daemon thread. Entries that fail to
 * persist are queued for retry (capacity: MAX_RETRY_QUEUE_CAPACITY); entries
 * dropped when the queue is full are logged at SEVERE.
 *
 * Secret key resolution order (via SecretManager):
 *   1. System property  -Dapp.verification.secret=<key>
 *   2. Environment variable  GESTION_SALLES_SECRET=<key>
 *   3. File  ~/.gestion-salles/app.secret (first line)
 * If none is available, SecretManager auto-generates and persists one.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditLogger {

    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());

    private static final String TABLE_NAME             = "audit_log";
    private static final int    MAX_RETRY_QUEUE_CAPACITY = 100;

    private static AuditLogger instance;

    private final DatabaseConnection         dbConnection;
    private final ExecutorService            executor;
    private final byte[]                     secretKey;
    private final LinkedBlockingQueue<AuditLogEntry> retryQueue;

    public enum AuditEvent {
        PASSWORD_CHANGE_SUCCESS,
        PASSWORD_CHANGE_FAILURE,
        PASSWORD_CHANGE_ATTEMPT_BLOCKED,
        RECOVERY_CODE_REQUESTED,
        RECOVERY_CODE_REQUEST_BLOCKED,
        RECOVERY_CODE_VALIDATED,
        RECOVERY_CODE_VALIDATION_FAILED,
        RECOVERY_CODE_EXPIRED,
        PASSWORD_RECOVERY_SUCCESS,
        PASSWORD_RECOVERY_FAILURE,
        LOGIN_SUCCESS,
        LOGIN_FAILURE
    }

    private AuditLogger() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.executor     = Executors.newSingleThreadExecutor(new AuditThreadFactory());
        this.secretKey    = SecretManager.getSecret().getBytes(StandardCharsets.UTF_8);
        this.retryQueue   = new LinkedBlockingQueue<>(MAX_RETRY_QUEUE_CAPACITY);
        createTableIfNotExists();
    }

    public static synchronized AuditLogger getInstance() {
        if (instance == null) instance = new AuditLogger();
        return instance;
    }

    private void createTableIfNotExists() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                     "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                     "user_email VARCHAR(255) NOT NULL," +
                     "event_type VARCHAR(64) NOT NULL," +
                     "event_detail VARCHAR(512)," +
                     "ip_address VARCHAR(64)," +
                     "success BOOLEAN NOT NULL," +
                     "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                     "checksum VARCHAR(64) NOT NULL" +
                     ")";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            LOGGER.info("Table '" + TABLE_NAME + "' ensured.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create audit_log table.", e);
        }
    }

    public void log(String userEmail, AuditEvent eventType, String detail, boolean success) {
        final String        email     = userEmail  != null ? userEmail          : "unknown";
        final String        type      = eventType  != null ? eventType.name()   : "UNKNOWN";
        final String        det       = detail     != null ? detail             : "";
        final LocalDateTime createdAt = LocalDateTime.now();

        executor.submit(() -> {
            drainAndRetryQueue();
            AuditLogEntry entry = new AuditLogEntry(email, type, det, success, createdAt);
            try {
                persistLogEntry(entry);
            } catch (SQLException | NoSuchAlgorithmException | InvalidKeyException e) {
                LOGGER.log(Level.WARNING, "Failed to persist audit entry for " + email + "/" + type + "; queueing for retry.", e);
                if (!retryQueue.offer(entry)) {
                    LOGGER.severe("Retry queue full — dropping audit entry for " + email + "/" + type);
                }
            }
        });
    }

    private void persistLogEntry(AuditLogEntry entry)
            throws SQLException, NoSuchAlgorithmException, InvalidKeyException {
        String checksum = hmacSha256(
            entry.userEmail + entry.eventType + entry.detail + entry.success + entry.createdAt,
            secretKey
        );
        String sql = "INSERT INTO " + TABLE_NAME +
                     " (user_email, event_type, event_detail, ip_address, success, created_at, checksum)" +
                     " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(   1, entry.userEmail);
            stmt.setString(   2, entry.eventType);
            stmt.setString(   3, entry.detail);
            stmt.setString(   4, "localhost");
            stmt.setBoolean(  5, entry.success);
            stmt.setTimestamp(6, Timestamp.valueOf(entry.createdAt));
            stmt.setString(   7, checksum);
            stmt.executeUpdate();
            LOGGER.fine("Audit entry persisted: " + entry.userEmail + "/" + entry.eventType);
        }
    }

    private void drainAndRetryQueue() {
        AuditLogEntry entry;
        while ((entry = retryQueue.poll()) != null) {
            try {
                persistLogEntry(entry);
                LOGGER.info("Re-persisted queued audit entry: " + entry.userEmail + "/" + entry.eventType);
            } catch (SQLException | NoSuchAlgorithmException | InvalidKeyException e) {
                LOGGER.log(Level.WARNING, "Retry failed for " + entry.userEmail + "/" + entry.eventType + "; re-queueing.", e);
                if (!retryQueue.offer(entry)) {
                    LOGGER.severe("Retry queue full — dropping audit entry for " + entry.userEmail + "/" + entry.eventType);
                }
                break; // DB is likely still unavailable; stop draining this cycle
            }
        }
    }

    public List<Long> verifyIntegrity() {
        List<Long> tampered = new ArrayList<>();
        String sql = "SELECT id, user_email, event_type, event_detail, success, created_at, checksum FROM " + TABLE_NAME;
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                long   id             = rs.getLong("id");
                String userEmail      = rs.getString("user_email");
                String eventType      = rs.getString("event_type");
                String eventDetail    = rs.getString("event_detail");
                boolean success       = rs.getBoolean("success");
                Timestamp createdAt   = rs.getTimestamp("created_at");
                String storedChecksum = rs.getString("checksum");

                String data = userEmail + eventType
                            + (eventDetail != null ? eventDetail : "")
                            + success + createdAt.toLocalDateTime();
                String computed = hmacSha256(data, secretKey);

                if (!computed.equals(storedChecksum)) {
                    tampered.add(id);
                    LOGGER.warning("Tampered audit entry detected — id=" + id);
                }
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during audit log integrity check.", e);
        }
        return tampered;
    }

    private String hmacSha256(String data, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    private static class AuditThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "AuditLogger-Thread");
            t.setDaemon(true);
            return t;
        }
    }

    private static class AuditLogEntry {
        final String        userEmail;
        final String        eventType;
        final String        detail;
        final boolean       success;
        final LocalDateTime createdAt;

        AuditLogEntry(String userEmail, String eventType, String detail, boolean success, LocalDateTime createdAt) {
            this.userEmail = userEmail;
            this.eventType = eventType;
            this.detail    = detail;
            this.success   = success;
            this.createdAt = createdAt;
        }
    }
}
