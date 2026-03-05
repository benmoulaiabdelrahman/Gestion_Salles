package com.gestion.salles.views.ChefDepartement;

import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.schedule.ScheduleTableSupport;
import com.gestion.salles.views.shared.schedule.SchedulePanelBase;
import com.gestion.salles.views.shared.schedule.ScheduleUiText;

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
import java.util.logging.Level;
import java.util.stream.Collectors; // Added import
import com.gestion.salles.views.Admin.ScheduleTableModel;
import com.gestion.salles.views.ChefDepartement.ScheduleTableCellRenderer;

/**
 * Panel for Schedule Viewing in the Chef de Département Dashboard.
 * Displays schedules filtered by the HoD's specific department.
 *
 * @author Gemini
 * @version 1.0 - Adapted from Admin ScheduleViewerPanel.
 */
public class ScheduleViewerPanel extends SchedulePanelBase implements RefreshablePanel {

    private static final Logger LOGGER = Logger.getLogger(ScheduleViewerPanel.class.getName());

    public JComboBox<String> getContextFilterComboBox() {
        return contextFilterComboBox;
    }
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
        
        // Listeners to prevent re-entrancy issues (now only for contextFilterComboBox and filterComboBox)
        private ItemListener contextFilterListener; // New listener for contextFilterComboBox
        private ItemListener mainFilterListener; // New listener for filterComboBox

    private ScheduleDAO scheduleDAO;
    private DepartementDAO departementDAO;
    private RoomDAO roomDAO = new RoomDAO();
    private UserDAO userDAO = new UserDAO();
    private NiveauDAO niveauDAO = new NiveauDAO();
    private BlocDAO blocDAO = new BlocDAO(); 

    private List<Departement> allDepartements; // Will only contain HoD's department
    private List<Room> allRooms; // Will only contain HoD's department rooms
    private List<User> allTeachers; // Will only contain HoD's department teachers
    private List<Niveau> allNiveaux; // Will only contain HoD's department niveaux
    private List<Bloc> allBlocs; // Will only contain HoD's bloc

    // Define the session time slots based on 1h 25m duration and 10m breaks
    private DashboardChef parentDashboard; 
    private final User currentUser; // Added current user

    public ScheduleViewerPanel(User currentUser, DashboardChef parentDashboard) { // Modified constructor
        this.currentUser = currentUser;
        this.scheduleDAO = new ScheduleDAO();
        this.departementDAO = new DepartementDAO();
        this.selectedDate = LocalDate.now();
        this.startDateOfWeek = calculateStartOfWeek(selectedDate); // Initialize start of week
        this.endDateOfWeek = startDateOfWeek.plusDays(6);   // Initialize end of week
        this.parentDashboard = parentDashboard; 

        // Initialize listeners once
        // These are simplified as blocFilterComboBox and departementFilterComboBox are disabled
        contextFilterListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFilterComboBox();
                checkAndEnableTousCheckBox();
            }
        };

        mainFilterListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadScheduleData();
                checkAndEnableTousCheckBox();
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
        setLayout(new MigLayout("insets 20, fill", "[grow]", "[][grow, fill]"));
        setBackground(ThemeConstants.CARD_WHITE);
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- A single container for all top filter controls ---
        JPanel controlsContainer = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));
        controlsContainer.setOpaque(false);

        // --- Context Filter Row ---
        JPanel contextFilterRow = new JPanel(new MigLayout("insets 0, fillx", "[pref!][grow, fill][pref!][grow, fill]")); 
        contextFilterRow.setOpaque(false);
        contextFilterRow.add(new JLabel("Filtrer par:"), "align label");
        
        contextFilterComboBox = new JComboBox<>(new String[]{"Sélectionner...", "Salle", "Enseignant", "Niveau"});
        contextFilterComboBox.putClientProperty(FlatClientProperties.STYLE, "arc:10;");
        contextFilterComboBox.addItemListener(contextFilterListener);
        contextFilterRow.add(contextFilterComboBox, "growx, h 35!, sg combox");
        controlsContainer.add(contextFilterRow, "wrap, growx");

        // --- Detailed Filters and Actions Row ---
        // This row will now hold three main components: filterComboBox, a panel for date controls, and a panel for action buttons.
        JPanel detailsFilterRow = new JPanel(new MigLayout("insets 8 0 0 0, fillx", "[pref!][grow, fill][grow, fill][grow, fill]")); 
        detailsFilterRow.setOpaque(false);

        // 1. Selection Filter (Existing filterComboBox)
        detailsFilterRow.add(new JLabel("Sélection:"), "align label, gapleft 10");
        filterComboBox = new JComboBox<>();
        filterComboBox.putClientProperty(FlatClientProperties.STYLE, "arc:10;");
        filterComboBox.addItemListener(mainFilterListener);
        detailsFilterRow.add(filterComboBox, "h 35!"); // Removed sg combox here, as it's not needed for horizontal alignment anymore

        // 2. Date Controls Panel
        JPanel dateControlsPanel = new JPanel(new MigLayout("insets 0, fillx, gap 5", "[grow, fill][pref!]"));
        dateControlsPanel.setOpaque(false);
        datePickerButton = UIUtils.createSecondaryButton(selectedDate.toString());
        datePickerButton.addActionListener(e -> showDatePickerDialog());
        dateControlsPanel.add(datePickerButton, "h 35!, growx");

        allDatesCheckBox = UIUtils.createStyledCheckBox(ScheduleUiText.CHECKBOX_FULL_WEEK);
        allDatesCheckBox.setEnabled(false); // Initially disabled
        allDatesCheckBox.addActionListener(e -> {
            datePickerButton.setEnabled(!allDatesCheckBox.isSelected());
            loadScheduleData();
        });
        dateControlsPanel.add(allDatesCheckBox, "h 35!, aligny center");
        detailsFilterRow.add(dateControlsPanel, "gapleft 10, growx");

        // 3. Action Buttons Panel
        JPanel actionButtonsPanel = new JPanel(new MigLayout("insets 0, fillx, gap 5", "[grow, fill][grow, fill]"));
        actionButtonsPanel.setOpaque(false);
        JButton savePdfButton = UIUtils.createSecondaryButton(ScheduleUiText.BUTTON_EXPORT_PDF);
        savePdfButton.addActionListener(e -> exportScheduleToPdf());
        actionButtonsPanel.add(savePdfButton, "h 35!, growx");

        JButton printButton = UIUtils.createSecondaryButton(ScheduleUiText.BUTTON_PRINT);
        printButton.addActionListener(e -> printSchedule());
        actionButtonsPanel.add(printButton, "h 35!, growx");
        detailsFilterRow.add(actionButtonsPanel, "gapleft 10, growx");
        
        controlsContainer.add(detailsFilterRow, "growx");

        // Add the single controls container to the main panel
        add(controlsContainer, "wrap, growx");

        // --- Schedule Table (The second row, set to grow and fill) ---
        JScrollPane scrollPane = createScheduleTable(new ScheduleTableCellRenderer(this));
        
        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                adjustRowHeight();
            }
        });
        
        // Add scroll pane to the second row, ensuring it grows
        add(scrollPane, "grow, push"); 

        updateFilterComboBox(); 
    }

    private void checkAndEnableTousCheckBox() {
        // Since bloc and departement filters are disabled and pre-selected, only check filterComboBox
        boolean allFiltersSelected = (filterComboBox.getSelectedIndex() != 0 && !"Sélectionner...".equals(filterComboBox.getSelectedItem()));

        if (allFiltersSelected) {
            allDatesCheckBox.setEnabled(true);
        } else {
            allDatesCheckBox.setEnabled(false);
            if (allDatesCheckBox.isSelected()) {
                allDatesCheckBox.setSelected(false);
                datePickerButton.setEnabled(true); 
                loadScheduleData(); 
            }
        }
    }



    private void loadInitialFilterData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Only load the HoD's specific department and bloc
                Integer departmentId = currentUser.getIdDepartement();
                Integer blocId = currentUser.getIdBloc(); 
                
                if (departmentId != null) {
                    allDepartements = new ArrayList<>();
                    Departement hodDept = departementDAO.getDepartementById(departmentId);
                    if (hodDept != null) allDepartements.add(hodDept);
                } else {
                    allDepartements = new ArrayList<>();
                }

                if (blocId != null) {
                    allBlocs = new ArrayList<>();
                    Bloc hodBloc = blocDAO.getBlocById(blocId);
                    if (hodBloc != null) allBlocs.add(hodBloc);

                    // Populate allRooms, allTeachers, allNiveaux for the HoD's bloc
                    allRooms = roomDAO.getRoomsByBloc(blocId);
                    allTeachers = userDAO.getTeachersByBloc(blocId);
                    allNiveaux = niveauDAO.getNiveauxByBloc(blocId);
                } else {
                    allBlocs = new ArrayList<>();
                    allRooms = new ArrayList<>();
                    allTeachers = new ArrayList<>();
                    allNiveaux = new ArrayList<>();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); 
                    
                    updateFilterComboBox(); 
                    checkAndEnableTousCheckBox(); 
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
        // In the ChefDepartement panel, contextFilterComboBox is {"Sélectionner...", "Salle", "Enseignant", "Niveau"}
        // So Niveau is index 3
        boolean isNiveauFilter = (contextIndex == 3);
        
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
        filterModel.addElement("Sélectionner..."); 
        
        // Department and Bloc are fixed from currentUser
        final Integer departementId = currentUser.getIdDepartement();
        final Integer blocId = currentUser.getIdBloc();

        SwingWorker<List<?>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<?> doInBackground() throws Exception {
                List<?> result = new ArrayList<>();
                                        
                switch (selectedContextIndex) {
                    case 0: // "Sélectionner..."
                        result = new ArrayList<>();
                        break;
                    case 1: // Salle
                        result = allRooms.stream()
                                .filter(r -> r.isActif())
                                .collect(Collectors.toList());
                        LOGGER.info("Salle filter: Returned " + result.size() + " active rooms for department.");
                        break;
                    case 2: // Enseignant
                        result = allTeachers.stream()
                                .filter(u -> u.isActif())
                                .collect(Collectors.toList());
                        LOGGER.info("Enseignant filter: Returned " + result.size() + " active teachers for department.");
                        break;
                    case 3: // Niveau
                        result = allNiveaux.stream()
                                .filter(n -> n.isActif())
                                .collect(Collectors.toList());
                        LOGGER.info("Niveau filter: Returned " + result.size() + " active niveaux for department.");
                        break;
                    default:
                        result = new ArrayList<>();
                        break;
                }
                return result; 
            }

            @Override
            protected void done() {
                try {
                    List<?> items = get();
                    
                    filterComboBox.removeItemListener(mainFilterListener); // Remove listener before setting model
                    filterComboBox.setModel(filterModel); 
                    for (Object item : items) {
                        filterModel.addElement(item);
                    }
                    filterComboBox.addItemListener(mainFilterListener); // Re-add listener

                    loadScheduleData(); 
                    checkAndEnableTousCheckBox(); 
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
        yearSpinner.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        SpinnerModel monthModel = new SpinnerNumberModel(selectedDate.getMonthValue(), 1, 12, 1);
        JSpinner monthSpinner = new JSpinner(monthModel);
        monthSpinner.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        SpinnerModel dayModel = new SpinnerNumberModel(selectedDate.getDayOfMonth(), 1, selectedDate.lengthOfMonth(), 1);
        JSpinner daySpinner = new JSpinner(dayModel);
        daySpinner.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        datePanel.add(yearSpinner, "growx");
        datePanel.add(monthSpinner, "growx");
        datePanel.add(daySpinner, "growx");

        datePickerDialog.add(datePanel, "wrap, growx");

        // Action buttons
        JButton btnOk = new JButton("OK");
        btnOk.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:0;" +
                "focusWidth:0;" +
                "innerFocusWidth:0;" +
                "background:" + com.gestion.salles.utils.UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "foreground:" + com.gestion.salles.utils.UIUtils.colorToHex(Color.WHITE));
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

        JButton btnCancel = new JButton("Annuler");
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "focusWidth:0;" +
                "innerFocusWidth:0;" +
                "borderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(ThemeConstants.DEFAULT_BORDER) + ";" +
                "focusedBorderColor:" + com.gestion.salles.utils.UIUtils.colorToHex(ThemeConstants.FOCUS_BORDER));
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

        // Department and Bloc IDs are taken directly from currentUser
        final Integer finalDepartementId = currentUser.getIdDepartement();
        final Integer finalBlocId = currentUser.getIdBloc();


        Object selectedFilterValue = filterComboBox.getSelectedItem();

        // Gather filter parameters for the main filterComboBox
        Integer currentRoomId = null;
        Integer currentTeacherId = null;
        Integer currentNiveauId = null;
        
        if (selectedFilterValue != null && !"Sélectionner...".equals(selectedFilterValue.toString())) {
            final int finalSelectedIndex = contextFilterComboBox.getSelectedIndex();
            switch (finalSelectedIndex) {
                case 1: // Salle
                    currentRoomId = ((Room) selectedFilterValue).getId();
                    break;
                case 2: // Enseignant
                    currentTeacherId = ((User) selectedFilterValue).getIdUtilisateur();
                    break;
                case 3: // Niveau
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
        boolean isNiveauFilter = (contextIndex == 3);
        Integer totalGroups = null;
        if (isNiveauFilter) {
            Object selectedNiveauItem = filterComboBox.getSelectedItem();
            if (selectedNiveauItem instanceof Niveau) {
                totalGroups = ((Niveau) selectedNiveauItem).getNombreGroupes();
            }
        }

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
        // Only check contextFilterComboBox and filterComboBox, as bloc/departement are fixed
        if (contextFilterComboBox.getSelectedIndex() == 0 || "Sélectionner...".equals(contextFilterComboBox.getSelectedItem())) {
            return false;
        }

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
        String formatName = "png"; 
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
                    System.err.println("Error loading logo: " + e.getMessage());
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

                // --- Capture the currently displayed schedule table (same approach as Admin/Enseignant) ---
                int tableWidth = scheduleTable.getWidth();
                int tableHeight = scheduleTable.getHeight();
                int headerHeight = scheduleTable.getTableHeader().getHeight();
                int totalHeight = tableHeight + headerHeight;

                BufferedImage tableImage = new BufferedImage(tableWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D imageGraphics = tableImage.createGraphics();
                
                // Render header
                imageGraphics.translate(0, 0);
                scheduleTable.getTableHeader().paint(imageGraphics);
                
                // Render table
                imageGraphics.translate(0, headerHeight);
                scheduleTable.paint(imageGraphics);
                imageGraphics.dispose();


                PDImageXObject tablePdImage = PDImageXObject.createFromByteArray(document, toByteArray(tableImage), "png");

                // --- Calculate Image Dimensions ---
                float availableWidth = pageWidth - 2 * margin;
                float availableHeight = yPosition - margin;
                
                float imageAspectRatio = (float) tableWidth / (float) totalHeight;
                
                float finalWidth, finalHeight;
                finalWidth = availableWidth;
                finalHeight = finalWidth / imageAspectRatio;

                if (finalHeight > availableHeight) {
                    finalHeight = availableHeight;
                    finalWidth = finalHeight * imageAspectRatio;
                }

                float imageX = margin + (availableWidth - finalWidth) / 2;
                float imageY = margin + (availableHeight - finalHeight) / 2;

                contents.drawImage(tablePdImage, imageX, imageY, finalWidth, finalHeight);

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

        // Department and Bloc are fixed from currentUser
        String selectedDepartementName = currentUser.getNomDepartement();
        String selectedBlocName = currentUser.getNomBloc();

        if (selectedFilterItem != null && !"Sélectionner...".equals(selectedFilterItem.toString())) {
            switch (contextFilterComboBox.getSelectedIndex()) { // Use index for context
                case 1: // Salle
                    Room room = (Room) selectedFilterItem;
                    StringBuilder salleInfo = new StringBuilder("Emploi du temps de la salle : " + room.getName());
                    if (selectedDepartementName != null) {
                        salleInfo.append(" - Département: ").append(selectedDepartementName);
                    }
                    if (selectedBlocName != null) {
                        salleInfo.append(" - Faculté: ").append(selectedBlocName);
                    }
                    dynamicFilterInfo = salleInfo.toString();
                    break;
                case 2: // Enseignant
                    User teacher = (User) selectedFilterItem;
                    StringBuilder teacherInfo = new StringBuilder("Emploi du temps de l'enseignant : " + teacher.getNom() + " " + teacher.getPrenom());
                    if (selectedDepartementName != null) {
                        teacherInfo.append(" - Département: ").append(selectedDepartementName);
                    }
                    if (selectedBlocName != null) {
                        teacherInfo.append(" - Faculté: ").append(selectedBlocName);
                    }
                    dynamicFilterInfo = teacherInfo.toString();
                    break;
                case 3: // Niveau
                    Niveau niveau = (Niveau) selectedFilterItem;
                    StringBuilder niveauInfo = new StringBuilder("Emploi du temps du niveau : " + niveau.getNom());
                    if (selectedDepartementName != null) {
                        niveauInfo.append(" - Département: ").append(selectedDepartementName);
                    }
                    if (selectedBlocName != null) {
                        niveauInfo.append(" - Faculté: ").append(selectedBlocName);
                    }
                    dynamicFilterInfo = niveauInfo.toString();
                    break;
                default: // Case 0: "Sélectionner..." or other unexpected context
                    StringBuilder generalInfo = new StringBuilder("Emploi du temps");
                    if (selectedDepartementName != null) {
                        generalInfo.append(" - Département: ").append(selectedDepartementName);
                    }
                    if (selectedBlocName != null) {
                        generalInfo.append(" - Faculté: ").append(selectedBlocName);
                    }
                    dynamicFilterInfo = generalInfo.toString();
                    if ("Emploi du temps".equals(dynamicFilterInfo)) { 
                        dynamicFilterInfo = "Emploi du temps général du Département";
                    }
                    break;
            }
        } else {
            // If no specific item is selected in the main filterComboBox.
            StringBuilder generalInfo = new StringBuilder("Emploi du temps");
            if (selectedDepartementName != null) {
                generalInfo.append(" - Département: ").append(selectedDepartementName);
            }
            if (selectedBlocName != null) {
                generalInfo.append(" - Faculté: ").append(selectedBlocName);
            }
            dynamicFilterInfo = generalInfo.toString();
            if ("Emploi du temps".equals(dynamicFilterInfo)) { 
                dynamicFilterInfo = "Emploi du temps général du Département";
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
                        yPosition += filterMetrics.getAscent();
                        g2d.drawString(dynamicFilterInfo, (int) filterX, (int) yPosition);

                        yPosition += 15; // Space between header and table image

                // --- 2. Create an image from the currently displayed table ---
                int tableWidth = scheduleTable.getWidth();
                int tableHeight = scheduleTable.getHeight();
                int headerHeight = scheduleTable.getTableHeader().getHeight();
                int totalHeight = tableHeight + headerHeight;

                BufferedImage tableImage = new BufferedImage(tableWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D imageGraphics = tableImage.createGraphics();

                // Print the header at the top of the image
                imageGraphics.translate(0, 0);
                scheduleTable.getTableHeader().paint(imageGraphics);

                // Print the table right below the header
                imageGraphics.translate(0, headerHeight);
                scheduleTable.paint(imageGraphics);
                        
                        imageGraphics.dispose();


                        // --- 3. Draw the Image onto the Page (Scaled and Centered) ---
                        double availablePrintHeight = pageHeight - yPosition;
                        if (availablePrintHeight <= 0) return NO_SUCH_PAGE;

                        double imgWidth = tableImage.getWidth();
                        double imgHeight = tableImage.getHeight();
                        
                        double scale = 1.0;
                        if (imgWidth > pageWidth) {
                            scale = pageWidth / imgWidth;
                        }
                        
                        double scaledWidth = imgWidth * scale;
                        double scaledHeight = imgHeight * scale;
                        
                        if (scaledHeight > availablePrintHeight) {
                            scale = availablePrintHeight / imgHeight;
                            scaledWidth = imgWidth * scale;
                            scaledHeight = imgHeight * scale;
                        }


                        double imageX = (pageWidth - scaledWidth) / 2;
                        
                        g2d.drawImage(tableImage, (int) imageX, (int) yPosition, (int) scaledWidth, (int) scaledHeight, null);

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
        LocalDate current = date;
        while (current.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
            current = current.minusDays(1);
        }
        return current;
    }
}
