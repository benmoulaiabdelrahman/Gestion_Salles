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
