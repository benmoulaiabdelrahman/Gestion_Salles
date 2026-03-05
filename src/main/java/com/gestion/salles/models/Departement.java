package com.gestion.salles.models;

/******************************************************************************
 * Departement.java
 *
 * Model for the 'departements' table. idBloc and blocName are denormalised
 * fields populated by join queries for UI display; they may be null when the
 * department has no associated bloc. Equality and hashing are based solely
 * on id.
 ******************************************************************************/

import java.util.Objects;

public class Departement {

    private int id;
    private String nom;
    private String code;
    private String description;
    private boolean actif;
    private Integer idBloc;
    private String blocName;

    public Departement() {}

    public Departement(String nom) {
        this.nom = nom;
    }

    public Departement(String nom, int id) {
        this.id = id;
        this.nom = nom;
    }

    public Departement(int id, String nom, String code, String description, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.code = code;
        this.description = description;
        this.actif = actif;
    }

    public Departement(int id, String nom, String code, String description, boolean actif, Integer idBloc, String blocName) {
        this(id, nom, code, description, actif);
        this.idBloc = idBloc;
        this.blocName = blocName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Integer getIdBloc() { return idBloc; }
    public void setIdBloc(Integer idBloc) { this.idBloc = idBloc; }

    public String getBlocName() { return blocName; }
    public void setBlocName(String blocName) { this.blocName = blocName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Departement) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nom; }
}
