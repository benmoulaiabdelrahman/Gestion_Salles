package com.gestion.salles.views.shared.recovery;

interface PasswordRecoveryFlow {
    void showStep1();
    void showStep2();
    void showStep3();
    void showSettings();
    String getUserEmail();
    void setUserEmail(String userEmail);

    default String getBackNavigationLabel() {
        return "Retour aux paramètres";
    }
}
