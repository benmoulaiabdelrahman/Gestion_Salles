package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.NiveauDAO;
import com.gestion.salles.dao.BlocDAO;
import com.gestion.salles.models.Niveau;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.Bloc;
import com.gestion.salles.utils.ThemeConstants;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent; // Added import for ItemEvent
import java.util.List;
import java.util.ArrayList; // Added import for ArrayList
import java.util.stream.Collectors; // Added import for Collectors
import com.gestion.salles.utils.DialogCallback; // Import DialogCallback
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.views.shared.management.FormValidationUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for adding or editing a Niveau.
 *
 * @author Gemini
 * @version 1.0
 */
public class NiveauDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(NiveauDialog.class.getName());
    private static final int MIN_DIALOG_WIDTH = 560;

    private final NiveauDAO niveauDAO;
    private final BlocDAO blocDAO;
    private Niveau currentNiveau;
    private boolean isEditMode;
    private DialogCallback callback; 
    
    private List<Departement> allDepartments;
    private List<Niveau> allNiveaux;
    private List<Bloc> allBlocs; // New field to store all blocs
    private boolean isUpdatingComboBoxes = false;
    private boolean scopeLocked = false;
    private Integer lockedDepartementId;
    private Integer lockedBlocId;

    private JTextField txtNom;
    private JTextField txtCode;
    private JComboBox<Departement> cmbDepartement;
    private JComboBox<com.gestion.salles.models.Bloc> cmbBloc;
    private JSpinner spnNombreEtudiants;
    private JSpinner spnNombreGroupes; // New spinner for groups
    private JTextField txtAnneeAcademique;
    private JCheckBox chkActif;

    private JLabel lblErrorNom;
    private JLabel lblErrorCode;
    private JLabel lblErrorDepartement;
    private JLabel lblErrorBloc;
    private JLabel lblErrorNombreEtudiants;
    private JLabel lblErrorNombreGroupes; // New error label for groups
    private JLabel lblErrorAnneeAcademique;
    private JLabel lblDepartement;
    private JLabel lblBloc;


    public NiveauDialog(Frame owner, NiveauDAO niveauDAO, List<Departement> allDepartments, BlocDAO blocDAO, List<Niveau> allNiveaux, Niveau niveau, DialogCallback callback) { // Modified constructor signature
        this(owner, niveauDAO, allDepartments, blocDAO, allNiveaux, niveau, callback, false, null, null);
    }

    public NiveauDialog(Frame owner, NiveauDAO niveauDAO, List<Niveau> allNiveaux, Niveau niveau, DialogCallback callback, int lockedDepartementId, Integer lockedBlocId) {
        this(owner, niveauDAO, java.util.Collections.emptyList(), null, allNiveaux, niveau, callback, true, lockedDepartementId, lockedBlocId);
    }

    public NiveauDialog(Frame owner, NiveauDAO niveauDAO, List<Departement> allDepartments, BlocDAO blocDAO, List<Niveau> allNiveaux, Niveau niveau, DialogCallback callback, boolean scopeLocked, Integer lockedDepartementId, Integer lockedBlocId) {
        super(owner, true);
        this.niveauDAO = niveauDAO;
        this.blocDAO = blocDAO; // Assign blocDAO from parameter
        this.allDepartments = allDepartments;
        this.allNiveaux = allNiveaux;
        this.currentNiveau = niveau;
        this.isEditMode = (niveau != null);
        this.callback = callback;
        this.scopeLocked = scopeLocked;
        this.lockedDepartementId = lockedDepartementId;
        this.lockedBlocId = lockedBlocId;

        initComponents();
        pack();
        int width = Math.max(getWidth(), MIN_DIALOG_WIDTH);
        setSize(new Dimension(width, getHeight()));
        setMinimumSize(new Dimension(width, getHeight()));
        setLocationRelativeTo(null);
        loadData(); // Call a new method to handle SwingWorker
    }
    
    private void loadData() {
        if (scopeLocked) {
            if (lockedDepartementId == null || lockedBlocId == null) {
                LOGGER.log(Level.SEVERE, "Locked departement/bloc ids are required when scope is locked.");
                JOptionPane.showMessageDialog(this,
                        "Erreur: informations de périmètre manquantes.",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            if (isEditMode) {
                setTitle("Modifier le Niveau");
                populateFields();
            } else {
                setTitle("Ajouter un Nouveau Niveau");
            }
            if (!isVisible()) {
                pack();
                setLocationRelativeTo(null);
            }
            return;
        }

        if (blocDAO == null) {
            LOGGER.log(Level.SEVERE, "BlocDAO is required when scope is not locked.");
            JOptionPane.showMessageDialog(this,
                    "Erreur: configuration invalide. Veuillez réessayer.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        SwingWorker<List<Bloc>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Bloc> doInBackground() throws Exception {
                // Fetch all active blocs off the EDT
                return blocDAO.getAllActiveBlocs();
            }

            @Override
            protected void done() {
                try {
                    allBlocs = get(); // Assign fetched blocs to class member
                    
                    populateComboBoxes();

                    if (isEditMode) {
                        setTitle("Modifier le Niveau");
                        populateFields();
                    } else {
                        setTitle("Ajouter un Nouveau Niveau");
                    }
                    revalidate();
                    repaint();
                    if (!isVisible()) {
                        pack();
                        setLocationRelativeTo(null);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du chargement des données initiales.", e);
                    JOptionPane.showMessageDialog(NiveauDialog.this,
                            "Erreur lors du chargement des données initiales: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        };
        worker.execute();
    }


    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 20", "[pref!, grow 0][fill,grow]"));
        formPanel.setBackground(ThemeConstants.CARD_WHITE);

        // Form Fields
        txtNom = UIUtils.createStyledTextField("Ex: Licence 1 Informatique");
        txtCode = UIUtils.createStyledTextField("Ex: L1-INFO");
        cmbDepartement = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbBloc = UIUtils.createStyledComboBox(new JComboBox<>());
        spnNombreEtudiants = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        spnNombreGroupes = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)); // New spinner for Nombre de groupes
        txtAnneeAcademique = UIUtils.createStyledTextField("Ex: 2024-2025");
        chkActif = new JCheckBox("Niveau actif");
        chkActif.setSelected(true);
        chkActif.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));

        // Initialize error labels
        lblErrorNom = new JLabel("Nom du niveau obligatoire.");
        lblErrorNom.setForeground(ThemeConstants.ERROR_RED);
        lblErrorNom.setVisible(false);
        lblErrorCode = new JLabel("Code du niveau obligatoire.");
        lblErrorCode.setForeground(ThemeConstants.ERROR_RED);
        lblErrorCode.setVisible(false);
        lblErrorDepartement = new JLabel("Faculté obligatoire.");
        lblErrorDepartement.setForeground(ThemeConstants.ERROR_RED);
        lblErrorDepartement.setVisible(false);
        lblErrorBloc = new JLabel("Département obligatoire.");
        lblErrorBloc.setForeground(ThemeConstants.ERROR_RED);
        lblErrorBloc.setVisible(false);
        lblErrorNombreEtudiants = new JLabel("Nombre d'étudiants ne peut pas être négatif.");
        lblErrorNombreEtudiants.setForeground(ThemeConstants.ERROR_RED);
        lblErrorNombreEtudiants.setVisible(false);
        lblErrorNombreGroupes = new JLabel("Nombre de groupes doit être supérieur à zéro."); // New error label
        lblErrorNombreGroupes.setForeground(ThemeConstants.ERROR_RED);
        lblErrorNombreGroupes.setVisible(false);
        lblErrorAnneeAcademique = new JLabel("Format d'année académique invalide (YYYY-YYYY).");
        lblErrorAnneeAcademique.setForeground(ThemeConstants.ERROR_RED);
        lblErrorAnneeAcademique.setVisible(false);


        // Styling
        spnNombreEtudiants.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN));
        spnNombreGroupes.putClientProperty(FlatClientProperties.STYLE, "arc:10;" +
                "focusedBorderColor:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN)); // Styling for new spinner

        int rowIndex = 0;
        formPanel.add(new JLabel("Nom du Niveau:"), "cell 0 " + rowIndex);
        formPanel.add(txtNom, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorNom, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Code du Niveau:"), "cell 0 " + rowIndex);
        formPanel.add(txtCode, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorCode, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        if (!scopeLocked) {
            lblDepartement = new JLabel("Faculté:");
            formPanel.add(lblDepartement, "cell 0 " + rowIndex);
            formPanel.add(cmbDepartement, "cell 1 " + rowIndex + ", growx, h 35!");
            rowIndex++;
            formPanel.add(lblErrorDepartement, "cell 1 " + rowIndex + ", gapy 0");
            rowIndex++;

            lblBloc = new JLabel("Département:");
            formPanel.add(lblBloc, "cell 0 " + rowIndex);
            formPanel.add(cmbBloc, "cell 1 " + rowIndex + ", growx, h 35!");
            rowIndex++;
            formPanel.add(lblErrorBloc, "cell 1 " + rowIndex + ", gapy 0");
            rowIndex++;
        }

        formPanel.add(new JLabel("Nombre d'Étudiants:"), "cell 0 " + rowIndex);
        formPanel.add(spnNombreEtudiants, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorNombreEtudiants, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Nombre de Groupes:"), "cell 0 " + rowIndex); // New label
        formPanel.add(spnNombreGroupes, "cell 1 " + rowIndex + ", growx, h 35!"); // New spinner
        rowIndex++;
        formPanel.add(lblErrorNombreGroupes, "cell 1 " + rowIndex + ", gapy 0"); // New error label
        rowIndex++;
        
        formPanel.add(new JLabel("Année Académique:"), "cell 0 " + rowIndex);
        formPanel.add(txtAnneeAcademique, "cell 1 " + rowIndex + ", growx, h 35!");
        rowIndex++;
        formPanel.add(lblErrorAnneeAcademique, "cell 1 " + rowIndex + ", gapy 0");
        rowIndex++;

        formPanel.add(new JLabel("Statut:"), "cell 0 " + rowIndex);
        formPanel.add(chkActif, "cell 1 " + rowIndex + ", growx, wrap");
        // rowIndex++; // Removed as it's not used after this point

        // Buttons
        JButton btnSave = UIUtils.createPrimaryButton("Enregistrer");
        JButton btnCancel = UIUtils.createSecondaryButton("Annuler");


        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());

        formPanel.add(new JLabel(""), "span, growx, pushx");
        formPanel.add(btnSave, "split 2, right, gapright 5");
        formPanel.add(btnCancel, "right");

        add(formPanel, BorderLayout.CENTER);

        if (scopeLocked) {
            applyScopeLock();
        }
    }

    private void populateComboBoxes() {
        // Populate cmbDepartement (Faculté)
        DefaultComboBoxModel<Departement> deptModel = new DefaultComboBoxModel<>();
        deptModel.addElement(new Departement("Sélectioner...", 0)); // Placeholder
        if (allDepartments != null) {
            for (Departement dept : allDepartments) {
                deptModel.addElement(dept);
            }
        }
        cmbDepartement.setModel(deptModel);

        // Add ItemListener to cmbDepartement to dynamically populate cmbBloc
        cmbDepartement.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                populateBlocsComboBox((Departement) cmbDepartement.getSelectedItem());
            }
        });

        // Initial population of cmbBloc
        if (isEditMode && currentNiveau.getIdDepartement() != 0) {
            // Select the current niveau's departement in cmbDepartement first
            for (int i = 0; i < cmbDepartement.getItemCount(); i++) {
                Departement dept = cmbDepartement.getItemAt(i);
                if (dept != null && dept.getId() == currentNiveau.getIdDepartement()) {
                    cmbDepartement.setSelectedItem(dept);
                    break;
                }
            }
        } else {
            // Default to the placeholder and populate cmbBloc with all blocs initially
            populateBlocsComboBox(null); // Pass null to show all or placeholder
        }
    }

    private void populateBlocsComboBox(Departement selectedDepartement) {
        DefaultComboBoxModel<Bloc> blocModel = new DefaultComboBoxModel<>();
        blocModel.addElement(new Bloc("Sélectioner...", 0)); // Placeholder

        List<Bloc> blocsToDisplay = new ArrayList<>();
        if (selectedDepartement != null && selectedDepartement.getId() != 0) {
            // Filter from the pre-fetched allBlocs list
            blocsToDisplay = allBlocs.stream()
                .filter(bloc -> bloc.getDepartement() != null && bloc.getDepartement().getId() == selectedDepartement.getId())
                .collect(Collectors.toList());
        } else {
            // If no department selected, show all blocs (from the pre-fetched list)
            blocsToDisplay = new ArrayList<>(allBlocs);
        }
        
        if (blocsToDisplay != null) {
            for (Bloc bloc : blocsToDisplay) {
                blocModel.addElement(bloc);
            }
        }
        cmbBloc.setModel(blocModel);

        // In edit mode, try to re-select the current Niveau's bloc if it's in the filtered list
        if (isEditMode && currentNiveau.getIdBloc() != null && currentNiveau.getIdBloc() != 0) {
            for (int i = 0; i < cmbBloc.getItemCount(); i++) {
                Bloc bloc = cmbBloc.getItemAt(i);
                if (bloc != null && bloc.getId() != 0 && bloc.getId() == currentNiveau.getIdBloc()) {
                    cmbBloc.setSelectedItem(bloc);
                    break;
                }
            }
        }
    }

    private void populateFields() {
        if (currentNiveau != null) {
            txtNom.setText(currentNiveau.getNom());
            txtCode.setText(currentNiveau.getCode());
            spnNombreEtudiants.setValue(currentNiveau.getNombreEtudiants());
            spnNombreGroupes.setValue(currentNiveau.getNombreGroupes()); // NEW
            txtAnneeAcademique.setText(currentNiveau.getAnneeAcademique());
            chkActif.setSelected(currentNiveau.isActif());

            if (scopeLocked) {
                return;
            }

            // Select Department
            if (currentNiveau.getIdDepartement() > 0) {
                for (int i = 0; i < cmbDepartement.getItemCount(); i++) {
                    Departement dept = cmbDepartement.getItemAt(i);
                    if (dept != null && dept.getId() == currentNiveau.getIdDepartement()) {
                        cmbDepartement.setSelectedItem(dept);
                        break;
                    }
                }
            } else {
                cmbDepartement.setSelectedIndex(0);
            }

            // Select Bloc
            if (currentNiveau.getIdBloc() != null && currentNiveau.getIdBloc() > 0) {
                for (int i = 0; i < cmbBloc.getItemCount(); i++) {
                    Bloc bloc = cmbBloc.getItemAt(i);
                    if (bloc != null && bloc.getId() != 0 && bloc.getId() == currentNiveau.getIdBloc()) {
                        cmbBloc.setSelectedItem(bloc);
                        break;
                    }
                }
            } else {
                cmbBloc.setSelectedIndex(0);
            }
        }
    }

    private void onSave() {
        if (!validateForm()) {
            return;
        }

        Niveau niveauToSave = isEditMode ? currentNiveau : new Niveau();
        niveauToSave.setNom(txtNom.getText().trim());
        niveauToSave.setCode(txtCode.getText().trim());

        if (scopeLocked) {
            niveauToSave.setIdDepartement(lockedDepartementId != null ? lockedDepartementId : 0);
        } else {
            Departement selectedDept = (Departement) cmbDepartement.getSelectedItem();
            if (selectedDept != null && selectedDept.getId() != 0) {
                niveauToSave.setIdDepartement(selectedDept.getId());
            } else {
                niveauToSave.setIdDepartement(0); 
            }
        }

        if (scopeLocked) {
            niveauToSave.setIdBloc(lockedBlocId);
        } else {
            Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
            if (selectedBloc != null && selectedBloc.getId() != 0) {
                niveauToSave.setIdBloc(selectedBloc.getId());
            } else {
                niveauToSave.setIdBloc(null);
            }
        }

        niveauToSave.setNombreEtudiants((Integer) spnNombreEtudiants.getValue());
        niveauToSave.setNombreGroupes((Integer) spnNombreGroupes.getValue()); // NEW
        niveauToSave.setAnneeAcademique(txtAnneeAcademique.getText().trim());
        niveauToSave.setActif(chkActif.isSelected());

        boolean success;
        String message;
        if (isEditMode) {
            success = niveauDAO.updateNiveau(niveauToSave);
            message = success ? "Niveau modifié avec succès." : "Échec de la modification du niveau.";
        } else {
            success = niveauDAO.addNiveau(niveauToSave);
            message = success ? "Niveau ajouté avec succès." : "Échec de l'ajout du niveau.";
        }
        
        callback.onDialogClose(success, message);
        dispose();
    }
    
    private boolean validateForm() {
        boolean isValid = true;

        // --- Reset Error States ---
        FormValidationUtils.applyDefaultStyle(txtNom);
        lblErrorNom.setVisible(false);
        FormValidationUtils.applyDefaultStyle(txtCode);
        lblErrorCode.setVisible(false);
        FormValidationUtils.applyDefaultStyle(cmbDepartement);
        lblErrorDepartement.setVisible(false);
        FormValidationUtils.applyDefaultStyle(cmbBloc);
        lblErrorBloc.setVisible(false);
        FormValidationUtils.applyDefaultStyle(spnNombreEtudiants);
        lblErrorNombreEtudiants.setVisible(false);
        FormValidationUtils.applyDefaultStyle(spnNombreGroupes); // NEW
        lblErrorNombreGroupes.setVisible(false); // NEW
        FormValidationUtils.applyDefaultStyle(txtAnneeAcademique);
        lblErrorAnneeAcademique.setVisible(false);

        // --- Validate Nom ---
        String nom = txtNom.getText().trim();
        if (nom.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtNom);
            lblErrorNom.setText("Le nom du niveau est obligatoire.");
            lblErrorNom.setVisible(true);
            isValid = false;
        } else {
            boolean nameExists;
            if (isEditMode) {
                nameExists = niveauDAO.existsNiveauByName(nom, currentNiveau.getId());
            } else {
                nameExists = niveauDAO.existsNiveauByName(nom);
            }
            if (nameExists) {
                FormValidationUtils.applyErrorStyle(txtNom);
                lblErrorNom.setText("Un niveau avec ce nom existe déjà.");
                lblErrorNom.setVisible(true);
                isValid = false;
            }
        }

        // --- Validate Code ---
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtCode);
            lblErrorCode.setText("Le code du niveau est obligatoire.");
            lblErrorCode.setVisible(true);
            isValid = false;
        } else {
            boolean codeExists;
            if (isEditMode) {
                codeExists = niveauDAO.existsNiveauByCode(code, currentNiveau.getId());
            } else {
                codeExists = niveauDAO.existsNiveauByCode(code);
            }
            if (codeExists) {
                FormValidationUtils.applyErrorStyle(txtCode);
                lblErrorCode.setText("Un niveau avec ce code existe déjà.");
                lblErrorCode.setVisible(true);
                isValid = false;
            }
        }

        if (!scopeLocked) {
            // --- Validate Departement ---
            if (cmbDepartement.getSelectedItem() == null || ((Departement) cmbDepartement.getSelectedItem()).getId() == 0) {
                FormValidationUtils.applyErrorStyle(cmbDepartement);
                lblErrorDepartement.setText("La faculté est obligatoire.");
                lblErrorDepartement.setVisible(true);
                isValid = false;
            }

            // --- Validate Bloc ---
            if (cmbBloc.getSelectedItem() == null || ((Bloc) cmbBloc.getSelectedItem()).getId() == 0) {
                FormValidationUtils.applyErrorStyle(cmbBloc);
                lblErrorBloc.setText("Le département est obligatoire.");
                lblErrorBloc.setVisible(true);
                isValid = false;
            }
        }

        // --- Validate Nombre d'Étudiants ---
        if ((Integer) spnNombreEtudiants.getValue() < 0) {
            FormValidationUtils.applyErrorStyle(spnNombreEtudiants);
            lblErrorNombreEtudiants.setText("Le nombre d'étudiants ne peut pas être négatif.");
            lblErrorNombreEtudiants.setVisible(true);
            isValid = false;
        }

        // --- Validate Nombre de Groupes ---
        if ((Integer) spnNombreGroupes.getValue() <= 0) { // NEW
            FormValidationUtils.applyErrorStyle(spnNombreGroupes);
            lblErrorNombreGroupes.setText("Le nombre de groupes doit être supérieur à zéro.");
            lblErrorNombreGroupes.setVisible(true);
            isValid = false;
        }

        // --- Validate Année Académique (Optional, but check format if not empty) ---
        String anneeAcademique = txtAnneeAcademique.getText().trim();
        if (!anneeAcademique.isEmpty()) {
            // Basic regex for YYYY-YYYY format (e.g., 2023-2024)
            if (!anneeAcademique.matches("^\\d{4}-\\d{4}$")) {
                FormValidationUtils.applyErrorStyle(txtAnneeAcademique);
                lblErrorAnneeAcademique.setText("Format invalide (Ex: 2024-2025).");
                lblErrorAnneeAcademique.setVisible(true);
                isValid = false;
            }
        }

        return isValid;
    }
    

    private void applyScopeLock() {
        if (lblDepartement != null) lblDepartement.setVisible(false);
        if (cmbDepartement != null) cmbDepartement.setVisible(false);
        if (lblErrorDepartement != null) lblErrorDepartement.setVisible(false);

        if (lblBloc != null) lblBloc.setVisible(false);
        if (cmbBloc != null) cmbBloc.setVisible(false);
        if (lblErrorBloc != null) lblErrorBloc.setVisible(false);
    }

    
}
