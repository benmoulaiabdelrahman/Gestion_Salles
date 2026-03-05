package com.gestion.salles.views.ChefDepartement;

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
import java.awt.event.ItemEvent;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class ReservationDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(ReservationDialog.class.getName());

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
    private JPanel formPanel;
    private Frame ownerFrame;

    private final ReservationDAO reservationDAO;
    private final ConflictDetectionService conflictDetectionService;
    private final RoomDAO roomDAO; // Added
    private final DepartementDAO departementDAO; // Added
    private final BlocDAO blocDAO; // Added
    private final NiveauDAO niveauDAO; // Added
    private final UserDAO userDAO; // Added
    private final ActivityTypeDAO activityTypeDAO; // Added

    private final Reservation currentReservation;
    private final boolean isEditMode;
    private final DialogCallback callback;

    private List<Departement> allDepartments;
    private List<Bloc> allBlocs;
    private List<Niveau> allNiveaux;
    private List<User> allEnseignants;
    private List<Room> allRooms;
    private List<ActivityType> allActivityTypes;
    private final User currentHoD;
    
    private int onlineRoomId; // Added for the online room ID
    private Integer pendingGroupNumber;
    
    private static final int BASE_WIDTH = 860;
    private static final int BASE_HEIGHT = 550; // Adjusted for ChefDepartement default size
    private static final int RECURRING_HEIGHT_ADD = 70;
    private static final int GROUP_HEIGHT_ADD = 35;

    public ReservationDialog(Frame owner, ReservationDAO reservationDAO, Reservation reservationToEdit, List<Room> allRoomsFiltered, List<User> allTeachersFiltered, DialogCallback callback, User currentHoD) {
        super(owner, true);
        if (reservationDAO == null) {
            throw new IllegalArgumentException("ReservationDAO cannot be null.");
        }
        this.reservationDAO = reservationDAO;
        this.conflictDetectionService = new ConflictDetectionService();
        
        // Initialize all DAOs here
        this.roomDAO = new RoomDAO();
        this.departementDAO = new DepartementDAO();
        this.blocDAO = new BlocDAO();
        this.niveauDAO = new NiveauDAO();
        this.userDAO = new UserDAO();
        this.activityTypeDAO = new ActivityTypeDAO();
        
        // Ensure online room exists and get its ID
        this.onlineRoomId = roomDAO.ensureOnlineRoomExists();

        this.currentReservation = reservationToEdit;
        this.isEditMode = (reservationToEdit != null);
        this.callback = callback;
        this.ownerFrame = owner;
        this.currentHoD = currentHoD;

        this.allRooms = allRoomsFiltered;
        this.allEnseignants = allTeachersFiltered;
        
        initComponents();
        loadComboBoxData();

        if (currentHoD.getIdDepartement() == null || currentHoD.getIdBloc() == null) {
             SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "Le chef de département n'est pas correctement associé à une faculté et un département. Opération annulée.",
                    "Erreur de configuration",
                    JOptionPane.WARNING_MESSAGE);
                dispose(); 
                callback.onDialogClose(false, "Erreur de configuration du chef de département.");
            });
            return;
        }

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


        setLayout(new BorderLayout(10, 10));

        formPanel = new JPanel(new MigLayout(
            "wrap 4, fillx, insets 20 20 15 20",
            "[100!, right][grow, fill][100!, right][grow, fill]",
            "[]8[]"
        ));
        formPanel.setBackground(ThemeConstants.CARD_WHITE);

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
            editorComponent.setBackground(ThemeConstants.CARD_WHITE);
            editorComponent.setOpaque(true);
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
            editorComponent.setBackground(ThemeConstants.CARD_WHITE);
            editorComponent.setOpaque(true);
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

        lblError = new JLabel("");
        lblError.setForeground(ThemeConstants.ERROR_RED);
        lblError.setFont(lblError.getFont().deriveFont(Font.BOLD, 12f));
        lblError.setVisible(false);

        cmbRoomType = UIUtils.createStyledComboBox(new JComboBox<>());
        lblEquipment = new JLabel(" ");

        // ROW 1: Enseignant / Niveau
        addFormRow("Enseignant:", cmbEnseignant, "Niveau:", cmbNiveau);

        // ROW 2: Titre / Type d'activité
        addFormRow("Titre:", txtTitre, "Type d'activité:", cmbActivityType);

        // Group row (after titre/type)
        formPanel.add(lblGroupNumber, "hidemode 2, right");
        formPanel.add(cmbGroupNumber, "hidemode 2, h 35!, span 3, grow, wrap");

        // Checkboxes
        formPanel.add(chkIsOnline, "span 2");
        formPanel.add(chkIsRecurring, "span 2, wrap");

        // Recurrence fields
        formPanel.add(lblRecurrenceStartDate, "hidemode 2, right");
        formPanel.add(dateChooserDebutRecurrence, "hidemode 2, h 35!, grow");
        formPanel.add(lblRecurrenceEndDate, "hidemode 2, right");
        formPanel.add(dateChooserFinRecurrence, "hidemode 2, h 35!, grow, wrap");

        formPanel.add(lblDayOfWeek, "hidemode 2, right");
        formPanel.add(cmbDayOfWeek, "hidemode 2, h 35!, span 3, grow, wrap");

        // Date / Session
        JPanel datePanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][grow][grow]"));
        datePanel.setBackground(ThemeConstants.CARD_WHITE);
        datePanel.add(spinnerDay, "h 35!");
        datePanel.add(spinnerMonth, "h 35!");
        datePanel.add(spinnerYear, "h 35!");
        addFormRow("Date:", datePanel, "Session:", cmbSession);

        // Type de salle / Salle
        addFormRow("Type de salle:", cmbRoomType, "Salle:", cmbRoom);

        // Équipements / Statut
        addFormRow("Équipements:", lblEquipment, "Statut:", cmbStatut);

        // Description
        formPanel.add(new JLabel("Description:"), "right");
        formPanel.add(scrollDescription, "span 3, grow, h 70!, gaptop 3");

        // Observations
        formPanel.add(new JLabel("Observations:"), "right");
        formPanel.add(scrollObservations, "span 3, grow, h 70!, gaptop 3");

        JButton btnSave = UIUtils.createPrimaryButton("Enregistrer");
        JButton btnCancel = UIUtils.createSecondaryButton("Annuler");

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());

        JPanel buttonAndErrorPanel = new JPanel(new MigLayout("insets 0, fillx", "[][][]push[]"));
        buttonAndErrorPanel.setBackground(ThemeConstants.CARD_WHITE);
        buttonAndErrorPanel.add(btnSave);
        buttonAndErrorPanel.add(btnCancel, "gap 5");
        buttonAndErrorPanel.add(lblError, "pushx, alignx right, h 20!");

        formPanel.add(buttonAndErrorPanel, "span 4, grow, gaptop 15");

        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(formScrollPane, BorderLayout.CENTER);

        cmbRoom.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateEquipmentLabel();
            }
        });

        cmbRoomType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                filterRooms();
            }
        });
        
        chkIsRecurring.addActionListener(e -> updateRecurrenceVisibility());
        cmbActivityType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateGroupVisibility();
            }
        });
        cmbNiveau.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateGroupVisibility();
            }
        });

        updateRecurrenceVisibility();
        updateGroupVisibility();
        updateDialogHeight(); // Call after initial visibility updates
        
        chkIsOnline.addActionListener(e -> toggleRoomFields());
        toggleRoomFields(); // Initial state setup
    }

    private void addFormRow(String label1, JComponent component1, String label2, JComponent component2) {
        JLabel lbl1 = new JLabel(label1);
        JLabel lbl2 = new JLabel(label2);

        formPanel.add(lbl1, "right");
        formPanel.add(component1, "grow, h 35!");
        formPanel.add(lbl2, "right");
        formPanel.add(component2, "grow, h 35!, wrap");
    }

    private void toggleRoomFields() {
        boolean isOnline = chkIsOnline.isSelected();
        cmbRoomType.setEnabled(!isOnline);
        cmbRoom.setEnabled(!isOnline);
        lblEquipment.setEnabled(!isOnline);

        if (isOnline) {
            cmbRoom.setSelectedItem(null); // Clear selected room
            lblEquipment.setText("En ligne");
        } else {
            // Restore original state
            filterRooms(); // Re-populate rooms based on current filters
            updateEquipmentLabel(); // Update equipment label based on selected room
        }
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
        updateDialogHeight(); // Call updateDialogHeight after visibility changes
    }

    private void updateGroupVisibility() {
        ActivityType selectedActivityType = (ActivityType) cmbActivityType.getSelectedItem();
        boolean isGroupSpecificActivity = (selectedActivityType != null && selectedActivityType.isGroupSpecific());
        
        lblGroupNumber.setVisible(isGroupSpecificActivity);
        cmbGroupNumber.setVisible(isGroupSpecificActivity);

        formPanel.revalidate();
        formPanel.repaint();

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
        if (pendingGroupNumber != null && isGroupSpecificActivity) {
            if (selectGroupIfPresent(pendingGroupNumber)) {
                pendingGroupNumber = null;
            }
        }
        updateDialogHeight(); // Call updateDialogHeight after visibility changes
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

    private void updateDialogHeight() {
        int newHeight = BASE_HEIGHT;
        
        if (chkIsRecurring.isSelected()) {
            newHeight += RECURRING_HEIGHT_ADD;
        }
        
        if (lblGroupNumber.isVisible()) {
            newHeight += GROUP_HEIGHT_ADD;
        }
        
        // Adjust height for chkIsOnline if it affects the layout significantly
        // For now, assuming it's a single line and fits within existing space or is handled by MigLayout's wrap
        // If it introduces an extra line, uncomment and adjust:
        // if (chkIsOnline.isVisible()) {
        //     newHeight += SOME_HEIGHT_FOR_CHECKBOX;
        // }
        
        setSize(new Dimension(BASE_WIDTH, newHeight));
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
                if (equipmentList != null) {
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
        // Use class-level DAOs
        
        allDepartments = new ArrayList<>();
        Departement hodDept = departementDAO.getDepartementById(currentHoD.getIdDepartement());
        if (hodDept != null) {
            allDepartments.add(hodDept);
        }

        allBlocs = new ArrayList<>();
        Bloc hodBloc = blocDAO.getBlocById(currentHoD.getIdBloc());
        if (hodBloc != null) {
            allBlocs.add(hodBloc);
        }

        allNiveaux = niveauDAO.getNiveauxByBloc(currentHoD.getIdBloc());

        // allEnseignants and allRooms are passed to the constructor, so they should already be set
        // If they are null or empty, then load them using the class-level DAOs
        if (this.allEnseignants == null || this.allEnseignants.isEmpty()) {
            this.allEnseignants = userDAO.getTeachersByBloc(currentHoD.getIdBloc());
        }
        if (this.allRooms == null || this.allRooms.isEmpty()) {
            this.allRooms = roomDAO.getRoomsByBloc(currentHoD.getIdBloc());
        }
        
        allActivityTypes = activityTypeDAO.getAllActivityTypes();
        List<String> roomTypes = roomDAO.getRoomTypes();

        Niveau niveauPlaceholder = new Niveau("Sélectioner...", 0);
        updateComboBoxItems(cmbNiveau, allNiveaux, niveauPlaceholder, null);

        User enseignantPlaceholder = new User("Sélectioner...", 0);
        updateComboBoxItems(cmbEnseignant, allEnseignants, enseignantPlaceholder, null);

        String roomTypePlaceholder = "Sélectioner...";
        updateComboBoxItems(cmbRoomType, roomTypes, roomTypePlaceholder, null);

        ActivityType activityTypePlaceholder = new ActivityType("Sélectioner...", 0);
        updateComboBoxItems(cmbActivityType, allActivityTypes, activityTypePlaceholder, null);

        filterRooms();
        filterEnseignants(hodDept, hodBloc);
        updateEquipmentLabel();
    }

    private void updateNiveauAndRoomFilters() {
        Integer selectedDepartementId = currentHoD.getIdDepartement();
        Integer selectedBlocId = currentHoD.getIdBloc();

        Niveau currentSelectedNiveau = (Niveau) cmbNiveau.getSelectedItem();

        List<Niveau> filteredNiveaux = new ArrayList<>();
        if (selectedDepartementId != null && selectedBlocId != null) {
            filteredNiveaux = new NiveauDAO().getNiveauxByDepartementAndBloc(selectedDepartementId, selectedBlocId);
        } else if (selectedDepartementId != null) {
            filteredNiveaux = new NiveauDAO().getNiveauxByDepartement(selectedDepartementId);
        }

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

        filterEnseignants(new DepartementDAO().getDepartementById(selectedDepartementId), new BlocDAO().getBlocById(selectedBlocId));

        filterRooms();
        updateGroupVisibility();
    }

    private void filterRooms() {
        if (chkIsOnline.isSelected()) {
            cmbRoom.removeAllItems();
            cmbRoom.addItem(new Room("En ligne", onlineRoomId)); // Use the dynamically fetched onlineRoomId
            return;
        }

        Integer selectedBlocId = currentHoD.getIdBloc();
        String selectedRoomType = (String) cmbRoomType.getSelectedItem();

        Room currentSelectedRoom = (Room) cmbRoom.getSelectedItem();

        List<Room> filteredRooms = new ArrayList<>();

        if (selectedBlocId != null && selectedBlocId != 0) {
            filteredRooms = allRooms.stream()
                    .filter(r -> r.getIdBloc() == selectedBlocId)
                    .filter(r -> selectedRoomType == null
                            || "Sélectioner...".equals(selectedRoomType)
                            || r.getTypeSalle().equals(selectedRoomType))
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

    private void populateFields() {
        if (currentReservation != null) {
            pendingGroupNumber = currentReservation.getGroupNumber();

            // Set online checkbox and room fields state
            chkIsOnline.setSelected(currentReservation.isOnline());
            
            // If it's a physical reservation, set the room type first
            if (!currentReservation.isOnline() && currentReservation.getIdSalle() != 0) {
                Room room = new RoomDAO().getRoomById(currentReservation.getIdSalle());
                if (room != null && room.getTypeSalle() != null) {
                    cmbRoomType.setSelectedItem(room.getTypeSalle());
                }
            }
            
            // Call updateNiveauAndRoomFilters and toggleRoomFields to ensure cmbRoom is filtered and UI is consistent
            // updateNiveauAndRoomFilters calls filterRooms() and updateGroupVisibility().
            // toggleRoomFields also calls filterRooms().
            // Let's prioritize correct room filtering before attempting to select the room.
            updateNiveauAndRoomFilters(); // This will filter rooms based on current HoD's bloc/dept and selected room type
            toggleRoomFields(); // This handles enabling/disabling of room fields and calls filterRooms() again based on chkIsOnline

            // After cmbRoom has been potentially re-filtered and populated, select the correct room if physical
            if (!currentReservation.isOnline() && currentReservation.getIdSalle() != 0) {
                for (int i = 0; i < cmbRoom.getItemCount(); i++) {
                    Room room = cmbRoom.getItemAt(i);
                    if (room != null && room.getId() == currentReservation.getIdSalle()) {
                        cmbRoom.setSelectedItem(room);
                        break;
                    }
                }
            }
            
            if (currentReservation.getIdNiveau() != null) {
                for (int i = 0; i < cmbNiveau.getItemCount(); i++) {
                    if (cmbNiveau.getItemAt(i).getId() == currentReservation.getIdNiveau()) {
                        cmbNiveau.setSelectedIndex(i);
                        break;
                    }
                }
            }

            for (int i = 0; i < cmbEnseignant.getItemCount(); i++) {
                if (cmbEnseignant.getItemAt(i).getIdUtilisateur() == currentReservation.getIdEnseignant()) {
                    cmbEnseignant.setSelectedIndex(i);
                    break;
                }
            }

            for (int i = 0; i < cmbActivityType.getItemCount(); i++) {
                if (cmbActivityType.getItemAt(i).getId() == currentReservation.getIdTypeActivite()) {
                    cmbActivityType.setSelectedIndex(i);
                    break;
                }
            }

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

            String session = String.format("%s - %s",
                currentReservation.getHeureDebut().format(java.time.format.DateTimeFormatter.ofPattern("H:mm")),
                currentReservation.getHeureFin().format(java.time.format.DateTimeFormatter.ofPattern("H:mm")));
            cmbSession.setSelectedItem(session);

            txtTitre.setText(currentReservation.getTitreActivite());
            txtDescription.setText(currentReservation.getDescription());
            if (currentReservation.getStatut() != null) {
                cmbStatut.setSelectedItem(currentReservation.getStatut());
            } else if (cmbStatut.getItemCount() > 1) {
                cmbStatut.setSelectedIndex(1);
            }
            txtObservations.setText(currentReservation.getObservations());
            
            updateRecurrenceVisibility();
            updateGroupVisibility();
            if (currentReservation.getGroupNumber() != null) {
                cmbGroupNumber.setSelectedItem(currentReservation.getGroupNumber());
            }
        }
    }

    private void onSave() {
        lblError.setVisible(false);
        resetFieldStyles();

        if (!validateFields()) {
            return;
        }

        int day = (Integer) spinnerDay.getValue();
        Month month = (Month) spinnerMonth.getValue();
        int year = (Integer) spinnerYear.getValue();
        LocalDate selectedDate = LocalDate.of(year, month, day);

        String[] sessionTimes = ((String)cmbSession.getSelectedItem()).split(" - ");
        LocalTime selectedStartTime = LocalTime.parse(sessionTimes[0], java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        LocalTime selectedEndTime = LocalTime.parse(sessionTimes[1], java.time.format.DateTimeFormatter.ofPattern("H:mm"));

        boolean isRecurring = chkIsRecurring.isSelected();

        Reservation reservationData = isEditMode ? currentReservation : new Reservation();
        reservationData.setIdReservation(isEditMode ? currentReservation.getIdReservation() : 0);
        
        boolean isOnlineReservation = chkIsOnline.isSelected();
        reservationData.setOnline(isOnlineReservation);

        Room selectedRoom = null; // Declare here
        if (isOnlineReservation) {
            reservationData.setIdSalle(onlineRoomId);
        } else {
            selectedRoom = (Room) cmbRoom.getSelectedItem(); // Assign here
            if (selectedRoom != null && selectedRoom.getId() != 0) {
                reservationData.setIdSalle(selectedRoom.getId());
            }
        }
        
        Niveau selectedNiveau = (Niveau) cmbNiveau.getSelectedItem();
        User selectedEnseignant = (User) cmbEnseignant.getSelectedItem();
        ActivityType selectedActivityType = (ActivityType) cmbActivityType.getSelectedItem();
        Reservation.ReservationStatus selectedStatus = (Reservation.ReservationStatus) cmbStatut.getSelectedItem();

        // No need for reservationData.setIdSalle(selectedRoom.getId()); here anymore
        reservationData.setIdDepartement(currentHoD.getIdDepartement());
        reservationData.setIdBloc(currentHoD.getIdBloc());
        if (selectedNiveau != null && selectedNiveau.getId() != 0) {
            reservationData.setIdNiveau(selectedNiveau.getId());
        } else {
            reservationData.setIdNiveau(null);
        }
        
        Integer selectedGroup = (Integer) cmbGroupNumber.getSelectedItem();
        if (cmbGroupNumber.isVisible() && selectedGroup != null && selectedGroup != 0) {
            reservationData.setGroupNumber(selectedGroup);
        } else {
            reservationData.setGroupNumber(null);
        }

        reservationData.setIdEnseignant(selectedEnseignant.getIdUtilisateur());
        reservationData.setIdTypeActivite(selectedActivityType.getId());
        reservationData.setTitreActivite(txtTitre.getText().trim());
        reservationData.setHeureDebut(selectedStartTime);
        reservationData.setHeureFin(selectedEndTime);
        reservationData.setDescription(txtDescription.getText().trim());
        reservationData.setObservations(txtObservations.getText().trim());
        reservationData.setStatut(selectedStatus);
        reservationData.setIdUtilisateurCreation(currentHoD.getIdUtilisateur());
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

        if (selectedRoom != null && selectedRoom.getId() != 0) {
            reservationData.setNomSalle(selectedRoom.getName());
        }
        if (selectedEnseignant != null && selectedEnseignant.getIdUtilisateur() != 0) {
            reservationData.setNomEnseignant(selectedEnseignant.getNom());
            reservationData.setPrenomEnseignant(selectedEnseignant.getPrenom());
        }
        if (selectedNiveau != null && selectedNiveau.getId() != 0) {
            reservationData.setNomNiveau(selectedNiveau.getNom());
        }
        if (cmbGroupNumber.isVisible() && reservationData.getGroupNumber() != null && reservationData.getGroupNumber() != 0) {
            reservationData.setDescription("Groupe " + reservationData.getGroupNumber());
        }

        Integer excludeId = isEditMode ? currentReservation.getIdReservation() : null;
        List<Conflict> conflicts = conflictDetectionService.checkConflicts(reservationData, excludeId);

        if (!conflicts.isEmpty()) {
            showConflictDialog(conflicts);
            return;
        }

        boolean finalSaveSuccess;
        String finalNotificationMessage;

        if (isEditMode) {
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
                finalSaveSuccess = reservationDAO.updateReservation(currentReservation);
                finalNotificationMessage = finalSaveSuccess ? "Réservation modifiée avec succès." : "Échec de la modification de la réservation.";
            } catch (ReservationConflictException ex) {
                JOptionPane.showMessageDialog(this,
                        "Conflit de réservation récurrente détecté. Veuillez ajuster les dates ou l'horaire.",
                        "Conflit",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            int newId = reservationDAO.addReservation(reservationData);
            finalSaveSuccess = (newId != -1);
            if (finalSaveSuccess) {
                reservationData.setIdReservation(newId);
                finalNotificationMessage = "Réservation ajoutée avec succès.";
            } else {
                finalNotificationMessage = "Échec de l'ajout de la réservation.";
            }
        }

        if (finalSaveSuccess) {
            UIUtils.showTemporaryMessage(ownerFrame, finalNotificationMessage, true, 3000);
            callback.onDialogClose(finalSaveSuccess, finalNotificationMessage);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, finalNotificationMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (currentHoD.getIdDepartement() == null || currentHoD.getIdDepartement() == 0) {
            lblError.setText("Veuillez remplir tous les champs obligatoires.");
            lblError.setVisible(true);
            return false;
        }
        if (currentHoD.getIdBloc() == null || currentHoD.getIdBloc() == 0) {
            lblError.setText("Veuillez remplir tous les champs obligatoires.");
            lblError.setVisible(true);
            return false;
        }

        if (cmbEnseignant.getSelectedItem() == null || ((User) cmbEnseignant.getSelectedItem()).getIdUtilisateur() == 0) {
            applyErrorStyle(cmbEnseignant);
            isValid = false;
        }
        if (cmbNiveau.getSelectedItem() == null || ((Niveau) cmbNiveau.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbNiveau);
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
                revalidate();
                repaint();
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
                    revalidate();
                    repaint();
                    return false;
                }
            } catch (java.time.DateTimeException e) {
                applyErrorStyle(spinnerDay);
                applyErrorStyle(spinnerMonth);
                applyErrorStyle(spinnerYear);
                lblError.setText("Date invalide. Veuillez vérifier le jour, le mois et l'année.");
                lblError.setVisible(true);
                revalidate();
                repaint();
                return false;
            }
        }

        if (cmbGroupNumber.isVisible() && (Integer)cmbGroupNumber.getSelectedItem() == 0) {
            applyErrorStyle(cmbGroupNumber);
            isValid = false;
        }

        if (!isValid) {
            lblError.setText("Veuillez remplir tous les champs obligatoires.");
            lblError.setVisible(true);
            revalidate();
            repaint();
            return false;
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
        // Reset all form fields to default style
        applyDefaultStyle(cmbEnseignant);
        applyDefaultStyle(cmbNiveau);
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

    private void filterEnseignants(Departement selectedDepartement, Bloc selectedBloc) {
        User currentSelectedEnseignant = (User) cmbEnseignant.getSelectedItem();
        List<User> filteredEnseignants = new ArrayList<>();

        if (selectedDepartement != null && selectedDepartement.getId() != 0) {
            if (selectedBloc != null && selectedBloc.getId() != 0) {
                filteredEnseignants = new UserDAO().getTeachersByDepartmentAndBloc(selectedDepartement.getId(), selectedBloc.getId());
            } else {
                filteredEnseignants = new UserDAO().getTeachersByDepartment(selectedDepartement.getId());
            }
        }

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
    }

    private void showConflictDialog(List<Conflict> conflicts) {
        StringBuilder conflictDetails = new StringBuilder();
        
        boolean isCompleteDuplicate = conflicts.stream().anyMatch(c -> c.getType() == Conflict.ConflictType.COMPLETE_DUPLICATE);

        if (isCompleteDuplicate) {
            Conflict duplicateConflict = conflicts.stream().filter(c -> c.getType() == Conflict.ConflictType.COMPLETE_DUPLICATE).findFirst().orElse(null);
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
                    Reservation conflictingReservation = reservationDAO.getReservationById(firstConflictWithRes.getConflictingReservation().getIdReservation());
                    if (conflictingReservation != null) {
                        ReservationDialog conflictingReservationDialog = new ReservationDialog(
                                ownerFrame,
                                reservationDAO,
                                conflictingReservation,
                                allRooms,
                                allEnseignants,
                                (success, message) -> {
                                    // Callback for when the conflicting dialog closes
                                    // Similar to Admin, for now, just acknowledge.
                                    if (callback != null) {
                                        // Forward the callback to the original dialog's callback if needed,
                                        // or just handle logging for now.
                                    }
                                },
                                currentHoD
                        );
                        conflictingReservationDialog.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(this, "Impossible de trouver la réservation en conflit.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
                // No need for pane.setValue("OK"); anymore as we explicitly dispose
            });
            pane.setOptions(new Object[]{viewReservationButton, okButton}); // Use our custom buttons
        }
        
        dialog.setVisible(true); // This blocks until dialog is dismissed by dispose()
    }
}
