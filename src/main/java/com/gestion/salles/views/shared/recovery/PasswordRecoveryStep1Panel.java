package com.gestion.salles.views.shared.recovery;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.User;
import com.gestion.salles.services.EmailService;
import com.gestion.salles.services.VerificationCodeManager;
import com.gestion.salles.utils.AuditLogger;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class PasswordRecoveryStep1Panel extends JPanel {

    private JTextField txtEmail;
    private JButton    btnSendCode;
    private JLabel     lblError;
    private JLabel     lblEmailRequired;

    private final PasswordRecoveryFlow    flow;
    private final VerificationCodeManager codeManager;
    private final UserDAO                 userDAO;
    private final EmailService            emailService;

    public PasswordRecoveryStep1Panel(PasswordRecoveryFlow flow, VerificationCodeManager codeManager) {
        this.flow         = flow;
        this.codeManager  = codeManager;
        this.userDAO      = new UserDAO();
        this.emailService = EmailService.getInstance();
        initComponents();
        reset();
    }

    private void initComponents() {
        setBackground(ThemeConstants.APP_BACKGROUND);
        setLayout(new MigLayout("fill,insets 20", "[grow,fill]", "[grow,fill]"));
        add(createFormPanel());
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 35 45 30 45", "[fill,grow]"));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel lblIcon = new JLabel("🔒");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblIcon, "alignx center,gapy 0 10");

        JLabel lblTitle = new JLabel("Récupération de mot de passe");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 24f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, "alignx center");

        JLabel lblDescription = new JLabel("<html><center>Un code de vérification sera envoyé à votre email.</center></html>");
        lblDescription.setFont(lblDescription.getFont().deriveFont(14f));
        lblDescription.setForeground(ThemeConstants.SECONDARY_TEXT);
        lblDescription.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblDescription, "alignx center,gapy 0 20");

        JPanel emailLabelPanel = new JPanel(new MigLayout("insets 0", "[][]", "[]"));
        emailLabelPanel.setBackground(null);
        JLabel lblEmail = new JLabel("Votre email");
        lblEmail.setFont(lblEmail.getFont().deriveFont(Font.BOLD, 13f));
        lblEmail.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblEmailRequired = UIUtils.createRequiredFieldIndicator();
        emailLabelPanel.add(lblEmail);
        emailLabelPanel.add(lblEmailRequired, "gapleft 3");
        panel.add(emailLabelPanel, "gapy 8");

        txtEmail = UIUtils.createStyledTextField("votre.email@faculte.dz");
        panel.add(txtEmail, "height 45");

        btnSendCode = UIUtils.createPrimaryButton("Envoyer le code");
        btnSendCode.addActionListener(e -> onSendCodeClick());
        panel.add(btnSendCode, "gapy 15,height 45");

        lblError = UIUtils.createErrorLabel();
        panel.add(lblError, "gapy 10,align center");

        JButton btnBack = UIUtils.createLinkButton(flow.getBackNavigationLabel(), "arrow_left");
        btnBack.addActionListener(e -> flow.showSettings());
        panel.add(btnBack, "gapy 10,align center");

        return panel;
    }

    private void onSendCodeClick() {
        clearValidation();

        String email = resolveEmailInput();
        if (email == null) {
            showError("Veuillez saisir un email valide.");
            lblEmailRequired.setVisible(true);
            return;
        }

        if (codeManager.isRateLimited(email)) {
            showError("Trop de tentatives. Réessayez dans 15 minutes.");
            AuditLogger.getInstance().log(email,
                AuditLogger.AuditEvent.RECOVERY_CODE_REQUEST_BLOCKED, "Rate limit active", false);
            return;
        }

        User user = userDAO.findByEmail(email);
        if (user == null || !user.isActif()) {
            codeManager.recordFailedAttempt(email);
            showError("Impossible d'envoyer le code. Contactez l'administrateur.");
            AuditLogger.getInstance().log(email,
                AuditLogger.AuditEvent.RECOVERY_CODE_REQUEST_BLOCKED, "User not found or inactive", false);
            return;
        }
        if (!emailService.isConfigured()) {
            showError("Service d'email non configuré. Contactez l'administrateur.");
            AuditLogger.getInstance().log(email,
                AuditLogger.AuditEvent.RECOVERY_CODE_REQUEST_BLOCKED, "Email service not configured", false);
            return;
        }
        if (codeManager.hasValidCode(email)) {
            long remaining = codeManager.getRemainingMinutes(email);
            showError(String.format("Un code a déjà été envoyé. Expire dans %d min", remaining));
            AuditLogger.getInstance().log(email,
                AuditLogger.AuditEvent.RECOVERY_CODE_REQUEST_BLOCKED,
                "Code already sent: " + remaining + " min remaining", false);
            return;
        }

        flow.setUserEmail(email);
        sendVerificationCode(email, user);
    }

    private void sendVerificationCode(String email, User user) {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("Envoi en cours...");
        btnSendCode.setBackground(ThemeConstants.DISABLED_GREEN);

        new SwingWorker<Boolean, Void>() {
            private String verificationCode;

            @Override
            protected Boolean doInBackground() {
                verificationCode = codeManager.generateCode(email);
                return emailService.sendVerificationCode(email, user.getPrenom(), user.getNom(), verificationCode);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        AuditLogger.getInstance().log(email,
                            AuditLogger.AuditEvent.RECOVERY_CODE_REQUESTED, null, true);
                        flow.showStep2();
                    } else {
                        codeManager.recordFailedAttempt(email);
                        showError("Échec de l'envoi de l'email. Réessayez.");
                        AuditLogger.getInstance().log(email,
                            AuditLogger.AuditEvent.RECOVERY_CODE_REQUESTED, "Email send failed", false);
                    }
                } catch (Exception ex) {
                    codeManager.recordFailedAttempt(email);
                    showError("Erreur lors de l'envoi. Réessayez.");
                    AuditLogger.getInstance().log(email,
                        AuditLogger.AuditEvent.RECOVERY_CODE_REQUESTED, "Exception: " + ex.getMessage(), false);
                } finally {
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText("Envoyer le code");
                    btnSendCode.setBackground(ThemeConstants.PRIMARY_GREEN);
                }
            }
        }.execute();
    }

    public void reset() {
        clearValidation();
        String email = flow.getUserEmail();
        if (email != null && !email.isBlank()) {
            txtEmail.setText(email);
            txtEmail.setEditable(false);
        } else {
            txtEmail.setText("");
            txtEmail.setEditable(true);
        }
        btnSendCode.setEnabled(true);
        btnSendCode.setText("Envoyer le code");
        btnSendCode.setBackground(ThemeConstants.PRIMARY_GREEN);
    }

    private String resolveEmailInput() {
        String email = flow.getUserEmail();
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        String entered = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        if (entered.isEmpty() || !UIUtils.isValidEmail(entered)) {
            return null;
        }
        return entered;
    }

    public void showInfoMessage(String message) {
        lblError.setForeground(ThemeConstants.SECONDARY_TEXT);
        lblError.setText(message);
    }

    private void showError(String message) {
        UIUtils.showTemporaryErrorMessage(lblError, message, 5000);
    }

    private void clearValidation() {
        lblError.setText("");
        lblError.setForeground(ThemeConstants.ERROR_RED);
        lblEmailRequired.setVisible(false);
    }
}
