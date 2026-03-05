package com.gestion.salles.services;

/****************************************************************************
 * VerificationCodeManager.java
 *
 * Singleton manager for email verification codes used in the password recovery
 * flow. Generates 6-digit codes, stores only their HMAC-SHA256 hash (never
 * plaintext) both in memory and in the `verification_codes` database table,
 * and validates submitted codes using constant-time comparison.
 *
 * On startup, unexpired rows are rehydrated from the database so codes survive
 * an application restart. The in-memory map is a ConcurrentHashMap to handle
 * concurrent access safely.
 *
 * Secret configuration — checked in this order:
 *   1. System property:    -Dapp.verification.secret=<secret>
 *   2. Environment var:    GESTION_SALLES_SECRET=<secret>
 *   3. File (first line):  ~/.gestion-salles/app.secret
 * If no source is configured, SecretManager auto-generates and persists a
 * secret to ~/.gestion-salles/app.secret on first startup.
 ****************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.utils.SecretManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VerificationCodeManager {

    private static final Logger LOGGER = Logger.getLogger(VerificationCodeManager.class.getName());
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int RATE_LIMIT_MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final String ALGORITHM = "HmacSHA256";

    private static volatile VerificationCodeManager instance;

    private final ConcurrentHashMap<String, VerificationEntry> verificationCodes;
    private final Map<String, AttemptRecord> attempts;
    private final SecureRandom random;
    private final VerificationCodeStore codeStore;
    private final byte[] secretKey;

    private VerificationCodeManager() {
        this(new DatabaseVerificationCodeStore(DatabaseConnection.getInstance()), SecretManager.getSecret(), new SecureRandom());
    }

    VerificationCodeManager(VerificationCodeStore codeStore, String secret) {
        this(codeStore, secret, new SecureRandom());
    }

    VerificationCodeManager(VerificationCodeStore codeStore, String secret, SecureRandom random) {
        this.verificationCodes = new ConcurrentHashMap<>();
        this.attempts = new ConcurrentHashMap<>();
        this.random = random;
        this.codeStore = codeStore;
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        createTableIfNotExists();
        rehydrateFromStore();
    }

    public static VerificationCodeManager getInstance() {
        if (instance == null) {
            synchronized (VerificationCodeManager.class) {
                if (instance == null) {
                    instance = new VerificationCodeManager();
                }
            }
        }
        return instance;
    }

    public String generateCode(String email) {
        String emailKey = email.toLowerCase();
        int code = 100000 + random.nextInt(900000);
        String codeStr = String.format("%0" + CODE_LENGTH + "d", code);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);

        String codeHash = hmac(codeStr, hmacKey(emailKey));
        verificationCodes.put(emailKey, new VerificationEntry(codeHash, expiry));

        codeStore.upsert(emailKey, codeHash, expiry, LocalDateTime.now());
        LOGGER.info("Generated and persisted verification code for: " + email
            + ", codeHashPrefix=" + codeHash.substring(0, 12) + ", Expires at: " + expiry);

        return codeStr;
    }

    public ValidationResult validateCode(String email, String code) {
        String emailKey = email.toLowerCase();

        VerificationEntry entry = verificationCodes.get(emailKey);
        if (entry == null) {
            entry = loadFromStore(emailKey);
            if (entry != null) {
                verificationCodes.put(emailKey, entry);
            }
        }

        if (entry == null) {
            return new ValidationResult(false, "Aucun code de vérification trouvé pour cet email");
        }

        LOGGER.info("Validating code for " + emailKey + ". Current time: " + LocalDateTime.now() + ", Expiry time: " + entry.expiryTime);
        if (LocalDateTime.now().isAfter(entry.expiryTime)) {
            invalidateCode(emailKey);
            return new ValidationResult(false, "Le code de vérification a expiré");
        }

        String submittedHash = hmac(code, hmacKey(emailKey));
        if (!MessageDigest.isEqual(
            submittedHash.getBytes(StandardCharsets.UTF_8),
            entry.codeHash.getBytes(StandardCharsets.UTF_8))) {
            return new ValidationResult(false, "Code de vérification incorrect");
        }

        invalidateCode(emailKey);
        LOGGER.info("Code validated successfully for: " + email);
        return new ValidationResult(true, "Code vérifié avec succès");
    }

    public void invalidateCode(String email) {
        String emailKey = email.toLowerCase();
        verificationCodes.remove(emailKey);
        codeStore.delete(emailKey);
        LOGGER.info("Invalidated verification code for: " + email);
    }

    public boolean hasValidCode(String email) {
        String emailKey = email.toLowerCase();
        VerificationEntry entry = verificationCodes.get(emailKey);
        if (entry == null) {
            entry = loadFromStore(emailKey);
            if (entry != null) {
                verificationCodes.put(emailKey, entry);
            }
        }
        return entry != null && LocalDateTime.now().isBefore(entry.expiryTime);
    }

    public long getRemainingMinutes(String email) {
        String emailKey = email.toLowerCase();
        VerificationEntry entry = verificationCodes.get(emailKey);
        if (entry == null) {
            entry = loadFromStore(emailKey);
            if (entry != null) {
                verificationCodes.put(emailKey, entry);
            }
        }
        if (entry == null || LocalDateTime.now().isAfter(entry.expiryTime)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), entry.expiryTime).toMinutes();
    }

    public int getRemainingSeconds(String email) {
        String emailKey = email.toLowerCase();
        VerificationEntry entry = verificationCodes.get(emailKey);
        if (entry == null) {
            entry = loadFromStore(emailKey);
            if (entry != null) {
                verificationCodes.put(emailKey, entry);
            }
        }

        if (entry == null) {
            LOGGER.info("Remaining seconds for " + emailKey + ": No verification entry found. Returning 0.");
            return 0;
        }

        long seconds = java.time.Duration.between(LocalDateTime.now(), entry.expiryTime).getSeconds();
        LOGGER.info("Remaining seconds for " + emailKey + ": Current time: " + LocalDateTime.now() + ", Expiry time: " + entry.expiryTime + ", Calculated seconds: " + seconds);
        return (int) Math.max(0, seconds);
    }

    public boolean isRateLimited(String email) {
        String emailKey = email.toLowerCase();
        AttemptRecord record = attempts.get(emailKey);
        if (record == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (record.windowStart.plusMinutes(RATE_LIMIT_WINDOW_MINUTES).isBefore(now)) {
            attempts.remove(emailKey, record);
            return false;
        }
        return record.count >= RATE_LIMIT_MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String email) {
        String emailKey = email.toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        attempts.compute(emailKey, (key, record) -> {
            if (record == null || record.windowStart.plusMinutes(RATE_LIMIT_WINDOW_MINUTES).isBefore(now)) {
                return new AttemptRecord(1, now.truncatedTo(ChronoUnit.SECONDS));
            }
            record.count++;
            return record;
        });
    }

    public void clearAttempts(String email) {
        attempts.remove(email.toLowerCase());
    }

    private void createTableIfNotExists() {
        codeStore.ensureTable();
    }

    private void rehydrateFromStore() {
        Map<String, PersistedCode> persisted = codeStore.loadUnexpiredCodes();
        for (Map.Entry<String, PersistedCode> row : persisted.entrySet()) {
            verificationCodes.put(row.getKey().toLowerCase(),
                new VerificationEntry(row.getValue().codeHash(), row.getValue().expiryTime()));
        }
        LOGGER.info("Rehydrated " + verificationCodes.size() + " verification codes from persistence store.");
    }

    private VerificationEntry loadFromStore(String email) {
        PersistedCode row = codeStore.loadUnexpiredCode(email);
        if (row == null) {
            return null;
        }
        return new VerificationEntry(row.codeHash(), row.expiryTime());
    }

    private byte[] hmacKey(String email) {
        return (email + new String(secretKey, StandardCharsets.UTF_8))
            .getBytes(StandardCharsets.UTF_8);
    }

    private String hmac(String data, byte[] key) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC computation failed — secret key may be misconfigured", e);
        }
    }

    interface VerificationCodeStore {
        void ensureTable();
        void upsert(String email, String codeHash, LocalDateTime expiryTime, LocalDateTime createdAt);
        PersistedCode loadUnexpiredCode(String email);
        Map<String, PersistedCode> loadUnexpiredCodes();
        void delete(String email);
    }

    static final class PersistedCode {
        private final String codeHash;
        private final LocalDateTime expiryTime;

        PersistedCode(String codeHash, LocalDateTime expiryTime) {
            this.codeHash = codeHash;
            this.expiryTime = expiryTime;
        }

        String codeHash() {
            return codeHash;
        }

        LocalDateTime expiryTime() {
            return expiryTime;
        }
    }

    private static final class DatabaseVerificationCodeStore implements VerificationCodeStore {
        private static final String TABLE_NAME = "verification_codes";
        private final DatabaseConnection dbConnection;

        private DatabaseVerificationCodeStore(DatabaseConnection dbConnection) {
            this.dbConnection = dbConnection;
        }

        @Override
        public void ensureTable() {
            String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "email VARCHAR(255) PRIMARY KEY,"
                + "code_hash VARCHAR(64) NOT NULL,"
                + "expires_at TIMESTAMP NOT NULL,"
                + "created_at TIMESTAMP NOT NULL"
                + ")";
            try (Connection conn = dbConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
                LOGGER.info("Table '" + TABLE_NAME + "' ensured to exist.");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error creating table " + TABLE_NAME, e);
            }
        }

        @Override
        public void upsert(String email, String codeHash, LocalDateTime expiryTime, LocalDateTime createdAt) {
            String sql = "INSERT INTO " + TABLE_NAME + " (email, code_hash, expires_at, created_at) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE code_hash = VALUES(code_hash), expires_at = VALUES(expires_at), created_at = VALUES(created_at)";
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.setString(2, codeHash);
                stmt.setTimestamp(3, Timestamp.valueOf(expiryTime));
                stmt.setTimestamp(4, Timestamp.valueOf(createdAt));
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error persisting verification code for: " + email, e);
            }
        }

        @Override
        public PersistedCode loadUnexpiredCode(String email) {
            String sql = "SELECT code_hash, expires_at FROM " + TABLE_NAME + " WHERE email = ? AND expires_at > CURRENT_TIMESTAMP";
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PersistedCode(
                            rs.getString("code_hash"),
                            rs.getTimestamp("expires_at").toLocalDateTime());
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error loading verification code from database for: " + email, e);
            }
            return null;
        }

        @Override
        public Map<String, PersistedCode> loadUnexpiredCodes() {
            Map<String, PersistedCode> rows = new ConcurrentHashMap<>();
            String sql = "SELECT email, code_hash, expires_at FROM " + TABLE_NAME + " WHERE expires_at > CURRENT_TIMESTAMP";
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.put(rs.getString("email").toLowerCase(),
                        new PersistedCode(
                            rs.getString("code_hash"),
                            rs.getTimestamp("expires_at").toLocalDateTime()));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error rehydrating verification codes from database", e);
            }
            return rows;
        }

        @Override
        public void delete(String email) {
            String sql = "DELETE FROM " + TABLE_NAME + " WHERE email = ?";
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error deleting verification code from database for: " + email, e);
            }
        }
    }

    private static class VerificationEntry {
        final String codeHash;
        final LocalDateTime expiryTime;

        VerificationEntry(String codeHash, LocalDateTime expiryTime) {
            this.codeHash = codeHash;
            this.expiryTime = expiryTime;
        }
    }

    private static class AttemptRecord {
        int count;
        final LocalDateTime windowStart;

        AttemptRecord(int count, LocalDateTime windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    public static class ValidationResult {
        private final boolean success;
        private final String message;

        public ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
