package com.gestion.salles.views.Admin;

import com.gestion.salles.models.ScheduleEntry;

import javax.swing.table.AbstractTableModel;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger; // Added

public class ScheduleTableModel extends AbstractTableModel {

    private static final Logger LOGGER = Logger.getLogger(ScheduleTableModel.class.getName()); // Added

    private String[] columnNames;
    private Object[][] data; // Will hold ScheduleEntry objects or null for free slots

    public ScheduleTableModel(String[] columnNames) {
        this(columnNames, new Object[0][columnNames.length]); // Initialize with empty data for columns
    }
    
    public ScheduleTableModel(String[] columnNames, Object[][] data) {
        this.columnNames = columnNames;
        this.data = data;
    }

    public void updateData(Object[][] newData) {
        if (newData == null || newData.length == 0) {
            this.data = new Object[0][getColumnCount()]; // Set empty data with correct column count
            fireTableDataChanged();
            return;
        }

        // Validate column count for each row in newData
        int expectedColumnCount = getColumnCount(); // This should be 6
        for (int i = 0; i < newData.length; i++) {
            if (newData[i] == null || newData[i].length != expectedColumnCount) {
                LOGGER.warning(String.format("Invalid row data at index %d. Expected %d columns, but found %d. Skipping update.",
                        i, expectedColumnCount, (newData[i] != null ? newData[i].length : 0)));
                // Optionally, could throw an IllegalArgumentException, but logging and skipping is safer for UI
                return; 
            }
        }
        this.data = newData;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data.length; // 8 time slots
    }

    @Override
    public int getColumnCount() {
        return columnNames.length; // "Heure" + 7 days = 8 columns
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public String[] getColumnNames() {
        return columnNames;
    }
}
