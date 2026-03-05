package com.gestion.salles.views.Login;

import javax.swing.SwingUtilities;

/**
 * Manual smoke harness for login/password recovery navigation.
 * Run from IDE when validating desktop UI flows.
 */
public final class LoginFlowDevTest {

    private LoginFlowDevTest() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);

            // Optional visual check path for password-recovery navigator.
            frame.showPasswordRecoveryPanel();
            frame.showLoginPanel();
        });
    }
}
