package com.gestion.salles.views.shared.dashboard;

import com.gestion.salles.dao.DashboardDAO;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.BarChartPanel;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.views.shared.RecentActivityPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DashboardOverviewPanelBase extends JPanel implements RefreshablePanel, RecentActivityRefreshable, DashboardHeaderUpdatable {

    protected static final ExecutorService STATS_EXECUTOR = new ThreadPoolExecutor(
        0, 6, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), runnable -> {
            Thread t = new Thread(runnable, "dashboard-stats");
            t.setDaemon(true);
            return t;
        });

    protected final Logger logger;
    protected final User currentUser;
    protected final DashboardFrameBase parentDashboard;
    protected final DashboardDAO dao;

    protected final JLabel lblRoomCount = new JLabel("0", SwingConstants.LEFT);
    protected final JLabel lblUserCount = new JLabel("0", SwingConstants.LEFT);
    protected final JLabel lblReservationsToday = new JLabel("0", SwingConstants.LEFT);
    protected final JLabel lblRoomsInUse = new JLabel("0", SwingConstants.LEFT);
    protected final JPanel chartContainer = new JPanel(new BorderLayout());
    protected final RecentActivityPanel recentActivityPanel;

    private DashboardHeaderPanel headerPanel;
    private JPanel statsCardsPanel;
    private JPanel statsContainer;
    private CardLayout statsCardLayout;
    private JPanel emptyStatsPanel;

    protected DashboardOverviewPanelBase(User user,
                                         DashboardFrameBase parentDashboard,
                                         Runnable onSettings,
                                         Logger logger) {
        this.currentUser = user;
        this.parentDashboard = parentDashboard;
        this.logger = logger;
        this.dao = new DashboardDAO();
        this.recentActivityPanel = new RecentActivityPanel(currentUser);
        initComponents(onSettings);
    }

    private void initComponents(Runnable onSettings) {
        setLayout(new BorderLayout(20, 15));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel combinedTop = new JPanel(new BorderLayout(0, 10));
        combinedTop.setOpaque(false);
        combinedTop.add(createHeaderPanel(onSettings), BorderLayout.NORTH);
        combinedTop.add(createStatsContainer(), BorderLayout.CENTER);

        JPanel mainContent = new JPanel(new GridLayout(1, 2, 20, 20));
        mainContent.setOpaque(false);
        mainContent.add(createChartPanel());
        mainContent.add(recentActivityPanel);

        add(combinedTop, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel(Runnable onSettings) {
        headerPanel = new DashboardHeaderPanel(currentUser, onSettings);
        return headerPanel;
    }

    private JPanel createStatsContainer() {
        statsCardsPanel = createStatsPanel();
        emptyStatsPanel = createEmptyStatsPanel();
        if (emptyStatsPanel == null) {
            statsContainer = statsCardsPanel;
        } else {
            Dimension statsSize = statsCardsPanel.getPreferredSize();
            if (statsSize != null) {
                emptyStatsPanel.setPreferredSize(statsSize);
                emptyStatsPanel.setMinimumSize(statsSize);
            }
            statsCardLayout = new CardLayout();
            statsContainer = new JPanel(statsCardLayout);
            statsContainer.setOpaque(false);
            statsContainer.add(statsCardsPanel, "STATS");
            statsContainer.add(emptyStatsPanel, "EMPTY");
        }
        return statsContainer;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 0, gap 10",
            "[fill,grow][fill,grow][fill,grow][fill,grow]"));
        panel.setOpaque(false);
        panel.add(new DashboardStatCard("Salles Gérées",       lblRoomCount,         "room_stats.png",            logger), "grow");
        panel.add(new DashboardStatCard("Utilisateurs",        lblUserCount,         "user_stats.png",            logger), "grow");
        panel.add(new DashboardStatCard("Réservations (jour)", lblReservationsToday, "reservation_day_stats.png", logger), "grow");
        panel.add(new DashboardStatCard("Salles en cours",     lblRoomsInUse,        "room_in_use_stats.png",     logger), "grow");
        return panel;
    }

    private JPanel createChartPanel() {
        chartContainer.setOpaque(false);
        return chartContainer;
    }

    @Override
    public void refreshData() {
        lblRoomCount.setText("…");
        lblUserCount.setText("…");
        lblReservationsToday.setText("…");
        lblRoomsInUse.setText("…");
        if (statsCardLayout != null) statsCardLayout.show(statsContainer, "STATS");
        chartContainer.removeAll();
        chartContainer.add(new JLabel("…", SwingConstants.CENTER), BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
        recentActivityPanel.refreshData();

        loadDashboardData()
            .thenAcceptAsync(this::applyDashboardData, SwingUtilities::invokeLater)
            .exceptionally(ex -> {
                logger.log(Level.SEVERE, "Failed to load dashboard data", ex);
                SwingUtilities.invokeLater(() -> {
                    lblRoomCount.setText("—");
                    lblUserCount.setText("—");
                    lblReservationsToday.setText("—");
                    lblRoomsInUse.setText("—");
                    chartContainer.removeAll();
                    chartContainer.add(
                        new JLabel("Erreur de chargement des données du tableau de bord.", SwingConstants.CENTER),
                        BorderLayout.CENTER);
                    chartContainer.revalidate();
                    chartContainer.repaint();
                });
                return null;
            });
    }

    private void applyDashboardData(DashboardStatsData data) {
        if (data == null) return;
        if (data.isEmpty()) {
            if (statsCardLayout != null && emptyStatsPanel != null) {
                statsCardLayout.show(statsContainer, "EMPTY");
            }
            chartContainer.removeAll();
            chartContainer.add(new JLabel("", SwingConstants.CENTER), BorderLayout.CENTER);
            chartContainer.revalidate();
            chartContainer.repaint();
            return;
        }

        lblRoomCount.setText(String.valueOf(data.getRoomCount()));
        lblUserCount.setText(String.valueOf(data.getUserCount()));
        lblReservationsToday.setText(String.valueOf(data.getReservationsToday()));
        lblRoomsInUse.setText(String.valueOf(data.getRoomsInUse()));

        chartContainer.removeAll();
        chartContainer.add(new BarChartPanel(data.getRoomTypeDistribution(), getChartTitle()), BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    @Override
    public void refreshRecentActivity() { recentActivityPanel.refreshData(); }

    @Override
    public void updateUserInfo(User user) { headerPanel.updateUserInfo(user); }

    protected JPanel createEmptyStatsPanel() { return null; }

    protected abstract String getChartTitle();

    protected abstract CompletableFuture<DashboardStatsData> loadDashboardData();
}
