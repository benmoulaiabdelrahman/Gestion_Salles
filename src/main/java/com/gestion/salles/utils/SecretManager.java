package com.gestion.salles.utils;

/******************************************************************************
 * SecretManager.java
 *
 * Singleton utility for loading the application verification secret. Resolves
 * the secret in priority order: system property, environment variable, then
 * a persistent file at ~/.gestion-salles/app.secret. If no value is found,
 * a cryptographically strong secret is generated and persisted automatically.
 ******************************************************************************/

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecretManager {

    private static final Logger LOGGER                  = Logger.getLogger(SecretManager.class.getName());
    private static final String SECRET_SYSTEM_PROPERTY  = "app.verification.secret";
    private static final String SECRET_ENV_VARIABLE     = "GESTION_SALLES_SECRET";
    private static final String SECRET_FILE             = "app.secret";
    private static final String APP_CONFIG_DIR          = ".gestion-salles";
    private static final int GENERATED_SECRET_BYTES     = 32;

    private static volatile String secret = null;

    private SecretManager() {}

    public static String getSecret() {
        if (secret == null) {
            synchronized (SecretManager.class) {
                if (secret == null) {
                    secret = loadSecret();
                }
            }
        }
        return secret;
    }

    private static String loadSecret() {
        String value = System.getProperty(SECRET_SYSTEM_PROPERTY);
        if (value != null && !value.isEmpty()) {
            LOGGER.info("Secret loaded from system property: " + SECRET_SYSTEM_PROPERTY);
            return value;
        }

        value = System.getenv(SECRET_ENV_VARIABLE);
        if (value != null && !value.isEmpty()) {
            LOGGER.info("Secret loaded from environment variable: " + SECRET_ENV_VARIABLE);
            return value;
        }

        Path persistentSecretPath = getPersistentSecretPath();
        value = readFirstNonBlankLine(persistentSecretPath);
        if (value != null) {
            ensureOwnerOnlyPermissions(persistentSecretPath);
            LOGGER.info("Secret loaded from file: " + persistentSecretPath);
            return value;
        }

        Path legacyPath = Paths.get(SECRET_FILE);
        value = readFirstNonBlankLine(legacyPath);
        if (value != null) {
            ensureOwnerOnlyPermissions(legacyPath);
            LOGGER.info("Secret loaded from legacy file: " + legacyPath.toAbsolutePath());
            persistSecret(persistentSecretPath, value);
            return value;
        }

        value = generateSecret();
        persistSecret(persistentSecretPath, value);
        LOGGER.info("Generated persistent verification secret at: " + persistentSecretPath);
        return value;
    }

    private static Path getPersistentSecretPath() {
        return Paths.get(System.getProperty("user.home"), APP_CONFIG_DIR, SECRET_FILE);
    }

    private static String readFirstNonBlankLine(Path path) {
        try {
            if (!Files.exists(path)) {
                return null;
            }
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading secret from file " + path, e);
            return null;
        }
    }

    private static void persistSecret(Path path, String value) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, value + System.lineSeparator(), StandardCharsets.UTF_8);
            ensureOwnerOnlyPermissions(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist verification secret to " + path, e);
        }
    }

    private static String generateSecret() {
        byte[] randomBytes = new byte[GENERATED_SECRET_BYTES];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static void ensureOwnerOnlyPermissions(Path path) {
        try {
            Set<PosixFilePermission> securePermissions = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, securePermissions);
        } catch (UnsupportedOperationException e) {
            LOGGER.fine("POSIX permissions not supported for " + path + ". Skipping chmod 600 enforcement.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to enforce secure permissions on " + path, e);
            return;
        }

        try {
            Set<PosixFilePermission> currentPermissions = Files.getPosixFilePermissions(path);
            if (!currentPermissions.equals(PosixFilePermissions.fromString("rw-------"))) {
                LOGGER.warning("Secret file permissions are broader than expected: " + path + " " + currentPermissions);
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.fine("POSIX permissions not supported while verifying " + path + ".");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to verify file permissions for " + path, e);
        }
    }
}
