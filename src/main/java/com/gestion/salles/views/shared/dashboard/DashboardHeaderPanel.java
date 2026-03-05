package com.gestion.salles.views.shared.dashboard;

import com.gestion.salles.models.User;
import com.gestion.salles.utils.AnimatedIconButton;
import com.gestion.salles.utils.RoundImage;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class DashboardHeaderPanel extends JPanel {

    private final User currentUser;

    private JLabel lblUserName;
    private JLabel lblUserEmail;
    private RoundImage headerProfileImage;
    private JPanel headerPanel;

    public DashboardHeaderPanel(User user, Runnable onSettings) {
        this.currentUser = user;
        initComponents(onSettings);
    }

    private void initComponents(Runnable onSettings) {
        setOpaque(false);
        setLayout(new MigLayout("insets 0, fillx", "[grow,fill][]", "[]"));

        AnimatedIconButton btnSettings = new AnimatedIconButton(
            new ImageIcon(getClass().getResource("/icons/settings.png")), 30);
        btnSettings.addActionListener(e -> onSettings.run());

        JPanel settingsWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        settingsWrapper.setOpaque(false);
        settingsWrapper.add(btnSettings);

        add(createHeaderPanel(), "growx");
        add(settingsWrapper, "align right, gapx 0 0");
    }

    private JPanel createHeaderPanel() {
        headerPanel = new JPanel(new MigLayout("insets 0, gap 15", "[][]", "[][]"));
        headerPanel.setOpaque(false);

        headerProfileImage = new RoundImage(
            UIUtils.getProfilePictureIcon(currentUser.getPhotoProfil()), currentUser.getFullName(), 60);
        headerPanel.add(headerProfileImage, "spany 2");

        lblUserName = new JLabel();
        lblUserName.setFont(lblUserName.getFont().deriveFont(Font.BOLD, 24f));
        lblUserName.setForeground(ThemeConstants.PRIMARY_TEXT);
        headerPanel.add(lblUserName, "wrap");

        lblUserEmail = new JLabel();
        lblUserEmail.setFont(lblUserEmail.getFont().deriveFont(14f));
        lblUserEmail.setForeground(ThemeConstants.SECONDARY_TEXT);
        headerPanel.add(lblUserEmail);

        updateUserInfo(currentUser);
        return headerPanel;
    }

    public void updateUserInfo(User user) {
        if (user == null) return;
        lblUserName.setText(user.getFullName());
        lblUserEmail.setText(user.getEmail());

        String filename = user.getPhotoProfil();
        ImageIcon icon = (filename != null && !filename.isEmpty())
            ? UIUtils.getProfilePictureIcon(filename) : null;
        headerProfileImage.setImage(icon, user.getFullName());

        headerPanel.revalidate();
        headerPanel.repaint();
    }
}
