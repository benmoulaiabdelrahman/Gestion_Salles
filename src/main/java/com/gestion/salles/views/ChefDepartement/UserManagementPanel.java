package com.gestion.salles.views.ChefDepartement;

import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Admin.UserDialog;
import com.gestion.salles.views.Admin.UserTablePrintable;
import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;
import com.gestion.salles.views.shared.users.UserManagementUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterJob;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Panel for User Management in the Chef de Département Dashboard.
 * Displays and manages users belonging to the HoD's specific department.
 *
 * @author Gemini
 * @version 1.0 - Adapted from Admin UserManagementPanel.
 */
public class UserManagementPanel extends JPanel implements RefreshablePanel {
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;
    private CardLayout cardLayout;
    private JPanel contentCardPanel;
    private Timer searchDebounceTimer;

    private final UserDAO userDAO;
    private final DepartementDAO departementDAO;
    private final BlocDAO blocDAO;
    private List<User> allUsers;
    private List<User> displayedUsers;
    private List<Departement> allDepartments;
    private List<Bloc> allBlocs;
    private final DashboardChef refreshListener;
    private final User currentUser; // Added current user

    private final List<String> originalColumnNames = new ArrayList<>();

    public UserManagementPanel(User currentUser, DashboardChef listener) {
        this.currentUser = currentUser;
        this.userDAO = new UserDAO();
        this.departementDAO = new DepartementDAO();
        this.blocDAO = new com.gestion.salles.dao.BlocDAO();
        this.displayedUsers = new ArrayList<>();
        this.allUsers = new ArrayList<>();
        this.allDepartments = new ArrayList<>();
        this.allBlocs = new ArrayList<>();
        this.refreshListener = listener;
        initComponents();
        refreshData();
    }

    public UserManagementPanel(User currentUser) { // Overloaded constructor for convenience
        this(currentUser, null);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");

        JPanel centerContentPanel = new JPanel(new BorderLayout(10, 10));
        centerContentPanel.setOpaque(false);

        txtSearch = UIUtils.createStyledTextField("Rechercher un utilisateur par nom, email...");
        searchDebounceTimer = UserManagementUIHelper.installSearchDebounce(txtSearch, 300, this::applyFilters);

        btnAdd = UIUtils.createPrimaryButton("Ajouter");
        btnAdd.addActionListener(this::onAddUser);
        btnEdit = UIUtils.createSecondaryButton("Modifier");
        btnEdit.addActionListener(this::onEditUser);
        btnEdit.setEnabled(false);
        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDeleteUser);
        btnDelete.setEnabled(false);
        btnPrint = UIUtils.createSecondaryButton("Imprimer");
        btnPrint.addActionListener(this::onPrint);
        btnSaveExcel = UIUtils.createSecondaryButton("Exporter");
        btnSaveExcel.addActionListener(this::onSaveExcel);

        JPanel topToolbar = UIUtils.createSearchActionBar(
            txtSearch, btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel
        );

        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, null);
        centerContentPanel.add(topControlsPanel, BorderLayout.NORTH);

        tableModel = UserManagementUIHelper.createUserTableModel();

        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        userTable = (JTable) styledTableScrollPane.getViewport().getView();
        UserManagementUIHelper.configureUserTable(userTable, originalColumnNames);
        setColumnVisible("Rôle", false);
        setColumnVisible("Faculté", false);
        setColumnVisible("Département", false);

        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRowCount = userTable.getSelectedRowCount();
                btnEdit.setEnabled(selectedRowCount == 1);
                btnDelete.setEnabled(selectedRowCount >= 1);
            }
        });

        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = userTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < userTable.getRowCount()) {
                        if (!userTable.isRowSelected(r)) {
                            userTable.addRowSelectionInterval(r, r);
                        }
                    }
                }
            }
        });

        lblNoResults = new JLabel("Aucun résultat trouvé pour votre recherche.", SwingConstants.CENTER);
        lblNoResults.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblNoResults.setFont(lblNoResults.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        
        cardLayout = new CardLayout();
        contentCardPanel = new JPanel(cardLayout);
        contentCardPanel.add(styledTableScrollPane, "tableCard");
        contentCardPanel.add(lblNoResults, "noResultsCard");

        centerContentPanel.add(contentCardPanel, BorderLayout.CENTER);
        add(centerContentPanel, BorderLayout.CENTER);

    }
    
    @Override
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Integer blocId = currentUser.getIdBloc();
                if (blocId != null) {
                    // Get all users for this bloc
                    List<User> blocUsers = userDAO.getUsersByBloc(blocId);
                    
                    // Start with the current HoD
                    allUsers = new ArrayList<>();
                    allUsers.add(currentUser); 
                    
                    // Add teachers from the bloc, ensuring no duplicates and only ENSEIGNANT roles
                    blocUsers.forEach(user -> {
                        if (user.getIdUtilisateur() != currentUser.getIdUtilisateur() && user.isEnseignant()) {
                            allUsers.add(user);
                        }
                    });
                    
                    // Sort users by name for consistent display
                    allUsers.sort(Comparator.comparing(User::getFullName));


                    // Filter departments and blocs relevant to the current HoD's department
                    Departement hodDepartement = departementDAO.getDepartementById(currentUser.getIdDepartement());
                    if (hodDepartement != null) {
                        allDepartments = new ArrayList<>();
                        allDepartments.add(hodDepartement); // HoD only manages their department's users
                        allBlocs = new ArrayList<>();
                        Bloc hodBloc = blocDAO.getBlocById(hodDepartement.getIdBloc());
                        if (hodBloc != null) {
                            allBlocs.add(hodBloc);
                        }
                    }
                } else {
                    allUsers = new ArrayList<>(); // No department assigned, no users to show
                    allDepartments = new ArrayList<>();
                    allBlocs = new ArrayList<>();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); 
                    initializeFilterComboBoxes();
                    if (refreshListener != null) {
                        refreshListener.onDataChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                        "Erreur lors du chargement des données des utilisateurs.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void initializeFilterComboBoxes() {
        applyFilters();
    }

    private void populateTable(List<User> users) {
        tableModel.setRowCount(0);
        displayedUsers.clear();

        if (users == null || users.isEmpty()) {
            cardLayout.show(contentCardPanel, "noResultsCard");
        } else {
            cardLayout.show(contentCardPanel, "tableCard");
            displayedUsers.addAll(users);
            for (User user : displayedUsers) {
                tableModel.addRow(new Object[]{ 
                    user.getPhotoProfil(),
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

    private void setColumnVisible(String columnName, boolean visible) {
        javax.swing.table.TableColumnModel columnModel = userTable.getColumnModel();
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
                column.setResizable(false);
            } else {
                column.setMinWidth(15);
                column.setMaxWidth(Integer.MAX_VALUE);
                column.setPreferredWidth(100);
                column.setResizable(true);
            }
        }
    }
    


    private void applyFilters() {
        List<User> filteredUsers = new ArrayList<>(allUsers);

        String searchText = txtSearch.getText();
        String placeholder = "Rechercher un utilisateur par nom, email...";
        if (searchText != null && !searchText.trim().isEmpty() && !searchText.equals(placeholder)) {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredUsers = filteredUsers.stream()
                .filter(user -> user.getNom().toLowerCase().contains(lowerCaseSearchText) ||
                                user.getPrenom().toLowerCase().contains(lowerCaseSearchText) ||
                                user.getEmail().toLowerCase().contains(lowerCaseSearchText) ||
                                user.getTelephone().toLowerCase().contains(lowerCaseSearchText) ||
                                (user.getRole() != null && user.getRole().getRoleName().toLowerCase().contains(lowerCaseSearchText)) ||
                                (user.getNomBloc() != null && user.getNomBloc().toLowerCase().contains(lowerCaseSearchText)) ||
                                (user.getNomDepartement() != null && user.getNomDepartement().toLowerCase().contains(lowerCaseSearchText)))
                .collect(Collectors.toList());
        }
        populateTable(filteredUsers);
    }

    private void onAddUser(ActionEvent e) {
        UserDialog dialog = new UserDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                userDAO,
                blocDAO,
                allDepartments,
                allBlocs,
                null,
                (success, message) -> {
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000);
                },
                User.Role.Enseignant,
                currentUser.getIdDepartement(),
                currentUser.getIdBloc()
        );
        try {
            if (refreshListener != null) {
                refreshListener.showOverlay();
            }
            dialog.setVisible(true);
        } finally {
            if (refreshListener != null) {
                refreshListener.hideOverlay();
            }
        }
    }

    private void onEditUser(ActionEvent e) {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow != -1) {
            User selectedUser = displayedUsers.get(userTable.convertRowIndexToModel(selectedRow));
            if (selectedUser != null) {
                UserDialog dialog = new UserDialog(
                        (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                        userDAO,
                        blocDAO,
                        allDepartments,
                        allBlocs,
                        selectedUser,
                        (success, message) -> {
                            refreshData();
                            UIUtils.showTemporaryMessage(this, message, success, 3000);
                        },
                        User.Role.Enseignant,
                        currentUser.getIdDepartement(),
                        currentUser.getIdBloc()
                );
                try {
                    if (refreshListener != null) {
                        refreshListener.showOverlay();
                    }
                    dialog.setVisible(true);
                } finally {
                    if (refreshListener != null) {
                        refreshListener.hideOverlay();
                    }
                }
            } else {
                UIUtils.showTemporaryMessage(this, "Utilisateur sélectionné introuvable dans les données.", false, 3000);
            }
        } else {
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner un utilisateur à modifier.", false, 3000);
        }
    }

    private void onDeleteUser(ActionEvent e) {
        int[] selectedRows = userTable.getSelectedRows();
        if (selectedRows.length > 0) {
            String message;
            if (selectedRows.length == 1) {
                message = "Voulez-vous vraiment supprimer cet utilisateur ?\nCette action est irréversible.";
            } else {
                message = String.format("Voulez-vous vraiment supprimer les %d éléments sélectionnés ?\nCette action est irréversible.", selectedRows.length);
            }

            int confirm = JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                List<Integer> userIds = new ArrayList<>();
                for (int selectedRow : selectedRows) {
                    User selectedUser = displayedUsers.get(userTable.convertRowIndexToModel(selectedRow));
                    if (selectedUser != null) {
                        userIds.add(selectedUser.getIdUtilisateur());
                    }
                }

                btnDelete.setEnabled(false);
                btnEdit.setEnabled(false);

                SwingWorker<int[], Void> worker = new SwingWorker<>() {
                    @Override
                    protected int[] doInBackground() {
                        int successCount = 0;
                        for (Integer userId : userIds) {
                            if (userDAO.deleteUser(userId)) {
                                successCount++;
                            }
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
                                    "Échec de la suppression d'un ou plusieurs utilisateurs.", false, 3000);
                            }

                            refreshData();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(UserManagementPanel.this,
                                "Erreur lors de la suppression des utilisateurs.", "Erreur", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            btnDelete.setEnabled(true);
                        }
                    }
                };
                worker.execute();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins un utilisateur à supprimer.", "Avertissement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        String subtitle = UserManagementUIHelper.buildScopeSubtitle(currentUser);
            
        StringBuilder dynamicFilterInfoBuilder = new StringBuilder();
        String searchFilter = txtSearch.getText().trim();
        if (!searchFilter.isEmpty()) {
            dynamicFilterInfoBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }


        List<String> visibleColumnsForPrint = new ArrayList<>();
        for (String colName : originalColumnNames) {
            if (colName.equals("Image")) continue;
            if ("Faculté".equals(colName)) continue;
            if ("Département".equals(colName)) continue;
            visibleColumnsForPrint.add(colName);
        }

        String filterInfo = subtitle + (dynamicFilterInfoBuilder.length() > 0 ? " - " + dynamicFilterInfoBuilder : "");
        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Utilisateurs", filterInfo);
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
        String subtitle = UserManagementUIHelper.buildScopeSubtitle(currentUser);
        String searchFilter = txtSearch.getText().trim();
        StringBuilder filterBuilder = new StringBuilder();
        if (!searchFilter.isEmpty()) {
            filterBuilder.append("Filtre: \"").append(searchFilter).append("\"");
        }
        String filterInfo = subtitle + (filterBuilder.length() > 0 ? " - " + filterBuilder : "");

        ManagementExportUtils.exportTableToExcel(
            this,
            userTable,
            "Utilisateurs",
            "Utilisateurs",
            filterInfo,
            colName -> {
                if ("Image".equals(colName)) return false;
                if ("Faculté".equals(colName)) return false;
                if ("Département".equals(colName)) return false;
                return true;
            }
        );
    }

}
