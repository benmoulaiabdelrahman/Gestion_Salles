package com.gestion.salles.views.ChefDepartement;

import java.awt.CardLayout;

import com.gestion.salles.dao.ReservationDAO;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.User;
import com.gestion.salles.models.Room;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;
import com.gestion.salles.views.shared.reservations.ReservationManagementUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.print.PrinterJob;
import com.gestion.salles.views.Admin.ReservationTablePrintable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Arrays; // Added import

/**
 * Panel for Reservation Management in the Chef de Département Dashboard.
 * Displays and manages reservations belonging to the HoD's specific department.
 *
 * @author Gemini
 * @version 1.0 - Adapted from Admin ReservationManagementPanel.
 */
public class ReservationManagementPanel extends JPanel implements RefreshablePanel {

    private static final java.awt.Color PRIMARY_TEXT = ThemeConstants.PRIMARY_TEXT;

    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JComboBox<User> cmbUser;
    private JTable reservationTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private Timer searchDebounceTimer;

    private final List<String> originalColumnNames = new ArrayList<>();

    private final ReservationDAO reservationDAO;
    private final UserDAO userDAO;
    private final com.gestion.salles.dao.RoomDAO roomDAO;

    private List<Reservation> allReservations;
    private List<Reservation> displayedReservations;
    private List<User> allUsers; // Contains both HoDs and Teachers
    private List<Room> allRoomsInDepartment; // New: To pass to ReservationDialog

    private DashboardChef parentDashboard; // Changed type to DashboardChef
    private final User currentUser; // Added current user

    public ReservationManagementPanel(User currentUser, DashboardChef parentDashboard) { // Modified constructor
        this.currentUser = currentUser;
        this.reservationDAO = new ReservationDAO();
        this.userDAO = new UserDAO();
        this.roomDAO = new com.gestion.salles.dao.RoomDAO();
        this.displayedReservations = new ArrayList<>();

        this.parentDashboard = parentDashboard; 
        initComponents();
        refreshData();
    }

    private CardLayout cardLayout;
    private JPanel tableCardPanel;

    private void initComponents() {
        setLayout(new java.awt.BorderLayout(10, 10));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");

        JPanel centerContentPanel = new JPanel(new java.awt.BorderLayout(10, 10));
        centerContentPanel.setOpaque(false);

        txtSearch = UIUtils.createStyledTextField("Rechercher une reservation...");
        searchDebounceTimer = ReservationManagementUIHelper.installSearchDebounce(txtSearch, 300, this::applyFilters);

        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddReservation);

        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditReservation);
        btnEdit.setEnabled(false);

        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteReservation);
        btnDelete.setEnabled(false);

        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);

        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel searchActionBar = UIUtils.createSearchActionBar(txtSearch, btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel);

        JPanel filterPanel = new JPanel(new MigLayout("insets 0 10 10 10, fillx, gap 10", "[][fill,grow]"));
        filterPanel.setOpaque(false);

        cmbUser = UIUtils.createStyledComboBox(new JComboBox<>());

        cmbUser.putClientProperty("JComboBox.arc", 10);

        filterPanel.add(new JLabel("Utilisateur:"));
        filterPanel.add(cmbUser, "growx, h 35!");
        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(searchActionBar, filterPanel);
        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);

        tableModel = ReservationManagementUIHelper.createReservationTableModel();

        // Use UIUtils.createStyledTable for the base table
        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        reservationTable = (JTable) styledTableScrollPane.getViewport().getView();
        ReservationManagementUIHelper.configureReservationTable(
            reservationTable,
            originalColumnNames,
            modelRow -> (modelRow >= 0 && modelRow < displayedReservations.size())
                ? displayedReservations.get(modelRow)
                : null
        );

        reservationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = reservationTable.getSelectedRowCount();
                btnEdit.setEnabled(selectedRowCount == 1);
                btnDelete.setEnabled(selectedRowCount >= 1);
            }
        });

        reservationTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = reservationTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < reservationTable.getRowCount()) {
                        if (!reservationTable.isRowSelected(r)) {
                            reservationTable.addRowSelectionInterval(r, r);
                        }
                    }
                }
            }
        });


        lblNoResults = new JLabel("Aucun résultat trouvé pour votre recherche.", SwingConstants.CENTER);
        lblNoResults.setForeground(PRIMARY_TEXT);
        lblNoResults.setFont(lblNoResults.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        cardLayout = new CardLayout();
        tableCardPanel = new JPanel(cardLayout);
        tableCardPanel.add(styledTableScrollPane, "table");
        tableCardPanel.add(lblNoResults, "noResults");

        centerContentPanel.add(tableCardPanel, java.awt.BorderLayout.CENTER);
        add(centerContentPanel, java.awt.BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Reservation> fetchedReservations;
            List<User> departmentUsers;
            List<Room> fetchedRoomsInDepartment; // New: to store rooms in department

            @Override
            protected Void doInBackground() throws Exception {
                Integer departmentId = currentUser.getIdDepartement();
                Integer blocId = currentUser.getIdBloc();

                if (departmentId != null && blocId != null) {
                    fetchedReservations = reservationDAO.getReservationsByBloc(blocId);
                    LocalDateTime currentDateTime = LocalDateTime.now();
                    for (Reservation res : fetchedReservations) {
                        if (res.getDateReservation() != null && res.getHeureDebut() != null) {
                            LocalDateTime reservationDateTime = LocalDateTime.of(res.getDateReservation(), res.getHeureDebut());
                            res.setPastReservation(reservationDateTime.isBefore(currentDateTime));
                        } else {
                            res.setPastReservation(false);
                        }
                    }
                    
                    // Get users (HoDs and Teachers) associated with this bloc
                    List<User.Role> rolesToFetch = Arrays.asList(User.Role.Chef_Departement, User.Role.Enseignant);
                    departmentUsers = userDAO.getUsersByBlocAndRoles(blocId, rolesToFetch);

                    // Fetch rooms specific to this bloc
                    fetchedRoomsInDepartment = roomDAO.getRoomsByBloc(blocId);

                } else {
                    fetchedReservations = new ArrayList<>();
                    departmentUsers = new ArrayList<>();
                    fetchedRoomsInDepartment = new ArrayList<>();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    allReservations = fetchedReservations;
                    allUsers = departmentUsers;
                    allRoomsInDepartment = fetchedRoomsInDepartment; // Assign fetched rooms
                    


                    populateFilters(); // Call the new method to populate all filters
                    applyFilters(); // Apply filters after populating them
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ReservationManagementPanel.this,
                            "Erreur lors du chargement des données.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    // START OF REFACTORED/ADDED METHODS

    private void populateFilters() {
        // Populate cmbUser filter (only teachers from the HoD's department)
        removeItemListeners(cmbUser);
        cmbUser.removeAllItems();
        User allUsersOption = new User();
        allUsersOption.setIdUtilisateur(0);
        allUsersOption.setNom("Tous");
        cmbUser.addItem(allUsersOption);
        allUsers.stream()
            .filter(u -> u.getRole() == User.Role.Enseignant)
            .sorted((u1, u2) -> u1.getFullName().compareToIgnoreCase(u2.getFullName()))
            .forEach(cmbUser::addItem);
        addItemListener(cmbUser, e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilters();
            }
        });
    }

    private void removeItemListeners(JComboBox<?> comboBox) {
        for (java.awt.event.ItemListener listener : comboBox.getItemListeners()) {
            comboBox.removeItemListener(listener);
        }
    }

    private void addItemListener(JComboBox<?> comboBox, java.awt.event.ItemListener listener) {
        comboBox.addItemListener(listener);
    }
    
    private void applyFilters() {
        List<Reservation> filteredReservations = new ArrayList<>(allReservations);

        // User Filter
        User selectedUser = (User) cmbUser.getSelectedItem();
        if (selectedUser != null && selectedUser.getIdUtilisateur() != 0) {
            filteredReservations.removeIf(res -> res.getIdEnseignant() != selectedUser.getIdUtilisateur());
        }

        String searchText = txtSearch.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredReservations = filteredReservations.stream()
                    .filter(res -> res.getNomSalle().toLowerCase().contains(lowerCaseSearchText) ||
                            res.getTitreActivite().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        populateTable(filteredReservations);
    }

    private void populateTable(List<Reservation> reservations) {
        tableModel.setRowCount(0);
        displayedReservations.clear();
        if (reservations == null || reservations.isEmpty()) {
            cardLayout.show(tableCardPanel, "noResults");
        } else {
            cardLayout.show(tableCardPanel, "table");
            displayedReservations.addAll(reservations);
            for (Reservation res : displayedReservations) {
                String dateDisplay;
                if (res.isRecurring()) {
                    dateDisplay = "Récurrente"; // Simplified display
                } else {
                    dateDisplay = res.getDateReservation().toString();
                }

                tableModel.addRow(new Object[]{
                        res.getNomSalle(),
                        res.getTitreActivite(),
                        dateDisplay, // Updated to use dateDisplay
                        res.getHeureDebut(),
                        res.getHeureFin(),
                        res.getStatut().getDisplayName()
                });
            }
        }
    }

    private void onAddReservation(ActionEvent e) {
        parentDashboard.showOverlay();
        try {
            ReservationDialog dialog = new ReservationDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    reservationDAO,
                    null,
                    allRoomsInDepartment, // Pass department-filtered rooms
                    allUsers.stream().filter(u -> u.getRole() == User.Role.Enseignant).collect(Collectors.toList()), // Pass only teachers
                    (success, message) -> refreshData(),
                    currentUser // Pass currentHoD
            );
            dialog.setVisible(true);
        } finally {
            parentDashboard.hideOverlay();
        }
    }

    private void onEditReservation(ActionEvent e) {
        int selectedRow = reservationTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = reservationTable.convertRowIndexToModel(selectedRow);
            Reservation selectedReservation = displayedReservations.get(modelRow);
            parentDashboard.showOverlay();
            try {
                ReservationDialog dialog = new ReservationDialog(
                        (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                        reservationDAO,
                        selectedReservation,
                        allRoomsInDepartment, // Pass department-filtered rooms
                        allUsers.stream().filter(u -> u.getRole() == User.Role.Enseignant).collect(Collectors.toList()), // Pass only teachers
                        (success, message) -> refreshData(),
                        currentUser // Pass currentHoD
                );
                dialog.setVisible(true);
            } finally {
                parentDashboard.hideOverlay();
            }
        } else {
            UIUtils.showTemporaryMessage(parentDashboard, "Veuillez sélectionner une réservation à modifier.", false, 3000);
        }
    }

    private void onDeleteReservation(ActionEvent e) {
        int[] selectedRows = reservationTable.getSelectedRows();
        if (selectedRows.length > 0) {
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer cette réservation ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d éléments sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean allSuccess = true;
                for (int selectedRow : selectedRows) {
                    int modelRow = reservationTable.convertRowIndexToModel(selectedRow);
                    Reservation selectedReservation = displayedReservations.get(modelRow);
                    boolean success = reservationDAO.deleteReservation(selectedReservation.getIdReservation());
                    if (!success) {
                        allSuccess = false;
                    }
                }

                if (allSuccess) {
                    UIUtils.showTemporaryMessage(parentDashboard, String.format("%d réservation(s) supprimée(s) avec succès.", selectedRows.length), true, 3000);
                } else {
                    UIUtils.showTemporaryMessage(parentDashboard, "Échec de la suppression d'une ou plusieurs réservations.", false, 3000);
                }
                refreshData(); 
            }
        } else {
            JOptionPane.showMessageDialog(parentDashboard, "Veuillez sélectionner au moins une réservation à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        String searchFilter = txtSearch.getText().trim();
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        
        User currentSelectedUser = (User) cmbUser.getSelectedItem();
        boolean hasUserFilter = currentSelectedUser != null && currentSelectedUser.getIdUtilisateur() != 0;

        dynamicFilterInfoBuilder.append(ReservationManagementUIHelper.buildScopeSubtitle(currentUser));

        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append(" - Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasUserFilter) {
            dynamicFilterInfoBuilder.append(" - Utilisateur: ").append(currentSelectedUser.getFullName());
        }

        List<String> visibleColumnsForPrint = new ArrayList<>(originalColumnNames);

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Réservations", dynamicFilterInfoBuilder.toString());
        job.setPrintable(new ReservationTablePrintable(
                reservationTable,
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
        StringBuilder filterBuilder = new StringBuilder();

        User currentSelectedUser = (User) cmbUser.getSelectedItem();
        boolean hasUserFilter = currentSelectedUser != null && currentSelectedUser.getIdUtilisateur() != 0;

        filterBuilder.append(ReservationManagementUIHelper.buildScopeSubtitle(currentUser));

        if (!searchFilter.isEmpty()) {
            filterBuilder.append(" - Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasUserFilter) {
            filterBuilder.append(" - Utilisateur: ").append(currentSelectedUser.getFullName());
        }

        ManagementExportUtils.exportTableToExcel(
                this,
                reservationTable,
                "Réservations",
                "Réservations",
                filterBuilder.toString(),
                null
        );
    }
    
}
