package com.gestion.salles.models;

/******************************************************************************
 * User.java
 *
 * Model for the 'utilisateurs' table. idDepartement, idBloc, and their
 * corresponding name fields are nullable; nomDepartement, idBloc, and nomBloc
 * are denormalised display fields populated by join queries. getFullName()
 * falls back to nom when prenom is absent. Equality and hashing are based
 * solely on idUtilisateur.
 ******************************************************************************/

import java.sql.Timestamp;
import java.util.Objects;

public class User {

    public enum Role {
        Admin,
        Chef_Departement,
        Enseignant;

        public String getRoleName() {
            switch (this) {
                case Admin:            return "Admin";
                case Chef_Departement: return "Chef Departement";
                case Enseignant:       return "Enseignant";
                default:               return this.name();
            }
        }
    }

    // Persistent fields
    private int idUtilisateur;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private Role role;
    private Integer idDepartement;
    private String telephone;
    private String photoProfil;
    private boolean actif;
    private boolean mustChangePassword;
    private Timestamp dateCreation;
    private Timestamp derniereConnexion;

    // Denormalised display fields
    private String nomDepartement;
    private Integer idBloc;
    private String nomBloc;

    public User() {}

    public User(String fullName, int id) {
        this.idUtilisateur = id;
        this.prenom = fullName;
        this.nom = "";
    }

    public User(int idUtilisateur, String nom, String prenom, String email, Role role) {
        this.idUtilisateur = idUtilisateur;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
    }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getIdDepartement() { return idDepartement; }
    public void setIdDepartement(Integer idDepartement) { this.idDepartement = idDepartement; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getPhotoProfil() { return photoProfil; }
    public void setPhotoProfil(String photoProfil) { this.photoProfil = photoProfil; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public Timestamp getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(Timestamp derniereConnexion) { this.derniereConnexion = derniereConnexion; }

    public String getNomDepartement() { return nomDepartement; }
    public void setNomDepartement(String nomDepartement) { this.nomDepartement = nomDepartement; }

    public Integer getIdBloc() { return idBloc; }
    public void setIdBloc(Integer idBloc) { this.idBloc = idBloc; }

    public String getNomBloc() { return nomBloc; }
    public void setNomBloc(String nomBloc) { this.nomBloc = nomBloc; }

    public String getFullName() {
        return (prenom == null || prenom.isEmpty()) ? nom : prenom + " " + nom;
    }

    public boolean isAdmin()           { return role == Role.Admin; }
    public boolean isChefDepartement() { return role == Role.Chef_Departement; }
    public boolean isEnseignant()      { return role == Role.Enseignant; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return idUtilisateur == ((User) o).idUtilisateur;
    }

    @Override
    public int hashCode() { return Objects.hash(idUtilisateur); }

    @Override
    public String toString() { return getFullName(); }
}
