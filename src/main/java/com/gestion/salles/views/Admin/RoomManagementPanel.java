package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.dao.RoomDAO;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Room;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.awt.Frame;
import java.util.function.Predicate;

import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;


/**
 * Panel for Room Management in the Admin Dashboard.
 *
 * @author Gemini
 * @version 1.0
 */
public class RoomManagementPanel extends JPanel implements RefreshablePanel {

    // UI Components
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JComboBox<String> comboFilterDepartement;
    private JComboBox<String> comboFilterBloc;
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private java.awt.CardLayout cardLayout;
    private JPanel contentCardPanel;

    private final RoomDAO roomDAO;
    private final DepartementDAO departementDAO;
    private final BlocDAO blocDAO;
    private List<Room> allRooms;
    private List<Departement> allDepartments;
    private List<Bloc> allBlocs;
    private List<Room> displayedRooms; // Declared displayedRooms

    private final List<String> originalColumnNames = new ArrayList<>();
    private final java.util.Map<String, Integer> columnWidths = new java.util.HashMap<>();
    private final Gson gson = new Gson();
    private Dashboard parentFrame;

    public RoomManagementPanel(Dashboard parentFrame) {
        this.roomDAO = new RoomDAO();
        this.departementDAO = new DepartementDAO();
        this.blocDAO = new BlocDAO();
        this.allRooms = new ArrayList<>();
        this.displayedRooms = new ArrayList<>();
        this.parentFrame = parentFrame;
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new java.awt.BorderLayout(10, 10));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");

        JPanel centerContentPanel = new JPanel(new java.awt.BorderLayout(10, 10));
        centerContentPanel.setOpaque(false);

        txtSearch = UIUtils.createStyledTextField("Rechercher une salle par nom, type...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddRoom); // Changed listener

        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditRoom); // Changed listener
        btnEdit.setEnabled(false);

        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteRoom); // Changed listener
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

        comboFilterDepartement = new JComboBox<>();
        comboFilterDepartement.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + UIUtils.colorToHex(ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.FOCUS_BORDER));
        comboFilterDepartement.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedDepartement = (String) e.getItem();
                updateBlocFilterOptions(selectedDepartement);        // Update département filter based on faculté
            }
        });
        
        comboFilterBloc = new JComboBox<>(); // Initialized Bloc JComboBox
        comboFilterBloc.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + UIUtils.colorToHex(ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.FOCUS_BORDER));
        comboFilterBloc.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedBloc = (String) e.getItem();
                updateDepartementFilterOptions(selectedBloc); // Update faculté filter based on département
                applyFilters();
            }
        });

        filterPanel.add(new JLabel("Faculté:"));
        filterPanel.add(comboFilterDepartement, "growx, h 35!");
        filterPanel.add(new JLabel("Département:")); // Added Bloc label
        filterPanel.add(comboFilterBloc, "growx, h 35!"); // Added Bloc combo box

        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, filterPanel);
        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);

        String[] columnNames = {"Nom", "Type", "Capacité", "Équipements", "Faculté", "Département"}; // Adjusted column names
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        roomTable = (JTable) styledTableScrollPane.getViewport().getView(); // Extract the JTable instance

        // Adjust column widths (example, will need fine-tuning)
        roomTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Nom
        roomTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Type
        roomTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Capacité
        roomTable.getColumnModel().getColumn(3).setPreferredWidth(250); // Description
        roomTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Faculté
        roomTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Département

        // Store original column names and widths
        for (int i = 0; i < roomTable.getColumnCount(); i++) {
            String colName = roomTable.getColumnModel().getColumn(i).getHeaderValue().toString();
            originalColumnNames.add(colName);
            columnWidths.put(colName, roomTable.getColumnModel().getColumn(i).getPreferredWidth());
        }

        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = roomTable.getSelectedRowCount();
                btnEdit.setEnabled(selectedRowCount == 1);
                btnDelete.setEnabled(selectedRowCount >= 1);
            }
        });

        roomTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = roomTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < roomTable.getRowCount()) {
                        if (!roomTable.isRowSelected(r)) {
                            roomTable.addRowSelectionInterval(r, r);
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
            List<Room> fetchedRooms; // Changed from Niveau
            List<Departement> fetchedDepartments;
            List<Bloc> fetchedBlocs; // Added fetchedBlocs

            @Override
            protected Void doInBackground() throws Exception {
                fetchedRooms = roomDAO.getAllRooms(); // Changed from NiveauDAO
                fetchedDepartments = departementDAO.getAllDepartements();
                fetchedBlocs = blocDAO.getAllBlocs(); // Fetched Blocs
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    allRooms = fetchedRooms; // Changed from allNiveaux
                    allDepartments = fetchedDepartments;
                    allBlocs = fetchedBlocs; // Assign allBlocs
                    populateTable(allRooms); // Changed from allNiveaux
                    populateFilters();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(RoomManagementPanel.this,
                        "Erreur lors du chargement des données des salles.", "Erreur", JOptionPane.ERROR_MESSAGE); // Adjusted message
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Room> rooms) { // Changed argument type
        tableModel.setRowCount(0);
        this.displayedRooms.clear(); // Clear the class field
        
        if (rooms == null || rooms.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
            this.displayedRooms.addAll(rooms); // Update the class field
            for (Room room : this.displayedRooms) { // Iterate over the class field
                String equipmentJson = room.getEquipment();
                String equipmentString = "";
                if (equipmentJson != null && !equipmentJson.isEmpty()) {
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    try {
                        List<String> equipmentList = gson.fromJson(equipmentJson, listType);
                        if (equipmentList != null) {
                            equipmentString = String.join(", ", equipmentList);
                        }
                    } catch (com.google.gson.JsonSyntaxException e) {
                        // Handle cases where the string is not valid JSON
                        equipmentString = equipmentJson; // Show the raw string if it's not JSON
                    }
                }

                tableModel.addRow(new Object[]{
                    room.getName(), // Adjusted to Room properties
                    room.getTypeSalle(),
                    room.getCapacity(),
                    equipmentString,
                    room.getDepartmentName(),
                    room.getBlockName()
                });
            }
        }
    }
    
    private void populateFilters() {
        // Populate Department Filter
        removeItemListeners(comboFilterDepartement); // Remove listeners before repopulating
        comboFilterDepartement.removeAllItems();
        comboFilterDepartement.addItem("Tous");
        allDepartments.stream()
                .map(Departement::getNom)
                .sorted()
                .forEach(comboFilterDepartement::addItem);
        addItemListener(comboFilterDepartement, e -> { // Re-add listener
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedDepartement = (String) e.getItem();
                updateBlocFilterOptions(selectedDepartement);
                applyFilters();
            }
        });

        // Populate Bloc Filter
        removeItemListeners(comboFilterBloc); // Remove listeners before repopulating
        comboFilterBloc.removeAllItems();
        comboFilterBloc.addItem("Tous");
        allBlocs.stream()
                .map(Bloc::getNom)
                .sorted()
                .forEach(comboFilterBloc::addItem);
        addItemListener(comboFilterBloc, e -> { // Re-add listener
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedBloc = (String) e.getItem();
                updateDepartementFilterOptions(selectedBloc);
                applyFilters();
            }
        });
        
        applyFilters();
    }

    private void applyFilters() {
        // Reset all columns to visible initially
        for (String colName : originalColumnNames) {
            setColumnVisible(colName, true);
        }

        List<Room> filteredRooms = new ArrayList<>(allRooms);

        String selectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        boolean departementFilterActive = selectedDepartement != null && !selectedDepartement.equals("Tous");
        if (departementFilterActive) {
            filteredRooms.removeIf(room -> room.getDepartmentName() == null || !room.getDepartmentName().equals(selectedDepartement));
            setColumnVisible("Faculté", false); // Hide column if filter is active
        }

        String selectedBloc = (String) comboFilterBloc.getSelectedItem();
        boolean blocFilterActive = selectedBloc != null && !selectedBloc.equals("Tous");
        if (blocFilterActive) {
            filteredRooms.removeIf(room -> room.getBlockName() == null || !room.getBlockName().equals(selectedBloc));
            setColumnVisible("Département", false); // Hide column if filter is active
        }

        String searchText = txtSearch.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getName().toLowerCase().contains(lowerCaseSearchText) ||
                                      room.getTypeSalle().toLowerCase().contains(lowerCaseSearchText) ||
                                      String.valueOf(room.getCapacity()).contains(lowerCaseSearchText) ||
                                      (room.getEquipment() != null && room.getEquipment().toLowerCase().contains(lowerCaseSearchText)) ||
                                      (room.getDepartmentName() != null && room.getDepartmentName().toLowerCase().contains(lowerCaseSearchText)) ||
                                      (room.getBlockName() != null && room.getBlockName().toLowerCase().contains(lowerCaseSearchText)))
                    .collect(Collectors.toList());
        }

        populateTable(filteredRooms);
    }

    private void onAddRoom(ActionEvent e) { // Changed method name
        parentFrame.showOverlay();
        try {
            RoomDialog dialog = new RoomDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                roomDAO,
                blocDAO, // Added
                departementDAO, // Added
                null,
                blocDAO.getAllActiveBlocs(), // Pass only active blocs
                departementDAO.getAllActiveDepartements(), // Pass only active departments
                allRooms,
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

    private void onEditRoom(ActionEvent e) { // Changed method name
        int selectedRow = roomTable.getSelectedRow(); // Changed from niveauTable
        if (selectedRow != -1) {
            Room selectedRoom = displayedRooms.get(selectedRow); // Directly get from displayedRooms

            // selectedRoom will never be null here if selectedRow is valid
            parentFrame.showOverlay();
            try {
                RoomDialog dialog = new RoomDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    roomDAO,
                    blocDAO, // Added
                    departementDAO, // Added
                    selectedRoom,
                    blocDAO.getAllActiveBlocs(), // Pass only active blocs
                    departementDAO.getAllActiveDepartements(), // Pass only active departments
                    allRooms,
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
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner une salle à modifier.", false, 3000); // Changed message type
        }
    }

    private void onDeleteRoom(ActionEvent e) {
        int[] selectedRows = roomTable.getSelectedRows();
        if (selectedRows.length > 0) {
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer cette salle ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d éléments sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean allSuccess = true;
                for (int selectedRow : selectedRows) {
                    Room selectedRoom = displayedRooms.get(selectedRow); // Directly get from displayedRooms
                    
                    // selectedRoom will never be null here if selectedRow is valid
                    boolean success = roomDAO.deleteRoom(selectedRoom.getId());
                    if (!success) {
                        allSuccess = false;
                        // Optionally, log which room failed to delete
                    }
                }

                if (allSuccess) {
                    UIUtils.showTemporaryMessage(this, String.format("%d salle(s) supprimée(s) avec succès.", selectedRows.length), true, 3000);
                } else {
                    UIUtils.showTemporaryMessage(this, "Échec de la suppression d'une ou plusieurs salles.", false, 3000);
                }
                refreshData(); // Refresh table after deletion attempts
            }
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins une salle à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        String searchFilter = txtSearch.getText().trim();
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        
        String currentSelectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        String currentSelectedBloc = (String) comboFilterBloc.getSelectedItem();

        boolean hasDepartementFilter = currentSelectedDepartement != null && !currentSelectedDepartement.equals("Tous");
        boolean hasBlocFilter = currentSelectedBloc != null && !currentSelectedBloc.equals("Tous");

        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }

        if (hasDepartementFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Faculté: ").append(currentSelectedDepartement);
        }
        if (hasBlocFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Département: ").append(currentSelectedBloc);
        }

        // Determine which columns to print based on active filters
        List<String> visibleColumnsForPrint = new ArrayList<>();
        for (String colName : originalColumnNames) {
            if (hasDepartementFilter && colName.equals("Faculté")) {
                continue; // Skip this column if filter is active
            }
            if (hasBlocFilter && colName.equals("Département")) {
                continue; // Skip this column if filter is active
            }
            visibleColumnsForPrint.add(colName);
        }

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Salles", dynamicFilterInfoBuilder.toString());
        job.setPrintable(new RoomTablePrintable(
            roomTable,
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
            roomTable,
            "Salles",
            "Salles",
            filterBuilder.toString(),
            includeColumn
        );
    }



    private void updateBlocFilterOptions(String selectedDepartementName) {
        String currentBlocSelection = (String) comboFilterBloc.getSelectedItem(); // Store current selection

        removeItemListeners(comboFilterBloc);
        comboFilterBloc.removeAllItems();
        comboFilterBloc.addItem("Tous");

        List<String> blocNames = new ArrayList<>();
        if ("Tous".equals(selectedDepartementName)) {
            allBlocs.stream().map(Bloc::getNom).sorted().forEach(blocNames::add);
        } else {
            List<Integer> blocIdsInSelectedDepartment = allRooms.stream()
                .filter(room -> selectedDepartementName.equals(room.getDepartmentName()))
                .map(Room::getIdBloc)
                .distinct()
                .collect(Collectors.toList());

            allBlocs.stream()
                .filter(bloc -> blocIdsInSelectedDepartment.contains(bloc.getId()))
                .map(Bloc::getNom)
                .sorted()
                .forEach(blocNames::add);
        }
        blocNames.forEach(comboFilterBloc::addItem);

        // Attempt to restore previous selection
        if (currentBlocSelection != null && blocNames.contains(currentBlocSelection)) {
            comboFilterBloc.setSelectedItem(currentBlocSelection);
        } else {
            comboFilterBloc.setSelectedItem("Tous"); // Default to "Tous" if previous selection is no longer valid
        }

        addItemListener(comboFilterBloc, e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedBloc = (String) e.getItem();
                if (!"Tous".equals(selectedBloc)) {
                    updateDepartementFilterOptions(selectedBloc);
                }
                applyFilters();
            }
        });
    }

    private void updateDepartementFilterOptions(String selectedBlocName) {
        String currentDepartementSelection = (String) comboFilterDepartement.getSelectedItem(); // Store current selection

        removeItemListeners(comboFilterDepartement);
        comboFilterDepartement.removeAllItems();
        comboFilterDepartement.addItem("Tous");

        List<String> departementNames = new ArrayList<>();
        if ("Tous".equals(selectedBlocName)) {
            allDepartments.stream().map(Departement::getNom).sorted().forEach(departementNames::add);
        } else {
            List<Integer> departementIdsInSelectedBloc = allRooms.stream()
                .filter(room -> selectedBlocName.equals(room.getBlockName()))
                .map(Room::getIdDepartementPrincipal)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

            allDepartments.stream()
                .filter(departement -> departementIdsInSelectedBloc.contains(departement.getId()))
                .map(Departement::getNom)
                .sorted()
                .forEach(departementNames::add);
        }
        departementNames.forEach(comboFilterDepartement::addItem);

        // Attempt to restore previous selection
        if (currentDepartementSelection != null && departementNames.contains(currentDepartementSelection)) {
            comboFilterDepartement.setSelectedItem(currentDepartementSelection);
        } else {
            comboFilterDepartement.setSelectedItem("Tous"); // Default to "Tous" if previous selection is no longer valid
        }

        addItemListener(comboFilterDepartement, e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedDepartement = (String) e.getItem();
                if (!"Tous".equals(selectedDepartement)) {
                    updateBlocFilterOptions(selectedDepartement);
                }
                applyFilters();
            }
        });
    }
    
    // Helper to remove all ItemListeners
    private void removeItemListeners(JComboBox<?> comboBox) {
        for (ItemListener listener : comboBox.getItemListeners()) {
            comboBox.removeItemListener(listener);
        }
    }

    // Helper to add an ItemListener
    private void addItemListener(JComboBox<?> comboBox, ItemListener listener) {
        comboBox.addItemListener(listener);
    }
    private void setColumnVisible(String columnName, boolean visible) {
        javax.swing.table.TableColumnModel columnModel = roomTable.getColumnModel(); // Now correctly references roomTable
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
                    // This assumes columnWidths map was populated for roomTable
                    if (columnWidths.containsKey(columnName)) {
                        column.setPreferredWidth(columnWidths.get(columnName));
                    } else {
                        // Fallback or default width if not found
                        column.setPreferredWidth(100); 
                    }
                }
                break;
            }
        }
    }
}
