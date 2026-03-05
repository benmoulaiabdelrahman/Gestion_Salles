package com.gestion.salles.dao;

/******************************************************************************
 * ScheduleDAO.java
 *
 * Data access layer for schedule views over the 'reservations' table. The
 * shared SELECT/JOIN is defined in BASE_SELECT. Date-range WHERE clauses are
 * appended by appendDateFilter(). Both public entry-point methods delegate to
 * fetchAndExpand() which executes the query, then calls expandReservations()
 * for post-processing. Recurring reservations are expanded into individual
 * ScheduleEntry occurrences within the requested date window. createScheduleEntry()
 * assembles the full object graph via the helper DAOs.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.models.ActivityType;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Niveau;
import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.Room;
import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.models.User;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduleDAO {

    private static final Logger LOGGER = Logger.getLogger(ScheduleDAO.class.getName());

    private static final String BASE_SELECT =
            "SELECT r.*, " +
            "       s.numero_salle, s.capacite, s.type_salle, s.equipements, s.etage, s.id_departement_principal, s.observations AS salle_observations, s.actif AS salle_active, " +
            "       b.nom_bloc, " +
            "       u.nom AS nom_enseignant, u.prenom AS prenom_enseignant, " +
            "       n.nom_niveau AS nom_niveau, " +
            "       d.nom_departement AS nom_departement, " +
            "       ta.id_type_activite, ta.nom_type, ta.couleur_hex, ta.is_group_specific " +
            "FROM reservations r " +
            "JOIN salles s ON r.id_salle = s.id_salle " +
            "LEFT JOIN utilisateurs u ON r.id_enseignant = u.id_utilisateur " +
            "LEFT JOIN departements d ON r.id_departement = d.id_departement " +
            "LEFT JOIN blocs b ON s.id_bloc = b.id_bloc " +
            "LEFT JOIN types_activites ta ON r.id_type_activite = ta.id_type_activite " +
            "LEFT JOIN niveaux n ON r.id_niveau = n.id_niveau " +
            "WHERE r.statut = 'CONFIRMEE' ";

    public List<ScheduleEntry> getScheduleEntries(LocalDate startDate, LocalDate endDate,
                                                  Integer roomId, Integer teacherId, Integer niveauId,
                                                  Integer departementId, Integer blocId,
                                                  boolean onlyRecurring) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();

        if (onlyRecurring) {
            sql.append("AND r.is_recurring = TRUE ");
            if (startDate != null && endDate != null) {
                sql.append("AND r.date_debut_recurrence <= ? AND r.date_fin_recurrence >= ? ");
                params.add(Date.valueOf(endDate));
                params.add(Date.valueOf(startDate));
            }
        } else {
            appendDateFilter(sql, params, startDate, endDate);
        }

        if (roomId != null)        { sql.append("AND r.id_salle = ? ");       params.add(roomId); }
        if (teacherId != null)     { sql.append("AND r.id_enseignant = ? ");   params.add(teacherId); }
        if (niveauId != null)      { sql.append("AND r.id_niveau = ? ");       params.add(niveauId); }
        if (departementId != null) { sql.append("AND r.id_departement = ? "); params.add(departementId); }
        if (blocId != null)        { sql.append("AND (s.id_bloc = ? OR r.is_online = TRUE) "); params.add(blocId); }

        sql.append("ORDER BY r.date_reservation, r.heure_debut");

        return fetchAndExpand(sql.toString(), params, startDate, endDate);
    }

    public List<ScheduleEntry> getScheduleEntriesForTeacher(Integer teacherId,
                                                            LocalDate startDate,
                                                            LocalDate endDate) throws SQLException {
        if (teacherId == null) {
            LOGGER.warning("Attempted to fetch teacher schedule with null teacherId.");
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();

        sql.append("AND r.id_enseignant = ? ");
        params.add(teacherId);

        appendDateFilter(sql, params, startDate, endDate);
        sql.append("ORDER BY r.date_reservation, r.heure_debut");

        return fetchAndExpand(sql.toString(), params, startDate, endDate);
    }

    public List<ScheduleEntry> getUpcomingScheduleEntriesForTeacher(Integer teacherId, int limit) throws SQLException {
        if (teacherId == null) {
            LOGGER.warning("Attempted to fetch upcoming teacher schedule with null teacherId.");
            return new ArrayList<>();
        }

        String sql = BASE_SELECT +
            "AND r.id_enseignant = ? " +
            "AND ((r.date_reservation > CURRENT_DATE()) " +
            "  OR (r.date_reservation = CURRENT_DATE() AND r.heure_fin >= CURRENT_TIME())) " +
            "ORDER BY r.date_reservation, r.heure_debut " +
            "LIMIT ?";

        List<ScheduleEntry> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            stmt.setInt(2, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapRow(rs);
                    result.add(toScheduleEntry(reservation, rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching upcoming teacher schedule", e);
            throw e;
        }

        return result;
    }

    private List<ScheduleEntry> fetchAndExpand(String sql, List<Object> params,
                                               LocalDate startDate, LocalDate endDate) throws SQLException {
        List<ScheduleContext> fetched = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = mapRow(rs);
                    fetched.add(new ScheduleContext(reservation, toScheduleEntry(reservation, rs)));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching reservations for schedule", e);
            throw e;
        }
        return expandReservations(fetched, startDate, endDate);
    }

    private List<ScheduleEntry> expandReservations(List<ScheduleContext> reservations,
                                                   LocalDate startDate, LocalDate endDate) throws SQLException {
        LocalDate windowStart = startDate != null ? startDate : LocalDate.MIN;
        LocalDate windowEnd   = endDate   != null ? endDate   : LocalDate.MAX;

        List<ScheduleEntry> result = new ArrayList<>();
        for (ScheduleContext ctx : reservations) {
            Reservation r = ctx.reservation;
            if (r.isRecurring()) {
                LocalDate recStart = r.getDateDebutRecurrence() != null ? r.getDateDebutRecurrence() : LocalDate.MIN;
                LocalDate recEnd   = r.getDateFinRecurrence()   != null ? r.getDateFinRecurrence()   : LocalDate.MAX;
                LocalDate loopStart = windowStart.isAfter(recStart) ? windowStart : recStart;
                LocalDate loopEnd   = windowEnd.isBefore(recEnd)    ? windowEnd   : recEnd;

                if (loopStart.isAfter(loopEnd)) continue;

                for (LocalDate day = loopStart; !day.isAfter(loopEnd); day = day.plusDays(1)) {
                    if (r.getDayOfWeek() != null && day.getDayOfWeek().getValue() == r.getDayOfWeek().ordinal() + 1) {
                        Reservation copy = new Reservation(r);
                        copy.setDateReservation(day);
                        result.add(new ScheduleEntry(copy, ctx.baseEntry.getRoom(), ctx.baseEntry.getTeacher(),
                                ctx.baseEntry.getNiveau(), ctx.baseEntry.getDepartement(), ctx.baseEntry.getActivityType()));
                    }
                }
            } else {
                LocalDate d = r.getDateReservation();
                if (d != null && !d.isBefore(windowStart) && !d.isAfter(windowEnd)) {
                    result.add(ctx.baseEntry);
                }
            }
        }
        return result;
    }

    private void appendDateFilter(StringBuilder sql, List<Object> params,
                                  LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            sql.append("AND ((r.is_recurring = FALSE AND r.date_reservation BETWEEN ? AND ?) " +
                       "  OR (r.is_recurring = TRUE  AND r.date_debut_recurrence <= ? AND r.date_fin_recurrence >= ?)) ");
            params.add(Date.valueOf(startDate));
            params.add(Date.valueOf(endDate));
            params.add(Date.valueOf(endDate));
            params.add(Date.valueOf(startDate));
        } else if (startDate != null) {
            sql.append("AND ((r.is_recurring = FALSE AND r.date_reservation >= ?) " +
                       "  OR (r.is_recurring = TRUE  AND r.date_fin_recurrence >= ?)) ");
            params.add(Date.valueOf(startDate));
            params.add(Date.valueOf(startDate));
        } else if (endDate != null) {
            sql.append("AND ((r.is_recurring = FALSE AND r.date_reservation <= ?) " +
                       "  OR (r.is_recurring = TRUE  AND r.date_debut_recurrence <= ?)) ");
            params.add(Date.valueOf(endDate));
            params.add(Date.valueOf(endDate));
        }
    }

    private ScheduleEntry toScheduleEntry(Reservation reservation, ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(reservation.getIdSalle());
        room.setName(rs.getString("numero_salle"));
        room.setIdBloc(rs.getObject("id_bloc", Integer.class) != null ? rs.getInt("id_bloc") : 0);
        room.setBlockName(rs.getString("nom_bloc"));
        room.setCapacity(rs.getInt("capacite"));
        room.setTypeSalle(rs.getString("type_salle"));
        room.setEquipment(rs.getString("equipements"));
        room.setEtage(rs.getInt("etage"));
        room.setIdDepartementPrincipal(rs.getObject("id_departement_principal", Integer.class));
        room.setDepartmentName(rs.getString("nom_departement"));
        room.setObservations(rs.getString("salle_observations"));
        room.setActif(rs.getBoolean("salle_active"));

        User teacher = new User();
        teacher.setIdUtilisateur(reservation.getIdEnseignant());
        teacher.setNom(reservation.getNomEnseignant());
        teacher.setPrenom(reservation.getPrenomEnseignant());

        Niveau niveau = null;
        if (reservation.getIdNiveau() != null) {
            niveau = new Niveau();
            niveau.setId(reservation.getIdNiveau());
            niveau.setNom(reservation.getNomNiveau());
            if (reservation.getIdDepartement() != null) {
                niveau.setIdDepartement(reservation.getIdDepartement());
            }
            niveau.setIdBloc(reservation.getIdBloc());
            niveau.setNomBloc(rs.getString("nom_bloc"));
            niveau.setActif(true);
        }

        Departement departement = null;
        if (reservation.getIdDepartement() != null) {
            departement = new Departement();
            departement.setId(reservation.getIdDepartement());
            departement.setNom(reservation.getNomDepartement());
            departement.setIdBloc(reservation.getIdBloc());
            departement.setBlocName(rs.getString("nom_bloc"));
            departement.setActif(true);
        }

        ActivityType activityType = new ActivityType();
        activityType.setId(rs.getInt("id_type_activite"));
        activityType.setName(rs.getString("nom_type"));
        activityType.setColorHex(rs.getString("couleur_hex"));
        activityType.setGroupSpecific(rs.getBoolean("is_group_specific"));

        return new ScheduleEntry(reservation, room, teacher, niveau, departement, activityType);
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return ReservationMapper.mapFull(rs);
    }

    private record ScheduleContext(Reservation reservation, ScheduleEntry baseEntry) {}
}
