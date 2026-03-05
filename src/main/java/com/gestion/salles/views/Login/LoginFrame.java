package com.gestion.salles.views.Login;

/******************************************************************************
 * LoginFrame.java
 *
 * Root JFrame for the authentication flow. Acts as a navigator: hosts one
 * JPanel at a time (login form, or one of the three password-recovery steps)
 * and swaps them via switchPanel(). On successful login, creates a session
 * token via SessionManager, disposes itself, and opens the role-appropriate
 * dashboard. Unknown or error cases invalidate the session and return the
 * user to a fresh LoginFrame.
 ******************************************************************************/

import com.gestion.salles.models.User;
import com.gestion.salles.utils.AppConfig;
import com.gestion.salles.utils.SessionException;
import com.gestion.salles.utils.SessionManager;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Admin.Dashboard;
import com.gestion.salles.views.ChefDepartement.DashboardChef;
import com.gestion.salles.views.shared.recovery.PasswordRecoveryContainer;
import com.gestion.salles.views.shared.recovery.PasswordRecoveryNavigator;
import com.gestion.salles.services.AuthService;
import com.gestion.salles.services.VerificationCodeManager;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());
    private static final String APP_WM_CLASS = "com-gestion-salles-Main";

    private JPanel                   currentPanel;
    private LoginPanel               loginPanel;
    private PasswordRecoveryContainer passwordRecoveryContainer;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Gestion Salles");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 700);
        setResizable(false);
        setLocationRelativeTo(null);

        getContentPane().setBackground(ThemeConstants.APP_BACKGROUND);
        UIUtils.applyAppIcon(this);

        loginPanel = new LoginPanel(this);
        passwordRecoveryContainer = new PasswordRecoveryContainer(
            new PasswordRecoveryNavigator() {
                @Override
                public void showSettings() {
                    showLoginPanel();
                }

                @Override
                public void showPasswordRecovery() {
                    // no-op for login flow
                }
            },
            null,
            "Retour à la connexion",
            AuthService.getInstance(),
            VerificationCodeManager.getInstance()
        );
        showLoginPanel();
    }

    public void showLoginPanel() {
        switchPanel(loginPanel);
    }

    public void showPasswordRecoveryPanel() {
        passwordRecoveryContainer.reset();
        switchPanel(passwordRecoveryContainer);
        passwordRecoveryContainer.showPasswordRecovery();
    }

    private void switchPanel(JPanel newPanel) {
        if (currentPanel != null) {
            getContentPane().remove(currentPanel);
        }
        currentPanel = newPanel;
        getContentPane().add(currentPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        newPanel.requestFocusInWindow();
    }

    public void openDashboard(User user) {
        if (user == null) {
            LOGGER.severe("openDashboard appelé avec un utilisateur null.");
            showRoleError("UTILISATEUR INCONNU");
            new LoginFrame().setVisible(true);
            return;
        }

        dispose();

        try {
            String sessionToken = SessionManager.getInstance().createSession(user.getEmail());
            SessionContext.setCurrentUser(user.getIdUtilisateur(), user.getEmail(), sessionToken);

            SwingUtilities.invokeLater(() -> {
                try {
                    switch (user.getRole()) {
                        case Admin            -> new Dashboard(user, sessionToken).setVisible(true);
                        case Chef_Departement -> new DashboardChef(user, sessionToken).setVisible(true);
                        case Enseignant       -> new com.gestion.salles.views.Enseignant.Dashboard(user, sessionToken).setVisible(true);
                        default -> {
                            LOGGER.warning("Rôle inconnu : " + user.getRole());
                            showRoleError(user.getRole().toString());
                            SessionManager.getInstance().invalidateSession(user.getEmail());
                            new LoginFrame().setVisible(true);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture du tableau de bord pour le rôle : " + user.getRole(), e);
                    JOptionPane.showMessageDialog(null,
                        "Erreur lors de l'ouverture du tableau de bord.\n" + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                    SessionManager.getInstance().invalidateSession(user.getEmail());
                    new LoginFrame().setVisible(true);
                }
            });

        } catch (SessionException e) {
            LOGGER.log(Level.SEVERE, "Échec de création de session pour : " + user.getEmail(), e);
            JOptionPane.showMessageDialog(null,
                "Erreur de session: Impossible d'initialiser la session.\nVeuillez contacter l'administrateur.",
                "Erreur", JOptionPane.ERROR_MESSAGE);
            new LoginFrame().setVisible(true);
        }
    }

    private void showRoleError(String role) {
        JOptionPane.showMessageDialog(null,
            "Rôle utilisateur invalide ou non reconnu : " + role,
            "Erreur d'authentification", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        configureLinuxWindowIdentityProperties();
        configureLinuxWmClass();
        AppConfig.initialize();
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            LOGGER.log(Level.SEVERE, "Échec de configuration de FlatLaf.", e);
        }
        EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void configureLinuxWmClass() {
        if (!isLinux()) {
            return;
        }

        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Field awtAppClassNameField = resolveAwtAppClassNameField(toolkit.getClass());
            if (awtAppClassNameField != null) {
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(toolkit, APP_WM_CLASS);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Impossible de définir awtAppClassName pour la liaison WM_CLASS.", e);
        }
    }

    private static Field resolveAwtAppClassNameField(Class<?> toolkitClass) {
        Class<?> currentClass = toolkitClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField("awtAppClassName");
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    private static void configureLinuxWindowIdentityProperties() {
        if (!isLinux()) {
            return;
        }

        System.setProperty("sun.awt.X11.appName", APP_WM_CLASS);
        System.setProperty("sun.awt.X11.XWMClass", APP_WM_CLASS);

        String display = System.getenv("DISPLAY");
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        if (display != null && !display.isBlank() && (waylandDisplay == null || waylandDisplay.isBlank())) {
            System.setProperty("awt.toolkit", "sun.awt.X11.XToolkit");
        }
    }

    private static boolean isLinux() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("linux");
    }
}
