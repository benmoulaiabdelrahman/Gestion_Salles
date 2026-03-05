package com.gestion.salles.services;

import com.gestion.salles.models.Conflict;
import com.gestion.salles.models.Reservation;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConflictDetectionServiceTest {

    @Test
    void returnsCompleteDuplicateWhenAllConflictTypesPointToSameReservation() {
        FakeReservationLookup reservationLookup = new FakeReservationLookup();
        Reservation existing = buildExistingReservation(1001);
        reservationLookup.byId.put(1001, existing);

        TestConflictDetectionService service = new TestConflictDetectionService(reservationLookup);
        service.roomConflict = result(true, 1001);
        service.teacherConflict = result(true, 1001);
        service.niveauConflict = result(true, 1001);

        Reservation incoming = buildIncomingReservation();
        List<Conflict> conflicts = service.checkConflicts(incoming, null);

        assertEquals(1, conflicts.size());
        assertEquals(Conflict.ConflictType.COMPLETE_DUPLICATE, conflicts.get(0).getType());
    }

    @Test
    void returnsNoConflictsWhenProceduresReportNoConflict() {
        FakeReservationLookup reservationLookup = new FakeReservationLookup();

        TestConflictDetectionService service = new TestConflictDetectionService(reservationLookup);
        service.roomConflict = result(false, 0);
        service.teacherConflict = result(false, 0);
        service.niveauConflict = result(false, 0);

        Reservation incoming = buildIncomingReservation();
        List<Conflict> conflicts = service.checkConflicts(incoming, null);

        assertTrue(conflicts.isEmpty());
    }

    private static Reservation buildIncomingReservation() {
        Reservation r = new Reservation();
        r.setIdReservation(0);
        r.setIdSalle(10);
        r.setNomSalle("INFO-01");
        r.setIdEnseignant(20);
        r.setNomEnseignant("Doe");
        r.setPrenomEnseignant("Jane");
        r.setIdNiveau(30);
        r.setNomNiveau("L3 INFO");
        r.setIdTypeActivite(1);
        r.setDateReservation(LocalDate.of(2026, 3, 2));
        r.setHeureDebut(LocalTime.of(8, 0));
        r.setHeureFin(LocalTime.of(9, 30));
        r.setRecurring(false);
        r.setOnline(false);
        return r;
    }

    private static Reservation buildExistingReservation(int id) {
        Reservation r = buildIncomingReservation();
        r.setIdReservation(id);
        r.setGroupNumber(1);
        return r;
    }

    private static ConflictDetectionService.CallableStatementResult result(boolean exists, int reservationId) {
        ConflictDetectionService.CallableStatementResult result = new ConflictDetectionService.CallableStatementResult();
        result.conflitExiste = exists;
        result.idReservationConflit = reservationId;
        return result;
    }

    private static final class FakeReservationLookup implements ConflictDetectionService.ReservationLookup {
        private final Map<Integer, Reservation> byId = new HashMap<>();

        @Override
        public Reservation getReservationById(int id) {
            return byId.get(id);
        }
    }

    private static class TestConflictDetectionService extends ConflictDetectionService {
        CallableStatementResult roomConflict;
        CallableStatementResult teacherConflict;
        CallableStatementResult niveauConflict;

        TestConflictDetectionService(ReservationLookup reservationLookup) {
            super(reservationLookup);
        }

        @Override
        protected CallableStatementResult callRoomConflictProcedure(int roomId, boolean isRecurring, LocalDate date,
                                                                    LocalTime startTime, LocalTime endTime,
                                                                    LocalDate recurrenceStart, LocalDate recurrenceEnd,
                                                                    String dayOfWeek, Integer excludeId,
                                                                    boolean isOnline) throws SQLException {
            return roomConflict;
        }

        @Override
        protected CallableStatementResult callTeacherConflictProcedure(int idEnseignant, boolean isRecurring,
                                                                       LocalDate date, LocalTime startTime,
                                                                       LocalTime endTime, LocalDate recurrenceStart,
                                                                       LocalDate recurrenceEnd, String dayOfWeek,
                                                                       Integer excludeId) throws SQLException {
            return teacherConflict;
        }

        @Override
        protected CallableStatementResult callNiveauConflictProcedure(int idNiveau, boolean isRecurring,
                                                                      LocalDate date, LocalTime startTime,
                                                                      LocalTime endTime, LocalDate recurrenceStart,
                                                                      LocalDate recurrenceEnd, String dayOfWeek,
                                                                      Integer groupNumber, int activityType,
                                                                      Integer excludeId) throws SQLException {
            return niveauConflict;
        }
    }
}
