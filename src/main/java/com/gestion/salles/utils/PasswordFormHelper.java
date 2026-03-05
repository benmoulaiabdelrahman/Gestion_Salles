package com.gestion.salles.utils;

/******************************************************************************
 * PasswordFormHelper.java
 *
 * Utility class encapsulating common password form logic: strength checking,
 * validation, button state management, and success/error feedback panels.
 * Reduces code duplication across account settings and
 * password recovery panels.
 ******************************************************************************/

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.views.Login.PasswordStrengthMeter;
import com.gestion.salles.utils.PasswordStrengthChecker;
import com.gestion.salles.utils.PasswordStrengthChecker.StrengthStatus;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;

public class PasswordFormHelper {

    @FunctionalInterface
    public interface SimpleDocumentListener extends DocumentListener {
        void update(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) { update(e); }

        @Override
        default void removeUpdate(DocumentEvent e) { update(e); }

        @Override
        default void changedUpdate(DocumentEvent e) {}
    }

    public static StrengthStatus checkPasswordStrength(JPasswordField passwordField, PasswordStrengthMeter strengthMeter, JLabel lblStrengthStatus) {
        StrengthStatus status = PasswordStrengthChecker.checkStrength(new String(passwordField.getPassword()));
        strengthMeter.setStrengthStatus(status);
        lblStrengthStatus.setText(status.getMessage());
        lblStrengthStatus.setForeground(status.getColor());
        return status;
    }

    public static boolean isPasswordValid(JPasswordField newPasswordField, JPasswordField confirmPasswordField) {
        String newPass     = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());
        if (!newPass.equals(confirmPass)) return false;
        StrengthStatus status = PasswordStrengthChecker.checkStrength(newPass);
        return status == StrengthStatus.MEDIUM || status == StrengthStatus.STRONG;
    }

    public static void updateButtonState(JButton button, JPasswordField newPasswordField, JPasswordField confirmPasswordField) {
        if (button == null) return;
        boolean isValid = isPasswordValid(newPasswordField, confirmPasswordField);
        button.setEnabled(isValid);
        button.setBackground(isValid ? ThemeConstants.PRIMARY_GREEN : ThemeConstants.DISABLED_GREEN);
    }

    public static JPanel createSuccessPanel(String successMessage, String iconName, Runnable redirectAction) {
        JPanel panel = buildFeedbackPanel();

        JLabel lblIcon = new JLabel(getIcon(iconName, 64));
        lblIcon.setForeground(ThemeConstants.SUCCESS_GREEN);
        panel.add(lblIcon, "align center");

        JLabel lblTitle = new JLabel("Succès !");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 28f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblTitle, "align center");

        JLabel lblMessage = new JLabel("<html><center>" + successMessage + "</center></html>");
        lblMessage.setFont(lblMessage.getFont().deriveFont(14f));
        lblMessage.setForeground(ThemeConstants.SECONDARY_TEXT);
        panel.add(lblMessage, "align center, gapy 0 30");

        Timer timer = new Timer(2500, evt -> { if (redirectAction != null) redirectAction.run(); });
        timer.setRepeats(false);
        timer.start();

        return panel;
    }

    public static JPanel createErrorPanel(String errorMessage, String iconName, ActionListener retryAction) {
        JPanel panel = buildFeedbackPanel();

        JLabel lblIcon = new JLabel(getIcon(iconName, 64));
        lblIcon.setForeground(ThemeConstants.ERROR_RED);
        panel.add(lblIcon, "align center");

        JLabel lblTitle = new JLabel("Échec de l'opération");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 28f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblTitle, "align center");

        JLabel lblMessage = new JLabel("<html><center>" + errorMessage + "</center></html>");
        lblMessage.setFont(lblMessage.getFont().deriveFont(14f));
        lblMessage.setForeground(ThemeConstants.SECONDARY_TEXT);
        panel.add(lblMessage, "align center, gapy 0 30");

        JButton btnRetry = UIUtils.createPrimaryButton("Réessayer");
        btnRetry.setBackground(ThemeConstants.ERROR_RED);
        btnRetry.addActionListener(retryAction);
        panel.add(btnRetry, "align center, height 45, width 200");

        return panel;
    }

    public static ImageIcon getIcon(String path, int size) {
        try {
            ImageIcon icon = new ImageIcon(PasswordFormHelper.class.getResource("/icons/" + path));
            return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.err.println("Error loading icon: " + path + " - " + e.getMessage());
            return null;
        }
    }

    private static JPanel buildFeedbackPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap, fill, insets 50 45 40 45", "[center]", "[grow 0]20[grow 0]20[grow 0]"));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");
        return panel;
    }
}
