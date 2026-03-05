package com.gestion.salles.views.ChefDepartement;

import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.dao.RoomDAO;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Room;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Admin.RoomDialog; // Use Admin.RoomDialog
import com.gestion.salles.views.Admin.RoomTablePrintable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterJob;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;

/**
 * Panel for Room Management in the Chef de Département Dashboard.
 * Displays and manages rooms belonging to the HoD's specific bloc only.
 * No department/bloc filters — the HoD's scope is fixed.
 *
 * @version 1.2 - Restructured to match Admin panel layout; top toolbar fixed;
 *                unused fields removed; CardLayout moved to field declarations.
 */
public class RoomManagementPanel extends JPanel implements RefreshablePanel {

    private static final Logger LOGGER = Logger.getLogger(RoomManagementPanel.class.getName());

    // ── UI Components ────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private java.awt.CardLayout cardLayout;
    private JPanel contentCardPanel;


    // ── Data ─────────────────────────────────────────────────────────────────
    private final RoomDAO roomDAO;
    private final DepartementDAO departementDAO;
    private final BlocDAO blocDAO;
    private List<Room> allRooms;
    private List<Room> displayedRooms;

    private final List<String> originalColumnNames = new ArrayList<>();
    private final Gson gson = new Gson();
    private final DashboardChef parentDashboard;
    private final User currentUser;


    public RoomManagementPanel(User currentUser, DashboardChef parentDashboard) {
        this.currentUser = currentUser;
        this.parentDashboard = parentDashboard;
        this.roomDAO = new RoomDAO();
        this.departementDAO = new DepartementDAO();
        this.blocDAO = new BlocDAO();
        this.allRooms = new ArrayList<>();
        this.displayedRooms = new ArrayList<>();
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
        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddRoom);

        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditRoom);
        btnEdit.setEnabled(false);

        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteRoom);
        btnDelete.setEnabled(false);

        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);

        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel topToolbar = UIUtils.createSearchActionBar(
                txtSearch, btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel);
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, null);
        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);

        String[] columnNames = {"Nom", "Type", "Capacité", "Équipements"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        roomTable = (JTable) styledTableScrollPane.getViewport().getView();

        roomTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Nom
        roomTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Type
        roomTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Capacité
        roomTable.getColumnModel().getColumn(3).setPreferredWidth(250); // Équipements

        for (int i = 0; i < roomTable.getColumnCount(); i++) {
            String colName = roomTable.getColumnModel().getColumn(i).getHeaderValue().toString();
            originalColumnNames.add(colName);
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
    // ── Data Loading ──────────────────────────────────────────────────────────

    @Override
    public void refreshData() {
        SwingWorker<List<Room>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Room> doInBackground() throws Exception {
                Integer blocId = currentUser.getIdBloc();
                if (blocId == null) return new ArrayList<>();
                return roomDAO.getRoomsByBloc(blocId);
            }

            @Override
            protected void done() {
                try {
                    allRooms = get();
                    populateTable(allRooms);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading rooms", e);
                    JOptionPane.showMessageDialog(RoomManagementPanel.this,
                            "Erreur lors du chargement des donnees des salles.",
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Room> rooms) {
        tableModel.setRowCount(0);
        displayedRooms.clear();

        if (rooms == null || rooms.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
            return;
        }

        cardLayout.show(contentCardPanel, "tableCard");
        displayedRooms.addAll(rooms);
        for (Room room : displayedRooms) {
            tableModel.addRow(new Object[]{
                    room.getName(),
                    room.getTypeSalle(),
                    room.getCapacity(),
                    parseEquipment(room.getEquipment())
            });
        }
    }

    /** Parses the JSON equipment string into a comma-separated display string. */
    private String parseEquipment(String equipmentJson) {
        if (equipmentJson == null || equipmentJson.isEmpty()) return "";
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(equipmentJson, listType);
            return list != null ? String.join(", ", list) : "";
        } catch (com.google.gson.JsonSyntaxException e) {
            return equipmentJson;
        }
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    private void applyFilters() {
        String searchText = txtSearch.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            populateTable(allRooms);
            return;
        }
        String q = searchText.trim().toLowerCase();
        List<Room> filtered = allRooms.stream()
                .filter(r -> r.getName().toLowerCase().contains(q)
                        || r.getTypeSalle().toLowerCase().contains(q)
                        || String.valueOf(r.getCapacity()).contains(q)
                        || (r.getEquipment() != null && r.getEquipment().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        populateTable(filtered);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onAddRoom(ActionEvent e) {
        parentDashboard.showOverlay();
        List<Bloc> scopedBlocs = blocDAO.getAllActiveBlocs().stream()
                .filter(bloc -> bloc.getId() == currentUser.getIdBloc())
                .collect(Collectors.toList());
        List<com.gestion.salles.models.Departement> scopedDepartments = departementDAO.getAllActiveDepartements().stream()
                .filter(dept -> dept.getId() == currentUser.getIdDepartement())
                .collect(Collectors.toList());
        RoomDialog dialog = new RoomDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                roomDAO, blocDAO, departementDAO,
                null, scopedBlocs, scopedDepartments, allRooms,
                (success, message) -> {
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000);
                },
                true
        );
        try {
            dialog.setVisible(true);
        } finally {
            parentDashboard.hideOverlay();
        }
    }

    private void onEditRoom(ActionEvent e) {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            UIUtils.showTemporaryMessage(this, "Veuillez selectionner une salle a modifier.", false, 3000);
            return;
        }
        if (selectedRow < 0 || selectedRow >= displayedRooms.size()) return;
        Room selectedRoom = displayedRooms.get(selectedRow);

        parentDashboard.showOverlay();
        List<Bloc> scopedBlocs = blocDAO.getAllActiveBlocs().stream()
                .filter(bloc -> bloc.getId() == currentUser.getIdBloc())
                .collect(Collectors.toList());
        List<com.gestion.salles.models.Departement> scopedDepartments = departementDAO.getAllActiveDepartements().stream()
                .filter(dept -> dept.getId() == currentUser.getIdDepartement())
                .collect(Collectors.toList());
        RoomDialog dialog = new RoomDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                roomDAO, blocDAO, departementDAO,
                selectedRoom, scopedBlocs, scopedDepartments, allRooms,
                (success, message) -> {
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000);
                },
                true
        );
        try {
            dialog.setVisible(true);
        } finally {
            parentDashboard.hideOverlay();
        }
    }

    private void onDeleteRoom(ActionEvent e) {
        int[] selectedRows = roomTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Veuillez selectionner au moins une salle a supprimer.",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String message = selectedRows.length == 1
                ? "Voulez-vous vraiment supprimer cette salle ?\nCette action est irreversible."
                : String.format("Voulez-vous vraiment supprimer les %d salles selectionnees ?\nCette action est irreversible.", selectedRows.length);

        if (JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;

        List<Room> roomsToDelete = new ArrayList<>();
        for (int row : selectedRows) {
            if (row >= 0 && row < displayedRooms.size()) {
                roomsToDelete.add(displayedRooms.get(row));
            }
        }

        boolean allSuccess = true;
        for (Room room : roomsToDelete) {
            if (!roomDAO.deleteRoom(room.getId())) {
                allSuccess = false;
            }
        }

        UIUtils.showTemporaryMessage(this,
                allSuccess
                        ? String.format("%d salle(s) supprimee(s) avec succes.", selectedRows.length)
                        : "Echec de la suppression d'une ou plusieurs salles.",
                allSuccess, 3000);
        refreshData();
    }

    // ── Print & Export ────────────────────────────────────────────────────────

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();

        String subtitle = "Faculté: " + currentUser.getNomDepartement()
                          + " - Département: " + currentUser.getNomBloc();
        String searchText = txtSearch.getText().trim();
        String filterInfo = searchText.isEmpty() ? subtitle : subtitle + " - Filtre: \"" + searchText + "\"";

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Salles", filterInfo);
        job.setPrintable(new RoomTablePrintable(
            roomTable,
            ManagementExportUtils.UNIVERSITY_TITLE,
            dynamicInfo,
            new ArrayList<>(originalColumnNames)
        ));

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Document envoye a l'imprimante avec succes.",
                        "Impression Reussie", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.awt.print.PrinterException ex) {
                LOGGER.log(Level.WARNING, "Print failed", ex);
                JOptionPane.showMessageDialog(this,
                        "Echec de l'impression. Veuillez verifier votre imprimante.",
                        "Erreur d'Impression", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSaveExcel(ActionEvent e) {
        String subtitle = "Faculté: " + currentUser.getNomDepartement()
            + " - Département: " + currentUser.getNomBloc();
        String searchText = txtSearch.getText().trim();
        String filterInfo = searchText.isEmpty() ? subtitle : subtitle + " - Filtre: \"" + searchText + "\"";

        Predicate<String> includeColumn = colName -> true;

        ManagementExportUtils.exportTableToExcel(
            this,
            roomTable,
            "Salles_" + currentUser.getNomBloc(),
            "Salles",
            filterInfo,
            includeColumn
        );
    }

    // Excel export handled by ManagementExportUtils
}
