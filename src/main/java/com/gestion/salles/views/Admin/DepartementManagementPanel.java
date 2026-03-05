package com.gestion.salles.views.Admin;


import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.models.Departement;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils; // Added UIUtils import
import com.gestion.salles.utils.DialogCallback; // Added DialogCallback import
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;

/**
 * Panel for Department Management in the Admin Dashboard.
 *
 * @author Gemini
 * @version 1.3 - Fixed styling issues and populateTable.
 */
public class DepartementManagementPanel extends JPanel implements RefreshablePanel {

    // ============================================================================ 
    // UI Components
    // ============================================================================ 
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JTable departementTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private java.awt.CardLayout cardLayout;
    private JPanel contentCardPanel;

    private final DepartementDAO departementDAO;
    private List<Departement> allDepartements;
    private List<Departement> displayedDepartements;
    private Dashboard parentFrame; // Added field

    public DepartementManagementPanel(Dashboard parentFrame) { // Modified constructor
        this.departementDAO = new DepartementDAO();
        this.displayedDepartements = new ArrayList<>();
        this.parentFrame = parentFrame; // Assigned parentFrame
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new java.awt.BorderLayout(10, 10));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");

        JPanel centerContentPanel = new JPanel(new java.awt.BorderLayout(10, 10));
        centerContentPanel.setOpaque(false);

        txtSearch = UIUtils.createStyledTextField("Rechercher un département par nom, code...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddDepartement); // Added listener

        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditDepartement);
        btnEdit.setEnabled(false);

        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteDepartement);
        btnDelete.setEnabled(false);
        
        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);

        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel topToolbar = UIUtils.createSearchActionBar(
            txtSearch, 
            btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel
        );
        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, null);
        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);

        String[] columnNames = {"Nom", "Code", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        departementTable = (JTable) styledTableScrollPane.getViewport().getView();

        // Adjust column widths (example, will need fine-tuning)
        departementTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Nom
        departementTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Code
        departementTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Description

        departementTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = departementTable.getSelectedRowCount();
                btnEdit.setEnabled(selectedRowCount == 1);
                btnDelete.setEnabled(selectedRowCount >= 1);
            }
        });

        departementTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = departementTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < departementTable.getRowCount()) {
                        if (!departementTable.isRowSelected(r)) {
                            departementTable.addRowSelectionInterval(r, r);
                        }
                    }
                }
            }
        });

        lblNoResults = new JLabel("Aucun résultat trouvé pour votre recherche.", SwingConstants.CENTER);
        lblNoResults.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblNoResults.setFont(lblNoResults.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        
        this.cardLayout = new java.awt.CardLayout();
        this.contentCardPanel = new JPanel(this.cardLayout);
        contentCardPanel.add(styledTableScrollPane, "tableCard");
        contentCardPanel.add(lblNoResults, "noResultsCard");
        
        centerContentPanel.add(contentCardPanel, java.awt.BorderLayout.CENTER);
        add(centerContentPanel, java.awt.BorderLayout.CENTER);

    }

    @Override
    public void refreshData() {
        SwingWorker<List<Departement>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Departement> doInBackground() throws Exception {
                return departementDAO.getAllDepartements();
            }

            @Override
            protected void done() {
                try {
                    allDepartements = get();
                    populateTable(allDepartements);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(DepartementManagementPanel.this,
                            "Erreur lors du chargement des données.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Departement> departements) {
        tableModel.setRowCount(0);
        displayedDepartements.clear();
        if (departements == null || departements.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
            displayedDepartements.addAll(departements);
            for (Departement dept : displayedDepartements) {
                tableModel.addRow(new Object[]{
                        dept.getNom(),
                        dept.getCode(),
                        dept.getDescription()
                });
            }
        }
    }

    private void applyFilters() {
        String searchText = txtSearch.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            populateTable(allDepartements);
            return;
        }
        String lowerCaseSearchText = searchText.trim().toLowerCase();
        List<Departement> filteredDepts = allDepartements.stream()
                .filter(dept -> dept.getNom().toLowerCase().contains(lowerCaseSearchText) ||
                                 dept.getCode().toLowerCase().contains(lowerCaseSearchText))
                .collect(Collectors.toList());
        populateTable(filteredDepts);
    }

    private void onAddDepartement(ActionEvent e) {
        parentFrame.showOverlay();
        try {
            DepartementDialog dialog = new DepartementDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                departementDAO,
                null,
                (success, message) -> { // DialogCallback implementation
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000); // Fixed success parameter
                }
            );
            dialog.setVisible(true);
        } finally {
            parentFrame.hideOverlay();
        }
    }

    private void onEditDepartement(ActionEvent e) {
        int selectedRow = departementTable.getSelectedRow();
        if (selectedRow != -1) {
            Departement selectedDepartement = displayedDepartements.get(selectedRow);
            parentFrame.showOverlay();
            try {
                DepartementDialog dialog = new DepartementDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    departementDAO,
                    selectedDepartement,
                    (success, message) -> { // DialogCallback implementation
                        refreshData();
                        UIUtils.showTemporaryMessage(this, message, success, 3000); // Fixed success parameter
                    }
                );
                dialog.setVisible(true);
            } finally {
                parentFrame.hideOverlay();
            }
        } else {
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner une faculté à modifier.", false, 3000);
        }
    }

    private void onDeleteDepartement(ActionEvent e) {
        int[] selectedRows = departementTable.getSelectedRows();
        if (selectedRows.length > 0) {
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer cette faculté ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d éléments sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean allSuccess = true;
                for (int selectedRow : selectedRows) {
                    Departement selectedDepartement = displayedDepartements.get(selectedRow);
                    boolean success = departementDAO.deleteDepartement(selectedDepartement.getId());
                    if (!success) {
                        allSuccess = false;
                        // Optionally, log which departement failed to delete
                    }
                }

                if (allSuccess) {
                    UIUtils.showTemporaryMessage(this, String.format("%d faculté(s) supprimée(s) avec succès.", selectedRows.length), true, 3000);
                } else {
                    UIUtils.showTemporaryMessage(this, "Échec de la suppression d'une ou plusieurs facultés.", false, 3000);
                }
                refreshData(); // Refresh table after deletion attempts
            }
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins une faculté à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        String searchFilter = txtSearch.getText().trim();
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        
        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }

        List<String> visibleColumnsForPrint = new ArrayList<>();
        for (int i = 0; i < departementTable.getColumnCount(); i++) {
            visibleColumnsForPrint.add(departementTable.getColumnName(i));
        }

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Facultés", dynamicFilterInfoBuilder.toString());
        job.setPrintable(new DepartementTablePrintable(
            departementTable,
            ManagementExportUtils.UNIVERSITY_TITLE,
            dynamicInfo,
            visibleColumnsForPrint
        ));

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Document envoyé à l'imprimante avec succès.", "Impression Réussie", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.awt.print.PrinterException ex) {
                System.err.format("Cannot print %s%n", ex.getMessage());
                JOptionPane.showMessageDialog(this, "Échec de l'impression. Veuillez vérifier la connexion de votre imprimante ou les paramètres.", "Erreur d'Impression", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSaveExcel(ActionEvent e) {
        String searchFilter = txtSearch.getText().trim();
        String filterInfo = searchFilter.isEmpty() ? "" : "Filtre: \"" + searchFilter + "\"";

        ManagementExportUtils.exportTableToExcel(
            this,
            departementTable,
            "Facultés",
            "Facultés",
            filterInfo,
            (Predicate<String>) null
        );
    }



}
