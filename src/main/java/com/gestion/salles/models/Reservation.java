package com.gestion.salles.models;

/******************************************************************************
 * Reservation.java
 *
 * Model for the 'reservations' table. Nullable Integer FK fields (idDepartement,
 * idBloc, idNiveau, groupNumber, capacity) reflect optional associations.
 * Fields from nomSalle onward are denormalised display fields populated by
 * join queries; isPastReservation is a transient UI flag. The copy constructor
 * performs a shallow clone of all fields. Equality and hashing are based
 * solely on idReservation.
 ******************************************************************************/

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Reservation {

    public enum ReservationStatus {
        CONFIRMEE,
        EN_ATTENTE,
        ANNULEE;

        public String getDisplayName() {
            switch (this) {
                case CONFIRMEE:  return "Confirmée";
                case EN_ATTENTE: return "En attente";
                case ANNULEE:    return "Annulée";
                default:         return this.name();
            }
        }
    }

    public enum DayOfWeek {
        MONDAY("Lundi"),
        TUESDAY("Mardi"),
        WEDNESDAY("Mercredi"),
        THURSDAY("Jeudi"),
        FRIDAY("Vendredi"),
        SATURDAY("Samedi"),
        SUNDAY("Dimanche");

        private final String displayName;

        DayOfWeek(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }

        public static DayOfWeek fromDisplayName(String displayName) {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day.getDisplayName().equalsIgnoreCase(displayName)) return day;
            }
            throw new IllegalArgumentException("No matching DayOfWeek for display name: " + displayName);
        }

        public static DayOfWeek fromString(String name) {
            return DayOfWeek.valueOf(name.toUpperCase());
        }
    }

    // Persistent fields
    private int idReservation;
    private int idSalle;
    private Integer idDepartement;
    private Integer idBloc;
    private Integer idNiveau;
    private int idEnseignant;
    private int idTypeActivite;
    private LocalDate dateReservation;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String titreActivite;
    private String description;
    private Integer groupNumber;
    private boolean isRecurring;
    private LocalDate dateDebutRecurrence;
    private LocalDate dateFinRecurrence;
    private DayOfWeek dayOfWeek;
    private boolean isOnline;
    private ReservationStatus statut;
    private int idUtilisateurCreation;
    private Timestamp dateCreation;
    private Timestamp dateModification;
    private String observations;

    // Denormalised display fields
    private String nomSalle;
    private String nomEnseignant;
    private String prenomEnseignant;
    private String nomDepartement;
    private String nomNiveau;
    private String nomTypeActivite;
    private String couleurHexActivite;
    private Integer capacity;
    private String nomBloc;
    private transient boolean isPastReservation;

    public Reservation() {}

    public Reservation(Reservation other) {
        this.idReservation        = other.idReservation;
        this.idSalle              = other.idSalle;
        this.idDepartement        = other.idDepartement;
        this.idBloc               = other.idBloc;
        this.idNiveau             = other.idNiveau;
        this.idEnseignant         = other.idEnseignant;
        this.idTypeActivite       = other.idTypeActivite;
        this.dateReservation      = other.dateReservation;
        this.heureDebut           = other.heureDebut;
        this.heureFin             = other.heureFin;
        this.titreActivite        = other.titreActivite;
        this.description          = other.description;
        this.groupNumber          = other.groupNumber;
        this.isRecurring          = other.isRecurring;
        this.dateDebutRecurrence  = other.dateDebutRecurrence;
        this.dateFinRecurrence    = other.dateFinRecurrence;
        this.dayOfWeek            = other.dayOfWeek;
        this.isOnline             = other.isOnline;
        this.statut               = other.statut;
        this.idUtilisateurCreation = other.idUtilisateurCreation;
        this.dateCreation         = other.dateCreation;
        this.dateModification     = other.dateModification;
        this.observations         = other.observations;
        this.nomSalle             = other.nomSalle;
        this.nomEnseignant        = other.nomEnseignant;
        this.prenomEnseignant     = other.prenomEnseignant;
        this.nomDepartement       = other.nomDepartement;
        this.nomNiveau            = other.nomNiveau;
        this.nomTypeActivite      = other.nomTypeActivite;
        this.couleurHexActivite   = other.couleurHexActivite;
        this.capacity             = other.capacity;
        this.nomBloc              = other.nomBloc;
        this.isPastReservation    = other.isPastReservation;
    }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public int getIdSalle() { return idSalle; }
    public void setIdSalle(int idSalle) { this.idSalle = idSalle; }

    public Integer getIdDepartement() { return idDepartement; }
    public void setIdDepartement(Integer idDepartement) { this.idDepartement = idDepartement; }

    public Integer getIdBloc() { return idBloc; }
    public void setIdBloc(Integer idBloc) { this.idBloc = idBloc; }

    public Integer getIdNiveau() { return idNiveau; }
    public void setIdNiveau(Integer idNiveau) { this.idNiveau = idNiveau; }

    public int getIdEnseignant() { return idEnseignant; }
    public void setIdEnseignant(int idEnseignant) { this.idEnseignant = idEnseignant; }

    public int getIdTypeActivite() { return idTypeActivite; }
    public void setIdTypeActivite(int idTypeActivite) { this.idTypeActivite = idTypeActivite; }

    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getTitreActivite() { return titreActivite; }
    public void setTitreActivite(String titreActivite) { this.titreActivite = titreActivite; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getGroupNumber() { return groupNumber; }
    public void setGroupNumber(Integer groupNumber) { this.groupNumber = groupNumber; }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    public LocalDate getDateDebutRecurrence() { return dateDebutRecurrence; }
    public void setDateDebutRecurrence(LocalDate dateDebutRecurrence) { this.dateDebutRecurrence = dateDebutRecurrence; }

    public LocalDate getDateFinRecurrence() { return dateFinRecurrence; }
    public void setDateFinRecurrence(LocalDate dateFinRecurrence) { this.dateFinRecurrence = dateFinRecurrence; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public ReservationStatus getStatut() { return statut; }
    public void setStatut(ReservationStatus statut) { this.statut = statut; }

    public int getIdUtilisateurCreation() { return idUtilisateurCreation; }
    public void setIdUtilisateurCreation(int idUtilisateurCreation) { this.idUtilisateurCreation = idUtilisateurCreation; }

    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public Timestamp getDateModification() { return dateModification; }
    public void setDateModification(Timestamp dateModification) { this.dateModification = dateModification; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getNomSalle() { return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }

    public String getNomEnseignant() { return nomEnseignant; }
    public void setNomEnseignant(String nomEnseignant) { this.nomEnseignant = nomEnseignant; }

    public String getPrenomEnseignant() { return prenomEnseignant; }
    public void setPrenomEnseignant(String prenomEnseignant) { this.prenomEnseignant = prenomEnseignant; }

    public String getNomDepartement() { return nomDepartement; }
    public void setNomDepartement(String nomDepartement) { this.nomDepartement = nomDepartement; }

    public String getNomNiveau() { return nomNiveau; }
    public void setNomNiveau(String nomNiveau) { this.nomNiveau = nomNiveau; }

    public String getNomTypeActivite() { return nomTypeActivite; }
    public void setNomTypeActivite(String nomTypeActivite) { this.nomTypeActivite = nomTypeActivite; }

    public String getCouleurHexActivite() { return couleurHexActivite; }
    public void setCouleurHexActivite(String couleurHexActivite) { this.couleurHexActivite = couleurHexActivite; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getNomBloc() { return nomBloc; }
    public void setNomBloc(String nomBloc) { this.nomBloc = nomBloc; }

    public boolean isPastReservation() { return isPastReservation; }
    public void setPastReservation(boolean pastReservation) { isPastReservation = pastReservation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idReservation == ((Reservation) o).idReservation;
    }

    @Override
    public int hashCode() { return Objects.hash(idReservation); }

    @Override
    public String toString() {
        return "Reservation{" +
               "idReservation=" + idReservation +
               ", titreActivite='" + titreActivite + '\'' +
               ", dateReservation=" + dateReservation +
               ", heureDebut=" + heureDebut +
               ", heureFin=" + heureFin +
               ", nomSalle='" + nomSalle + '\'' +
               '}';
    }
}
