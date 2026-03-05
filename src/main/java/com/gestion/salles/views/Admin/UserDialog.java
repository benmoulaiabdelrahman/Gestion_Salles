package com.gestion.salles.views.Admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.dao.UserDAO;
import com.gestion.salles.models.Departement;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.AppConfig;
import com.gestion.salles.utils.PasswordUtils;
import com.gestion.salles.utils.RoundImage;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.services.EmailService; // Added missing import
import com.gestion.salles.dao.BlocDAO; // Import BlocDAO
import com.gestion.salles.models.Bloc; // Import Bloc
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent; // Added ItemEvent import
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gestion.salles.utils.UIUtils; // Added UIUtils import
import com.gestion.salles.utils.DialogCallback; // Import DialogCallback
import com.gestion.salles.views.shared.management.FormValidationUtils;

/**
 * Dialog for adding or editing a User.
 *
 * @author Gemini
 * @version 1.1 - Added Profile Picture Management
 */
public class UserDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(UserDialog.class.getName());
    private static final int BASE_DIALOG_WIDTH = 335;
    private static final int BASE_DIALOG_HEIGHT = 545;
    private static final int EXTENDED_DIALOG_WIDTH = 470;
    private static final int EXTENDED_DIALOG_HEIGHT = 630;
    private static final int EDIT_BASE_WIDTH_DELTA = 70;
    private static final int EDIT_BASE_HEIGHT_DELTA = 20;
    private static final int CHEF_ADD_HEIGHT = 500;
    private static final int CHEF_EDIT_HEIGHT = 500;
    private static final int CHEF_ADD_WIDTH = 340;
    private static final int CHEF_EDIT_WIDTH = 410;

    private JTextField txtNom;
    private JTextField txtPrenom;
    private JTextField txtEmail;
    private JComboBox<Object> cmbRole;
    private JComboBox<Departement> cmbDepartement;
    private JComboBox<Bloc> cmbBloc; // New JComboBox for Bloc
    private JTextField txtTelephone;
    private JCheckBox chkActif;
    private JLabel lblDepartement;
    private JLabel lblBloc; // New label for Bloc

    private JLabel lblErrorNom;
    private JLabel lblErrorPrenom;
    private JLabel lblErrorEmail;
    private JLabel lblErrorTelephone;
    private JLabel lblErrorRole;
    private JLabel lblErrorDepartement;
    private JLabel lblErrorBloc;
    private JLabel lblRole;

    // Profile picture components
    private RoundImage profileImagePreview;
    private JLabel lblPhotoProfil;
    private JPanel photoPanel;
    private JButton btnAttach;
    private JButton btnDelete;
    private String newProfileImageName = null;
    private boolean profileImageDeleted = false;

    private UserDAO userDAO;
    private BlocDAO blocDAO;
    private User currentUser;
    private boolean isEditMode;
    private DialogCallback callback; // Changed from Runnable onSave
    private List<Departement> allDepartments;
    private List<Bloc> allBlocs;
    private boolean scopeLocked = false;
    private User.Role lockedRole;
    private Integer lockedDepartementId;
    private Integer lockedBlocId;

    public UserDialog(Frame owner, UserDAO userDAO, BlocDAO blocDAO, List<Departement> allDepartments, List<Bloc> allBlocs, User user, DialogCallback callback) {
        this(owner, userDAO, blocDAO, allDepartments, allBlocs, user, callback, false, null, null, null);
    }

    public UserDialog(Frame owner, UserDAO userDAO, BlocDAO blocDAO, List<Departement> allDepartments, List<Bloc> allBlocs, User user,
                      DialogCallback callback, User.Role lockedRole, Integer lockedDepartementId, Integer lockedBlocId) {
        this(owner, userDAO, blocDAO, allDepartments, allBlocs, user, callback, true, lockedRole, lockedDepartementId, lockedBlocId);
    }

    private UserDialog(Frame owner, UserDAO userDAO, BlocDAO blocDAO, List<Departement> allDepartments, List<Bloc> allBlocs, User user,
                       DialogCallback callback, boolean scopeLocked, User.Role lockedRole, Integer lockedDepartementId, Integer lockedBlocId) {
        super(owner, true);
        this.userDAO = userDAO;
        this.blocDAO = blocDAO;
        this.allDepartments = allDepartments;
        this.allBlocs = allBlocs;
        this.currentUser = user;
        this.isEditMode = (user != null);
        this.callback = callback;
        this.scopeLocked = scopeLocked;
        this.lockedRole = lockedRole;
        this.lockedDepartementId = lockedDepartementId;
        this.lockedBlocId = lockedBlocId;

        initComponents();
        populateComboBoxes();

        if (scopeLocked && (lockedRole == null || lockedDepartementId == null || lockedBlocId == null)) {
            JOptionPane.showMessageDialog(this,
                    "Erreur: informations de périmètre manquantes.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        if (isEditMode) {
            setTitle("Modifier l'Utilisateur");
            populateFields();
        } else {
            setTitle("Ajouter un Nouvel Utilisateur");
        }
        pack();
        applyDialogSize(getTargetSizeForRole(getSelectedRole()));
        setLocationRelativeTo(null);
    }

    private JPanel formPanel; // Declared as a member variable to access components easily

    private void initComponents() {
                        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                        setModal(true);
                        setResizable(true); // Allow resizing to expand dynamically
                
                        setLayout(new BorderLayout(10, 10));
                
                        formPanel = new JPanel(new MigLayout("wrap 2, insets 20, gapy 8, gapx 10", "[left, pref!][grow]"));
                        formPanel.setBackground(ThemeConstants.CARD_WHITE);
        // Form Fields
        txtNom = UIUtils.createStyledTextField("Nom");
        txtPrenom = UIUtils.createStyledTextField("Prénom");
        txtEmail = UIUtils.createStyledTextField("e.email@lagh-univ.com");
        cmbRole = UIUtils.createStyledComboBox(new JComboBox<>());
        DefaultComboBoxModel<Object> roleModel = new DefaultComboBoxModel<>();
        roleModel.addElement("Sélectioner...");
        for (User.Role role : User.Role.values()) {
            roleModel.addElement(role);
        }
        cmbRole.setModel(roleModel);
        
        // Custom renderer for cmbRole to display user-friendly names
        cmbRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User.Role) {
                    setText(((User.Role) value).getRoleName());
                } else if (value instanceof String) {
                    setText((String) value); // For "Sélectioner..."
                }
                return this;
            }
        });

        cmbDepartement = UIUtils.createStyledComboBox(new JComboBox<>());
        cmbBloc = UIUtils.createStyledComboBox(new JComboBox<>()); // Initialize Bloc JComboBox
        txtTelephone = UIUtils.createStyledTextField("Ex: 0555123456");
        chkActif = new JCheckBox("Utilisateur actif");
        chkActif.setSelected(true);
        chkActif.putClientProperty(FlatClientProperties.STYLE, "" +
                "icon.checkmarkColor:" + UIUtils.colorToHex(ThemeConstants.CARD_WHITE) + ";" +
                "icon.focusedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.selectedBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
                "icon.hoverBackground:" + UIUtils.colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));

        // Initialize error labels
        lblErrorNom = new JLabel("Nom obligatoire.");
        lblErrorNom.setForeground(ThemeConstants.ERROR_RED);
        lblErrorNom.setVisible(false);
        lblErrorPrenom = new JLabel("Prénom obligatoire.");
        lblErrorPrenom.setForeground(ThemeConstants.ERROR_RED);
        lblErrorPrenom.setVisible(false);
        lblErrorEmail = new JLabel("Email obligatoire et valide.");
        lblErrorEmail.setForeground(ThemeConstants.ERROR_RED);
        lblErrorEmail.setVisible(false);
        lblErrorTelephone = new JLabel("Format de téléphone invalide.");
        lblErrorTelephone.setForeground(ThemeConstants.ERROR_RED);
        lblErrorTelephone.setVisible(false);
        lblErrorRole = new JLabel("Rôle obligatoire.");
        lblErrorRole.setForeground(ThemeConstants.ERROR_RED);
        lblErrorRole.setVisible(false);
        lblErrorDepartement = new JLabel("Faculté obligatoire.");
        lblErrorDepartement.setForeground(ThemeConstants.ERROR_RED);
        lblErrorDepartement.setVisible(false);
        lblErrorBloc = new JLabel("Département obligatoire.");
        lblErrorBloc.setForeground(ThemeConstants.ERROR_RED);
        lblErrorBloc.setVisible(false);

        // Styling
        txtNom.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nom");
        txtNom.putClientProperty(FlatClientProperties.STYLE, "arc:10;");
        txtPrenom.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Prénom");
        txtPrenom.putClientProperty(FlatClientProperties.STYLE, "arc:10;");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.email@lagh-univ.dz");
        txtEmail.putClientProperty(FlatClientProperties.STYLE, "arc:10;");
        txtTelephone.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex: 0555123456");
        txtTelephone.putClientProperty(FlatClientProperties.STYLE, "arc:10;");

        formPanel.add(new JLabel("Nom:"));
        formPanel.add(txtNom, "growx, h 35!");
        formPanel.add(lblErrorNom, "skip 1, wrap, gapy 0");

        formPanel.add(new JLabel("Prénom:"));
        formPanel.add(txtPrenom, "growx, h 35!");
        formPanel.add(lblErrorPrenom, "skip 1, wrap, gapy 0");

        formPanel.add(new JLabel("Email:"));
        formPanel.add(txtEmail, "growx, h 35!");
        formPanel.add(lblErrorEmail, "skip 1, wrap, gapy 0");
        
        lblRole = new JLabel("Rôle:");
        formPanel.add(lblRole, "hidemode 3");
        formPanel.add(cmbRole, "growx, h 35!, hidemode 3");
        formPanel.add(lblErrorRole, "skip 1, wrap, gapy 0, hidemode 3");
        
        lblDepartement = new JLabel("Faculté:");
        formPanel.add(lblDepartement, "hidemode 3");
        formPanel.add(cmbDepartement, "growx, h 35!, hidemode 3");
        formPanel.add(lblErrorDepartement, "skip 1, wrap, gapy 0, hidemode 3");
        
        lblBloc = new JLabel("Département:"); // Add Bloc label
        formPanel.add(lblBloc, "hidemode 3"); // Add Bloc label with hidemode
        formPanel.add(cmbBloc, "growx, h 35!, hidemode 3"); // Add Bloc combo box
        formPanel.add(lblErrorBloc, "skip 1, wrap, gapy 0, hidemode 3");
        
        formPanel.add(new JLabel("Téléphone:"));
        formPanel.add(txtTelephone, "growx, h 35!");
        formPanel.add(lblErrorTelephone, "skip 1, wrap, gapy 0");

        lblPhotoProfil = new JLabel("Photo de Profil:");
        formPanel.add(lblPhotoProfil, "top");

        // Profile picture panel
        photoPanel = new JPanel(new MigLayout("insets 0", "[center, 80]10[left]"));
        photoPanel.setOpaque(false);
        
        btnAttach = UIUtils.createSecondaryButton("Attacher");
        btnAttach.addActionListener(this::onAttach);
        
        btnDelete = UIUtils.createDangerButton("Supprimer");
        btnDelete.addActionListener(this::onDelete);

        updateProfileImagePreview(); // Initial setup of the photo panel

        formPanel.add(photoPanel, "wrap, span"); // Span to take whole row if needed

        formPanel.add(new JLabel("Statut:"));
        chkActif.putClientProperty("JCheckBox.arc", 10);
        formPanel.add(chkActif, "growx, wrap");

        // Dynamic Department and Bloc combo box visibility based on role
        cmbRole.addActionListener(e -> updateDepartmentAndBlocVisibility());
        updateDepartmentAndBlocVisibility(); // Initial call

        if (scopeLocked || lockedRole != null) {
            applyScopeLock();
        }

        // Buttons
        JButton btnSave = UIUtils.createPrimaryButton("Enregistrer");
        JButton btnCancel = UIUtils.createSecondaryButton("Annuler");
        JButton btnResetPassword = UIUtils.createDangerButton("Réinitialiser Mot de Passe"); // New button

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());
        btnResetPassword.addActionListener(e -> onResetPassword()); // Add action listener

        formPanel.add(btnResetPassword, "newline, span 2, split 3, left, gapright 5, hidemode 3, gaptop 10"); // Add reset button spanning both columns
        formPanel.add(btnSave, "gapright 5");
        formPanel.add(btnCancel, "");

        // Set visibility of reset password button
        if (!isEditMode) {
            btnResetPassword.setVisible(false);
        }

        add(formPanel, BorderLayout.CENTER); // Add formPanel directly
    }

    private void populateComboBoxes() {
        // Populate cmbDepartement (Faculté)
        DefaultComboBoxModel<Departement> deptModel = new DefaultComboBoxModel<>();
        deptModel.addElement(new Departement("Sélectioner...", 0)); // Explicitly add placeholder
        if (allDepartments != null) {
            for (Departement dept : allDepartments) {
                deptModel.addElement(dept);
            }
        }
        cmbDepartement.setModel(deptModel);
        cmbDepartement.setSelectedIndex(0); // Ensure "Sélectioner..." is selected by default

        // Add ItemListener to cmbDepartement to dynamically populate cmbBloc
        cmbDepartement.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                populateBlocsForSelectedDepartement((Departement) cmbDepartement.getSelectedItem());
            }
        });
        
        // Initial population of cmbBloc.
        // If in edit mode and a department is already set for the user, use that.
        // Otherwise, pass null to populate with all blocs or the placeholder.
        if (isEditMode && currentUser.getIdDepartement() != null && currentUser.getIdDepartement() > 0) {
            Departement currentDepartement = allDepartments.stream()
                .filter(d -> d.getId() == currentUser.getIdDepartement())
                .findFirst()
                .orElse(null);
            if (currentDepartement != null) {
                cmbDepartement.setSelectedItem(currentDepartement);
                populateBlocsForSelectedDepartement(currentDepartement);
            } else {
                cmbDepartement.setSelectedIndex(0); // If current dept not found, select placeholder
                populateBlocsForSelectedDepartement(null);
            }
        } else {
            // For add mode or if no department is set, populate with all or placeholder
            cmbDepartement.setSelectedIndex(0); // Always select placeholder if no current dept
            populateBlocsForSelectedDepartement(null);
        }
    }

    private void populateBlocsForSelectedDepartement(Departement selectedDepartement) {
        // Disable cmbBloc temporarily to indicate loading
        cmbBloc.setEnabled(false);
        // Clear current items and add loading message
        DefaultComboBoxModel<Bloc> loadingModel = new DefaultComboBoxModel<>();
        loadingModel.addElement(new Bloc("Chargement...", 0));
        cmbBloc.setModel(loadingModel);

        SwingWorker<List<Bloc>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Bloc> doInBackground() throws Exception {
                if (selectedDepartement != null && selectedDepartement.getId() != 0) {
                    return blocDAO.getBlocsByDepartement(selectedDepartement.getId());
                } else {
                    return blocDAO.getAllActiveBlocs(); 
                }
            }

            @Override
            protected void done() {
                try {
                    List<Bloc> blocsToDisplay = get(); // Get results from background thread

                    DefaultComboBoxModel<Bloc> blocModel = new DefaultComboBoxModel<>();
                    blocModel.addElement(new Bloc("Sélectioner...", 0)); // Explicitly add placeholder
                    if (blocsToDisplay != null) {
                        for (Bloc bloc : blocsToDisplay) {
                            blocModel.addElement(bloc);
                        }
                    }
                    cmbBloc.setModel(blocModel);

                    // Re-select the current User's bloc if in edit mode and it's in the filtered list
                    if (isEditMode && currentUser != null && currentUser.getIdBloc() != null && currentUser.getIdBloc() != 0) {
                        for (int i = 0; i < cmbBloc.getItemCount(); i++) {
                            Bloc bloc = cmbBloc.getItemAt(i);
                            if (bloc != null && bloc.getId() == currentUser.getIdBloc()) {
                                cmbBloc.setSelectedItem(bloc);
                                break;
                            }
                        }
                    } else {
                        cmbBloc.setSelectedIndex(0);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading blocs for selected departement", e);
                    DefaultComboBoxModel<Bloc> errorModel = new DefaultComboBoxModel<>();
                    errorModel.addElement(new Bloc("Erreur de chargement", 0));
                    cmbBloc.setModel(errorModel);
                } finally {
                    cmbBloc.setEnabled(true); // Re-enable cmbBloc
                }
            }
        };
        worker.execute();
    }

    private void applyScopeLock() {
        if (lockedRole != null) {
            cmbRole.setSelectedItem(lockedRole);
            lblRole.setVisible(false);
            cmbRole.setVisible(false);
            lblErrorRole.setVisible(false);
        }

        if (scopeLocked) {
            lblDepartement.setVisible(false);
            cmbDepartement.setVisible(false);
            lblErrorDepartement.setVisible(false);

            lblBloc.setVisible(false);
            cmbBloc.setVisible(false);
            lblErrorBloc.setVisible(false);
        }
    }

    private void updateDepartmentAndBlocVisibility() {
        if (scopeLocked || lockedRole != null) {
            return;
        }
        User.Role selectedRole = getSelectedRole();

        boolean needsDepartementOrBloc = (selectedRole == User.Role.Chef_Departement || selectedRole == User.Role.Enseignant);
        lblDepartement.setVisible(needsDepartementOrBloc);
        cmbDepartement.setVisible(needsDepartementOrBloc);
        lblBloc.setVisible(needsDepartementOrBloc);
        cmbBloc.setVisible(needsDepartementOrBloc);
        revalidate();
        repaint();

        applyDialogSize(getTargetSizeForRole(selectedRole));
        setLocationRelativeTo(null);
    }

    private void onAttach(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG)", "png", "jpg", "jpeg"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Create a unique filename
                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                String uniqueName = UUID.randomUUID().toString() + extension;
                Path destPath = Paths.get(AppConfig.getProfilePictureDirectory().getAbsolutePath(), uniqueName);

                // Ensure directory exists
                Files.createDirectories(destPath.getParent());

                // Copy file
                Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                // Update state
                newProfileImageName = uniqueName;
                profileImageDeleted = false;
                
                // Update preview
                updateProfileImagePreview();

            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to attach profile picture.", ex);
                JOptionPane.showMessageDialog(this, "Erreur lors de la copie de l'image.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onDelete(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Voulez-vous vraiment supprimer la photo de profil ?",
            "Confirmer la suppression",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            profileImageDeleted = true;
            newProfileImageName = null;
            updateProfileImagePreview();
        }
    }
    
    private void updateProfileImagePreview() {
        // 1. Determine the icon and initials
        ImageIcon profileIcon = null;
        String fullName = txtPrenom.getText() + " " + txtNom.getText();
        String imageToLoad = newProfileImageName;

        if (profileImageDeleted) {
            imageToLoad = null;
        } else if (imageToLoad == null && currentUser != null && currentUser.getPhotoProfil() != null && !currentUser.getPhotoProfil().isEmpty()) {
            imageToLoad = currentUser.getPhotoProfil();
        }

        if (imageToLoad != null) {
            profileIcon = UIUtils.getProfilePictureIcon(imageToLoad);
        }
        
        // If profileIcon is still null, RoundImage will automatically generate an initials placeholder.
        // No need to explicitly load users.png anymore.

        // 2. Rebuild the panel
        photoPanel.removeAll();
        
        profileImagePreview = new RoundImage(profileIcon, fullName, 80);
        
        photoPanel.add(profileImagePreview, "spany 2");
        photoPanel.add(btnAttach, "wrap, h 30!");
        photoPanel.add(btnDelete, "h 30!");

        photoPanel.revalidate();
        photoPanel.repaint();
    }



    private void populateFields() {
        if (currentUser != null) {
            txtNom.setText(currentUser.getNom());
            txtPrenom.setText(currentUser.getPrenom());
            txtEmail.setText(currentUser.getEmail());
            // Password fields are intentionally left blank for security in edit mode
            if (lockedRole != null) {
                cmbRole.setSelectedItem(lockedRole);
            } else if (currentUser.getRole() == null) {
                cmbRole.setSelectedIndex(0);
            } else {
                cmbRole.setSelectedItem(currentUser.getRole());
            }
            txtTelephone.setText(currentUser.getTelephone());
            chkActif.setSelected(currentUser.isActif());

            updateProfileImagePreview();
            applyDialogSize(getTargetSizeForRole(getSelectedRole()));

            if (scopeLocked) {
                return;
            }

            updateDepartmentAndBlocVisibility();
        }
    }

    private void onSave() {
        // --- Validation ---
        try {
            if (!validateForm()) {
                revalidate();
                repaint();
                return;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de validation de l'email: " + e.getMessage(), e);
            String errorMessage = "Erreur de base de données lors de la validation de l'email: " + e.getMessage();
            callback.onDialogClose(false, errorMessage);
            JOptionPane.showMessageDialog(this, errorMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- Collect Data ---
        String oldProfileImage = isEditMode ? currentUser.getPhotoProfil() : null;
        
        User userToSave = isEditMode ? currentUser : new User();
        userToSave.setNom(txtNom.getText().trim());
        userToSave.setPrenom(txtPrenom.getText().trim());
        userToSave.setEmail(txtEmail.getText().trim());
        
        Object selectedRoleObj = cmbRole.getSelectedItem();
        User.Role selectedRole = lockedRole != null
                ? lockedRole
                : (selectedRoleObj instanceof User.Role ? (User.Role) selectedRoleObj : null);
        userToSave.setRole(selectedRole);

        if (scopeLocked) {
            userToSave.setIdDepartement(lockedDepartementId);
            userToSave.setIdBloc(lockedBlocId);
        } else {
            Departement selectedDepartement = (Departement) cmbDepartement.getSelectedItem();
            if (selectedDepartement != null && (selectedRole == User.Role.Chef_Departement || selectedRole == User.Role.Enseignant)) {
                userToSave.setIdDepartement(selectedDepartement.getId());
            } else {
                userToSave.setIdDepartement(null);
            }

            Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
            if (selectedBloc != null && (selectedRole == User.Role.Chef_Departement || selectedRole == User.Role.Enseignant)) {
                userToSave.setIdBloc(selectedBloc.getId());
            } else {
                userToSave.setIdBloc(null);
            }
        }
        
        userToSave.setTelephone(txtTelephone.getText().trim());
        userToSave.setActif(chkActif.isSelected());
        
        // Handle profile picture logic
        if (profileImageDeleted) {
            userToSave.setPhotoProfil(null);
        } else if (newProfileImageName != null) {
            userToSave.setPhotoProfil(newProfileImageName);
        }
        
        // Handle Password
        // No direct password field in dialog, password is either generated (new user) or reset separately (existing user)
        String password = ""; // Initialize as empty for now, will be filled if new user
        if (!isEditMode) { // For new users, generate temporary password
            String temporaryPassword = PasswordUtils.generateRandomPassword(12);
            try {
                String hashedPassword = PasswordUtils.hashPassword(temporaryPassword);
                userToSave.setMotDePasse(hashedPassword);
                password = temporaryPassword; // Store temporary password for email
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du hachage du mot de passe temporaire", e);
                String errorMessage = "Erreur interne: échec du hachage du mot de passe temporaire.";
                callback.onDialogClose(false, errorMessage);
                JOptionPane.showMessageDialog(this, errorMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        // If in edit mode, and no password was generated, existing password in currentUser is retained.

        // --- Call DAO ---
        try {
            boolean success;
            String message = ""; // Initialize message

            if (isEditMode) {
                // Client-side uniqueness check already handled by validateForm for email
                success = userDAO.updateUser(userToSave);
                message = success ? "Utilisateur modifié avec succès." : "Échec de la modification de l'utilisateur.";
            } else {
                // Client-side uniqueness check already handled by validateForm for email
                success = userDAO.addUser(userToSave);
                message = success ? "Utilisateur ajouté avec succès." : "Échec de l'ajout de l'utilisateur.";
                
                // If user added successfully AND email service configured, send email with credentials
                if (success && EmailService.getInstance().isConfigured()) {
                    String finalPassword = password;
                    new Thread(() -> {
                        boolean emailSent = EmailService.getInstance().sendNewUserCredentials(
                            userToSave.getEmail(),
                            userToSave.getPrenom(),
                            userToSave.getNom(),
                            userToSave.getEmail(),
                            finalPassword
                        );
                        if (emailSent) {
                            LOGGER.info("Credentials email sent to new user: " + userToSave.getEmail());
                        } else {
                            LOGGER.warning("Failed to send credentials email to new user: " + userToSave.getEmail());
                        }
                    }).start();
                } else if (success && !EmailService.getInstance().isConfigured()) {
                     LOGGER.warning("Email service not configured. Cannot send credentials email.");
                     message += "\n(Avertissement: Service e-mail non configuré, identifiants non envoyés.)";
                }
            }
            
            if (success) {
                // Cleanup old profile picture if it changed
                if (oldProfileImage != null && (profileImageDeleted || newProfileImageName != null)) {
                    try {
                        Files.deleteIfExists(Paths.get(AppConfig.getProfilePictureDirectory().getAbsolutePath(), oldProfileImage));
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Could not delete old profile picture: " + oldProfileImage, e);
                    }
                }
                
                JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
                callback.onDialogClose(true, message);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
                callback.onDialogClose(false, message);
            }
        } catch (SQLException ex) { // Catch SQLException specifically from DAO methods
            LOGGER.log(Level.SEVERE, "Database error during user save", ex);
            String errorMessage = "Erreur de base de données: " + ex.getMessage();
            JOptionPane.showMessageDialog(this, errorMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
            callback.onDialogClose(false, errorMessage);
        } catch (Exception ex) { // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during user save", ex);
            String errorMessage = "Une erreur inattendue est survenue: " + ex.getMessage();
            JOptionPane.showMessageDialog(this, errorMessage, "Erreur", JOptionPane.ERROR_MESSAGE);
            callback.onDialogClose(false, errorMessage);
        }
    }

    private void applyDialogSize(Dimension target) {
        if (target == null) return;
        getContentPane().setPreferredSize(target);
        setPreferredSize(target);
        setMinimumSize(target);
        setMaximumSize(target);
        pack();
        setSize(target);
    }

    private User.Role getSelectedRole() {
        if (lockedRole != null) {
            return lockedRole;
        }
        Object selectedItem = cmbRole.getSelectedItem();
        return (selectedItem instanceof User.Role) ? (User.Role) selectedItem : null;
    }

    private Dimension getTargetSizeForRole(User.Role selectedRole) {
        boolean needsDepartementOrBloc = (selectedRole == User.Role.Chef_Departement || selectedRole == User.Role.Enseignant);
        Dimension target = needsDepartementOrBloc
            ? new Dimension(EXTENDED_DIALOG_WIDTH, EXTENDED_DIALOG_HEIGHT)
            : new Dimension(BASE_DIALOG_WIDTH, BASE_DIALOG_HEIGHT);
        if (scopeLocked) {
            int height = isEditMode ? CHEF_EDIT_HEIGHT : CHEF_ADD_HEIGHT;
            int width = isEditMode ? CHEF_EDIT_WIDTH : CHEF_ADD_WIDTH;
            target = new Dimension(width, height);
        }
        if (isEditMode && needsDepartementOrBloc) {
            target = new Dimension(target.width, target.height + 20);
        }
        if (isEditMode && !needsDepartementOrBloc && (selectedRole == User.Role.Admin || selectedRole == null)) {
            target = new Dimension(
                target.width + EDIT_BASE_WIDTH_DELTA,
                target.height + EDIT_BASE_HEIGHT_DELTA
            );
        }
        return target;
    }



    private void onResetPassword() {
        if (currentUser == null) {
            UIUtils.showTemporaryMessage(this, "Impossible de réinitialiser le mot de passe : aucun utilisateur sélectionné.", false, 3000);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Voulez-vous vraiment réinitialiser le mot de passe de l'utilisateur " + currentUser.getFullName() + "?\n" +
            "Un nouveau mot de passe sera généré et envoyé à son adresse e-mail.",
            "Confirmer la Réinitialisation du Mot de Passe",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String newTemporaryPassword = PasswordUtils.generateRandomPassword(12);
            boolean updateSuccess = false;
            try {
                // Update password in DB
                updateSuccess = userDAO.updatePassword(currentUser.getEmail(), newTemporaryPassword);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation du mot de passe pour " + currentUser.getEmail(), e);
                UIUtils.showTemporaryMessage(this, "Échec de la réinitialisation du mot de passe en base de données.", false, 3000);
                return;
            }

            if (updateSuccess) {
                // If DB update is successful, attempt to send email
                if (EmailService.getInstance().isConfigured()) {
                    new Thread(() -> {
                        boolean emailSent = EmailService.getInstance().sendPasswordResetConfirmation(
                            currentUser.getEmail(),
                            currentUser.getPrenom(),
                            currentUser.getNom(),
                            newTemporaryPassword
                        );
                        if (emailSent) {
                            LOGGER.info("Nouveau mot de passe envoyé par e-mail à : " + currentUser.getEmail());
                            SwingUtilities.invokeLater(() ->
                                UIUtils.showTemporaryMessage(this, "Mot de passe réinitialisé et envoyé à l'utilisateur.", true, 3000)
                            );
                        } else {
                            LOGGER.warning("Échec de l'envoi du mot de passe réinitialisé par e-mail à : " + currentUser.getEmail());
                            SwingUtilities.invokeLater(() ->
                                UIUtils.showTemporaryMessage(this, "Mot de passe réinitialisé en base de données, mais échec de l'envoi par e-mail.", false, 3000)
                            );
                        }
                    }).start();
                } else {
                    LOGGER.warning("Service e-mail non configuré. Impossible d'envoyer le mot de passe réinitialisé à : " + currentUser.getEmail());
                    UIUtils.showTemporaryMessage(this, "Mot de passe réinitialisé en base de données. Le service e-mail n'est pas configuré pour l'envoi.", false, 3000);
                }
            } else {
                UIUtils.showTemporaryMessage(this, "Échec de la réinitialisation du mot de passe en base de données.", false, 3000);
            }
        }
    }

    private boolean validateForm() throws SQLException {
        boolean isValid = true;

        // --- Reset Error States ---
        FormValidationUtils.applyDefaultStyle(txtNom);
        lblErrorNom.setVisible(false);
        FormValidationUtils.applyDefaultStyle(txtPrenom);
        lblErrorPrenom.setVisible(false);
        FormValidationUtils.applyDefaultStyle(txtEmail);
        lblErrorEmail.setVisible(false);
        FormValidationUtils.applyDefaultStyle(txtTelephone);
        lblErrorTelephone.setVisible(false);
        FormValidationUtils.applyDefaultStyle(cmbRole);
        lblErrorRole.setVisible(false);
        FormValidationUtils.applyDefaultStyle(cmbDepartement);
        lblErrorDepartement.setVisible(false);
        FormValidationUtils.applyDefaultStyle(cmbBloc);
        lblErrorBloc.setVisible(false);

        // --- Validate Nom ---
        if (txtNom.getText().trim().isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtNom);
            lblErrorNom.setText("Le nom est obligatoire.");
            lblErrorNom.setVisible(true);
            isValid = false;
        }

        // --- Validate Prenom ---
        if (txtPrenom.getText().trim().isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtPrenom);
            lblErrorPrenom.setText("Le prénom est obligatoire.");
            lblErrorPrenom.setVisible(true);
            isValid = false;
        }

        // --- Validate Email ---
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            FormValidationUtils.applyErrorStyle(txtEmail);
            lblErrorEmail.setText("L'email est obligatoire.");
            lblErrorEmail.setVisible(true);
            isValid = false;
        } else if (!UIUtils.isValidEmail(email)) {
            FormValidationUtils.applyErrorStyle(txtEmail);
            lblErrorEmail.setText("Format d'email invalide.");
            lblErrorEmail.setVisible(true);
            isValid = false;
                    } else {
                        // Email uniqueness check
                        boolean emailAlreadyExists;
                        if (isEditMode && currentUser != null) {
                            // In edit mode, exclude the current user's own email from the check
                            emailAlreadyExists = userDAO.emailExists(email, currentUser.getIdUtilisateur());
                        } else {
                            // In add mode, or if currentUser is null, check for email existence normally
                            emailAlreadyExists = userDAO.emailExists(email);
                        }
                        
                        if (emailAlreadyExists) {
                            FormValidationUtils.applyErrorStyle(txtEmail);
                            lblErrorEmail.setText("Cet email est déjà utilisé.");
                            lblErrorEmail.setVisible(true);
                            isValid = false;
                        }
                    }
        // --- Validate Role ---
        if (lockedRole == null) {
            if (cmbRole.getSelectedItem() instanceof String) {
                FormValidationUtils.applyErrorStyle(cmbRole);
                lblErrorRole.setVisible(true);
                isValid = false;
            }
        }

        // --- Validate Role-specific Department/Bloc ---
        Object selectedRoleObj = cmbRole.getSelectedItem();
        User.Role selectedRole = lockedRole != null
                ? lockedRole
                : (selectedRoleObj instanceof User.Role ? (User.Role) selectedRoleObj : null);
        if (!scopeLocked && selectedRole != null) {
            boolean needsDepartementOrBloc = (selectedRole == User.Role.Chef_Departement || selectedRole == User.Role.Enseignant);

            if (needsDepartementOrBloc) {
                if (cmbDepartement.getSelectedItem() == null || ((Departement) cmbDepartement.getSelectedItem()).getId() == 0) {
                    FormValidationUtils.applyErrorStyle(cmbDepartement);
                    lblErrorDepartement.setText("La faculté est obligatoire pour ce rôle.");
                    lblErrorDepartement.setVisible(true);
                    isValid = false;
                } else {
                    Departement selectedDept = (Departement) cmbDepartement.getSelectedItem();
                    if (selectedRole == User.Role.Chef_Departement) {
                        Bloc selectedBloc = (Bloc) cmbBloc.getSelectedItem();
                        if (selectedBloc != null && selectedBloc.getId() != 0) {
                            Integer excludeUserId = isEditMode ? currentUser.getIdUtilisateur() : null;
                            if (userDAO.doesBlocHaveChef(selectedBloc.getId(), excludeUserId)) {
                                FormValidationUtils.applyErrorStyle(cmbBloc);
                                lblErrorBloc.setText("Un Chef de Département existe déjà pour ce département.");
                                lblErrorBloc.setVisible(true);
                                isValid = false;
                            }
                        }
                    }
                }
                if (cmbBloc.getSelectedItem() == null || ((Bloc) cmbBloc.getSelectedItem()).getId() == 0) {
                    FormValidationUtils.applyErrorStyle(cmbBloc);
                    lblErrorBloc.setText("Le département est obligatoire pour ce rôle.");
                    lblErrorBloc.setVisible(true);
                    isValid = false;
                }
            }
        }



        // --- Validate Telephone (Optional, but check format if not empty) ---
        String telephone = txtTelephone.getText().trim();
        if (!telephone.isEmpty()) {
            // Regex for a basic phone number format (e.g., 10-15 digits, possibly with spaces or dashes)
            // This is a simple example, can be made more robust based on requirements
            if (!Pattern.matches("^(\\+?[0-9\\s-]{7,15})$", telephone)) {
                FormValidationUtils.applyErrorStyle(txtTelephone);
                lblErrorTelephone.setText("Format de téléphone invalide.");
                lblErrorTelephone.setVisible(true);
                isValid = false;
            }
        }
        
        return isValid;
    }

}
