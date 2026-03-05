package com.gestion.salles.services;

import com.gestion.salles.models.User;
import com.gestion.salles.utils.SessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private FakeUserRepository fakeUserRepository;
    private AuthService authService;
    private String originalUserHome;
    private Path tempUserHome;

    @BeforeEach
    void setUp() throws Exception {
        fakeUserRepository = new FakeUserRepository();
        authService = new AuthService(fakeUserRepository);
        SessionContext.clear();
        originalUserHome = System.getProperty("user.home");
        tempUserHome = Files.createTempDirectory("auth-service-test-home");
        System.setProperty("user.home", tempUserHome.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        SessionContext.clear();
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        if (tempUserHome != null) {
            Files.walk(tempUserHome)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Best-effort test cleanup.
                    }
                });
        }
    }

    @Test
    void authenticateSuccessSetsSessionContext() {
        User user = new User();
        user.setIdUtilisateur(42);
        user.setEmail("test.user@lagh-univ.dz");
        user.setRole(User.Role.Enseignant);
        fakeUserRepository.authenticatedUser = user;

        AuthService.AuthenticationResult result = authService.authenticate(
            "test.user@lagh-univ.dz",
            "secret123".toCharArray(),
            false
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertEquals(42, SessionContext.getCurrentUserId());
        assertEquals("test.user@lagh-univ.dz", SessionContext.getCurrentUserEmail());
    }

    @Test
    void authenticateInvalidEmailFailsFast() {
        AuthService.AuthenticationResult result = authService.authenticate(
            "not-an-email",
            "secret123".toCharArray(),
            false
        );

        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertFalse(fakeUserRepository.authenticateCalled);
    }

    @Test
    void authenticateWrongCredentialsReturnsFailure() {
        fakeUserRepository.authenticatedUser = null;

        AuthService.AuthenticationResult result = authService.authenticate(
            "missing@lagh-univ.dz",
            "wrong".toCharArray(),
            false
        );

        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertTrue(fakeUserRepository.authenticateCalled);
        assertNull(SessionContext.getCurrentUserId());
    }

    private static final class FakeUserRepository implements AuthService.UserRepository {
        private User authenticatedUser;
        private boolean authenticateCalled;

        @Override
        public User authenticate(String email, char[] password) {
            authenticateCalled = true;
            return authenticatedUser;
        }

        @Override
        public User findUserByRememberToken(String token) {
            return null;
        }

        @Override
        public User findByEmail(String email) {
            return null;
        }

        @Override
        public boolean updatePassword(String email, String newPassword) {
            return true;
        }

        @Override
        public boolean storeRememberToken(int userId, String token, Timestamp expiry) {
            return true;
        }

        @Override
        public boolean clearRememberToken(int userId) {
            return true;
        }
    }
}
