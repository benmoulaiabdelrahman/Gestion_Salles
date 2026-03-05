-- Sanitized schema-only package (no production data).
-- Seed data lives in db/seed/minimal_seed.sql
-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: gestion_salles
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `active_sessions`
--

DROP TABLE IF EXISTS `active_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `active_sessions` (
  `user_email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL,
  `last_seen_at` timestamp NOT NULL,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`session_token`),
  KEY `idx_active_sessions_user_email` (`user_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `activity_log`
--

DROP TABLE IF EXISTS `activity_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_log` (
  `id_log` int NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `action_type` enum('CREATE','UPDATE','DELETE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_type` enum('USER','ROOM','RESERVATION','FACULTE','DEPARTEMENT','NIVEAU') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` int DEFAULT NULL,
  `id_user_acting` int NOT NULL,
  `details` text COLLATE utf8mb4_unicode_ci,
  `id_bloc` int DEFAULT NULL,
  PRIMARY KEY (`id_log`),
  KEY `id_user_acting` (`id_user_acting`),
  KEY `fk_activity_log_bloc` (`id_bloc`),
  KEY `idx_activity_log_timestamp` (`timestamp`),
  KEY `idx_activity_log_entity_type` (`entity_type`),
  CONSTRAINT `activity_log_ibfk_1` FOREIGN KEY (`id_user_acting`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE RESTRICT,
  CONSTRAINT `fk_activity_log_bloc` FOREIGN KEY (`id_bloc`) REFERENCES `blocs` (`id_bloc`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2864 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_detail` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `success` tinyint(1) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `checksum` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_log_user_email` (`user_email`),
  KEY `idx_audit_log_created_at` (`created_at`),
  KEY `idx_audit_log_user_created` (`user_email`,`created_at` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `blocs`
--

DROP TABLE IF EXISTS `blocs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blocs` (
  `id_bloc` int NOT NULL AUTO_INCREMENT,
  `nom_bloc` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_bloc` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `adresse` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `nombre_etages` int DEFAULT '1',
  `actif` tinyint(1) DEFAULT '1',
  `id_departement` int DEFAULT NULL,
  PRIMARY KEY (`id_bloc`),
  UNIQUE KEY `nom_bloc` (`nom_bloc`),
  UNIQUE KEY `code_bloc` (`code_bloc`),
  KEY `fk_bloc_departement` (`id_departement`),
  CONSTRAINT `fk_bloc_departement` FOREIGN KEY (`id_departement`) REFERENCES `departements` (`id_departement`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `departements`
--

DROP TABLE IF EXISTS `departements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departements` (
  `id_departement` int NOT NULL AUTO_INCREMENT,
  `nom_departement` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_departement` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `actif` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id_departement`),
  UNIQUE KEY `nom_departement` (`nom_departement`),
  UNIQUE KEY `code_departement` (`code_departement`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `historique_reservations`
--

DROP TABLE IF EXISTS `historique_reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historique_reservations` (
  `id_historique` int NOT NULL AUTO_INCREMENT,
  `id_reservation` int NOT NULL,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_utilisateur` int NOT NULL,
  `anciennes_valeurs` json DEFAULT NULL,
  `nouvelles_valeurs` json DEFAULT NULL,
  `commentaire` text COLLATE utf8mb4_unicode_ci,
  `date_enregistrement` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_historique`),
  KEY `fk_historique_reservation` (`id_reservation`),
  KEY `fk_historique_utilisateur` (`id_utilisateur`),
  CONSTRAINT `fk_historique_reservation` FOREIGN KEY (`id_reservation`) REFERENCES `reservations` (`id_reservation`) ON DELETE CASCADE,
  CONSTRAINT `fk_historique_utilisateur` FOREIGN KEY (`id_utilisateur`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `niveaux`
--

DROP TABLE IF EXISTS `niveaux`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `niveaux` (
  `id_niveau` int NOT NULL AUTO_INCREMENT,
  `nom_niveau` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_niveau` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_departement` int NOT NULL,
  `id_bloc` int DEFAULT NULL,
  `nombre_etudiants` int DEFAULT '0',
  `nombre_groupes` int NOT NULL DEFAULT '1',
  `annee_academique` varchar(9) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actif` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id_niveau`),
  UNIQUE KEY `unique_niveau_dept` (`code_niveau`,`id_departement`),
  KEY `idx_dept_niveau` (`id_departement`),
  KEY `idx_bloc_niveau` (`id_bloc`),
  KEY `idx_bloc_dept_niveau` (`id_bloc`,`id_departement`),
  CONSTRAINT `fk_niveau_bloc` FOREIGN KEY (`id_bloc`) REFERENCES `blocs` (`id_bloc`) ON DELETE SET NULL,
  CONSTRAINT `niveaux_ibfk_1` FOREIGN KEY (`id_departement`) REFERENCES `departements` (`id_departement`) ON DELETE CASCADE,
  CONSTRAINT `chk_annee_format` CHECK (regexp_like(`annee_academique`,_utf8mb4'^[0-9]{4}-[0-9]{4}$'))
) ENGINE=InnoDB AUTO_INCREMENT=318 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `id_reservation` int NOT NULL AUTO_INCREMENT,
  `id_salle` int NOT NULL,
  `id_departement` int NOT NULL,
  `id_bloc` int DEFAULT NULL,
  `id_niveau` int DEFAULT NULL,
  `group_number` int DEFAULT NULL,
  `id_enseignant` int NOT NULL COMMENT 'Teacher making/using the reservation',
  `id_type_activite` int NOT NULL,
  `date_reservation` date DEFAULT NULL,
  `heure_debut` time NOT NULL,
  `heure_fin` time NOT NULL,
  `titre_activite` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_recurring` tinyint(1) NOT NULL DEFAULT '0',
  `date_debut_recurrence` date DEFAULT NULL,
  `date_fin_recurrence` date DEFAULT NULL,
  `day_of_week` enum('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_online` tinyint(1) NOT NULL DEFAULT '0',
  `statut` enum('CONFIRMEE','EN_ATTENTE','ANNULEE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'CONFIRMEE',
  `id_utilisateur_creation` int NOT NULL COMMENT 'User who created the reservation',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `date_modification` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `observations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id_reservation`),
  KEY `id_type_activite` (`id_type_activite`),
  KEY `id_utilisateur_creation` (`id_utilisateur_creation`),
  KEY `idx_salle_date` (`id_salle`,`date_reservation`),
  KEY `idx_enseignant_date` (`id_enseignant`,`date_reservation`),
  KEY `idx_dept_date` (`id_departement`,`date_reservation`),
  KEY `idx_niveau_date` (`id_niveau`,`date_reservation`),
  KEY `idx_statut` (`statut`),
  KEY `idx_date_heure` (`date_reservation`,`heure_debut`,`heure_fin`),
  KEY `idx_reservation_datetime_statut` (`date_reservation`,`heure_debut`,`heure_fin`,`statut`),
  KEY `fk_reservation_bloc` (`id_bloc`),
  KEY `idx_recurring` (`id_salle`,`day_of_week`,`heure_debut`,`heure_fin`),
  KEY `idx_reservations_recurring_conflict` (`id_salle`,`is_recurring`,`day_of_week`,`statut`,`date_debut_recurrence`,`date_fin_recurrence`,`heure_debut`,`heure_fin`),
  CONSTRAINT `fk_reservation_bloc` FOREIGN KEY (`id_bloc`) REFERENCES `blocs` (`id_bloc`) ON DELETE CASCADE,
  CONSTRAINT `reservations_ibfk_1` FOREIGN KEY (`id_salle`) REFERENCES `salles` (`id_salle`) ON DELETE CASCADE,
  CONSTRAINT `reservations_ibfk_2` FOREIGN KEY (`id_departement`) REFERENCES `departements` (`id_departement`) ON DELETE CASCADE,
  CONSTRAINT `reservations_ibfk_3` FOREIGN KEY (`id_niveau`) REFERENCES `niveaux` (`id_niveau`) ON DELETE SET NULL,
  CONSTRAINT `reservations_ibfk_4` FOREIGN KEY (`id_enseignant`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE,
  CONSTRAINT `reservations_ibfk_5` FOREIGN KEY (`id_type_activite`) REFERENCES `types_activites` (`id_type_activite`) ON DELETE CASCADE,
  CONSTRAINT `reservations_ibfk_6` FOREIGN KEY (`id_utilisateur_creation`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE,
  CONSTRAINT `chk_heure_debut_fin` CHECK ((`heure_debut` < `heure_fin`))
) ENGINE=InnoDB AUTO_INCREMENT=437 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_check_recurring_conflict_bi` BEFORE INSERT ON `reservations` FOR EACH ROW BEGIN
      IF NEW.is_recurring = 1
         AND NEW.day_of_week IS NOT NULL
         AND NEW.date_debut_recurrence IS NOT NULL
         AND NEW.date_fin_recurrence IS NOT NULL THEN
          IF EXISTS (
              SELECT 1
              FROM reservations r
              WHERE r.id_salle = NEW.id_salle
                AND r.is_recurring = 1
                AND r.statut <> 'ANNULEE'
                AND r.day_of_week = NEW.day_of_week
                AND r.heure_debut < NEW.heure_fin
                AND r.heure_fin > NEW.heure_debut
                AND r.date_debut_recurrence < NEW.date_fin_recurrence
                AND r.date_fin_recurrence > NEW.date_debut_recurrence
          ) THEN
              SIGNAL SQLSTATE '45000'
              SET MESSAGE_TEXT = 'Recurring reservation conflict detected';
          END IF;
      END IF;
  END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_historique_insert` AFTER INSERT ON `reservations` FOR EACH ROW BEGIN
    INSERT INTO historique_reservations (
        id_reservation,
        action,
        id_utilisateur,
        nouvelles_valeurs,
        commentaire
    ) VALUES (
        NEW.id_reservation,
        'CREATION',
        NEW.id_utilisateur_creation,
        JSON_OBJECT(
            'id_salle', NEW.id_salle,
            'id_departement', NEW.id_departement,
            'id_bloc', NEW.id_bloc,
            'id_niveau', NEW.id_niveau,
            'id_enseignant', NEW.id_enseignant,
            'date_reservation', NEW.date_reservation,
            'heure_debut', NEW.heure_debut,
            'heure_fin', NEW.heure_fin,
            'titre_activite', NEW.titre_activite,
            'statut', NEW.statut
        ),
        'Nouvelle réservation créée'
    );
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_check_recurring_conflict_bu` BEFORE UPDATE ON `reservations` FOR EACH ROW BEGIN
      IF NEW.is_recurring = 1
         AND NEW.day_of_week IS NOT NULL
         AND NEW.date_debut_recurrence IS NOT NULL
         AND NEW.date_fin_recurrence IS NOT NULL THEN
          IF EXISTS (
              SELECT 1
              FROM reservations r
              WHERE r.id_reservation <> NEW.id_reservation
                AND r.id_salle = NEW.id_salle
                AND r.is_recurring = 1
                AND r.statut <> 'ANNULEE'
                AND r.day_of_week = NEW.day_of_week
                AND r.heure_debut < NEW.heure_fin
                AND r.heure_fin > NEW.heure_debut
                AND r.date_debut_recurrence < NEW.date_fin_recurrence
                AND r.date_fin_recurrence > NEW.date_debut_recurrence
          ) THEN
              SIGNAL SQLSTATE '45000'
              SET MESSAGE_TEXT = 'Recurring reservation conflict detected';
          END IF;
      END IF;
  END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_historique_update` AFTER UPDATE ON `reservations` FOR EACH ROW BEGIN
    DECLARE v_action VARCHAR(20);
    DECLARE v_commentaire TEXT;
    
    
    IF NEW.statut = 'ANNULEE' AND OLD.statut != 'ANNULEE' THEN
        SET v_action = 'ANNULATION';
        SET v_commentaire = 'Réservation annulée';
    ELSE
        SET v_action = 'MODIFICATION';
        SET v_commentaire = 'Réservation modifiée';
    END IF;
    
    INSERT INTO historique_reservations (
        id_reservation,
        action,
        id_utilisateur,
        anciennes_valeurs,
        nouvelles_valeurs,
        commentaire
    ) VALUES (
        NEW.id_reservation,
        v_action,
        NEW.id_utilisateur_creation,
        JSON_OBJECT(
            'id_salle', OLD.id_salle,
            'id_departement', OLD.id_departement,
            'id_bloc', OLD.id_bloc,
            'id_niveau', OLD.id_niveau,
            'id_enseignant', OLD.id_enseignant,
            'date_reservation', OLD.date_reservation,
            'heure_debut', OLD.heure_debut,
            'heure_fin', OLD.heure_fin,
            'titre_activite', OLD.titre_activite,
            'statut', OLD.statut
        ),
        JSON_OBJECT(
            'id_salle', NEW.id_salle,
            'id_departement', NEW.id_departement,
            'id_bloc', NEW.id_bloc,
            'id_niveau', NEW.id_niveau,
            'id_enseignant', NEW.id_enseignant,
            'date_reservation', NEW.date_reservation,
            'heure_debut', NEW.heure_debut,
            'heure_fin', NEW.heure_fin,
            'titre_activite', NEW.titre_activite,
            'statut', NEW.statut
        ),
        v_commentaire
    );
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `salles`
--

DROP TABLE IF EXISTS `salles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salles` (
  `id_salle` int NOT NULL AUTO_INCREMENT,
  `numero_salle` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_bloc` int NOT NULL,
  `capacite` int NOT NULL,
  `type_salle` enum('AMPHI','TD','TP','REUNION') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'TD',
  `equipements` json DEFAULT NULL COMMENT 'JSON payload of room equipment',
  `etage` int DEFAULT NULL,
  `id_departement_principal` int DEFAULT NULL COMMENT 'Primary department managing this room',
  `observations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `actif` tinyint(1) DEFAULT '1',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_salle`),
  UNIQUE KEY `unique_salle_bloc` (`numero_salle`,`id_bloc`),
  KEY `id_departement_principal` (`id_departement_principal`),
  KEY `idx_bloc_salle` (`id_bloc`),
  KEY `idx_capacite` (`capacite`),
  KEY `idx_type_salle` (`type_salle`),
  KEY `idx_salle_actif` (`actif`,`id_bloc`),
  CONSTRAINT `salles_ibfk_1` FOREIGN KEY (`id_bloc`) REFERENCES `blocs` (`id_bloc`) ON DELETE CASCADE,
  CONSTRAINT `salles_ibfk_2` FOREIGN KEY (`id_departement_principal`) REFERENCES `departements` (`id_departement`) ON DELETE SET NULL,
  CONSTRAINT `chk_capacite_positive` CHECK ((`capacite` > 0)),
  CONSTRAINT `chk_salles_equipements_json` CHECK (((`equipements` is null) or json_valid(`equipements`)))
) ENGINE=InnoDB AUTO_INCREMENT=267 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `types_activites`
--

DROP TABLE IF EXISTS `types_activites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `types_activites` (
  `id_type_activite` int NOT NULL AUTO_INCREMENT,
  `nom_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `couleur_hex` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Color code for calendar display',
  `duree_standard` int DEFAULT NULL COMMENT 'Standard duration in minutes',
  `is_group_specific` tinyint(1) NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id_type_activite`),
  UNIQUE KEY `nom_type` (`nom_type`),
  UNIQUE KEY `code_type` (`code_type`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `utilisateurs`
--

DROP TABLE IF EXISTS `utilisateurs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utilisateurs` (
  `id_utilisateur` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `prenom` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `mot_de_passe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('Admin','Chef Departement','Enseignant') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_departement` int DEFAULT NULL,
  `telephone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_profil` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Path to profile picture file',
  `actif` tinyint(1) DEFAULT '1',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `derniere_connexion` timestamp NULL DEFAULT NULL,
  `id_bloc` int DEFAULT NULL,
  `remember_token_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remember_token_expiry` timestamp NULL DEFAULT NULL,
  `must_change_password` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_utilisateur`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_role` (`role`),
  KEY `idx_dept_user` (`id_departement`),
  KEY `idx_utilisateur_actif_role` (`actif`,`role`),
  KEY `fk_user_bloc` (`id_bloc`),
  CONSTRAINT `fk_user_bloc` FOREIGN KEY (`id_bloc`) REFERENCES `blocs` (`id_bloc`) ON DELETE SET NULL,
  CONSTRAINT `utilisateurs_ibfk_1` FOREIGN KEY (`id_departement`) REFERENCES `departements` (`id_departement`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=647 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--

--
-- Table structure for table `verification_codes`
--

DROP TABLE IF EXISTS `verification_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verification_codes` (
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` timestamp NOT NULL,
  `created_at` timestamp NOT NULL,
  PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-27 18:27:46


-- Stored procedures required by ReservationDAO and ConflictDetectionService

DELIMITER $$

DROP PROCEDURE IF EXISTS verifier_conflit_salle $$
CREATE PROCEDURE verifier_conflit_salle(
    IN p_id_salle INT,
    IN p_is_recurring BOOLEAN,
    IN p_date_reservation DATE,
    IN p_heure_debut TIME,
    IN p_heure_fin TIME,
    IN p_date_debut_recurrence DATE,
    IN p_date_fin_recurrence DATE,
    IN p_day_of_week VARCHAR(16),
    IN p_exclude_reservation_id INT,
    IN p_is_online BOOLEAN,
    OUT p_conflit_existe BOOLEAN,
    OUT p_id_reservation_conflit INT
)
BEGIN
    DECLARE v_conflict_id INT DEFAULT NULL;

    IF p_is_online THEN
        SET p_conflit_existe = FALSE;
        SET p_id_reservation_conflit = NULL;
    ELSE
        SELECT r.id_reservation
          INTO v_conflict_id
          FROM reservations r
         WHERE r.id_salle = p_id_salle
           AND r.statut IN ('CONFIRMEE','EN_ATTENTE')
           AND (p_exclude_reservation_id IS NULL OR r.id_reservation <> p_exclude_reservation_id)
           AND (
                (
                    p_is_recurring = FALSE
                    AND r.is_recurring = FALSE
                    AND r.date_reservation = p_date_reservation
                    AND r.heure_debut < p_heure_fin
                    AND r.heure_fin > p_heure_debut
                )
                OR (
                    p_is_recurring = FALSE
                    AND r.is_recurring = TRUE
                    AND r.day_of_week = UPPER(DAYNAME(p_date_reservation))
                    AND p_date_reservation BETWEEN r.date_debut_recurrence AND r.date_fin_recurrence
                    AND r.heure_debut < p_heure_fin
                    AND r.heure_fin > p_heure_debut
                )
                OR (
                    p_is_recurring = TRUE
                    AND r.is_recurring = FALSE
                    AND UPPER(DAYNAME(r.date_reservation)) = p_day_of_week
                    AND r.date_reservation BETWEEN p_date_debut_recurrence AND p_date_fin_recurrence
                    AND r.heure_debut < p_heure_fin
                    AND r.heure_fin > p_heure_debut
                )
                OR (
                    p_is_recurring = TRUE
                    AND r.is_recurring = TRUE
                    AND r.day_of_week = UPPER(DAYNAME(p_date_reservation))
                    AND r.date_debut_recurrence <= p_date_fin_recurrence
                    AND r.date_fin_recurrence >= p_date_debut_recurrence
                    AND r.heure_debut < p_heure_fin
                    AND r.heure_fin > p_heure_debut
                )
           )
         LIMIT 1;

        SET p_conflit_existe = v_conflict_id IS NOT NULL;
        SET p_id_reservation_conflit = v_conflict_id;
    END IF;
END $$

DROP PROCEDURE IF EXISTS verifier_conflit_enseignant $$
CREATE PROCEDURE verifier_conflit_enseignant(
    IN p_id_enseignant INT,
    IN p_is_recurring BOOLEAN,
    IN p_date_reservation DATE,
    IN p_heure_debut TIME,
    IN p_heure_fin TIME,
    IN p_date_debut_recurrence DATE,
    IN p_date_fin_recurrence DATE,
    IN p_day_of_week VARCHAR(16),
    IN p_exclude_reservation_id INT,
    OUT p_conflit_existe BOOLEAN,
    OUT p_id_reservation_conflit INT
)
BEGIN
    DECLARE v_conflict_id INT DEFAULT NULL;

    SELECT r.id_reservation
      INTO v_conflict_id
      FROM reservations r
     WHERE r.id_enseignant = p_id_enseignant
       AND r.statut IN ('CONFIRMEE','EN_ATTENTE')
       AND (p_exclude_reservation_id IS NULL OR r.id_reservation <> p_exclude_reservation_id)
       AND (
            (
                p_is_recurring = FALSE
                AND r.is_recurring = FALSE
                AND r.date_reservation = p_date_reservation
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
            OR (
                p_is_recurring = FALSE
                AND r.is_recurring = TRUE
                AND r.day_of_week = UPPER(DAYNAME(p_date_reservation))
                AND p_date_reservation BETWEEN r.date_debut_recurrence AND r.date_fin_recurrence
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
            OR (
                p_is_recurring = TRUE
                AND r.is_recurring = FALSE
                AND UPPER(DAYNAME(r.date_reservation)) = p_day_of_week
                AND r.date_reservation BETWEEN p_date_debut_recurrence AND p_date_fin_recurrence
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
            OR (
                p_is_recurring = TRUE
                AND r.is_recurring = TRUE
                AND r.day_of_week = UPPER(DAYNAME(p_date_reservation))
                AND r.date_debut_recurrence <= p_date_fin_recurrence
                AND r.date_fin_recurrence >= p_date_debut_recurrence
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
       )
     LIMIT 1;

    SET p_conflit_existe = v_conflict_id IS NOT NULL;
    SET p_id_reservation_conflit = v_conflict_id;
END $$

DROP PROCEDURE IF EXISTS verifier_conflit_niveau $$
CREATE PROCEDURE verifier_conflit_niveau(
    IN p_id_niveau INT,
    IN p_is_recurring BOOLEAN,
    IN p_date_reservation DATE,
    IN p_heure_debut TIME,
    IN p_heure_fin TIME,
    IN p_date_debut_recurrence DATE,
    IN p_date_fin_recurrence DATE,
    IN p_day_of_week VARCHAR(16),
    IN p_group_number INT,
    IN p_activity_type INT,
    IN p_exclude_reservation_id INT,
    OUT p_conflit_existe BOOLEAN,
    OUT p_id_reservation_conflit INT
)
BEGIN
    DECLARE v_conflict_id INT DEFAULT NULL;

    SELECT r.id_reservation
      INTO v_conflict_id
      FROM reservations r
     WHERE r.id_niveau = p_id_niveau
       AND r.statut IN ('CONFIRMEE','EN_ATTENTE')
       AND (p_exclude_reservation_id IS NULL OR r.id_reservation <> p_exclude_reservation_id)
       AND (
            p_group_number IS NULL
            OR r.group_number IS NULL
            OR r.group_number = p_group_number
       )
       AND (
            (
                p_is_recurring = FALSE
                AND r.is_recurring = FALSE
                AND r.date_reservation = p_date_reservation
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
            OR (
                p_is_recurring = FALSE
                AND r.is_recurring = TRUE
                AND r.day_of_week = UPPER(DAYNAME(p_date_reservation))
                AND p_date_reservation BETWEEN r.date_debut_recurrence AND r.date_fin_recurrence
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
            OR (
                p_is_recurring = TRUE
                AND r.is_recurring = FALSE
                AND UPPER(DAYNAME(r.date_reservation)) = p_day_of_week
                AND r.date_reservation BETWEEN p_date_debut_recurrence AND p_date_fin_recurrence
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
            OR (
                p_is_recurring = TRUE
                AND r.is_recurring = TRUE
                AND r.day_of_week = UPPER(DAYNAME(p_date_reservation))
                AND r.date_debut_recurrence <= p_date_fin_recurrence
                AND r.date_fin_recurrence >= p_date_debut_recurrence
                AND r.heure_debut < p_heure_fin
                AND r.heure_fin > p_heure_debut
            )
       )
     LIMIT 1;

    SET p_conflit_existe = v_conflict_id IS NOT NULL;
    SET p_id_reservation_conflit = v_conflict_id;
END $$

DELIMITER ;
