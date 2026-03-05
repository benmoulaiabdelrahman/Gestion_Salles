package com.gestion.salles.views.Login;

/******************************************************************************
 * PasswordStrengthMeter.java
 *
 * Purely presentational Swing component that renders password strength as
 * three coloured bar segments. Callers update it by passing a StrengthStatus
 * to setStrengthStatus(). Has no validation logic and does not interact with
 * any DAO or service.
 ******************************************************************************/

import com.gestion.salles.utils.PasswordStrengthChecker.StrengthStatus;

import javax.swing.*;
import java.awt.*;

public class PasswordStrengthMeter extends JPanel {

    private static final Color ERROR_RED      = new Color(198, 40,  40);
    private static final Color WARNING_ORANGE = new Color(234, 88,  12);
    private static final Color SUCCESS_GREEN  = new Color(22,  163, 74);
    private static final Color DISABLED_GRAY  = new Color(229, 231, 235);

    private StrengthStatus status = StrengthStatus.WEAK;

    public PasswordStrengthMeter() {
        setOpaque(false);
        setPreferredSize(new Dimension(200, 10));
    }

    public void setStrengthStatus(StrengthStatus status) {
        if (this.status != status) {
            this.status = status;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int totalWidth = getWidth();
        int height     = getHeight();
        int gap        = 4;
        int barWidth   = (totalWidth - 2 * gap) / 3;
        int activeBars = getActiveBars(status);
        Color activeColor = getActiveBarColor(status);

        for (int i = 0; i < 3; i++) {
            int x = i * (barWidth + gap);
            g2.setColor(i < activeBars ? activeColor : DISABLED_GRAY);
            g2.fillRoundRect(x, 0, barWidth, height, 5, 5);
        }

        g2.dispose();
    }

    private int getActiveBars(StrengthStatus s) {
        return switch (s) {
            case WEAK   -> 1;
            case MEDIUM -> 2;
            case STRONG -> 3;
        };
    }

    private Color getActiveBarColor(StrengthStatus s) {
        return switch (s) {
            case WEAK   -> ERROR_RED;
            case MEDIUM -> WARNING_ORANGE;
            case STRONG -> SUCCESS_GREEN;
        };
    }
}
