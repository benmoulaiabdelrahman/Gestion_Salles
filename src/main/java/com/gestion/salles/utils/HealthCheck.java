package com.gestion.salles.utils;

import com.gestion.salles.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Lightweight operational health-check entry point.
 * Exit code 0 means all checks passed.
 */
public final class HealthCheck {

    private HealthCheck() {}

    public static void main(String[] args) {
        boolean ok = true;

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            if (!db.verifyDatabaseAccess()) {
                System.err.println("[FAIL] Database access check failed.");
                ok = false;
            } else {
                System.out.println("[OK] Database access check passed.");
            }

            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = DATABASE() AND table_name = 'verification_codes'");
                 ResultSet rs = stmt.executeQuery()) {
                boolean tableExists = rs.next() && rs.getInt(1) > 0;
                if (!tableExists) {
                    System.err.println("[FAIL] verification_codes table is missing.");
                    ok = false;
                } else {
                    System.out.println("[OK] verification_codes table exists.");
                }
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Database health-check error: " + e.getMessage());
            ok = false;
        }

        try {
            String secret = SecretManager.getSecret();
            if (secret == null || secret.isBlank()) {
                System.err.println("[FAIL] SecretManager returned an empty secret.");
                ok = false;
            } else {
                System.out.println("[OK] SecretManager secret is available.");
            }
        } catch (Exception e) {
            System.err.println("[FAIL] SecretManager check failed: " + e.getMessage());
            ok = false;
        }

        if (!ok) {
            System.exit(1);
        }
        System.out.println("[OK] Health-check completed successfully.");
    }
}
