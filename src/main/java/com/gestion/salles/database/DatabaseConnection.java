package com.gestion.salles.database;

/******************************************************************************
 * DatabaseConnection.java
 *
 * Centralized database access layer for the Gestion des Salles application.
 * Manages a HikariCP connection pool whose credentials are resolved from
 * environment variables first, then from database.properties on the classpath.
 *
 * Connection URL parameters (SSL, timezone, charset) are assembled at startup
 * and logged so operators can confirm the active configuration. A JVM shutdown
 * hook ensures the pool is always closed cleanly even if closePool() is never
 * called explicitly.
 ******************************************************************************/

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger LOGGER =
        Logger.getLogger(DatabaseConnection.class.getName());

    private static volatile DatabaseConnection instance;

    private HikariDataSource dataSource;
    private final String dbUrl;

    private DatabaseConnection() {
        Properties props = loadProperties();

        String resolvedUrl      = require("DB_URL",      "db.url",      props);
        String resolvedUser     = require("DB_USER",     "db.user",     props);
        String resolvedPassword = require("DB_PASSWORD", "db.password", props);
        String resolvedDriver   = require("DB_DRIVER",   "db.driver",   props);
        boolean useSSL          = Boolean.parseBoolean(
            optional("DB_USESSL", "db.useSSL", props, "true"));
        validateConfiguration(resolvedUser, resolvedPassword);

        this.dbUrl = resolvedUrl;

        String connectionUrl = resolvedUrl
            + (useSSL ? "?useSSL=true" : "?useSSL=false")
            + "&serverTimezone=GMT%2B1"
            + "&allowPublicKeyRetrieval=false"
            + "&useUnicode=true"
            + "&characterEncoding=utf8";

        LOGGER.info(String.format(
            "Initializing HikariCP pool — url=%s ssl=%b user=%s",
            resolvedUrl, useSSL, resolvedUser));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionUrl);
        config.setUsername(resolvedUser);
        config.setPassword(resolvedPassword);
        config.setDriverClassName(resolvedDriver);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.addDataSourceProperty("cachePrepStmts",        "true");
        config.addDataSourceProperty("prepStmtCacheSize",     "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        resolvedPassword = null;

        LOGGER.info("HikariCP connection pool initialized successfully.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("JVM shutdown detected — closing HikariCP pool.");
            closePool();
        }, "hikari-shutdown-hook"));
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is not initialized or has been closed.");
        }
        return dataSource.getConnection();
    }

    public boolean verifyDatabaseAccess() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database access verification failed.", e);
            return false;
        }
    }

    public String getDatabaseURL() {
        return dbUrl;
    }

    public boolean isDatabaseInitialized() {
        String sql =
            "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = 'gestion_salles' " +
            "AND table_name = 'utilisateurs'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not verify database initialization.", e);
            return false;
        }
    }

    public void setCurrentUser(Connection conn, int userId) {
        try (PreparedStatement stmt = conn.prepareStatement("SET @current_user_id = ?")) {
            stmt.setInt(1, userId);
            stmt.execute();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "Failed to set @current_user_id session variable on provided connection.", e);
        }
    }

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP connection pool closed.");
        }
    }

    @Deprecated
    public static void closeStatement(PreparedStatement statement) {
        if (statement != null) {
            try { statement.close(); }
            catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing PreparedStatement.", e);
            }
        }
    }

    @Deprecated
    public static void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try { resultSet.close(); }
            catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing ResultSet.", e);
            }
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input =
                 getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                LOGGER.warning("database.properties not found on classpath; relying on environment variables.");
                return props;
            }
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database.properties.", e);
        }
        return props;
    }

    private String require(String envVar, String propKey, Properties props) {
        String value  = System.getenv(envVar);
        String source = "environment variable " + envVar;

        if (value == null || value.isBlank()) {
            value  = props.getProperty(propKey);
            source = "database.properties key " + propKey;
        }

        if (value == null || value.isBlank()) {
            throw new RuntimeException(String.format(
                "Required configuration '%s' not found. Set environment variable %s " +
                "or add '%s' to database.properties.", propKey, envVar, propKey));
        }

        LOGGER.info(String.format("Config '%s' resolved from %s%s.",
            propKey, source,
            propKey.contains("password") ? " [value masked]" : ": " + (propKey.contains("password") ? "" : value)));
        return value;
    }

    private String optional(String envVar, String propKey, Properties props, String defaultValue) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            LOGGER.info(String.format("Config '%s' resolved from environment variable %s.", propKey, envVar));
            return value;
        }
        value = props.getProperty(propKey);
        if (value != null && !value.isEmpty()) {
            LOGGER.info(String.format("Config '%s' resolved from database.properties.", propKey));
            return value;
        }
        LOGGER.info(String.format("Config '%s' not set — using default: %s.", propKey, defaultValue));
        return defaultValue;
    }

    private void validateConfiguration(String username, String password) {
        if ("root".equalsIgnoreCase(username) && password.isBlank()) {
            throw new IllegalStateException(
                "Refusing insecure database defaults (root user with blank password). " +
                "Set DB_USER and DB_PASSWORD (or secure values in database.properties).");
        }
    }
}
