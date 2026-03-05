package com.gestion.salles.views.shared.settings;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.User;
import com.gestion.salles.services.AuthService;
import com.gestion.salles.services.VerificationCodeManager;
import com.gestion.salles.utils.AnimatedIconButton;
import com.gestion.salles.utils.AuditLogger;
import com.gestion.salles.utils.PasswordFormHelper;
import com.gestion.salles.utils.PasswordStrengthChecker.StrengthStatus;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.utils.SessionManager;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Login.PasswordStrengthMeter;
import com.gestion.salles.views.shared.dashboard.DashboardFrameBase;
import com.gestion.salles.views.shared.recovery.PasswordRecoveryContainer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class AccountSettingsPanel extends JPanel {

    private static final int MAX_PASSWORD_ATTEMPTS = 5;

    private final DashboardFrameBase parentDashboard;
    private final User currentUser;
    private final AuthService authService;
    private final Runnable backToDashboard;

    private CardLayout mainCardLayout;
    private JPanel mainContentCards;

    private JPasswordField txtCurrentPassword;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmNewPassword;
    private JButton btnChangePassword;
    private JLabel lblErrorMessage;
    private JLabel lblStrengthStatus;
    private PasswordStrengthMeter strengthMeter;

    private PasswordRecoveryContainer passwordRecoveryContainer;

    public AccountSettingsPanel(DashboardFrameBase parentDashboard, Runnable backToDashboard) {
        this.parentDashboard = parentDashboard;
        this.currentUser = parentDashboard.getCurrentUser();
        this.authService = AuthService.getInstance();
        this.backToDashboard = backToDashboard;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.APP_BACKGROUND);

        JPanel headerPanel = new JPanel(new MigLayout("insets 10 20 10 20", "[]push[]", "[]"));
        headerPanel.setOpaque(false);

        AnimatedIconButton btnBack = new AnimatedIconButton(
            new ImageIcon(getClass().getResource("/icons/back.png")), 30);
        btnBack.addActionListener(e -> {
            if (passwordRecoveryContainer.isShowingPasswordRecovery()) {
                passwordRecoveryContainer.goBack();
            } else if (UIUtils.isCardCurrentlyVisible(mainContentCards, "FeedbackPanel")) {
                showMainSettings();
            } else {
                backToDashboard.run();
            }
        });
        headerPanel.add(btnBack, "align left");

        JLabel lblTitle = new JLabel("Paramètres du Compte");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        headerPanel.add(lblTitle, "align center, wrap");

        add(headerPanel, BorderLayout.NORTH);

        mainCardLayout = new CardLayout();
        mainContentCards = new JPanel(mainCardLayout);
        mainContentCards.setOpaque(false);
        mainContentCards.add(createFormPanel(), "MainSettings");

        passwordRecoveryContainer = new PasswordRecoveryContainer(
            this::showMainSettings, currentUser.getEmail(), authService, VerificationCodeManager.getInstance());
        mainContentCards.add(passwordRecoveryContainer, "PasswordRecovery");

        add(mainContentCards, BorderLayout.CENTER);
        mainCardLayout.show(mainContentCards, "MainSettings");
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 35 45 30 45", "fill,300:380"));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel lblSectionTitle = new JLabel("Changer le mot de passe");
        lblSectionTitle.setFont(lblSectionTitle.getFont().deriveFont(Font.BOLD, 18f));
        lblSectionTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblSectionTitle, "alignx center, gapy 0 20");

        panel.add(new JLabel("Mot de passe actuel"), "gapy 8");
        txtCurrentPassword = UIUtils.createStyledPasswordField("Entrez votre mot de passe actuel", true, true);
        panel.add(txtCurrentPassword, "height 45");

        panel.add(new JLabel("Nouveau mot de passe"), "gapy 8");
        txtNewPassword = UIUtils.createStyledPasswordField("Entrez votre nouveau mot de passe", true, true);
        txtNewPassword.getDocument().addDocumentListener(
            (PasswordFormHelper.SimpleDocumentListener) e ->
                PasswordFormHelper.checkPasswordStrength(txtNewPassword, strengthMeter, lblStrengthStatus));
        panel.add(txtNewPassword, "height 45");

        JPanel strengthPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]8[pref!]"));
        strengthPanel.setOpaque(false);
        strengthMeter = new PasswordStrengthMeter();
        strengthPanel.add(strengthMeter, "growx, pushx, height 10");
        lblStrengthStatus = new JLabel("Mot de passe faible");
        lblStrengthStatus.setFont(lblStrengthStatus.getFont().deriveFont(Font.BOLD, 11f));
        lblStrengthStatus.setForeground(ThemeConstants.ERROR_RED);
        strengthPanel.add(lblStrengthStatus);
        panel.add(strengthPanel, "gapy 5 0, growx");

        panel.add(new JLabel("Confirmer le nouveau mot de passe"), "gapy 8");
        txtConfirmNewPassword = UIUtils.createStyledPasswordField("Confirmez votre nouveau mot de passe", true, true);
        txtConfirmNewPassword.getDocument().addDocumentListener(
            (PasswordFormHelper.SimpleDocumentListener) e ->
                PasswordFormHelper.updateButtonState(btnChangePassword, txtNewPassword, txtConfirmNewPassword));
        panel.add(txtConfirmNewPassword, "height 45");

        btnChangePassword = UIUtils.createPrimaryButton("Changer le mot de passe");
        btnChangePassword.setEnabled(false);
        btnChangePassword.addActionListener(this::onChangePasswordClick);
        panel.add(btnChangePassword, "gapy 15,height 45");

        JSeparator separator = new JSeparator();
        separator.setForeground(ThemeConstants.DEFAULT_BORDER);
        panel.add(separator, "gapy 20, growx");

        JLabel lblForgotTitle = new JLabel("Mot de passe oublié ?");
        lblForgotTitle.setFont(lblForgotTitle.getFont().deriveFont(Font.BOLD, 16f));
        lblForgotTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblForgotTitle, "alignx center, gapy 10 10");

        JLabel lblForgotDesc = new JLabel("Si vous avez oublié votre mot de passe, vous pouvez le réinitialiser.");
        lblForgotDesc.setFont(lblForgotDesc.getFont().deriveFont(12f));
        lblForgotDesc.setForeground(ThemeConstants.SECONDARY_TEXT);
        panel.add(lblForgotDesc, "alignx center, wrap");

        JButton btnForgotPassword = UIUtils.createPrimaryButton("Réinitialiser le mot de passe");
        btnForgotPassword.addActionListener(this::onForgotPasswordClick);
        panel.add(btnForgotPassword, "gapy 10,height 45");

        lblErrorMessage = new JLabel("");
        lblErrorMessage.setForeground(ThemeConstants.ERROR_RED);
        lblErrorMessage.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblErrorMessage, "gapy 10,align center");

        return panel;
    }

    private void onChangePasswordClick(ActionEvent e) {
        showError("");

        if (SessionContext.getPasswordAttempts() >= MAX_PASSWORD_ATTEMPTS) {
            showError("Trop de tentatives incorrectes. Veuillez réinitialiser votre mot de passe.");
            btnChangePassword.setEnabled(false);
            AuditLogger.getInstance().log(currentUser.getEmail(),
                AuditLogger.AuditEvent.PASSWORD_CHANGE_ATTEMPT_BLOCKED, "Lockout", false);
            return;
        }

        char[] currentPass = txtCurrentPassword.getPassword();
        char[] newPass = txtNewPassword.getPassword();
        char[] confirmPass = txtConfirmNewPassword.getPassword();

        try {
            if (currentPass.length == 0 || newPass.length == 0 || confirmPass.length == 0) {
                showError("Veuillez remplir tous les champs.");
                return;
            }
            if (!Arrays.equals(newPass, confirmPass)) {
                showError("Les nouveaux mots de passe ne correspondent pas.");
                return;
            }
            StrengthStatus status = PasswordFormHelper.checkPasswordStrength(
                txtNewPassword, strengthMeter, lblStrengthStatus);
            if (status != StrengthStatus.MEDIUM && status != StrengthStatus.STRONG) {
                showError("Le nouveau mot de passe n'est pas assez fort (" +
                    status.getMessage() + ", minimum Moyen requis).");
                return;
            }

            final char[] currentPassForVerification = Arrays.copyOf(currentPass, currentPass.length);
            final char[] newPassForUpdate = Arrays.copyOf(newPass, newPass.length);

            btnChangePassword.setEnabled(false);
            btnChangePassword.setText("Vérification...");
            btnChangePassword.setBackground(ThemeConstants.DISABLED_GREEN);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return authService.verifyPassword(currentUser.getEmail(), currentPassForVerification);
                }

                @Override
                protected void done() {
                    Arrays.fill(currentPassForVerification, '\0');

                    btnChangePassword.setEnabled(true);
                    btnChangePassword.setText("Changer le mot de passe");
                    btnChangePassword.setBackground(ThemeConstants.PRIMARY_GREEN);

                    try {
                        boolean passwordValid = get();
                        if (!passwordValid) {
                            Arrays.fill(newPassForUpdate, '\0');
                            int attempts = SessionContext.incrementPasswordAttempts();
                            int remaining = MAX_PASSWORD_ATTEMPTS - attempts;
                            showError(remaining > 0
                                ? "Mot de passe actuel incorrect. " + remaining + " tentative(s) restante(s)."
                                : "Trop de tentatives incorrectes. Veuillez réinitialiser votre mot de passe.");
                            if (remaining <= 0) btnChangePassword.setEnabled(false);
                            AuditLogger.getInstance().log(currentUser.getEmail(),
                                AuditLogger.AuditEvent.PASSWORD_CHANGE_FAILURE, "Wrong current password", false);
                            return;
                        }

                        updatePassword(newPassForUpdate);
                    } catch (Exception ex) {
                        Arrays.fill(newPassForUpdate, '\0');
                        showError("Erreur lors de la vérification. Veuillez réessayer.");
                        AuditLogger.getInstance().log(currentUser.getEmail(),
                            AuditLogger.AuditEvent.PASSWORD_CHANGE_FAILURE,
                            "Verification exception: " + ex.getMessage(), false);
                    }
                }
            }.execute();
        } finally {
            Arrays.fill(currentPass, '\0');
            Arrays.fill(newPass, '\0');
            Arrays.fill(confirmPass, '\0');
        }
    }

    private void onForgotPasswordClick(ActionEvent e) {
        mainCardLayout.show(mainContentCards, "PasswordRecovery");
        passwordRecoveryContainer.showPasswordRecovery();
    }

    private void updatePassword(char[] newPassword) {
        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Modification...");
        btnChangePassword.setBackground(ThemeConstants.DISABLED_GREEN);
        txtCurrentPassword.setEnabled(false);
        txtNewPassword.setEnabled(false);
        txtConfirmNewPassword.setEnabled(false);
        lblErrorMessage.setText("");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                boolean updated = authService.updatePassword(currentUser.getEmail(), newPassword);
                if (!updated) return false;
                boolean cleared = new UserDAO().clearMustChangePassword(currentUser.getIdUtilisateur());
                if (cleared) currentUser.setMustChangePassword(false);
                return cleared;
            }

            @Override
            protected void done() {
                Arrays.fill(newPassword, '\0');
                txtCurrentPassword.setEnabled(true);
                txtNewPassword.setEnabled(true);
                txtConfirmNewPassword.setEnabled(true);

                try {
                    boolean success = get();
                    if (success) {
                        SessionContext.resetPasswordAttempts();
                        replaceMainContentPanel(PasswordFormHelper.createSuccessPanel(
                            "Votre mot de passe a été changé avec succès.<br>Veuillez vous reconnecter avec le nouveau mot de passe.",
                            "check.png",
                            () -> {
                                SessionManager.getInstance().invalidateSession(currentUser.getEmail());
                                parentDashboard.stopForLogout();
                                UIUtils.logout(parentDashboard);
                            }));
                        AuditLogger.getInstance().log(currentUser.getEmail(),
                            AuditLogger.AuditEvent.PASSWORD_CHANGE_SUCCESS, null, true);
                    } else {
                        replaceMainContentPanel(PasswordFormHelper.createErrorPanel(
                            "Échec de la mise à jour. Veuillez réessayer.", "x.png",
                            ev -> resetToMainSettings()));
                        AuditLogger.getInstance().log(currentUser.getEmail(),
                            AuditLogger.AuditEvent.PASSWORD_CHANGE_FAILURE, "Update failed", false);
                    }
                } catch (Exception ex) {
                    replaceMainContentPanel(PasswordFormHelper.createErrorPanel(
                        "Erreur lors de la mise à jour: " + ex.getMessage(), "x.png",
                        ev -> resetToMainSettings()));
                    AuditLogger.getInstance().log(currentUser.getEmail(),
                        AuditLogger.AuditEvent.PASSWORD_CHANGE_FAILURE,
                        "Exception during update: " + ex.getMessage(), false);
                }
            }
        }.execute();
    }

    private void resetToMainSettings() {
        mainCardLayout.show(mainContentCards, "MainSettings");
        lblErrorMessage.setText("");
        txtCurrentPassword.setText("");
        txtNewPassword.setText("");
        txtConfirmNewPassword.setText("");
        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Changer le mot de passe");
        PasswordFormHelper.checkPasswordStrength(txtNewPassword, strengthMeter, lblStrengthStatus);
        lblErrorMessage.setText("");
    }

    private void replaceMainContentPanel(JPanel newPanel) {
        for (Component c : mainContentCards.getComponents()) {
            if ("FeedbackPanel".equals(c.getName())) {
                mainContentCards.remove(c);
                break;
            }
        }
        newPanel.setName("FeedbackPanel");
        mainContentCards.add(newPanel, "FeedbackPanel");
        mainCardLayout.show(mainContentCards, "FeedbackPanel");
        mainContentCards.revalidate();
        mainContentCards.repaint();
    }

    private void showError(String message) {
        if (lblErrorMessage != null) lblErrorMessage.setText(message);
    }

    public void showMainSettings() {
        mainCardLayout.show(mainContentCards, "MainSettings");
        passwordRecoveryContainer.reset();
        txtCurrentPassword.setText("");
        txtNewPassword.setText("");
        txtConfirmNewPassword.setText("");
        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Changer le mot de passe");
        PasswordFormHelper.checkPasswordStrength(txtNewPassword, strengthMeter, lblStrengthStatus);
        lblErrorMessage.setText("");
    }
}
