package com.gestion.salles.views.shared.recovery;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.services.VerificationCodeManager;
import com.gestion.salles.utils.AuditLogger;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordRecoveryStep2Panel extends JPanel {

    private static final Logger LOGGER              = Logger.getLogger(PasswordRecoveryStep2Panel.class.getName());
    private static final int    CODE_LENGTH         = 6;
    private static final int    WARN_ORANGE_SECONDS = 180;
    private static final int    WARN_RED_SECONDS    = 60;
    private static final int    SUCCESS_DELAY_MS    = 800;
    private static final Color  COLOR_ORANGE        = new Color(234, 88, 12);

    private JTextField[] codeFields;
    private JButton      btnVerify;
    private JLabel       lblError;
    private JLabel       lblTimer;
    private JLabel       lblEmailDisplay;

    private final PasswordRecoveryFlow    flow;
    private final VerificationCodeManager codeManager;

    private Timer countdownTimer;
    private int   remainingSeconds;

    public PasswordRecoveryStep2Panel(PasswordRecoveryFlow flow, VerificationCodeManager codeManager) {
        this.flow        = flow;
        this.codeManager = codeManager;
        initComponents();
        initializeCountdown();
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

        JLabel lblIcon = new JLabel("✉️");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblIcon, "alignx center,gapy 0 10");

        JLabel lblTitle = new JLabel("Vérification");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 24f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, "alignx center");

        JLabel lblDescription = new JLabel("<html><center>Entrez le code de vérification<br>envoyé à</center></html>");
        lblDescription.setFont(lblDescription.getFont().deriveFont(14f));
        lblDescription.setForeground(ThemeConstants.SECONDARY_TEXT);
        lblDescription.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblDescription, "alignx center,gapy 0 5");

        lblEmailDisplay = new JLabel(getUserEmail());
        lblEmailDisplay.setFont(lblEmailDisplay.getFont().deriveFont(Font.BOLD, 14f));
        lblEmailDisplay.setForeground(ThemeConstants.PRIMARY_GREEN);
        lblEmailDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblEmailDisplay, "alignx center,gapy 0 20");

        JLabel lblCodeLabel = new JLabel("Code de vérification");
        lblCodeLabel.setFont(lblCodeLabel.getFont().deriveFont(Font.BOLD, 13f));
        lblCodeLabel.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblCodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel lblCodeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        lblCodeWrapper.setOpaque(false);
        lblCodeWrapper.add(lblCodeLabel);
        panel.add(lblCodeWrapper, "gapy 8");

        JPanel codeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        codeWrapper.setOpaque(false);
        codeWrapper.add(createCodeInputPanel());
        panel.add(codeWrapper, "gapy 5");

        lblTimer = new JLabel("Chargement...");
        lblTimer.setFont(lblTimer.getFont().deriveFont(Font.BOLD, 12f));
        lblTimer.setForeground(ThemeConstants.SECONDARY_TEXT);
        lblTimer.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTimer, "alignx center,gapy 10");

        btnVerify = UIUtils.createPrimaryButton("Vérifier le code");
        btnVerify.addActionListener(e -> onVerifyClick());
        panel.add(btnVerify, "gapy 15,height 45,growx");

        lblError = UIUtils.createErrorLabel();
        JPanel errorWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        errorWrapper.setOpaque(false);
        errorWrapper.add(lblError);
        panel.add(errorWrapper, "gapy 10");

        JPanel resendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        resendPanel.setBackground(null);
        JLabel lblResendText = new JLabel("Vous n'avez pas reçu le code ?");
        lblResendText.setFont(lblResendText.getFont().deriveFont(13f));
        lblResendText.setForeground(ThemeConstants.MUTED_TEXT);
        JButton btnResend = UIUtils.createLinkButton("Renvoyer");
        btnResend.addActionListener(e -> onResendClick());
        btnResend.setMargin(new Insets(0, 0, 0, 0));
        resendPanel.add(lblResendText);
        resendPanel.add(btnResend);
        panel.add(resendPanel, "gapy 5");

        JButton btnBack = UIUtils.createLinkButton(flow.getBackNavigationLabel(), "arrow_left");
        btnBack.addActionListener(e -> {
            stopCountdown();
            flow.showSettings();
        });
        panel.add(btnBack, "gapy 5");

        return panel;
    }

    private JPanel createCodeInputPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0,gap 6", "[45!][45!][45!][45!][45!][45!]", "[]"));
        panel.setBackground(ThemeConstants.CARD_WHITE);

        codeFields = new JTextField[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            codeFields[i] = createCodeField(i);
            panel.add(codeFields[i], "width 45!,height 45!");
        }
        return panel;
    }

    private JTextField createCodeField(int index) {
        JTextField field = new JTextField();
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(field.getFont().deriveFont(Font.BOLD, 18f));
        field.setBackground(ThemeConstants.CARD_WHITE);
        field.setForeground(ThemeConstants.PRIMARY_TEXT);
        field.setCaretColor(ThemeConstants.PRIMARY_TEXT);
        field.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;" +
                "borderWidth:2;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + UIUtils.colorToHex(ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.FOCUS_BORDER));

        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
                if (string != null && string.matches("\\d") && fb.getDocument().getLength() == 0) {
                    super.insertString(fb, offset, string, attr);
                    focusNextField(index);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
                if (text != null && text.matches("\\d") && fb.getDocument().getLength() <= 1) {
                    super.replace(fb, offset, length, text, attrs);
                    focusNextField(index);
                }
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE:
                        if (field.getText().isEmpty() && index > 0) {
                            codeFields[index - 1].requestFocus();
                            codeFields[index - 1].setText("");
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                        if (index == CODE_LENGTH - 1 && !field.getText().isEmpty()) {
                            btnVerify.doClick();
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        if (index > 0) codeFields[index - 1].requestFocus();
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (index < CODE_LENGTH - 1) codeFields[index + 1].requestFocus();
                        break;
                    default:
                        if ((e.isControlDown() || e.isMetaDown()) && e.getKeyCode() == KeyEvent.VK_V) {
                            e.consume();
                            handlePaste();
                        }
                }
            }
        });

        return field;
    }

    private void focusNextField(int currentIndex) {
        SwingUtilities.invokeLater(() -> {
            if (currentIndex < CODE_LENGTH - 1) codeFields[currentIndex + 1].requestFocus();
            else btnVerify.requestFocus();
        });
    }

    private void handlePaste() {
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (text != null && text.matches("\\d{" + CODE_LENGTH + "}")) {
                for (int i = 0; i < CODE_LENGTH; i++) {
                    codeFields[i].setText(String.valueOf(text.charAt(i)));
                }
                codeFields[CODE_LENGTH - 1].requestFocus();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to read clipboard content", ex);
        }
    }

    private String getEnteredCode() {
        StringBuilder code = new StringBuilder();
        for (JTextField field : codeFields) code.append(field.getText());
        return code.toString();
    }

    private void clearCodeFields() {
        for (JTextField field : codeFields) field.setText("");
        codeFields[0].requestFocus();
    }

    private static String formatTimer(int seconds) {
        return String.format("Code expire dans : %d:%02d", seconds / 60, seconds % 60);
    }

    private void onVerifyClick() {
        clearError();
        String code = getEnteredCode();
        if (getUserEmail().isBlank()) {
            showError("Email non défini. Retournez à l'étape précédente.");
            return;
        }

        if (code.length() != CODE_LENGTH) {
            showError("Veuillez entrer le code complet (" + CODE_LENGTH + " chiffres)");
            codeFields[0].requestFocus();
            return;
        }
        if (remainingSeconds <= 0) {
            applyExpiredState();
            return;
        }
        verifyCode(code);
    }

    private void verifyCode(String code) {
        btnVerify.setEnabled(false);
        btnVerify.setText("Vérification...");
        btnVerify.setBackground(ThemeConstants.DISABLED_GREEN);

        new SwingWorker<VerificationCodeManager.ValidationResult, Void>() {
            @Override
            protected VerificationCodeManager.ValidationResult doInBackground() {
                return codeManager.validateCode(getUserEmail(), code);
            }

            @Override
            protected void done() {
                try {
                    VerificationCodeManager.ValidationResult result = get();
                    if (result.isSuccess()) {
                        codeManager.clearAttempts(getUserEmail());
                        stopCountdown();
                        showSuccess("Code vérifié !");
                        AuditLogger.getInstance().log(getUserEmail(), AuditLogger.AuditEvent.RECOVERY_CODE_VALIDATED, null, true);
                        Timer delay = new Timer(SUCCESS_DELAY_MS, evt -> flow.showStep3());
                        delay.setRepeats(false);
                        delay.start();
                    } else {
                        showError(result.getMessage());
                        clearCodeFields();
                        AuditLogger.getInstance().log(getUserEmail(),
                            AuditLogger.AuditEvent.RECOVERY_CODE_VALIDATION_FAILED, result.getMessage(), false);
                    }
                } catch (Exception ex) {
                    showError("Erreur inattendue. Veuillez réessayer.");
                    clearCodeFields();
                    AuditLogger.getInstance().log(getUserEmail(),
                        AuditLogger.AuditEvent.RECOVERY_CODE_VALIDATION_FAILED,
                        "Exception: " + ex.getMessage(), false);
                } finally {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("Vérifier le code");
                    btnVerify.setBackground(ThemeConstants.PRIMARY_GREEN);
                }
            }
        }.execute();
    }

    private void onResendClick() {
        clearError();
        int confirm = JOptionPane.showConfirmDialog(
            this, "Voulez-vous renvoyer le code de vérification ?",
            "Renvoyer le code", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            stopCountdown();
            flow.showStep1();
        }
    }

    private void startCountdown() {
        if (remainingSeconds <= 0) {
            applyExpiredState();
            return;
        }

        lblTimer.setText(formatTimer(remainingSeconds));
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            lblTimer.setText(formatTimer(remainingSeconds));

            if (remainingSeconds <= WARN_RED_SECONDS) {
                lblTimer.setForeground(ThemeConstants.ERROR_RED);
            } else if (remainingSeconds <= WARN_ORANGE_SECONDS) {
                lblTimer.setForeground(COLOR_ORANGE);
            }

            if (remainingSeconds <= 0) {
                stopCountdown();
                applyExpiredState();
            }
        });
        countdownTimer.start();
    }

    private void initializeCountdown() {
        if (getUserEmail().isBlank()) {
            remainingSeconds = 0;
            lblTimer.setText("Code expiré");
            lblTimer.setForeground(ThemeConstants.ERROR_RED);
            showError("Email non défini. Retournez à l'étape précédente.");
            btnVerify.setEnabled(false);
            btnVerify.setBackground(ThemeConstants.DISABLED_GREEN);
            return;
        }
        lblTimer.setText("Chargement...");
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return codeManager.getRemainingSeconds(getUserEmail());
            }

            @Override
            protected void done() {
                try {
                    remainingSeconds = get();
                    if (remainingSeconds <= 0) applyExpiredState();
                    else startCountdown();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Failed to initialize countdown from server", ex);
                    remainingSeconds = 0;
                    applyExpiredState();
                }
            }
        }.execute();
    }

    public void restartForNewCode() {
        stopCountdown();
        clearCodeFields();
        clearError();
        btnVerify.setEnabled(true);
        btnVerify.setText("Vérifier le code");
        btnVerify.setBackground(ThemeConstants.PRIMARY_GREEN);
        lblTimer.setForeground(ThemeConstants.SECONDARY_TEXT);
        initializeCountdown();
    }

    private void applyExpiredState() {
        remainingSeconds = 0;
        lblTimer.setText("Code expiré");
        lblTimer.setForeground(ThemeConstants.ERROR_RED);
        showError("Le code a expiré. Veuillez en demander un nouveau.");
        btnVerify.setEnabled(false);
        btnVerify.setBackground(ThemeConstants.DISABLED_GREEN);
        if (!getUserEmail().isBlank()) {
            AuditLogger.getInstance().log(getUserEmail(), AuditLogger.AuditEvent.RECOVERY_CODE_EXPIRED, null, false);
        }
    }

    void stopCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
    }

    @Override
    public void removeNotify() {
        stopCountdown();
        super.removeNotify();
    }

    private void showError(String message) {
        UIUtils.showTemporaryErrorMessage(lblError, message, 5000);
    }

    private void showSuccess(String message) {
        lblError.setText(message);
        lblError.setForeground(ThemeConstants.SUCCESS_GREEN);
    }

    private void clearError() {
        lblError.setText("");
    }

    public void refreshEmailDisplay() {
        if (lblEmailDisplay != null) {
            lblEmailDisplay.setText(getUserEmail());
        }
    }

    private String getUserEmail() {
        String email = flow.getUserEmail();
        return email != null ? email : "";
    }
}
