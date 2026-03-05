package com.gestion.salles.dao;

/******************************************************************************
 * ReservationDAO.java
 *
 * Data access layer for the 'reservations' table. The six-table SELECT/JOIN
 * is defined once in BASE_SELECT; all list queries delegate to fetchList()
 * with a WHERE-clause suffix. INSERT and UPDATE share bindReservation() for
 * the common parameter block, differing only in the trailing columns.
 * Nullable SQL date/string bindings are handled by setNullableDate() and
 * setNullableString() to avoid repetitive null-checks inline. The three
 * stored-procedure availability checks (room, teacher, niveau) keep their
 * own try-with-resources blocks due to CallableStatement-specific OUT params.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.Reservation.ReservationStatus;
import com.gestion.salles.dao.ReservationMapper;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.utils.UIUtils;

import java.sql.CallableStatement;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gestion.salles.dao.ActivityLogDAO;

public class ReservationDAO {

    private static final Logger LOGGER = Logger.getLogger(ReservationDAO.class.getName());
    private static final int DEFAULT_LIST_LIMIT = 500;
    private final ActivityLogDAO activityLogDAO;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    public ReservationDAO() {
        this(new ActivityLogDAO());
    }

    public ReservationDAO(ActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    private static final String BASE_SELECT =
            "SELECT r.id_reservation, r.id_salle, s.numero_salle, r.id_departement, d.nom_departement, " +
            "       r.id_bloc, b.nom_bloc, r.id_niveau, r.group_number, r.id_enseignant, r.id_type_activite, " +
            "       r.date_reservation, r.heure_debut, r.heure_fin, r.titre_activite, r.description, " +
            "       r.is_online, r.is_recurring, r.date_debut_recurrence, r.date_fin_recurrence, r.day_of_week, " +
            "       r.statut, r.id_utilisateur_creation, r.date_creation, r.date_modification, r.observations, " +
            "       CONCAT(u.nom, ' ', u.prenom) AS nom_enseignant, u.prenom AS prenom_enseignant, " +
            "       niv.nom_niveau AS nom_niveau " +
            "FROM reservations r " +
            "JOIN salles s ON r.id_salle = s.id_salle " +
            "LEFT JOIN departements d ON r.id_departement = d.id_departement " +
            "LEFT JOIN blocs b ON r.id_bloc = b.id_bloc " +
            "LEFT JOIN utilisateurs u ON r.id_enseignant = u.id_utilisateur " +
            "LEFT JOIN niveaux niv ON r.id_niveau = niv.id_niveau ";

    public List<Reservation> getAllReservations() {
        return getReservationsPage(0, DEFAULT_LIST_LIMIT);
    }

    public List<Reservation> getReservationsPage(int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, limit);
        return fetchList(BASE_SELECT + "ORDER BY r.date_creation DESC LIMIT ? OFFSET ?", safeLimit, safeOffset);
    }

    public List<Reservation> getReservationsForRoom(int roomId) {
        return fetchList(BASE_SELECT + "WHERE r.id_salle = ?", roomId);
    }

    public List<Reservation> getReservationsForUser(int userId) {
        return fetchList(BASE_SELECT + "WHERE r.id_utilisateur_creation = ?", userId);
    }

    public List<Reservation> getReservationsByDepartement(int departementId) {
        return fetchList(BASE_SELECT + "WHERE r.id_departement = ?", departementId);
    }

    public List<Reservation> getReservationsByBloc(int blocId) {
        return fetchList(BASE_SELECT + "WHERE r.id_bloc = ?", blocId);
    }

    public Reservation getReservationById(int id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(BASE_SELECT + "WHERE r.id_reservation = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting reservation by ID: " + id, e);
        }
        return null;
    }

    public int addReservation(Reservation reservation) {
        requireAuthenticated();
        String sql = "INSERT INTO reservations (id_salle, is_online, id_departement, id_bloc, id_niveau, group_number, id_enseignant, id_type_activite, " +
                     "date_reservation, heure_debut, heure_fin, titre_activite, description, is_recurring, " +
                     "date_debut_recurrence, date_fin_recurrence, day_of_week, statut, id_utilisateur_creation, date_creation, observations) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int i = bindReservation(stmt, reservation, 1);
            stmt.setString(i++, reservation.getStatut().name());
            stmt.setInt(i++, reservation.getIdUtilisateurCreation());
            stmt.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
            stmt.setString(i, reservation.getObservations());

            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);
                        reservation.setIdReservation(newId);
                        activityLogDAO.insert(ActivityLog.ActionType.CREATE, ActivityLog.EntityType.RESERVATION,
                                reservation.getIdReservation(), reservation.getIdUtilisateurCreation(),
                                "Réservation '" + reservation.getTitreActivite() + "' ajoutée pour la salle " + reservation.getNomSalle(),
                                reservation.getIdBloc());
                        return newId;
                    }
                }
            }
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new ReservationConflictException("Recurring reservation conflict detected", e);
            }
            LOGGER.log(Level.SEVERE, "Error adding reservation: " + reservation.getTitreActivite(), e);
        }
        return -1;
    }

    public boolean updateReservation(Reservation reservation) {
        requireAuthenticated();
        String sql = "UPDATE reservations SET id_salle = ?, is_online = ?, id_departement = ?, id_bloc = ?, id_niveau = ?, group_number = ?, id_enseignant = ?, " +
                     "id_type_activite = ?, date_reservation = ?, heure_debut = ?, heure_fin = ?, titre_activite = ?, description = ?, is_recurring = ?, " +
                     "date_debut_recurrence = ?, date_fin_recurrence = ?, day_of_week = ?, statut = ?, date_modification = ?, observations = ? " +
                     "WHERE id_reservation = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = bindReservation(stmt, reservation, 1);
            stmt.setString(i++, reservation.getStatut().name());
            stmt.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
            stmt.setString(i++, reservation.getObservations());
            stmt.setInt(i, reservation.getIdReservation());

            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.UPDATE, ActivityLog.EntityType.RESERVATION,
                        reservation.getIdReservation(), reservation.getIdUtilisateurCreation(),
                        "Réservation '" + reservation.getTitreActivite() + "' mise à jour.",
                        reservation.getIdBloc());
                return true;
            }
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new ReservationConflictException("Recurring reservation conflict detected", e);
            }
            LOGGER.log(Level.SEVERE, "Error updating reservation with ID: " + reservation.getIdReservation(), e);
        }
        return false;
    }

    public boolean deleteReservation(int id) {
        requireAuthenticated();
        Reservation reservationToDelete = getReservationById(id);
        if (reservationToDelete == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete non-existent reservation with ID: " + id);
            return false;
        }

        String sql = "DELETE FROM reservations WHERE id_reservation = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            if (stmt.executeUpdate() > 0) {
                activityLogDAO.insert(ActivityLog.ActionType.DELETE, ActivityLog.EntityType.RESERVATION,
                        id, reservationToDelete.getIdUtilisateurCreation(),
                        "Réservation '" + reservationToDelete.getTitreActivite() + "' supprimée.",
                        reservationToDelete.getIdBloc());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting reservation with ID: " + id, e);
        }
        return false;
    }

    public boolean updateReservationStatus(int reservationId, ReservationStatus newStatus) {
        requireAuthenticated();
        String sql = "UPDATE reservations SET statut = ?, date_modification = ? WHERE id_reservation = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(3, reservationId);
            if (stmt.executeUpdate() > 0) {
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating reservation status for ID: " + reservationId, e);
        }
        return false;
    }

    public List<String> getReservedSessions(LocalDate date, int idSalle) {
        String sql = "SELECT heure_debut, heure_fin FROM reservations " +
                     "WHERE date_reservation = ? AND id_salle = ? AND statut IN ('CONFIRMEE', 'EN_ATTENTE')";

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setInt(2, idSalle);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getTime("heure_debut").toLocalTime().format(TIME_FMT) + " - " +
                               rs.getTime("heure_fin").toLocalTime().format(TIME_FMT));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting reserved sessions for date " + date + " and room " + idSalle, e);
        }
        return result;
    }

    public boolean checkRoomAvailability(int roomId, LocalDate date, LocalTime startTime, LocalTime endTime, Integer excludeReservationId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement cstmt = conn.prepareCall("{CALL verifier_conflit_salle(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cstmt.setInt(1, roomId);
            cstmt.setBoolean(2, false);
            cstmt.setDate(3, Date.valueOf(date));
            cstmt.setTime(4, Time.valueOf(startTime));
            cstmt.setTime(5, Time.valueOf(endTime));
            cstmt.setNull(6, Types.DATE);
            cstmt.setNull(7, Types.DATE);
            cstmt.setNull(8, Types.VARCHAR);
            if (excludeReservationId != null) cstmt.setInt(9, excludeReservationId); else cstmt.setNull(9, Types.INTEGER);
            cstmt.setBoolean(10, false);
            cstmt.registerOutParameter(11, Types.BOOLEAN);
            cstmt.registerOutParameter(12, Types.INTEGER);
            cstmt.execute();
            return !cstmt.getBoolean(11);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking room availability for room ID: " + roomId, e);
            return false;
        }
    }

    public boolean checkTeacherAvailability(int idEnseignant, LocalDate date, LocalTime startTime, LocalTime endTime, Integer excludeReservationId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement cstmt = conn.prepareCall("{CALL verifier_conflit_enseignant(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cstmt.setInt(1, idEnseignant);
            cstmt.setBoolean(2, false);
            cstmt.setDate(3, Date.valueOf(date));
            cstmt.setTime(4, Time.valueOf(startTime));
            cstmt.setTime(5, Time.valueOf(endTime));
            cstmt.setNull(6, Types.DATE);
            cstmt.setNull(7, Types.DATE);
            cstmt.setNull(8, Types.VARCHAR);
            if (excludeReservationId != null) cstmt.setInt(9, excludeReservationId); else cstmt.setNull(9, Types.INTEGER);
            cstmt.registerOutParameter(10, Types.BOOLEAN);
            cstmt.registerOutParameter(11, Types.INTEGER);
            cstmt.execute();
            return !cstmt.getBoolean(10);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking teacher availability for teacher ID: " + idEnseignant, e);
            return false;
        }
    }

    public boolean checkNiveauAvailability(Integer niveauId, boolean isRecurring, LocalDate date, LocalTime startTime, LocalTime endTime,
                                           LocalDate recurrenceStartDate, LocalDate recurrenceEndDate, String dayOfWeek,
                                           Integer groupNumber, int activityType, Integer excludeReservationId) {
        if (niveauId == null) return true;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement cstmt = conn.prepareCall("{CALL verifier_conflit_niveau(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cstmt.setInt(1, niveauId);
            cstmt.setBoolean(2, isRecurring);
            if (date != null) cstmt.setDate(3, Date.valueOf(date)); else cstmt.setNull(3, Types.DATE);
            cstmt.setTime(4, Time.valueOf(startTime));
            cstmt.setTime(5, Time.valueOf(endTime));
            if (recurrenceStartDate != null) cstmt.setDate(6, Date.valueOf(recurrenceStartDate)); else cstmt.setNull(6, Types.DATE);
            if (recurrenceEndDate != null) cstmt.setDate(7, Date.valueOf(recurrenceEndDate)); else cstmt.setNull(7, Types.DATE);
            if (dayOfWeek != null) cstmt.setString(8, dayOfWeek); else cstmt.setNull(8, Types.VARCHAR);
            if (groupNumber != null) cstmt.setInt(9, groupNumber); else cstmt.setNull(9, Types.INTEGER);
            cstmt.setInt(10, activityType);
            if (excludeReservationId != null) cstmt.setInt(11, excludeReservationId); else cstmt.setNull(11, Types.INTEGER);
            cstmt.registerOutParameter(12, Types.BOOLEAN);
            cstmt.registerOutParameter(13, Types.INTEGER);
            cstmt.execute();
            return !cstmt.getBoolean(12);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking niveau availability for niveau ID: " + niveauId, e);
            return false;
        }
    }

    private List<Reservation> fetchList(String sql, Object... params) {
        List<Reservation> result = new ArrayList<>();
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

    private int bindReservation(PreparedStatement stmt, Reservation r, int i) throws SQLException {
        stmt.setInt(i++, r.getIdSalle());
        stmt.setBoolean(i++, r.isOnline());
        UIUtils.setNullableInt(stmt, i++, r.getIdDepartement());
        UIUtils.setNullableInt(stmt, i++, r.getIdBloc());
        UIUtils.setNullableInt(stmt, i++, r.getIdNiveau());
        UIUtils.setNullableInt(stmt, i++, r.getGroupNumber());
        stmt.setInt(i++, r.getIdEnseignant());
        stmt.setInt(i++, r.getIdTypeActivite());
        if (r.isRecurring()) {
            stmt.setNull(i++, Types.DATE);
        } else {
            stmt.setDate(i++, Date.valueOf(r.getDateReservation()));
        }
        stmt.setTime(i++, Time.valueOf(r.getHeureDebut()));
        stmt.setTime(i++, Time.valueOf(r.getHeureFin()));
        stmt.setString(i++, r.getTitreActivite());
        stmt.setString(i++, r.getDescription());
        stmt.setBoolean(i++, r.isRecurring());
        i = setNullableDate(stmt, i, r.getDateDebutRecurrence());
        i = setNullableDate(stmt, i, r.getDateFinRecurrence());
        i = setNullableString(stmt, i, r.getDayOfWeek() != null ? r.getDayOfWeek().name() : null, Types.VARCHAR);
        return i;
    }

    private int setNullableDate(PreparedStatement stmt, int i, LocalDate value) throws SQLException {
        if (value != null) stmt.setDate(i, Date.valueOf(value)); else stmt.setNull(i, Types.DATE);
        return i + 1;
    }

    private int setNullableString(PreparedStatement stmt, int i, String value, int sqlType) throws SQLException {
        if (value != null) stmt.setString(i, value); else stmt.setNull(i, sqlType);
        return i + 1;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return ReservationMapper.mapFull(rs);
    }

    private void requireAuthenticated() {
        SessionContext.requireAuthenticated();
    }
}
