package com.gestion.salles.dao;

import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.Reservation.ReservationStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Date;

public final class ReservationMapper {

    public static Reservation mapFull(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        // Core reservation fields
        r.setIdReservation(rs.getInt("id_reservation"));
        r.setIdSalle(rs.getInt("id_salle"));
        r.setIdDepartement(rs.getObject("id_departement", Integer.class));
        r.setIdBloc(rs.getObject("id_bloc", Integer.class));
        r.setIdNiveau(rs.getObject("id_niveau", Integer.class));
        r.setGroupNumber(rs.getObject("group_number", Integer.class));
        r.setIdEnseignant(rs.getInt("id_enseignant"));
        r.setIdTypeActivite(rs.getInt("id_type_activite"));
        r.setDateReservation(mapDate(rs, "date_reservation"));
        r.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        r.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        r.setTitreActivite(rs.getString("titre_activite"));
        r.setDescription(rs.getString("description"));
        r.setOnline(rs.getBoolean("is_online"));
        r.setRecurring(rs.getBoolean("is_recurring"));
        r.setDateDebutRecurrence(mapDate(rs, "date_debut_recurrence"));
        r.setDateFinRecurrence(mapDate(rs, "date_fin_recurrence"));
        r.setDayOfWeek(mapDayOfWeek(rs, "day_of_week"));
        r.setStatut(ReservationStatus.valueOf(rs.getString("statut")));
        r.setIdUtilisateurCreation(rs.getInt("id_utilisateur_creation"));
        r.setDateCreation(rs.getTimestamp("date_creation"));
        r.setDateModification(rs.getTimestamp("date_modification"));
        r.setObservations(rs.getString("observations"));

        // Joined fields (from ReservationDAO's BASE_SELECT and ScheduleDAO's BASE_SELECT)
        // These fields might not always be present depending on the SELECT statement used
        try { r.setNomSalle(rs.getString("numero_salle")); } catch (SQLException e) { /* ignore if column not found */ }
        try { r.setNomDepartement(rs.getString("nom_departement")); } catch (SQLException e) { /* ignore if column not found */ }
        try { r.setNomBloc(rs.getString("nom_bloc")); } catch (SQLException e) { /* ignore if column not found */ }
        try { r.setNomEnseignant(rs.getString("nom_enseignant")); } catch (SQLException e) { /* ignore if column not found */ }
        try { r.setPrenomEnseignant(rs.getString("prenom_enseignant")); } catch (SQLException e) { /* ignore if column not found */ }
        try { r.setNomNiveau(rs.getString("nom_niveau")); } catch (SQLException e) { /* ignore if column not found */ }
        // ScheduleDAO also selects 'nom_type', 'couleur_hex', 'is_group_specific' from types_activites
        // These are not directly mapped to Reservation model's fields, so they are not included here.

        return r;
    }

    public static Reservation mapBasic(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setIdReservation(rs.getInt("id_reservation"));
        r.setIdSalle(rs.getInt("id_salle"));
        r.setIdDepartement(rs.getObject("id_departement", Integer.class));
        r.setIdBloc(rs.getObject("id_bloc", Integer.class));
        r.setIdNiveau(rs.getObject("id_niveau", Integer.class));
        r.setGroupNumber(rs.getObject("group_number", Integer.class));
        r.setIdEnseignant(rs.getInt("id_enseignant"));
        r.setIdTypeActivite(rs.getInt("id_type_activite"));
        r.setDateReservation(mapDate(rs, "date_reservation"));
        r.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        r.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        r.setTitreActivite(rs.getString("titre_activite"));
        r.setDescription(rs.getString("description"));
        r.setOnline(rs.getBoolean("is_online"));
        r.setRecurring(rs.getBoolean("is_recurring"));
        r.setDateDebutRecurrence(mapDate(rs, "date_debut_recurrence"));
        r.setDateFinRecurrence(mapDate(rs, "date_fin_recurrence"));
        r.setDayOfWeek(mapDayOfWeek(rs, "day_of_week"));
        r.setStatut(ReservationStatus.valueOf(rs.getString("statut")));
        r.setIdUtilisateurCreation(rs.getInt("id_utilisateur_creation"));
        r.setDateCreation(rs.getTimestamp("date_creation"));
        r.setDateModification(rs.getTimestamp("date_modification"));
        r.setObservations(rs.getString("observations"));
        return r;
    }

    private static LocalDate mapDate(ResultSet rs, String columnLabel) throws SQLException {
        Date sqlDate = rs.getDate(columnLabel);
        return sqlDate != null ? sqlDate.toLocalDate() : null;
    }

    private static Reservation.DayOfWeek mapDayOfWeek(ResultSet rs, String columnLabel) throws SQLException {
        String dayOfWeekStr = rs.getString(columnLabel);
        return dayOfWeekStr != null ? Reservation.DayOfWeek.valueOf(dayOfWeekStr) : null;
    }
}
