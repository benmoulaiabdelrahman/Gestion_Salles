package com.gestion.salles.dao;

/******************************************************************************
 * RoomDAO.java
 *
 * Data access layer for the 'salles' table. The three-table SELECT/JOIN is
 * defined once in BASE_SELECT; all list queries delegate to fetchList() with
 * a WHERE-clause suffix. Single-row lookups use fetchOne(). INSERT and UPDATE
 * share bindRoom() for the common parameter block. The repeated time-overlap
 * NOT IN subquery is extracted into AVAILABILITY_SUBQUERY; the two dynamic
 * availability methods build their WHERE prefix then append it. Nullable
 * id_departement_principal is handled uniformly in bindRoom() via setNullableInt().
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.Room;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.utils.SessionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gestion.salles.dao.ActivityLogDAO;

public class RoomDAO {

    private static final Logger LOGGER = Logger.getLogger(RoomDAO.class.getName());
    private static final int DEFAULT_LIST_LIMIT = 500;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ActivityLogDAO activityLogDAO;

    private static final String BASE_SELECT =
            "SELECT s.id_salle, s.numero_salle, s.id_bloc, b.nom_bloc, s.capacite, s.type_salle, " +
            "       s.equipements, s.etage, s.id_departement_principal, d.nom_departement, s.observations, s.actif " +
            "FROM salles s " +
            "JOIN blocs b ON s.id_bloc = b.id_bloc " +
            "LEFT JOIN departements d ON s.id_departement_principal = d.id_departement ";

    private static final String AVAILABILITY_SUBQUERY =
            "AND s.id_salle NOT IN (" +
            "  SELECT r.id_salle FROM reservations r " +
            "  WHERE r.date_reservation = ? " +
            "    AND ((r.heure_debut < ? AND r.heure_fin > ?) " +
            "      OR (r.heure_debut >= ? AND r.heure_debut < ?) " +
            "      OR (r.heure_fin > ? AND r.heure_fin <= ?)) " +
            "    AND r.statut IN ('CONFIRMEE', 'EN_ATTENTE')) " +
            "ORDER BY s.capacite DESC, s.numero_salle";

    public RoomDAO() {
        this(new ActivityLogDAO());
    }

    public RoomDAO(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    public List<Room> getAllRooms() {
        return getRoomsPage(0, DEFAULT_LIST_LIMIT);
    }

    public List<Room> getRoomsPage(int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, limit);
        return fetchList(BASE_SELECT + "ORDER BY s.numero_salle ASC LIMIT ? OFFSET ?", safeLimit, safeOffset);
    }

    public List<Room> getAllActiveRooms() {
        return fetchList(BASE_SELECT + "WHERE s.actif = TRUE ORDER BY s.numero_salle ASC LIMIT ?", DEFAULT_LIST_LIMIT);
    }

    public List<Room> getRoomsByBloc(int idBloc) {
        return fetchList(BASE_SELECT + "WHERE s.id_bloc = ?", idBloc);
    }

    public List<Room> getRoomsByDepartement(int idDepartement) {
        return fetchList(BASE_SELECT + "WHERE s.id_departement_principal = ?", idDepartement);
    }

    public List<Room> getRoomsByDepartmentAndBloc(int idDepartement, int idBloc) {
        return fetchList(BASE_SELECT + "WHERE s.id_departement_principal = ? AND s.id_bloc = ?",
                idDepartement, idBloc);
    }

    public List<Room> getRoomsByDepartmentAndActive(int idDepartement) {
        return fetchList(BASE_SELECT + "WHERE s.id_departement_principal = ? AND s.actif = TRUE",
                idDepartement);
    }

    public List<Room> getRoomsByBlocAndActive(int idBloc) {
        return fetchList(BASE_SELECT + "WHERE s.id_bloc = ? AND s.actif = TRUE", idBloc);
    }

    public List<Room> getRoomsByDepartmentAndBlocAndActive(int idDepartement, int idBloc) {
        return fetchList(BASE_SELECT + "WHERE s.id_departement_principal = ? AND s.id_bloc = ? AND s.actif = TRUE",
                idDepartement, idBloc);
    }

    public Room getRoomById(int id) {
        return fetchOne(BASE_SELECT + "WHERE s.id_salle = ?", id);
    }

    public Room getRoomByName(String name) {
        String sql = BASE_SELECT + "WHERE s.numero_salle = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting room by name: " + name, e);
        }
        return null;
    }

    public Room getOnlineRoom() {
        String sql = BASE_SELECT + "WHERE s.numero_salle IN ('ONLINE', 'VIRTUAL')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting online/virtual room", e);
        }
        return null;
    }

    public List<String> getRoomTypes() {
        String sql = "SELECT DISTINCT type_salle FROM salles WHERE type_salle IS NOT NULL AND type_salle != '' ORDER BY type_salle";
        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) result.add(rs.getString("type_salle"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting room types", e);
        }
        return result;
    }

    public int addRoom(Room room) {
        requireAuthenticated();
        String sql = "INSERT INTO salles (numero_salle, id_bloc, capacite, type_salle, equipements, etage, " +
                     "id_departement_principal, observations, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindRoom(stmt, room);
            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);
                        room.setId(newId);
                        activityLogDAO.insert(ActivityLog.ActionType.CREATE, ActivityLog.EntityType.ROOM,
                                room.getId(), SessionContext.get(), "Nouvelle salle '" + room.getName() + "' ajoutée.", room.getIdBloc());
                        return newId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding room: " + room.getName(), e);
        }
        return -1;
    }

    public boolean updateRoom(Room room) {
        requireAuthenticated();
        String sql = "UPDATE salles SET numero_salle = ?, id_bloc = ?, capacite = ?, type_salle = ?, " +
                     "equipements = ?, etage = ?, id_departement_principal = ?, observations = ?, actif = ? " +
                     "WHERE id_salle = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRoom(stmt, room);
            stmt.setInt(10, room.getId());
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.UPDATE, ActivityLog.EntityType.ROOM,
                        room.getId(), SessionContext.get(), "Salle '" + room.getName() + "' mise à jour.", room.getIdBloc());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating room: " + room.getName(), e);
        }
        return false;
    }

    public boolean deleteRoom(int id) {
        requireAuthenticated();
        Room roomToDelete = getRoomById(id);
        if (roomToDelete == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete non-existent room with ID: " + id);
            return false;
        }

        String sql = "DELETE FROM salles WHERE id_salle = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.DELETE, ActivityLog.EntityType.ROOM,
                        id, SessionContext.get(), "Salle '" + roomToDelete.getName() + "' supprimée.", roomToDelete.getIdBloc());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting room with ID: " + id, e);
        }
        return false;
    }

    public List<Room> getAvailableRooms(LocalDate date, LocalTime startTime, LocalTime endTime,
                                        Integer minCapacity, String typeSalle) {
        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append("WHERE s.actif = TRUE AND s.capacite >= ? ");
        List<Object> params = new ArrayList<>();
        params.add(minCapacity != null ? minCapacity : 0);

        if (typeSalle != null && !typeSalle.isEmpty()) {
            sql.append("AND s.type_salle = ? ");
            params.add(typeSalle);
        }

        sql.append(AVAILABILITY_SUBQUERY);
        addTimeParams(params, date, startTime, endTime);
        return fetchAvailability(sql.toString(), params);
    }

    public List<Room> getAvailableRoomsByDepartment(LocalDate date, LocalTime startTime, LocalTime endTime,
                                                    Integer idDepartement) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE s.actif = TRUE ");
        List<Object> params = new ArrayList<>();

        if (idDepartement != null) {
            sql.append("AND s.id_departement_principal = ? ");
            params.add(idDepartement);
        }

        sql.append(AVAILABILITY_SUBQUERY);
        addTimeParams(params, date, startTime, endTime);
        return fetchAvailability(sql.toString(), params);
    }

    public List<Room> getAvailableRoomsByDepartmentAndType(LocalDate date, LocalTime startTime, LocalTime endTime,
                                                           Integer idDepartement, String typeSalle) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE s.actif = TRUE ");
        List<Object> params = new ArrayList<>();

        if (idDepartement != null) {
            sql.append("AND s.id_departement_principal = ? ");
            params.add(idDepartement);
        }
        if (typeSalle != null && !typeSalle.isEmpty()) {
            sql.append("AND s.type_salle = ? ");
            params.add(typeSalle);
        }

        sql.append(AVAILABILITY_SUBQUERY);
        addTimeParams(params, date, startTime, endTime);
        return fetchAvailability(sql.toString(), params);
    }

    private List<Room> fetchList(String sql, Object... params) {
        List<Room> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing list query", e);
        }
        return result;
    }

    private Room fetchOne(String sql, int param) {
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

    private List<Room> fetchAvailability(String sql, List<Object> params) {
        List<Room> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing availability query", e);
        }
        return result;
    }

    private void bindRoom(PreparedStatement stmt, Room room) throws SQLException {
        stmt.setString(1, room.getName());
        stmt.setInt(2, room.getIdBloc());
        stmt.setInt(3, room.getCapacity());
        stmt.setString(4, room.getTypeSalle());
        String equipements = normalizeEquipementsJson(room.getEquipment());
        stmt.setString(5, equipements);
        stmt.setInt(6, room.getEtage());
        if (room.getIdDepartementPrincipal() != null) {
            stmt.setInt(7, room.getIdDepartementPrincipal());
        } else {
            stmt.setNull(7, Types.INTEGER);
        }
        stmt.setString(8, room.getObservations());
        stmt.setBoolean(9, room.isActif());
    }

    private void addTimeParams(List<Object> params, LocalDate date, LocalTime startTime, LocalTime endTime) {
        params.add(Date.valueOf(date));
        params.add(Time.valueOf(endTime));
        params.add(Time.valueOf(startTime));
        params.add(Time.valueOf(startTime));
        params.add(Time.valueOf(endTime));
        params.add(Time.valueOf(startTime));
        params.add(Time.valueOf(endTime));
    }

    private void requireAuthenticated() {
        SessionContext.requireAuthenticated();
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id_salle"));
        room.setName(rs.getString("numero_salle"));
        room.setIdBloc(rs.getInt("id_bloc"));
        room.setBlockName(rs.getString("nom_bloc"));
        room.setCapacity(rs.getInt("capacite"));
        room.setTypeSalle(rs.getString("type_salle"));
        room.setEquipment(normalizeEquipementsJson(rs.getString("equipements")));
        room.setEtage(rs.getInt("etage"));
        room.setIdDepartementPrincipal(rs.getObject("id_departement_principal", Integer.class));
        room.setDepartmentName(rs.getString("nom_departement"));
        room.setObservations(rs.getString("observations"));
        room.setActif(rs.getBoolean("actif"));
        return room;
    }

    private String normalizeEquipementsJson(String equipementsJson) {
        if (equipementsJson == null || equipementsJson.isBlank()) {
            return null;
        }
        String trimmed = equipementsJson.trim();
        try {
            // Validate JSON (object or array). If valid, preserve original payload.
            OBJECT_MAPPER.readTree(trimmed);
            return trimmed;
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Malformed equipements JSON. Falling back to []", e);
            return "[]";
        }
    }

    /**
     * Ensures that an "En ligne" room exists in the database.
     * If it does not exist, it creates one.
     *
     * @return The ID of the "En ligne" room.
     */
    public int ensureOnlineRoomExists() {
        Room onlineRoom = getRoomByName("En ligne");
        if (onlineRoom == null) {
            LOGGER.log(Level.INFO, "Online room 'En ligne' not found. Creating it now.");

            // Create a new Room object for "En ligne"
            // id is 0 for auto-increment
            // idBloc=8 (Informatique), capacity=999, typeSalle='VIRTUAL', equipment='[]', etage=0,
            // idDepartementPrincipal=2 (Sciences), observations='Salle virtuelle pour les reservations en ligne', actif=true
            Room newOnlineRoom = new Room(
                    0, // id (will be auto-incremented)
                    "En ligne",
                    8, // idBloc (placeholder, e.g., Informatique)
                    999, // capacity (arbitrarily large)
                    "REUNION", // typeSalle (Changed from VIRTUAL to REUNION to match enum)
                    "{}", // equipment (empty JSON object)
                    0, // etage
                    2, // idDepartementPrincipal (placeholder, e.g., Sciences)
                    "Salle virtuelle pour les reservations en ligne",
                    true // actif
            );

            int newId = addRoom(newOnlineRoom);
            if (newId > 0) {
                LOGGER.log(Level.INFO, "Online room 'En ligne' created successfully with ID: " + newId);
                return newId;
            } else {
                LOGGER.log(Level.SEVERE, "Failed to create online room 'En ligne'.");
                return 0; // Indicate failure
            }
        }
        return onlineRoom.getId();
    }
}
