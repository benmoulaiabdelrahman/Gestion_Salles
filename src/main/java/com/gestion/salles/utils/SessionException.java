package com.gestion.salles.utils;

/******************************************************************************
 * SessionException.java
 *
 * Checked exception for session management failures. Forces callers to handle
 * session initialization issues explicitly.
 ******************************************************************************/

public class SessionException extends Exception {

    public SessionException(String message) {
        super(message);
    }

    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
