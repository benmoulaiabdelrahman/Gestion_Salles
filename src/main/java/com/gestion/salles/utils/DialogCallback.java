package com.gestion.salles.utils;

/******************************************************************************
 * DialogCallback.java
 *
 * Functional interface used as a callback for custom dialogs (e.g. confirm,
 * delete). Implementations receive the outcome of the dialog once it closes.
 ******************************************************************************/

@FunctionalInterface
public interface DialogCallback {
    void onDialogClose(boolean success, String message);
}
