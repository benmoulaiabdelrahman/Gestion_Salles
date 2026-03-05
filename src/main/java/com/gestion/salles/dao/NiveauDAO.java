package com.gestion.salles.dao;

/******************************************************************************
 * NiveauDAO.java
 *
 * Data access layer for the 'niveaux' table. All list queries share the
 * BASE_SELECT constant and delegate to fetchList() which handles both
 * unparameterised and multi-parameter variants. Single-row lookups use
 * fetchOne(). The four existsBy* overloads collapse into existsBy() which
 * accepts a nullable excludeId, mirroring the DepartementDAO pattern.
 * Nullable id_bloc is handled uniformly in mapRow() via getObject().
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.Niveau;
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
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gestion.salles.dao.ActivityLogDAO;

public class NiveauDAO {

    private static final Logger LOGGER = Logger.getLogger(NiveauDAO.class.getName());
    private static final Pattern ANNEE_ACADEMIQUE_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");
    private final ActivityLogDAO activityLogDAO;

    private static final String BASE_SELECT =
            "SELECT n.id_niveau, n.nom_niveau, n.code_niveau, n.id_departement, d.nom_departement, " +
            "       n.id_bloc, b.nom_bloc, n.nombre_etudiants, n.nombre_groupes, n.annee_academique, n.actif " +
            "FROM niveaux n " +
            "JOIN departements d ON n.id_departement = d.id_departement " +
            "LEFT JOIN blocs b ON n.id_bloc = b.id_bloc ";

    public NiveauDAO() {
        this(new ActivityLogDAO());
    }

    public NiveauDAO(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    public List<Niveau> getAllNiveaux() {
        return fetchList(BASE_SELECT);
    }

    public List<Niveau> getAllActiveNiveaux() {
        return fetchList(BASE_SELECT + "WHERE n.actif = TRUE ORDER BY n.nom_niveau");
    }

    public List<Niveau> getNiveauxByDepartement(int idDepartement) {
        return fetchList(BASE_SELECT + "WHERE n.id_departement = ? AND n.actif = TRUE", idDepartement);
    }

    public List<Niveau> getNiveauxByBloc(int idBloc) {
        return fetchList(BASE_SELECT + "WHERE n.id_bloc = ? AND n.actif = TRUE", idBloc);
    }

    public List<Niveau> getNiveauxByDepartementAndBloc(int idDepartement, int idBloc) {
        return fetchList(BASE_SELECT + "WHERE n.id_departement = ? AND n.id_bloc = ? AND n.actif = TRUE",
                idDepartement, idBloc);
    }

    public Niveau getNiveauById(Integer id) {
        if (id == null) return null;
        return fetchOne(BASE_SELECT + "WHERE n.id_niveau = ?", id);
    }

    public boolean addNiveau(Niveau niveau) {
        requireAuthenticated();
        String sql = "INSERT INTO niveaux (nom_niveau, code_niveau, id_departement, id_bloc, " +
                     "nombre_etudiants, nombre_groupes, annee_academique, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        validateAnneeAcademique(niveau.getAnneeAcademique());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindNiveau(stmt, niveau);
            stmt.setBoolean(8, niveau.isActif());

            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) niveau.setId(keys.getInt(1));
                }
                activityLogDAO.insert(ActivityLog.ActionType.CREATE, ActivityLog.EntityType.NIVEAU,
                        niveau.getId(), SessionContext.get(),
                        "Nouveau niveau '" + niveau.getNom() + "' ajouté.",
                        niveau.getIdBloc()); // Log the actual id_bloc
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding niveau: " + niveau.getNom(), e);
        }
        return false;
    }

    public boolean updateNiveau(Niveau niveau) {
        requireAuthenticated();
        String sql = "UPDATE niveaux SET nom_niveau = ?, code_niveau = ?, id_departement = ?, id_bloc = ?, " +
                     "nombre_etudiants = ?, nombre_groupes = ?, annee_academique = ?, actif = ? WHERE id_niveau = ?";

        validateAnneeAcademique(niveau.getAnneeAcademique());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            bindNiveau(stmt, niveau);
            stmt.setBoolean(8, niveau.isActif());
            stmt.setInt(9, niveau.getId());

            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.UPDATE, ActivityLog.EntityType.NIVEAU,
                        niveau.getId(), SessionContext.get(),
                        "Niveau '" + niveau.getNom() + "' mis à jour.",
                        niveau.getIdBloc()); // Log the actual id_bloc
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating niveau: " + niveau.getNom(), e);
        }
        return false;
    }

    public boolean deleteNiveau(int id) {
        requireAuthenticated();
        Niveau niveauToDelete = getNiveauById(id);
        if (niveauToDelete == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete non-existent niveau with ID: " + id);
            return false;
        }

        String sql = "DELETE FROM niveaux WHERE id_niveau = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.DELETE, ActivityLog.EntityType.NIVEAU,
                        id, SessionContext.get(),
                        "Niveau '" + niveauToDelete.getNom() + "' supprimé.",
                        niveauToDelete.getIdBloc()); // Log the actual id_bloc
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting niveau with ID: " + id, e);
        }
        return false;
    }

    public boolean existsNiveauByName(String name) {
        return existsBy("nom_niveau", name, null);
    }

    public boolean existsNiveauByName(String name, int excludeId) {
        return existsBy("nom_niveau", name, excludeId);
    }

    public boolean existsNiveauByCode(String code) {
        return existsBy("code_niveau", code, null);
    }

    public boolean existsNiveauByCode(String code, int excludeId) {
        return existsBy("code_niveau", code, excludeId);
    }

    private List<Niveau> fetchList(String sql, int... params) {
        List<Niveau> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) stmt.setInt(i + 1, params[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing list query", e);
        }
        return result;
    }

    private Niveau fetchOne(String sql, int param) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
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

    private void bindNiveau(PreparedStatement stmt, Niveau niveau) throws SQLException {
        stmt.setString(1, niveau.getNom());
        stmt.setString(2, niveau.getCode());
        stmt.setInt(3, niveau.getIdDepartement());
        if (niveau.getIdBloc() != null) {
            stmt.setInt(4, niveau.getIdBloc());
        } else {
            stmt.setNull(4, Types.INTEGER);
        }
        stmt.setInt(5, niveau.getNombreEtudiants());
        stmt.setInt(6, niveau.getNombreGroupes());
        stmt.setString(7, niveau.getAnneeAcademique());
    }

    private Niveau mapRow(ResultSet rs) throws SQLException {
        Niveau n = new Niveau();
        n.setId(rs.getInt("id_niveau"));
        n.setNom(rs.getString("nom_niveau"));
        n.setCode(rs.getString("code_niveau"));
        n.setIdDepartement(rs.getInt("id_departement"));
        n.setDepartementName(rs.getString("nom_departement"));
        n.setIdBloc(rs.getObject("id_bloc", Integer.class));
        n.setNomBloc(rs.getString("nom_bloc"));
        n.setNombreEtudiants(rs.getInt("nombre_etudiants"));
        n.setNombreGroupes(rs.getInt("nombre_groupes"));
        n.setAnneeAcademique(rs.getString("annee_academique"));
        n.setActif(rs.getBoolean("actif"));
        return n;
    }

    private boolean existsBy(String column, String value, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM niveaux WHERE " + column + " = ?" +
                     (excludeId != null ? " AND id_niveau != ?" : "");

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

    private void validateAnneeAcademique(String value) {
        if (value == null || !ANNEE_ACADEMIQUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("annee_academique must match format YYYY-YYYY (example: 2025-2026)");
        }
    }

    private void requireAuthenticated() {
        SessionContext.requireAuthenticated();
    }
}
