package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.models.Departement;
import com.gestion.salles.utils.ThemeConstants;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import com.gestion.salles.utils.DialogCallback;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.management.FormValidationUtils;

/**
 * Dialog for adding or editing a Block.
 *
 * @author Gemini
 * @version 1.1
 */
public class BlocDialog extends JDialog {

    private JTextField txtName;
    private JTextField txtCode;
    private JTextArea txtAddress;
    private JSpinner spnFloors;
    private JCheckBox chkActive;
    private JComboBox<Departement> cmbDepartement;

    private JLabel lblErrorName;
    private JLabel lblErrorCode;
    private JLabel lblErrorNombreEtages;
    private JLabel lblErrorDepartement;

    private BlocDAO blocDAO;
    private DepartementDAO departementDAO;
    private Bloc currentBloc;
    private boolean isEditMode;
    private DialogCallback callback;
    private List<Departement> departements;

    public BlocDialog(Frame owner, BlocDAO blocDAO, Bloc bloc, DialogCallback callback) {
        super(owner, true);
        this.blocDAO = blocDAO;
        this.departementDAO = new DepartementDAO();
        this.currentBloc = bloc;
        this.isEditMode = (bloc != null);
        this.callback = callback;

        loadDepartements();
        initComponents();

        if (isEditMode) {
            setTitle("Modifier le Département");
            populateFields();
        } else {
            setTitle("Ajouter un Nouveau Département");
        }
        pack();
        setLocationRelativeTo(null);
    }

    private void loadDepartements() {
        this.departements = departementDAO.getAllActiveDepartements();
        // No placeholder object added here; the renderer will handle it.
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 20, gapy 8, gapx 10", "[pref!, grow 0][fill,grow]"));
        formPanel.setBackground(ThemeConstants.CARD_WHITE);

        // Form Fields
        txtName = UIUtils.createStyledTextField("Ex: Département A, Département B...");
        txtCode = UIUtils.createStyledTextField("Ex: DEPT_A, DEPT_B...");
        txtAddress = UIUtils.createStyledTextArea("Ex: Adresse du bloc...", 3, 20);
        JScrollPane scrollAddress = new JScrollPane(txtAddress);
        scrollAddress.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        spnFloors = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        spnFloors.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        chkActive = new JCheckBox("Département actif");
        chkActive.setSelected(true);
        chkActive.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        
        cmbDepartement = UIUtils.createStyledComboBox(new JComboBox<>(departements.toArray(new Departement[0])));
        cmbDepartement.setRenderer(new DepartementComboBoxRenderer("Sélectionner...")); // Set custom renderer
        cmbDepartement.setSelectedItem(null); // Initially select no item to show placeholder

        // Initialize error labels
        lblErrorName = new JLabel("Nom du département obligatoire.");
        lblErrorName.setForeground(ThemeConstants.ERROR_RED);
        lblErrorName.setVisible(false);
        lblErrorCode = new JLabel("Code du département obligatoire.");
        lblErrorCode.setForeground(ThemeConstants.ERROR_RED);
        lblErrorCode.setVisible(false);
        lblErrorNombreEtages = new JLabel("Nombre d'étages doit être >= 1.");
        lblErrorNombreEtages.setForeground(ThemeConstants.ERROR_RED);
        lblErrorNombreEtages.setVisible(false);
        lblErrorDepartement = new JLabel("Veuillez sélectionner une faculté.");
        lblErrorDepartement.setForeground(ThemeConstants.ERROR_RED);
        lblErrorDepartement.setVisible(false);

        int rowIndex = 0;
        formPanel.add(new JLabel("Nom du Département:"), "cell 0 " + rowIndex);
        formPanel.add(txtName, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorName, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Code:"), "cell 0 " + rowIndex);
        formPanel.add(txtCode, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorCode, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Adresse:"), "cell 0 " + rowIndex);
        formPanel.add(scrollAddress, "cell 1 " + rowIndex + ", growx, h 60");
        rowIndex++;

        formPanel.add(new JLabel("Nombre d'étages:"), "cell 0 " + rowIndex);
        formPanel.add(spnFloors, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorNombreEtages, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;
        
        formPanel.add(new JLabel("Faculté:"), "cell 0 " + rowIndex);
        formPanel.add(cmbDepartement, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorDepartement, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Statut:"), "cell 0 " + rowIndex);
        formPanel.add(chkActive, "cell 1 " + rowIndex + ", growx, wrap");


        // Buttons
        JButton btnSave = UIUtils.createPrimaryButton("Enregistrer");
        JButton btnCancel = UIUtils.createSecondaryButton("Annuler");

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());

        formPanel.add(new JLabel(""), "span, growx, pushx");
        formPanel.add(btnSave, "split 2, right, gapright 5");
        formPanel.add(btnCancel, "right");

        add(formPanel, BorderLayout.CENTER);
    }

    private void populateFields() {
        if (currentBloc != null) {
            txtName.setText(currentBloc.getNom());
            txtCode.setText(currentBloc.getCode());
            txtAddress.setText(currentBloc.getAdresse());
            spnFloors.setValue(currentBloc.getNombreEtages());
            chkActive.setSelected(currentBloc.isActif()); // Corrected to getActif()
            
            if (currentBloc.getDepartement() != null) {
                cmbDepartement.setSelectedItem(currentBloc.getDepartement());
            } else {
                cmbDepartement.setSelectedItem(null); // Display placeholder
            }
        }
    }

    private void onSave() {
        if (!validateForm()) {
            return;
        }

        Bloc blocToSave = isEditMode ? currentBloc : new Bloc();
        blocToSave.setNom(txtName.getText().trim());
        blocToSave.setCode(txtCode.getText().trim());
        blocToSave.setAdresse(txtAddress.getText().trim());
        blocToSave.setNombreEtages((Integer) spnFloors.getValue());
        blocToSave.setActif(chkActive.isSelected());
        
        Departement selectedDepartement = (Departement) cmbDepartement.getSelectedItem();
        if (selectedDepartement != null) { // Check if a Departement is actually selected (not null/placeholder)
            blocToSave.setDepartement(selectedDepartement);
        } else {
            blocToSave.setDepartement(null);
        }

        boolean success;
        String message;
        if (isEditMode) {
            success = blocDAO.updateBloc(blocToSave);
            message = success ? "Département modifié avec succès." : "Échec de la modification du département.";
        } else {
            success = blocDAO.addBloc(blocToSave);
            message = success ? "Département ajouté avec succès." : "Échec de l'ajout du département.";
        }
        
        callback.onDialogClose(success, message);
        dispose();
    }
    
    private boolean validateForm() {
        boolean isValid = true;

        // --- Reset Error States ---
        FormValidationUtils.applyDefaultStyle(txtName);
        lblErrorName.setVisible(false);
        FormValidationUtils.applyDefaultStyle(txtCode);
        lblErrorCode.setVisible(false);
        FormValidationUtils.applyDefaultStyle(spnFloors);
        lblErrorNombreEtages.setVisible(false);
        FormValidationUtils.applyDefaultStyle(cmbDepartement);
        lblErrorDepartement.setVisible(false);

        // --- Validate Name ---
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtName);
            lblErrorName.setText("Le nom du bloc est obligatoire.");
            lblErrorName.setVisible(true);
            isValid = false;
        } else {
            boolean nameExists = blocDAO.existsBlocByName(name, isEditMode ? currentBloc.getId() : null);
            if (nameExists) {
                FormValidationUtils.applyErrorStyle(txtName);
                lblErrorName.setText("Un département avec ce nom existe déjà.");
                lblErrorName.setVisible(true);
                isValid = false;
            }
        }

        // --- Validate Code ---
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtCode);
            lblErrorCode.setText("Le code du bloc est obligatoire.");
            lblErrorCode.setVisible(true);
            isValid = false;
        } else {
            boolean codeExists = blocDAO.existsBlocByCode(code, isEditMode ? currentBloc.getId() : null);
            if (codeExists) {
                FormValidationUtils.applyErrorStyle(txtCode);
                lblErrorCode.setText("Un département avec ce code existe déjà.");
                lblErrorCode.setVisible(true);
                isValid = false;
            }
        }

        // --- Validate Nombre d'étages ---
        if ((Integer) spnFloors.getValue() < 1) {
            FormValidationUtils.applyErrorStyle(spnFloors);
            lblErrorNombreEtages.setText("Le nombre d'étages doit être >= 1.");
            lblErrorNombreEtages.setVisible(true);
            isValid = false;
        }

        // --- Validate Departement selection ---
        if (cmbDepartement.getSelectedItem() == null) { // Check for null (placeholder selected)
            FormValidationUtils.applyErrorStyle(cmbDepartement);
            lblErrorDepartement.setVisible(true);
            isValid = false;
        }
        
        return isValid;
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
