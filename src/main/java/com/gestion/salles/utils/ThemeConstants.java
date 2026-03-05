package com.gestion.salles.utils;

/******************************************************************************
 * ThemeConstants.java
 *
 * Centralized constants for the application theme: fonts, colors, and
 * UI-specific values. Ensures visual consistency across all components.
 ******************************************************************************/

import java.awt.Color;
import java.awt.Font;

public class ThemeConstants {

    // Fonts
    public static final Font FONT_BOLD_18    = new Font("Roboto", Font.BOLD,  18);
    public static final Font FONT_REGULAR_13 = new Font("Roboto", Font.PLAIN, 13);
    public static final Font FONT_LIGHT_12   = new Font("Roboto", Font.PLAIN, 12);

    // Brand
    public static final Color PRIMARY_GREEN       = new Color( 15, 107,  63);
    public static final Color PRIMARY_GREEN_HOVER = new Color( 76, 175, 122);
    public static final Color DISABLED_GREEN      = new Color(159, 191, 176);

    // Backgrounds
    public static final Color APP_BACKGROUND = new Color(247, 249, 248);
    public static final Color CARD_WHITE     = new Color(255, 255, 255);

    // Text
    public static final Color PRIMARY_TEXT   = new Color( 30,  30,  30);
    public static final Color SECONDARY_TEXT = new Color(107, 114, 128);
    public static final Color MUTED_TEXT     = new Color(156, 163, 175);

    // Borders
    public static final Color DEFAULT_BORDER = new Color(209, 213, 219);
    public static final Color FOCUS_BORDER   = PRIMARY_GREEN;

    // Feedback
    public static final Color ERROR_RED    = new Color(198,  40,  40);
    public static final Color SUCCESS_GREEN = new Color( 22, 163,  74);

    // Navigation
    public static final Color NAV_BACKGROUND       = new Color(237, 242, 239);
    public static final Color NAV_HOVER_BACKGROUND = new Color(228, 233, 230);

    // Table / Schedule
    public static final Color TABLE_SELECTION_BACKGROUND        = new Color(197, 225, 207);
    public static final Color TABLE_SELECTION_FOREGROUND        = PRIMARY_TEXT;
    public static final Color SCHEDULE_FREE_COLOR               = new Color(229, 245, 233);
    public static final Color SCHEDULE_OCCUPIED_FALLBACK_COLOR  = new Color(254, 226, 226);
    public static final Color SCHEDULE_ONLINE_COLOR             = new Color(220, 238, 255);
}
