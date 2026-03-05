package com.gestion.salles.models;

/******************************************************************************
 * Conflict.java
 *
 * Immutable model representing a scheduling conflict between reservations.
 * ConflictType distinguishes exact duplicates from room, teacher, level, and
 * group conflicts, including mixed level/group scenarios. details is optional
 * and intended for UI display only.
 ******************************************************************************/

public class Conflict {

    public enum ConflictType {
        COMPLETE_DUPLICATE,
        ROOM_CONFLICT,
        TEACHER_CONFLICT,
        LEVEL_CONFLICT,
        GROUP_CONFLICT,
        LEVEL_VS_GROUP_CONFLICT,
        GROUP_VS_LEVEL_CONFLICT
    }

    private final ConflictType type;
    private final Reservation conflictingReservation;
    private final String message;
    private final String details;

    public Conflict(ConflictType type, Reservation conflictingReservation, String message, String details) {
        this.type = type;
        this.conflictingReservation = conflictingReservation;
        this.message = message;
        this.details = details;
    }

    public ConflictType getType() { return type; }
    public Reservation getConflictingReservation() { return conflictingReservation; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return "Conflict{type=" + type +
               ", message='" + message + '\'' +
               ", conflictingReservation=" + (conflictingReservation != null ? conflictingReservation.getIdReservation() : null) +
               '}';
    }
}
