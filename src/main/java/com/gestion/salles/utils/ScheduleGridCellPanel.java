package com.gestion.salles.utils;

/******************************************************************************
 * ScheduleGridCellPanel.java
 *
 * Custom JPanel used as a table cell renderer component for the schedule grid.
 * Displays activity title, teacher, room, niveau, and type labels with
 * adaptive font sizing. Layout varies by context filter (Salle, Enseignant,
 * or Niveau).
 ******************************************************************************/

import com.gestion.salles.models.ScheduleEntry;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

public class ScheduleGridCellPanel extends JPanel {

    private int contextFilterIndex;

    private static final int MAX_TITLE_LINES    = 2;
    private static final int MIN_TITLE_FONT_SIZE   = 9;
    private static final int MAX_TEACHER_LINES  = 2;
    private static final int MIN_TEACHER_FONT_SIZE = 9;

    public ScheduleGridCellPanel() {
        this(ThemeConstants.DEFAULT_BORDER);
    }

    public ScheduleGridCellPanel(Color borderColor) {
        setLayout(new MigLayout("fill, insets 2 2 2 2", "[grow]", "[]0[]0[]"));
        setOpaque(true);
        setBorder(new LineBorder(borderColor, 1));
    }

    public void setContextFilterIndex(int contextFilterIndex) {
        this.contextFilterIndex = contextFilterIndex;
    }


    public void setCellContent(Object value, boolean isSelected, JTable table, int column) {
        removeAll();

        if (isSelected) {
            setBackground(ThemeConstants.TABLE_SELECTION_BACKGROUND);
            setForeground(ThemeConstants.TABLE_SELECTION_FOREGROUND);
        } else {
            setForeground(ThemeConstants.PRIMARY_TEXT);
        }

        if (value == null || value instanceof List) {
            setBackground(ThemeConstants.CARD_WHITE);
            setToolTipText(null);

        } else if (value instanceof ScheduleEntry) {
            ScheduleEntry entry = (ScheduleEntry) value;

            Color  bgColor;
            String roomDisplay;
            String tooltipRoomInfo;

            if (entry.getReservation().isOnline()) {
                bgColor        = ThemeConstants.SCHEDULE_ONLINE_COLOR;
                roomDisplay    = "En ligne";
                tooltipRoomInfo = "<b>Type de session:</b> En ligne<br>";
            } else {
                bgColor = (entry.getActivityType() != null && entry.getActivityType().getColor() != null)
                    ? entry.getActivityType().getColor()
                    : ThemeConstants.SCHEDULE_OCCUPIED_FALLBACK_COLOR;
                roomDisplay    = entry.getRoomName();
                tooltipRoomInfo = "<b>Salle:</b> " + entry.getRoomName() + "<br>" +
                                  "<b>Bloc:</b> " + (entry.getRoom() != null && entry.getRoom().getBlockName() != null
                                      ? entry.getRoom().getBlockName() : "N/A") + "<br>";
            }
            setBackground(bgColor);

            Color labelFg        = isSelected ? ThemeConstants.TABLE_SELECTION_FOREGROUND : ThemeConstants.PRIMARY_TEXT;
            int   availableWidth = table.getColumnModel().getColumn(column).getWidth() - getInsets().left - getInsets().right - 4;

            JLabel titleLabel   = makeLabel("<html><body><b>" + escapeHtml(entry.getTitreActivite()) + "</b></body></html>", labelFg);
            adjustFontSizeToFitLines(titleLabel, MAX_TITLE_LINES, MIN_TITLE_FONT_SIZE, availableWidth);

            JLabel teacherLabel = makeLabel("<html><body>" + escapeHtml(entry.getTeacherFullName()) + "</body></html>", labelFg);
            teacherLabel.setFont(teacherLabel.getFont().deriveFont(11f));
            JLabel tempTeacher  = new JLabel(teacherLabel.getText());
            tempTeacher.setFont(teacherLabel.getFont());
            tempTeacher.setSize(new Dimension(availableWidth, Integer.MAX_VALUE));
            if (tempTeacher.getPreferredSize().height > tempTeacher.getFontMetrics(teacherLabel.getFont()).getHeight()) {
                adjustFontSizeToFitLines(teacherLabel, MAX_TEACHER_LINES, MIN_TEACHER_FONT_SIZE, availableWidth);
            }

            String niveauText = (entry.getNiveau() != null ? entry.getNiveau().getNom() : "");
            if (entry.isGroupSpecific() && (contextFilterIndex == 0 || contextFilterIndex == 1)) {
                niveauText += " - Gr." + entry.getGroupNumber();
            }

            String blocName = (entry.getReservation().isOnline() || entry.getRoom() == null || entry.getRoom().getBlockName() == null)
                ? "N/A" : entry.getRoom().getBlockName();

            JLabel roomLabel     = makeHtmlLabel(escapeHtml(entry.getReservation().isOnline() ? roomDisplay : entry.getRoomName()), "-1", labelFg);
            JLabel typeLabel     = makeHtmlLabel(escapeHtml(entry.getActivityType() != null ? entry.getActivityType().getName() : ""), "-2", labelFg);
            JLabel niveauLabel   = makeHtmlLabel(escapeHtml(niveauText), "-1", labelFg);
            JLabel blocNameLabel = makeHtmlLabel(escapeHtml(blocName), "-1", labelFg);

            switch (contextFilterIndex) {
                case 0:
                    add(titleLabel,   "growx, wrap");
                    add(teacherLabel, "growx, wrap");
                    add(niveauLabel,  "growx, wrap");
                    add(typeLabel,    "growx");
                    break;
                case 1:
                    add(titleLabel,  "growx, wrap");
                    add(niveauLabel, "growx, wrap");
                    add(roomLabel,   "growx, wrap");
                    add(typeLabel,   "growx");
                    break;
                default:
                    add(titleLabel,   "growx, wrap");
                    add(teacherLabel, "growx, wrap");
                    add(roomLabel,    "growx, wrap");
                    add(typeLabel,    "growx");
                    break;
            }

            setToolTipText("<html>" +
                "<b>Activité:</b> "   + entry.getTitreActivite() + "<br>" +
                "<b>Enseignant:</b> " + entry.getTeacherFullName() + "<br>" +
                tooltipRoomInfo +
                "<b>Niveau:</b> "     + (entry.getNiveau() != null ? entry.getNiveau().getNom() : "N/A") + "<br>" +
                (entry.isGroupSpecific() ? "<b>Groupe:</b> " + entry.getGroupNumber() + "<br>" : "") +
                (entry.isRecurring()     ? "<b>Récurrente:</b> Oui<br>" : "") +
                "<b>Heure:</b> "  + entry.getHeureDebut() + " - " + entry.getHeureFin() + "<br>" +
                "<b>Type:</b> "   + (entry.getActivityType() != null ? entry.getActivityType().getName() : "N/A") + "<br>" +
                "<b>Statut:</b> " + entry.getStatut() + "</html>");
        }

        revalidate();
        repaint();
    }


    private JLabel makeLabel(String text, Color fg) {
        JLabel label = new JLabel(text);
        label.setForeground(fg);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private JLabel makeHtmlLabel(String text, String fontSize, Color fg) {
        return makeLabel("<html><body style='width: 100px'><font size='" + fontSize + "'>" + text + "</font></body></html>", fg);
    }

    private void adjustFontSizeToFitLines(JLabel label, int maxLines, int minFontSize, int availableWidth) {
        if (availableWidth <= 0 || label.getText().isEmpty()) return;

        Font   initialFont    = label.getFont();
        int    currentFontSize = initialFont.getSize();
        String plainText      = label.getText().replaceAll("<html><body><b>|</b></body></html>", "");

        JTextArea tempArea = new JTextArea(plainText);
        tempArea.setLineWrap(true);
        tempArea.setWrapStyleWord(true);

        while (currentFontSize >= minFontSize) {
            tempArea.setFont(initialFont.deriveFont((float) currentFontSize));
            tempArea.setSize(new Dimension(availableWidth, Integer.MAX_VALUE));
            FontMetrics fm = tempArea.getFontMetrics(tempArea.getFont());
            if (tempArea.getPreferredSize().height <= maxLines * fm.getHeight()) {
                label.setFont(tempArea.getFont());
                return;
            }
            currentFontSize--;
        }
        label.setFont(initialFont.deriveFont((float) minFontSize));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
