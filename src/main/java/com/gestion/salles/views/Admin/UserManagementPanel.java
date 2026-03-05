package com.gestion.salles.views.Admin;

import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger; // Added import for Logger

import com.gestion.salles.dao.DepartementDAO; // Import for DepartementDAO
import com.gestion.salles.dao.BlocDAO; // Import for BlocDAO
import com.gestion.salles.models.Departement; // Import for Departement model
import com.gestion.salles.models.Bloc; // Import for Bloc model
import com.gestion.salles.utils.DialogCallback; // Import for DialogCallback
import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;
import com.gestion.salles.views.shared.users.UserManagementUIHelper;

public class UserManagementPanel extends JPanel implements RefreshablePanel { // Assuming it implements RefreshablePanel

    private final UserDAO userDAO;
    private final DepartementDAO departementDAO; // New
    private final BlocDAO blocDAO; // New

    private List<Departement> allDepartments; // New
    private List<Bloc> allBlocs; // New

    private JTable userTable;
    private DefaultTableModel tableModel;
    private final List<User> displayedUsers = new ArrayList<>();
    private JTextField searchField;
    private JButton addNewButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JComboBox<String> comboFilterRole;
    private JComboBox<String> comboFilterFaculte; // This will now represent Departments (Faculté in UI)
    private JComboBox<String> comboFilterDepartement; // This will now represent Blocs (Département in UI)

    private final List<String> originalColumnNames = new ArrayList<>();

    private JLabel lblNoResults;
    private java.awt.CardLayout cardLayout;
    private JPanel contentCardPanel;
    private Timer searchDebounceTimer; // Debounce timer for search
    private Dashboard parentFrame;

    private ItemListener faculteItemListener;
    private ItemListener departementItemListener;

    private static final Logger LOGGER = Logger.getLogger(UserManagementPanel.class.getName());

    public UserManagementPanel(Dashboard parentFrame) { // Constructor to match Dashboard's call
        this.parentFrame = parentFrame;
        this.userDAO = new UserDAO();
        this.departementDAO = new DepartementDAO(); // New
        this.blocDAO = new BlocDAO(); // New
        this.allDepartments = new ArrayList<>(); // Initialize
        this.allBlocs = new ArrayList<>(); // Initialize
        initComponents();
        refreshData();
    }

    private static class UserResultHolder {
        List<User> users;
        List<Departement> departments;
        List<Bloc> blocs;
    }

    @Override
    public void refreshData() {
        SwingWorker<UserResultHolder, Void> worker = new SwingWorker<>() {
            @Override
            protected UserResultHolder doInBackground() throws Exception {
                UserResultHolder data = new UserResultHolder();
                data.users = userDAO.getAllUsers();
                data.departments = departementDAO.getAllDepartements();
                data.blocs = blocDAO.getAllBlocs();
                return data;
            }

            @Override
            protected void done() {
                try {
                    UserResultHolder data = get(); // Ensure background task completed
                    // Update member variables
                    allDepartments = data.departments;
                    allBlocs = data.blocs;

                    // Update cachedUsers BEFORE calling initializeFilterComboBoxes(),
                    // which triggers applyFilters(). applyFilters() reads cachedUsers directly,
                    // so it must be populated with the full user list first.
                    cachedUsers = new ArrayList<>(data.users);

                    // Initialize combos (detaches listeners, populates, re-attaches, calls applyFilters)
                    initializeFilterComboBoxes();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Erreur lors du chargement des données.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void initializeFilterComboBoxes() {
        // Detach all listeners BEFORE touching combo contents.
        // Without this, every removeAllItems()/addItem() fires ActionListener/ItemListener
        // -> applyFilters() -> loadUsers() which overwrites cachedUsers with a filtered
        // (potentially empty) subset, corrupting the cache for all subsequent filter calls.
        ActionListener[] roleActionListeners = comboFilterRole.getActionListeners();
        for (ActionListener al : roleActionListeners) comboFilterRole.removeActionListener(al);
        comboFilterFaculte.removeItemListener(this.faculteItemListener);
        comboFilterDepartement.removeItemListener(this.departementItemListener);

        // Populate Role Filter
        comboFilterRole.removeAllItems();
        comboFilterRole.addItem("Tous");
        for (User.Role role : User.Role.values()) {
            comboFilterRole.addItem(role.getRoleName());
        }

        // Populate Faculte Filter (which is now Departement)
        comboFilterFaculte.removeAllItems();
        comboFilterFaculte.addItem("Tous");
        allDepartments.stream()
                .map(Departement::getNom)
                .sorted()
                .forEach(comboFilterFaculte::addItem);

        // Populate Departement Filter (which is now Bloc)
        comboFilterDepartement.removeAllItems();
        comboFilterDepartement.addItem("Tous");
        allBlocs.stream()
                .map(Bloc::getNom)
                .sorted()
                .forEach(comboFilterDepartement::addItem);

        // Re-attach listeners now that combo contents are stable
        for (ActionListener al : roleActionListeners) comboFilterRole.addActionListener(al);
        comboFilterFaculte.addItemListener(this.faculteItemListener);
        comboFilterDepartement.addItemListener(this.departementItemListener);

        // Now safe to trigger one clean filter pass with "Tous" selected everywhere
        updateDependentFilterComboBoxes();
        applyFilters();
    }

    /**
     * Updates the dependent filter combo boxes (Faculté and Département) based on each other's
     * current selection. The full repopulation logic lives inside applyFilters(), so this method
     * simply triggers it without re-running the table filter — it's called at init time and
     * whenever a combo selection changes before applyFilters() runs.
     *
     * Since applyFilters() already handles the repopulation of both combos AND the table data,
     * we keep this as a lightweight trigger that applyFilters() can rely on being called before it.
     * In practice, the listeners call updateDependentFilterComboBoxes() then applyFilters(), and
     * applyFilters() does the real work, so this is effectively a no-op placeholder.
     */
    private void updateDependentFilterComboBoxes() {
        // The dependent combo repopulation is handled inside applyFilters().
        // This method exists as a hook called at initialisation and from item listeners.
    }
    






    
    // Helper to set column visibility, replicated from RoomManagementPanel
    private void setColumnVisible(String columnName, boolean visible) {
        javax.swing.table.TableColumnModel columnModel = userTable.getColumnModel();
        // Check if the column exists in the model
        int columnIndex = -1;
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            if (columnModel.getColumn(i).getHeaderValue().equals(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex != -1) {
            javax.swing.table.TableColumn column = columnModel.getColumn(columnIndex);
            if (!visible) {
                column.setMinWidth(0);
                column.setMaxWidth(0);
                column.setPreferredWidth(0);
                column.setResizable(false); // Make it non-resizable when hidden
            } else {
                column.setMinWidth(15); // A reasonable minimum width
                column.setMaxWidth(Integer.MAX_VALUE);
                column.setPreferredWidth(100); // Default width since columnWidths map is removed
                column.setResizable(true); // Make it resizable again when visible
            }
        }
    }


    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");

        // --- Initialize UI Components ---
        searchField = UIUtils.createStyledTextField("Rechercher un utilisateur par nom, email...");
        
        searchDebounceTimer = UserManagementUIHelper.installSearchDebounce(searchField, 300, this::applyFilters);

        addNewButton = UIUtils.createPrimaryButton("Ajouter");
        addNewButton.addActionListener(this::addNewUser);

        editButton = UIUtils.createSecondaryButton("Modifier");
        editButton.addActionListener(this::editUser);
        editButton.setEnabled(false); // Initially disabled

        deleteButton = UIUtils.createDangerButton("Supprimer");
        deleteButton.addActionListener(this::deleteUser);
        deleteButton.setEnabled(false); // Initially disabled
        
        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);

        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel topToolbar = UIUtils.createSearchActionBar(
            searchField,
            addNewButton, editButton, deleteButton, btnPrint, btnSaveExcel
        );

        // Filter Panel (by role, faculté, and département)
        JPanel filterPanel = new JPanel(new MigLayout("insets 0 10 10 10, fillx, gap 10", "[][fill,grow][][fill,grow][][fill,grow]"));
        filterPanel.setOpaque(false);

        comboFilterRole = UIUtils.createStyledComboBox(new JComboBox<>());
        comboFilterFaculte = UIUtils.createStyledComboBox(new JComboBox<>());
        comboFilterDepartement = UIUtils.createStyledComboBox(new JComboBox<>());
        
        comboFilterRole.putClientProperty("JComboBox.arc", 10);
        comboFilterFaculte.putClientProperty("JComboBox.arc", 10);
        comboFilterDepartement.putClientProperty("JComboBox.arc", 10);

        filterPanel.add(new JLabel("Rôle:"));
        filterPanel.add(comboFilterRole, "growx, h 35!");
        filterPanel.add(new JLabel("Faculté:")); // Label for Departments
        filterPanel.add(comboFilterFaculte, "growx, h 35!");
        filterPanel.add(new JLabel("Département:")); // Label for Blocs
        filterPanel.add(comboFilterDepartement, "growx, h 35!");
        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, filterPanel);

        comboFilterRole.addActionListener(e -> applyFilters()); // Role filter is independent
        
        this.faculteItemListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDependentFilterComboBoxes();
                applyFilters();
            }
        };
        comboFilterFaculte.addItemListener(this.faculteItemListener);
        
        this.departementItemListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDependentFilterComboBoxes();
                applyFilters();
            }
        };
        comboFilterDepartement.addItemListener(this.departementItemListener);
        
        // --- Center Content Panel ---
        JPanel centerContentPanel = new JPanel(new BorderLayout(10, 10));
        centerContentPanel.setOpaque(false);
        centerContentPanel.add(topControlsPanel, BorderLayout.NORTH);

        // Main Content - User Table
        tableModel = UserManagementUIHelper.createUserTableModel();
        
        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        userTable = (JTable) styledTableScrollPane.getViewport().getView(); // Extract the JTable instance
        UserManagementUIHelper.configureUserTable(userTable, originalColumnNames);


        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = userTable.getSelectedRowCount();
                editButton.setEnabled(selectedRowCount == 1);
                deleteButton.setEnabled(selectedRowCount >= 1);
            }
        });
        
        lblNoResults = new JLabel("Aucun résultat trouvé pour votre recherche.", SwingConstants.CENTER);
        lblNoResults.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblNoResults.setFont(lblNoResults.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        this.cardLayout = new java.awt.CardLayout();
        this.contentCardPanel = new JPanel(this.cardLayout);
        contentCardPanel.add(styledTableScrollPane, "tableCard");
        contentCardPanel.add(lblNoResults, "noResultsCard");
        
        centerContentPanel.add(contentCardPanel, BorderLayout.CENTER); // Add styledTableScrollPane to centerContentPanel's CENTER
        add(centerContentPanel, BorderLayout.CENTER); // Add centerContentPanel to main panel's CENTER
    } // Close initComponents here

    private void loadUsers(List<User> users) { // Changed signature
        LOGGER.info("loadUsers: incoming users size=" + (users != null ? users.size() : "null"));
        // NOTE: cachedUsers is NOT updated here. It is only set in refreshData() when fresh
        // data arrives from the DB. applyFilters() must never overwrite cachedUsers, because
        // loadUsers() is called from applyFilters() with a *filtered* subset -- overwriting
        // cachedUsers here would permanently shrink the cache on every filter operation.
        tableModel.setRowCount(0); // Clear existing data
        displayedUsers.clear();
        
        if (users == null || users.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
            LOGGER.info("loadUsers: Showing noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
            LOGGER.info("loadUsers: Showing tableCard with " + users.size() + " users");
            displayedUsers.addAll(users);
            for (User user : users) {
                tableModel.addRow(new Object[]{
                        user.getPhotoProfil(), // Image
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail(),
                        user.getTelephone(),
                        user.getRole() != null ? user.getRole().getRoleName() : "",
                        (user.getNomDepartement() != null && !user.getNomDepartement().isEmpty() ? user.getNomDepartement() : "-"),
                        (user.getNomBloc() != null && !user.getNomBloc().isEmpty() ? user.getNomBloc() : "-")
                });
            }
        }
    }

    private List<User> cachedUsers = new ArrayList<>(); // Cache users to avoid DB calls
    private SwingWorker<Void, Void> currentFilterWorker = null; // Track current filter operation
    
    private void applyFilters() {
        // Store current selections before potentially repopulating
        final String currentSelectedRole = (String) comboFilterRole.getSelectedItem();
        final String currentSelectedFaculte = (String) comboFilterFaculte.getSelectedItem();
        final String currentSelectedDepartement = (String) comboFilterDepartement.getSelectedItem();

        LOGGER.info("applyFilters: currentSelectedRole='" + currentSelectedRole +
                    "', currentSelectedFaculte='" + currentSelectedFaculte +
                    "', currentSelectedDepartement='" + currentSelectedDepartement + "'");

        // --- Temporarily remove item listeners ---
        // This is crucial to prevent recursive calls when programmatically setting selected items
        comboFilterFaculte.removeItemListener(this.faculteItemListener);
        comboFilterDepartement.removeItemListener(this.departementItemListener);

        // --- Repopulate comboFilterFaculte (Departments) based on current Bloc selection ---
        comboFilterFaculte.removeAllItems();
        comboFilterFaculte.addItem("Tous");
        List<String> departmentNamesForFilter = new ArrayList<>();
        // This populates the dropdown options, it should not filter actual users at this stage
        allDepartments.stream().map(Departement::getNom).sorted().collect(Collectors.toCollection(() -> departmentNamesForFilter));
        departmentNamesForFilter.forEach(comboFilterFaculte::addItem);
        // Restore previous selection or set to "Tous" if not found
        if (departmentNamesForFilter.contains(currentSelectedFaculte)) {
            comboFilterFaculte.setSelectedItem(currentSelectedFaculte);
        } else {
            comboFilterFaculte.setSelectedItem("Tous");
        }


        // --- Repopulate comboFilterDepartement (Blocs) based on current Faculte selection ---
        comboFilterDepartement.removeAllItems();
        comboFilterDepartement.addItem("Tous");
        List<String> blocNamesForFilter = new ArrayList<>();
        // This populates the dropdown options, it should not filter actual users at this stage
        allBlocs.stream().map(Bloc::getNom).sorted().collect(Collectors.toCollection(() -> blocNamesForFilter));
        blocNamesForFilter.forEach(comboFilterDepartement::addItem);
        // Restore previous selection or set to "Tous" if not found
        if (blocNamesForFilter.contains(currentSelectedDepartement)) {
            comboFilterDepartement.setSelectedItem(currentSelectedDepartement);
        } else {
            comboFilterDepartement.setSelectedItem("Tous");
        }
        
        // --- Restore item listeners ---
        comboFilterFaculte.addItemListener(this.faculteItemListener);
        comboFilterDepartement.addItemListener(this.departementItemListener);


        // --- Apply Actual Filters to Table Data ---
        // Start with all cached users and apply filters sequentially
        List<User> finalFilteredUsers = new ArrayList<>(cachedUsers);

        // Apply role filter
        boolean roleFilterActive = currentSelectedRole != null && !currentSelectedRole.equals("Tous");
        if (roleFilterActive) {
            finalFilteredUsers = finalFilteredUsers.stream()
                .filter(user -> user.getRole() != null && user.getRole().getRoleName().equals(currentSelectedRole))
                .collect(Collectors.toList());
            setColumnVisible("Rôle", false);
        } else {
            setColumnVisible("Rôle", true);
        }

        // Apply Faculté filter (using nomDepartement)
        boolean faculteFilterActive = currentSelectedFaculte != null && !currentSelectedFaculte.equals("Tous");
        if (faculteFilterActive) {
            finalFilteredUsers = finalFilteredUsers.stream()
                .filter(user -> user.getNomDepartement() != null && user.getNomDepartement().equals(currentSelectedFaculte))
                .collect(Collectors.toList());
            setColumnVisible("Faculté", false);
        } else {
            setColumnVisible("Faculté", true);
        }

        // Apply Département filter (using nomBloc)
        boolean departementFilterActive = currentSelectedDepartement != null && !currentSelectedDepartement.equals("Tous");
        if (departementFilterActive) {
            finalFilteredUsers = finalFilteredUsers.stream()
                .filter(user -> user.getNomBloc() != null && user.getNomBloc().equals(currentSelectedDepartement))
                .collect(Collectors.toList());
            setColumnVisible("Département", false);
        } else {
            setColumnVisible("Département", true);
        }
        
        // Apply search text filter
        String searchText = searchField.getText();
        final String placeholder = "Rechercher un utilisateur par nom, email...";
        if (searchText != null && !searchText.trim().isEmpty() && !searchText.equals(placeholder)) {
            final String lowerCaseQuery = searchText.trim().toLowerCase();
            finalFilteredUsers = finalFilteredUsers.stream() // Filter from the already filtered list
                .filter(user -> user.getNom().toLowerCase().contains(lowerCaseQuery) ||
                                 user.getPrenom().toLowerCase().contains(lowerCaseQuery) ||
                                 user.getEmail().toLowerCase().contains(lowerCaseQuery) ||
                                 user.getTelephone().toLowerCase().contains(lowerCaseQuery) ||
                                 user.getRole().getRoleName().toLowerCase().contains(lowerCaseQuery) ||
                                 (user.getNomBloc() != null && user.getNomBloc().toLowerCase().contains(lowerCaseQuery)) ||
                                 (user.getNomDepartement() != null && user.getNomDepartement().toLowerCase().contains(lowerCaseQuery)))
                .collect(Collectors.toList());
        }

        LOGGER.info("applyFilters: searchText='" + searchText + "', filteredUsers.size()=" + finalFilteredUsers.size());
        
        loadUsers(finalFilteredUsers);
    }

    private void addNewUser(ActionEvent e) {
        parentFrame.showOverlay(); // Show overlay while dialog is open
        try {
            UserDialog dialog = new UserDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                userDAO,
                blocDAO,
                allDepartments,
                allBlocs,
                null, // No user for new
                (success, message) -> { // DialogCallback
                    if (success) {
                        refreshData(); // Refresh table after successful add
                        UIUtils.showTemporaryMessage(this, message, true, 3000);
                    } else {
                        UIUtils.showTemporaryMessage(this, message, false, 3000);
                    }
                }
            );
            dialog.setVisible(true);
        } finally {
            parentFrame.hideOverlay(); // Hide overlay when dialog closes
        }
    }

    private void editUser(ActionEvent e) {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = userTable.convertRowIndexToModel(selectedRow);
            User userToEdit = modelRow >= 0 && modelRow < displayedUsers.size()
                ? displayedUsers.get(modelRow)
                : null;

            if (userToEdit != null) {
                parentFrame.showOverlay();
                try {
                    UserDialog dialog = new UserDialog(
                        (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                        userDAO,
                        blocDAO,
                        allDepartments,
                        allBlocs,
                        userToEdit, // Pass the user object
                        (success, message) -> { // DialogCallback
                            if (success) {
                                refreshData(); // Refresh table after successful edit
                                UIUtils.showTemporaryMessage(this, message, true, 3000);
                            } else {
                                UIUtils.showTemporaryMessage(this, message, false, 3000);
                            }
                        }
                    );
                    dialog.setVisible(true);
                } finally {
                    parentFrame.hideOverlay();
                }
            } else {
                UIUtils.showTemporaryMessage(this, "Utilisateur non trouvé.", false, 3000);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un utilisateur à modifier.", "Aucun Utilisateur Sélectionné", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteUser(ActionEvent e) {
        int[] selectedRows = userTable.getSelectedRows();
        if (selectedRows.length > 0) {
            // Build confirmation message
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer cet utilisateur ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d utilisateurs sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                List<Integer> userIds = new ArrayList<>();
                for (int selectedRow : selectedRows) {
                    int modelRow = userTable.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < displayedUsers.size()) {
                        userIds.add(displayedUsers.get(modelRow).getIdUtilisateur());
                    }
                }
                Integer actingUserId = com.gestion.salles.utils.SessionContext.getCurrentUserId();
                String actingUserEmail = com.gestion.salles.utils.SessionContext.getCurrentUserEmail();
                if (actingUserId == null) {
                    JOptionPane.showMessageDialog(this,
                        "Session invalide. Veuillez vous reconnecter pour supprimer des utilisateurs.",
                        "Session expirée", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                deleteButton.setEnabled(false);
                editButton.setEnabled(false);

                SwingWorker<int[], Void> worker = new SwingWorker<>() {
                    @Override
                    protected int[] doInBackground() {
                        com.gestion.salles.utils.SessionContext.setCurrentUser(actingUserId, actingUserEmail);
                        int successCount = 0;
                        try {
                            for (Integer userId : userIds) {
                                if (userDAO.deleteUser(userId)) {
                                    successCount++;
                                }
                            }
                        } finally {
                            com.gestion.salles.utils.SessionContext.clear();
                        }
                        return new int[]{successCount, userIds.size()};
                    }

                    @Override
                    protected void done() {
                        try {
                            int[] result = get();
                            int successCount = result[0];
                            int total = result[1];

                            if (successCount == total && total > 0) {
                                UIUtils.showTemporaryMessage(UserManagementPanel.this,
                                    String.format("%d utilisateur(s) supprimé(s) avec succès.", successCount), true, 3000);
                            } else if (successCount > 0) {
                                UIUtils.showTemporaryMessage(UserManagementPanel.this,
                                    String.format("%d utilisateur(s) supprimé(s), mais certaines suppressions ont échoué.", successCount), false, 3000);
                            } else {
                                UIUtils.showTemporaryMessage(UserManagementPanel.this,
                                    "Échec de la suppression des utilisateurs.", false, 3000);
                            }

                            refreshData();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(UserManagementPanel.this,
                                "Erreur lors de la suppression des utilisateurs.", "Erreur", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            deleteButton.setEnabled(true);
                            editButton.setEnabled(userTable.getSelectedRowCount() == 1);
                        }
                    }
                };
                worker.execute();
            }
        } else {
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner au moins un utilisateur à supprimer.", false, 3000);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        String searchFilter = searchField.getText().trim();
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        
        String currentSelectedRole = (String) comboFilterRole.getSelectedItem();
        boolean hasRoleFilter = currentSelectedRole != null && !currentSelectedRole.equals("Tous");

        String currentSelectedFaculte = (String) comboFilterFaculte.getSelectedItem(); // This is now a Department Name
        boolean hasFaculteFilter = currentSelectedFaculte != null && !currentSelectedFaculte.equals("Tous");
        
        String currentSelectedDepartement = (String) comboFilterDepartement.getSelectedItem(); // This is now a Bloc Name
        boolean hasDepartementFilter = currentSelectedDepartement != null && !currentSelectedDepartement.equals("Tous");

        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasRoleFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Rôle: ").append(currentSelectedRole);
        }
        if (hasFaculteFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Faculté: ").append(currentSelectedFaculte); // This is a Department Name
        }
        if (hasDepartementFilter) {
            if (dynamicFilterInfoBuilder.length() > 0) dynamicFilterInfoBuilder.append(" - ");
            dynamicFilterInfoBuilder.append("Département: ").append(currentSelectedDepartement); // This is a Bloc Name
        }

        // Determine which columns to print based on active filters and exclusions
        List<String> visibleColumnsForPrint = new ArrayList<>();
        for (String colName : originalColumnNames) {
            if ("Image".equals(colName)) continue;
            if (hasRoleFilter && "Rôle".equals(colName)) continue;
            if (hasFaculteFilter && "Faculté".equals(colName)) continue;
            if (hasDepartementFilter && "Département".equals(colName)) continue;
            visibleColumnsForPrint.add(colName);
        }

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Utilisateurs", dynamicFilterInfoBuilder.toString());
        job.setPrintable(new UserTablePrintable(
            userTable,
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
        String currentSelectedRole = (String) comboFilterRole.getSelectedItem();
        boolean hasRoleFilter = currentSelectedRole != null && !currentSelectedRole.equals("Tous");

        String currentSelectedFaculte = (String) comboFilterFaculte.getSelectedItem();
        boolean hasFaculteFilter = currentSelectedFaculte != null && !currentSelectedFaculte.equals("Tous");

        String currentSelectedDepartement = (String) comboFilterDepartement.getSelectedItem();
        boolean hasDepartementFilter = currentSelectedDepartement != null && !currentSelectedDepartement.equals("Tous");

        StringBuilder filterBuilder = new StringBuilder();
        String searchFilter = searchField.getText().trim();
        if (!searchFilter.isEmpty()) {
            filterBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }
        if (hasRoleFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Rôle: ").append(currentSelectedRole);
        }
        if (hasFaculteFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Faculté: ").append(currentSelectedFaculte);
        }
        if (hasDepartementFilter) {
            if (filterBuilder.length() > 0) filterBuilder.append(" - ");
            filterBuilder.append("Département: ").append(currentSelectedDepartement);
        }

        ManagementExportUtils.exportTableToExcel(
            this,
            userTable,
            "Utilisateurs",
            "Utilisateurs",
            filterBuilder.toString(),
            colName -> {
                if ("Image".equals(colName)) return false;
                if (hasRoleFilter && "Rôle".equals(colName)) return false;
                if (hasFaculteFilter && "Faculté".equals(colName)) return false;
                if (hasDepartementFilter && "Département".equals(colName)) return false;
                return true;
            }
        );
    }



}
