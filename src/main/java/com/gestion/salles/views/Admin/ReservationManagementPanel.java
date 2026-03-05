package com.gestion.salles.views.Admin;

import java.awt.CardLayout;

import com.gestion.salles.dao.ReservationDAO;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.dao.RoomDAO;
import com.gestion.salles.dao.NiveauDAO;
import com.gestion.salles.dao.ActivityTypeDAO;
import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.User;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReservationManagementPanel extends JPanel implements RefreshablePanel {

    private static class ResultHolder {
        List<Reservation> allReservations;
        List<Departement> allDepartments;
        List<Bloc> allBlocs;
        List<User> allUsers;

        public ResultHolder(List<Reservation> allReservations, List<Departement> allDepartments, List<Bloc> allBlocs, List<User> allUsers) {
            this.allReservations = allReservations;
            this.allDepartments = allDepartments;
            this.allBlocs = allBlocs;
            this.allUsers = allUsers;
        }
    }

    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JComboBox<Departement> cmbDepartement;
    private JComboBox<Bloc> cmbBloc;
    private JComboBox<User> cmbUser;
    private JTable reservationTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private CardLayout cardLayout; // Added for CardLayout management
    private JPanel contentCardPanel; // Added for CardLayout management
    private Timer searchDebounceTimer;

    private final List<String> originalColumnNames = new ArrayList<>();
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ReservationManagementPanel.class.getName());


    private final ReservationDAO reservationDAO;

    private final DepartementDAO departementDAO;
    private final BlocDAO blocDAO;
    private final UserDAO userDAO;
    private final RoomDAO roomDAO;
    private final NiveauDAO niveauDAO;
    private final ActivityTypeDAO activityTypeDAO;

    private List<Reservation> allReservations;
    private List<Reservation> displayedReservations;
    private List<Departement> allDepartments;
    private List<Bloc> allBlocs;
    private List<User> allUsers;

    private Dashboard parentFrame; // Added field

    public ReservationManagementPanel(Dashboard parentFrame) { // Modified constructor
        this.reservationDAO = new ReservationDAO();

        this.departementDAO = new DepartementDAO();
        this.blocDAO = new BlocDAO();
        this.userDAO = new UserDAO();
        this.roomDAO = new RoomDAO();
        this.niveauDAO = new NiveauDAO();
        this.activityTypeDAO = new ActivityTypeDAO();
        this.displayedReservations = new ArrayList<>();

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

        JPanel filterPanel = new JPanel(new MigLayout("insets 0 10 10 10, fillx, gap 10", "[][fill,grow][][fill,grow][][fill,grow]"));
        filterPanel.setOpaque(false);
        cmbDepartement = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbBloc = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbUser = UIUtils.createStyledComboBox(new JComboBox<>());
        
        cmbDepartement.putClientProperty("JComboBox.arc", 10);
        cmbBloc.putClientProperty("JComboBox.arc", 10);
        cmbUser.putClientProperty("JComboBox.arc", 10);

        filterPanel.add(new JLabel("Faculté:"));
        filterPanel.add(cmbDepartement, "growx, h 35!");
        filterPanel.add(new JLabel("Département:"));
        filterPanel.add(cmbBloc, "growx, h 35!");
        filterPanel.add(new JLabel("Utilisateur:"));
        filterPanel.add(cmbUser, "growx, h 35!");
        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(searchActionBar, filterPanel);
        centerContentPanel.add(topControlsPanel, java.awt.BorderLayout.NORTH);

        tableModel = ReservationManagementUIHelper.createReservationTableModel();

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
        lblNoResults.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblNoResults.setFont(lblNoResults.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        
        // Use CardLayout for switching between table and no results message
        this.cardLayout = new CardLayout();
        this.contentCardPanel = new JPanel(this.cardLayout);
        this.contentCardPanel.add(styledTableScrollPane, "tableCard");
        this.contentCardPanel.add(lblNoResults, "noResultsCard");

        centerContentPanel.add(this.contentCardPanel, java.awt.BorderLayout.CENTER);
        add(centerContentPanel, java.awt.BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        SwingWorker<ResultHolder, Void> worker = new SwingWorker<>() {
            @Override
            protected ResultHolder doInBackground() throws Exception {
                List<Reservation> fetchedReservations = reservationDAO.getAllReservations();
                final LocalDateTime currentDateTime = LocalDateTime.now(); // Calculate once

                for (Reservation res : fetchedReservations) {
                    if (res.getDateReservation() != null && res.getHeureDebut() != null) {
                        LocalDateTime reservationDateTime = LocalDateTime.of(res.getDateReservation(), res.getHeureDebut());
                        res.setPastReservation(reservationDateTime.isBefore(currentDateTime));
                    } else {
                        res.setPastReservation(false); // Or handle as appropriate if date/time is null
                    }
                }
                
                List<Departement> fetchedDepartments = departementDAO.getAllDepartements();
                List<Bloc> fetchedBlocs = blocDAO.getAllBlocs();
                List<User> fetchedUsers = Stream.concat(
                    userDAO.getUsersByRole(User.Role.Chef_Departement).stream(),
                    userDAO.getUsersByRole(User.Role.Enseignant).stream()
                ).collect(Collectors.toList());
                return new ResultHolder(fetchedReservations, fetchedDepartments, fetchedBlocs, fetchedUsers);
            }

            @Override
            protected void done() {
                try {
                    ResultHolder result = get();
                    allReservations = result.allReservations;
                    allDepartments = result.allDepartments;
                    allBlocs = result.allBlocs;
                    allUsers = result.allUsers;

                    applyFilters(); // applyFilters will handle populating filter components and applying filters
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ReservationManagementPanel.this,
                            "Erreur lors du chargement des données.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void applyFilters() {
        // Temporarily remove listeners to prevent recursive calls during updates
        removeListeners();

        // Store current selections to attempt to restore them after repopulation
        Departement previouslySelectedDept = (Departement) cmbDepartement.getSelectedItem();
        Bloc previouslySelectedBloc = (Bloc) cmbBloc.getSelectedItem();
        User previouslySelectedUser = (User) cmbUser.getSelectedItem();

        // --- Populate cmbDepartement ---
        cmbDepartement.removeAllItems();
        cmbDepartement.addItem(new Departement("Tous", 0));
        allDepartments.stream()
                .sorted((d1, d2) -> d1.getNom().compareToIgnoreCase(d2.getNom()))
                .forEach(cmbDepartement::addItem);
        restoreSelection(cmbDepartement, previouslySelectedDept != null ? previouslySelectedDept.getId() : 0);

        // --- Populate cmbBloc based on selected Departement ---
        cmbBloc.removeAllItems();
        cmbBloc.addItem(new Bloc("Tous", 0));
        Departement currentSelectedDeptForBlocs = (Departement) cmbDepartement.getSelectedItem();
        if (currentSelectedDeptForBlocs != null && currentSelectedDeptForBlocs.getId() != 0) {
            allUsers.stream()
                .filter(u -> u.getIdDepartement() != null && u.getIdDepartement().intValue() == currentSelectedDeptForBlocs.getId())
                .filter(u -> u.getIdBloc() != null)
                .map(u -> allBlocs.stream()
                            .filter(b -> b.getId() == u.getIdBloc().intValue())
                            .findFirst()
                            .orElse(new Bloc(u.getNomBloc(), u.getIdBloc()))) // Fallback if bloc not found in allBlocs
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted((b1, b2) -> b1.getNom().compareToIgnoreCase(b2.getNom()))
                .forEach(cmbBloc::addItem);
        } else {
            allBlocs.stream()
                .sorted((b1, b2) -> b1.getNom().compareToIgnoreCase(b2.getNom()))
                .forEach(cmbBloc::addItem);
        }
        restoreSelection(cmbBloc, previouslySelectedBloc != null ? previouslySelectedBloc.getId() : 0);

        // --- Populate cmbUser based on selected Departement and Bloc ---
        cmbUser.removeAllItems();
        User allUsersOption = new User();
        allUsersOption.setIdUtilisateur(0);
        allUsersOption.setNom("Tous");
        cmbUser.addItem(allUsersOption);
        
        Departement currentSelectedDeptForUsers = (Departement) cmbDepartement.getSelectedItem();
        Bloc currentSelectedBlocForUsers = (Bloc) cmbBloc.getSelectedItem();
        Stream<User> userStream = allUsers.stream();

        if (currentSelectedDeptForUsers != null && currentSelectedDeptForUsers.getId() != 0) {
            userStream = userStream.filter(u -> u.getIdDepartement() != null && u.getIdDepartement().intValue() == currentSelectedDeptForUsers.getId());
        }
        if (currentSelectedBlocForUsers != null && currentSelectedBlocForUsers.getId() != 0) {
            userStream = userStream.filter(u -> u.getIdBloc() != null && u.getIdBloc().intValue() == currentSelectedBlocForUsers.getId());
        }
        
        List<User> filteredUsers = userStream.sorted((u1, u2) -> u1.getFullName().compareToIgnoreCase(u2.getFullName()))
                  .collect(Collectors.toList());
        
        filteredUsers.forEach(cmbUser::addItem);
        restoreSelection(cmbUser, previouslySelectedUser != null ? previouslySelectedUser.getIdUtilisateur() : 0);

        addListeners(); // Re-add listeners after all programmatic updates

        List<Reservation> filteredReservations = new ArrayList<>(allReservations);

        Departement selectedDept = (Departement) cmbDepartement.getSelectedItem();
        if (selectedDept != null && selectedDept.getId() != 0) {
            filteredReservations.removeIf(res -> res.getIdDepartement() == null || res.getIdDepartement() != selectedDept.getId());
        }

        Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
        if (selectedBloc != null && selectedBloc.getId() != 0) {
            filteredReservations.removeIf(res -> res.getIdBloc() == null || res.getIdBloc() != selectedBloc.getId());
        }

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

    private void addListeners() {
        cmbDepartement.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilters();
            }
        });
        cmbBloc.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilters();
            }
        });
        cmbUser.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyFilters();
            }
        });
    }

    private void removeListeners() {
        for (var listener : cmbDepartement.getItemListeners()) {
            cmbDepartement.removeItemListener(listener);
        }
        for (var listener : cmbBloc.getItemListeners()) {
            cmbBloc.removeItemListener(listener);
        }
        for (var listener : cmbUser.getItemListeners()) {
            cmbUser.removeItemListener(listener);
        }
    }

    private <T> void restoreSelection(JComboBox<T> comboBox, Integer idToRestore) {
        if (idToRestore != null && idToRestore != 0) {
            boolean restored = false;
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                T item = comboBox.getItemAt(i);
                if (item instanceof Departement && ((Departement) item).getId() == idToRestore) {
                    comboBox.setSelectedItem(item);
                    restored = true;
                    break;
                } else if (item instanceof Bloc && ((Bloc) item).getId() == idToRestore) {
                    comboBox.setSelectedItem(item);
                    restored = true;
                    break;
                } else if (item instanceof User && ((User) item).getIdUtilisateur() == idToRestore) {
                    comboBox.setSelectedItem(item);
                    restored = true;
                    break;
                }
            }
            if (!restored && comboBox.getItemCount() > 0) {
                comboBox.setSelectedIndex(0); // Select "Tous" or first item if restore failed
            }
        } else if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0); // Select "Tous"
        }
    }

    private void populateTable(List<Reservation> reservations) {
        tableModel.setRowCount(0);
        displayedReservations.clear();
        if (reservations == null || reservations.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
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
        ReservationDialog dialog = new ReservationDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                reservationDAO,
                departementDAO, blocDAO, niveauDAO,
                userDAO, roomDAO, activityTypeDAO,
                null,
                (success, message) -> refreshData(),
                parentFrame.getCurrentUser().getIdUtilisateur()
        );
        parentFrame.showOverlay();
        try {
            dialog.setVisible(true);
        } finally {
            parentFrame.hideOverlay();
        }
    }

    private void onEditReservation(ActionEvent e) {
        int selectedRow = reservationTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = reservationTable.convertRowIndexToModel(selectedRow);
            Reservation selectedReservation = displayedReservations.get(modelRow);
            ReservationDialog dialog = new ReservationDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    reservationDAO,
                    departementDAO, blocDAO, niveauDAO,
                    userDAO, roomDAO, activityTypeDAO,
                    selectedReservation,
                    (success, message) -> refreshData(),
                    parentFrame.getCurrentUser().getIdUtilisateur()
            );
            parentFrame.showOverlay();
            try {
                dialog.setVisible(true);
            } finally {
                parentFrame.hideOverlay();
            }
        } else {
            UIUtils.showTemporaryMessage(SwingUtilities.getWindowAncestor(this), "Veuillez sélectionner une réservation à modifier.", false, 3000);
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
                        // Optionally, log which reservation failed to delete
                    }
                }

                if (allSuccess) {
                    UIUtils.showTemporaryMessage(SwingUtilities.getWindowAncestor(this), String.format("%d réservation(s) supprimée(s) avec succès.", selectedRows.length), true, 3000);
                } else {
                    UIUtils.showTemporaryMessage(SwingUtilities.getWindowAncestor(this), "Échec de la suppression d'une ou plusieurs réservations.", false, 3000);
                }
                refreshData(); // Refresh table after deletion attempts
            }
        } else {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "Veuillez sélectionner au moins une réservation à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        String searchFilter = txtSearch.getText().trim();
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        
        Departement currentSelectedDepartement = (Departement) cmbDepartement.getSelectedItem();
        Bloc currentSelectedBloc = (Bloc) cmbBloc.getSelectedItem();
        User currentSelectedUser = (User) cmbUser.getSelectedItem();

        boolean hasDepartementFilter = currentSelectedDepartement != null && currentSelectedDepartement.getId() != 0;
        boolean hasBlocFilter = currentSelectedBloc != null && currentSelectedBloc.getId() != 0;
        boolean hasUserFilter = currentSelectedUser != null && currentSelectedUser.getIdUtilisateur() != 0;

        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }

        if (hasDepartementFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Faculté: ").append(currentSelectedDepartement.getNom());
        }
        if (hasBlocFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Département: ").append(currentSelectedBloc.getNom());
        }
        if (hasUserFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Utilisateur: ").append(currentSelectedUser.getFullName());
        }

        // For reservations, all columns are always conceptually visible, as filtering is on rows.
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
                LOGGER.log(java.util.logging.Level.WARNING, "Cannot print", ex);
                JOptionPane.showMessageDialog(this, "Échec de l'impression. Veuillez vérifier la connexion de votre imprimante ou les paramètres.", "Erreur d'Impression", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSaveExcel(ActionEvent e) {
        Departement selectedDepartement = (Departement) cmbDepartement.getSelectedItem();
        Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
        User selectedUser = (User) cmbUser.getSelectedItem();

        boolean hasDepartementFilter = selectedDepartement != null && selectedDepartement.getId() != 0;
        boolean hasBlocFilter = selectedBloc != null && selectedBloc.getId() != 0;
        boolean hasUserFilter = selectedUser != null && selectedUser.getIdUtilisateur() != 0;

        StringBuilder filterBuilder = new StringBuilder();
        String searchFilter = txtSearch.getText().trim();
        if (!searchFilter.isEmpty()) {
            filterBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasDepartementFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Faculté: ").append(selectedDepartement.getNom());
        }
        if (hasBlocFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Département: ").append(selectedBloc.getNom());
        }
        if (hasUserFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Utilisateur: ").append(selectedUser.getFullName());
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
