package com.gestion.salles.models;

/******************************************************************************
 * ActivityLog.java
 *
 * Model for the 'activity_logs' table. Tracks create/update/delete actions
 * performed on system entities. actingUser is a transient field populated
 * by the DAO JOIN and is not persisted directly.
 ******************************************************************************/

import java.sql.Timestamp;

public class ActivityLog {

    public enum ActionType { CREATE, UPDATE, DELETE }
    public enum EntityType { USER, ROOM, RESERVATION, FACULTE, DEPARTEMENT, NIVEAU }

    private int idLog;
    private Timestamp timestamp;
    private ActionType actionType;
    private EntityType entityType;
    private int entityId;
    private Integer idUserActing;
    private Integer idBloc; // New field for bloc ID
    private String details;
    private User actingUser;

    public ActivityLog() {}

    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }

    public Integer getIdUserActing() { return idUserActing; }
    public void setIdUserActing(Integer idUserActing) { this.idUserActing = idUserActing; }

    // New getter and setter for idBloc
    public Integer getIdBloc() { return idBloc; }
    public void setIdBloc(Integer idBloc) { this.idBloc = idBloc; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public User getActingUser() { return actingUser; }
    public void setActingUser(User actingUser) { this.actingUser = actingUser; }
}
