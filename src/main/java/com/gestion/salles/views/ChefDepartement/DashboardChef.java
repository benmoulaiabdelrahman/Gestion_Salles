package com.gestion.salles.views.ChefDepartement;

/******************************************************************************
 * DashboardChef.java
 *
 * Main JFrame for the Chef de Département role. Renders a left navigation
 * sidebar and a CardLayout content area that lazily instantiates each
 * management panel on first visit, throttling refreshes to once every 30
 * seconds. Handles session validation via a background Swing Timer that fires
 * every 60 seconds; on invalidation it forces a logout. Exposes stopForLogout()
 * so settings panels can kill the timer before initiating their own logout
 * sequence, preventing a second LoginFrame from opening.
 ******************************************************************************/

import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.dashboard.DashboardFrameBase;
import com.gestion.salles.views.shared.settings.AccountSettingsPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardChef extends DashboardFrameBase {

    private static final Logger LOGGER = Logger.getLogger(DashboardChef.class.getName());

    private JButton    btnOverview;

    public DashboardChef(User user, String sessionToken) {
        super(user, sessionToken, LOGGER);
        initComponents();
        enforcePasswordChangeIfRequired();
        startSessionValidationTimer();
    }

    public void showChefDepartementSettings() { showPanel("ChefDepartementSettings"); }

    public void showChefDepartementDashboard() {
        cardLayout.show(contentPanel, "Apercu");
        setSelectedButton(btnOverview);
    }

    @Override
    public void onDataChanged() {
        LOGGER.info("Data change detected, refreshing dashboard...");

        JPanel overviewPanel = panelMap.get("Apercu");
        if (overviewPanel instanceof RefreshablePanel) {
            long now         = System.currentTimeMillis();
            long lastRefresh = lastRefreshTimes.getOrDefault("Apercu", 0L);
            if (now - lastRefresh > 30_000) { // Throttle refresh to prevent rapid double calls
                ((RefreshablePanel) overviewPanel).refreshData();
                lastRefreshTimes.put("Apercu", now);
            } else {
                LOGGER.info("Throttling refresh for 'Apercu' panel due to recent activity.");
            }
        }

        if (overviewPanel instanceof DashboardOverviewPanel) {
            ((DashboardOverviewPanel) overviewPanel).refreshRecentActivity();
        }

        reloadCurrentUserAndRefreshHeader("Apercu");
    }

    private void initComponents() {
        initBaseComponents("Gestion Salles", 1200, 800, false);
        setSelectedButton(btnOverview);
        showPanel("Apercu");
    }

    private void enforcePasswordChangeIfRequired() {
        if (currentUser != null && currentUser.isMustChangePassword()) {
            SwingUtilities.invokeLater(this::showChefDepartementSettings);
        }
    }

    @Override
    protected JPanel buildNavigationPanel() {
        JPanel panel = new JPanel(new MigLayout(
            "wrap, fillx, insets 20 15 20 15", "[fill]",
            "[]20[][][][][][grow, push][]"
        ));
        panel.setBackground(ThemeConstants.NAV_BACKGROUND);

        JLabel lblTitle = new JLabel("Gestion Salles");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_GREEN);
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/University_of_Laghouat_logo_64x64.png"));
            lblTitle.setIcon(new ImageIcon(icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
            lblTitle.setIconTextGap(10);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Logo icon not found: University_of_Laghouat_logo_64x64.png", e);
        }
        panel.add(lblTitle, "alignx center, gapbottom 20");

        btnOverview             = createNavButton("Tableau de Bord",        "Apercu",       "dashboard.png");
        JButton btnRoomMgmt     = createNavButton("Salles",                 "Salles",       "rooms.png");
        JButton btnNiveauMgmt   = createNavButton("Niveaux",                "Niveaux",      "niveaux.png");
        JButton btnUserMgmt     = createNavButton("Utilisateurs",           "Utilisateurs", "users.png");
        JButton btnReservations = createNavButton("Réservations",           "Reservations", "reservations.png");
        JButton btnSchedule     = createNavButton("Visualiseur d'Horaires", "Horaires",     "schedule.png");

        panel.add(btnOverview,     "growx");
        panel.add(btnRoomMgmt,     "growx");
        panel.add(btnNiveauMgmt,   "growx");
        panel.add(btnUserMgmt,     "growx");
        panel.add(btnReservations, "growx");
        panel.add(btnSchedule,     "growx");

        JButton btnLogout = new JButton("Déconnexion") {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ThemeConstants.NAV_HOVER_BACKGROUND);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        applyCommonButtonStyle(btnLogout, "logout.png", ThemeConstants.SECONDARY_TEXT);
        btnLogout.addActionListener((ActionEvent e) -> {
            stopSessionValidationTimer();
            com.gestion.salles.utils.SessionManager.getInstance().invalidateSession(currentUser.getEmail());
            UIUtils.logout(DashboardChef.this);
        });
        panel.add(btnLogout, "growx, gaptop 270");

        return panel;
    }

    @Override
    protected void buildPanelFactories(Map<String, Supplier<JPanel>> panelFactories) {
        panelFactories.put("Apercu",                   () -> new DashboardOverviewPanel(currentUser, this));
        panelFactories.put("Reservations",             () -> new ReservationManagementPanel(currentUser, this));
        panelFactories.put("Salles",                   () -> new RoomManagementPanel(currentUser, this));
        panelFactories.put("Niveaux",                  () -> new NiveauManagementPanel(currentUser, this));
        panelFactories.put("Utilisateurs",             () -> new UserManagementPanel(currentUser, this));
        panelFactories.put("Horaires",                 () -> new ScheduleViewerPanel(currentUser, this));
        panelFactories.put("ChefDepartementSettings",  () -> new AccountSettingsPanel(this, this::showChefDepartementDashboard));
    }
}
