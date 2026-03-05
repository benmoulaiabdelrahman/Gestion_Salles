-- Minimal non-sensitive fixtures for local/dev testing
-- Apply after db/schema.sql

INSERT INTO departements (id_departement, nom_departement, code_departement, description, actif)
VALUES
  (1, 'Informatique', 'INFO', 'Département Informatique', 1),
  (2, 'Sciences', 'SCI', 'Département Sciences', 1)
ON DUPLICATE KEY UPDATE nom_departement = VALUES(nom_departement);

INSERT INTO blocs (id_bloc, nom_bloc, code_bloc, adresse, nombre_etages, actif, id_departement)
VALUES
  (1, 'Bloc A', 'BLA', 'Campus Principal', 2, 1, 1),
  (2, 'Bloc B', 'BLB', 'Campus Principal', 3, 1, 2)
ON DUPLICATE KEY UPDATE nom_bloc = VALUES(nom_bloc);

INSERT INTO types_activites (id_type_activite, nom_type, description, couleur_hex, is_group_specific, actif)
VALUES
  (1, 'Cours', 'Séance de cours', '#1E88E5', 0, 1),
  (2, 'TD', 'Travaux dirigés', '#43A047', 1, 1),
  (3, 'TP', 'Travaux pratiques', '#FB8C00', 1, 1)
ON DUPLICATE KEY UPDATE nom_type = VALUES(nom_type);

INSERT INTO niveaux (id_niveau, nom_niveau, code_niveau, id_departement, id_bloc, nombre_etudiants, nombre_groupes, annee_academique, actif)
VALUES
  (1, 'L3 Informatique', 'L3-INFO', 1, 1, 120, 3, '2025-2026', 1),
  (2, 'M1 Informatique', 'M1-INFO', 1, 1, 60, 2, '2025-2026', 1)
ON DUPLICATE KEY UPDATE nom_niveau = VALUES(nom_niveau);

INSERT INTO salles (id_salle, numero_salle, id_bloc, capacite, type_salle, equipements, etage, id_departement_principal, observations, actif)
VALUES
  (1, 'INFO-01', 1, 40, 'TD', '[]', 1, 1, 'Salle standard', 1),
  (2, 'En ligne', 1, 999, 'REUNION', '{}', 0, 1, 'Salle virtuelle', 1)
ON DUPLICATE KEY UPDATE numero_salle = VALUES(numero_salle);

-- Password hash corresponds to the placeholder password "password".
INSERT INTO utilisateurs (
  id_utilisateur, nom, prenom, email, mot_de_passe, role, id_departement,
  telephone, photo_profil, actif, id_bloc, must_change_password
)
VALUES
  (1, 'Admin', 'System', 'admin@gestion-salles.local',
   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
   'Admin', 1, NULL, NULL, 1, 1, 1),
  (2, 'Doe', 'Jane', 'jane.doe@gestion-salles.local',
   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
   'Enseignant', 1, NULL, NULL, 1, 1, 1)
ON DUPLICATE KEY UPDATE email = VALUES(email);
