package com.gestion.salles.views.Admin;

import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.dao.NiveauDAO;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Niveau;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
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
 * Panel for Niveau Management in the Admin Dashboard.
 *
 * @author Gemini
 * @version 1.0
 */
public class NiveauManagementPanel extends JPanel implements RefreshablePanel {

    // ============================================================================ 
    // UI Components
    // ============================================================================ 
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JComboBox<String> comboFilterDepartement; // Filter by department (Faculté)
    private JComboBox<String> comboFilterBloc; // Filter by bloc (Département)
    private JTable niveauTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private java.awt.CardLayout cardLayout;
    private JPanel contentCardPanel;

    // Class Member Declarations (Corrected and Initialized)
    private final NiveauDAO niveauDAO = new NiveauDAO();
    private final DepartementDAO departementDAO = new DepartementDAO();
    private final com.gestion.salles.dao.BlocDAO blocDAO = new com.gestion.salles.dao.BlocDAO();
    private List<Niveau> allNiveaux = new ArrayList<>();
    private List<Niveau> displayedNiveaux = new ArrayList<>();
    private List<Departement> allDepartments = new ArrayList<>();
    private List<com.gestion.salles.models.Bloc> allBlocs = new ArrayList<>();

    private final List<String> originalColumnNames = new ArrayList<>();

    private Dashboard parentFrame; // Added field

    // Helper Methods (Grouped and Placed Before Constructor for clarity and scope)











    
    // Helper to set column visibility
    private void setColumnVisible(String columnName, boolean visible) {
        javax.swing.table.TableColumnModel columnModel = niveauTable.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = columnModel.getColumn(i);
            if (column.getHeaderValue().equals(columnName)) {
                if (!visible) {
                    column.setMinWidth(0);
                    column.setMaxWidth(0);
                    column.setPreferredWidth(0);
                } else {
                    column.setMinWidth(15);
                    column.setMaxWidth(Integer.MAX_VALUE);
                    // Since originalColumnWidths map is removed, use a reasonable default
                    column.setPreferredWidth(100); 
                }
                break;
            }
        }
    }


    public NiveauManagementPanel(Dashboard parentFrame) { // Modified constructor
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

        txtSearch = UIUtils.createStyledTextField("Rechercher un niveau par nom, code...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddNiveau);

        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditNiveau);
        btnEdit.setEnabled(false);

        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteNiveau);
        btnDelete.setEnabled(false);
        
        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);

        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel topToolbar = UIUtils.createSearchActionBar(
            txtSearch, 
            btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel
        );

        // Filter Panel (by department and bloc)
        JPanel filterPanel = new JPanel(new MigLayout("insets 0 10 10 10, fillx, gap 10", "[right][fill,grow,sg g1][right][fill,grow,sg g1]")); // Adjusted MigLayout for two filters
        filterPanel.setOpaque(false);

        comboFilterDepartement = UIUtils.createStyledComboBox(new JComboBox<>());
        comboFilterDepartement.addActionListener(e -> applyFilters()); // Use ActionListener instead of ItemListener for simplicity, or re-implement ItemListener to call applyFilters.
        
        comboFilterBloc = UIUtils.createStyledComboBox(new JComboBox<>());
        comboFilterBloc.addActionListener(e -> applyFilters());

        filterPanel.add(new JLabel("Faculté:"));
        filterPanel.add(comboFilterDepartement, "growx, h 35!");
        filterPanel.add(new JLabel("Département:")); // Added Bloc label (now "Département")
        filterPanel.add(comboFilterBloc, "growx, h 35!"); // Added Bloc combo box

        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, filterPanel);
        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);
        
        // --- Table Setup ---
        String[] columnNames = {"Nom", "Code", "Nb Étudiants", "Nb Groupes", "Année Académique", "Faculté", "Département"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        niveauTable = (JTable) styledTableScrollPane.getViewport().getView();

        // Adjust column widths
        niveauTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Nom
        niveauTable.getColumnModel().getColumn(1).setPreferredWidth(80); // Code
        niveauTable.getColumnModel().getColumn(2).setPreferredWidth(80); // Nb Étudiants
        niveauTable.getColumnModel().getColumn(3).setPreferredWidth(80); // Nb Groupes
        niveauTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Année Académique
        niveauTable.getColumnModel().getColumn(5).setPreferredWidth(120); // Faculté
        niveauTable.getColumnModel().getColumn(6).setPreferredWidth(120); // Département

        // Populate originalColumnNames so onPrint() has a non-empty list to iterate.
        // Without this, visibleColumnsForPrint is always empty and the printable
        // returns NO_SUCH_PAGE immediately → blank page.
        for (int i = 0; i < niveauTable.getColumnCount(); i++) {
            originalColumnNames.add(niveauTable.getColumnModel().getColumn(i).getHeaderValue().toString());
        }



        niveauTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = niveauTable.getSelectedRowCount();
                btnEdit.setEnabled(selectedRowCount == 1);
                btnDelete.setEnabled(selectedRowCount >= 1);
            }
        });

        niveauTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = niveauTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < niveauTable.getRowCount()) {
                        if (!niveauTable.isRowSelected(r)) {
                            niveauTable.addRowSelectionInterval(r, r);
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
    } // Correctly close initComponents here


    private static class DataHolder {
        List<Niveau> niveaux;
        List<Departement> departments;
        List<com.gestion.salles.models.Bloc> blocs;
    }

    @Override
    public void refreshData() {
        SwingWorker<DataHolder, Void> worker = new SwingWorker<>() {
            @Override
            protected DataHolder doInBackground() throws Exception {
                DataHolder data = new DataHolder();
                data.niveaux = niveauDAO.getAllNiveaux();
                data.departments = departementDAO.getAllDepartements();
                data.blocs = blocDAO.getAllBlocs();
                return data;
            }

            @Override
            protected void done() {
                try {
                    DataHolder data = get();
                    allNiveaux = data.niveaux;
                    allDepartments = data.departments;
                    allBlocs = data.blocs;
                    populateTable(allNiveaux);
                    populateFilters();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(NiveauManagementPanel.this,
                        "Erreur lors du chargement des données.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Niveau> niveaux) {
        tableModel.setRowCount(0);
        this.displayedNiveaux.clear(); // Clear the class field
        if (niveaux == null || niveaux.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
            this.displayedNiveaux.addAll(niveaux); // Add to the class field
            for (Niveau niveau : this.displayedNiveaux) {
                tableModel.addRow(new Object[]{
                    niveau.getNom(),
                    niveau.getCode(),
                    niveau.getNombreEtudiants(),
                    niveau.getNombreGroupes(), // NEW
                    niveau.getAnneeAcademique(),
                    niveau.getDepartementName(),
                    niveau.getNomBloc()
                });
            }
        }
    }
    
    private void populateFilters() {
        // Populate Department Filter
        String currentDepartementSelection = (String) comboFilterDepartement.getSelectedItem(); // Preserve selection
        comboFilterDepartement.removeAllItems();
        comboFilterDepartement.addItem("Tous");
        allDepartments.stream()
                .map(Departement::getNom)
                .sorted()
                .forEach(comboFilterDepartement::addItem);
        comboFilterDepartement.setSelectedItem(currentDepartementSelection); // Restore selection

        // Populate Bloc Filter
        String currentBlocSelection = (String) comboFilterBloc.getSelectedItem(); // Preserve selection
        comboFilterBloc.removeAllItems();
        comboFilterBloc.addItem("Tous");
        allBlocs.stream()
                .map(com.gestion.salles.models.Bloc::getNom)
                .sorted()
                .forEach(comboFilterBloc::addItem);
        comboFilterBloc.setSelectedItem(currentBlocSelection); // Restore selection
    }

    private void applyFilters() {
        // Store current selections before clearing/repopulating
        String currentSelectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        String currentSelectedBloc = (String) comboFilterBloc.getSelectedItem();

        // Remove listeners to prevent recursive calls during programmatic updates


        // --- Repopulate comboFilterDepartement ---
        comboFilterDepartement.removeAllItems();
        comboFilterDepartement.addItem("Tous");
        List<String> departementNamesForFilter = new ArrayList<>();
        if (currentSelectedBloc != null && !currentSelectedBloc.equals("Tous")) {
            List<Integer> departementIdsInSelectedBloc = allNiveaux.stream()
                .filter(niveau -> currentSelectedBloc.equals(niveau.getNomBloc()))
                .map(Niveau::getIdDepartement)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

            allDepartments.stream()
                .filter(departement -> departementIdsInSelectedBloc.contains(departement.getId()))
                .map(Departement::getNom)
                .sorted()
                .forEach(departementNamesForFilter::add);
        } else {
            allDepartments.stream()
                .map(Departement::getNom)
                .sorted()
                .forEach(departementNamesForFilter::add);
        }
        departementNamesForFilter.forEach(comboFilterDepartement::addItem);
        comboFilterDepartement.setSelectedItem(currentSelectedDepartement);
        if (comboFilterDepartement.getSelectedItem() == null) {
            comboFilterDepartement.setSelectedItem("Tous");
        }

        // --- Repopulate comboFilterBloc ---
        comboFilterBloc.removeAllItems();
        comboFilterBloc.addItem("Tous");
        List<String> blocNamesForFilter = new ArrayList<>();
        String actualSelectedDepartementForBlocs = (String) comboFilterDepartement.getSelectedItem(); // Use potentially updated selection
        if (actualSelectedDepartementForBlocs != null && !actualSelectedDepartementForBlocs.equals("Tous")) {
             List<Integer> blocIdsInSelectedDepartment = allNiveaux.stream()
                .filter(niveau -> actualSelectedDepartementForBlocs.equals(niveau.getDepartementName()))
                .map(Niveau::getIdBloc)
                .distinct()
                .collect(Collectors.toList());

            allBlocs.stream()
                .filter(bloc -> blocIdsInSelectedDepartment.contains(bloc.getId()))
                .map(com.gestion.salles.models.Bloc::getNom)
                .sorted()
                .forEach(blocNamesForFilter::add);
        } else {
            allBlocs.stream()
                .map(com.gestion.salles.models.Bloc::getNom)
                .sorted()
                .forEach(blocNamesForFilter::add);
        }
        blocNamesForFilter.forEach(comboFilterBloc::addItem);
        comboFilterBloc.setSelectedItem(currentSelectedBloc);
        if (comboFilterBloc.getSelectedItem() == null) {
            comboFilterBloc.setSelectedItem("Tous");
        }




        // --- Apply Actual Filters to Table Data ---
        List<Niveau> filteredNiveaux = new ArrayList<>(allNiveaux);

        String selectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        boolean departementFilterActive = selectedDepartement != null && !selectedDepartement.equals("Tous");
        if (departementFilterActive) {
            filteredNiveaux.removeIf(niveau -> niveau.getDepartementName() == null || !niveau.getDepartementName().equals(selectedDepartement));
            setColumnVisible("Faculté", false); // Hide column if filter is active
        } else {
            setColumnVisible("Faculté", true); // Show column if filter is inactive
        }


        String selectedBloc = (String) comboFilterBloc.getSelectedItem();
        boolean blocFilterActive = selectedBloc != null && !selectedBloc.equals("Tous");
        if (blocFilterActive) {
            filteredNiveaux.removeIf(niveau -> niveau.getNomBloc() == null || !niveau.getNomBloc().equals(selectedBloc));
            setColumnVisible("Département", false); // Hide column if filter is active
        } else {
            setColumnVisible("Département", true); // Show column if filter is inactive
        }

        String searchText = txtSearch.getText().trim();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredNiveaux = filteredNiveaux.stream()
                    .filter(niveau -> niveau.getNom().toLowerCase().contains(lowerCaseSearchText) ||
                                      niveau.getCode().toLowerCase().contains(lowerCaseSearchText) ||
                                      (niveau.getDepartementName() != null && niveau.getDepartementName().toLowerCase().contains(lowerCaseSearchText)) ||
                                      (niveau.getNomBloc() != null && niveau.getNomBloc().toLowerCase().contains(lowerCaseSearchText)) ||
                                      String.valueOf(niveau.getNombreEtudiants()).contains(lowerCaseSearchText) ||
                                      String.valueOf(niveau.getNombreGroupes()).contains(lowerCaseSearchText) ||
                                      (niveau.getAnneeAcademique() != null && niveau.getAnneeAcademique().toLowerCase().contains(lowerCaseSearchText)))
                    .collect(Collectors.toList());
        }

        populateTable(filteredNiveaux);
    }

    private void onAddNiveau(ActionEvent e) {
        parentFrame.showOverlay();
        try {
            NiveauDialog dialog = new NiveauDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                niveauDAO,
                departementDAO.getAllActiveDepartements(), // Pass only active departments
                blocDAO, // Pass blocDAO
                allNiveaux,
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

    private void onEditNiveau(ActionEvent e) {
        int selectedRow = niveauTable.getSelectedRow();
        if (selectedRow != -1) {
            Niveau selectedNiveau = displayedNiveaux.get(selectedRow); // Directly get from displayedNiveaux

            // selectedNiveau will never be null here if selectedRow is valid
            parentFrame.showOverlay();
            try {
                NiveauDialog dialog = new NiveauDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    niveauDAO,
                    departementDAO.getAllActiveDepartements(), // Pass only active departments
                    blocDAO, // Pass blocDAO
                    allNiveaux,
                    selectedNiveau,
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
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner un niveau à modifier.", false, 3000);
        }
    }

    private void onDeleteNiveau(ActionEvent e) {
        int[] selectedRows = niveauTable.getSelectedRows();
        if (selectedRows.length > 0) {
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer ce niveau ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d éléments sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean allSuccess = true;
                for (int selectedRow : selectedRows) {
                    Niveau selectedNiveau = displayedNiveaux.get(selectedRow); // Directly get from displayedNiveaux
                    
                    // selectedNiveau will never be null here if selectedRow is valid
                    boolean success = niveauDAO.deleteNiveau(selectedNiveau.getId());
                    if (!success) {
                        allSuccess = false;
                        // Optionally, log which niveau failed to delete
                    }
                }

                if (allSuccess) {
                    UIUtils.showTemporaryMessage(this, String.format("%d niveau(x) supprimé(s) avec succès.", selectedRows.length), true, 3000);
                } else {
                    UIUtils.showTemporaryMessage(this, "Échec de la suppression d'un ou plusieurs niveaux.", false, 3000);
                }
                refreshData(); // Refresh table after deletion attempts
            }
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins un niveau à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        
        String searchFilter = txtSearch.getText().trim();
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        String selectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        String selectedBloc = (String) comboFilterBloc.getSelectedItem();
        boolean hasDepartementFilter = selectedDepartement != null && !selectedDepartement.equals("Tous");
        boolean hasBlocFilter = selectedBloc != null && !selectedBloc.equals("Tous");

        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasDepartementFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Faculté: ").append(selectedDepartement);
        }
        if (hasBlocFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Département: ").append(selectedBloc);
        }

        // Determine which columns to print based on active filters
        List<String> visibleColumnsForPrint = new ArrayList<>();
        // The originalColumnNames already contains the correct order and the new "Nb Groupes"
        // We just need to make sure the filtering logic is correct for existing columns
        for (String colName : originalColumnNames) {
            // Apply filtering logic for existing columns if needed
            if (hasDepartementFilter && colName.equals("Faculté")) {
                continue; // Skip this column if filter is active
            }
            if (hasBlocFilter && colName.equals("Département")) {
                continue; // Skip this column if filter is active
            }
            visibleColumnsForPrint.add(colName);
        }

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Niveaux", dynamicFilterInfoBuilder.toString());
        job.setPrintable(new NiveauTablePrintable(
            niveauTable,
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
        String selectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        String selectedBloc = (String) comboFilterBloc.getSelectedItem();
        boolean hasDepartementFilter = selectedDepartement != null && !selectedDepartement.equals("Tous");
        boolean hasBlocFilter = selectedBloc != null && !selectedBloc.equals("Tous");

        Predicate<String> includeColumn = colName -> {
            if (hasDepartementFilter && "Faculté".equals(colName)) return false;
            if (hasBlocFilter && "Département".equals(colName)) return false;
            return true;
        };

        StringBuilder filterBuilder = new StringBuilder();
        String searchFilter = txtSearch.getText().trim();
        if (!searchFilter.isEmpty()) {
            filterBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasDepartementFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Faculté: ").append(selectedDepartement);
        }
        if (hasBlocFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Département: ").append(selectedBloc);
        }

        ManagementExportUtils.exportTableToExcel(
            this,
            niveauTable,
            "Niveaux",
            "Niveaux",
            filterBuilder.toString(),
            includeColumn
        );
    }


}
