package com.gestion.salles.views.Admin;

import com.gestion.salles.models.User;

import javax.swing.SwingUtilities;

/**
 * Manual development harness for Admin Dashboard only.
 * Lives under src/test so it is not a production entry point.
 */
public final class DashboardDevTest {

    private DashboardDevTest() {}

    public static void main(String[] args) {
        User devUser = new User();
        devUser.setIdUtilisateur(1);
        devUser.setNom("Dev");
        devUser.setPrenom("Admin");
        devUser.setEmail("admin.dev@example.com");
        devUser.setRole(User.Role.Admin);
        devUser.setActif(true);

        TestSessionManager testSessionManager = new InMemoryTestSessionManager();
        String testToken = testSessionManager.createSession(devUser.getEmail());

        SwingUtilities.invokeLater(() -> new Dashboard(devUser, testToken).setVisible(true));
    }

    interface TestSessionManager {
        String createSession(String userEmail);
    }

    static final class InMemoryTestSessionManager implements TestSessionManager {
        @Override
        public String createSession(String userEmail) {
            if (userEmail == null || userEmail.isBlank()) {
                throw new IllegalArgumentException("userEmail is required for test session");
            }
            return "dev-session-token-" + System.currentTimeMillis();
        }
    }
}
