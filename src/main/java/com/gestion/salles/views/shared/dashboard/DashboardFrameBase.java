package com.gestion.salles.views.shared.dashboard;

import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.DataRefreshListener;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.utils.SessionManager;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DashboardFrameBase extends JFrame implements DataRefreshListener {

    protected final User currentUser;
    protected final String sessionToken;
    protected final Logger logger;

    protected JPanel contentPanel;
    protected JPanel navigationPanel;
    protected CardLayout cardLayout;
    protected JButton selectedButton;

    protected Map<String, Supplier<JPanel>> panelFactories;
    protected Map<String, JPanel> panelMap;
    protected Map<String, Long> lastRefreshTimes;

    protected DashboardOverlayPanel overlayPanel;
    protected Timer sessionValidationTimer;

    protected final javax.swing.border.Border emptyBorder = BorderFactory.createEmptyBorder(8, 12, 8, 12);

    protected DashboardFrameBase(User user, String sessionToken, Logger logger) {
        this.currentUser = user;
        this.sessionToken = sessionToken;
        this.logger = logger;
    }

    protected void initBaseComponents(String title, int width, int height, boolean maximize) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setLocationRelativeTo(null);
        UIUtils.applyAppIcon(this);
        if (maximize) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeConstants.APP_BACKGROUND);

        contentPanel = createContentPanel();
        navigationPanel = buildNavigationPanel();

        if (navigationPanel != null) {
            mainPanel.add(navigationPanel, BorderLayout.WEST);
        }
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);

        overlayPanel = new DashboardOverlayPanel();
        setGlassPane(overlayPanel);
    }

    protected JPanel createContentPanel() {
        cardLayout = new CardLayout();
        JPanel panel = new JPanel(cardLayout);
        panel.setBackground(ThemeConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelFactories = new HashMap<>();
        panelMap = new HashMap<>();
        lastRefreshTimes = new HashMap<>();

        buildPanelFactories(panelFactories);
        return panel;
    }

    public User getCurrentUser() { return currentUser; }

    public void stopForLogout() { stopSessionValidationTimer(); }

    public void showOverlay() {
        SwingUtilities.invokeLater(() -> {
            overlayPanel.setVisible(true);
            overlayPanel.revalidate();
            overlayPanel.repaint();
        });
    }

    public void hideOverlay() {
        SwingUtilities.invokeLater(() -> {
            overlayPanel.setVisible(false);
            overlayPanel.revalidate();
            overlayPanel.repaint();
        });
    }

    protected void startSessionValidationTimer() {
        sessionValidationTimer = new Timer(60_000, e -> {
            if (!SessionManager.getInstance().isSessionValid(currentUser.getEmail(), sessionToken)) {
                stopSessionValidationTimer();
                JOptionPane.showMessageDialog(this,
                    "Votre session a été invalidée car votre mot de passe a été modifié ou une nouvelle connexion a été établie.",
                    "Session Invalidée", JOptionPane.WARNING_MESSAGE);
                SessionContext.clear();
                UIUtils.logout(this);
            } else {
                SessionManager.getInstance().refreshLastSeen(currentUser.getEmail(), sessionToken);
            }
        });
        sessionValidationTimer.start();
        if (logger != null) {
            logger.info("Session validation timer started for user: " + currentUser.getEmail());
        }
    }

    protected void stopSessionValidationTimer() {
        if (sessionValidationTimer != null) {
            sessionValidationTimer.stop();
            if (logger != null) {
                logger.info("Session validation timer stopped for user: " + currentUser.getEmail());
            }
        }
    }

    protected JButton createNavButton(String text, String panelName, String iconName) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (selectedButton == this || getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(selectedButton == this ? ThemeConstants.CARD_WHITE : ThemeConstants.NAV_HOVER_BACKGROUND);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        applyCommonButtonStyle(button, iconName, ThemeConstants.SECONDARY_TEXT);
        button.addActionListener(e -> {
            setSelectedButton(button);
            showPanel(panelName);
        });
        return button;
    }

    protected void setSelectedButton(JButton button) {
        if (selectedButton != null) selectedButton.setForeground(ThemeConstants.SECONDARY_TEXT);
        button.setForeground(ThemeConstants.PRIMARY_TEXT);
        selectedButton = button;
        if (navigationPanel != null) navigationPanel.repaint();
    }

    protected void applyCommonButtonStyle(JButton button, String iconName, Color foregroundColor) {
        if (iconName != null && !iconName.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource("/icons/" + iconName));
                button.setIcon(new ImageIcon(icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                if (logger != null) {
                    logger.log(Level.WARNING, "Icon not found: " + iconName, e);
                }
            }
        }
        button.setForeground(foregroundColor);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(emptyBorder);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setIconTextGap(15);
        button.putClientProperty("JButton.arc", 8);
    }

    public void showPanel(String panelName) {
        JPanel panel = panelMap.get(panelName);
        if (panel == null && panelFactories.containsKey(panelName)) {
            panel = panelFactories.get(panelName).get();
            panelMap.put(panelName, panel);
            contentPanel.add(panel, panelName);
        }

        cardLayout.show(contentPanel, panelName);

        if (panel instanceof RecentActivityRefreshable) {
            ((RecentActivityRefreshable) panel).refreshRecentActivity();
        }

        if (panel instanceof RefreshablePanel) {
            long now = System.currentTimeMillis();
            long lastRefresh = lastRefreshTimes.getOrDefault(panelName, 0L);
            if (now - lastRefresh > 30_000) {
                ((RefreshablePanel) panel).refreshData();
                lastRefreshTimes.put(panelName, now);
            }
        }
    }

    protected void refreshUserInfoInOverview(User updatedUser, String overviewKey) {
        JPanel panel = panelMap.get(overviewKey);
        if (panel instanceof DashboardHeaderUpdatable) {
            ((DashboardHeaderUpdatable) panel).updateUserInfo(updatedUser);
        }
    }

    protected void reloadCurrentUserAndRefreshHeader(String overviewKey) {
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return new UserDAO().findById(currentUser.getIdUtilisateur());
            }

            @Override
            protected void done() {
                try {
                    User updated = get();
                    if (updated != null) {
                        currentUser.setNom(updated.getNom());
                        currentUser.setPrenom(updated.getPrenom());
                        currentUser.setEmail(updated.getEmail());
                        currentUser.setPhotoProfil(updated.getPhotoProfil());
                        refreshUserInfoInOverview(currentUser, overviewKey);
                    }
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.log(Level.WARNING, "Failed to refresh user data after data change event", ex);
                    }
                }
            }
        }.execute();
    }

    protected abstract JPanel buildNavigationPanel();

    protected abstract void buildPanelFactories(Map<String, Supplier<JPanel>> factories);
}
