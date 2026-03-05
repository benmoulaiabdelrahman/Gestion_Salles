package com.gestion.salles.views.shared;

import com.gestion.salles.dao.ActivityLogDAO;
import com.gestion.salles.models.ActivityLog;
import com.gestion.salles.models.ActivityItemData;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.RoundImage;
import com.gestion.salles.utils.ThemeConstants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecentActivityPanel extends JPanel implements RefreshablePanel {

    private final ActivityLogDAO activityLogDAO;
    private static final Logger LOGGER = Logger.getLogger(RecentActivityPanel.class.getName());
    private final JPanel contentPanel;
    private static final Map<String, ImageIcon> ICON_CACHE = new ConcurrentHashMap<>();
    private SwingWorker<List<ActivityItemData>, Void> currentWorker;
    private final User currentUser; // Field to store currentUser

    // Modified constructor to accept currentUser (null for Admin)
    public RecentActivityPanel(User currentUser) {
        this.currentUser = currentUser;
        this.activityLogDAO = new ActivityLogDAO();
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.CARD_WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        JLabel titleLabel = new JLabel("Activité Récente");
        titleLabel.setFont(ThemeConstants.FONT_BOLD_18);
        titleLabel.setForeground(ThemeConstants.PRIMARY_TEXT);
        add(titleLabel, BorderLayout.NORTH);

        contentPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 10 0 0 0, gapy 10"));
        contentPanel.setBackground(getBackground());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadActivities(contentPanel);
    }

    @Override
    public void refreshData() {
        contentPanel.removeAll();
        loadActivities(contentPanel);
    }

    public static void invalidateCache() {
        ICON_CACHE.clear();
        LOGGER.info("Shared RecentActivityPanel ICON_CACHE invalidated.");
    }

    private void loadActivities(JPanel panel) {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        SwingWorker<List<ActivityItemData>, Void> worker = new SwingWorker<List<ActivityItemData>, Void>() {
            @Override
            protected List<ActivityItemData> doInBackground() throws Exception {
                List<ActivityLog> activityLogs;
                Integer currentBlocId = (currentUser != null) ? currentUser.getIdBloc() : null;

                if (currentBlocId == null) { // Admin scope
                    activityLogs = activityLogDAO.getTodaysActivities();
                    LOGGER.info("Loading global activities for Admin.");
                } else { // Chef scope
                    activityLogs = activityLogDAO.getTodaysActivitiesForBloc(currentBlocId);
                    LOGGER.info("Loading activities for blocId: " + currentBlocId + " for user: " + currentUser.getEmail());
                }
                
                List<ActivityItemData> resolvedActivityData = new ArrayList<>();

                for (ActivityLog log : activityLogs) {
                    ImageIcon resolvedIcon = resolveImageIcon(log);
                    resolvedActivityData.add(new ActivityItemData(log, resolvedIcon));
                }
                return resolvedActivityData;
            }

            @Override
            protected void done() {
                if (this != currentWorker) {
                    LOGGER.info("Shared RecentActivityPanel: Stale worker completed, skipping UI update.");
                    return;
                }
                if (isCancelled()) {
                    LOGGER.info("Shared RecentActivityPanel: Worker cancelled, skipping done() processing.");
                    return;
                }
                try {
                    List<ActivityItemData> activitiesData = get();
                    if (activitiesData.isEmpty()) {
                        panel.add(new JLabel("Aucune activité aujourd'hui."));
                    } else {
                        for (ActivityItemData itemData : activitiesData) {
                            panel.add(createActivityItemPanel(itemData), "growx");
                        }
                    }
                    panel.revalidate();
                    panel.repaint();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading activities into RecentActivityPanel", e);
                    panel.add(new JLabel("Erreur de chargement des activités: " + e.getMessage()));
                    panel.revalidate();
                    panel.repaint();
                }
            }
        };
        currentWorker = worker;
        worker.execute();
    }

    private JPanel createActivityItemPanel(ActivityItemData itemData) {
        ActivityLog log = itemData.getActivityLog();
        JPanel panel = new JPanel(new MigLayout("insets 5, fillx, gap 10", "[40!][grow]"));
        panel.setBackground(getBackground());

        JLabel iconLabel = new JLabel(itemData.getResolvedIcon());
        panel.add(iconLabel, "cell 0 0 1 2, aligny top");

        String summary = log.getDetails();
        try (java.io.StringReader reader = new java.io.StringReader(log.getDetails())) {
            com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(reader);
            jsonReader.setLenient(true);
            com.google.gson.JsonElement jsonElement = JsonParser.parseReader(jsonReader);

            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject detailsJson = jsonElement.getAsJsonObject();
                if (detailsJson.has("summary")) {
                    summary = detailsJson.get("summary").getAsString();
                }
            }
        } catch (com.google.gson.JsonParseException e) {
            LOGGER.log(Level.FINE, "Details not valid JSON for log ID: " + log.getIdLog() + ". Falling back to raw string.", e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error parsing JSON details for log ID: " + log.getIdLog(), e);
        }
        JLabel detailsLabel = new JLabel(summary);
        detailsLabel.setFont(ThemeConstants.FONT_REGULAR_13);
        panel.add(detailsLabel, "cell 1 0, growx");

        JLabel timeLabel = new JLabel(formatTimestamp(log.getTimestamp()));
        timeLabel.setFont(ThemeConstants.FONT_LIGHT_12);
        timeLabel.setForeground(ThemeConstants.SECONDARY_TEXT);
        panel.add(timeLabel, "cell 1 1, aligny bottom");

        return panel;
    }

    private ImageIcon resolveImageIcon(ActivityLog log) {
        String genericIconCacheKey = getIconNameForEntityType(log.getEntityType());

        User user = log.getActingUser();
        if (user != null && log.getEntityType() == ActivityLog.EntityType.USER && log.getActionType() == ActivityLog.ActionType.CREATE) {
            String photoFileName = user.getPhotoProfil();
            if (photoFileName != null && !photoFileName.isEmpty()) {
                String photoPath = "uploads/profile-pictures/" + photoFileName;
                File photoFile = new File(photoPath);

                String profilePicCacheKey = photoPath + "_rounded_40x40";

                if (ICON_CACHE.containsKey(profilePicCacheKey)) {
                    return ICON_CACHE.get(profilePicCacheKey);
                } else if (photoFile.exists()) {
                    ImageIcon originalIcon = UIUtils.getProfilePictureIcon(photoFileName);
                    if (originalIcon != null) {
                        Image roundedImage = RoundImage.getRoundedImage(originalIcon.getImage(), 40);
                        if (roundedImage != null) {
                            ImageIcon cachedIcon = new ImageIcon(roundedImage);
                            ICON_CACHE.put(profilePicCacheKey, cachedIcon);
                            return cachedIcon;
                        }
                    }
                }
            }
        }
        
        if (ICON_CACHE.containsKey(genericIconCacheKey)) {
            return ICON_CACHE.get(genericIconCacheKey);
        } else {
            String iconResourcePath = "icons/" + genericIconCacheKey;
            URL resource = getClass().getClassLoader().getResource(iconResourcePath);
            if (resource == null) {
                LOGGER.log(Level.WARNING, "Generic icon resource not found: " + iconResourcePath);
                return new ImageIcon();
            }
            ImageIcon icon = new ImageIcon(resource);
            Image scaledImage = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            ImageIcon cachedIcon = new ImageIcon(scaledImage);
            ICON_CACHE.put(genericIconCacheKey, cachedIcon);
            return cachedIcon;
        }
    }

    private String getIconNameForEntityType(ActivityLog.EntityType type) {
        // Current intended icon mapping for activity entity types:
        // USER        → users.png
        // ROOM        → rooms.png
        // RESERVATION → reservations.png
        // FACULTE     → department.png
        // DEPARTEMENT → bloc.png
        // NIVEAU      → niveaux.png
        switch (type) {
            case USER: return "users.png";
            case ROOM: return "rooms.png";
            case RESERVATION: return "reservations.png";
            case FACULTE: return "department.png";
            case DEPARTEMENT: return "bloc.png";
            case NIVEAU: return "niveaux.png";
            default: return "dashboard.png";
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        long diff = System.currentTimeMillis() - timestamp.getTime();
        long diffMinutes = diff / (60 * 1000);
        if (diffMinutes < 1) return "à l'instant";
        if (diffMinutes < 60) return "il y a " + diffMinutes + " min";
        long diffHours = diff / (60 * 60 * 1000);
        if (diffHours < 24) return "il y a " + diffHours + " h";
        return new SimpleDateFormat("dd/MM/yyyy").format(timestamp);
    }
}
