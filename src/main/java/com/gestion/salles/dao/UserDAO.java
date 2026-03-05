package com.gestion.salles.dao;

/******************************************************************************
 * UserDAO.java
 *
 * Data access layer for the 'utilisateurs' table. The three-table SELECT/JOIN
 * is defined once in BASE_SELECT; all list queries delegate to fetchList().
 * Single-row lookups use fetchOne(). INSERT and UPDATE share bindUser() for
 * the common parameter block. The two emailExists() overloads collapse into
 * one method accepting a nullable excludeId, as do doesDepartmentHaveChef()
 * and doesBlocHaveChef() via the shared hasChef() helper. Authentication
 * zeroises the char[] password in a finally block and delegates hashing and
 * verification to PasswordUtils.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.User;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.utils.PasswordUtils;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.views.shared.RecentActivityPanel; // Import the shared RecentActivityPanel

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());
    private static final int DEFAULT_LIST_LIMIT = 500;
    private final ActivityLogDAO activityLogDAO;

    private final DatabaseConnection dbConnection;

    private static final String BASE_SELECT =
            "SELECT u.*, d.nom_departement, b.nom_bloc " +
            "FROM utilisateurs u " +
            "LEFT JOIN departements d ON u.id_departement = d.id_departement " +
            "LEFT JOIN blocs b ON u.id_bloc = b.id_bloc ";

    public UserDAO() {
        this(new ActivityLogDAO());
    }

    public UserDAO(ActivityLogDAO activityLogDAO) {
        this.dbConnection = DatabaseConnection.getInstance();
        this.activityLogDAO = activityLogDAO;
    }

    public User findById(int userId) {
        return fetchOne(BASE_SELECT + "WHERE u.id_utilisateur = ? AND u.actif = 1", userId);
    }

    public User findByEmail(String email) {
        String sql = BASE_SELECT + "WHERE u.email = ? AND u.actif = 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by email: " + email, e);
        }
        return null;
    }

    public User findUserByRememberToken(String token) {
        String sql = BASE_SELECT + "WHERE u.remember_token_hash = ? AND u.remember_token_expiry > NOW() AND u.actif = 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashToken(token));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by remember token", e);
        }
        return null;
    }

    public User authenticate(String email, char[] password) {
        String sql = BASE_SELECT + "WHERE u.email = ? AND u.actif = 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("mot_de_passe");
                    if (PasswordUtils.verifyPassword(new String(password), hashedPassword)) {
                        User user = mapRow(rs);
                        updateLastLogin(user.getIdUtilisateur());
                        return user;
                    }
                    LOGGER.warning("Invalid password for email: " + email);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Authentication error for email: " + email, e);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
        return null;
    }

    public List<User> getAllUsers() {
        return getUsersPage(0, DEFAULT_LIST_LIMIT);
    }

    public List<User> getUsersPage(int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, limit);
        return fetchList(BASE_SELECT + "ORDER BY u.nom ASC, u.prenom ASC LIMIT ? OFFSET ?", safeLimit, safeOffset);
    }

    public List<User> getAllActiveUsers() {
        return fetchList(BASE_SELECT + "WHERE u.actif = TRUE ORDER BY u.nom ASC, u.prenom ASC LIMIT ?", DEFAULT_LIST_LIMIT);
    }

    public List<User> getUsersByRole(User.Role role) {
        List<String> roleAliases = getRoleAliases(role);
        String placeholders = roleAliases.stream().map(v -> "?").collect(Collectors.joining(","));
        String sql = BASE_SELECT + "WHERE u.role IN (" + placeholders + ") AND u.actif = TRUE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < roleAliases.size(); i++) {
                stmt.setString(i + 1, roleAliases.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) users.add(mapRow(rs));
                return users;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching users by role: " + role.getRoleName(), e);
        }
        return new ArrayList<>();
    }

    public List<User> getUsersByDepartement(int idDepartement) {
        return fetchList(BASE_SELECT + "WHERE u.id_departement = ? AND u.actif = TRUE", idDepartement);
    }

    public List<User> getUsersByBloc(int idBloc) {
        return fetchList(BASE_SELECT + "WHERE u.id_bloc = ? AND u.actif = TRUE", idBloc);
    }

    public List<User> getUsersByDepartementAndRole(int idDepartement, User.Role role) {
        List<String> roleAliases = getRoleAliases(role);
        String placeholders = roleAliases.stream().map(v -> "?").collect(Collectors.joining(","));
        String sql = BASE_SELECT + "WHERE u.id_departement = ? AND u.role IN (" + placeholders + ") AND u.actif = TRUE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDepartement);
            for (int i = 0; i < roleAliases.size(); i++) {
                stmt.setString(i + 2, roleAliases.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) users.add(mapRow(rs));
                return users;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching users by department " + idDepartement + " and role " + role.getRoleName(), e);
        }
        return new ArrayList<>();
    }

    public List<User> getTeachersByDepartment(int idDepartement) {
        return getUsersByDepartementAndRole(idDepartement, User.Role.Enseignant);
    }

    public List<User> getTeachersByBloc(int idBloc) {
        return getUsersByBlocAndRoles(idBloc, Arrays.asList(User.Role.Enseignant));
    }

    public List<User> getTeachersByDepartmentAndBloc(int idDepartement, int idBloc) {
        String sql = BASE_SELECT + "WHERE u.id_departement = ? AND u.id_bloc = ? AND u.actif = TRUE";
        List<User> users = getUsersByDepartementAndRole(idDepartement, User.Role.Enseignant);
        return users.stream()
                .filter(user -> user.getIdBloc() != null && user.getIdBloc() == idBloc)
                .collect(Collectors.toList());
    }

    public List<User> getUsersByBlocAndRoles(int idBloc, List<User.Role> roles) {
        Set<String> roleAliases = new LinkedHashSet<>();
        for (User.Role role : roles) {
            roleAliases.addAll(getRoleAliases(role));
        }
        String placeholders = roleAliases.stream().map(r -> "?").collect(Collectors.joining(","));
        String sql = BASE_SELECT + "WHERE u.id_bloc = ? AND u.role IN (" + placeholders + ") AND u.actif = TRUE";
        List<User> users = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idBloc);
            int paramIndex = 2;
            for (String roleAlias : roleAliases) {
                stmt.setString(paramIndex++, roleAlias);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching users by bloc: " + idBloc + " and roles: " + roles, e);
        }
        return users;
    }

    public List<User> getAvailableTeachers(LocalDate date, LocalTime startTime, LocalTime endTime,
                                           Integer excludeReservationId) {
        List<String> teacherAliases = getRoleAliases(User.Role.Enseignant);
        String rolePlaceholders = teacherAliases.stream().map(r -> "?").collect(Collectors.joining(","));
        String sql = BASE_SELECT +
                "WHERE u.role IN (" + rolePlaceholders + ") AND u.actif = TRUE " +
                "AND u.id_utilisateur NOT IN (" +
                "  SELECT r.id_enseignant FROM reservations r " +
                "  WHERE r.date_reservation = ? " +
                "    AND ((r.heure_debut < ? AND r.heure_fin > ?) " +
                "      OR (r.heure_debut >= ? AND r.heure_debut < ?) " +
                "      OR (r.heure_fin > ? AND r.heure_fin <= ?)) " +
                "    AND r.statut IN ('CONFIRMEE', 'EN_ATTENTE') " +
                "    AND r.id_reservation != ?) " +
                "ORDER BY u.nom, u.prenom";

        List<User> users = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (String teacherAlias : teacherAliases) {
                stmt.setString(paramIndex++, teacherAlias);
            }
            stmt.setDate(paramIndex++, Date.valueOf(date));
            stmt.setTime(paramIndex++, Time.valueOf(endTime));
            stmt.setTime(paramIndex++, Time.valueOf(startTime));
            stmt.setTime(paramIndex++, Time.valueOf(startTime));
            stmt.setTime(paramIndex++, Time.valueOf(endTime));
            stmt.setTime(paramIndex++, Time.valueOf(startTime));
            stmt.setTime(paramIndex++, Time.valueOf(endTime));
            stmt.setInt(paramIndex, excludeReservationId != null ? excludeReservationId : -1);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting available teachers", e);
        }
        return users;
    }

    public boolean addUser(User user) throws SQLException {
        requireAuthenticated();
        String sql = "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, id_departement, id_bloc, telephone, photo_profil, actif) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUser(stmt, user, true);
            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) user.setIdUtilisateur(keys.getInt(1));
                }
                activityLogDAO.insert(ActivityLog.ActionType.CREATE, ActivityLog.EntityType.USER,
                        user.getIdUtilisateur(), user.getIdUtilisateur(),
                        "Nouvel utilisateur '" + user.getEmail() + "' (" + user.getRole().name() + ") ajouté(e).",
                        user.getIdBloc());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding user: " + user.getEmail(), e);
            throw e;
        }
        return false;
    }

    public boolean updateUser(User user) throws SQLException {
        requireAuthenticated();
        String sql = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?, role = ?, id_departement = ?, " +
                     "id_bloc = ?, telephone = ?, photo_profil = ?, actif = ? WHERE id_utilisateur = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindUser(stmt, user, false);
            stmt.setInt(10, user.getIdUtilisateur());
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.UPDATE, ActivityLog.EntityType.USER,
                        user.getIdUtilisateur(), SessionContext.get(),
                        "Utilisateur '" + user.getEmail() + "' mis(e) à jour.",
                        user.getIdBloc());
                RecentActivityPanel.invalidateCache();
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user: " + user.getEmail(), e);
            throw e;
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        requireAuthenticated();
        User userToDelete = findById(userId); // Use findById to get all user details
        if (userToDelete == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete non-existent user with ID: " + userId);
            return false;
        }

        String deleteActivityLogSql = "DELETE FROM activity_log WHERE id_user_acting = ?";
        String sql = "DELETE FROM utilisateurs WHERE id_utilisateur = ?";
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement deleteLogStmt = conn.prepareStatement(deleteActivityLogSql);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                deleteLogStmt.setInt(1, userId);
                deleteLogStmt.executeUpdate();

                stmt.setInt(1, userId);
                if (stmt.executeUpdate() > 0) {
                    conn.commit();
                    // Log after commit to avoid lock waits with a separate connection.
                    activityLogDAO.insert(ActivityLog.ActionType.DELETE, ActivityLog.EntityType.USER,
                            userId, SessionContext.get(),
                            "Utilisateur '" + userToDelete.getEmail() + "' supprimé(e).",
                            userToDelete.getIdBloc());
                    return true;
                }
                conn.rollback();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user with ID: " + userId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Error rolling back delete user transaction", rollbackEx);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) {
                    // Ignore cleanup errors.
                }
            }
        }
        return false;
    }

    public boolean updatePassword(String email, String newPassword) {
        try {
            String hashedPassword = PasswordUtils.hashPassword(newPassword);
            String sql = "UPDATE utilisateurs SET mot_de_passe = ? WHERE email = ? AND actif = 1";
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hashedPassword);
                stmt.setString(2, email);
                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating password for email: " + e, e);
        }
        return false;
    }

    public boolean emailExists(String email, Integer excludeUserId) {
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?" +
                     (excludeUserId != null ? " AND id_utilisateur != ?" : "");
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            if (excludeUserId != null) stmt.setInt(2, excludeUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking email existence: " + email, e);
        }
        return false;
    }

    public boolean emailExists(String email) {
        return emailExists(email, null);
    }

    public boolean doesDepartmentHaveChef(int idDepartement, Integer excludeUserId) {
        return hasChef("id_departement", idDepartement, excludeUserId);
    }

    public boolean doesBlocHaveChef(int idBloc, Integer excludeUserId) {
        return hasChef("id_bloc", idBloc, excludeUserId);
    }

    public boolean storeRememberToken(int userId, String token, Timestamp expiry) {
        String sql = "UPDATE utilisateurs SET remember_token_hash = ?, remember_token_expiry = ? WHERE id_utilisateur = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashToken(token));
            stmt.setTimestamp(2, expiry);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error storing remember token for user: " + userId, e);
        }
        return false;
    }

    public boolean clearRememberToken(int userId) {
        String sql = "UPDATE utilisateurs SET remember_token_hash = NULL, remember_token_expiry = NULL WHERE id_utilisateur = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error clearing remember token for user: " + userId, e);
        }
        return false;
    }

    public boolean clearMustChangePassword(int userId) {
        String sql = "UPDATE utilisateurs SET must_change_password = FALSE WHERE id_utilisateur = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error clearing must_change_password for user: " + userId, e);
        }
        return false;
    }

    private String hashToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    private List<User> fetchList(String sql, Object... params) {
        List<User> result = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i] instanceof User.Role ? ((User.Role) params[i]).name() : params[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing list query", e);
        }
        return result;
    }

    private User fetchOne(String sql, int param) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing single-row query", e);
        }
        return null;
    }

    private void bindUser(PreparedStatement stmt, User user, boolean includePassword) throws SQLException {
        stmt.setString(1, user.getNom());
        stmt.setString(2, user.getPrenom());
        stmt.setString(3, user.getEmail());
        if (includePassword) {
            stmt.setString(4, user.getMotDePasse());
            stmt.setString(5, user.getRole().getRoleName());
            if (user.getIdDepartement() != null) stmt.setInt(6, user.getIdDepartement()); else stmt.setNull(6, Types.INTEGER);
            if (user.getIdBloc() != null) stmt.setInt(7, user.getIdBloc()); else stmt.setNull(7, Types.INTEGER);
            stmt.setString(8, user.getTelephone());
            stmt.setString(9, user.getPhotoProfil());
            stmt.setBoolean(10, user.isActif());
        } else {
            stmt.setString(4, user.getRole().getRoleName());
            if (user.getIdDepartement() != null) stmt.setInt(5, user.getIdDepartement()); else stmt.setNull(5, Types.INTEGER);
            if (user.getIdBloc() != null) stmt.setInt(6, user.getIdBloc()); else stmt.setNull(6, Types.INTEGER);
            stmt.setString(7, user.getTelephone());
            stmt.setString(8, user.getPhotoProfil());
            stmt.setBoolean(9, user.isActif());
        }
    }

    private boolean hasChef(String column, int id, Integer excludeUserId) {
        List<String> chefAliases = getRoleAliases(User.Role.Chef_Departement);
        String placeholders = chefAliases.stream().map(v -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE " + column + " = ? AND role IN (" + placeholders + ")" +
                (excludeUserId != null ? " AND id_utilisateur != ?" : "");
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            stmt.setInt(paramIndex++, id);
            for (String alias : chefAliases) {
                stmt.setString(paramIndex++, alias);
            }
            if (excludeUserId != null) {
                stmt.setInt(paramIndex, excludeUserId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking for chef in " + column + ": " + id, e);
        }
        return false;
    }

    private void updateLastLogin(int userId) {
        String sql = "UPDATE utilisateurs SET derniere_connexion = CURRENT_TIMESTAMP WHERE id_utilisateur = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to update last login for user: " + userId, e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setIdUtilisateur(rs.getInt("id_utilisateur"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setMotDePasse(rs.getString("mot_de_passe"));

        String roleStr = rs.getString("role");
        User.Role parsedRole = parseRole(roleStr);
        if (parsedRole == null) {
            LOGGER.log(Level.WARNING, "Unknown user role: {0}", roleStr);
            throw new IllegalArgumentException("Unknown user role: " + roleStr);
        }
        user.setRole(parsedRole);

        int deptId = rs.getInt("id_departement");
        user.setIdDepartement(rs.wasNull() ? null : deptId);
        int blocId = rs.getInt("id_bloc");
        user.setIdBloc(rs.wasNull() ? null : blocId);

        user.setTelephone(rs.getString("telephone"));
        user.setPhotoProfil(rs.getString("photo_profil"));
        user.setActif(rs.getBoolean("actif"));
        user.setMustChangePassword(rs.getBoolean("must_change_password"));
        user.setDateCreation(rs.getTimestamp("date_creation"));
        user.setDerniereConnexion(rs.getTimestamp("derniere_connexion"));
        user.setNomDepartement(rs.getString("nom_departement"));
        user.setNomBloc(rs.getString("nom_bloc"));
        return user;
    }

    private User.Role parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return null;
        }
        String normalizedRole = roleValue.trim();
        String normalizedWithUnderscore = normalizedRole.replace(' ', '_');
        String normalizedUpperUnderscore = normalizedWithUnderscore.toUpperCase(Locale.ROOT);

        for (User.Role role : User.Role.values()) {
            List<String> aliases = getRoleAliases(role);
            for (String alias : aliases) {
                if (alias.equalsIgnoreCase(normalizedRole)) {
                    return role;
                }
                String aliasUnderscore = alias.replace(' ', '_');
                if (aliasUnderscore.equalsIgnoreCase(normalizedWithUnderscore)) {
                    return role;
                }
                if (aliasUnderscore.toUpperCase(Locale.ROOT).equals(normalizedUpperUnderscore)) {
                    return role;
                }
            }
        }
        return null;
    }

    private List<String> getRoleAliases(User.Role role) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(role.getRoleName());
        aliases.add(role.name());
        aliases.add(role.name().toUpperCase(Locale.ROOT));
        aliases.add(role.getRoleName().replace(' ', '_'));
        aliases.add(role.getRoleName().replace(' ', '_').toUpperCase(Locale.ROOT));
        return new ArrayList<>(aliases);
    }

    private void requireAuthenticated() {
        SessionContext.requireAuthenticated();
    }
}
