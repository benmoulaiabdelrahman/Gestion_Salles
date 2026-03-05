package com.gestion.salles.models;

/******************************************************************************
 * Room.java
 *
 * Model for the 'salles' table. idDepartementPrincipal is nullable. blockName
 * and departmentName are denormalised display fields populated by join queries.
 * equipment is stored as a JSON string. Equality and hashing are based solely
 * on id.
 ******************************************************************************/

import java.util.Objects;

public class Room {

    // Persistent fields
    private int id;
    private String name;
    private int idBloc;
    private int capacity;
    private String typeSalle;
    private String equipment;
    private int etage;
    private Integer idDepartementPrincipal;
    private String observations;
    private boolean actif;

    // Denormalised display fields
    private String blockName;
    private String departmentName;

    public Room() {}

    public Room(String name, int id) {
        this.id = id;
        this.name = name;
    }

    public Room(int id, String name, int idBloc, int capacity, String typeSalle, String equipment,
                int etage, Integer idDepartementPrincipal, String observations, boolean actif) {
        this.id = id;
        this.name = name;
        this.idBloc = idBloc;
        this.capacity = capacity;
        this.typeSalle = typeSalle;
        this.equipment = equipment;
        this.etage = etage;
        this.idDepartementPrincipal = idDepartementPrincipal;
        this.observations = observations;
        this.actif = actif;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIdBloc() { return idBloc; }
    public void setIdBloc(int idBloc) { this.idBloc = idBloc; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getTypeSalle() { return typeSalle; }
    public void setTypeSalle(String typeSalle) { this.typeSalle = typeSalle; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }

    public Integer getIdDepartementPrincipal() { return idDepartementPrincipal; }
    public void setIdDepartementPrincipal(Integer idDepartementPrincipal) { this.idDepartementPrincipal = idDepartementPrincipal; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Room) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return name; }
}
