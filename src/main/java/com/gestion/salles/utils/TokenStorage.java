package com.gestion.salles.utils;

/******************************************************************************
 * TokenStorage.java
 *
 * Utility class for persisting the "Remember Me" session token to a local
 * file under the user's home directory. Handles save, load, and delete
 * operations with appropriate logging.
 ******************************************************************************/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenStorage {

    private static final Logger LOGGER          = Logger.getLogger(TokenStorage.class.getName());
    private static final String APP_DIR_NAME    = ".GestionSalles";
    private static final String TOKEN_FILE_NAME = "session.token";

    private TokenStorage() {}

    private static Path getStoragePath() throws IOException {
        Path appDir = Paths.get(System.getProperty("user.home"), APP_DIR_NAME);
        if (Files.notExists(appDir)) Files.createDirectory(appDir);
        return appDir.resolve(TOKEN_FILE_NAME);
    }

    public static void saveToken(String token) {
        try {
            Path path = getStoragePath();
            Files.writeString(path, token);
            ensureOwnerOnlyPermissions(path);
            LOGGER.info("Remember me token saved successfully.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save remember me token.", e);
        }
    }

    public static String loadToken() {
        try {
            Path tokenFile = getStoragePath();
            if (Files.exists(tokenFile)) {
                ensureOwnerOnlyPermissions(tokenFile);
                String token = Files.readString(tokenFile).trim();
                LOGGER.info("Remember me token loaded successfully.");
                return token.isEmpty() ? null : token;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load remember me token.", e);
        }
        return null;
    }

    public static void deleteToken() {
        try {
            if (Files.deleteIfExists(getStoragePath())) {
                LOGGER.info("Remember me token deleted successfully.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete remember me token.", e);
        }
    }

    private static void ensureOwnerOnlyPermissions(Path path) {
        try {
            Set<PosixFilePermission> securePermissions = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, securePermissions);
            Set<PosixFilePermission> currentPermissions = Files.getPosixFilePermissions(path);
            if (!currentPermissions.equals(securePermissions)) {
                LOGGER.warning("Remember-me token file permissions are broader than expected: " + path + " " + currentPermissions);
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.fine("POSIX permissions not supported for " + path + ". Skipping chmod 600 enforcement.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to enforce secure permissions on token file: " + path, e);
        }
    }
}
