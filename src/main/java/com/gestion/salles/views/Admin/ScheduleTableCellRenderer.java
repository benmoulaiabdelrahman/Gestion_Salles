package com.gestion.salles.views.Admin;

import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.utils.ScheduleGridCellPanel;
import com.gestion.salles.utils.GroupSpecificCellPanel;
import com.gestion.salles.utils.ThemeConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.awt.Color; // Added import
import javax.swing.border.LineBorder;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

public class ScheduleTableCellRenderer extends DefaultTableCellRenderer {

    private static final Logger LOGGER = Logger.getLogger(ScheduleTableCellRenderer.class.getName());

    private ScheduleViewerPanel parentPanel;
    private final GroupSpecificCellPanel reusableGroupPanel;
    private final ScheduleGridCellPanel reusableCellPanel;

    public ScheduleTableCellRenderer(ScheduleViewerPanel parentPanel) {
        this.parentPanel = parentPanel;
        this.reusableGroupPanel = new GroupSpecificCellPanel(Color.BLACK);
        this.reusableCellPanel = new ScheduleGridCellPanel(Color.BLACK);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // First column is the day name header
        if (column == 0) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(ThemeConstants.NAV_BACKGROUND);
            c.setForeground(ThemeConstants.PRIMARY_TEXT);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            ((JComponent) c).setBorder(new LineBorder(Color.BLACK, 1));
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }

        int contextIndex = parentPanel.getContextFilterComboBox().getSelectedIndex();
        
        // Check if we're in Niveau filter mode (contextIndex == 2) and have group-specific data
        // This applies to schedule data cells (columns > 0)
        if (contextIndex == 2 && value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cellData = (Map<String, Object>) value;
            
            if (cellData.containsKey("groupEntries")) {
                @SuppressWarnings("unchecked")
                Map<Integer, ScheduleEntry> groupEntries = (Map<Integer, ScheduleEntry>) cellData.get("groupEntries");
                Integer totalGroups = (Integer) cellData.get("totalGroups");
                
                GroupSpecificCellPanel groupPanel = reusableGroupPanel;
                groupPanel.setGroupContent(groupEntries, totalGroups != null ? totalGroups : 0, isSelected, Color.BLACK);
                return groupPanel;
            }
        }

        // For other columns (schedule data), use ScheduleGridCellPanel
        ScheduleGridCellPanel cellPanel = reusableCellPanel;
        cellPanel.setContextFilterIndex(contextIndex);
        cellPanel.setCellContent(value, isSelected, table, column);

        return cellPanel;
    }
}
