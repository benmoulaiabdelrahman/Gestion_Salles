package com.gestion.salles.views.shared.management;

import javax.swing.*;

public final class FormValidationUtils {

    private FormValidationUtils() {}

    public static void applyErrorStyle(JComponent component) {
        if (component instanceof JTextField) {
            component.putClientProperty("JComponent.outline", "error");
        } else if (component instanceof JSpinner) {
            JComponent editor = ((JSpinner) component).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().putClientProperty("JComponent.outline", "error");
            }
        } else if (component instanceof JComboBox) {
            component.putClientProperty("JComponent.outline", "error");
        } else if (component instanceof JScrollPane) {
            component.putClientProperty("JComponent.outline", "error");
        }
    }

    public static void applyDefaultStyle(JComponent component) {
        if (component instanceof JTextField) {
            component.putClientProperty("JComponent.outline", null);
        } else if (component instanceof JSpinner) {
            JComponent editor = ((JSpinner) component).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().putClientProperty("JComponent.outline", null);
            }
        } else if (component instanceof JComboBox) {
            component.putClientProperty("JComponent.outline", null);
        } else if (component instanceof JScrollPane) {
            component.putClientProperty("JComponent.outline", null);
        }
    }
}
