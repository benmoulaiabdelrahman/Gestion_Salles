package com.gestion.salles.views.ChefDepartement;

import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.utils.ScheduleGridCellPanel; // NEW IMPORT
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.GroupSpecificCellPanel; // Added import for GroupSpecificCellPanel

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component; // Added import
import java.awt.Font; // Added import
import java.awt.Color;
import javax.swing.border.LineBorder;
import java.util.Map; // Added import for Map

import java.util.logging.Logger;
//import net.miginfocom.swing.MigLayout; // No longer needed directly here

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

        int selectedIndex = parentPanel.getContextFilterComboBox().getSelectedIndex();
        // Chef combo includes a placeholder at index 0 ("Sélectionner...").
        // Normalize to ScheduleGridCellPanel context indexes: 0=Salle, 1=Enseignant, 2=Niveau.
        int contextIndex = Math.max(0, selectedIndex - 1);
        
        // Check if we're in Niveau filter mode (contextIndex == 3 for ChefDepartement) and have group-specific data
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
