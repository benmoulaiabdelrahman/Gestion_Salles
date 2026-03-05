package com.gestion.salles.views.shared.users;

import com.gestion.salles.models.User;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.List;

public final class UserManagementUIHelper {
    public static final String[] COLUMN_NAMES = {
        "Image", "Nom", "Prénom", "Email", "Téléphone", "Rôle", "Faculté", "Département"
    };

    private static final int[] COLUMN_WIDTHS = {50, 100, 100, 180, 100, 100, 120, 120};

    private UserManagementUIHelper() {}

    public static DefaultTableModel createUserTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ImageIcon.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void configureUserTable(JTable table, List<String> originalColumnNames) {
        table.setRowHeight(50);
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

        table.getColumn("Image")
            .setCellRenderer(new UserTableCellRenderers.ProfileImageRenderer("Nom", "Prénom", 40));
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i)
                .setCellRenderer(new UserTableCellRenderers.DashCellRenderer());
        }
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
