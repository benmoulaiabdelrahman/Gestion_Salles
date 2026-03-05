package com.gestion.salles.views.shared.schedule;

import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.views.Admin.ScheduleTableModel;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class SchedulePanelBase extends javax.swing.JPanel implements RefreshablePanel {

    protected final List<LocalTime[]> timeSlots = List.of(
        new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(9, 25)},
        new LocalTime[]{LocalTime.of(9, 35), LocalTime.of(11, 0)},
        new LocalTime[]{LocalTime.of(11, 10), LocalTime.of(12, 35)},
        new LocalTime[]{LocalTime.of(12, 45), LocalTime.of(14, 10)},
        new LocalTime[]{LocalTime.of(14, 20), LocalTime.of(15, 45)}
    );

    protected ScheduleTableModel scheduleTableModel;
    protected JTable scheduleTable;
    protected JScrollPane scheduleScrollPane;

    protected JScrollPane createScheduleTable(TableCellRenderer renderer) {
        String[] columnNames = {
            "",
            formatTimeSlot(timeSlots.get(0)),
            formatTimeSlot(timeSlots.get(1)),
            formatTimeSlot(timeSlots.get(2)),
            formatTimeSlot(timeSlots.get(3)),
            formatTimeSlot(timeSlots.get(4))
        };

        scheduleTableModel = new ScheduleTableModel(columnNames);
        ScheduleTableSupport.ScheduleTableBundle bundle =
            ScheduleTableSupport.buildScheduleTable(scheduleTableModel, renderer);
        scheduleScrollPane = bundle.scrollPane;
        scheduleTable = bundle.table;
        clearScheduleGrid();
        return scheduleScrollPane;
    }

    protected void populateScheduleGrid(List<ScheduleEntry> entries,
                                        boolean enableGroupSpecific,
                                        Integer totalGroups) {
        Object[][] data = ScheduleTableSupport.buildGridData(entries, timeSlots, enableGroupSpecific, totalGroups);
        scheduleTableModel.updateData(data);
        SwingUtilities.invokeLater(this::adjustRowHeight);
    }

    protected void clearScheduleGrid() {
        Object[][] data = ScheduleTableSupport.createEmptyScheduleGridData(timeSlots);
        scheduleTableModel.updateData(data);
        SwingUtilities.invokeLater(this::adjustRowHeight);
    }

    protected void loadScheduleAsync(Supplier<List<ScheduleEntry>> loader,
                                     boolean enableGroupSpecific,
                                     Integer totalGroups,
                                     Consumer<Exception> onError) {
        new javax.swing.SwingWorker<List<ScheduleEntry>, Void>() {
            @Override
            protected List<ScheduleEntry> doInBackground() {
                return loader.get();
            }

            @Override
            protected void done() {
                try {
                    List<ScheduleEntry> entries = get();
                    populateScheduleGrid(entries, enableGroupSpecific, totalGroups);
                } catch (Exception e) {
                    if (onError != null) onError.accept(e);
                }
            }
        }.execute();
    }

    protected void adjustRowHeight() {
        ScheduleTableSupport.adjustRowHeight(scheduleTable);
    }

    protected String formatTimeSlot(LocalTime[] slot) {
        return String.format("%s - %s", slot[0].toString(), slot[1].toString());
    }
}
