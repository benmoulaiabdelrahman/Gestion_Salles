package com.gestion.salles.views.Admin;

import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.models.Bloc;
import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.dao.NiveauDAO;
import com.gestion.salles.dao.RoomDAO;
import com.gestion.salles.dao.ScheduleDAO;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Niveau;
import com.gestion.salles.models.Reservation;
import com.gestion.salles.models.Room;
import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.ItemEvent; // Needed for ItemListener
import java.awt.event.ItemListener; // Needed for ItemListener
import java.time.LocalDate;
import java.time.LocalTime; // Needed for ScheduleEntry
import java.util.ArrayList; // Needed for List implementations
import java.util.List; // Needed for List declarations
import java.text.MessageFormat; // For printing header/footer

// PDFBox imports
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject; // Added
import javax.imageio.ImageIO; // Added
import java.io.ByteArrayOutputStream; // Added
import java.io.IOException; // Already there, but make sure
import java.net.URL; // Already there, but make sure

// Printing imports
import java.awt.print.PrinterException;
import java.awt.image.BufferedImage; // Added
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import org.apache.pdfbox.printing.PDFPageable;

import java.util.logging.Logger;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.schedule.ScheduleTableSupport;
import com.gestion.salles.views.shared.schedule.SchedulePanelBase;
import com.gestion.salles.views.shared.schedule.ScheduleUiText;

public class ScheduleViewerPanel extends SchedulePanelBase implements RefreshablePanel {

    private static final Logger LOGGER = Logger.getLogger(ScheduleViewerPanel.class.getName());

    public JComboBox<String> getContextFilterComboBox() {
        return contextFilterComboBox;
    }
    private JComboBox<Object> faculteFilterComboBox;
    private JComboBox<Object> blocFilterComboBox;
    private JComboBox<Object> filterComboBox;
    private JComboBox<String> contextFilterComboBox;
    // scheduleTable and scheduleTableModel provided by SchedulePanelBase
    private JButton datePickerButton;
    private JCheckBox allDatesCheckBox;
    private LocalDate selectedDate;
    private LocalDate startDateOfWeek; // New field for start of the week
    private LocalDate endDateOfWeek;   // New field for end of the week

    private JLabel blocLabel;
    private JLabel departementLabel; 

    // Listeners to prevent re-entrancy issues
    private ItemListener blocFilterListener;
    private ItemListener departementFilterListener;

    private ScheduleDAO scheduleDAO;
    private DepartementDAO departementDAO;
    private RoomDAO roomDAO = new RoomDAO();
    private UserDAO userDAO = new UserDAO();
    private NiveauDAO niveauDAO = new NiveauDAO();
    private BlocDAO blocDAO = new BlocDAO(); // New DAO instance

    private List<Departement> allDepartements;
    private List<Room> allRooms;
    private List<User> allTeachers;
    private List<Niveau> allNiveaux;
    private List<Bloc> allBlocs; // Declaring allBlocs

    // Define the session time slots based on 1h 25m duration and 10m breaks
    private Dashboard parentFrame; // Added field



    public ScheduleViewerPanel(Dashboard parentFrame) { // Modified constructor
        this.scheduleDAO = new ScheduleDAO();
        this.departementDAO = new DepartementDAO();
        // Calculate the current week (Saturday to Friday)
        this.selectedDate = LocalDate.now();
        this.startDateOfWeek = calculateStartOfWeek(selectedDate);
        this.endDateOfWeek = startDateOfWeek.plusDays(6); // Friday of the same week
        this.parentFrame = parentFrame; // Assigned parentFrame

        // Initialize listeners once
        blocFilterListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFilterComboBox();
            }
        };

        departementFilterListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFilterComboBox();
            }
        };

        initComponents();
        refreshData();
    }
    @Override
    public void refreshData() {
        loadInitialFilterData();
    }

    private void initComponents() {
        // Main panel setup with a 2-row MigLayout. Controls on top, table below.
        setLayout(new MigLayout("insets 20, fill", "[grow]", "[][grow, fill, 200]"));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");
        // Removed redundant setBorder, relying on MigLayout insets for padding.

        // --- A single container for all top filter controls ---
        JPanel controlsContainer = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));
        controlsContainer.setOpaque(false);

        // --- Context Filter Row (Row 1) ---
        JPanel contextFilterRow = new JPanel(new MigLayout("insets 0, fillx, gap 10", "[grow, fill, 0][grow, fill, 0][grow, fill, 0]"));
        contextFilterRow.setOpaque(false);

        JPanel contextPairPanel = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[][grow, fill, 0]"));
        contextPairPanel.setOpaque(false);
        contextPairPanel.add(new JLabel("Filtrer par:"), "align label");
        contextFilterComboBox = new JComboBox<>(new String[]{"Salle", "Enseignant", "Niveau"});
        contextFilterComboBox.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.FOCUS_BORDER));
        contextFilterComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFilterComboBox();
                checkAndEnableTousCheckBox();
            }
        });
        contextPairPanel.add(contextFilterComboBox, "growx, w 0, h 35!, sg combox");
        contextFilterRow.add(contextPairPanel, "growx, w 0");

        // Faculté Filter
        JPanel facultePairPanel = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[][grow, fill, 0]"));
        facultePairPanel.setOpaque(false);
        blocLabel = new JLabel("Faculté:");
        facultePairPanel.add(blocLabel, "align label");
        faculteFilterComboBox = new JComboBox<>();
        faculteFilterComboBox.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.FOCUS_BORDER));
        faculteFilterComboBox.addItemListener(blocFilterListener);
        facultePairPanel.add(faculteFilterComboBox, "growx, w 0, h 35!, sg combox");
        contextFilterRow.add(facultePairPanel, "growx, w 0");

        // Département Filter
        JPanel departementPairPanel = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[][grow, fill, 0]"));
        departementPairPanel.setOpaque(false);
        departementLabel = new JLabel("Département:");
        departementPairPanel.add(departementLabel, "align label");
        blocFilterComboBox = new JComboBox<>();
        blocFilterComboBox.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.FOCUS_BORDER));
        blocFilterComboBox.addItemListener(departementFilterListener);
        departementPairPanel.add(blocFilterComboBox, "growx, w 0, h 35!, sg combox");
        contextFilterRow.add(departementPairPanel, "growx, w 0");

        controlsContainer.add(contextFilterRow, "growx");

        // --- Detailed Filters and Actions Row (Row 2) ---
        JPanel detailsFilterRow = new JPanel(new MigLayout("insets 8 0 0 0, fillx, gap 10", "[grow, fill, 0][grow, fill, 0][grow, fill, 0]"));
        detailsFilterRow.setOpaque(false);

        JPanel selectionPairPanel = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[][grow, fill, 0]"));
        selectionPairPanel.setOpaque(false);
        selectionPairPanel.add(new JLabel("Sélection:"), "align label");
        filterComboBox = new JComboBox<>();
        filterComboBox.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:2;" +
                "innerFocusWidth:0;" +
                "borderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(com.gestion.salles.utils.ThemeConstants.FOCUS_BORDER));
        filterComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadScheduleData();
                checkAndEnableTousCheckBox();
            }
        });
        selectionPairPanel.add(filterComboBox, "growx, w 0, h 35!");
        detailsFilterRow.add(selectionPairPanel, "growx, w 0");

        JPanel dateControlsPanel = new JPanel(new MigLayout("insets 0, fillx, gap 5", "[grow, fill][pref!]"));
        dateControlsPanel.setOpaque(false);
        datePickerButton = com.gestion.salles.utils.UIUtils.createSecondaryButton(selectedDate.toString());
        datePickerButton.addActionListener(e -> showDatePickerDialog());
        dateControlsPanel.add(datePickerButton, "h 35!, growx");

        allDatesCheckBox = UIUtils.createStyledCheckBox(ScheduleUiText.CHECKBOX_FULL_WEEK);
        allDatesCheckBox.setEnabled(false);
        allDatesCheckBox.addActionListener(e -> {
            datePickerButton.setEnabled(!allDatesCheckBox.isSelected());
            loadScheduleData();
        });
        dateControlsPanel.add(allDatesCheckBox, "h 35!, aligny center");
        detailsFilterRow.add(dateControlsPanel, "growx, w 0");

        JPanel actionButtonsPanel = new JPanel(new MigLayout("insets 0, fillx, gap 5", "[grow, fill][grow, fill]"));
        actionButtonsPanel.setOpaque(false);
        JButton savePdfButton = com.gestion.salles.utils.UIUtils.createSecondaryButton(ScheduleUiText.BUTTON_EXPORT_PDF);
        savePdfButton.addActionListener(e -> exportScheduleToPdf());
        actionButtonsPanel.add(savePdfButton, "h 35!, growx");

        JButton printButton = com.gestion.salles.utils.UIUtils.createSecondaryButton(ScheduleUiText.BUTTON_PRINT);
        printButton.addActionListener(e -> printSchedule());
        actionButtonsPanel.add(printButton, "h 35!, growx");
        detailsFilterRow.add(actionButtonsPanel, "growx, w 0");
        
        controlsContainer.add(detailsFilterRow, "growx");

        // Add the single controls container to the main panel
        add(controlsContainer, "wrap, growx");

        // --- Schedule Table (The second row, set to grow and fill) ---
        JScrollPane scrollPane = createScheduleTable(new ScheduleTableCellRenderer(this));
        
        // Add listener to adjust row heights when the viewport is resized
        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> adjustRowHeight());
            }
        });
        
        // Add scroll pane to the second row, ensuring it grows to fill all available space
        add(scrollPane, "grow, push"); 

        updateFilterComboBox(); // Initial setup of filter options and schedule load
        // All filters are now always visible, no initial hide needed.
    }

    private void checkAndEnableTousCheckBox() {
        boolean allFiltersSelected = true;

        // Check contextFilterComboBox (Admin panel has no placeholder item)
        if ("Sélectionner...".equals(contextFilterComboBox.getSelectedItem())) {
            allFiltersSelected = false;
        }

        // Check faculteFilterComboBox (Faculté)
        if (faculteFilterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(faculteFilterComboBox.getSelectedItem())) {
            allFiltersSelected = false;
        }

        // Check blocFilterComboBox (Département)
        if (blocFilterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(blocFilterComboBox.getSelectedItem())) {
            allFiltersSelected = false;
        }

        // Check filterComboBox (Sélection)
        if (filterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(filterComboBox.getSelectedItem())) {
            allFiltersSelected = false;
        }

        if (allFiltersSelected) {
            allDatesCheckBox.setEnabled(true);
        } else {
            allDatesCheckBox.setEnabled(false);
            if (allDatesCheckBox.isSelected()) {
                allDatesCheckBox.setSelected(false);
                datePickerButton.setEnabled(true); // Ensure date picker is enabled if "Tous" is deselected
                loadScheduleData(); // Reload data when "Tous" is deselected due to filters becoming invalid
            }
        }
    }

    private void populateFaculteFilterComboBox(List<Departement> departementsToPopulate, Departement selectedDepartement) {
        faculteFilterComboBox.removeItemListener(blocFilterListener); // Remove listener before repopulating
        DefaultComboBoxModel<Object> newDepartementModelForFaculteFilter = new DefaultComboBoxModel<>();
        newDepartementModelForFaculteFilter.addElement("Sélectionner...");
        for (Departement dept : departementsToPopulate) {
            newDepartementModelForFaculteFilter.addElement(dept);
        }
        faculteFilterComboBox.setModel(newDepartementModelForFaculteFilter);
        if (selectedDepartement != null) {
            faculteFilterComboBox.setSelectedItem(selectedDepartement);
        }
        faculteFilterComboBox.addItemListener(blocFilterListener); // Re-add listener
    }

    private void populateBlocFilterComboBox(List<Bloc> blocsToPopulate, Bloc selectedBloc) {
        blocFilterComboBox.removeItemListener(departementFilterListener); // Remove listener before repopulating
        DefaultComboBoxModel<Object> newBlocModelForBlocFilter = new DefaultComboBoxModel<>();
        newBlocModelForBlocFilter.addElement("Sélectionner...");
        for (Bloc bloc : blocsToPopulate) {
            newBlocModelForBlocFilter.addElement(bloc);
        }
        blocFilterComboBox.setModel(newBlocModelForBlocFilter);
        if (selectedBloc != null) {
            blocFilterComboBox.setSelectedItem(selectedBloc);
        }
        blocFilterComboBox.addItemListener(departementFilterListener); // Re-add listener
    }


    private void loadInitialFilterData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Load blocs
                allBlocs = blocDAO.getAllActiveBlocs();
                // Load departments
                allDepartements = departementDAO.getAllActiveDepartements();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions during doInBackground
                    
                    // Temporarily remove listeners
                    faculteFilterComboBox.removeItemListener(blocFilterListener);
                    blocFilterComboBox.removeItemListener(departementFilterListener);

                    // Populate faculteFilterComboBox with Departments (Faculté)
                    DefaultComboBoxModel<Object> departementModelForFaculteFilter = new DefaultComboBoxModel<>();
                    departementModelForFaculteFilter.addElement("Sélectionner...");
                    for (Departement dept : allDepartements) {
                        departementModelForFaculteFilter.addElement(dept);
                    }
                    faculteFilterComboBox.setModel(departementModelForFaculteFilter);

                    // Populate blocFilterComboBox with Blocs (Département)
                    DefaultComboBoxModel<Object> blocModelForBlocFilter = new DefaultComboBoxModel<>();
                    blocModelForBlocFilter.addElement("Sélectionner...");
                    for (Bloc bloc : allBlocs) {
                        blocModelForBlocFilter.addElement(bloc);
                    }
                    blocFilterComboBox.setModel(blocModelForBlocFilter);
                    
                    // Re-add listeners
                    faculteFilterComboBox.addItemListener(blocFilterListener);
                    blocFilterComboBox.addItemListener(departementFilterListener);

                    updateFilterComboBox(); // Call updateFilterComboBox only after both are loaded
                    checkAndEnableTousCheckBox(); // Add this line
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ScheduleViewerPanel.this, "Erreur lors du chargement initial des données de filtre.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void populateScheduleGrid(List<ScheduleEntry> entries) {
        int contextIndex = contextFilterComboBox.getSelectedIndex();
        // In the Admin panel, contextFilterComboBox is {"Salle", "Enseignant", "Niveau"}
        // So Niveau is index 2
        boolean isNiveauFilter = (contextIndex == 2);
        
        Integer totalGroups = null;
        if (isNiveauFilter) {
            Object selectedNiveauItem = filterComboBox.getSelectedItem();
            if (selectedNiveauItem instanceof Niveau) {
                totalGroups = ((Niveau) selectedNiveauItem).getNombreGroupes();
            }
        }
        populateScheduleGrid(entries, isNiveauFilter, totalGroups);
    }

    private void updateFilterComboBox() {
        final int selectedContextIndex = contextFilterComboBox.getSelectedIndex();
        final DefaultComboBoxModel<Object> filterModel = new DefaultComboBoxModel<>();
        filterModel.addElement("Sélectionner..."); // Default empty selection
        
        final DefaultComboBoxModel<Object> currentDepartementModel = (DefaultComboBoxModel<Object>) blocFilterComboBox.getModel();
        
        // faculteFilterComboBox now represents Departement (Faculté)
        final Object selectedDepartementItem = faculteFilterComboBox.getSelectedItem();
        Departement currentSelectedDepartement = null;
        if (selectedDepartementItem instanceof Departement) {
            currentSelectedDepartement = (Departement) selectedDepartementItem;
        }
        final Departement finalSelectedDepartement = currentSelectedDepartement;
        final Integer departementId = (finalSelectedDepartement != null && finalSelectedDepartement.getId() != 0) ? finalSelectedDepartement.getId() : null;

        // blocFilterComboBox now represents Bloc (Département)
        final Object selectedBlocItem = blocFilterComboBox.getSelectedItem();
        Bloc currentSelectedBloc = null;
        if (selectedBlocItem instanceof Bloc) {
            currentSelectedBloc = (Bloc) selectedBlocItem;
        }
        final Bloc finalSelectedBloc = currentSelectedBloc;
        final Integer blocId = (finalSelectedBloc != null && finalSelectedBloc.getId() != 0) ? finalSelectedBloc.getId() : null;

        SwingWorker<List<?>, Void> worker = new SwingWorker<>() {
            List<Departement> departementsToPopulateFaculte = new ArrayList<>();
            List<Bloc> blocsToPopulateBloc = new ArrayList<>();
            List<?> resultForMainFilter = new ArrayList<>(); // Renamed for clarity

            @Override
            protected List<?> doInBackground() throws Exception {
                // Load blocs and departments first
                List<Bloc> currentBlocs = blocDAO.getAllActiveBlocs();
                List<Departement> currentDepartements = departementDAO.getAllActiveDepartements();

                // Determine options for faculteFilterComboBox (Departments) and blocFilterComboBox (Blocs)
                if (blocId != null) { // If a Bloc is selected in blocFilterComboBox
                    // Filter departments that have rooms in the selected bloc
                    departementsToPopulateFaculte = departementDAO.getDepartementsByBloc(blocId);
                } else {
                    // If no Bloc is selected, show all active departments
                    departementsToPopulateFaculte = currentDepartements;
                }

                if (departementId != null) { // If a Department is selected in faculteFilterComboBox
                    // Filter blocs that have rooms in the selected department
                    blocsToPopulateBloc = blocDAO.getBlocsByDepartement(departementId);
                } else {
                    // If no Department is selected, show all active blocs
                    blocsToPopulateBloc = currentBlocs;
                }

                // --- Now, based on the context, fetch data for the main filterComboBox ---
                switch (selectedContextIndex) {
                    case 0: // Salle
                        List<Room> roomsForFilter; // Declare as List<Room>
                        if (departementId != null && blocId != null) {
                            roomsForFilter = roomDAO.getRoomsByDepartmentAndBlocAndActive(departementId, blocId);
                        } else if (departementId != null) {
                            roomsForFilter = roomDAO.getRoomsByDepartmentAndActive(departementId);
                        } else if (blocId != null) {
                            roomsForFilter = roomDAO.getRoomsByBlocAndActive(blocId);
                        } else {
                            roomsForFilter = roomDAO.getAllActiveRooms();
                        }
                        // Always add the "Online-Room" to the list of rooms if context is "Salle"
                        Room onlineRoom = roomDAO.getOnlineRoom();
                        if (onlineRoom != null && !roomsForFilter.contains(onlineRoom)) {
                            roomsForFilter.add(onlineRoom); // No unchecked cast here
                        }
                        LOGGER.info("Salle filter: Returned " + roomsForFilter.size() + " active rooms.");
                        return roomsForFilter; // Return List<Room>
                    case 1: // Enseignant
                        List<User> teachersForFilter;
                        if (departementId != null && blocId != null) {
                            teachersForFilter = userDAO.getTeachersByDepartmentAndBloc(departementId, blocId);
                        } else if (departementId != null) {
                            teachersForFilter = userDAO.getTeachersByDepartment(departementId);
                        } else if (blocId != null) {
                            teachersForFilter = userDAO.getTeachersByBloc(blocId);
                        } else {
                            teachersForFilter = userDAO.getUsersByRole(User.Role.Enseignant);
                        }
                        LOGGER.info("Enseignant filter: Returned " + teachersForFilter.size() + " active teachers.");
                        return teachersForFilter; // Return List<User>
                    case 2: // Niveau
                        List<Niveau> niveauxForFilter;
                        if (departementId != null && blocId != null) {
                            niveauxForFilter = niveauDAO.getNiveauxByDepartementAndBloc(departementId, blocId);
                        } else if (departementId != null) {
                            niveauxForFilter = niveauDAO.getNiveauxByDepartement(departementId);
                        } else if (blocId != null) {
                            niveauxForFilter = niveauDAO.getNiveauxByBloc(blocId);
                        } else {
                            niveauxForFilter = niveauDAO.getAllActiveNiveaux();
                        }
                        LOGGER.info("Niveau filter: Returned " + niveauxForFilter.size() + " active niveaux.");
                        return niveauxForFilter; // Return List<Niveau>
                    default:
                        return new ArrayList<>();
                }
            }

                        @Override
                        protected void done() {
                            try {
                                List<?> items = get(); // This now holds resultForMainFilter
                                
                                // Populate faculteFilterComboBox using the helper method
                                populateFaculteFilterComboBox(departementsToPopulateFaculte, finalSelectedDepartement);
            
                                // Populate blocFilterComboBox using the helper method
                                populateBlocFilterComboBox(blocsToPopulateBloc, finalSelectedBloc);
                                
                                filterComboBox.setModel(filterModel); // Reset filterComboBox model initially
                                for (Object item : items) { // 'items' now contains resultForMainFilter
                                    filterModel.addElement(item);
                                }
                            } catch (Exception e) {
                    JOptionPane.showMessageDialog(ScheduleViewerPanel.this, "Erreur lors du chargement des filtres: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void showDatePickerDialog() {
        JDialog datePickerDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Sélectionner une Date", Dialog.ModalityType.APPLICATION_MODAL);
        datePickerDialog.setLayout(new MigLayout("fill, insets 15", "[grow]", "[][grow][]"));
        datePickerDialog.setBackground(ThemeConstants.CARD_WHITE);

        // Date selection components
        JPanel datePanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][grow][grow]"));
        datePanel.setOpaque(false);

        SpinnerModel yearModel = new SpinnerNumberModel(selectedDate.getYear(), selectedDate.getYear() - 10, selectedDate.getYear() + 10, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));

        SpinnerModel monthModel = new SpinnerNumberModel(selectedDate.getMonthValue(), 1, 12, 1);
        JSpinner monthSpinner = new JSpinner(monthModel);
        monthSpinner.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));

        SpinnerModel dayModel = new SpinnerNumberModel(selectedDate.getDayOfMonth(), 1, selectedDate.lengthOfMonth(), 1);
        JSpinner daySpinner = new JSpinner(dayModel);
        daySpinner.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));

        datePanel.add(yearSpinner, "growx");
        datePanel.add(monthSpinner, "growx");
        datePanel.add(daySpinner, "growx");

        datePickerDialog.add(datePanel, "wrap, growx");

        // Action buttons
        JButton btnOk = com.gestion.salles.utils.UIUtils.createPrimaryButton("OK");
        btnOk.addActionListener(e -> {
            int year = (int) yearSpinner.getValue();
            int month = (int) monthSpinner.getValue();
            int day = (int) daySpinner.getValue();
            selectedDate = LocalDate.of(year, month, day);
            // Update week boundaries based on the newly selected date
            startDateOfWeek = calculateStartOfWeek(selectedDate);
            endDateOfWeek = startDateOfWeek.plusDays(6); // Friday of the same week
            datePickerButton.setText(selectedDate.toString());
            datePickerDialog.dispose();
            loadScheduleData();
        });

        JButton btnCancel = com.gestion.salles.utils.UIUtils.createSecondaryButton("Annuler");
        btnCancel.addActionListener(e -> datePickerDialog.dispose());

        JPanel buttonPanel = new JPanel(new MigLayout("insets 0, right", "[][]"));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnOk, "h 35!");
        buttonPanel.add(btnCancel, "h 35!");
        datePickerDialog.add(buttonPanel, "growx");

        datePickerDialog.pack();
        datePickerDialog.setLocationRelativeTo(this);
        datePickerDialog.setVisible(true);
    }

    private void loadScheduleData() {
        if (!areAllFiltersSelected()) {
            clearScheduleGrid();
            return;
        }

        // Correctly extract Departement from faculteFilterComboBox (Faculté)
        Object selectedDepartementInFaculteFilter = faculteFilterComboBox.getSelectedItem();
        Departement currentSelectedDepartement = null;
        if (selectedDepartementInFaculteFilter instanceof Departement) {
            currentSelectedDepartement = (Departement) selectedDepartementInFaculteFilter;
        }
        final Integer finalDepartementId = (currentSelectedDepartement != null && currentSelectedDepartement.getId() != 0) ? currentSelectedDepartement.getId() : null;

        // Correctly extract Bloc from blocFilterComboBox (Département)
        Object selectedBlocInBlocFilter = blocFilterComboBox.getSelectedItem();
        Bloc currentSelectedBloc = null;
        if (selectedBlocInBlocFilter instanceof Bloc) {
            currentSelectedBloc = (Bloc) selectedBlocInBlocFilter;
        }
        final Integer finalBlocId = (currentSelectedBloc != null && currentSelectedBloc.getId() != 0) ? currentSelectedBloc.getId() : null;


        Object selectedFilterValue = filterComboBox.getSelectedItem();

        // Gather filter parameters for the main filterComboBox
        Integer currentRoomId = null;
        Integer currentTeacherId = null;
        Integer currentNiveauId = null;
        
        if (selectedFilterValue != null && !"Sélectionner...".equals(selectedFilterValue.toString())) {
            final int finalSelectedIndex = contextFilterComboBox.getSelectedIndex();
            switch (finalSelectedIndex) {
                case 0: // Salle
                    currentRoomId = ((Room) selectedFilterValue).getId();
                    break;
                case 1: // Enseignant
                    currentTeacherId = ((User) selectedFilterValue).getIdUtilisateur();
                    break;
                case 2: // Niveau
                    currentNiveauId = ((Niveau) selectedFilterValue).getId();
                    break;
            }
        }
        
        final Integer finalRoomId = currentRoomId;
        final Integer finalTeacherId = currentTeacherId;
        final Integer finalNiveauId = currentNiveauId;
        
        final LocalDate loadStartDate;
        final LocalDate loadEndDate;

        if (allDatesCheckBox.isSelected()) {
            loadStartDate = startDateOfWeek;
            loadEndDate = endDateOfWeek;
        } else {
            loadStartDate = selectedDate;
            loadEndDate = selectedDate;
        }

        int contextIndex = contextFilterComboBox.getSelectedIndex();
        boolean isNiveauFilter = (contextIndex == 2);
        Integer totalGroups = null;
        if (isNiveauFilter) {
            Object selectedNiveauItem = filterComboBox.getSelectedItem();
            if (selectedNiveauItem instanceof Niveau) {
                totalGroups = ((Niveau) selectedNiveauItem).getNombreGroupes();
            }
        }

        boolean isDepartmentSelected = (finalDepartementId != null);
        boolean isBlocSelected = (finalBlocId != null);
        boolean isSpecificFilterSelected = (finalRoomId != null || finalTeacherId != null || finalNiveauId != null);

        // No early exit. scheduleDAO.getScheduleEntries will handle null parameters correctly.

        // Execute in a SwingWorker to prevent UI freeze
        loadScheduleAsync(
            () -> {
                try {
                    return scheduleDAO.getScheduleEntries(loadStartDate, loadEndDate, finalRoomId, finalTeacherId,
                        finalNiveauId, finalDepartementId, finalBlocId, allDatesCheckBox.isSelected());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            isNiveauFilter,
            totalGroups,
            e -> JOptionPane.showMessageDialog(ScheduleViewerPanel.this,
                "Erreur lors du chargement de l'horaire: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE)
        );
    }

    private boolean areAllFiltersSelected() {
        // Check contextFilterComboBox (Admin panel has no placeholder item)
        if ("Sélectionner...".equals(contextFilterComboBox.getSelectedItem())) {
            return false;
        }

        // Check faculteFilterComboBox (Faculté)
        if (faculteFilterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(faculteFilterComboBox.getSelectedItem())) {
            return false;
        }

        // Check blocFilterComboBox (Département)
        if (blocFilterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(blocFilterComboBox.getSelectedItem())) {
            return false;
        }

        // Check filterComboBox (Sélection)
        if (filterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(filterComboBox.getSelectedItem())) {
            return false;
        }
        return true;
    }

    private Object[][] createEmptyScheduleGridData() {
        return ScheduleTableSupport.createEmptyScheduleGridData(timeSlots);
    }

    private ScheduleTableModel getFilteredTableModel() {
        ScheduleTableModel currentModel = (ScheduleTableModel) scheduleTable.getModel();
        // The first column is always the day name, which we want to keep.
        // We need to check columns from index 1 onwards for actual schedule entries.
        int dataColumnCount = currentModel.getColumnCount() - 1; // Number of columns with schedule data

        List<Object[]> filteredRows = new ArrayList<>();
        // Note: The column names are implicitly handled by the ScheduleTableModel constructor when passing the data.

        for (int i = 0; i < currentModel.getRowCount(); i++) {
            boolean rowHasContent = false;
            // Check cells from column 1 (time slots) to the end
            for (int j = 1; j <= dataColumnCount; j++) {
                if (currentModel.getValueAt(i, j) != null) {
                    rowHasContent = true;
                    break;
                }
            }
            if (rowHasContent) {
                // If row has content, add it to the filtered list
                Object[] rowData = new Object[currentModel.getColumnCount()];
                for (int col = 0; col < currentModel.getColumnCount(); col++) {
                    rowData[col] = currentModel.getValueAt(i, col);
                }
                filteredRows.add(rowData);
            }
        }
        
        // Convert the list of Object[] to the format expected by ScheduleTableModel's constructor
        // If no rows have content, ensure to return a model with appropriate column headers and 0 data rows.
        if (filteredRows.isEmpty()) {
            return new ScheduleTableModel(currentModel.getColumnNames(), new Object[0][currentModel.getColumnCount()]);
        }
        
        Object[][] filteredData = new Object[filteredRows.size()][currentModel.getColumnCount()];

        for(int i = 0; i < filteredRows.size(); i++) {
            filteredData[i] = filteredRows.get(i);
        }

        return new ScheduleTableModel(currentModel.getColumnNames(), filteredData);
    }


    private PDImageXObject loadImage(PDDocument document, String path) throws IOException {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            throw new IOException("Image not found: " + path);
        }
        BufferedImage bImage = ImageIO.read(imageUrl);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Determine image format from URL path
        String formatName = "png"; // Default to png
        String fileName = imageUrl.getFile();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            formatName = fileName.substring(dotIndex + 1);
        }
        ImageIO.write(bImage, formatName, bos);
        return PDImageXObject.createFromByteArray(document, bos.toByteArray(), formatName);
    }
    
    private void exportScheduleToPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(ScheduleUiText.PDF_DIALOG_TITLE);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(ScheduleUiText.PDF_FILTER_LABEL, "pdf"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf";
            }

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); // Landscape
                document.addPage(page);

                PDPageContentStream contents = new PDPageContentStream(document, page);
                
                float margin = 30;
                float pageHeight = page.getMediaBox().getHeight();
                float pageWidth = page.getMediaBox().getWidth();
                float yPosition = pageHeight - margin;

                // --- Draw Header: Logo and Title ---
                float logoHeight = 50;
                PDImageXObject uniLogo = null;
                try {
                    uniLogo = loadImage(document, "/icons/University_of_Laghouat_logo.png");
                } catch (IOException e) {
                    LOGGER.log(java.util.logging.Level.SEVERE, "Error loading logo: " + e.getMessage(), e);
                }

                if (uniLogo != null) {
                    float logoWidth = uniLogo.getWidth() * (logoHeight / uniLogo.getHeight());
                    float logoX = (pageWidth - logoWidth) / 2;
                    contents.drawImage(uniLogo, logoX, yPosition - logoHeight, logoWidth, logoHeight);
                    yPosition -= logoHeight + 20;
                }

                String universityTitle = "Université Amar Telidji de Laghouat";
                String dynamicFilterInfo = getDynamicFilterInfo();

                // Draw University Title
                contents.beginText();
                contents.setFont(PDType1Font.HELVETICA_BOLD, 16);
                float uniTitleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(universityTitle) / 1000 * 16;
                contents.newLineAtOffset((pageWidth - uniTitleWidth) / 2, yPosition);
                contents.showText(universityTitle);
                contents.endText();
                yPosition -= 20;

                // Draw Dynamic Filter Info
                contents.beginText();
                contents.setFont(PDType1Font.HELVETICA, 12);
                float filterInfoWidth = PDType1Font.HELVETICA.getStringWidth(dynamicFilterInfo) / 1000 * 12;
                contents.newLineAtOffset((pageWidth - filterInfoWidth) / 2, yPosition);
                contents.showText(dynamicFilterInfo);
                contents.endText();
                yPosition -= 30;

                // --- Take HD screenshot of the entire table ---
                int tableWidth = scheduleTable.getWidth();
                int tableHeight = scheduleTable.getHeight();
                int headerHeight = scheduleTable.getTableHeader().getHeight();
                int totalHeight = tableHeight + headerHeight;
                
                // Create HD image
                BufferedImage tableImage = new BufferedImage(tableWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D imageGraphics = tableImage.createGraphics();
                
                // Enable HD rendering
                imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                imageGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                
                // White background
                imageGraphics.setColor(ThemeConstants.CARD_WHITE);
                imageGraphics.fillRect(0, 0, tableWidth, totalHeight);
                
                // Paint header
                imageGraphics.translate(0, 0);
                scheduleTable.getTableHeader().paint(imageGraphics);
                
                // Paint table
                imageGraphics.translate(0, headerHeight);
                scheduleTable.paint(imageGraphics);
                
                imageGraphics.dispose();

                // Convert to PDF image
                PDImageXObject tablePdImage = PDImageXObject.createFromByteArray(document, toByteArray(tableImage), "png");

                // --- Scale the screenshot to fit the page ---
                float availableWidth = pageWidth - 2 * margin;
                float availableHeight = yPosition - margin;
                
                float scale = Math.min(availableWidth / tableWidth, availableHeight / totalHeight);
                
                float scaledWidth = tableWidth * scale;
                float scaledHeight = totalHeight * scale;
                
                // Center horizontally
                float imageX = margin + (availableWidth - scaledWidth) / 2;
                float imageY = yPosition - scaledHeight;
                
                contents.drawImage(tablePdImage, imageX, imageY, scaledWidth, scaledHeight);

                contents.close();
                document.save(filePath);
                JOptionPane.showMessageDialog(this, ScheduleUiText.PDF_SUCCESS_MESSAGE, ScheduleUiText.PDF_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, ScheduleUiText.buildPdfErrorMessage(e.getMessage()), ScheduleUiText.PDF_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private String getDynamicFilterInfo() {
        String dynamicFilterInfo = "";
        Object selectedFilterItem = filterComboBox.getSelectedItem();
        String context = (String) contextFilterComboBox.getSelectedItem();

        // Get selected Department (Faculté) and Bloc (Département) from the filter combo boxes
        Object selectedDepartementObj = faculteFilterComboBox.getSelectedItem();
        String selectedDepartementName = null;
        if (selectedDepartementObj instanceof Departement) {
            selectedDepartementName = ((Departement) selectedDepartementObj).getNom();
        }

        Object selectedBlocObj = blocFilterComboBox.getSelectedItem();
        String selectedBlocName = null;
        if (selectedBlocObj instanceof Bloc) {
            selectedBlocName = ((Bloc) selectedBlocObj).getNom();
        }

        if (selectedFilterItem != null && !"Sélectionner...".equals(selectedFilterItem.toString())) {
            switch (context) {
                case "Salle":
                    Room room = (Room) selectedFilterItem;
                    StringBuilder salleInfo = new StringBuilder("Emploi du temps de la salle : " + room.getName());
                    if (selectedDepartementName != null && !"Sélectionner...".equals(selectedDepartementName)) {
                        salleInfo.append(" - Faculté: ").append(selectedDepartementName);
                    }
                    if (selectedBlocName != null && !"Sélectionner...".equals(selectedBlocName)) {
                        salleInfo.append(" - Département: ").append(selectedBlocName);
                    }
                    dynamicFilterInfo = salleInfo.toString();
                    break;
                case "Enseignant":
                    User teacher = (User) selectedFilterItem;
                    dynamicFilterInfo = "Emploi du temps de l'enseignant : " + teacher.getNom() + " " + teacher.getPrenom();
                    break;
                case "Niveau":
                    Niveau niveau = (Niveau) selectedFilterItem;
                    StringBuilder niveauInfo = new StringBuilder("Emploi du temps du niveau : " + niveau.getNom());
                    if (selectedDepartementName != null && !"Sélectionner...".equals(selectedDepartementName)) {
                        niveauInfo.append(" - Faculté: ").append(selectedDepartementName);
                    }
                    if (selectedBlocName != null && !"Sélectionner...".equals(selectedBlocName)) {
                        niveauInfo.append(" - Département: ").append(selectedBlocName);
                    }
                    dynamicFilterInfo = niveauInfo.toString();
                    break;
            }
        } else {
            // If no specific item is selected in the main filterComboBox, but Faculté/Département are.
            StringBuilder generalInfo = new StringBuilder("Emploi du temps");
            if (selectedDepartementName != null && !"Sélectionner...".equals(selectedDepartementName)) {
                generalInfo.append(" - Faculté: ").append(selectedDepartementName);
            }
            if (selectedBlocName != null && !"Sélectionner...".equals(selectedBlocName)) {
                generalInfo.append(" - Département: ").append(selectedBlocName);
            }
            dynamicFilterInfo = generalInfo.toString();
            if ("Emploi du temps".equals(dynamicFilterInfo)) { // If only "Emploi du temps" and no specific filters, keep it simple
                dynamicFilterInfo = "Emploi du temps général";
            }
        }

        if (allDatesCheckBox.isSelected()) {
            return dynamicFilterInfo;
        }
        return dynamicFilterInfo + " - " + selectedDate.toString();
    }

    private byte[] toByteArray(BufferedImage bi) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        return baos.toByteArray();
    }
    protected void adjustRowHeight() {
        super.adjustRowHeight();
    }

    private void printSchedule() {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        if (printerJob.printDialog()) {
            try {
                printerJob.setPrintable(new Printable() {
                    @Override
                    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                        if (pageIndex > 0) {
                            return NO_SUCH_PAGE;
                        }

                        Graphics2D g2d = (Graphics2D) graphics;
                        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        double yPosition = 0;
                        double pageWidth = pageFormat.getImageableWidth();
                        double pageHeight = pageFormat.getImageableHeight();

                        // --- 1. Draw Header ---
                        Image logo = getPrintableLogo();
                        if (logo != null) {
                            int logoHeight = 40;
                            int logoWidth = logo.getWidth(null) * logoHeight / logo.getHeight(null);
                            double logoX = (pageWidth - logoWidth) / 2;
                            g2d.drawImage(logo, (int) logoX, (int) yPosition, logoWidth, logoHeight, null);
                            yPosition += logoHeight + 10;
                        }

                        String universityTitle = "Université Amar Telidji de Laghouat";
                        g2d.setFont(new Font("Helvetica", Font.BOLD, 14));
                        FontMetrics titleMetrics = g2d.getFontMetrics();
                        double titleX = (pageWidth - titleMetrics.stringWidth(universityTitle)) / 2;
                        yPosition += titleMetrics.getAscent();
                        g2d.drawString(universityTitle, (int) titleX, (int) yPosition);

                        String dynamicFilterInfo = getDynamicFilterInfo();
                        g2d.setFont(new Font("Helvetica", Font.PLAIN, 10));
                        FontMetrics filterMetrics = g2d.getFontMetrics();
                        double filterX = (pageWidth - filterMetrics.stringWidth(dynamicFilterInfo)) / 2;
                        yPosition += filterMetrics.getAscent() + 5;
                        g2d.drawString(dynamicFilterInfo, (int) filterX, (int) yPosition);

                        yPosition += 15;

                        // --- 2. Take HD screenshot of the entire table ---
                        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, scheduleTable);
                        
                        // Get the full table size including header
                        int tableWidth = scheduleTable.getWidth();
                        int tableHeight = scheduleTable.getHeight();
                        int headerHeight = scheduleTable.getTableHeader().getHeight();
                        int totalHeight = tableHeight + headerHeight;
                        
                        // Create HD image (2x scale for better quality)
                        BufferedImage tableImage = new BufferedImage(tableWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D imageGraphics = tableImage.createGraphics();
                        
                        // Enable HD rendering
                        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        imageGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        
                        // White background
                        imageGraphics.setColor(ThemeConstants.CARD_WHITE);
                        imageGraphics.fillRect(0, 0, tableWidth, totalHeight);
                        
                        // Paint header
                        imageGraphics.translate(0, 0);
                        scheduleTable.getTableHeader().paint(imageGraphics);
                        
                        // Paint table
                        imageGraphics.translate(0, headerHeight);
                        scheduleTable.paint(imageGraphics);
                        
                        imageGraphics.dispose();

                        // --- 3. Scale the screenshot to fit the page ---
                        double availableHeight = pageHeight - yPosition;
                        double scale = Math.min(pageWidth / tableWidth, availableHeight / totalHeight);
                        
                        int scaledWidth = (int) (tableWidth * scale);
                        int scaledHeight = (int) (totalHeight * scale);
                        
                        // Center horizontally
                        int imageX = (int) ((pageWidth - scaledWidth) / 2);
                        
                        g2d.drawImage(tableImage, imageX, (int) yPosition, scaledWidth, scaledHeight, null);

                        return PAGE_EXISTS;
                    }
                });
                printerJob.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(ScheduleViewerPanel.this, ScheduleUiText.PRINT_ERROR_MESSAGE, ScheduleUiText.PRINT_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private Image getPrintableLogo() {
        URL imageUrl = getClass().getResource("/icons/University_of_Laghouat_logo.png");
        if (imageUrl != null) {
            return new ImageIcon(imageUrl).getImage();
        }
        return null;
    }

    /**
     * Calculates the Saturday of the week for a given date.
     * The week is considered to start on Saturday and end on Friday.
     * @param date The date for which to find the start of the week.
     * @return The LocalDate representing the Saturday of that week.
     */
    private LocalDate calculateStartOfWeek(LocalDate date) {
        // Adjust to the previous Saturday.
        // If the current day is Saturday, then it's already the start.
        // Otherwise, go back until Saturday.
        return date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SATURDAY));
    }

}
