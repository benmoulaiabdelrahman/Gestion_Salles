package com.gestion.salles.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationCodeManagerTest {

    private static final String TEST_SECRET = "test_secret_for_hmac";

    private InMemoryVerificationCodeStore codeStore;
    private VerificationCodeManager verificationCodeManager;

    @BeforeEach
    void setUp() {
        codeStore = new InMemoryVerificationCodeStore();
        verificationCodeManager = new VerificationCodeManager(codeStore, TEST_SECRET);
    }

    @Test
    void testGenerateAndValidateCodeSuccess() {
        String email = "test@example.com";
        String code = verificationCodeManager.generateCode(email);

        assertNotNull(code);
        assertEquals(6, code.length());

        VerificationCodeManager.ValidationResult result = verificationCodeManager.validateCode(email, code);
        assertTrue(result.isSuccess());
        assertEquals("Code vérifié avec succès", result.getMessage());
        assertFalse(verificationCodeManager.hasValidCode(email));
    }

    @Test
    void testValidateCodeIncorrect() {
        String email = "test2@example.com";
        verificationCodeManager.generateCode(email);

        VerificationCodeManager.ValidationResult result = verificationCodeManager.validateCode(email, "999999");
        assertFalse(result.isSuccess());
        assertEquals("Code de vérification incorrect", result.getMessage());
        assertTrue(verificationCodeManager.hasValidCode(email));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidateCodeExpired() throws Exception {
        String email = "expired@example.com";
        String code = verificationCodeManager.generateCode(email);

        Field verificationCodesField = VerificationCodeManager.class.getDeclaredField("verificationCodes");
        verificationCodesField.setAccessible(true);
        Map<String, Object> codesMap = (Map<String, Object>) verificationCodesField.get(verificationCodeManager);

        Object entryObject = codesMap.get(email.toLowerCase());
        Field expiryTimeField = entryObject.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        expiryTimeField.set(entryObject, LocalDateTime.now().minusMinutes(1));

        VerificationCodeManager.ValidationResult result = verificationCodeManager.validateCode(email, code);
        assertFalse(result.isSuccess());
        assertEquals("Le code de vérification a expiré", result.getMessage());
        assertFalse(verificationCodeManager.hasValidCode(email));
    }

    @Test
    void testHasValidCode() {
        String email = "validcheck@example.com";
        verificationCodeManager.generateCode(email);
        assertTrue(verificationCodeManager.hasValidCode(email));
        assertFalse(verificationCodeManager.hasValidCode("nonexistent@example.com"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetRemainingMinutes() throws Exception {
        String email = "remaining@example.com";
        verificationCodeManager.generateCode(email);

        long minutes = verificationCodeManager.getRemainingMinutes(email);
        assertTrue(minutes >= 0 && minutes <= 10);

        Field verificationCodesField = VerificationCodeManager.class.getDeclaredField("verificationCodes");
        verificationCodesField.setAccessible(true);
        Map<String, Object> codesMap = (Map<String, Object>) verificationCodesField.get(verificationCodeManager);

        Object entryObject = codesMap.get(email.toLowerCase());
        Field expiryTimeField = entryObject.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        expiryTimeField.set(entryObject, LocalDateTime.now().minusMinutes(1));

        assertEquals(0, verificationCodeManager.getRemainingMinutes(email));
    }

    @Test
    void testInvalidateCode() {
        String email = "tobeinvalidated@example.com";
        verificationCodeManager.generateCode(email);
        assertTrue(verificationCodeManager.hasValidCode(email));

        verificationCodeManager.invalidateCode(email);
        assertFalse(verificationCodeManager.hasValidCode(email));
        assertFalse(codeStore.hasEmail(email));
    }

    @Test
    void testRehydrationOnRestart() {
        String email = "rehydrate@example.com";
        String code = verificationCodeManager.generateCode(email);
        assertTrue(verificationCodeManager.hasValidCode(email));

        VerificationCodeManager restarted = new VerificationCodeManager(codeStore, TEST_SECRET);
        assertTrue(restarted.hasValidCode(email));

        VerificationCodeManager.ValidationResult result = restarted.validateCode(email, code);
        assertTrue(result.isSuccess());
        assertEquals("Code vérifié avec succès", result.getMessage());
        assertFalse(restarted.hasValidCode(email));
    }

    private static class InMemoryVerificationCodeStore implements VerificationCodeManager.VerificationCodeStore {
        private final Map<String, VerificationCodeManager.PersistedCode> rows = new ConcurrentHashMap<>();

        @Override
        public void ensureTable() {
            // No-op for in-memory store.
        }

        @Override
        public void upsert(String email, String codeHash, LocalDateTime expiryTime, LocalDateTime createdAt) {
            rows.put(email.toLowerCase(), new VerificationCodeManager.PersistedCode(codeHash, expiryTime));
        }

        @Override
        public VerificationCodeManager.PersistedCode loadUnexpiredCode(String email) {
            VerificationCodeManager.PersistedCode row = rows.get(email.toLowerCase());
            if (row == null || row.expiryTime().isBefore(LocalDateTime.now())) {
                return null;
            }
            return row;
        }

        @Override
        public Map<String, VerificationCodeManager.PersistedCode> loadUnexpiredCodes() {
            Map<String, VerificationCodeManager.PersistedCode> active = new ConcurrentHashMap<>();
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, VerificationCodeManager.PersistedCode> entry : rows.entrySet()) {
                if (entry.getValue().expiryTime().isAfter(now)) {
                    active.put(entry.getKey(), entry.getValue());
                }
            }
            return active;
        }

        @Override
        public void delete(String email) {
            rows.remove(email.toLowerCase());
        }

        boolean hasEmail(String email) {
            return rows.containsKey(email.toLowerCase());
        }
    }
}
