package com.gestion.salles.views.Login;

/******************************************************************************
 * LoginPanel.java
 *
 * Main authentication form panel, hosted inside LoginFrame. Renders email,
 * password, remember-me checkbox, and a forgot-password link. Authentication
 * runs on a SwingWorker so the EDT is never blocked; during the call the
 * login button is disabled and the frame shows a WAIT_CURSOR. Password
 * char[] arrays are always zeroed in a finally block regardless of which
 * validation path exits. On success delegates to LoginFrame.openDashboard().
 ******************************************************************************/

import com.gestion.salles.services.AuthService;
import com.gestion.salles.services.AuthService.AuthenticationResult;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class LoginPanel extends JPanel {

    private JTextField     txtEmail;
    private JPasswordField txtPassword;
    private JButton        btnLogin;
    private JLabel         lblError;
    private JCheckBox      chkRememberMe;
    private JLabel         lblEmailRequired;
    private JLabel         lblPasswordRequired;

    private final LoginFrame  parentFrame;
    private final AuthService authService;

    public LoginPanel(LoginFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.authService = AuthService.getInstance();
        initComponents();
    }

    private void initComponents() {
        setBackground(ThemeConstants.APP_BACKGROUND);
        setLayout(new MigLayout("fill,insets 20", "[center]", "[center]"));

        JPanel formPanel = UIUtils.createLoginCardPanel("insets 35 45 30 45", "fill,300:380");

        formPanel.add(UIUtils.createLoginHeaderPanel(
            "/icons/University_of_Laghouat_logo.png", 100, "Logo",
            "Bienvenue", "Gestion Salles", null), "growx");

        JPanel emailLabelPanel = new JPanel(new MigLayout("insets 0", "[][]"));
        emailLabelPanel.setBackground(null);
        emailLabelPanel.add(UIUtils.createStyledLabel("Email",
            UIManager.getDefaults().getFont("Label.font").deriveFont(Font.BOLD, 13f),
            ThemeConstants.PRIMARY_TEXT, SwingConstants.LEFT));
        lblEmailRequired = UIUtils.createRequiredFieldIndicator();
        emailLabelPanel.add(lblEmailRequired, "gapleft 3");
        formPanel.add(emailLabelPanel, "gapy 8");

        txtEmail = UIUtils.createStyledTextField("votre.email@lagh-univ.dz");
        formPanel.add(txtEmail, "height 45");

        JPanel passwordLabelPanel = new JPanel(new MigLayout("insets 0", "[][]"));
        passwordLabelPanel.setBackground(null);
        passwordLabelPanel.add(UIUtils.createStyledLabel("Mot de passe",
            UIManager.getDefaults().getFont("Label.font").deriveFont(Font.BOLD, 13f),
            ThemeConstants.PRIMARY_TEXT, SwingConstants.LEFT));
        lblPasswordRequired = UIUtils.createRequiredFieldIndicator();
        passwordLabelPanel.add(lblPasswordRequired, "gapleft 3");
        formPanel.add(passwordLabelPanel, "gapy 8");

        txtPassword = UIUtils.createStyledPasswordField("Entrez votre mot de passe", true, true);
        formPanel.add(txtPassword, "height 45");

        chkRememberMe = UIUtils.createStyledCheckBox("Se souvenir de moi");
        JButton btnForgotPassword = UIUtils.createLinkButton("Mot de passe oublié ?");
        btnForgotPassword.addActionListener(this::onForgotPasswordClick);

        JPanel optionsPanel = new JPanel(new BorderLayout(50, 0));
        optionsPanel.setBackground(null);
        optionsPanel.add(chkRememberMe,     BorderLayout.WEST);
        optionsPanel.add(btnForgotPassword, BorderLayout.EAST);
        formPanel.add(optionsPanel, "gapy 5,growx");

        btnLogin = UIUtils.createPrimaryButton("Se connecter");
        btnLogin.addActionListener(this::onLoginClick);
        formPanel.add(btnLogin, "gapy 15,height 45");

        lblError = UIUtils.createErrorLabel();
        formPanel.add(lblError, "gapy 10,align center");

        formPanel.add(createSignupSection(), "gapy 10");

        add(formPanel);
    }

    private JPanel createSignupSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setBackground(null);

        JLabel lblQuestion = new JLabel("Première connexion ?");
        lblQuestion.setFont(lblQuestion.getFont().deriveFont(13f));
        lblQuestion.setForeground(ThemeConstants.MUTED_TEXT);

        JButton btnContact = UIUtils.createLinkButton("Contacter l'université");
        btnContact.setMargin(new Insets(0, 5, 0, 0));
        btnContact.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ContactAdminDialog(owner).setVisible(true);
        });

        panel.add(lblQuestion);
        panel.add(btnContact);
        return panel;
    }

    private void onLoginClick(ActionEvent e) {
        clearValidation();

        String email         = txtEmail.getText().trim();
        char[] passwordChars = txtPassword.getPassword();

        try {
            boolean emailEmpty    = email.isEmpty();
            boolean passwordEmpty = passwordChars.length == 0;

            if (emailEmpty)    lblEmailRequired.setVisible(true);
            if (passwordEmpty) lblPasswordRequired.setVisible(true);

            if (emailEmpty && passwordEmpty) {
                showError("Veuillez remplir tous les champs");
                return;
            }
            if (emailEmpty) {
                showError("Veuillez saisir votre email");
                return;
            }
            if (passwordEmpty) {
                showError("Veuillez saisir votre mot de passe");
                return;
            }
            if (!UIUtils.isValidEmail(email)) {
                lblEmailRequired.setVisible(true);
                showError("Format d'email invalide");
                return;
            }

            performLogin(email, Arrays.copyOf(passwordChars, passwordChars.length));

        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    private void performLogin(String email, char[] passwordChars) {
        setAuthLoading(true);

        boolean rememberMe = chkRememberMe.isSelected();

        SwingWorker<AuthenticationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthenticationResult doInBackground() throws Exception {
                try {
                    return authService.authenticate(email, passwordChars, rememberMe);
                } finally {
                    Arrays.fill(passwordChars, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    AuthenticationResult result = get();
                    if (result.isSuccess()) {
                        parentFrame.openDashboard(result.getUser());
                    } else {
                        showError("Email ou mot de passe invalide.");
                    }
                } catch (Exception ex) {
                    showError("Erreur de connexion : " + ex.getMessage());
                } finally {
                    setAuthLoading(false);
                    txtPassword.setText("");
                }
            }
        };

        worker.execute();
    }

    private void setAuthLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Connexion en cours…" : "Se connecter");
        btnLogin.setBackground(loading ? ThemeConstants.DISABLED_GREEN : ThemeConstants.PRIMARY_GREEN);

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.setCursor(loading
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
        }
    }

    private void onForgotPasswordClick(ActionEvent e) {
        parentFrame.showPasswordRecoveryPanel();
    }

    private void showError(String message) {
        UIUtils.showTemporaryErrorMessage(lblError, message, 5000);
    }

    private void clearValidation() {
        lblError.setText("");
        lblEmailRequired.setVisible(false);
        lblPasswordRequired.setVisible(false);
    }

    public String  getEmailInput()        { return txtEmail.getText().trim(); }
    public String  getPasswordInput()     { return new String(txtPassword.getPassword()); }
    public boolean isRememberMeSelected() { return chkRememberMe.isSelected(); }

    public void clearInputs() {
        txtEmail.setText("");
        txtPassword.setText("");
        chkRememberMe.setSelected(false);
        clearValidation();
    }
}
