package com.gestion.salles.utils;

/******************************************************************************
 * GroupSpecificCellPanel.java
 *
 * Custom JPanel used in the schedule grid to render a cell that may contain
 * multiple group entries. Each group occupies an equal-height row within the
 * cell, separated by a thin inner border. Free slots are labelled "Libre".
 * Activity rows display abbreviated title, teacher last name, and room name
 * on a single line, coloured by activity type.
 ******************************************************************************/

import com.gestion.salles.models.ScheduleEntry;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Map;

public class GroupSpecificCellPanel extends JPanel {

    public GroupSpecificCellPanel() {
        this(ThemeConstants.DEFAULT_BORDER);
    }

    public GroupSpecificCellPanel(Color mainBorderColor) {
        setOpaque(true);
        setBorder(new LineBorder(mainBorderColor, 1));
    }


    public void setGroupContent(Map<Integer, ScheduleEntry> groupEntries, int totalGroups, boolean isSelected, Color innerBorderColor) {
        removeAll();

        if (groupEntries == null || groupEntries.isEmpty()) {
            setBackground(ThemeConstants.CARD_WHITE);
            setLayout(new MigLayout("fill, insets 0", "[grow,fill]", "[grow,fill]"));
            JLabel freeLabel = new JLabel("Libre");
            freeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            freeLabel.setVerticalAlignment(SwingConstants.CENTER);
            add(freeLabel, "grow, center");
            setToolTipText(null);
        } else {
            setLayout(new MigLayout(
                "fill, insets 0, gap 0",
                "[grow,fill]",
                buildRowConstraints(totalGroups)
            ));

            setBackground(resolveLastRowBackground(groupEntries, totalGroups));

            for (int groupNum = 1; groupNum <= totalGroups; groupNum++) {
                JPanel groupRow = createGroupRow(
                    groupNum,
                    groupEntries.get(groupNum),
                    isSelected,
                    groupNum < totalGroups,
                    innerBorderColor
                );
                add(groupRow, "grow, wrap");
            }

            setToolTipText(buildTooltip(groupEntries, totalGroups));
        }

        revalidate();
        repaint();
    }


    private String buildRowConstraints(int totalGroups) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= totalGroups; i++) {
            if (i > 1) sb.append("0");
            sb.append("[").append(100.0 / totalGroups).append("%!]");
        }
        return sb.toString();
    }

    private Color resolveLastRowBackground(Map<Integer, ScheduleEntry> groupEntries, int totalGroups) {
        ScheduleEntry lastEntry = groupEntries.get(totalGroups);
        if (lastEntry != null && lastEntry.getActivityType() != null && lastEntry.getActivityType().getColor() != null) {
            return lastEntry.getActivityType().getColor();
        }
        return lastEntry != null ? ThemeConstants.SCHEDULE_OCCUPIED_FALLBACK_COLOR : ThemeConstants.CARD_WHITE;
    }

    private String buildTooltip(Map<Integer, ScheduleEntry> groupEntries, int totalGroups) {
        StringBuilder tooltip = new StringBuilder("<html>");
        for (int groupNum = 1; groupNum <= totalGroups; groupNum++) {
            ScheduleEntry entry = groupEntries.get(groupNum);
            tooltip.append("<b>Groupe ").append(groupNum).append(":</b> ");
            if (entry != null) {
                tooltip.append(entry.getTitreActivite())
                       .append(" (").append(entry.getTeacherFullName()).append(")<br>");
            } else {
                tooltip.append("Libre<br>");
            }
        }
        return tooltip.append("</html>").toString();
    }

    private JPanel createGroupRow(int groupNum, ScheduleEntry entry, boolean isSelected, boolean addBottomBorder, Color innerBorderColor) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setOpaque(true);

        if (addBottomBorder) {
            rowPanel.setBorder(new MatteBorder(0, 0, 1, 0, innerBorderColor));
        }

        if (entry == null) {
            rowPanel.setBackground(ThemeConstants.CARD_WHITE);
            JLabel groupLabel = new JLabel("");
            groupLabel.setHorizontalAlignment(SwingConstants.CENTER);
            groupLabel.setVerticalAlignment(SwingConstants.CENTER);
            groupLabel.setForeground(ThemeConstants.PRIMARY_TEXT);
            rowPanel.add(groupLabel, BorderLayout.CENTER);
        } else {
            Color bgColor = (entry.getActivityType() != null && entry.getActivityType().getColor() != null)
                ? entry.getActivityType().getColor()
                : ThemeConstants.SCHEDULE_OCCUPIED_FALLBACK_COLOR;
            rowPanel.setBackground(bgColor);

            String abbreviatedTitle = extractActivityInitials(entry.getTitreActivite());
            String teacherLastName  = entry.getTeacher() != null ? entry.getTeacher().getNom() : "N/A";
            Color  labelFg          = isSelected ? ThemeConstants.TABLE_SELECTION_FOREGROUND : ThemeConstants.PRIMARY_TEXT;

            String labelText = "<html><b>Gr." + groupNum + "</b> - " +
                               "<font size='-1'>" + abbreviatedTitle + "</font> - " +
                               "<font size='-2'>" + teacherLastName + " - " + entry.getRoomName() + "</font></html>";

            JLabel infoLabel = new JLabel(labelText);
            infoLabel.setForeground(labelFg);
            infoLabel.setHorizontalAlignment(SwingConstants.LEFT);
            infoLabel.setVerticalAlignment(SwingConstants.CENTER);
            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            rowPanel.add(infoLabel, BorderLayout.CENTER);
        }

        return rowPanel;
    }

    private String extractActivityInitials(String title) {
        if (title == null || title.trim().isEmpty()) return "";

        StringBuilder initials = new StringBuilder();
        for (String word : title.split("\\s+")) {
            if (!word.isEmpty()) {
                initials.append(word.matches("\\d+")
                    ? word
                    : Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.toString().trim();
    }
}
