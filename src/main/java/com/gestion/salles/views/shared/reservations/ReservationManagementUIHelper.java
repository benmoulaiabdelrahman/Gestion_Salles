package com.gestion.salles.views.shared.reservations;

import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.User;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.util.List;

public final class ReservationManagementUIHelper {
    public static final String[] COLUMN_NAMES = {
        "Salle", "Titre", "Date", "Heure Debut", "Heure Fin", "Statut"
    };

    private static final int[] COLUMN_WIDTHS = {100, 150, 100, 100, 100, 80};

    @FunctionalInterface
    public interface ReservationRowProvider {
        Reservation getReservationAt(int modelRow);
    }

    private ReservationManagementUIHelper() {}

    public static DefaultTableModel createReservationTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void configureReservationTable(JTable table,
                                                  List<String> originalColumnNames,
                                                  ReservationRowProvider rowProvider) {
        table.setRowHeight(30);
        table.setFont(table.getFont().deriveFont(13f));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount() && i < COLUMN_WIDTHS.length; i++) {
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(COLUMN_WIDTHS[i]);
        }

        if (originalColumnNames != null) {
            originalColumnNames.clear();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                originalColumnNames.add(columnModel.getColumn(i).getHeaderValue().toString());
            }
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(t.getSelectionBackground());
                    c.setForeground(t.getSelectionForeground());
                    return c;
                }
                c.setBackground(t.getBackground());
                c.setForeground(t.getForeground());
                if (rowProvider != null) {
                    int modelRow = t.convertRowIndexToModel(row);
                    Reservation res = rowProvider.getReservationAt(modelRow);
                    if (res != null && res.isPastReservation()) {
                        c.setBackground(java.awt.Color.LIGHT_GRAY);
                    }
                }
                return c;
            }
        });
    }

    public static Timer installSearchDebounce(JTextField field, int delayMs, Runnable onDebouncedChange) {
        Timer timer = new Timer(delayMs, e -> onDebouncedChange.run());
        timer.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                timer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                timer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                timer.restart();
            }
        });
        return timer;
    }

    public static String buildScopeSubtitle(User user) {
        String faculte = user.getNomDepartement() != null ? user.getNomDepartement() : "-";
        String departement = user.getNomBloc() != null ? user.getNomBloc() : "-";
        return "Faculté: " + faculte + " - Département: " + departement;
    }
}
