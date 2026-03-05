package com.gestion.salles.views.shared.schedule;

public final class ScheduleUiText {
    public static final String BUTTON_EXPORT_PDF = "Exporter PDF";
    public static final String BUTTON_PRINT = "Imprimer";
    public static final String CHECKBOX_FULL_WEEK = "Semaine complète";

    public static final String PDF_DIALOG_TITLE = "Enregistrer l'emploi du temps en PDF";
    public static final String PDF_FILTER_LABEL = "Fichiers PDF";
    public static final String PDF_SUCCESS_TITLE = "Export PDF";
    public static final String PDF_SUCCESS_MESSAGE = "Emploi du temps exporté en PDF avec succès.";
    public static final String PDF_ERROR_TITLE = "Erreur d'export PDF";

    public static final String PRINT_SUCCESS_TITLE = "Impression Réussie";
    public static final String PRINT_SUCCESS_MESSAGE = "Document envoyé à l'imprimante avec succès.";
    public static final String PRINT_ERROR_TITLE = "Erreur d'Impression";
    public static final String PRINT_ERROR_MESSAGE =
        "Échec de l'impression. Veuillez vérifier la connexion de votre imprimante ou les paramètres.";

    private ScheduleUiText() {}

    public static String buildPdfErrorMessage(String details) {
        if (details == null || details.isBlank()) {
            return "Échec de l'export PDF.";
        }
        return "Échec de l'export PDF: " + details;
    }
}
