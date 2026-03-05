package com.gestion.salles.utils;

/******************************************************************************
 * PasswordStrengthChecker.java
 *
 * Utility class for evaluating password strength. Exposes the StrengthStatus
 * enum and the checkStrength() method based on length and character variety.
 ******************************************************************************/

import java.awt.Color;

public class PasswordStrengthChecker {

    public enum StrengthStatus {
        WEAK  ("Faible", new Color(198, 40,  40)),
        MEDIUM("Moyen",  new Color(234, 88,  12)),
        STRONG("Fort",   new Color( 22, 163, 74));

        private final String message;
        private final Color  color;

        StrengthStatus(String message, Color color) {
            this.message = message;
            this.color   = color;
        }

        public String getMessage() { return message; }
        public Color  getColor()   { return color;   }
    }

    public static StrengthStatus checkStrength(String password) {
        if (password == null || password.isEmpty()) return StrengthStatus.WEAK;

        int score = 0;
        if (password.matches(".*[a-z].*"))        score++;
        if (password.matches(".*[A-Z].*"))        score++;
        if (password.matches(".*\\d.*"))          score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;

        if (password.length() < 8) return StrengthStatus.WEAK;
        if (score >= 3)            return StrengthStatus.STRONG;
        if (score == 2)            return StrengthStatus.MEDIUM;
        return StrengthStatus.WEAK;
    }
}
