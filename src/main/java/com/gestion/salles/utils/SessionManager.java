package com.gestion.salles.utils;

/******************************************************************************
 * SessionManager.java
 *
 * Singleton managing user sessions backed by a MySQL table. Handles session
 * creation, validation, invalidation, and last-seen refresh. Token comparison
 * uses MessageDigest.isEqual for constant-time safety.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {

    private static final Logger LOGGER     = Logger.getLogger(SessionManager.class.getName());
    private static final String TABLE_NAME = "active_sessions";

    private static SessionManager instance;

    private final DatabaseConnection dbConnection;
    private final SecureRandom       secureRandom;

    private SessionManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.secureRandom = new SecureRandom();
        createTableIfNotExists();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    private void createTableIfNotExists() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                     "session_token VARCHAR(255) PRIMARY KEY," +
                     "user_email VARCHAR(255) NOT NULL," +
                     "created_at TIMESTAMP NOT NULL," +
                     "last_seen_at TIMESTAMP NOT NULL," +
                     "expires_at DATETIME NOT NULL," +
                     "INDEX idx_active_sessions_user_email (user_email)" +
                     ")";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt  = conn.createStatement()) {
            stmt.execute(ddl);
            LOGGER.info("Table '" + TABLE_NAME + "' ensured to exist.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating " + TABLE_NAME + " table", e);
        }
    }

    public String createSession(String userEmail) throws SessionException {
        invalidateSession(userEmail);
        String token = generateNewToken();
        String query = "INSERT INTO " + TABLE_NAME + " (user_email, session_token, created_at, last_seen_at, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn        = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(24));
            stmt.setString(1, userEmail);
            stmt.setString(2, token);
            stmt.setTimestamp(3, now);
            stmt.setTimestamp(4, now);
            stmt.setTimestamp(5, expiresAt);
            stmt.executeUpdate();
            LOGGER.info("Session created for user: " + userEmail);
            return token;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating session for user: " + userEmail, e);
            throw new SessionException("Failed to create session for user: " + userEmail, e);
        }
    }

    public boolean isSessionValid(String userEmail, String token) {
        String query = "SELECT session_token FROM " + TABLE_NAME + " WHERE user_email = ? AND session_token = ? AND expires_at > NOW()";
        try (Connection conn        = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userEmail);
            stmt.setString(2, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8),
                        rs.getString("session_token").getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validating session for user: " + userEmail, e);
        }
        return false;
    }

    public void invalidateSession(String userEmail) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE user_email = ?";
        try (Connection conn        = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userEmail);
            stmt.executeUpdate();
            LOGGER.info("Session invalidated for user: " + userEmail);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error invalidating session for user: " + userEmail, e);
        }
    }

    public void refreshLastSeen(String userEmail, String token) {
        if (!isSessionValid(userEmail, token)) {
            LOGGER.log(Level.WARNING, "Attempt to refresh invalid session for user: " + userEmail);
            return;
        }
        String query = "UPDATE " + TABLE_NAME + " SET last_seen_at = ? WHERE user_email = ? AND session_token = ?";
        try (Connection conn        = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, userEmail);
            stmt.setString(3, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing last_seen_at for user: " + userEmail, e);
        }
    }

    private String generateNewToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
