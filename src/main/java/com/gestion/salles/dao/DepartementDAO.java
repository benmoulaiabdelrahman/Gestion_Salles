package com.gestion.salles.dao;

/******************************************************************************
 * DepartementDAO.java
 *
 * Data access layer for the 'departements' table. Provides full CRUD plus
 * existence checks and filtered queries. Repeated ResultSet mapping is
 * centralised in mapRow(). Uniqueness checks are handled by the single
 * existsBy() overload. The nullable excludeId parameter allows the same
 * method to serve both insert and update validation.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.utils.SessionContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gestion.salles.dao.ActivityLogDAO;

public class DepartementDAO {

    private static final Logger LOGGER = Logger.getLogger(DepartementDAO.class.getName());
    private final ActivityLogDAO activityLogDAO;

    private static final String RANKED_BLOC_JOIN =
            "LEFT JOIN ( " +
            "    SELECT n.id_departement, n.id_bloc, " +
            "           ROW_NUMBER() OVER (PARTITION BY n.id_departement ORDER BY COUNT(*) DESC) AS rn " +
            "    FROM niveaux n WHERE n.id_bloc IS NOT NULL " +
            "    GROUP BY n.id_departement, n.id_bloc " +
            ") AS ranked_blocs ON d.id_departement = ranked_blocs.id_departement AND ranked_blocs.rn = 1 " +
            "LEFT JOIN blocs b ON ranked_blocs.id_bloc = b.id_bloc ";

    public DepartementDAO() {
        this(new ActivityLogDAO());
    }

    public DepartementDAO(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    public List<Departement> getAllDepartements() {
        String sql = "SELECT d.id_departement, d.nom_departement, d.code_departement, d.description, d.actif, b.nom_bloc " +
                     "FROM departements d " + RANKED_BLOC_JOIN +
                     "ORDER BY d.nom_departement";

        List<Departement> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs, false));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all departements", e);
        }
        return result;
    }

    public List<Departement> getAllActiveDepartements() {
        String sql = "SELECT id_departement, nom_departement, code_departement, description, actif " +
                     "FROM departements WHERE actif = TRUE ORDER BY nom_departement";

        List<Departement> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs, false));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching active departements", e);
        }
        return result;
    }

    public Departement getDepartementById(int id) {
        String sql = "SELECT d.id_departement, d.nom_departement, d.code_departement, d.description, d.actif, " +
                     "ranked_blocs.id_bloc, b.nom_bloc " +
                     "FROM departements d " + RANKED_BLOC_JOIN +
                     "WHERE d.id_departement = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs, true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching departement by ID: " + id, e);
        }
        return null;
    }

    public List<Departement> getDepartementsByBloc(int blocId) {
        String sql = "SELECT DISTINCT d.id_departement, d.nom_departement, d.code_departement, d.description, d.actif " +
                     "FROM departements d " +
                     "JOIN salles s ON d.id_departement = s.id_departement_principal " +
                     "WHERE s.id_bloc = ? AND d.actif = TRUE ORDER BY d.nom_departement";

        List<Departement> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, blocId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs, false));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching departements for bloc: " + blocId, e);
        }
        return result;
    }

    public boolean addDepartement(Departement departement) {
        requireAuthenticated();
        String sql = "INSERT INTO departements (nom_departement, code_departement, description, actif) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, departement.getNom());
            stmt.setString(2, departement.getCode());
            stmt.setString(3, departement.getDescription());
            stmt.setBoolean(4, departement.isActif());

            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) departement.setId(keys.getInt(1));
                }
                activityLogDAO.insert(ActivityLog.ActionType.CREATE, ActivityLog.EntityType.FACULTE,
                        departement.getId(), SessionContext.get(),
                        "Nouvelle faculté '" + departement.getNom() + "' ajoutée.",
                        null);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding departement: " + departement.getNom(), e);
        }
        return false;
    }

    public boolean updateDepartement(Departement departement) {
        requireAuthenticated();
        String sql = "UPDATE departements SET nom_departement = ?, code_departement = ?, description = ?, actif = ? " +
                     "WHERE id_departement = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, departement.getNom());
            stmt.setString(2, departement.getCode());
            stmt.setString(3, departement.getDescription());
            stmt.setBoolean(4, departement.isActif());
            stmt.setInt(5, departement.getId());

            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.UPDATE, ActivityLog.EntityType.FACULTE,
                        departement.getId(), SessionContext.get(),
                        "Faculté '" + departement.getNom() + "' mise à jour.",
                        null);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating departement: " + departement.getNom(), e);
        }
        return false;
    }

    public boolean deleteDepartement(int id) {
        requireAuthenticated();
        Departement departementToDelete = getDepartementById(id);
        if (departementToDelete == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete non-existent departement with ID: " + id);
            return false;
        }

        String sql = "DELETE FROM departements WHERE id_departement = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.DELETE, ActivityLog.EntityType.FACULTE,
                        id, SessionContext.get(),
                        "Faculté '" + departementToDelete.getNom() + "' supprimée.",
                        null);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting departement with ID: " + id, e);
        }
        return false;
    }

    public boolean existsDepartementByName(String name, Integer excludeId) {
        return existsBy("nom_departement", name, excludeId);
    }

    public boolean existsDepartementByCode(String code, Integer excludeId) {
        return existsBy("code_departement", code, excludeId);
    }

    private Departement mapRow(ResultSet rs, boolean includeBloc) throws SQLException {
        Departement d = new Departement();
        d.setId(rs.getInt("id_departement"));
        d.setNom(rs.getString("nom_departement"));
        d.setCode(rs.getString("code_departement"));
        d.setDescription(rs.getString("description"));
        d.setActif(rs.getBoolean("actif"));
        if (includeBloc) {
            int idBloc = rs.getInt("id_bloc");
            d.setIdBloc(rs.wasNull() ? null : idBloc);
            d.setBlocName(rs.getString("nom_bloc"));
        } else {
            try { d.setBlocName(rs.getString("nom_bloc")); } catch (SQLException ignored) {}
        }
        return d;
    }

    private boolean existsBy(String column, String value, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM departements WHERE " + column + " = ?" +
                     (excludeId != null ? " AND id_departement != ?" : "");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            if (excludeId != null) stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking uniqueness for " + column + ": " + value, e);
        }
        return false;
    }

    private void requireAuthenticated() {
        SessionContext.requireAuthenticated();
    }
}
