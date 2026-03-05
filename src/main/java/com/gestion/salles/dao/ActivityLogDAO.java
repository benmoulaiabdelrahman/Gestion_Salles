package com.gestion.salles.dao;

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.SessionContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityLogDAO {
    private static final Logger LOGGER = Logger.getLogger(ActivityLogDAO.class.getName());

    public List<ActivityLog> getTodaysActivities() {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = "SELECT al.*, u.nom, u.prenom, u.photo_profil, u.role " +
                     "FROM activity_log al " +
                     "LEFT JOIN utilisateurs u ON al.id_user_acting = u.id_utilisateur " +
                     "WHERE DATE(al.timestamp) = CURDATE() " +
                     "ORDER BY al.timestamp DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                activities.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching activities", e);
        }
        return activities;
    }

    public List<ActivityLog> getTodaysActivitiesForBloc(Integer blocId) {
        if (blocId == null || blocId == 0) {
            LOGGER.warning("getTodaysActivitiesForBloc called with null or 0 blocId — returning empty list.");
            return new ArrayList<>();
        }

        List<ActivityLog> activities = new ArrayList<>();

        // Filter by the entity's bloc, not the acting user's bloc.
        // This ensures we capture all actions (even by admins) that affect this bloc's data.
        String sql = "SELECT al.*, u.nom, u.prenom, u.photo_profil, u.role " +
                     "FROM activity_log al " +
                     "LEFT JOIN utilisateurs u ON al.id_user_acting = u.id_utilisateur " +
                     "WHERE DATE(al.timestamp) = CURDATE() " +
                     "AND al.id_bloc = ? " +
                     "ORDER BY al.timestamp DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, blocId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapRow(rs));
                }
            }
            LOGGER.info("Activities found for blocId " + blocId + ": " + activities.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching bloc activities for blocId: " + blocId, e);
        }
        return activities;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setIdLog(rs.getInt("id_log"));
        log.setTimestamp(rs.getTimestamp("timestamp"));
        log.setActionType(ActivityLog.ActionType.valueOf(rs.getString("action_type")));
        log.setEntityType(ActivityLog.EntityType.valueOf(rs.getString("entity_type")));
        log.setEntityId(rs.getInt("entity_id"));
        log.setDetails(rs.getString("details"));
        log.setIdBloc(rs.getObject("id_bloc", Integer.class)); // Read id_bloc

        Integer userId = (Integer) rs.getObject("id_user_acting");
        if (userId != null) {
            User user = new User();
            user.setIdUtilisateur(userId);
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setPhotoProfil(rs.getString("photo_profil"));
            User.Role parsedRole = parseRole(rs.getString("role"));
            if (parsedRole != null) {
                user.setRole(parsedRole);
            }
            log.setActingUser(user);
        }
        return log;
    }

    private User.Role parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return null;
        }
        String normalized = roleValue.trim().replace(' ', '_');
        for (User.Role role : User.Role.values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
            if (role.getRoleName().equalsIgnoreCase(roleValue.trim())) {
                return role;
            }
        }
        return null;
    }

    public void insert(ActivityLog.ActionType actionType, ActivityLog.EntityType entityType, int entityId, Integer idUserActing, String details, Integer idBloc) {
        Integer resolvedUserId = idUserActing != null ? idUserActing : SessionContext.getCurrentUserId();
        if (resolvedUserId == null) {
            throw new IllegalStateException("Cannot write activity log without an authenticated user.");
        }
        try {
            insertInternal(actionType, entityType, entityId, resolvedUserId, details, idBloc);
        } catch (SQLException e) {
            if (idBloc != null && isBlocForeignKeyViolation(e)) {
                LOGGER.log(Level.WARNING, "Invalid id_bloc " + idBloc + " for activity log; retrying with NULL. Details: " + details, e);
                try {
                    insertInternal(actionType, entityType, entityId, resolvedUserId, details, null);
                    return;
                } catch (SQLException retryException) {
                    LOGGER.log(Level.SEVERE, "Error inserting activity log after fallback: " + details, retryException);
                    return;
                }
            }
            LOGGER.log(Level.SEVERE, "Error inserting activity log: " + details, e);
        }
    }

    public void insert(ActivityLog.ActionType actionType, ActivityLog.EntityType entityType, int entityId, String details, Integer idBloc) {
        insert(actionType, entityType, entityId, null, details, idBloc);
    }

    private void insertInternal(ActivityLog.ActionType actionType, ActivityLog.EntityType entityType, int entityId,
                                int idUserActing, String details, Integer idBloc) throws SQLException {
        String sql = "INSERT INTO activity_log (timestamp, action_type, entity_type, entity_id, id_user_acting, details, id_bloc) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, actionType.name());
            pstmt.setString(3, entityType.name());
            pstmt.setInt(4, entityId);
            pstmt.setInt(5, idUserActing);
            pstmt.setString(6, details);
            if (idBloc != null) {
                pstmt.setInt(7, idBloc);
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }

    private boolean isBlocForeignKeyViolation(SQLException e) {
        return "23000".equals(e.getSQLState())
            && e.getMessage() != null
            && e.getMessage().contains("fk_activity_log_bloc");
    }
}
