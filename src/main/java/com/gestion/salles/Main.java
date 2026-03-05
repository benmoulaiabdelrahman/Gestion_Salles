package com.gestion.salles;

/******************************************************************************
 * Main.java
 *
 * Application entry point. Bootstraps the logging system (reads
 * logging.properties from the classpath and creates the logs/ directory),
 * initialises AppConfig and FlatLaf, verifies the database connection and
 * schema, then either performs a silent token-based auto-login (Remember Me)
 * or opens the LoginFrame. On auto-login success a fresh session is created
 * in the DB before the role-appropriate dashboard is displayed.
 ******************************************************************************/

import com.gestion.salles.database.DatabaseConnection;
import com.gestion.salles.utils.AppConfig;
import com.gestion.salles.utils.SessionManager;
import com.gestion.salles.utils.SessionException;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.utils.TokenStorage;
import com.gestion.salles.services.AuthService;
import com.gestion.salles.models.User;
import com.gestion.salles.views.Admin.Dashboard;
import com.gestion.salles.views.ChefDepartement.DashboardChef;
import com.gestion.salles.views.Login.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String APP_WM_CLASS = "com-gestion-salles-Main";

    public static void main(String[] args) {
        configureLinuxWindowIdentityProperties();
        configureLinuxWmClass();
        initLogging();
        AppConfig.initialize();
        configureFlatLaf();

        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Démarrage du système de gestion des salles...");
                LOGGER.info("Test de la connexion à la base de données...");

                DatabaseConnection dbConnection = DatabaseConnection.getInstance();

                if (!dbConnection.verifyDatabaseAccess()) {
                    LOGGER.severe("Échec de la connexion à la base de données.");
                    showErrorDialog("Erreur de connexion", "Impossible de se connecter à la base de données.");
                    System.exit(1);
                    return;
                }

                LOGGER.info("Connexion à la base de données réussie.");

                if (!dbConnection.isDatabaseInitialized()) {
                    LOGGER.severe("Schéma de la base de données introuvable.");
                    showErrorDialog("Erreur de schéma", "Schéma de base de données non initialisé. Veuillez exécuter le script SQL.");
                    System.exit(1);
                    return;
                }

                LOGGER.info("Schéma de la base de données vérifié.");

                String token = TokenStorage.loadToken();
                if (token != null) {
                    AuthService.AuthenticationResult result = AuthService.getInstance().authenticateWithToken(token);
                    if (result.isSuccess()) {
                        try {
                            String newSessionToken = SessionManager.getInstance().createSession(result.getUser().getEmail());
                            if (newSessionToken != null) {
                                LOGGER.info("Auto-login réussi pour : " + result.getUser().getEmail());
                                openDashboard(result.getUser(), newSessionToken);
                                return;
                            }
                        } catch (SessionException e) {
                            LOGGER.log(Level.WARNING, "Échec de création de session pour l'auto-login : " + result.getUser().getEmail(), e);
                        }
                    } else {
                        LOGGER.warning("Auto-login échoué : " + result.getMessage());
                    }
                }

                new LoginFrame().setVisible(true);
                LOGGER.info("Application lancée, affichage de la fenêtre de connexion.");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors du démarrage de l'application", e);
                showErrorDialog("Erreur de l'application", "Une erreur fatale est survenue : " + e.getMessage());
                System.exit(1);
            }
        });
    }

    private static void initLogging() {
        File logsDir = new File("logs");
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            System.err.println("Impossible de créer le répertoire de logs : " + logsDir.getAbsolutePath());
        }

        try (java.io.InputStream is = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                LOGGER.info("Configuration de logging chargée depuis logging.properties.");
            } else {
                LOGGER.warning("logging.properties introuvable sur le classpath. Configuration de logging par défaut utilisée.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erreur lors du chargement de logging.properties. Configuration par défaut utilisée.", e);
        }
    }

    private static void openDashboard(User user, String sessionToken) {
        if (user == null || sessionToken == null) {
            LOGGER.severe("openDashboard appelé avec un utilisateur ou un token null.");
            return;
        }

        SessionContext.setCurrentUser(user.getIdUtilisateur(), user.getEmail(), sessionToken);
        switch (user.getRole()) {
            case Admin:
                new Dashboard(user, sessionToken).setVisible(true);
                break;
            case Chef_Departement:
                new DashboardChef(user, sessionToken).setVisible(true);
                break;
            case Enseignant:
                new com.gestion.salles.views.Enseignant.Dashboard(user, sessionToken).setVisible(true);
                break;
            default:
                showErrorDialog("Rôle Inconnu", "Le rôle de l'utilisateur n'est pas reconnu.");
                SessionManager.getInstance().invalidateSession(user.getEmail());
                new LoginFrame().setVisible(true);
                break;
        }
    }

    private static void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static void configureFlatLaf() {
        try {
            FlatRobotoFont.install();

            UIManager.put("defaultFont",              new Font("Arial", Font.PLAIN, 13));
            UIManager.put("TitlePane.showIconifyButton",  Boolean.FALSE);
            UIManager.put("TitlePane.showMaximizeButton", Boolean.FALSE);

            FlatLightLaf.setup();

            UIManager.put("Component.focusWidth",      1);
            UIManager.put("Component.innerFocusWidth", 1);
            UIManager.put("ScrollBar.thumbArc",        999);
            UIManager.put("ScrollBar.thumbInsets",     new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.selectedBackground", java.awt.Color.white);

            LOGGER.info("FlatLaf configuré avec succès avec la police Roboto.");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de configurer FlatLaf, utilisation du thème par défaut.", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Impossible d'appliquer le Look and Feel système.", ex);
            }
        }
    }

    private static void configureLinuxWmClass() {
        if (!isLinux()) {
            return;
        }

        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Field awtAppClassNameField = resolveAwtAppClassNameField(toolkit.getClass());
            if (awtAppClassNameField != null && awtAppClassNameField.trySetAccessible()) {
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
