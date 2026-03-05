package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.*;
import com.gestion.salles.models.*;
import com.gestion.salles.services.ConflictDetectionService;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.DialogCallback;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.management.FormValidationUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ReservationDialog extends JDialog {

    private JComboBox<Departement> cmbDepartement;
    private JComboBox<Bloc> cmbBloc;
    private JComboBox<Niveau> cmbNiveau;
    private JComboBox<User> cmbEnseignant;
    private JComboBox<String> cmbRoomType;
    private JComboBox<Room> cmbRoom;
    private JLabel lblEquipment;
    private JComboBox<ActivityType> cmbActivityType;
    private JCheckBox chkIsOnline;

    private JCheckBox chkIsRecurring;
    private JLabel lblGroupNumber;
    private JComboBox<Integer> cmbGroupNumber;
    private JLabel lblRecurrenceStartDate;
    private com.toedter.calendar.JDateChooser dateChooserDebutRecurrence;
    private JLabel lblRecurrenceEndDate;
    private com.toedter.calendar.JDateChooser dateChooserFinRecurrence;
    private JLabel lblDayOfWeek;
    private JComboBox<Reservation.DayOfWeek> cmbDayOfWeek;

    private JSpinner spinnerDay;
    private JSpinner spinnerMonth;
    private JSpinner spinnerYear;

    private JComboBox<String> cmbSession;
    private JTextField txtTitre;
    private JTextArea txtDescription;
    private JScrollPane scrollDescription;
    private JComboBox<Reservation.ReservationStatus> cmbStatut;
    private JTextArea txtObservations;
    private JScrollPane scrollObservations;

    private JLabel lblError;
    private JPanel formContentPanel;
    private Frame ownerFrame;
    private JButton btnSave;
    private JButton btnCancel;
    
    // private static final int ONLINE_ROOM_ID = 244; // Removed hardcoded ID
    private int onlineRoomId = 0; // Dynamically set
    private static final int BASE_HEIGHT = 590;
    private static final int RECURRING_HEIGHT_ADD = 70;
    private static final int GROUP_HEIGHT_ADD = 35;

    private final ReservationDAO reservationDAO;
    private final ConflictDetectionService conflictDetectionService;

    private final DepartementDAO departementDAO;
    private final BlocDAO blocDAO;
    private final NiveauDAO niveauDAO;
    private final UserDAO userDAO;
    private final RoomDAO roomDAO;
    private final ActivityTypeDAO activityTypeDAO;
    private final int currentUserId;

    private Reservation currentReservation;
    private final boolean isEditMode;
    private final DialogCallback callback;
    private boolean pendingEditPopulate;
    private Integer pendingGroupNumber;

    private List<Departement> allDepartments;
    private List<Bloc> allBlocs;
    private List<Niveau> allNiveaux;
    private List<User> allEnseignants;
    private List<Room> allRooms;
    private List<ActivityType> allActivityTypes;
    private List<String> allRoomTypes; // New field to store fetched room types

    // Inner class to hold results from background loading
    private static class LoadDataResult {
        List<Departement> departments;
        List<Bloc> blocs;
        List<Niveau> niveaux;
        List<User> enseignants;
        List<Room> rooms;
        List<ActivityType> activityTypes;
        List<String> roomTypes;
        int onlineRoomId;

        public LoadDataResult(List<Departement> departments, List<Bloc> blocs, List<Niveau> niveaux,
                              List<User> enseignants, List<Room> rooms, List<ActivityType> activityTypes,
                              List<String> roomTypes, int onlineRoomId) {
            this.departments = departments;
            this.blocs = blocs;
            this.niveaux = niveaux;
            this.enseignants = enseignants;
            this.rooms = rooms;
            this.activityTypes = activityTypes;
            this.roomTypes = roomTypes;
            this.onlineRoomId = onlineRoomId;
        }
    }

    private static class FilterBlocsNiveauxResult {
        List<Bloc> filteredBlocs;
        List<Niveau> filteredNiveaux;

        public FilterBlocsNiveauxResult(List<Bloc> filteredBlocs, List<Niveau> filteredNiveaux) {
            this.filteredBlocs = filteredBlocs;
            this.filteredNiveaux = filteredNiveaux;
        }
    }


    public ReservationDialog(Frame owner, ReservationDAO reservationDAO,
                             DepartementDAO departementDAO, BlocDAO blocDAO, NiveauDAO niveauDAO,
                             UserDAO userDAO, RoomDAO roomDAO, ActivityTypeDAO activityTypeDAO,
                             Reservation reservation, DialogCallback callback, int currentUserId) {
        super(owner, true);
        if (reservationDAO == null) {
            throw new IllegalArgumentException("ReservationDAO cannot be null.");
        }
        this.reservationDAO = reservationDAO;
        this.conflictDetectionService = new ConflictDetectionService();

        this.departementDAO = departementDAO;
        this.blocDAO = blocDAO;
        this.niveauDAO = niveauDAO;
        this.userDAO = userDAO;
        this.roomDAO = roomDAO;
        this.activityTypeDAO = activityTypeDAO;
        this.currentUserId = currentUserId;
        this.ownerFrame = owner;

        this.currentReservation = reservation;
        this.isEditMode = (reservation != null);
        this.callback = callback;
        
        this.allRoomTypes = new ArrayList<>(); // Initialize the new list
        
        initComponents();
        loadComboBoxData();

        if (isEditMode) {
            setTitle("Modifier la Réservation");
            populateFields();
        } else {
            setTitle("Ajouter une Nouvelle Réservation");
        }

        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        setLayout(new BorderLayout(0, 0));

        // Updated layout with better spacing and structure
        formContentPanel = new JPanel(new MigLayout(
            "wrap 4, fillx, insets 20 20 15 20", 
            "[100!, right][grow, fill][100!, right][grow, fill]", 
            "[]8[]"
        ));
        formContentPanel.setBackground(ThemeConstants.CARD_WHITE);

        // Initialize all components
        cmbDepartement = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbBloc = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbNiveau = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbEnseignant = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbRoom = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbActivityType = UIUtils.createStyledComboBox(new JComboBox<>());
        chkIsOnline = new JCheckBox("Réservation en ligne");
        chkIsOnline.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkIsRecurring = new JCheckBox("Réservation récurrente");
        chkIsRecurring.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        
        lblGroupNumber = new JLabel("Groupe:");
        cmbGroupNumber = UIUtils.createStyledComboBox(new JComboBox<>());

        lblRecurrenceStartDate = new JLabel("Début récurrence:");
        dateChooserDebutRecurrence = new com.toedter.calendar.JDateChooser();
        dateChooserDebutRecurrence.setDateFormatString("yyyy-MM-dd");
        // Get the internal text field of JDateChooser and apply the style
        if (dateChooserDebutRecurrence.getDateEditor().getUiComponent() instanceof JComponent) {
            JComponent editorComponent = (JComponent) dateChooserDebutRecurrence.getDateEditor().getUiComponent();
            editorComponent.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                    "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        } else {
            // Fallback for cases where uiComponent is not a JComponent
            dateChooserDebutRecurrence.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                    "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        }

        lblRecurrenceEndDate = new JLabel("Fin récurrence:");
        dateChooserFinRecurrence = new com.toedter.calendar.JDateChooser();
        dateChooserFinRecurrence.setDateFormatString("yyyy-MM-dd");
        // Get the internal text field of JDateChooser and apply the style
        if (dateChooserFinRecurrence.getDateEditor().getUiComponent() instanceof JComponent) {
            JComponent editorComponent = (JComponent) dateChooserFinRecurrence.getDateEditor().getUiComponent();
            editorComponent.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                    "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        } else {
            // Fallback for cases where uiComponent is not a JComponent
            dateChooserFinRecurrence.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                    "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        }

        lblDayOfWeek = new JLabel("Jour semaine:");
        DefaultComboBoxModel<Reservation.DayOfWeek> dayModel = new DefaultComboBoxModel<>();
        dayModel.addElement(null);
        dayModel.addElement(Reservation.DayOfWeek.SATURDAY);
        dayModel.addElement(Reservation.DayOfWeek.SUNDAY);
        dayModel.addElement(Reservation.DayOfWeek.MONDAY);
        dayModel.addElement(Reservation.DayOfWeek.TUESDAY);
        dayModel.addElement(Reservation.DayOfWeek.WEDNESDAY);
        dayModel.addElement(Reservation.DayOfWeek.THURSDAY);
        cmbDayOfWeek = UIUtils.createStyledComboBox(new JComboBox<>(dayModel));
        cmbDayOfWeek.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Sélectioner...");
                } else if (value instanceof Reservation.DayOfWeek) {
                    setText(((Reservation.DayOfWeek) value).getDisplayName());
                }
                return this;
            }
        });

        spinnerDay = new JSpinner(new SpinnerNumberModel(LocalDate.now().getDayOfMonth(), 1, 31, 1));
        spinnerDay.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        spinnerMonth = new JSpinner(new SpinnerListModel(java.time.Month.values()));
        spinnerMonth.setValue(LocalDate.now().getMonth());
        spinnerMonth.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        spinnerYear = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 1900, 2100, 1));
        spinnerYear.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));

        spinnerMonth.addChangeListener(e -> updateDaySpinnerModel());
        spinnerYear.addChangeListener(e -> updateDaySpinnerModel());
        updateDaySpinnerModel();

        String[] sessions = {"8:00 - 9:25", "9:35 - 11:00", "11:10 - 12:35", "12:45 - 14:10", "14:20 - 15:45"};
        DefaultComboBoxModel<String> sessionModel = new DefaultComboBoxModel<>();
        sessionModel.addElement("Sélectioner...");
        for (String session : sessions) {
            sessionModel.addElement(session);
        }
        cmbSession = UIUtils.createStyledComboBox(new JComboBox<>(sessionModel));

        txtTitre = UIUtils.createStyledTextField("Titre de l'activité");
        txtDescription = UIUtils.createStyledTextArea("Description de l'activité", 3, 20);
        scrollDescription = new JScrollPane(txtDescription);
        scrollDescription.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "borderColor:#D1D5DB;" +
                "focusedBorderColor:#0F6B3F;");

        DefaultComboBoxModel<Reservation.ReservationStatus> statusModel = new DefaultComboBoxModel<>();
        statusModel.addElement(null);
        for (Reservation.ReservationStatus status : Reservation.ReservationStatus.values()) {
            statusModel.addElement(status);
        }
        cmbStatut = UIUtils.createStyledComboBox(new JComboBox<>(statusModel));
        cmbStatut.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Sélectioner...");
                } else if (value instanceof Reservation.ReservationStatus) {
                    setText(((Reservation.ReservationStatus) value).getDisplayName());
                }
                return this;
            }
        });
        
        txtObservations = UIUtils.createStyledTextArea("Observations", 3, 20);
        scrollObservations = new JScrollPane(txtObservations);
        scrollObservations.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "borderWidth:1;" +
                "borderColor:#D1D5DB;" +
                "focusedBorderColor:#0F6B3F;");

        cmbRoomType = UIUtils.createStyledComboBox(new JComboBox<>());
        lblEquipment = new JLabel(" ");

        lblError = new JLabel("");
        lblError.setForeground(ThemeConstants.ERROR_RED);
        lblError.setFont(lblError.getFont().deriveFont(Font.BOLD, 12f));
        lblError.setVisible(false);

        // ============================================================
        // OPTIMIZED FORM LAYOUT - Custom placement as requested
        // ============================================================
        
        // ROW 1: Faculté / Département
        addFormRow("Faculté:", cmbDepartement, "Département:", cmbBloc);
        
        // ROW 2: Enseignant / Niveau
        addFormRow("Enseignant:", cmbEnseignant, "Niveau:", cmbNiveau);
        
        // ROW 3: Titre / Type d'activité
        addFormRow("Titre:", txtTitre, "Type d'activité:", cmbActivityType);
        
        // ROW 3b: Group field (shown when activity type requires it) - appears right after titre/type
        formContentPanel.add(lblGroupNumber, "hidemode 2, right");
        formContentPanel.add(cmbGroupNumber, "hidemode 2, h 35!, span 3, grow, wrap");
        
        // ROW 4: Checkboxes (aligned to left, spanning 2 columns each)
        formContentPanel.add(chkIsOnline, "span 2");
        formContentPanel.add(chkIsRecurring, "span 2, wrap");
        
        // Dynamic recurring fields (shown when recurring is checked)
        formContentPanel.add(lblRecurrenceStartDate, "hidemode 2, right");
        formContentPanel.add(dateChooserDebutRecurrence, "hidemode 2, h 35!, grow");
        formContentPanel.add(lblRecurrenceEndDate, "hidemode 2, right");
        formContentPanel.add(dateChooserFinRecurrence, "hidemode 2, h 35!, grow, wrap");
        
        formContentPanel.add(lblDayOfWeek, "hidemode 2, right");
        formContentPanel.add(cmbDayOfWeek, "hidemode 2, h 35!, span 3, grow, wrap");
        
        // ROW 5: Date / Session
        JPanel datePanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][grow][grow]"));
        datePanel.setBackground(ThemeConstants.CARD_WHITE);
        datePanel.add(spinnerDay, "h 35!");
        datePanel.add(spinnerMonth, "h 35!");
        datePanel.add(spinnerYear, "h 35!");
        addFormRow("Date:", datePanel, "Session:", cmbSession);
        
        // ROW 6: Type de salle / Salle
        addFormRow("Type de salle:", cmbRoomType, "Salle:", cmbRoom);
        
        // ROW 7: Équipements / Statut
        addFormRow("Équipements:", lblEquipment, "Statut:", cmbStatut);
        
        // ROW 8: Description (full width)
        formContentPanel.add(new JLabel("Description:"), "right");
        formContentPanel.add(scrollDescription, "span 3, grow, h 70!, gaptop 3");
        
        // ROW 9: Observations (full width)
        formContentPanel.add(new JLabel("Observations:"), "right");
        formContentPanel.add(scrollObservations, "span 3, grow, h 70!, gaptop 3");

        // Error message and buttons
        btnSave = UIUtils.createPrimaryButton("Enregistrer");
        btnSave.addActionListener(e -> onSave());
        
        btnCancel = UIUtils.createSecondaryButton("Annuler");
        btnCancel.addActionListener(e -> dispose());
        
        JPanel buttonAndErrorPanel = new JPanel(new MigLayout("insets 0, fillx", "[][][]push[]"));
        buttonAndErrorPanel.setBackground(ThemeConstants.CARD_WHITE);
        buttonAndErrorPanel.add(btnSave);
        buttonAndErrorPanel.add(btnCancel, "gap 5");
        buttonAndErrorPanel.add(lblError, "pushx, alignx right, h 20!");
        
        formContentPanel.add(buttonAndErrorPanel, "span 4, grow, gaptop 15");

        JScrollPane formScrollPane = new JScrollPane(formContentPanel);
        formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(formScrollPane, BorderLayout.CENTER);

        // Event listeners
        cmbDepartement.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                filterBlocsNiveauxAndEnseignants();
            }
        });

        cmbBloc.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateNiveauAndRoomFilters();
            }
        });

        cmbRoomType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                filterRooms();
            }
        });

        cmbRoom.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateEquipmentLabel();
            }
        });

        cmbNiveau.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateGroupVisibility(null);
            }
        });

        chkIsRecurring.addActionListener(e -> updateRecurrenceVisibility());
        cmbActivityType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateGroupVisibility(null);
            }
        });
        
        chkIsOnline.addActionListener(e -> toggleRoomFields());
        
        updateRecurrenceVisibility();
        updateGroupVisibility(null);
        toggleRoomFields();

        setSize(950, BASE_HEIGHT); // Ensure a consistent initial size
    }

    /**
     * Helper method to add a form row with two label-component pairs
     */
    private void addFormRow(String label1, JComponent component1, String label2, JComponent component2) {
        JLabel lbl1 = new JLabel(label1);
        JLabel lbl2 = new JLabel(label2);
        
        formContentPanel.add(lbl1, "right");
        formContentPanel.add(component1, "grow, h 35!");
        formContentPanel.add(lbl2, "right");
        formContentPanel.add(component2, "grow, h 35!, wrap");
    }

    private void toggleRoomFields() {
        boolean isOnline = chkIsOnline.isSelected();
        cmbRoomType.setEnabled(!isOnline);
        cmbRoom.setEnabled(!isOnline);
        lblEquipment.setEnabled(!isOnline);

        if (isOnline) {
            cmbRoom.setSelectedItem(null);
            lblEquipment.setText("En ligne");
        } else {
            filterRooms();
            updateEquipmentLabel();
        }
    }

    private void updateEquipmentLabel() {
        if (chkIsOnline.isSelected()) {
            lblEquipment.setText("En ligne");
            return;
        }

        Room selectedRoom = (Room) cmbRoom.getSelectedItem();

        if (selectedRoom != null && selectedRoom.getId() != 0) {
            String equipmentJson = selectedRoom.getEquipment();

            if (equipmentJson != null && !equipmentJson.trim().isEmpty()) {
                Gson gson = new Gson();
                java.lang.reflect.Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> equipmentList = gson.fromJson(equipmentJson, listType);

                if (equipmentList != null && !equipmentList.isEmpty()) {
                    lblEquipment.setText(String.join(", ", equipmentList));
                } else {
                    lblEquipment.setText(" ");
                }
            } else {
                lblEquipment.setText(" ");
            }
        } else {
            lblEquipment.setText(" ");
        }
    }

    private void loadComboBoxData() {
        SwingWorker<LoadDataResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LoadDataResult doInBackground() throws Exception {
                List<Departement> departments = departementDAO.getAllActiveDepartements();
                List<Bloc> blocs = blocDAO.getAllActiveBlocs();
                List<Niveau> niveaux = niveauDAO.getAllActiveNiveaux();
                List<User> enseignants = userDAO.getUsersByRole(User.Role.Enseignant); 
                List<Room> rooms = roomDAO.getAllActiveRooms();
                List<ActivityType> activityTypes = activityTypeDAO.getAllActivityTypes();
                List<String> roomTypes = roomDAO.getRoomTypes();
                
                // Ensure the online room exists and get its ID
                int fetchedOnlineRoomId = roomDAO.ensureOnlineRoomExists();

                return new LoadDataResult(departments, blocs, niveaux, enseignants, rooms, activityTypes, roomTypes, fetchedOnlineRoomId);
            }

            @Override
            protected void done() {
                try {
                    LoadDataResult result = get(); // Ensure any exceptions from doInBackground are re-thrown

                    // Assign fetched data to member fields
                    ReservationDialog.this.allDepartments = result.departments;
                    ReservationDialog.this.allBlocs = result.blocs;
                    ReservationDialog.this.allNiveaux = result.niveaux;
                    ReservationDialog.this.allEnseignants = result.enseignants;
                    ReservationDialog.this.allRooms = result.rooms;
                    ReservationDialog.this.allActivityTypes = result.activityTypes;
                    ReservationDialog.this.allRoomTypes = result.roomTypes;
                    ReservationDialog.this.onlineRoomId = result.onlineRoomId; // Set the dynamic online room ID

                    // Populate combo boxes
                    Departement deptPlaceholder = new Departement("Sélectioner...", 0);
                    updateComboBoxItems(cmbDepartement, ReservationDialog.this.allDepartments, deptPlaceholder, null);

                    String roomTypePlaceholder = "Sélectioner...";
                    updateComboBoxItems(cmbRoomType, ReservationDialog.this.allRoomTypes, roomTypePlaceholder, null);

                    ActivityType activityTypePlaceholder = new ActivityType("Sélectioner...", 0);
                    updateComboBoxItems(cmbActivityType, ReservationDialog.this.allActivityTypes, activityTypePlaceholder, null);
                    
                    if (isEditMode && currentReservation != null) {
                        pendingEditPopulate = true;
                        for (int i = 0; i < cmbDepartement.getItemCount(); i++) {
                            Departement dept = cmbDepartement.getItemAt(i);
                            if (dept != null && dept.getId() == currentReservation.getIdDepartement()) {
                                cmbDepartement.setSelectedItem(dept);
                                break;
                            }
                        }
                    }

                    filterBlocsNiveauxAndEnseignants();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ReservationDialog.this,
                            "Erreur lors du chargement des données des combobox: " + e.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void filterBlocsNiveauxAndEnseignants() {
        Departement selectedDepartement = (Departement) cmbDepartement.getSelectedItem();

        Bloc currentSelectedBloc = (Bloc) cmbBloc.getSelectedItem();
        Niveau currentSelectedNiveau = (Niveau) cmbNiveau.getSelectedItem();

        runInBackground(() -> {
            List<Bloc> fetchedBlocs = new ArrayList<>();
            List<Niveau> fetchedNiveaux = new ArrayList<>();

            if (selectedDepartement != null && selectedDepartement.getId() != 0) {
                fetchedBlocs = blocDAO.getBlocsByDepartement(selectedDepartement.getId());
                fetchedNiveaux = niveauDAO.getNiveauxByDepartement(selectedDepartement.getId());
            }
            return new FilterBlocsNiveauxResult(fetchedBlocs, fetchedNiveaux);
        }, result -> {
            Bloc blocPlaceholder = new Bloc("Sélectioner...", 0);
            updateComboBoxItems(cmbBloc, result.filteredBlocs, blocPlaceholder, currentSelectedBloc);

            Niveau niveauPlaceholder = new Niveau("Sélectioner...", 0);
            updateComboBoxItems(cmbNiveau, result.filteredNiveaux, niveauPlaceholder, currentSelectedNiveau);
            if (isEditMode && currentReservation != null) {
                for (int i = 0; i < cmbBloc.getItemCount(); i++) {
                    Bloc bloc = cmbBloc.getItemAt(i);
                    if (bloc != null && bloc.getId() == currentReservation.getIdBloc()) {
                        cmbBloc.setSelectedItem(bloc);
                        break;
                    }
                }
                if (currentReservation.getIdNiveau() != null) {
                    for (int i = 0; i < cmbNiveau.getItemCount(); i++) {
                        Niveau niveau = cmbNiveau.getItemAt(i);
                        if (niveau != null && niveau.getId() == currentReservation.getIdNiveau()) {
                            cmbNiveau.setSelectedItem(niveau);
                            break;
                        }
                    }
                }
            }

            // Pass the currently selected bloc after updating cmbBloc
            filterEnseignants(selectedDepartement, (Bloc) cmbBloc.getSelectedItem());

            filterRooms();
            updateGroupVisibility(null);

            if (pendingEditPopulate) {
                pendingEditPopulate = false;
                populateFields();
            }
        });
    }

    private void updateNiveauAndRoomFilters() {
        Departement selectedDepartement = (Departement) cmbDepartement.getSelectedItem();
        Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();

        Niveau currentSelectedNiveau = (Niveau) cmbNiveau.getSelectedItem();

        runInBackground(() -> {
            List<Niveau> fetchedNiveaux = new ArrayList<>();
            if (selectedDepartement != null && selectedDepartement.getId() != 0 && selectedBloc != null && selectedBloc.getId() != 0) {
                fetchedNiveaux = niveauDAO.getNiveauxByDepartementAndBloc(selectedDepartement.getId(), selectedBloc.getId());
            }
            return fetchedNiveaux;
        }, filteredNiveaux -> {
            Niveau niveauPlaceholder = new Niveau("Sélectioner...", 0);
            updateComboBoxItems(cmbNiveau, filteredNiveaux, niveauPlaceholder, currentSelectedNiveau);
            if (isEditMode && currentReservation != null && currentReservation.getIdNiveau() != null) {
                for (int i = 0; i < cmbNiveau.getItemCount(); i++) {
                    Niveau niveau = cmbNiveau.getItemAt(i);
                    if (niveau != null && niveau.getId() == currentReservation.getIdNiveau()) {
                        cmbNiveau.setSelectedItem(niveau);
                        break;
                    }
                }
            }

            filterEnseignants(selectedDepartement, selectedBloc);

            filterRooms();
            updateGroupVisibility(null);
        });
    }

    private void filterRooms() {
        if (chkIsOnline.isSelected()) {
            cmbRoom.removeAllItems();
            cmbRoom.addItem(new Room("En ligne", onlineRoomId));
            return;
        }

        Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
        String selectedRoomType = (String) cmbRoomType.getSelectedItem();

        Room currentSelectedRoom = (Room) cmbRoom.getSelectedItem();

        List<Room> filteredRooms = new ArrayList<>();

        if (selectedBloc != null && selectedBloc.getId() != 0) {
            filteredRooms = allRooms.stream()
                    .filter(r -> r.getIdBloc() == selectedBloc.getId())
                    .filter(r -> "Tous".equals(selectedRoomType) || r.getTypeSalle().equals(selectedRoomType))
                    .collect(Collectors.toList());
        }

        Room roomPlaceholder = new Room("Sélectioner...", 0);
        updateComboBoxItems(cmbRoom, filteredRooms, roomPlaceholder, currentSelectedRoom);
        if (isEditMode && currentReservation != null && !currentReservation.isOnline() && currentReservation.getIdSalle() != 0) {
            for (int i = 0; i < cmbRoom.getItemCount(); i++) {
                Room room = cmbRoom.getItemAt(i);
                if (room != null && room.getId() == currentReservation.getIdSalle()) {
                    cmbRoom.setSelectedItem(room);
                    break;
                }
            }
        }
    }

    private void filterEnseignants(Departement selectedDepartement, Bloc selectedBloc) {
        User currentSelectedEnseignant = (User) cmbEnseignant.getSelectedItem();

        runInBackground(() -> {
            List<User> fetchedEnseignants = new ArrayList<>();
            if (selectedDepartement != null && selectedDepartement.getId() != 0) {
                if (selectedBloc != null && selectedBloc.getId() != 0) {
                    fetchedEnseignants = userDAO.getTeachersByDepartmentAndBloc(selectedDepartement.getId(), selectedBloc.getId());
                } else {
                    fetchedEnseignants = userDAO.getTeachersByDepartment(selectedDepartement.getId());
                }
            }
            return fetchedEnseignants;
        }, filteredEnseignants -> {
            User enseignantPlaceholder = new User("Sélectioner...", 0);
            updateComboBoxItems(cmbEnseignant, filteredEnseignants, enseignantPlaceholder, currentSelectedEnseignant);
            if (isEditMode && currentReservation != null) {
                for (int i = 0; i < cmbEnseignant.getItemCount(); i++) {
                    User user = cmbEnseignant.getItemAt(i);
                    if (user != null && user.getIdUtilisateur() == currentReservation.getIdEnseignant()) {
                        cmbEnseignant.setSelectedItem(user);
                        break;
                    }
                }
            }
        });
    }

    private void updateRecurrenceVisibility() {
        boolean isRecurringSelected = chkIsRecurring.isSelected();

        spinnerDay.setVisible(!isRecurringSelected);
        spinnerMonth.setVisible(!isRecurringSelected);
        spinnerYear.setVisible(!isRecurringSelected);
        
        lblRecurrenceStartDate.setVisible(isRecurringSelected);
        dateChooserDebutRecurrence.setVisible(isRecurringSelected);
        lblRecurrenceEndDate.setVisible(isRecurringSelected);
        dateChooserFinRecurrence.setVisible(isRecurringSelected);
        lblDayOfWeek.setVisible(isRecurringSelected);
        cmbDayOfWeek.setVisible(isRecurringSelected);
        
        updateDialogHeight();
    }

    private void updateGroupVisibility(Integer groupNumber) {
        ActivityType selectedActivityType = (ActivityType) cmbActivityType.getSelectedItem();
        boolean isGroupSpecificActivity = (selectedActivityType != null && selectedActivityType.isGroupSpecific());
        
        lblGroupNumber.setVisible(isGroupSpecificActivity);
        cmbGroupNumber.setVisible(isGroupSpecificActivity);

        formContentPanel.revalidate();
        formContentPanel.repaint();

        if (isGroupSpecificActivity) {
            Niveau selectedNiveau = (Niveau) cmbNiveau.getSelectedItem();
            if (selectedNiveau != null && selectedNiveau.getId() != 0) {
                populateGroupComboBox(selectedNiveau.getNombreGroupes());
            } else {
                populateGroupComboBox(0);
            }
        } else {
            populateGroupComboBox(0);
        }
        
        if (groupNumber != null && isGroupSpecificActivity) {
            cmbGroupNumber.setSelectedItem(groupNumber);
        }
        if (isEditMode && currentReservation != null && currentReservation.getGroupNumber() != null && isGroupSpecificActivity) {
            selectGroupIfPresent(currentReservation.getGroupNumber());
        }
        if (pendingGroupNumber != null && isGroupSpecificActivity) {
            if (selectGroupIfPresent(pendingGroupNumber)) {
                pendingGroupNumber = null;
            }
        }
        
        updateDialogHeight();
    }

    private void updateDialogHeight() {
        int newHeight = BASE_HEIGHT;
        
        if (chkIsRecurring.isSelected()) {
            newHeight += RECURRING_HEIGHT_ADD;
        }
        
        if (lblGroupNumber.isVisible()) {
            newHeight += GROUP_HEIGHT_ADD;
        }
        
        setSize(new Dimension(950, newHeight));
        setLocationRelativeTo(getOwner());
    }

    private void populateGroupComboBox(int numberOfGroups) {
        DefaultComboBoxModel<Integer> groupModel = new DefaultComboBoxModel<>();
        groupModel.addElement(0); // 0 will represent "Sélectioner..." or "Tous les groupes"
        for (int i = 1; i <= numberOfGroups; i++) {
            groupModel.addElement(i);
        }
        cmbGroupNumber.setModel(groupModel);
        if (numberOfGroups == 0) {
            cmbGroupNumber.setSelectedIndex(0);
        }
        if (pendingGroupNumber != null) {
            if (selectGroupIfPresent(pendingGroupNumber)) {
                pendingGroupNumber = null;
            }
        }
        // Custom renderer to display placeholder text for 0
        cmbGroupNumber.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer && (Integer) value == 0) {
                    setText("Sélectioner..."); // Placeholder text
                } else if (value instanceof Integer) {
                    setText("Groupe " + value);
                }
                return this;
            }
        });
    }

    private boolean selectGroupIfPresent(Integer groupNumber) {
        if (groupNumber == null) return false;
        ComboBoxModel<Integer> model = cmbGroupNumber.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Integer item = model.getElementAt(i);
            if (groupNumber.equals(item)) {
                cmbGroupNumber.setSelectedItem(item);
                return true;
            }
        }
        return false;
    }

    private void updateDaySpinnerModel() {
        int year = (Integer) spinnerYear.getValue();
        Month month = (Month) spinnerMonth.getValue();
        int maxDay = month.length(LocalDate.of(year, month, 1).isLeapYear());

        SpinnerNumberModel model = (SpinnerNumberModel) spinnerDay.getModel();
        int currentDay = (Integer) model.getValue();
        if (currentDay > maxDay) {
            model.setValue(maxDay);
        }
        model.setMaximum(maxDay);
    }

    private <T> void updateComboBoxItems(JComboBox<T> comboBox, List<T> items, T placeholder, T selectedItemToRestore) {
        comboBox.removeAllItems();
        comboBox.addItem(placeholder);
        
        for (T item : items) {
            comboBox.addItem(item);
        }

        if (selectedItemToRestore != null) {
            boolean restored = false;
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                T item = comboBox.getItemAt(i);
                if (item != null && item.equals(selectedItemToRestore)) {
                    comboBox.setSelectedItem(item);
                    restored = true;
                    break;
                }
            }
            if (!restored) {
                comboBox.setSelectedItem(placeholder);
            }
        } else {
            comboBox.setSelectedItem(placeholder);
        }
    }

    private void populateFields() {
        if (currentReservation == null) return;

        try {
            pendingGroupNumber = currentReservation.getGroupNumber();
            // Set Department
            for (int i = 0; i < cmbDepartement.getItemCount(); i++) {
                Departement dept = cmbDepartement.getItemAt(i);
                if (dept != null && dept.getId() == currentReservation.getIdDepartement()) {
                    cmbDepartement.setSelectedItem(dept);
                    break;
                }
            }

            // Set Bloc
            for (int i = 0; i < cmbBloc.getItemCount(); i++) {
                Bloc bloc = cmbBloc.getItemAt(i);
                if (bloc != null && bloc.getId() == currentReservation.getIdBloc()) {
                    cmbBloc.setSelectedItem(bloc);
                    break;
                }
            }

            // Set Niveau
            if (currentReservation.getIdNiveau() != null) {
                for (int i = 0; i < cmbNiveau.getItemCount(); i++) {
                    Niveau niveau = cmbNiveau.getItemAt(i);
                    if (niveau != null && niveau.getId() == currentReservation.getIdNiveau()) {
                        cmbNiveau.setSelectedItem(niveau);
                        break;
                    }
                }
            }

            // Set Enseignant
            for (int i = 0; i < cmbEnseignant.getItemCount(); i++) {
                User user = cmbEnseignant.getItemAt(i);
                if (user != null && user.getIdUtilisateur() == currentReservation.getIdEnseignant()) {
                    cmbEnseignant.setSelectedItem(user);
                    break;
                }
            }
            
            // Set online checkbox and room fields state
            chkIsOnline.setSelected(currentReservation.isOnline());
            
            // If it's a physical reservation, set the room type first
            if (!currentReservation.isOnline() && currentReservation.getIdSalle() != 0) {
                runInBackground(() -> roomDAO.getRoomById(currentReservation.getIdSalle()), room -> {
                    if (room != null && room.getTypeSalle() != null) {
                        cmbRoomType.setSelectedItem(room.getTypeSalle());
                    }
                    // Call toggleRoomFields to enable/disable fields and re-filter cmbRoom based on chkIsOnline
                    // This needs to happen AFTER attempting to select the specific room
                    toggleRoomFields(); // This calls filterRooms() internally

                    // After cmbRoom has been potentially re-filtered and populated, select the correct room if physical
                    for (int i = 0; i < cmbRoom.getItemCount(); i++) {
                        Room cmbItem = cmbRoom.getItemAt(i);
                        if (cmbItem != null && cmbItem.getId() == currentReservation.getIdSalle()) {
                            cmbRoom.setSelectedItem(cmbItem);
                            break;
                        }
                    }
                });
            } else {
                // If online or no room ID, just toggle fields directly
                toggleRoomFields();
            }
            
            
            // Set Activity Type
            for (int i = 0; i < cmbActivityType.getItemCount(); i++) {
                ActivityType type = cmbActivityType.getItemAt(i);
                if (type != null && type.getId() == currentReservation.getIdTypeActivite()) {
                    cmbActivityType.setSelectedItem(type);
                    break;
                }
            }
            
            updateGroupVisibility(currentReservation.getGroupNumber());
            if (currentReservation.getGroupNumber() != null) {
                selectGroupIfPresent(currentReservation.getGroupNumber());
            }

            // Set recurring fields
            if (currentReservation.isRecurring()) {
                chkIsRecurring.setSelected(true);
                dateChooserDebutRecurrence.setDate(java.sql.Date.valueOf(currentReservation.getDateDebutRecurrence()));
                dateChooserFinRecurrence.setDate(java.sql.Date.valueOf(currentReservation.getDateFinRecurrence()));
                cmbDayOfWeek.setSelectedItem(currentReservation.getDayOfWeek());
            } else {
                chkIsRecurring.setSelected(false);
                LocalDate date = currentReservation.getDateReservation();
                spinnerDay.setValue(date.getDayOfMonth());
                spinnerMonth.setValue(date.getMonth());
                spinnerYear.setValue(date.getYear());
            }

            // Set session from time
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("H:mm");
            String session = currentReservation.getHeureDebut().format(timeFormatter) + " - " + currentReservation.getHeureFin().format(timeFormatter);
            cmbSession.setSelectedItem(session);

            // Set text fields
            txtTitre.setText(currentReservation.getTitreActivite());
            txtDescription.setText(currentReservation.getDescription() != null ? currentReservation.getDescription() : "");
            if (currentReservation.getStatut() != null) {
                cmbStatut.setSelectedItem(currentReservation.getStatut());
            } else if (cmbStatut.getItemCount() > 1) {
                cmbStatut.setSelectedIndex(1);
            }
            txtObservations.setText(currentReservation.getObservations() != null ? currentReservation.getObservations() : "");
            
            updateRecurrenceVisibility();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement des données de la réservation: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        // Hide error label and reset styles
        lblError.setVisible(false);
        resetFieldStyles();

        if (!validateFields()) {
            return;
        }

        // Get selected date and time from form fields
        int day = (Integer) spinnerDay.getValue();
        Month month = (Month) spinnerMonth.getValue();
        int year = (Integer) spinnerYear.getValue();
        LocalDate selectedDate = LocalDate.of(year, month, day);

        String[] sessionTimes = ((String)cmbSession.getSelectedItem()).split(" - ");
        LocalTime selectedStartTime = LocalTime.parse(sessionTimes[0], java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        LocalTime selectedEndTime = LocalTime.parse(sessionTimes[1], java.time.format.DateTimeFormatter.ofPattern("H:mm"));

        // Determine if recurring
        boolean isRecurring = chkIsRecurring.isSelected();

        // Create a Reservation object with current form data for validation and potential saving
        Reservation reservationData = isEditMode ? currentReservation : new Reservation();
        // Set ID for update case; for new reservation, it's 0 until saved
        reservationData.setIdReservation(isEditMode ? currentReservation.getIdReservation() : 0);
        
        boolean isOnlineReservation = chkIsOnline.isSelected();
        reservationData.setOnline(isOnlineReservation);

        if (isOnlineReservation) {
            reservationData.setIdSalle(onlineRoomId);
        } else {
            reservationData.setIdSalle(((Room) cmbRoom.getSelectedItem()).getId());
        }
        
        reservationData.setIdDepartement(((Departement) cmbDepartement.getSelectedItem()).getId());
        reservationData.setIdBloc(((Bloc) cmbBloc.getSelectedItem()).getId());
        if (cmbNiveau.getSelectedItem() != null && ((Niveau) cmbNiveau.getSelectedItem()).getId() != 0) {
            reservationData.setIdNiveau(((Niveau) cmbNiveau.getSelectedItem()).getId());
        } else {
            reservationData.setIdNiveau(null);
        }
        
        Integer selectedGroup = (Integer) cmbGroupNumber.getSelectedItem();
        if (cmbGroupNumber.isVisible() && selectedGroup != null && selectedGroup != 0) {
            reservationData.setGroupNumber(selectedGroup);
        } else {
            reservationData.setGroupNumber(null);
        }

        reservationData.setIdEnseignant(((User) cmbEnseignant.getSelectedItem()).getIdUtilisateur());
        reservationData.setIdTypeActivite(((ActivityType) cmbActivityType.getSelectedItem()).getId());
        reservationData.setTitreActivite(txtTitre.getText().trim());
        reservationData.setHeureDebut(selectedStartTime);
        reservationData.setHeureFin(selectedEndTime);
        reservationData.setDescription(txtDescription.getText().trim());
        reservationData.setStatut((Reservation.ReservationStatus) cmbStatut.getSelectedItem());
        reservationData.setObservations(txtObservations.getText().trim());
        reservationData.setRecurring(isRecurring);

        if (isRecurring) {
            reservationData.setDateReservation(null);
            reservationData.setDateDebutRecurrence(dateChooserDebutRecurrence.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            reservationData.setDateFinRecurrence(dateChooserFinRecurrence.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            reservationData.setDayOfWeek((Reservation.DayOfWeek) cmbDayOfWeek.getSelectedItem());
        } else {
            reservationData.setDateReservation(selectedDate);
            reservationData.setDateDebutRecurrence(null);
            reservationData.setDateFinRecurrence(null);
            reservationData.setDayOfWeek(null);
        }

        // Populate transient fields for better conflict messages (using current selected items)
        Room roomForTransient = (Room) cmbRoom.getSelectedItem();
        if (roomForTransient != null && roomForTransient.getId() != 0) {
            reservationData.setNomSalle(roomForTransient.getName());
        }
        User enseignantForTransient = (User) cmbEnseignant.getSelectedItem();
        if (enseignantForTransient != null && enseignantForTransient.getIdUtilisateur() != 0) {
            reservationData.setNomEnseignant(enseignantForTransient.getNom());
            reservationData.setPrenomEnseignant(enseignantForTransient.getPrenom());
        }
        Niveau niveauForTransient = (Niveau) cmbNiveau.getSelectedItem();
        if (niveauForTransient != null && niveauForTransient.getId() != 0) {
            reservationData.setNomNiveau(niveauForTransient.getNom());
        }

        // Perform conflict checks
        Integer excludeId = isEditMode ? currentReservation.getIdReservation() : null;
        List<Conflict> conflicts = conflictDetectionService.checkConflicts(reservationData, excludeId);

        if (!conflicts.isEmpty()) {
            showConflictDialog(conflicts);
            return; // Prevent saving if conflicts exist
        }

        // If no conflicts, proceed with save/update
        boolean finalSuccess;
        String finalMessage;
        if (isEditMode) {
            // Update currentReservation object with changes before saving
            currentReservation.setIdSalle(reservationData.getIdSalle());
            currentReservation.setIdDepartement(reservationData.getIdDepartement());
            currentReservation.setIdBloc(reservationData.getIdBloc());
            currentReservation.setIdNiveau(reservationData.getIdNiveau());
            currentReservation.setGroupNumber(reservationData.getGroupNumber());
            currentReservation.setIdEnseignant(reservationData.getIdEnseignant());
            currentReservation.setIdTypeActivite(reservationData.getIdTypeActivite());
            currentReservation.setTitreActivite(reservationData.getTitreActivite());
            currentReservation.setHeureDebut(reservationData.getHeureDebut());
            currentReservation.setHeureFin(reservationData.getHeureFin());
            currentReservation.setDescription(reservationData.getDescription());
            currentReservation.setStatut(reservationData.getStatut());
            currentReservation.setObservations(reservationData.getObservations());
            currentReservation.setRecurring(reservationData.isRecurring());
            currentReservation.setDateReservation(reservationData.getDateReservation());
            currentReservation.setDateDebutRecurrence(reservationData.getDateDebutRecurrence());
            currentReservation.setDateFinRecurrence(reservationData.getDateFinRecurrence());
            currentReservation.setDayOfWeek(reservationData.getDayOfWeek());
            currentReservation.setOnline(reservationData.isOnline());

            try {
                finalSuccess = reservationDAO.updateReservation(currentReservation);
                finalMessage = finalSuccess ? "Réservation modifiée avec succès." : "Échec de la modification de la réservation.";
            } catch (ReservationConflictException ex) {
                JOptionPane.showMessageDialog(this,
                        "Conflit de réservation récurrente détecté. Veuillez ajuster les dates ou l'horaire.",
                        "Conflit",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            reservationData.setIdUtilisateurCreation(currentUserId);
            finalSuccess = reservationDAO.addReservation(reservationData) > 0;
            finalMessage = finalSuccess ? "Réservation ajoutée avec succès." : "Échec de l'ajout de la réservation.";
        }
        
        if (finalSuccess) {
            UIUtils.showTemporaryMessage(ownerFrame, finalMessage, true, 3000);
            callback.onDialogClose(finalSuccess, finalMessage);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, finalMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (cmbDepartement.getSelectedItem() == null || ((Departement) cmbDepartement.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbDepartement);
            isValid = false;
        }
        if (cmbBloc.getSelectedItem() == null || ((Bloc) cmbBloc.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbBloc);
            isValid = false;
        }
        if (cmbNiveau.getSelectedItem() == null || ((Niveau) cmbNiveau.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbNiveau);
            isValid = false;
        }
        if (cmbEnseignant.getSelectedItem() == null || ((User) cmbEnseignant.getSelectedItem()).getIdUtilisateur() == 0) {
            applyErrorStyle(cmbEnseignant);
            isValid = false;
        }
        if (!chkIsOnline.isSelected()) {
            if (cmbRoomType.getSelectedItem() == null || "Sélectioner...".equals(cmbRoomType.getSelectedItem())) {
                applyErrorStyle(cmbRoomType);
                isValid = false;
            }
            if (cmbRoom.getSelectedItem() == null || ((Room) cmbRoom.getSelectedItem()).getId() == 0) {
                applyErrorStyle(cmbRoom);
                isValid = false;
            }
        }
        if (cmbActivityType.getSelectedItem() == null || ((ActivityType) cmbActivityType.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbActivityType);
            isValid = false;
        }
        if (txtTitre.getText().trim().isEmpty()) {
            applyErrorStyle(txtTitre);
            isValid = false;
        }
        if (cmbSession.getSelectedItem() == null || "Sélectioner...".equals(cmbSession.getSelectedItem())) {
            applyErrorStyle(cmbSession);
            isValid = false;
        }
        if (cmbStatut.getSelectedItem() == null) {
            applyErrorStyle(cmbStatut);
            isValid = false;
        }
        
        if (chkIsRecurring.isSelected()) {
            if (dateChooserDebutRecurrence.getDate() == null) {
                applyErrorStyle(dateChooserDebutRecurrence);
                isValid = false;
            }
            if (dateChooserFinRecurrence.getDate() == null) {
                applyErrorStyle(dateChooserFinRecurrence);
                isValid = false;
            }
            if (cmbDayOfWeek.getSelectedItem() == null) {
                applyErrorStyle(cmbDayOfWeek);
                isValid = false;
            } else if (cmbDayOfWeek.getSelectedItem() == Reservation.DayOfWeek.FRIDAY) {
                applyErrorStyle(cmbDayOfWeek);
                lblError.setText("Il n'y a pas de séances le vendredi.");
                lblError.setVisible(true);
                return false;
            }
        } else {
            int day = (Integer) spinnerDay.getValue();
            Month month = (Month) spinnerMonth.getValue();
            int year = (Integer) spinnerYear.getValue();
            try {
                LocalDate selectedDate = LocalDate.of(year, month, day);
                if (selectedDate.getDayOfWeek() == java.time.DayOfWeek.FRIDAY) {
                    applyErrorStyle(spinnerDay);
                    applyErrorStyle(spinnerMonth);
                    applyErrorStyle(spinnerYear);
                    lblError.setText("Il n'y a pas de séances le vendredi.");
                    lblError.setVisible(true);
                    return false;
                }
            } catch (java.time.DateTimeException e) {
                applyErrorStyle(spinnerDay);
                applyErrorStyle(spinnerMonth);
                applyErrorStyle(spinnerYear);
                lblError.setText("Date invalide. Veuillez vérifier le jour, le mois et l'année.");
                lblError.setVisible(true);
                return false;
            }
        }
        
        if (cmbGroupNumber.isVisible() && ((Integer)cmbGroupNumber.getSelectedItem() == 0)) {
            applyErrorStyle(cmbGroupNumber);
            isValid = false;
        }

        if (!isValid) {
            lblError.setText("Veuillez remplir tous les champs obligatoires.");
            lblError.setVisible(true);
        }

        return isValid;
    }

    private void applyErrorStyle(JComponent component) {
        FormValidationUtils.applyErrorStyle(component);
        if (component instanceof com.toedter.calendar.JDateChooser) {
            ((com.toedter.calendar.JDateChooser) component).setBorder(BorderFactory.createLineBorder(ThemeConstants.ERROR_RED, 1));
        } else if (component instanceof JCheckBox) {
            component.setForeground(ThemeConstants.ERROR_RED);
        }
    }

    private void applyDefaultStyle(JComponent component) {
        FormValidationUtils.applyDefaultStyle(component);
        if (component instanceof com.toedter.calendar.JDateChooser) {
            ((com.toedter.calendar.JDateChooser) component).setBorder(UIManager.getBorder("TextField.border"));
        } else if (component instanceof JCheckBox) {
            component.setForeground(UIManager.getColor("CheckBox.foreground"));
        }
    }

    private void resetFieldStyles() {
        applyDefaultStyle(cmbDepartement);
        applyDefaultStyle(cmbBloc);
        applyDefaultStyle(cmbNiveau);
        applyDefaultStyle(cmbEnseignant);
        applyDefaultStyle(cmbRoomType);
        applyDefaultStyle(cmbRoom);
        applyDefaultStyle(cmbActivityType);
        applyDefaultStyle(txtTitre);
        applyDefaultStyle(cmbSession);
        applyDefaultStyle(dateChooserDebutRecurrence);
        applyDefaultStyle(dateChooserFinRecurrence);
        applyDefaultStyle(cmbDayOfWeek);
        applyDefaultStyle(spinnerDay);
        applyDefaultStyle(spinnerMonth);
        applyDefaultStyle(spinnerYear);
        applyDefaultStyle(cmbGroupNumber);
    }

    private void showConflictDialog(List<Conflict> conflicts) {
        StringBuilder conflictDetails = new StringBuilder();
        
        boolean isCompleteDuplicate = conflicts.stream().anyMatch(c -> c.getType() == Conflict.ConflictType.COMPLETE_DUPLICATE);

        if (isCompleteDuplicate) {
            Conflict duplicateConflict = conflicts.stream()
                .filter(c -> c.getType() == Conflict.ConflictType.COMPLETE_DUPLICATE)
                .findFirst().orElse(null);
            if (duplicateConflict != null) {
                conflictDetails.append("<html><b>").append(duplicateConflict.getMessage()).append("</b><br>");
                conflictDetails.append(duplicateConflict.getDetails().replace("\n", "<br>"));
            }
        } else if (conflicts.size() > 1) {
            conflictDetails.append("<html><b>Plusieurs conflits détectés !</b><br><br>");
            for (Conflict conflict : conflicts) {
                conflictDetails.append("<b>").append(conflict.getMessage()).append("</b><br>");
                conflictDetails.append(conflict.getDetails().replace("\n", "<br>")).append("<br>");
            }
        } else {
            Conflict conflict = conflicts.get(0);
            conflictDetails.append("<html><b>").append(conflict.getMessage()).append("</b><br>");
            conflictDetails.append(conflict.getDetails().replace("\n", "<br>"));
        }
        conflictDetails.append("</html>");

        // Create the OK button explicitly so we can add an action listener
        JButton okButton = UIUtils.createSecondaryButton("OK");
        
        JOptionPane pane = new JOptionPane(
            conflictDetails.toString(),
            JOptionPane.WARNING_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            new Object[]{okButton} // Use our custom OK button
        );
        
        JDialog dialog = pane.createDialog(this, "Conflit Détecté");

        okButton.addActionListener(e -> dialog.dispose()); // Add action to dismiss dialog

        if (!isCompleteDuplicate && conflicts.stream().anyMatch(c -> c.getConflictingReservation() != null)) {
            JButton viewReservationButton = UIUtils.createPrimaryButton("Voir la réservation");
            viewReservationButton.addActionListener(e -> {
                // When "Voir la réservation" is clicked, it should also dismiss the current conflict dialog
                dialog.dispose(); // Dismiss the conflict dialog
                
                Conflict firstConflictWithRes = conflicts.stream()
                    .filter(c -> c.getConflictingReservation() != null)
                    .findFirst().orElse(null);
                if (firstConflictWithRes != null) {
                    runInBackground(() -> reservationDAO.getReservationById(firstConflictWithRes.getConflictingReservation().getIdReservation()), conflictingReservation -> {
                        if (conflictingReservation != null) {
                            ReservationDialog conflictingReservationDialog = new ReservationDialog(
                                ownerFrame,
                                reservationDAO,
                                departementDAO,
                                blocDAO,
                                niveauDAO,
                                userDAO,
                                roomDAO,
                                activityTypeDAO,
                                conflictingReservation,
                                (success, message) -> {},
                                currentUserId
                            );
                            conflictingReservationDialog.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(
                                this,
                                "Impossible de trouver la réservation en conflit.",
                                "Erreur",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    });
                }
                // No need for pane.setValue("OK"); anymore as we explicitly dispose
            });
            pane.setOptions(new Object[]{viewReservationButton, okButton}); // Use our custom buttons
        }
        
        dialog.setVisible(true); // This blocks until dialog is dismissed by dispose()
    }

    private <T> void runInBackground(java.util.concurrent.Callable<T> backgroundTask, java.util.function.Consumer<T> onDone) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.call();
            }

            @Override
            protected void done() {
                try {
                    onDone.accept(get());
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ReservationDialog.this,
                            "Erreur lors du traitement en arrière-plan: " + e.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
