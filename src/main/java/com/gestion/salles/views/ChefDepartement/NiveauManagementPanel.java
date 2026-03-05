package com.gestion.salles.views.ChefDepartement;

import com.gestion.salles.dao.NiveauDAO;
import com.gestion.salles.models.Niveau;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.Admin.NiveauDialog;
import com.gestion.salles.views.Admin.NiveauTablePrintable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.gestion.salles.views.shared.management.ManagementExportUtils;
import com.gestion.salles.views.shared.management.ManagementHeaderBuilder;

/**
 * Panel for Niveau Management in the Chef de Département Dashboard.
 * Shows only niveaux belonging to the HoD's bloc. No faculty/department filters.
 *
 * @version 1.0 - Built to match Chef RoomManagementPanel structure.
 */
public class NiveauManagementPanel extends JPanel implements RefreshablePanel {

    private static final Logger LOGGER = Logger.getLogger(NiveauManagementPanel.class.getName());

    // ── UI Components ─────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnPrint;
    private JButton btnSaveExcel;
    private JTable niveauTable;
    private DefaultTableModel tableModel;
    private JLabel lblNoResults;

    // CardLayout for toggling between table and "no results" message
    private CardLayout cardLayout;
    private JPanel tableCardPanel;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final NiveauDAO niveauDAO;
    private List<Niveau> allNiveaux;
    private List<Niveau> displayedNiveaux;

    private final List<String> originalColumnNames = new ArrayList<>();
    private final DashboardChef parentDashboard;
    private final User currentUser;


    public NiveauManagementPanel(User currentUser, DashboardChef parentDashboard) {
        this.currentUser = currentUser;
        this.parentDashboard = parentDashboard;
        this.niveauDAO = new NiveauDAO();
        this.allNiveaux = new ArrayList<>();
        this.displayedNiveaux = new ArrayList<>();
        initComponents();
        refreshData();
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");

        JPanel centerContentPanel = new JPanel(new BorderLayout(10, 10));
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
                txtSearch, btnAdd, btnEdit, btnDelete, btnPrint, btnSaveExcel);
        JPanel topControlsPanel = ManagementHeaderBuilder.buildHeaderWithFilter(topToolbar, null);
        centerContentPanel.add(topControlsPanel, BorderLayout.NORTH);

        // --- Table ---
        // No Faculté / Département columns — scope is already fixed to the HoD's bloc
        String[] columnNames = {"Nom", "Code", "Nb Étudiants", "Nb Groupes", "Année Académique"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JScrollPane styledTableScrollPane = UIUtils.createStyledTable(tableModel);
        niveauTable = (JTable) styledTableScrollPane.getViewport().getView();

        niveauTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Nom
        niveauTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Code
        niveauTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Nb Étudiants
        niveauTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Nb Groupes
        niveauTable.getColumnModel().getColumn(4).setPreferredWidth(130); // Année Académique

        for (int i = 0; i < niveauTable.getColumnCount(); i++) {
            originalColumnNames.add(niveauTable.getColumnModel().getColumn(i).getHeaderValue().toString());
        }

        niveauTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int count = niveauTable.getSelectedRowCount();
                btnEdit.setEnabled(count == 1);
                btnDelete.setEnabled(count >= 1);
            }
        });

        niveauTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = niveauTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < niveauTable.getRowCount() && !niveauTable.isRowSelected(r)) {
                        niveauTable.addRowSelectionInterval(r, r);
                    }
                }
            }
        });

        // --- "No results" label ---
        lblNoResults = new JLabel("Aucun résultat trouvé pour votre recherche.", SwingConstants.CENTER);
        lblNoResults.setForeground(ThemeConstants.PRIMARY_TEXT);
        lblNoResults.setFont(lblNoResults.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        // --- Card panel ---
        cardLayout = new CardLayout();
        tableCardPanel = new JPanel(cardLayout);
        tableCardPanel.add(styledTableScrollPane, "table");
        tableCardPanel.add(lblNoResults, "noResults");

        centerContentPanel.add(tableCardPanel, BorderLayout.CENTER);
        add(centerContentPanel, BorderLayout.CENTER);
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    @Override
    public void refreshData() {
        SwingWorker<List<Niveau>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Niveau> doInBackground() throws Exception {
                Integer blocId = currentUser.getIdBloc();
                if (blocId == null) return new ArrayList<>();
                return niveauDAO.getNiveauxByBloc(blocId);
            }

            @Override
            protected void done() {
                try {
                    allNiveaux = get();
                    populateTable(allNiveaux);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading niveaux", e);
                    JOptionPane.showMessageDialog(NiveauManagementPanel.this,
                            "Erreur lors du chargement des données des niveaux.",
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Niveau> niveaux) {
        tableModel.setRowCount(0);
        displayedNiveaux.clear();

        if (niveaux == null || niveaux.isEmpty()) {
            cardLayout.show(tableCardPanel, "noResults");
            return;
        }

        cardLayout.show(tableCardPanel, "table");
        displayedNiveaux.addAll(niveaux);
        for (Niveau n : displayedNiveaux) {
            tableModel.addRow(new Object[]{
                    n.getNom(),
                    n.getCode(),
                    n.getNombreEtudiants(),
                    n.getNombreGroupes(),
                    n.getAnneeAcademique()
            });
        }
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    private void applyFilters() {
        String searchText = txtSearch.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            populateTable(allNiveaux);
            return;
        }
        String q = searchText.trim().toLowerCase();
        List<Niveau> filtered = allNiveaux.stream()
                .filter(n -> n.getNom().toLowerCase().contains(q)
                        || n.getCode().toLowerCase().contains(q)
                        || String.valueOf(n.getNombreEtudiants()).contains(q)
                        || String.valueOf(n.getNombreGroupes()).contains(q)
                        || (n.getAnneeAcademique() != null && n.getAnneeAcademique().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        populateTable(filtered);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onAddNiveau(ActionEvent e) {
        parentDashboard.showOverlay();
        NiveauDialog dialog = new NiveauDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                niveauDAO,
                allNiveaux,
                null,
                (success, message) -> {
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000);
                },
                currentUser.getIdDepartement(),
                currentUser.getIdBloc()
        );
        try {
            dialog.setVisible(true);
        } finally {
            parentDashboard.hideOverlay();
        }
    }

    private void onEditNiveau(ActionEvent e) {
        int selectedRow = niveauTable.getSelectedRow();
        if (selectedRow == -1) {
            UIUtils.showTemporaryMessage(this, "Veuillez sélectionner un niveau à modifier.", false, 3000);
            return;
        }

        if (selectedRow < 0 || selectedRow >= displayedNiveaux.size()) return;
        Niveau selectedNiveau = displayedNiveaux.get(selectedRow);

        parentDashboard.showOverlay();
        NiveauDialog dialog = new NiveauDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                niveauDAO,
                allNiveaux,
                selectedNiveau,
                (success, message) -> {
                    refreshData();
                    UIUtils.showTemporaryMessage(this, message, success, 3000);
                },
                currentUser.getIdDepartement(),
                currentUser.getIdBloc()
        );
        try {
            dialog.setVisible(true);
        } finally {
            parentDashboard.hideOverlay();
        }
    }

    private void onDeleteNiveau(ActionEvent e) {
        int[] selectedRows = niveauTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins un niveau à supprimer.",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String message = selectedRows.length == 1
                ? "Voulez-vous vraiment supprimer ce niveau ?\nCette action est irréversible."
                : String.format("Voulez-vous vraiment supprimer les %d niveaux sélectionnés ?\nCette action est irréversible.", selectedRows.length);

        if (JOptionPane.showConfirmDialog(this, message, "Confirmer Suppression",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;

        List<Niveau> niveauxToDelete = new ArrayList<>();
        for (int row : selectedRows) {
            if (row >= 0 && row < displayedNiveaux.size()) {
                niveauxToDelete.add(displayedNiveaux.get(row));
            }
        }

        boolean allSuccess = true;
        for (Niveau niveau : niveauxToDelete) {
            if (!niveauDAO.deleteNiveau(niveau.getId())) {
                allSuccess = false;
            }
        }

        UIUtils.showTemporaryMessage(this,
                allSuccess
                        ? String.format("%d niveau(x) supprimé(s) avec succès.", selectedRows.length)
                        : "Échec de la suppression d'un ou plusieurs niveaux.",
                allSuccess, 3000);
        refreshData();
    }

    // ── Print & Export ────────────────────────────────────────────────────────

    private void onPrint(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();

        String subtitle   = "Faculté: " + currentUser.getNomDepartement()
                          + " - Département: " + currentUser.getNomBloc();
        String searchText = txtSearch.getText().trim();
        String filterInfo = searchText.isEmpty() ? subtitle : subtitle + " - Filtre: \"" + searchText + "\"";

        String dynamicInfo = ManagementExportUtils.buildFilterInfo("Niveaux", filterInfo);
        job.setPrintable(new NiveauTablePrintable(
            niveauTable,
            ManagementExportUtils.UNIVERSITY_TITLE,
            dynamicInfo,
            new ArrayList<>(originalColumnNames)
        ));

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Document envoyé à l'imprimante avec succès.",
                        "Impression Réussie", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.awt.print.PrinterException ex) {
                LOGGER.log(Level.WARNING, "Print failed", ex);
                JOptionPane.showMessageDialog(this,
                        "Échec de l'impression. Veuillez vérifier votre imprimante.",
                        "Erreur d'Impression", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSaveExcel(ActionEvent e) {
        String subtitle = "Faculté: " + currentUser.getNomDepartement()
            + " - Département: " + currentUser.getNomBloc();
        String searchText = txtSearch.getText().trim();
        String filterInfo = searchText.isEmpty() ? subtitle : subtitle + " - Filtre: \"" + searchText + "\"";

        ManagementExportUtils.exportTableToExcel(
            this,
            niveauTable,
            "Niveaux_" + currentUser.getNomBloc(),
            "Niveaux",
            filterInfo,
            colName -> true
        );
    }
}
