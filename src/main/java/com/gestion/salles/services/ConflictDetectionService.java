package com.gestion.salles.services;

/******************************************************************************
 * ConflictDetectionService.java
 *
 * Pre-save conflict validation service for the Gestion des Salles application.
 * Detects room, teacher, level, and group scheduling conflicts for a given
 * reservation by delegating to three MySQL stored procedures via CallableStatement.
 * When all conflict types point to the same existing reservation, the result
 * is collapsed into a single COMPLETE_DUPLICATE conflict.
 ******************************************************************************/

import com.gestion.salles.dao.ReservationDAO;
import com.gestion.salles.models.Conflict;
import com.gestion.salles.models.Reservation;
import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.utils.UIUtils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConflictDetectionService {

    private static final Logger            LOGGER    = Logger.getLogger(ConflictDetectionService.class.getName());
    private static final DateTimeFormatter FMT_DATE  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TIME  = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationLookup reservationLookup;

    public ConflictDetectionService() {
        this(new ReservationDAOLookup(new ReservationDAO()));
    }

    ConflictDetectionService(ReservationLookup reservationLookup) {
        this.reservationLookup = reservationLookup;
    }

    public List<Conflict> checkConflicts(Reservation newReservation, Integer excludeReservationId) {
        List<Conflict> conflicts = new ArrayList<>();

        if (newReservation.isOnline()) {
            Conflict onlineDuplicate = findOnlineDuplicate(newReservation, excludeReservationId);
            if (onlineDuplicate != null) {
                conflicts.add(onlineDuplicate);
                return conflicts;
            }
        }

        checkRoomConflict(newReservation, excludeReservationId, conflicts);
        checkTeacherConflict(newReservation, excludeReservationId, conflicts);
        checkLevelGroupConflicts(newReservation, excludeReservationId, conflicts);

        if (!conflicts.isEmpty() && isCompleteDuplicate(newReservation, conflicts)) {
            List<Conflict> result = new ArrayList<>();
            result.add(createCompleteDuplicateConflict(newReservation));
            return result;
        }

        return conflicts;
    }

    private boolean isCompleteDuplicate(Reservation newReservation, List<Conflict> conflicts) {
        if (conflicts.isEmpty()) return false;

        int firstId = conflicts.get(0).getConflictingReservation().getIdReservation();
        for (Conflict conflict : conflicts) {
            if (conflict.getConflictingReservation() == null ||
                conflict.getConflictingReservation().getIdReservation() != firstId) {
                return false;
            }
        }

        boolean hasRoom    = false;
        boolean hasTeacher = false;
        boolean hasLevel   = false;

        for (Conflict conflict : conflicts) {
            switch (conflict.getType()) {
                case ROOM_CONFLICT    -> hasRoom    = true;
                case TEACHER_CONFLICT -> hasTeacher = true;
                case LEVEL_CONFLICT, GROUP_CONFLICT,
                     LEVEL_VS_GROUP_CONFLICT, GROUP_VS_LEVEL_CONFLICT -> hasLevel = true;
            }
        }
        return hasRoom && hasTeacher && hasLevel;
    }

    private Conflict createCompleteDuplicateConflict(Reservation r) {
        String details = String.format("Détails: Salle %s, Enseignant %s, Niveau %s%s%nHoraire: %s de %s à %s",
            r.getNomSalle(),
            formatTeacherName(r),
            r.getNomNiveau(),
            r.getGroupNumber() != null ? ", Groupe " + r.getGroupNumber() : "",
            r.getDateReservation() != null ? r.getDateReservation().format(FMT_DATE) : formatRecurrenceDates(r),
            r.getHeureDebut().format(FMT_TIME),
            r.getHeureFin().format(FMT_TIME));
        return new Conflict(Conflict.ConflictType.COMPLETE_DUPLICATE, null, "Cette réservation existe déjà !", details);
    }

    private Conflict createCompleteDuplicateConflictFromExisting(Reservation existing) {
        String details = String.format("Détails: Salle %s, Enseignant %s, Niveau %s%s%nHoraire: %s de %s à %s",
            existing.getNomSalle(),
            formatTeacherName(existing),
            existing.getNomNiveau(),
            existing.getGroupNumber() != null ? ", Groupe " + existing.getGroupNumber() : "",
            existing.getDateReservation() != null ? existing.getDateReservation().format(FMT_DATE) : formatRecurrenceDates(existing),
            existing.getHeureDebut().format(FMT_TIME),
            existing.getHeureFin().format(FMT_TIME));
        return new Conflict(Conflict.ConflictType.COMPLETE_DUPLICATE, existing, "Cette réservation existe déjà !", details);
    }

    private Conflict findOnlineDuplicate(Reservation r, Integer excludeId) {
        String base =
            "SELECT id_reservation " +
            "FROM reservations " +
            "WHERE is_online = 1 " +
            "  AND id_enseignant = ? " +
            "  AND id_niveau <=> ? " +
            "  AND group_number <=> ? " +
            "  AND statut IN ('CONFIRMEE','EN_ATTENTE') ";

        String byDate =
            "  AND is_recurring = 0 " +
            "  AND date_reservation = ? " +
            "  AND heure_debut = ? " +
            "  AND heure_fin = ? ";

        String byRecurrence =
            "  AND is_recurring = 1 " +
            "  AND day_of_week = ? " +
            "  AND date_debut_recurrence = ? " +
            "  AND date_fin_recurrence = ? " +
            "  AND heure_debut = ? " +
            "  AND heure_fin = ? ";

        String exclude = excludeId != null ? " AND id_reservation <> ? " : "";
        String sql = base + (r.isRecurring() ? byRecurrence : byDate) + exclude + " LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            stmt.setInt(i++, r.getIdEnseignant());
            if (r.getIdNiveau() != null) {
                stmt.setInt(i++, r.getIdNiveau());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }
            if (r.getGroupNumber() != null) {
                stmt.setInt(i++, r.getGroupNumber());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }

            if (r.isRecurring()) {
                stmt.setString(i++, r.getDayOfWeek() != null ? r.getDayOfWeek().name() : null);
                if (r.getDateDebutRecurrence() != null) {
                    stmt.setDate(i++, java.sql.Date.valueOf(r.getDateDebutRecurrence()));
                } else {
                    stmt.setNull(i++, Types.DATE);
                }
                if (r.getDateFinRecurrence() != null) {
                    stmt.setDate(i++, java.sql.Date.valueOf(r.getDateFinRecurrence()));
                } else {
                    stmt.setNull(i++, Types.DATE);
                }
                stmt.setTime(i++, Time.valueOf(r.getHeureDebut()));
                stmt.setTime(i++, Time.valueOf(r.getHeureFin()));
            } else {
                if (r.getDateReservation() != null) {
                    stmt.setDate(i++, java.sql.Date.valueOf(r.getDateReservation()));
                } else {
                    stmt.setNull(i++, Types.DATE);
                }
                stmt.setTime(i++, Time.valueOf(r.getHeureDebut()));
                stmt.setTime(i++, Time.valueOf(r.getHeureFin()));
            }

            if (excludeId != null) {
                stmt.setInt(i, excludeId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id_reservation");
                    Reservation existing = reservationLookup.getReservationById(existingId);
                    if (existing != null) {
                        return createCompleteDuplicateConflictFromExisting(existing);
                    }
                    return createCompleteDuplicateConflict(r);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking online duplicate reservation", e);
        }
        return null;
    }

    private void checkRoomConflict(Reservation r, Integer excludeId, List<Conflict> conflicts) {
        try {
            CallableStatementResult res = callRoomConflictProcedure(
                r.getIdSalle(), r.isRecurring(), r.getDateReservation(),
                r.getHeureDebut(), r.getHeureFin(),
                r.getDateDebutRecurrence(), r.getDateFinRecurrence(),
                r.getDayOfWeek() != null ? r.getDayOfWeek().name() : null,
                excludeId, r.isOnline());

            if (res.conflitExiste) {
                Reservation c = reservationLookup.getReservationById(res.idReservationConflit);
                if (c != null) {
                    String details = String.format(
                        "La salle %s est occupée par :%nEnseignant: %s%nNiveau: %s%nGroupe: %s%nHoraire: %s de %s à %s",
                        c.getNomSalle(),
                        formatTeacherName(c),
                        c.getNomNiveau(),
                        c.getGroupNumber() != null ? c.getGroupNumber() : "Tous les groupes",
                        c.getDateReservation() != null ? c.getDateReservation().format(FMT_DATE) : formatRecurrenceDates(c),
                        c.getHeureDebut().format(FMT_TIME),
                        c.getHeureFin().format(FMT_TIME));
                    conflicts.add(new Conflict(Conflict.ConflictType.ROOM_CONFLICT, c, "Cette salle est déjà réservée !", details));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking room conflict for reservation: " +
                (r.getIdReservation() > 0 ? r.getIdReservation() : "new"), e);
        }
    }

    private void checkTeacherConflict(Reservation r, Integer excludeId, List<Conflict> conflicts) {
        try {
            CallableStatementResult res = callTeacherConflictProcedure(
                r.getIdEnseignant(), r.isRecurring(), r.getDateReservation(),
                r.getHeureDebut(), r.getHeureFin(),
                r.getDateDebutRecurrence(), r.getDateFinRecurrence(),
                r.getDayOfWeek() != null ? r.getDayOfWeek().name() : null,
                excludeId);

            if (res.conflitExiste) {
                Reservation c = reservationLookup.getReservationById(res.idReservationConflit);
                if (c != null) {
                    String details = String.format(
                        "%s a déjà une séance programmée :%nSalle: %s%nNiveau: %s%nGroupe: %s%nHoraire: %s de %s à %s",
                        formatTeacherName(c),
                        c.getNomSalle(),
                        c.getNomNiveau(),
                        c.getGroupNumber() != null ? c.getGroupNumber() : "Tous les groupes",
                        c.getDateReservation() != null ? c.getDateReservation().format(FMT_DATE) : formatRecurrenceDates(c),
                        c.getHeureDebut().format(FMT_TIME),
                        c.getHeureFin().format(FMT_TIME));
                    conflicts.add(new Conflict(Conflict.ConflictType.TEACHER_CONFLICT, c, "Conflit d'emploi du temps pour l'enseignant !", details));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking teacher conflict for reservation: " +
                (r.getIdReservation() > 0 ? r.getIdReservation() : "new"), e);
        }
    }

    private void checkLevelGroupConflicts(Reservation r, Integer excludeId, List<Conflict> conflicts) {
        if (r.getIdNiveau() == null) return;

        try {
            Integer newGroup = r.getGroupNumber();
            CallableStatementResult res = callNiveauConflictProcedure(
                r.getIdNiveau(), r.isRecurring(), r.getDateReservation(),
                r.getHeureDebut(), r.getHeureFin(),
                r.getDateDebutRecurrence(), r.getDateFinRecurrence(),
                r.getDayOfWeek() != null ? r.getDayOfWeek().name() : null,
                newGroup, r.getIdTypeActivite(), excludeId);

            if (res.conflitExiste) {
                Reservation c = reservationLookup.getReservationById(res.idReservationConflit);
                if (c != null) {
                    String schedule = String.format("%s de %s à %s",
                        c.getDateReservation() != null ? c.getDateReservation().format(FMT_DATE) : formatRecurrenceDates(c),
                        c.getHeureDebut().format(FMT_TIME),
                        c.getHeureFin().format(FMT_TIME));

                    if (newGroup == null && c.getGroupNumber() != null) {
                        String details = String.format(
                            "Le niveau %s est déjà réservé (tous les groupes) :%nEnseignant: %s%nSalle: %s%nHoraire: %s%n%nVous ne pouvez pas réserver le niveau complet car le groupe %s est déjà pris.",
                            c.getNomNiveau(),
                            formatTeacherName(c),
                            c.getNomSalle(), schedule, c.getGroupNumber());
                        conflicts.add(new Conflict(Conflict.ConflictType.LEVEL_VS_GROUP_CONFLICT, c, "Conflit avec une réservation du niveau complet !", details));

                    } else if (newGroup != null && c.getGroupNumber() == null) {
                        String details = String.format(
                            "Le niveau %s est déjà réservé (tous les groupes) :%nEnseignant: %s%nSalle: %s%nHoraire: %s%n%nVous ne pouvez pas réserver le groupe %s pendant ce temps.",
                            c.getNomNiveau(),
                            formatTeacherName(c),
                            c.getNomSalle(), schedule, newGroup);
                        conflicts.add(new Conflict(Conflict.ConflictType.GROUP_VS_LEVEL_CONFLICT, c, "Conflit avec une réservation du niveau complet !", details));

                    } else if (newGroup != null) {
                        String details = String.format(
                            "Le groupe %s du niveau %s a déjà une séance :%nEnseignant: %s%nSalle: %s%nHoraire: %s",
                            c.getGroupNumber(), c.getNomNiveau(),
                            formatTeacherName(c),
                            c.getNomSalle(), schedule);
                        conflicts.add(new Conflict(Conflict.ConflictType.GROUP_CONFLICT, c, "Ce groupe a déjà une séance programmée !", details));

                    } else {
                        String details = String.format(
                            "Le niveau %s a déjà une séance :%nEnseignant: %s%nSalle: %s%nGroupe: %s%nHoraire: %s",
                            c.getNomNiveau(),
                            formatTeacherName(c),
                            c.getNomSalle(),
                            c.getGroupNumber() != null ? c.getGroupNumber() : "Tous les groupes",
                            schedule);
                        conflicts.add(new Conflict(Conflict.ConflictType.LEVEL_CONFLICT, c, "Ce niveau a déjà un cours programmé !", details));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking level/group conflict for reservation: " +
                (r.getIdReservation() > 0 ? r.getIdReservation() : "new"), e);
        }
    }

    private String formatRecurrenceDates(Reservation r) {
        if (r.isRecurring() && r.getDateDebutRecurrence() != null
                && r.getDateFinRecurrence() != null && r.getDayOfWeek() != null) {
            return String.format("du %s au %s (%s)",
                r.getDateDebutRecurrence().format(FMT_DATE),
                r.getDateFinRecurrence().format(FMT_DATE),
                r.getDayOfWeek().getDisplayName());
        }
        return "Date non spécifiée";
    }

    private String formatTeacherName(Reservation r) {
        String nom = r.getNomEnseignant();
        String prenom = r.getPrenomEnseignant();
        String safeNom = nom == null ? "" : nom.trim();
        String safePrenom = prenom == null ? "" : prenom.trim();

        if (!safePrenom.isEmpty()) {
            String nomLower = safeNom.toLowerCase();
            String prenomLower = safePrenom.toLowerCase();
            if (nomLower.contains(prenomLower)) {
                return safeNom;
            }
            if (!safeNom.isEmpty()) {
                return safePrenom + " " + safeNom;
            }
            return safePrenom;
        }
        return safeNom.isEmpty() ? "N/A" : safeNom;
    }

    protected static class CallableStatementResult {
        boolean conflitExiste;
        int     idReservationConflit;
    }

    protected CallableStatementResult callRoomConflictProcedure(
            int roomId, boolean isRecurring, LocalDate date, LocalTime startTime, LocalTime endTime,
            LocalDate recurrenceStart, LocalDate recurrenceEnd, String dayOfWeek,
            Integer excludeId, boolean isOnline) throws SQLException {

        CallableStatementResult result = new CallableStatementResult();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall("{CALL verifier_conflit_salle(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, roomId);
            cs.setBoolean(2, isRecurring);
            if (date != null) cs.setDate(3, java.sql.Date.valueOf(date)); else cs.setNull(3, Types.DATE);
            cs.setTime(4, Time.valueOf(startTime));
            cs.setTime(5, Time.valueOf(endTime));
            if (recurrenceStart != null) cs.setDate(6, java.sql.Date.valueOf(recurrenceStart)); else cs.setNull(6, Types.DATE);
            if (recurrenceEnd   != null) cs.setDate(7, java.sql.Date.valueOf(recurrenceEnd));   else cs.setNull(7, Types.DATE);
            if (dayOfWeek != null) cs.setString(8, dayOfWeek); else cs.setNull(8, Types.VARCHAR);
            UIUtils.setNullableInt(cs, 9, excludeId);
            cs.setBoolean(10, isOnline);
            cs.registerOutParameter(11, Types.BOOLEAN);
            cs.registerOutParameter(12, Types.INTEGER);
            cs.execute();

            result.conflitExiste        = cs.getBoolean(11);
            result.idReservationConflit = cs.getInt(12);
        }
        return result;
    }

    protected CallableStatementResult callTeacherConflictProcedure(
            int idEnseignant, boolean isRecurring, LocalDate date, LocalTime startTime, LocalTime endTime,
            LocalDate recurrenceStart, LocalDate recurrenceEnd, String dayOfWeek,
            Integer excludeId) throws SQLException {

        CallableStatementResult result = new CallableStatementResult();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall("{CALL verifier_conflit_enseignant(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, idEnseignant);
            cs.setBoolean(2, isRecurring);
            if (date != null) cs.setDate(3, java.sql.Date.valueOf(date)); else cs.setNull(3, Types.DATE);
            cs.setTime(4, Time.valueOf(startTime));
            cs.setTime(5, Time.valueOf(endTime));
            if (recurrenceStart != null) cs.setDate(6, java.sql.Date.valueOf(recurrenceStart)); else cs.setNull(6, Types.DATE);
            if (recurrenceEnd   != null) cs.setDate(7, java.sql.Date.valueOf(recurrenceEnd));   else cs.setNull(7, Types.DATE);
            if (dayOfWeek != null) cs.setString(8, dayOfWeek); else cs.setNull(8, Types.VARCHAR);
            UIUtils.setNullableInt(cs, 9, excludeId);
            cs.registerOutParameter(10, Types.BOOLEAN);
            cs.registerOutParameter(11, Types.INTEGER);
            cs.execute();

            result.conflitExiste        = cs.getBoolean(10);
            result.idReservationConflit = cs.getInt(11);
        }
        return result;
    }

    protected CallableStatementResult callNiveauConflictProcedure(
            int idNiveau, boolean isRecurring, LocalDate date, LocalTime startTime, LocalTime endTime,
            LocalDate recurrenceStart, LocalDate recurrenceEnd, String dayOfWeek,
            Integer groupNumber, int activityType, Integer excludeId) throws SQLException {

        CallableStatementResult result = new CallableStatementResult();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall("{CALL verifier_conflit_niveau(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, idNiveau);
            cs.setBoolean(2, isRecurring);
            if (date != null) cs.setDate(3, java.sql.Date.valueOf(date)); else cs.setNull(3, Types.DATE);
            cs.setTime(4, Time.valueOf(startTime));
            cs.setTime(5, Time.valueOf(endTime));
            if (recurrenceStart != null) cs.setDate(6, java.sql.Date.valueOf(recurrenceStart)); else cs.setNull(6, Types.DATE);
            if (recurrenceEnd   != null) cs.setDate(7, java.sql.Date.valueOf(recurrenceEnd));   else cs.setNull(7, Types.DATE);
            if (dayOfWeek != null) cs.setString(8, dayOfWeek); else cs.setNull(8, Types.VARCHAR);
            UIUtils.setNullableInt(cs, 9, groupNumber);
            cs.setInt(10, activityType);
            UIUtils.setNullableInt(cs, 11, excludeId);
            cs.registerOutParameter(12, Types.BOOLEAN);
            cs.registerOutParameter(13, Types.INTEGER);
            cs.execute();

            result.conflitExiste        = cs.getBoolean(12);
            result.idReservationConflit = cs.getInt(13);
        }
        return result;
    }

    interface ReservationLookup {
        Reservation getReservationById(int id);
    }

    private static final class ReservationDAOLookup implements ReservationLookup {
        private final ReservationDAO reservationDAO;

        private ReservationDAOLookup(ReservationDAO reservationDAO) {
            this.reservationDAO = reservationDAO;
        }

        @Override
        public Reservation getReservationById(int id) {
            return reservationDAO.getReservationById(id);
        }
    }
}
