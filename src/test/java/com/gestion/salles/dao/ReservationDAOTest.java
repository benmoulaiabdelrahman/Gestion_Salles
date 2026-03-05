package com.gestion.salles.dao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationDAOTest {

    @Test
    void checkNiveauAvailabilityReturnsTrueWhenNiveauIsNull() {
        ReservationDAO dao = new ReservationDAO();

        boolean available = dao.checkNiveauAvailability(
            null,
            false,
            LocalDate.of(2026, 3, 2),
            LocalTime.of(8, 0),
            LocalTime.of(9, 30),
            null,
            null,
            null,
            null,
            1,
            null
        );

        assertTrue(available);
    }
}
