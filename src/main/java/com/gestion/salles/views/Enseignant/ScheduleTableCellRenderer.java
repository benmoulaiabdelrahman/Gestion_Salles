package com.gestion.salles.views.Enseignant;

import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.utils.ScheduleGridCellPanel;
import com.gestion.salles.utils.ThemeConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import javax.swing.border.LineBorder;

import java.util.logging.Logger;

public class ScheduleTableCellRenderer extends DefaultTableCellRenderer {

    private static final Logger LOGGER = Logger.getLogger(ScheduleTableCellRenderer.class.getName());

    private final MySchedulePanel parentPanel;
    private final ScheduleGridCellPanel reusableCellPanel;

    public ScheduleTableCellRenderer(MySchedulePanel parentPanel) {
        this.parentPanel = parentPanel;
        this.reusableCellPanel = new ScheduleGridCellPanel(Color.BLACK);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // First column is the day header
        if (column == 0) {
            // Use DefaultTableCellRenderer for day headers
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(ThemeConstants.NAV_BACKGROUND);
            c.setForeground(ThemeConstants.PRIMARY_TEXT);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            ((JComponent) c).setBorder(new LineBorder(Color.BLACK, 1));
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER); // Center text for time cells
            return c;
        }

        // For other columns, use ScheduleGridCellPanel
        ScheduleGridCellPanel cellPanel = reusableCellPanel;
        // For Enseignant, the context filter index is not dynamic like Admin/ChefDepartement.
        // It's always displaying the teacher's schedule, so we can use a fixed context (e.g., 1 for Enseignant view).
        // The ScheduleGridCellPanel needs a contextFilterIndex to decide what labels to display.
        // If we want it to specifically display Teacher-centric info, we can pass a dummy index or modify ScheduleGridCellPanel to handle this.
        // For now, let's pass a value that makes sense, which for a teacher's view, would be like "filtered by Enseignant".
        // In ScheduleGridCellPanel: 0:Salle, 1:Enseignant, 2:Niveau. So passing 1 makes sense for Enseignant's view.
        cellPanel.setContextFilterIndex(1); // Set to Enseignant context

        cellPanel.setCellContent(value, isSelected, table, column);

        return cellPanel;
    }
}
