package com.gestion.salles.dao;

/******************************************************************************
 * BlocDAO.java
 *
 * Data access layer for the blocs table. All read queries join departements
 * via BASE_SELECT so Departement info is always hydrated in one query.
 * ResultSet mapping is centralised in mapRow(), and the nullable departement
 * parameter is handled once in setDepartementParam().
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.utils.SessionContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlocDAO {

    private static final Logger LOGGER = Logger.getLogger(BlocDAO.class.getName());
    private final ActivityLogDAO activityLogDAO;

    private static final String BASE_SELECT =
        "SELECT b.id_bloc, b.nom_bloc, b.code_bloc, b.adresse, b.nombre_etages, b.actif, " +
        "d.id_departement, d.nom_departement " +
        "FROM blocs b " +
        "LEFT JOIN departements d ON b.id_departement = d.id_departement ";

    public BlocDAO() {
        this(new ActivityLogDAO());
    }

    public BlocDAO(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    public List<Bloc> getAllBlocs() {
        List<Bloc> blocs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(BASE_SELECT + "ORDER BY b.nom_bloc");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) blocs.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all blocs", e);
        }
        return blocs;
    }

    public List<Bloc> getAllActiveBlocs() {
        List<Bloc> blocs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(BASE_SELECT + "WHERE b.actif = TRUE ORDER BY b.nom_bloc");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) blocs.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all active blocs", e);
        }
        return blocs;
    }

    public List<Bloc> getBlocsByDepartement(Integer departementId) {
        List<Bloc> blocs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                BASE_SELECT + "WHERE b.id_departement = ? AND b.actif = TRUE ORDER BY b.nom_bloc")) {
            stmt.setInt(1, departementId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) blocs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching blocs by departementId: " + departementId, e);
        }
        return blocs;
    }

    public Bloc getBlocById(Integer idBloc) {
        if (idBloc == null) return null;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(BASE_SELECT + "WHERE b.id_bloc = ?")) {
            stmt.setInt(1, idBloc);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching bloc by ID: " + idBloc, e);
        }
        return null;
    }

    public boolean addBloc(Bloc bloc) {
        requireAuthenticated();
        String sql = "INSERT INTO blocs (nom_bloc, code_bloc, adresse, nombre_etages, actif, id_departement) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, bloc.getNom());
            stmt.setString(2, bloc.getCode());
            stmt.setString(3, bloc.getAdresse());
            stmt.setInt(4, bloc.getNombreEtages());
            stmt.setBoolean(5, bloc.isActif());
            setDepartementParam(stmt, 6, bloc);
            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) bloc.setId(keys.getInt(1));
                }
                activityLogDAO.insert(ActivityLog.ActionType.CREATE, ActivityLog.EntityType.DEPARTEMENT,
                        bloc.getId(), SessionContext.get(),
                        "Nouveau département '" + bloc.getNom() + "' ajouté.",
                        bloc.getId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding bloc: " + bloc.getNom(), e);
        }
        return false;
    }

    public boolean updateBloc(Bloc bloc) {
        requireAuthenticated();
        String sql = "UPDATE blocs SET nom_bloc = ?, code_bloc = ?, adresse = ?, nombre_etages = ?, actif = ?, id_departement = ? WHERE id_bloc = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bloc.getNom());
            stmt.setString(2, bloc.getCode());
            stmt.setString(3, bloc.getAdresse());
            stmt.setInt(4, bloc.getNombreEtages());
            stmt.setBoolean(5, bloc.isActif());
            setDepartementParam(stmt, 6, bloc);
            stmt.setInt(7, bloc.getId());
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.UPDATE, ActivityLog.EntityType.DEPARTEMENT,
                        bloc.getId(), SessionContext.get(),
                        "Département '" + bloc.getNom() + "' mis à jour.",
                        bloc.getId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating bloc: " + bloc.getNom(), e);
        }
        return false;
    }

    public boolean deleteBloc(int blocId) {
        requireAuthenticated();
        Bloc blocToDelete = getBlocById(blocId);
        if (blocToDelete == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete non-existent bloc with ID: " + blocId);
            return false;
        }
        String sql = "DELETE FROM blocs WHERE id_bloc = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, blocId);
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.DELETE, ActivityLog.EntityType.DEPARTEMENT,
                        blocId, SessionContext.get(),
                        "Département '" + blocToDelete.getNom() + "' supprimé.",
                        null);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting bloc with ID: " + blocId, e);
        }
        return false;
    }

    public boolean existsBlocByName(String name, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM blocs WHERE nom_bloc = ?" +
                     (excludeId != null ? " AND id_bloc != ?" : "");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            if (excludeId != null) stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if bloc name exists: " + name, e);
        }
        return false;
    }

    public boolean existsBlocByCode(String code, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM blocs WHERE code_bloc = ?" +
                     (excludeId != null ? " AND id_bloc != ?" : "");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            if (excludeId != null) stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if bloc code exists: " + code, e);
        }
        return false;
    }

    private Bloc mapRow(ResultSet rs) throws SQLException {
        Bloc bloc = new Bloc();
        bloc.setId(rs.getInt("id_bloc"));
        bloc.setNom(rs.getString("nom_bloc"));
        bloc.setCode(rs.getString("code_bloc"));
        bloc.setAdresse(rs.getString("adresse"));
        bloc.setNombreEtages(rs.getInt("nombre_etages"));
        bloc.setActif(rs.getBoolean("actif"));
        int departementId = rs.getInt("id_departement");
        if (!rs.wasNull()) {
            bloc.setDepartement(new Departement(departementId, rs.getString("nom_departement"), null, null, false));
        }
        return bloc;
    }

    private void setDepartementParam(PreparedStatement stmt, int index, Bloc bloc) throws SQLException {
        if (bloc.getDepartement() != null && bloc.getDepartement().getId() != 0) {
            stmt.setInt(index, bloc.getDepartement().getId());
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    private void requireAuthenticated() {
        SessionContext.requireAuthenticated();
    }
}
