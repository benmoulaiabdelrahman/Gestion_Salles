package com.gestion.salles.views.Login;

/******************************************************************************
 * ContactAdminDialog.java
 *
 * Modal dialog shown when a user clicks "Contacter l'université" on the login
 * panel. Displays the university support email as a clickable link that opens
 * the system mail client. If the mail client is unavailable the address is
 * copied to the clipboard and a brief inline toast is shown inside the dialog
 * so the user always has a usable fallback.
 ******************************************************************************/

import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class ContactAdminDialog extends JDialog {

    private static final String UNIVERSITY_EMAIL = "uatlinbox@gmail.com";

    public ContactAdminDialog(Frame owner) {
        super(owner, "Contact Administrateur", true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 340);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel mainPanel = UIUtils.createCardDialogPanel(
            "insets 25 30 20 30",
            "[fill, 0:300:380]"
        );

        JPanel root = new JPanel(new BorderLayout());
        root.add(mainPanel, BorderLayout.CENTER);
        root.setBackground(ThemeConstants.APP_BACKGROUND);
        setContentPane(root);

        initComponents(mainPanel);
    }

    private void initComponents(JPanel panel) {
        JLabel lblIcon = UIUtils.createIconLabel(
            "/icons/support.png", 64, "Support",
            UIManager.getDefaults().getFont("Label.font").deriveFont(Font.BOLD, 18f));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblIcon, "alignx center, gapy 0 10");

        JLabel lblTitle = new JLabel("Contacter l'université");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 20f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, "alignx center");

        JLabel lblIntro = new JLabel(
            "<html><center>Veuillez contacter l'adresse e-mail de l'université ci-dessous :</center></html>");
        lblIntro.setFont(lblIntro.getFont().deriveFont(13f));
        lblIntro.setForeground(ThemeConstants.SECONDARY_TEXT);
        panel.add(lblIntro, "gapy 15 15, growx");

        JLabel lblToast = new JLabel();
        lblToast.setFont(lblToast.getFont().deriveFont(Font.BOLD, 12f));
        lblToast.setHorizontalAlignment(SwingConstants.CENTER);
        lblToast.setVisible(false);

        JButton btnEmail = UIUtils.createLinkButton(UNIVERSITY_EMAIL, null, Font.BOLD, 16f);
        btnEmail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEmail.addActionListener(e -> handleEmailClick(lblToast));
        panel.add(btnEmail, "alignx center, gapy 10 5");
        panel.add(lblToast, "alignx center, gapy 0 0");

        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, "growy, pushy");
    }

    private void handleEmailClick(JLabel lblToast) {
        try {
            UIUtils.openEmailClient(UNIVERSITY_EMAIL);
        } catch (Exception ignored) {
            try {
                StringSelection sel = new StringSelection(UNIVERSITY_EMAIL);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            } catch (Exception ex) {
                // clipboard not accessible — nothing more we can do
            }
            showInlineToast(lblToast, "✓ Adresse copiée dans le presse-papiers",
                ThemeConstants.SUCCESS_GREEN, 3500);
        }
    }

    private void showInlineToast(JLabel label, String message, Color color, int durationMs) {
        label.setText(message);
        label.setForeground(color);
        label.setVisible(true);
        label.getParent().revalidate();
        label.getParent().repaint();

        Timer hideTimer = new Timer(durationMs, evt -> {
            label.setVisible(false);
            label.getParent().revalidate();
            label.getParent().repaint();
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }
}
