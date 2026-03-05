package com.gestion.salles.models;

/******************************************************************************
 * Bloc.java
 *
 * Model for the 'blocs' table. Represents a physical building block with an
 * optional Departement association. Equality and hashing are based solely
 * on id.
 ******************************************************************************/

import java.util.Objects;

public class Bloc {

    private int id;
    private String nom;
    private String code;
    private String adresse;
    private int nombreEtages;
    private boolean actif;
    private Departement departement;

    public Bloc() {}

    public Bloc(String nom) {
        this.nom = nom;
    }

    public Bloc(String nom, int id) {
        this.id = id;
        this.nom = nom;
    }

    public Bloc(int id, String nom, String code, String adresse, int nombreEtages, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.code = code;
        this.adresse = adresse;
        this.nombreEtages = nombreEtages;
        this.actif = actif;
    }

    public Bloc(int id, String nom, String code, String adresse, int nombreEtages, boolean actif, Departement departement) {
        this(id, nom, code, adresse, nombreEtages, actif);
        this.departement = departement;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public int getNombreEtages() { return nombreEtages; }
    public void setNombreEtages(int nombreEtages) { this.nombreEtages = nombreEtages; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Departement getDepartement() { return departement; }
    public void setDepartement(Departement departement) { this.departement = departement; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Bloc) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nom; }
}
