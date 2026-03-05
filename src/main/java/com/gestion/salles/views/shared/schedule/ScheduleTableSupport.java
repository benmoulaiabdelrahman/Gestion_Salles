package com.gestion.salles.views.shared.schedule;

import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Admin.ScheduleTableModel;
import com.gestion.salles.models.ScheduleEntry;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public final class ScheduleTableSupport {

    public static final String[] DAY_NAMES = {"Samedi", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi"};

    private ScheduleTableSupport() {}

    public static ScheduleTableBundle buildScheduleTable(ScheduleTableModel model, TableCellRenderer cellRenderer) {
        JScrollPane scrollPane = UIUtils.createStyledTable(model);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JTable table = (JTable) scrollPane.getViewport().getView();
        table.setGridColor(Color.BLACK);
        table.setDefaultRenderer(Object.class, cellRenderer);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new ScheduleTableHeaderRenderer(header.getDefaultRenderer()));

        return new ScheduleTableBundle(scrollPane, table);
    }

    public static void adjustRowHeight(JTable table) {
        if (table != null && table.getRowCount() > 0) {
            table.setRowHeight(84);
        }
    }

    public static Object[][] createEmptyScheduleGridData(List<LocalTime[]> timeSlots) {
        int numDays = DAY_NAMES.length;
        int numCols = 1 + timeSlots.size();
        Object[][] data = new Object[numDays][numCols];

        for (int i = 0; i < numDays; i++) {
            data[i][0] = DAY_NAMES[i];
            for (int j = 1; j < numCols; j++) data[i][j] = null;
        }
        return data;
    }

    public static Object[][] buildGridData(List<ScheduleEntry> entries,
                                           List<LocalTime[]> timeSlots,
                                           boolean enableGroupSpecific,
                                           Integer totalGroups) {
        Object[][] data = createEmptyScheduleGridData(timeSlots);

        for (ScheduleEntry entry : entries) {
            if (entry == null || entry.getDateReservation() == null || entry.getHeureDebut() == null) continue;
            int rowIndex = getRowIndexForDay(entry.getDateReservation().getDayOfWeek());
            int columnIndex = getColumnIndexForTime(entry.getHeureDebut(), timeSlots);
            if (rowIndex == -1 || columnIndex == -1) continue;

            Object existingData = data[rowIndex][columnIndex];

            if (enableGroupSpecific && entry.isGroupSpecific() && totalGroups != null && totalGroups > 0) {
                Map<String, Object> cellData;
                if (existingData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingMap = (Map<String, Object>) existingData;
                    cellData = existingMap;
                } else {
                    cellData = new HashMap<>();
                    cellData.put("groupEntries", new HashMap<Integer, ScheduleEntry>());
                    cellData.put("totalGroups", totalGroups);
                    data[rowIndex][columnIndex] = cellData;
                }
                @SuppressWarnings("unchecked")
                Map<Integer, ScheduleEntry> groupEntries = (Map<Integer, ScheduleEntry>) cellData.get("groupEntries");
                groupEntries.put(entry.getGroupNumber(), entry);
                continue;
            }

            if (existingData == null) {
                data[rowIndex][columnIndex] = entry;
            } else if (existingData instanceof List) {
                @SuppressWarnings("unchecked")
                List<ScheduleEntry> conflictList = (List<ScheduleEntry>) existingData;
                conflictList.add(entry);
            } else if (existingData instanceof ScheduleEntry) {
                List<ScheduleEntry> conflictList = new ArrayList<>();
                conflictList.add((ScheduleEntry) existingData);
                conflictList.add(entry);
                data[rowIndex][columnIndex] = conflictList;
            }
        }
        return data;
    }

    public static int getRowIndexForDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SATURDAY -> 0;
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 3;
            case WEDNESDAY -> 4;
            case THURSDAY -> 5;
            default -> -1;
        };
    }

    public static int getColumnIndexForTime(LocalTime startTime, List<LocalTime[]> timeSlots) {
        for (int i = 0; i < timeSlots.size(); i++) {
            if (startTime.equals(timeSlots.get(i)[0])) return 1 + i;
        }
        return -1;
    }

    public static final class ScheduleTableBundle {
        public final JScrollPane scrollPane;
        public final JTable table;

        private ScheduleTableBundle(JScrollPane scrollPane, JTable table) {
            this.scrollPane = scrollPane;
            this.table = table;
        }
    }

    private static final class ScheduleTableHeaderRenderer extends DefaultTableCellRenderer {
        private final TableCellRenderer defaultRenderer;

        private ScheduleTableHeaderRenderer(TableCellRenderer defaultRenderer) {
            this.defaultRenderer = defaultRenderer;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JComponent) {
                ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                c.setBackground(ThemeConstants.NAV_BACKGROUND);
                c.setForeground(ThemeConstants.PRIMARY_TEXT);
            }
            return c;
        }
    }
}
