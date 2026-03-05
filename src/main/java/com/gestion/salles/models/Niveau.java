package com.gestion.salles.models;

/******************************************************************************
 * Niveau.java
 *
 * Model for the 'niveaux' table. departementName, idBloc, and nomBloc are
 * denormalised fields populated by join queries for UI display; idBloc may
 * be null when no bloc is associated. Equality and hashing are based solely
 * on id.
 ******************************************************************************/

import java.util.Objects;

public class Niveau {

    private int id;
    private String nom;
    private String code;
    private int idDepartement;
    private String departementName;
    private Integer idBloc;
    private String nomBloc;
    private int nombreEtudiants;
    private int nombreGroupes;
    private String anneeAcademique;
    private boolean actif;

    public Niveau() {}

    public Niveau(String nom) {
        this.nom = nom;
    }

    public Niveau(String nom, int id) {
        this.id = id;
        this.nom = nom;
    }

    public Niveau(int id, String nom, String code, int idDepartement, String departementName,
                  Integer idBloc, String nomBloc, int nombreEtudiants, int nombreGroupes,
                  String anneeAcademique, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.code = code;
        this.idDepartement = idDepartement;
        this.departementName = departementName;
        this.idBloc = idBloc;
        this.nomBloc = nomBloc;
        this.nombreEtudiants = nombreEtudiants;
        this.nombreGroupes = nombreGroupes;
        this.anneeAcademique = anneeAcademique;
        this.actif = actif;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getIdDepartement() { return idDepartement; }
    public void setIdDepartement(int idDepartement) { this.idDepartement = idDepartement; }

    public String getDepartementName() { return departementName; }
    public void setDepartementName(String departementName) { this.departementName = departementName; }

    public Integer getIdBloc() { return idBloc; }
    public void setIdBloc(Integer idBloc) { this.idBloc = idBloc; }

    public String getNomBloc() { return nomBloc; }
    public void setNomBloc(String nomBloc) { this.nomBloc = nomBloc; }

    public int getNombreEtudiants() { return nombreEtudiants; }
    public void setNombreEtudiants(int nombreEtudiants) { this.nombreEtudiants = nombreEtudiants; }

    public int getNombreGroupes() { return nombreGroupes; }
    public void setNombreGroupes(int nombreGroupes) { this.nombreGroupes = nombreGroupes; }

    public String getAnneeAcademique() { return anneeAcademique; }
    public void setAnneeAcademique(String anneeAcademique) { this.anneeAcademique = anneeAcademique; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Niveau) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nom; }
}
