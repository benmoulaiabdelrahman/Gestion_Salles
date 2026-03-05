package com.gestion.salles.views.shared.dashboard;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardStatCard extends JPanel {

    public DashboardStatCard(String title, JLabel valueLabel, String iconName, Logger logger) {
        super(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (iconName != null && !iconName.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource("/icons/" + iconName));
                add(new JLabel(new ImageIcon(icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH))),
                    BorderLayout.WEST);
            } catch (Exception e) {
                if (logger != null) {
                    logger.log(Level.WARNING, "Stat card icon not found: " + iconName, e);
                }
            }
        }

        JPanel textPanel = new JPanel(new net.miginfocom.swing.MigLayout("wrap, insets 0", "[left]", "[top][bottom]"));
        textPanel.setOpaque(false);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 20f));
        textPanel.add(valueLabel);
        textPanel.add(new JLabel(title));
        add(textPanel, BorderLayout.CENTER);
    }
}
