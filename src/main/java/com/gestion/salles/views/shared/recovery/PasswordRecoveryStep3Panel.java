package com.gestion.salles.views.shared.recovery;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.services.AuthService;
import com.gestion.salles.utils.AuditLogger;
import com.gestion.salles.utils.PasswordFormHelper;
import com.gestion.salles.utils.PasswordStrengthChecker.StrengthStatus;
import com.gestion.salles.utils.SessionManager;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Login.PasswordStrengthMeter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Logger;

public class PasswordRecoveryStep3Panel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(PasswordRecoveryStep3Panel.class.getName());

    private JPasswordField       txtNewPassword;
    private JPasswordField       txtConfirmPassword;
    private JButton              btnChangePassword;
    private JLabel               lblError;
    private JLabel               lblStrengthStatus;
    private PasswordStrengthMeter strengthMeter;
    private JPanel               currentContentPanel;
    private JLabel               lblDescription;

    private final PasswordRecoveryFlow flow;
    private final AuthService          authService;

    public PasswordRecoveryStep3Panel(PasswordRecoveryFlow flow, AuthService authService) {
        this.flow = flow;
        this.authService = authService;

        setBackground(ThemeConstants.APP_BACKGROUND);
        setLayout(new MigLayout("fill,insets 20", "[grow,fill]", "[grow,fill]"));

        initComponents();
        PasswordFormHelper.checkPasswordStrength(txtNewPassword, strengthMeter, lblStrengthStatus);
    }

    private void initComponents() {
        currentContentPanel = createFormPanel();
        add(currentContentPanel, "center");
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 35 45 30 45", "[fill,grow]"));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel lblIcon = new JLabel(PasswordFormHelper.getIcon("key.png", 32));
        if (lblIcon.getIcon() == null) {
            lblIcon = new JLabel("🔑");
            lblIcon.setFont(lblIcon.getFont().deriveFont(Font.PLAIN, 32f));
        }
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblIcon, "alignx center,gapy 0 10");

        JLabel lblTitle = new JLabel("Nouveau mot de passe");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 24f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, "alignx center");

        lblDescription = new JLabel();
        updateEmailDescription();
        lblDescription.setFont(lblDescription.getFont().deriveFont(13f));
        lblDescription.setForeground(ThemeConstants.SECONDARY_TEXT);
        lblDescription.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblDescription, "alignx center,gapy 0 20");

        JLabel lblNewPassword = new JLabel("Nouveau mot de passe");
        lblNewPassword.setFont(lblNewPassword.getFont().deriveFont(Font.BOLD, 13f));
        lblNewPassword.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblNewPassword, "gapy 8");

        txtNewPassword = new JPasswordField();
        txtNewPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Minimum 8 caractères, majuscule, chiffre");
        txtNewPassword.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;focusWidth:1;innerFocusWidth:0;focusColor: " + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) +
                ";showRevealButton: true;showCapsLock: true");
        panel.add(txtNewPassword, "height 40,growx");

        JPanel strengthPanel = new JPanel(new MigLayout("fillx,insets 0", "[grow]8[pref!]"));
        strengthPanel.setOpaque(false);
        strengthMeter = new PasswordStrengthMeter();
        strengthPanel.add(strengthMeter, "growx,pushx,height 10");
        lblStrengthStatus = new JLabel("Mot de passe faible");
        lblStrengthStatus.setFont(lblStrengthStatus.getFont().deriveFont(Font.BOLD, 11f));
        lblStrengthStatus.setForeground(ThemeConstants.ERROR_RED);
        strengthPanel.add(lblStrengthStatus);
        panel.add(strengthPanel, "gapy 5 0,growx");

        txtNewPassword.getDocument().addDocumentListener(
            (PasswordFormHelper.SimpleDocumentListener) e ->
                PasswordFormHelper.checkPasswordStrength(txtNewPassword, strengthMeter, lblStrengthStatus));

        JLabel lblConfirmPassword = new JLabel("Confirmer le mot de passe");
        lblConfirmPassword.setFont(lblConfirmPassword.getFont().deriveFont(Font.BOLD, 13f));
        lblConfirmPassword.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblConfirmPassword, "gapy 15");

        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Retapez le mot de passe");
        txtConfirmPassword.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;focusWidth:1;innerFocusWidth:0;focusColor: " + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) +
                ";showRevealButton: true;showCapsLock: true");
        txtConfirmPassword.addActionListener(this::onConfirmPasswordAction);
        txtConfirmPassword.getDocument().addDocumentListener(
            (PasswordFormHelper.SimpleDocumentListener) e ->
                PasswordFormHelper.updateButtonState(btnChangePassword, txtNewPassword, txtConfirmPassword));
        panel.add(txtConfirmPassword, "height 40,growx");

        btnChangePassword = UIUtils.createPrimaryButton("Changer le mot de passe");
        btnChangePassword.addActionListener(this::onChangePasswordClick);
        btnChangePassword.setEnabled(false);
        panel.add(btnChangePassword, "gapy 25,height 45,growx");

        lblError = UIUtils.createErrorLabel();
        panel.add(lblError, "gapy 10");

        JButton btnRestart = UIUtils.createLinkButton("Recommencer", "arrow_left");
        btnRestart.addActionListener(evt -> flow.showStep1());
        panel.add(btnRestart, "gapy 5");

        return panel;
    }

    private void onConfirmPasswordAction(java.awt.event.ActionEvent e) {
        if (btnChangePassword != null && btnChangePassword.isEnabled()) {
            onChangePasswordClick(e);
        }
    }

    private void onChangePasswordClick(java.awt.event.ActionEvent e) {
        showError("");

        char[] newPassChars     = txtNewPassword.getPassword();
        char[] confirmPassChars = txtConfirmPassword.getPassword();

        try {
            if (newPassChars.length == 0 || confirmPassChars.length == 0) {
                showError("Veuillez remplir les deux champs de mot de passe.");
                return;
            }
            if (!Arrays.equals(newPassChars, confirmPassChars)) {
                showError("Les mots de passe ne correspondent pas.");
                return;
            }
            StrengthStatus status = PasswordFormHelper.checkPasswordStrength(
                txtNewPassword, strengthMeter, lblStrengthStatus);
            if (status != StrengthStatus.MEDIUM && status != StrengthStatus.STRONG) {
                showError("Le mot de passe n'est pas assez fort (" + status.getMessage() + ", minimum Moyen requis).");
                return;
            }
            updatePassword(Arrays.copyOf(newPassChars, newPassChars.length));
        } finally {
            Arrays.fill(newPassChars, '\0');
            Arrays.fill(confirmPassChars, '\0');
        }
    }

    private void updatePassword(char[] newPassword) {
        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Modification...");
        btnChangePassword.setBackground(ThemeConstants.DISABLED_GREEN);
        txtNewPassword.setEnabled(false);
        txtConfirmPassword.setEnabled(false);
        lblError.setText("");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return authService.updatePassword(getUserEmail(), newPassword);
            }

            @Override
            protected void done() {
                Arrays.fill(newPassword, '\0');
                txtNewPassword.setEnabled(true);
                txtConfirmPassword.setEnabled(true);

                try {
                    if (get()) {
                        replaceContentPanel(PasswordFormHelper.createSuccessPanel(
                            "Votre mot de passe a été changé avec succès.<br>Veuillez vous reconnecter avec le nouveau mot de passe.",
                            "check.png",
                            () -> {
                                SessionManager.getInstance().invalidateSession(getUserEmail());
                                JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, PasswordRecoveryStep3Panel.this);
                                invokeIfPresent(parentFrame, "stopForLogout");
                                invokeIfPresent(parentFrame, "stopSessionValidationTimer");
                                UIUtils.logout(parentFrame);
                            }));
                        AuditLogger.getInstance().log(getUserEmail(), AuditLogger.AuditEvent.PASSWORD_RECOVERY_SUCCESS, null, true);
                    } else {
                        replaceContentPanel(PasswordFormHelper.createErrorPanel(
                            "Échec de la mise à jour. Veuillez réessayer.", "x.png",
                            ev -> flow.showStep3()));
                        AuditLogger.getInstance().log(getUserEmail(),
                            AuditLogger.AuditEvent.PASSWORD_RECOVERY_FAILURE, "Update failed", false);
                    }
                } catch (Exception ex) {
                    replaceContentPanel(PasswordFormHelper.createErrorPanel(
                        "Erreur lors de la mise à jour. Veuillez réessayer.", "x.png",
                        ev -> flow.showStep3()));
                    AuditLogger.getInstance().log(getUserEmail(),
                        AuditLogger.AuditEvent.PASSWORD_RECOVERY_FAILURE, "Exception: " + ex.getMessage(), false);
                }
            }
        }.execute();
    }

    private void invokeIfPresent(Object target, String methodName) {
        if (target == null) return;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.invoke(target);
        } catch (Exception ignored) {
            // optional hook
        }
    }

    public void refreshEmailDisplay() {
        updateEmailDescription();
    }

    private void updateEmailDescription() {
        String email = getUserEmail();
        String emailMarkup = (email == null || email.isBlank())
            ? "votre compte"
            : "<b>" + email + "</b>";
        lblDescription.setText("<html><center>Définissez un nouveau mot de passe fort<br>pour votre compte : " + emailMarkup + "</center></html>");
    }

    private String getUserEmail() {
        String email = flow.getUserEmail();
        return email != null ? email : "";
    }

    private void replaceContentPanel(JPanel newPanel) {
        removeAll();
        currentContentPanel = newPanel;
        add(currentContentPanel, "center");
        revalidate();
        repaint();
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setForeground(ThemeConstants.ERROR_RED);
        }
        PasswordFormHelper.updateButtonState(btnChangePassword, txtNewPassword, txtConfirmPassword);
    }
}
