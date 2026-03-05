package com.gestion.salles.views.ChefDepartement;

import com.gestion.salles.models.DashboardScope;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.dashboard.DashboardOverviewPanelBase;
import com.gestion.salles.views.shared.dashboard.DashboardStatsData;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DashboardOverviewPanel extends DashboardOverviewPanelBase {

    private static final Logger LOGGER = Logger.getLogger(DashboardOverviewPanel.class.getName());

    public DashboardOverviewPanel(User user, DashboardChef parentDashboard) {
        super(user, parentDashboard, parentDashboard::showChefDepartementSettings, LOGGER);
    }

    @Override
    protected JPanel createEmptyStatsPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap, insets 18, fillx", "[grow,center]", "[][][]20[]"));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel title = new JLabel("\u26a0\ufe0f Aucun bloc assigné");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(156, 101, 0));
        panel.add(title, "alignx center");

        JLabel line1 = new JLabel("Votre compte n'est pas encore associé à un bloc.");
        line1.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(line1, "alignx center");

        JLabel line2 = new JLabel("Contactez votre administrateur.");
        line2.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(line2, "alignx center");

        JButton refreshButton = UIUtils.createPrimaryButton("Rafraîchir");
        refreshButton.addActionListener(e -> refreshData());
        panel.add(refreshButton, "alignx center, w 140!, h 36!");

        return panel;
    }

    @Override
    protected String getChartTitle() {
        return "Distribution des Types de Salles du Département";
    }

    @Override
    protected CompletableFuture<DashboardStatsData> loadDashboardData() {
        Integer blocId = currentUser.getIdBloc();
        if (blocId == null) {
            LOGGER.severe("Chef dashboard access attempted with null id_bloc for user: " + currentUser.getEmail());
            return CompletableFuture.completedFuture(DashboardStatsData.empty());
        }

        DashboardScope chefScope = DashboardScope.forChef(blocId);

        CompletableFuture<Integer> roomCount = CompletableFuture.supplyAsync(
            () -> dao.getRoomCount(chefScope), STATS_EXECUTOR);
        CompletableFuture<Integer> userCount = CompletableFuture.supplyAsync(
            () -> dao.getActiveUserCount(chefScope), STATS_EXECUTOR);
        CompletableFuture<Integer> reservationsToday = CompletableFuture.supplyAsync(
            () -> dao.getReservationsTodayCount(chefScope), STATS_EXECUTOR);
        CompletableFuture<Integer> roomsInUse = CompletableFuture.supplyAsync(
            () -> dao.getRoomsCurrentlyInUseCount(chefScope), STATS_EXECUTOR);
        CompletableFuture<Map<String, Integer>> roomTypes = CompletableFuture.supplyAsync(
            () -> dao.getRoomTypeDistribution(chefScope), STATS_EXECUTOR);

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
