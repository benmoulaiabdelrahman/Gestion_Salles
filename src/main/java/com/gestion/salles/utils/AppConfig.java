package com.gestion.salles.utils;

/******************************************************************************
 * AppConfig.java
 *
 * Application-wide configuration and path resolution. initialize() must be
 * called once at startup; it creates the profile picture directory under
 * ~/.GestionSalles/profile-pictures/ and loads email credentials from
 * email settings from environment variables first, then email.properties.
 * Startup fails fast when sender credentials are missing or still placeholders.
 ******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());

    private static final String APP_DATA_DIR_NAME    = ".GestionSalles";
    private static final String PROFILE_PICS_SUBDIR  = "profile-pictures";
    private static final String EMAIL_PROPERTIES_FILE = "email.properties";
    private static final String EMAIL_ENV_VAR = "GESTION_SALLES_EMAIL_SENDER";
    private static final String EMAIL_PASSWORD_ENV_VAR = "GESTION_SALLES_EMAIL_APP_PASSWORD";

    private static String senderEmail;
    private static String senderAppPassword;

    private AppConfig() {}

    public static void initialize() {
        ensureDirectory(getProfilePictureDirectory(), "profile picture");
        ensureDirectory(getUserHomeProfilePictureDirectory(), "profile picture fallback");
        loadEmailProperties();
    }

    private static void ensureDirectory(File dir, String label) {
        if (dir.exists()) return;
        LOGGER.info("Creating " + label + " directory: " + dir.getAbsolutePath());
        if (dir.mkdirs()) {
            LOGGER.info("Directory created successfully.");
        } else {
            LOGGER.warning("Failed to create " + label + " directory: " + dir.getAbsolutePath());
        }
    }

    private static void loadEmailProperties() {
        String envEmail = trimToNull(System.getenv(EMAIL_ENV_VAR));
        String envPassword = trimToNull(System.getenv(EMAIL_PASSWORD_ENV_VAR));

        if (envEmail != null && envPassword != null) {
            validateEmailConfig(envEmail, envPassword, "environment variables");
            senderEmail = envEmail;
            senderAppPassword = envPassword;
            LOGGER.info("Email configuration loaded from environment variables.");
            return;
        }

        Properties emailProperties = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(EMAIL_PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + EMAIL_PROPERTIES_FILE + " and no email environment variables are set.");
            }
            emailProperties.load(input);
            String email = trimToNull(emailProperties.getProperty("sender.email"));
            String password = trimToNull(emailProperties.getProperty("sender.app.password"));
            validateEmailConfig(email, password, EMAIL_PROPERTIES_FILE);
            senderEmail = email;
            senderAppPassword = password;
            LOGGER.info(EMAIL_PROPERTIES_FILE + " loaded successfully.");
        } catch (IOException ex) {
            throw new IllegalStateException("Error loading " + EMAIL_PROPERTIES_FILE, ex);
        }
    }

    public static String getSenderEmail() {
        return senderEmail;
    }

    public static String getSenderAppPassword() {
        return senderAppPassword;
    }

    public static File getProfilePictureDirectory() {
        File dir = getUserHomeProfilePictureDirectory();
        LOGGER.fine("Resolved profile picture directory: " + dir.getAbsolutePath());
        return dir;
    }

    private static File getUserHomeProfilePictureDirectory() {
        return new File(System.getProperty("user.home"), APP_DATA_DIR_NAME + File.separator + PROFILE_PICS_SUBDIR);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void validateEmailConfig(String email, String password, String source) {
        if (email == null || password == null) {
            throw new IllegalStateException("Email configuration is incomplete in " + source +
                ". Set " + EMAIL_ENV_VAR + " and " + EMAIL_PASSWORD_ENV_VAR + ".");
        }
        if (isPlaceholder(email) || isPlaceholder(password)) {
            throw new IllegalStateException("Email configuration in " + source +
                " still uses placeholder values. Provide real deployment credentials.");
        }
    }

    private static boolean isPlaceholder(String value) {
        String normalized = value.trim().toLowerCase();
        return normalized.contains("change_me")
            || normalized.contains("changeme")
            || normalized.contains("example")
            || normalized.contains("<set");
    }
}
