package com.gestion.salles.views.shared.recovery;

import com.gestion.salles.services.AuthService;
import com.gestion.salles.services.VerificationCodeManager;
import com.gestion.salles.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class PasswordRecoveryContainer extends JPanel implements PasswordRecoveryFlow {

    private final CardLayout cardLayout = new CardLayout();
    private final PasswordRecoveryNavigator navigator;
    private final String initialUserEmail;
    private final String backNavigationLabel;
    private String userEmail;
    private final AuthService authService;
    private final VerificationCodeManager codeManager;

    private PasswordRecoveryStep1Panel step1Panel;
    private PasswordRecoveryStep2Panel step2Panel;
    private PasswordRecoveryStep3Panel step3Panel;

    public PasswordRecoveryContainer(PasswordRecoveryNavigator navigator,
                                     String userEmail,
                                     String backNavigationLabel,
                                     AuthService authService,
                                     VerificationCodeManager codeManager) {
        this.navigator = navigator;
        this.initialUserEmail = userEmail;
        this.userEmail = userEmail;
        this.backNavigationLabel = (backNavigationLabel == null || backNavigationLabel.isBlank())
            ? "Retour aux paramètres"
            : backNavigationLabel;
        this.authService = authService;
        this.codeManager = codeManager;
        initComponents();
    }

    public PasswordRecoveryContainer(Runnable showSettingsAction,
                                     String userEmail,
                                     AuthService authService,
                                     VerificationCodeManager codeManager) {
        this(new PasswordRecoveryNavigator() {
            @Override
            public void showSettings() {
                showSettingsAction.run();
            }

            @Override
            public void showPasswordRecovery() {
                // no-op callback for this shortcut constructor
            }
        }, userEmail, "Retour aux paramètres", authService, codeManager);
    }

    private void initComponents() {
        setLayout(cardLayout);
        setOpaque(false);
        rebuildSteps();
        showStep1();
    }

    private void rebuildSteps() {
        removeAll();
        step1Panel = new PasswordRecoveryStep1Panel(this, codeManager);
        step2Panel = new PasswordRecoveryStep2Panel(this, codeManager);
        step3Panel = new PasswordRecoveryStep3Panel(this, authService);
        add(step1Panel, "Step1");
        add(step2Panel, "Step2");
        add(step3Panel, "Step3");
        revalidate();
        repaint();
    }

    public boolean isShowingPasswordRecovery() {
        return UIUtils.isCardCurrentlyVisible(this, "Step1")
            || UIUtils.isCardCurrentlyVisible(this, "Step2")
            || UIUtils.isCardCurrentlyVisible(this, "Step3");
    }

    public void reset() {
        if (step2Panel != null) {
            step2Panel.stopCountdown();
        }
        userEmail = initialUserEmail;
        rebuildSteps();
    }

    public void goBack() {
        if (UIUtils.isCardCurrentlyVisible(this, "Step3")) {
            if (step2Panel != null) {
                step2Panel.stopCountdown();
            }
            if (step1Panel != null) {
                step1Panel.reset();
                step1Panel.showInfoMessage("Votre code a été utilisé. Demandez un nouveau code pour continuer.");
            }
            showStep1();
        } else if (UIUtils.isCardCurrentlyVisible(this, "Step2")) {
            if (step2Panel != null) {
                step2Panel.stopCountdown();
            }
            showStep1();
        } else if (UIUtils.isCardCurrentlyVisible(this, "Step1")) {
            showSettings();
        }
    }

    @Override
    public void showStep1() {
        cardLayout.show(this, "Step1");
    }

    @Override
    public void showStep2() {
        if (step2Panel != null) {
            step2Panel.refreshEmailDisplay();
            step2Panel.restartForNewCode();
        }
        cardLayout.show(this, "Step2");
    }

    @Override
    public void showStep3() {
        if (step3Panel != null) {
            step3Panel.refreshEmailDisplay();
        }
        cardLayout.show(this, "Step3");
    }

    @Override
    public void showSettings() {
        navigator.showSettings();
    }

    @Override
    public String getBackNavigationLabel() {
        return backNavigationLabel;
    }

    @Override
    public String getUserEmail() {
        return userEmail;
    }

    @Override
    public void setUserEmail(String userEmail) {
        if (userEmail == null) {
            this.userEmail = null;
        } else {
            String trimmed = userEmail.trim();
            this.userEmail = trimmed.isEmpty() ? null : trimmed;
        }
    }

    public void showPasswordRecovery() {
        navigator.showPasswordRecovery();
        showStep1();
    }
}
