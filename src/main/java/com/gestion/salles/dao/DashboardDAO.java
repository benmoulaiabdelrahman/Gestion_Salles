package com.gestion.salles.dao;

/******************************************************************************
 * DashboardDAO.java
 *
 * Data access layer for the Admin Dashboard overview statistics. All count
 * queries delegate to the private getCount() overloads which handle both
 * unparameterised and single-integer-parameter variants. Every public stat
 * method accepts a nullable Integer blocId — null means no bloc filter.
 * The room-type distribution query follows the same nullable-blocId pattern
 * via getRoomTypeDistribution(Integer blocId).
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List; // Added
import java.util.ArrayList; // Added
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gestion.salles.models.DashboardScope;

public class DashboardDAO {

    private static final Logger LOGGER = Logger.getLogger(DashboardDAO.class.getName());
    private final RoomDAO roomDAO; // Added RoomDAO field

    public DashboardDAO() {
        this.roomDAO = new RoomDAO(); // Initialize RoomDAO
    }

    public int getRoomCount(DashboardScope scope) {
        if (scope.getRole() == DashboardScope.Role.CHEF_DEPARTEMENT && scope.getBlocId() == null) {
            throw new IllegalArgumentException("Chef scope requires a valid blocId");
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM salles WHERE actif = 1");
        List<Object> params = new ArrayList<>();
        if (scope.getBlocId() != null) {
            sqlBuilder.append(" AND id_bloc = ?");
            params.add(scope.getBlocId());
        }
        return getCount(sqlBuilder.toString(), params);
    }

    public int getActiveUserCount(DashboardScope scope) {
        if (scope.getRole() == DashboardScope.Role.CHEF_DEPARTEMENT && scope.getBlocId() == null) {
            throw new IllegalArgumentException("Chef scope requires a valid blocId");
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM utilisateurs WHERE actif = 1");
        List<Object> params = new ArrayList<>();
        if (scope.getBlocId() != null) {
            sqlBuilder.append(" AND id_bloc = ?");
            params.add(scope.getBlocId());
        }
        return getCount(sqlBuilder.toString(), params);
    }

    public int getReservationsTodayCount(DashboardScope scope) {
        if (scope.getRole() == DashboardScope.Role.CHEF_DEPARTEMENT && scope.getBlocId() == null) {
            throw new IllegalArgumentException("Chef scope requires a valid blocId");
        }
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) FROM reservations WHERE statut <> 'ANNULEE' AND (");
        sqlBuilder.append("    (is_recurring = FALSE AND date_reservation = CURDATE())");
        sqlBuilder.append("    OR ");
        sqlBuilder.append("    (is_recurring = TRUE AND CURDATE() BETWEEN date_debut_recurrence AND date_fin_recurrence AND  ((DAYOFWEEK(CURDATE()) + 5) % 7 + 1) = day_of_week)");
        sqlBuilder.append(")");
        
        List<Object> params = new ArrayList<>();
        if (scope.getBlocId() != null) {
            sqlBuilder.append(" AND id_bloc = ?");
            params.add(scope.getBlocId());
        }
        return getCount(sqlBuilder.toString(), params);
    }

    public int getRoomsCurrentlyInUseCount(DashboardScope scope) {
        if (scope.getRole() == DashboardScope.Role.CHEF_DEPARTEMENT && scope.getBlocId() == null) {
            throw new IllegalArgumentException("Chef scope requires a valid blocId");
        }
        // Ensure the online room exists and get its ID
        int onlineRoomId = roomDAO.ensureOnlineRoomExists();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(DISTINCT id_salle) FROM reservations ");
        sqlBuilder.append("WHERE statut <> 'ANNULEE' "); // Moved statut filter here

        List<Object> params = new ArrayList<>();

        // Exclude online room
        if (onlineRoomId > 0) { // Only exclude if a valid onlineRoomId was found
            sqlBuilder.append("AND id_salle <> ? ");
            params.add(onlineRoomId);
        }

        // Handle recurring and fixed reservations
        sqlBuilder.append("AND (");
        sqlBuilder.append("    (is_recurring = FALSE AND date_reservation = CURDATE())");
        sqlBuilder.append("    OR ");
        sqlBuilder.append("    (is_recurring = TRUE AND CURDATE() BETWEEN date_debut_recurrence AND date_fin_recurrence AND ((DAYOFWEEK(CURDATE()) + 5) % 7 + 1) = day_of_week)");
        sqlBuilder.append(") ");

        // Handle current time
        sqlBuilder.append("AND NOW() BETWEEN heure_debut AND heure_fin ");


        if (scope.getBlocId() != null) {
            sqlBuilder.append(" AND id_bloc = ?");
            params.add(scope.getBlocId());
        }

        return getCount(sqlBuilder.toString(), params);
    }

    public Map<String, Integer> getRoomTypeDistribution(DashboardScope scope) {
        if (scope.getRole() == DashboardScope.Role.CHEF_DEPARTEMENT && scope.getBlocId() == null) {
            throw new IllegalArgumentException("Chef scope requires a valid blocId");
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT s.type_salle, COUNT(*) AS count FROM salles s WHERE s.actif = 1";
        if (scope.getBlocId() != null) sql += " AND s.id_bloc = ?";
        sql += " GROUP BY s.type_salle ORDER BY count DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (scope.getBlocId() != null) stmt.setInt(1, scope.getBlocId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("type_salle"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting room type distribution", e);
        }
        return result;
    }



    private int getCount(String sql, List<Object> params) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing count query with multiple parameters", e);
        }
        return 0;
    }
}
