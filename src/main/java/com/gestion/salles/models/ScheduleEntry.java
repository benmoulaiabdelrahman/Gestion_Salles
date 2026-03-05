package com.gestion.salles.models;

/******************************************************************************
 * ScheduleEntry.java
 *
 * Read-only view model that aggregates a Reservation with its resolved
 * associations. All fields are set once via the constructor; convenience
 * getters delegate to the wrapped objects and return "N/A" when optional
 * associations are null.
 ******************************************************************************/

import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleEntry {

    private final Reservation reservation;
    private final Room room;
    private final User teacher;
    private final Niveau niveau;
    private final Departement departement;
    private final ActivityType activityType;

    public ScheduleEntry(Reservation reservation, Room room, User teacher,
                         Niveau niveau, Departement departement, ActivityType activityType) {
        this.reservation  = reservation;
        this.room         = room;
        this.teacher      = teacher;
        this.niveau       = niveau;
        this.departement  = departement;
        this.activityType = activityType;
    }

    public Reservation  getReservation()  { return reservation; }
    public Room         getRoom()         { return room; }
    public User         getTeacher()      { return teacher; }
    public Niveau       getNiveau()       { return niveau; }
    public Departement  getDepartement()  { return departement; }
    public ActivityType getActivityType() { return activityType; }

    // Convenience getters delegating to Reservation
    public int                          getIdReservation()   { return reservation.getIdReservation(); }
    public LocalDate                    getDateReservation() { return reservation.getDateReservation(); }
    public LocalTime                    getHeureDebut()      { return reservation.getHeureDebut(); }
    public LocalTime                    getHeureFin()        { return reservation.getHeureFin(); }
    public String                       getTitreActivite()   { return reservation.getTitreActivite(); }
    public Reservation.ReservationStatus getStatut()         { return reservation.getStatut(); }
    public Integer                      getGroupNumber()     { return reservation.getGroupNumber(); }
    public boolean                      isRecurring()        { return reservation.isRecurring(); }

    // Convenience getters delegating to nullable associations
    public String getRoomName()       { return room        != null ? room.getName()        : "N/A"; }
    public String getTeacherFullName(){ return teacher     != null ? teacher.getFullName() : "N/A"; }
    public String getNiveauNom()      { return niveau      != null ? niveau.getNom()       : "N/A"; }
    public String getDepartementNom() { return departement != null ? departement.getNom()  : "N/A"; }

    public boolean isGroupSpecific() {
        Integer g = reservation.getGroupNumber();
        return g != null && g > 0;
    }
}
