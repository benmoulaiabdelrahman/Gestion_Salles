package com.gestion.salles.views.Admin;


import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.dao.DepartementDAO; // Added DepartementDAO import
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Departement; // Added Departement import
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
 * Panel for Block Management in the Admin Dashboard.
 *
 * @author Gemini
 * @version 1.2 - Fixed styling issues.
 */
public class BlocManagementPanel extends JPanel implements RefreshablePanel {

    // ============================================================================ 
    // UI Components
    // ============================================================================ 
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JTable blocTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private java.awt.CardLayout cardLayout;
    private JPanel contentCardPanel;
    private final java.util.Map<String, Integer> originalColumnWidths = new java.util.HashMap<>();

    private final BlocDAO blocDAO;
    private final DepartementDAO departementDAO; // Declared DepartementDAO
    private List<Bloc> allBlocs;
    private List<Bloc> displayedBlocs;
    private List<Departement> allDepartements; // To store all departments
    private JComboBox<String> cmbDepartementFilter; // JComboBox for filtering by department
    private Departement selectedDepartementFilter; // Field to hold the selected department for filtering
    private Dashboard parentFrame; // Added field

    // Helper to set column visibility
    private void setColumnVisible(String columnName, boolean visible) {
        javax.swing.table.TableColumnModel columnModel = blocTable.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            if (column.getHeaderValue().equals(columnName)) {
                if (!visible) {
                    column.setMinWidth(0);
                    column.setMaxWidth(0);
                    column.setPreferredWidth(0);
                } else {
                    column.setMinWidth(15); // Minimum width when visible
                    column.setMaxWidth(Integer.MAX_VALUE);
                    // Restore original preferred width if available, otherwise a default
                    // Note: originalColumnNames and columnWidths maps are needed for this.
                    // For now, setting a default large width.
                    Integer originalWidth = originalColumnWidths.get(columnName);
                    column.setPreferredWidth(originalWidth != null ? originalWidth : 150); // Fallback to a reasonable default if not found
                }
                break;
            }
        }
    }


    public BlocManagementPanel(Dashboard parentFrame) { // Modified constructor
        this.blocDAO = new BlocDAO();
        this.departementDAO = new DepartementDAO(); // Initialize DepartementDAO
        this.displayedBlocs = new ArrayList<>();
        this.allDepartements = new ArrayList<>(); // Initialize allDepartements
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
                filterBlocs(txtSearch.getText());
            }
        });

        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddBloc);

        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditBloc);
        btnEdit.setEnabled(false);

        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteBloc);
        btnDelete.setEnabled(false);
        
        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);

        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel topToolbar = UIUtils.createSearchActionBar(
            txtSearch, 
            btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel
        );

        // Panel for the Filter (on the second row)
        JPanel filterPanel = new JPanel(new MigLayout("insets 0 10 10 10, fillx, gap 10", "[][grow, fill]")); // Adjusted insets and gap
        filterPanel.setOpaque(false);

        filterPanel.add(new JLabel("Faculté:")); // Label for the filter
        cmbDepartementFilter = new JComboBox<>();
        UIUtils.createStyledComboBox(cmbDepartementFilter); // Apply styled combo box
        cmbDepartementFilter.addItem("Tous"); // Default option
        cmbDepartementFilter.addActionListener(e -> {
            String selected = (String) cmbDepartementFilter.getSelectedItem();
            if ("Tous".equals(selected)) {
                selectedDepartementFilter = null;
            } else {
                selectedDepartementFilter = allDepartements.stream()
                        .filter(d -> d.getNom().equals(selected))
                        .findFirst()
                        .orElse(null);
            }
            filterBlocs(txtSearch.getText()); // Re-filter with current search text and new department filter
        });
        filterPanel.add(cmbDepartementFilter, "growx, h 35!"); // Re-added h 35!

        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, filterPanel);

        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);

        String[] columnNames = {"Nom", "Code", "Adresse", "Nombre d'étages", "Faculté"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        blocTable = (JTable) styledTableScrollPane.getViewport().getView();

        // Adjust column widths (example, will need fine-tuning)
        blocTable.getColumnModel().getColumn(0).setPreferredWidth(220); // Nom
        blocTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Code
        blocTable.getColumnModel().getColumn(2).setPreferredWidth(230); // Adresse
        blocTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Nombre d'étages
        blocTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Faculté

        // Store original preferred widths
        originalColumnWidths.put("Nom", 220);
        originalColumnWidths.put("Code", 100);
        originalColumnWidths.put("Adresse", 230);
        originalColumnWidths.put("Nombre d'étages", 150);
        originalColumnWidths.put("Faculté", 200);

        blocTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = blocTable.getSelectedRowCount();
                btnEdit.setEnabled(selectedRowCount == 1);
                btnDelete.setEnabled(selectedRowCount >= 1);
            }
        });

        blocTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = blocTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < blocTable.getRowCount()) {
                        if (!blocTable.isRowSelected(r)) {
                            blocTable.addRowSelectionInterval(r, r);
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
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                allBlocs = blocDAO.getAllBlocs();
                allDepartements = departementDAO.getAllActiveDepartements(); // Load all active departments
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Ensure background task completed
                    populateDepartementFilter(); // Populate the department filter
                    filterBlocs(txtSearch.getText()); // Apply current filter after data refresh
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(BlocManagementPanel.this,
                            "Erreur lors du chargement des données.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void populateDepartementFilter() {
        cmbDepartementFilter.removeAllItems();
        cmbDepartementFilter.addItem("Tous");
        for (Departement dept : allDepartements) {
            cmbDepartementFilter.addItem(dept.getNom());
        }
        cmbDepartementFilter.setSelectedItem("Tous"); // Reset selection
    }

    private void populateTable(List<Bloc> blocs) {
        tableModel.setRowCount(0);
        displayedBlocs.clear();
        if (blocs == null || blocs.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
            displayedBlocs.addAll(blocs);
            for (Bloc bloc : displayedBlocs) {
                tableModel.addRow(new Object[]{
                        bloc.getNom(),
                        bloc.getCode(),
                        bloc.getAdresse(),
                        bloc.getNombreEtages(),
                        bloc.getDepartement() != null ? bloc.getDepartement().getNom() : ""
                });
            }
        }
    }

    private void filterBlocs(String searchText) {
        // Reset all columns to visible initially (before applying filters)
        // This is important because the filter might be removed
        setColumnVisible("Faculté", true); 
        
        List<Bloc> filteredBlocs = allBlocs.stream()
                .filter(bloc -> {
                    boolean matchesSearch = true;
                    if (searchText != null && !searchText.trim().isEmpty()) {
                        String lowerCaseSearchText = searchText.trim().toLowerCase();
                        matchesSearch = bloc.getNom().toLowerCase().contains(lowerCaseSearchText) ||
                                bloc.getCode().toLowerCase().contains(lowerCaseSearchText) ||
                                (bloc.getDepartement() != null && bloc.getDepartement().getNom().toLowerCase().contains(lowerCaseSearchText));
                    }

                    boolean matchesDepartement = true;
                    if (selectedDepartementFilter != null) {
                        matchesDepartement = (bloc.getDepartement() != null && bloc.getDepartement().getId() == selectedDepartementFilter.getId());
                    }
                    return matchesSearch && matchesDepartement;
                })
                .collect(Collectors.toList());
        populateTable(filteredBlocs);

        // Hide "Faculté" column if a specific department is selected
        if (selectedDepartementFilter != null) {
            setColumnVisible("Faculté", false);
        }
    }

    private void onAddBloc(ActionEvent e) {
        parentFrame.showOverlay();
        try {
            BlocDialog dialog = new BlocDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                blocDAO,
                null,
                (success, message) -> { // DialogCallback implementation
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000);
                }
            );
            dialog.setVisible(true);
        } finally {
            parentFrame.hideOverlay();
        }
    }

    private void onEditBloc(ActionEvent e) {
        int selectedRow = blocTable.getSelectedRow();
        if (selectedRow != -1) {
            Bloc selectedBloc = displayedBlocs.get(selectedRow);
            parentFrame.showOverlay();
            try {
                BlocDialog dialog = new BlocDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    blocDAO,
                    selectedBloc,
                    (success, message) -> { // DialogCallback implementation
                        refreshData();
                        UIUtils.showTemporaryMessage(this, message, success, 3000);
                    }
                );
                dialog.setVisible(true);
            } finally {
                parentFrame.hideOverlay();
            }
        } else {
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner un département à modifier.", false, 3000);
        }
    }

    private void onDeleteBloc(ActionEvent e) {
        int[] selectedRows = blocTable.getSelectedRows();
        if (selectedRows.length > 0) {
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer ce département ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d départements sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean allSuccess = true;
                for (int selectedRow : selectedRows) {
                    Bloc selectedBloc = displayedBlocs.get(selectedRow);
                    boolean success = blocDAO.deleteBloc(selectedBloc.getId());
                    if (!success) {
                        allSuccess = false;
                        // Optionally, log which bloc failed to delete
                    }
                }

                if (allSuccess) {
                    UIUtils.showTemporaryMessage(this, String.format("%d département(s) supprimé(s) avec succès.", selectedRows.length), true, 3000);
                } else {
                    UIUtils.showTemporaryMessage(this, "Échec de la suppression d'un ou plusieurs départements.", false, 3000);
                }
                refreshData(); // Refresh table after deletion attempts
            }
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins un département à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();

        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        boolean hasDepartementFilter = selectedDepartementFilter != null;
        if (hasDepartementFilter) {
            dynamicFilterInfoBuilder.append("Faculté: ").append(selectedDepartementFilter.getNom());
        }
        String searchFilter = txtSearch.getText().trim();
        if (!searchFilter.isEmpty()) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" | ");
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }

        // Determine which columns to print based on active filters
        List<String> visibleColumnsForPrint = new ArrayList<>();
        String[] allColumnNames = {"Nom", "Code", "Adresse", "Nombre d'étages", "Faculté"};

        for (String colName : allColumnNames) {
            if (hasDepartementFilter && colName.equals("Faculté")) {
                continue;
            }
            visibleColumnsForPrint.add(colName);
        }

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Départements", dynamicFilterInfoBuilder.toString());
        job.setPrintable(new BlocTablePrintable(
            blocTable,
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
        boolean hasDepartementFilter = selectedDepartementFilter != null;
        Predicate<String> includeColumn = colName -> !(hasDepartementFilter && "Faculté".equals(colName));

        StringBuilder filterBuilder = new StringBuilder();
        if (hasDepartementFilter) {
            filterBuilder.append("Faculté: ").append(selectedDepartementFilter.getNom());
        }
        String searchFilter = txtSearch.getText().trim();
        if (!searchFilter.isEmpty()) {
            if (filterBuilder.length() > 0) filterBuilder.append(" | ");
            filterBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }

        ManagementExportUtils.exportTableToExcel(
            this,
            blocTable,
            "Départements",
            "Départements",
            filterBuilder.toString(),
            includeColumn
        );
    }


}
