package com.gestion.salles.views.Admin;

import com.gestion.salles.views.shared.management.TablePrintableBase;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UserTablePrintable extends TablePrintableBase {

    public UserTablePrintable(JTable tableToPrint, String universityTitle, String dynamicFilterInfo, List<String> visibleColumnNames) {
        super(tableToPrint, universityTitle, dynamicFilterInfo, visibleColumnNames);
    }

    @Override
    protected List<Integer> getColumnWidths(Graphics2D g2d, Font headerFont, Font cellFont, JTable table,
                                            int printableWidth, List<String> visibleColumns) {
        List<Integer> widths = new ArrayList<>();
        int totalPreferredWidth = 0;

        double nameWeight = 1.5;
        double prenomWeight = 1.5;
        double emailWeight = 2.0;

        for (String colName : visibleColumns) {
            int col = table.getColumnModel().getColumnIndex(colName);
            int maxWidth = MIN_COLUMN_WIDTH;

            g2d.setFont(headerFont);
            FontMetrics headerMetrics = g2d.getFontMetrics();
            String headerText = table.getColumnName(col);
            maxWidth = Math.max(maxWidth, headerMetrics.stringWidth(headerText) + 2 * CELL_PADDING_X);

            g2d.setFont(cellFont);
            FontMetrics cellMetrics = g2d.getFontMetrics();
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, col);
                String cellText = value == null ? "" : value.toString();
                maxWidth = Math.max(maxWidth, cellMetrics.stringWidth(cellText) + 2 * CELL_PADDING_X);
            }

            if ("Nom".equalsIgnoreCase(headerText)) {
                maxWidth = (int) (maxWidth * nameWeight);
            } else if ("Prénom".equalsIgnoreCase(headerText)) {
                maxWidth = (int) (maxWidth * prenomWeight);
            } else if ("Email".equalsIgnoreCase(headerText)) {
                maxWidth = (int) (maxWidth * emailWeight);
            }

            widths.add(maxWidth);
            totalPreferredWidth += maxWidth;
        }

        double scaleFactor = totalPreferredWidth > 0 ? (double) printableWidth / totalPreferredWidth : 1.0;
        for (int i = 0; i < widths.size(); i++) {
            widths.set(i, Math.max(MIN_COLUMN_WIDTH, (int) (widths.get(i) * scaleFactor)));
        }
        return widths;
    }
}
