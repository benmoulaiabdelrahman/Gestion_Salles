package com.gestion.salles.views.Enseignant;

import com.gestion.salles.models.User;
import com.gestion.salles.utils.AnimatedIconButton;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.dashboard.DashboardHeaderPanel;
import com.gestion.salles.views.shared.dashboard.DashboardHeaderUpdatable;

import javax.swing.*;
import java.awt.*;

public class DashboardOverviewPanel extends JPanel implements DashboardHeaderUpdatable {

    private final User      currentUser;
    private final Dashboard parentDashboard;
    private DashboardHeaderPanel headerPanel;
    private MySchedulePanel schedulePanel;

    public DashboardOverviewPanel(User user, Dashboard parentDashboard) {
        this.currentUser     = user;
        this.parentDashboard = parentDashboard;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(20, 15));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createHeaderPanel(), BorderLayout.NORTH);

        add(createSchedulePanel(), BorderLayout.CENTER);

        AnimatedIconButton btnLogout = new AnimatedIconButton(
            new ImageIcon(getClass().getResource("/icons/logout.png")), 30);
        btnLogout.setToolTipText("Déconnexion");
        btnLogout.addActionListener(e -> {
            parentDashboard.stopForLogout();
            UIUtils.logout(parentDashboard);
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(btnLogout);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createSchedulePanel() {
        schedulePanel = new MySchedulePanel(currentUser, true);
        return schedulePanel;
    }

    private JPanel createHeaderPanel() {
        headerPanel = new DashboardHeaderPanel(currentUser, parentDashboard::showEnseignantSettings);
        return headerPanel;
    }

    public void updateUserInfo(User user) { headerPanel.updateUserInfo(user); }
}
