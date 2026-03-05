package com.gestion.salles.views.shared.management;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public final class ManagementHeaderBuilder {

    private ManagementHeaderBuilder() {}

    public static JPanel buildHeaderWithFilter(JPanel toolbar, JPanel filterPanel) {
        JPanel topControlsPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0", "[fill,grow]"));
        topControlsPanel.setOpaque(false);
        topControlsPanel.add(toolbar, "growx");
        if (filterPanel != null) {
            topControlsPanel.add(filterPanel, "growx");
        }
        return topControlsPanel;
    }
}
