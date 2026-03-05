package com.gestion.salles.views.Enseignant;

import com.gestion.salles.models.User;
import com.gestion.salles.views.shared.dashboard.DashboardFrameBase;
import com.gestion.salles.views.shared.settings.AccountSettingsPanel;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 * @author abdelrahman
 */
public class Dashboard extends DashboardFrameBase {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Dashboard.class.getName());

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            stopSessionValidationTimer();
        }
    }

    private DashboardOverviewPanel overviewPanel;
    private MySchedulePanel mySchedulePanel;
    private AccountSettingsPanel settingsPanel;

    public Dashboard(User user, String sessionToken) {
        super(user, sessionToken, logger);
        initComponents();
        enforcePasswordChangeIfRequired();
        startSessionValidationTimer();
    }

    public void showEnseignantDashboard() { showPanel("Overview"); }
    public void showEnseignantSchedule() {
        showPanel("MySchedule");
    }
    public void showEnseignantSettings() {
        showPanel("Settings");
        if (settingsPanel != null) settingsPanel.showMainSettings();
    }

    @Override
    public void onDataChanged() {
        reloadCurrentUserAndRefreshHeader("Overview");
    }

    private void initComponents() {
        initBaseComponents("Gestion Salles", 1200, 800, true);
        showPanel("Overview");
    }

    private void enforcePasswordChangeIfRequired() {
        if (currentUser != null && currentUser.isMustChangePassword()) {
            javax.swing.SwingUtilities.invokeLater(this::showEnseignantSettings);
        }
    }

    @Override
    protected JPanel buildNavigationPanel() {
        return null;
    }

    @Override
    protected void buildPanelFactories(Map<String, Supplier<JPanel>> panelFactories) {
        panelFactories.put("Overview", () -> {
            overviewPanel = new DashboardOverviewPanel(currentUser, this);
            return overviewPanel;
        });
        panelFactories.put("Settings", () -> {
            settingsPanel = new AccountSettingsPanel(this, this::showEnseignantDashboard);
            return settingsPanel;
        });
        panelFactories.put("MySchedule", () -> {
            mySchedulePanel = new MySchedulePanel(currentUser);
            return mySchedulePanel;
        });
    }
}
