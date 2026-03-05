package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.DepartementDAO;
import com.gestion.salles.models.Departement;
import com.gestion.salles.utils.ThemeConstants;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import com.gestion.salles.utils.DialogCallback; // Import DialogCallback
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.management.FormValidationUtils;

/**
 * Dialog for adding or editing a Department.
 *
 * @author Gemini
 * @version 1.0
 */
public class DepartementDialog extends JDialog {

    private JTextField txtName;
    private JTextField txtCode;
    private JTextArea txtDescription;
    private JCheckBox chkActive;

    private JLabel lblErrorName;
    private JLabel lblErrorCode;

    private DepartementDAO departementDAO;
    private Departement currentDepartement;
    private boolean isEditMode;
    private DialogCallback callback; // Changed from Runnable onSave

    public DepartementDialog(Frame owner, DepartementDAO departementDAO, Departement departement, DialogCallback callback) {
        super(owner, true);
        this.departementDAO = departementDAO;
        this.currentDepartement = departement;
        this.isEditMode = (departement != null);
        this.callback = callback;

        initComponents();

        if (isEditMode) {
            setTitle("Modifier la Faculté");
            populateFields();
        } else {
            setTitle("Ajouter une Nouvelle Faculté");
        }
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 20, gapy 8, gapx 10", "[pref!, grow 0][fill,grow]"));
        formPanel.setBackground(ThemeConstants.CARD_WHITE);

        // Form Fields
        txtName = UIUtils.createStyledTextField("Ex: Informatique, Mathématiques...");
        txtCode = UIUtils.createStyledTextField("Ex: INFO, MATH...");
        txtDescription = UIUtils.createStyledTextArea("Ex: Description de la faculté...", 3, 20);
        JScrollPane scrollDescription = new JScrollPane(txtDescription);
        chkActive = new JCheckBox("Faculté active");
        chkActive.setSelected(true);
        chkActive.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));

        // Initialize error labels
        lblErrorName = new JLabel("Nom de la faculté obligatoire.");
        lblErrorName.setForeground(ThemeConstants.ERROR_RED);
        lblErrorName.setVisible(false);
        lblErrorCode = new JLabel("Code de la faculté obligatoire.");
        lblErrorCode.setForeground(ThemeConstants.ERROR_RED);
        lblErrorCode.setVisible(false);

        // Styling
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex: Informatique, Mathématiques...");
        txtName.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        txtCode.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex: INFO, MATH...");
        txtCode.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        scrollDescription.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        int rowIndex = 0;


        formPanel.add(new JLabel("Nom de la Faculté:"), "cell 0 " + rowIndex);
        formPanel.add(txtName, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorName, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Code:"), "cell 0 " + rowIndex);
        formPanel.add(txtCode, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorCode, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Description:"), "cell 0 " + rowIndex);
        formPanel.add(scrollDescription, "cell 1 " + rowIndex + ", growx, h 60");
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
        if (currentDepartement != null) {
            txtName.setText(currentDepartement.getNom());
            txtCode.setText(currentDepartement.getCode());
            txtDescription.setText(currentDepartement.getDescription());
            chkActive.setSelected(currentDepartement.isActif());
        }
    }

    private void onSave() {
        if (!validateForm()) {
            return;
        }

        Departement deptToSave = isEditMode ? currentDepartement : new Departement();
        deptToSave.setNom(txtName.getText().trim());
        deptToSave.setCode(txtCode.getText().trim());
        deptToSave.setDescription(txtDescription.getText().trim());
        deptToSave.setActif(chkActive.isSelected());

        boolean success;
        String message;
        if (isEditMode) {
            success = departementDAO.updateDepartement(deptToSave);
            message = success ? "Faculté modifiée avec succès." : "Échec de la modification de la faculté.";
        } else {
            success = departementDAO.addDepartement(deptToSave);
            message = success ? "Faculté ajoutée avec succès." : "Échec de l'ajout de la faculté.";
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

        // --- Validate Name ---
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtName);
            lblErrorName.setText("Le nom de la faculté est obligatoire.");
            lblErrorName.setVisible(true);
            isValid = false;
        } else {
            // Client-side uniqueness check for Name
            boolean nameExists = departementDAO.existsDepartementByName(name, isEditMode ? currentDepartement.getId() : null);
            if (nameExists) {
                FormValidationUtils.applyErrorStyle(txtName);
                lblErrorName.setText("Une faculté avec ce nom existe déjà.");
                lblErrorName.setVisible(true);
                isValid = false;
            }
        }

        // --- Validate Code ---
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtCode);
            lblErrorCode.setText("Le code de la faculté est obligatoire.");
            lblErrorCode.setVisible(true);
            isValid = false;
        } else {
            // Client-side uniqueness check for Code
            boolean codeExists = departementDAO.existsDepartementByCode(code, isEditMode ? currentDepartement.getId() : null);
            if (codeExists) {
                FormValidationUtils.applyErrorStyle(txtCode);
                lblErrorCode.setText("Une faculté avec ce code existe déjà.");
                lblErrorCode.setVisible(true);
                isValid = false;
            }
        }
        
        return isValid;
    }
    


    // Uses FormValidationUtils for input error styles

}
