package com.gestion.salles.dao;

/******************************************************************************
 * ActivityTypeDAO.java
 *
 * Data access layer for the types_activites table. Provides lookup by ID
 * and full-list retrieval. ResultSet mapping is centralised in mapRow() to
 * eliminate duplication between the two query methods.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.ActivityType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityTypeDAO {

    private static final Logger LOGGER = Logger.getLogger(ActivityTypeDAO.class.getName());

    private static final String SELECT_COLUMNS =
        "SELECT id_type_activite, nom_type, couleur_hex, is_group_specific FROM types_activites";

    public ActivityType getActivityTypeById(int id) {
        String sql = SELECT_COLUMNS + " WHERE id_type_activite = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching activity type with ID: " + id, e);
        }
        return null;
    }

    public List<ActivityType> getAllActivityTypes() {
        List<ActivityType> activityTypes = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_COLUMNS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                activityTypes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all activity types", e);
        }
        return activityTypes;
    }

    private ActivityType mapRow(ResultSet rs) throws SQLException {
        ActivityType activityType = new ActivityType();
        activityType.setId(rs.getInt("id_type_activite"));
        activityType.setName(rs.getString("nom_type"));
        activityType.setColorHex(rs.getString("couleur_hex"));
        activityType.setGroupSpecific(rs.getBoolean("is_group_specific"));
        return activityType;
    }
}
