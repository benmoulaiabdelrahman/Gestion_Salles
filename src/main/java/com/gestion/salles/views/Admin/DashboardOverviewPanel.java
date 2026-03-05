package com.gestion.salles.views.Admin;

/******************************************************************************
 * DashboardOverviewPanel.java
 *
 * Data-driven overview panel for the Admin dashboard. Displays a user header
 * with profile picture, four live stat cards, a room-type bar chart, and a
 * recent-activity feed. All data is loaded off the EDT via SwingWorker and
 * refreshed on every RefreshablePanel.refreshData() call.
 ******************************************************************************/

import com.gestion.salles.models.User;
import com.gestion.salles.models.DashboardScope; // Import DashboardScope
import com.gestion.salles.views.shared.dashboard.DashboardOverviewPanelBase;
import com.gestion.salles.views.shared.dashboard.DashboardStatsData;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DashboardOverviewPanel extends DashboardOverviewPanelBase {

    private static final Logger LOGGER = Logger.getLogger(DashboardOverviewPanel.class.getName());

    public DashboardOverviewPanel(User user, Dashboard parentDashboard) {
        super(user, parentDashboard, parentDashboard::showAndResetAdminSettings, LOGGER);
    }

    @Override
    protected String getChartTitle() {
        return "Distribution des Types de Salles";
    }

    @Override
    protected CompletableFuture<DashboardStatsData> loadDashboardData() {
        DashboardScope adminScope = DashboardScope.forAdmin();

        CompletableFuture<Integer> roomCount = CompletableFuture.supplyAsync(
            () -> dao.getRoomCount(adminScope), STATS_EXECUTOR);
        CompletableFuture<Integer> userCount = CompletableFuture.supplyAsync(
            () -> dao.getActiveUserCount(adminScope), STATS_EXECUTOR);
        CompletableFuture<Integer> reservationsToday = CompletableFuture.supplyAsync(
            () -> dao.getReservationsTodayCount(adminScope), STATS_EXECUTOR);
        CompletableFuture<Integer> roomsInUse = CompletableFuture.supplyAsync(
            () -> dao.getRoomsCurrentlyInUseCount(adminScope), STATS_EXECUTOR);
        CompletableFuture<Map<String, Integer>> roomTypes = CompletableFuture.supplyAsync(
            () -> dao.getRoomTypeDistribution(adminScope), STATS_EXECUTOR);

        return CompletableFuture
            .allOf(roomCount, userCount, reservationsToday, roomsInUse, roomTypes)
            .thenApply(v -> DashboardStatsData.of(
                roomCount.join(),
                userCount.join(),
                reservationsToday.join(),
                roomsInUse.join(),
                roomTypes.join()));
    }
}
