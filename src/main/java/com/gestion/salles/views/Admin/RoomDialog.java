package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.RoomDAO;
import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Room;
import com.gestion.salles.utils.ThemeConstants;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.Gson; // For JSON handling
import com.google.gson.reflect.TypeToken; // For JSON handling
import java.lang.reflect.Type; // For JSON handling
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gestion.salles.utils.DialogCallback;
import com.gestion.salles.utils.UIUtils;

/**
 * Dialog for adding or editing a Room.
 *
 * @author Gemini
 * @version 1.3 - Refactored to match BlocDialog styling and layout.
 */
public class RoomDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(RoomDialog.class.getName());

    private final RoomDAO roomDAO;
    private final BlocDAO blocDAO;
    private final DepartementDAO departementDAO;
    private Room currentRoom;
    private boolean isEditMode;
    private DialogCallback callback; 
    
    private List<Bloc> allBlocs;
    private List<Departement> allDepartments;
    private List<Room> allRooms;
    private boolean isUpdatingComboBoxes = false;
    private boolean scopeLocked = false;

    private final Gson gson = new Gson();

    public RoomDialog(Frame owner, RoomDAO roomDAO, BlocDAO blocDAO, DepartementDAO departementDAO, Room room, List<Bloc> blocs, List<Departement> departments, List<Room> allRooms, DialogCallback callback) {
        this(owner, roomDAO, blocDAO, departementDAO, room, blocs, departments, allRooms, callback, false);
    }

    public RoomDialog(Frame owner, RoomDAO roomDAO, BlocDAO blocDAO, DepartementDAO departementDAO, Room room, List<Bloc> blocs, List<Departement> departments, List<Room> allRooms, DialogCallback callback, boolean scopeLocked) {
        super(owner, true);
        this.roomDAO = roomDAO;
        this.blocDAO = blocDAO;
        this.departementDAO = departementDAO;
        this.currentRoom = room;
        this.isEditMode = (room != null);
        this.callback = callback;
        this.allBlocs = blocs;
        this.allDepartments = departments;
        this.allRooms = allRooms;
        this.scopeLocked = scopeLocked;

        initComponents();
        populateComboBoxes();
        if (scopeLocked) {
            applyScopeLock();
        }

        if (allBlocs == null || allBlocs.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "Il n'y a aucun département de défini dans le système.\nVeuillez d'abord ajouter un département via le panneau 'Gestion des Départements'.",
                    "Aucun bloc trouvé",
                    JOptionPane.WARNING_MESSAGE);
                dispose(); 
                callback.onDialogClose(false, "Aucun bloc défini, opération annulée.");
            });
            return;
        }

        if (isEditMode) {
            setTitle("Modifier la Salle");
            populateFields();
        } else {
            setTitle("Ajouter une Nouvelle Salle");
        }
        pack();
        setLocationRelativeTo(null);
    }
    
    private JTextField txtName;
    private JComboBox<Bloc> cmbBloc;
    private JSpinner spnCapacity;
    private JComboBox<String> cmbRoomType;
    private JSpinner spnFloor;
    private JComboBox<Departement> cmbDepartment;
    private JLabel lblDepartment;
    private JLabel lblBloc;
    private JCheckBox chkActive;
    private JTextArea txtObservations;
    private JLabel lblErrorName;
    private JLabel lblErrorCapacity;
    private JLabel lblErrorBloc;
    private JLabel lblErrorDepartment;
    private JCheckBox chkProjector;
    private JCheckBox chkScreen;
    private JCheckBox chkTableau;
    private JCheckBox chkComputers;
    private JCheckBox chkInternet;
    private JCheckBox chkAudio;
    private JCheckBox chkVideoconference;
    private JCheckBox chkLaboratory;



    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 20, hidemode 3, gapy 8, gapx 10", "[pref!, grow 0][fill,grow]"));
        formPanel.setBackground(ThemeConstants.CARD_WHITE);

        txtName = UIUtils.createStyledTextField("Ex: S20, Amphi A, ...");
        cmbBloc = UIUtils.createStyledComboBox(new JComboBox<>());
        spnCapacity = new JSpinner(new SpinnerNumberModel(30, 1, 1000, 1));
        cmbRoomType = UIUtils.createStyledComboBox(new JComboBox<>(new String[]{"Amphi", "Réunion", "TD", "TP"}));
        spnFloor = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
        cmbDepartment = UIUtils.createStyledComboBox(new JComboBox<>());
        chkActive = new JCheckBox("Salle active");
        chkActive.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        txtObservations = UIUtils.createStyledTextArea("Observations...", 3, 20);
        JScrollPane scrollObservations = new JScrollPane(txtObservations);
        
        chkActive.setSelected(true);
        spnCapacity.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        spnFloor.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        scrollObservations.putClientProperty(FlatClientProperties.STYLE, "arc:10;");
        
        lblErrorName = new JLabel(" ");
        lblErrorName.setForeground(ThemeConstants.ERROR_RED);
        lblErrorCapacity = new JLabel(" ");
        lblErrorCapacity.setForeground(ThemeConstants.ERROR_RED);
        lblErrorBloc = new JLabel(" ");
        lblErrorBloc.setForeground(ThemeConstants.ERROR_RED);
        lblErrorDepartment = new JLabel(" ");
        lblErrorDepartment.setForeground(ThemeConstants.ERROR_RED);
        
        formPanel.add(new JLabel("Numéro/Nom de la Salle:"), "cell 0 0");
        formPanel.add(txtName, "cell 1 0, growx, h 35!");
        formPanel.add(lblErrorName, "cell 1 1, gapy 0");
        
        lblDepartment = new JLabel("Faculté:");
        formPanel.add(lblDepartment, "cell 0 2");
        formPanel.add(cmbDepartment, "cell 1 2, growx, h 35!");
        formPanel.add(lblErrorDepartment, "cell 1 3, gapy 0");
        
        lblBloc = new JLabel("Département:");
        formPanel.add(lblBloc, "cell 0 4");
        formPanel.add(cmbBloc, "cell 1 4, growx, h 35!");
        formPanel.add(lblErrorBloc, "cell 1 5, gapy 0");
        
        formPanel.add(new JLabel("Capacité:"), "cell 0 6");
        formPanel.add(spnCapacity, "cell 1 6, growx, h 35!");
        formPanel.add(lblErrorCapacity, "cell 1 7, gapy 0");
        
        formPanel.add(new JLabel("Type de Salle:"), "cell 0 8");
        formPanel.add(cmbRoomType, "cell 1 8, growx, h 35!");
        
        formPanel.add(new JLabel("Étage:"), "cell 0 9");
        formPanel.add(spnFloor, "cell 1 9, growx, h 35!");

        JPanel equipmentPanel = new JPanel(new MigLayout("wrap 4, fillx"));
        equipmentPanel.setOpaque(false);
        chkProjector = new JCheckBox("Projecteur");
        chkProjector.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkScreen = new JCheckBox("Écran");
        chkScreen.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkTableau = new JCheckBox("Tableau");
        chkTableau.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkComputers = new JCheckBox("Ordinateurs");
        chkComputers.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkInternet = new JCheckBox("Internet");
        chkInternet.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkAudio = new JCheckBox("Audio");
        chkAudio.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkVideoconference = new JCheckBox("Visioconférence");
        chkVideoconference.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        chkLaboratory = new JCheckBox("Laboratoire");
        chkLaboratory.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));

        equipmentPanel.add(chkProjector, "growx");
        equipmentPanel.add(chkScreen, "growx");
        equipmentPanel.add(chkTableau, "growx");
        equipmentPanel.add(chkComputers, "growx");
        equipmentPanel.add(chkInternet, "growx");
        equipmentPanel.add(chkAudio, "growx");
        equipmentPanel.add(chkVideoconference, "growx");
        equipmentPanel.add(chkLaboratory, "growx");

        formPanel.add(new JLabel("Équipements:"), "cell 0 10");
        formPanel.add(equipmentPanel, "cell 1 10, growx, span");

        formPanel.add(new JLabel("Observations:"), "cell 0 11");
        formPanel.add(scrollObservations, "cell 1 11, growx, h 60, span");
        
        formPanel.add(new JLabel("Statut:"), "cell 0 12");
        formPanel.add(chkActive, "cell 1 12, growx");

        JButton btnSave = UIUtils.createPrimaryButton("Enregistrer");
        JButton btnCancel = UIUtils.createSecondaryButton("Annuler");
		

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());

        
        formPanel.add(btnSave, "span 2, align left, split 2, gapy 15");
        formPanel.add(btnCancel, "align left");
        add(formPanel, BorderLayout.CENTER);
    }

    
    private void populateComboBoxes() {
        DefaultComboBoxModel<Bloc> blocModel = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<Departement> deptModel = new DefaultComboBoxModel<>();

        if (scopeLocked) {
            for (Bloc bloc : allBlocs) {
                blocModel.addElement(bloc);
            }
            for (Departement dept : allDepartments) {
                deptModel.addElement(dept);
            }
            cmbBloc.setModel(blocModel);
            cmbDepartment.setModel(deptModel);

            if (blocModel.getSize() > 0) {
                cmbBloc.setSelectedIndex(0);
            }
            if (deptModel.getSize() > 0) {
                cmbDepartment.setSelectedIndex(0);
            }
            return;
        }

        // Blocs
        blocModel.addElement(new Bloc(UIUtils.SELECT_PLACEHOLDER));
        for (Bloc bloc : allBlocs) {
            blocModel.addElement(bloc);
        }
        cmbBloc.setModel(blocModel);

        // Departments
        deptModel.addElement(new Departement(UIUtils.SELECT_PLACEHOLDER));
        for (Departement dept : allDepartments) {
            deptModel.addElement(dept);
        }
        cmbDepartment.setModel(deptModel);

        // Add listeners for dynamic filtering
        cmbDepartment.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !isUpdatingComboBoxes) {
                updateBlocComboBox();
            }
        });

        cmbBloc.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !isUpdatingComboBoxes) {
                updateDepartmentComboBox();
            }
        });
    }

    private void applyScopeLock() {
        if (lblDepartment != null) lblDepartment.setVisible(false);
        if (cmbDepartment != null) cmbDepartment.setVisible(false);
        if (lblErrorDepartment != null) lblErrorDepartment.setVisible(false);

        if (lblBloc != null) lblBloc.setVisible(false);
        if (cmbBloc != null) cmbBloc.setVisible(false);
        if (lblErrorBloc != null) lblErrorBloc.setVisible(false);

        if (cmbDepartment != null) cmbDepartment.setEnabled(false);
        if (cmbBloc != null) cmbBloc.setEnabled(false);
    }
    
    private void populateFields() {
        isUpdatingComboBoxes = true;
        try {
            if (currentRoom != null) {
                txtName.setText(currentRoom.getName());
                
                for (int i = 0; i < cmbBloc.getItemCount(); i++) {
                    if (cmbBloc.getItemAt(i).getId() == currentRoom.getIdBloc()) {
                        cmbBloc.setSelectedIndex(i);
                        break;
                    }
                }
                
                spnCapacity.setValue(currentRoom.getCapacity());
                cmbRoomType.setSelectedItem(currentRoom.getTypeSalle());
                spnFloor.setValue(currentRoom.getEtage());
                
                if (currentRoom.getIdDepartementPrincipal() != null) {
                    for (int i = 0; i < cmbDepartment.getItemCount(); i++) {
                        Departement dept = cmbDepartment.getItemAt(i);
                        if (dept != null && dept.getId() == currentRoom.getIdDepartementPrincipal()) {
                            cmbDepartment.setSelectedIndex(i);
                            break;
                        }
                    }
                } else {
                    cmbDepartment.setSelectedIndex(0);
                }
                
                if (currentRoom.getEquipment() != null && !currentRoom.getEquipment().isEmpty()) {
                    java.lang.reflect.Type type = new TypeToken<List<String>>() {}.getType();
                    List<String> equipmentList = gson.fromJson(currentRoom.getEquipment(), type);
                    
                    if (equipmentList != null) {
                        chkProjector.setSelected(equipmentList.contains("Projecteur"));
                        chkScreen.setSelected(equipmentList.contains("Écran"));
                        chkTableau.setSelected(equipmentList.contains("Tableau"));
                        chkComputers.setSelected(equipmentList.contains("Ordinateurs"));
                        chkInternet.setSelected(equipmentList.contains("Internet"));
                        chkAudio.setSelected(equipmentList.contains("Audio"));
                        chkVideoconference.setSelected(equipmentList.contains("Visioconférence"));
                        chkLaboratory.setSelected(equipmentList.contains("Laboratoire"));
                    }
                }
                
                txtObservations.setText(currentRoom.getObservations());
                chkActive.setSelected(currentRoom.isActif());
            }
        } finally {
            isUpdatingComboBoxes = false;
        }
    }

    private void onSave() {
        if (!validateForm()) {
            return;
        }
        
        Room roomToSave = isEditMode ? currentRoom : new Room();
        roomToSave.setName(txtName.getText().trim());
        roomToSave.setIdBloc(((Bloc) cmbBloc.getSelectedItem()).getId());
        roomToSave.setCapacity((Integer) spnCapacity.getValue());
        roomToSave.setTypeSalle((String) cmbRoomType.getSelectedItem());
        roomToSave.setEtage((Integer) spnFloor.getValue());
        
        Departement selectedDept = (Departement) cmbDepartment.getSelectedItem();
        if (selectedDept != null && selectedDept.getId() != 0) {
            roomToSave.setIdDepartementPrincipal(selectedDept.getId());
        } else {
            roomToSave.setIdDepartementPrincipal(null);
        }
        
        List<String> equipmentList = new ArrayList<>();
        if (chkProjector.isSelected()) equipmentList.add("Projecteur");
        if (chkScreen.isSelected()) equipmentList.add("Écran");
        if (chkTableau.isSelected()) equipmentList.add("Tableau");
        if (chkComputers.isSelected()) equipmentList.add("Ordinateurs");
        if (chkInternet.isSelected()) equipmentList.add("Internet");
        if (chkAudio.isSelected()) equipmentList.add("Audio");
        if (chkVideoconference.isSelected()) equipmentList.add("Visioconférence");
        if (chkLaboratory.isSelected()) equipmentList.add("Laboratoire");
        roomToSave.setEquipment(gson.toJson(equipmentList));
        
        roomToSave.setObservations(txtObservations.getText().trim());
        roomToSave.setActif(chkActive.isSelected());
        
        // No longer catching SQLException here, as RoomDAO methods handle them internally.
            if (isEditMode) {
                boolean success = roomDAO.updateRoom(roomToSave);
                if (success) {
                    callback.onDialogClose(true, "Salle modifiée avec succès.");
                    dispose();
                } else {
                    callback.onDialogClose(false, "Échec de la modification de la salle.");
                }
            } else {
                int newId = roomDAO.addRoom(roomToSave);
                if (newId != -1) {
                    callback.onDialogClose(true, "Salle ajoutée avec succès.");
                    dispose();
                } else {
                     callback.onDialogClose(false, "Échec de l'ajout de la salle.");
                }
            }
    }

    private boolean validateForm() {
        boolean isValid = true;

        // --- Reset Error States ---
        applyDefaultStyle(txtName);
        lblErrorName.setText(" ");
        lblErrorName.setVisible(false);
        applyDefaultStyle(spnCapacity);
        lblErrorCapacity.setText(" ");
        lblErrorCapacity.setVisible(false);

        applyDefaultStyle(cmbBloc);
        lblErrorBloc.setText(" ");
        lblErrorBloc.setVisible(false);
        applyDefaultStyle(cmbDepartment);
        lblErrorDepartment.setText(" ");
        lblErrorDepartment.setVisible(false);

        if (txtName.getText().trim().isEmpty()) {
            applyErrorStyle(txtName);
            lblErrorName.setText("Le nom de la salle est obligatoire.");
            lblErrorName.setVisible(true);
            isValid = false;
        } else {
            Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
            if (selectedBloc != null) {
                boolean nameExists = allRooms.stream()
                    .filter(r -> !isEditMode || r.getId() != currentRoom.getId())
                    .filter(r -> r.getName().equalsIgnoreCase(txtName.getText().trim()))
                    .filter(r -> r.getIdBloc() == selectedBloc.getId())
                    .findAny()
                    .isPresent();

                if (nameExists) {
                    applyErrorStyle(txtName);
                    lblErrorName.setText("Ce nom de salle existe déjà dans le département sélectionné.");
                    lblErrorName.setVisible(true);
                    isValid = false;
                }
            }
        }

        if ((Integer) spnCapacity.getValue() <= 0) {
            applyErrorStyle(spnCapacity);
            lblErrorCapacity.setText("La capacité doit être supérieure à 0.");
            lblErrorCapacity.setVisible(true);
            isValid = false;
        }

        if (cmbBloc.getSelectedItem() == null || ((Bloc) cmbBloc.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbBloc);
            lblErrorBloc.setText("Le département est obligatoire.");
            lblErrorBloc.setVisible(true);
            isValid = false;
        }

        if (cmbDepartment.getSelectedItem() != null && ((Departement) cmbDepartment.getSelectedItem()).getId() == 0) {
            applyErrorStyle(cmbDepartment);
            lblErrorDepartment.setText("La faculté est obligatoire.");
            lblErrorDepartment.setVisible(true);
            isValid = false;
        }
        
        return isValid;
    }

    private void updateBlocComboBox() {
        isUpdatingComboBoxes = true;
        try {
            Departement selectedDept = (Departement) cmbDepartment.getSelectedItem();
            DefaultComboBoxModel<Bloc> blocModel = new DefaultComboBoxModel<>();
            blocModel.addElement(new Bloc(UIUtils.SELECT_PLACEHOLDER)); // Add placeholder

            if (selectedDept != null && selectedDept.getId() != 0) { // Check if a specific department is selected
                List<Bloc> blocsForSelectedDept = blocDAO.getBlocsByDepartement(selectedDept.getId());
                for (Bloc bloc : blocsForSelectedDept) {
                    blocModel.addElement(bloc);
                }
            } else { // If "Sélectioner..." or no department is selected, show all blocs
                for (Bloc bloc : allBlocs) { // Assuming allBlocs holds all available blocs
                    blocModel.addElement(bloc);
                }
            }
            cmbBloc.setModel(blocModel);
        } finally {
            isUpdatingComboBoxes = false;
        }
    }

    private void updateDepartmentComboBox() {
        isUpdatingComboBoxes = true;
        try {
            Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
            if (selectedBloc != null && selectedBloc.getId() != 0) { // Check if a specific bloc is selected
                // Find the corresponding Departement object from allDepartments
                Departement associatedDept = allDepartments.stream()
                    .filter(dept -> selectedBloc.getDepartement() != null && dept.getId() == selectedBloc.getDepartement().getId())
                    .findFirst()
                    .orElse(null);

                if (associatedDept != null) {
                    cmbDepartment.setSelectedItem(associatedDept);
                } else {
                    cmbDepartment.setSelectedItem(new Departement(UIUtils.SELECT_PLACEHOLDER)); // If no department found, reset to placeholder
                }
            } else {
                // If "Sélectioner..." or no bloc is selected, reset department to placeholder
                cmbDepartment.setSelectedItem(new Departement(UIUtils.SELECT_PLACEHOLDER));
            }
        } finally {
            isUpdatingComboBoxes = false;
        }
    }
        
    private void applyErrorStyle(JComponent component) {
        if (component instanceof JTextField) {
            component.putClientProperty("JComponent.outline", "error"); // FlatLaf specific
        } else if (component instanceof JSpinner) {
            JComponent editor = ((JSpinner) component).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().putClientProperty("JComponent.outline", "error");
            }
        } else if (component instanceof JComboBox) {
             component.putClientProperty("JComponent.outline", "error"); // FlatLaf specific
        } else if (component instanceof JScrollPane) { // For JTextArea in JScrollPane
            component.putClientProperty("JComponent.outline", "error");
        }
    }

    private void applyDefaultStyle(JComponent component) {
        if (component instanceof JTextField) {
            component.putClientProperty("JComponent.outline", null);
        } else if (component instanceof JSpinner) {
            JComponent editor = ((JSpinner) component).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().putClientProperty("JComponent.outline", null);
            }
        } else if (component instanceof JComboBox) {
            component.putClientProperty("JComponent.outline", null);
        } else if (component instanceof JScrollPane) { // For JTextArea in JScrollPane
            component.putClientProperty("JComponent.outline", null);
        }
    }
    
    // Custom renderer for JComboBox to display placeholder text
    class DepartementComboBoxRenderer extends DefaultListCellRenderer {
        private String placeholder;

        public DepartementComboBoxRenderer(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText(placeholder);
                if (index == -1) { // When selected in the combobox itself
                    setForeground(UIManager.getColor("ComboBox.placeholderForeground"));
                } else { // In the dropdown list
                    setForeground(list.getForeground());
                }
            } else {
                Departement departement = (Departement) value;
                setText(departement.getNom());
                setForeground(list.getForeground());
            }
            return this;
        }
    }
}
