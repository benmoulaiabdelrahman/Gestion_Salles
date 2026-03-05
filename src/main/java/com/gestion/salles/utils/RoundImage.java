package com.gestion.salles.utils;

/******************************************************************************
 * RoundImage.java
 *
 * Custom JComponent that renders an image clipped to a circle. If no image
 * is provided, displays the user's initials on a coloured circular background.
 * Also exposes static helpers for creating rounded images and initials icons.
 ******************************************************************************/

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class RoundImage extends JComponent {

    private Image  image;
    private String initials;
    private final int diameter;

    public RoundImage(ImageIcon icon, String fullName, int diameter) {
        this.diameter = diameter;
        setPreferredSize(new Dimension(diameter, diameter));
        this.image    = (icon != null) ? icon.getImage() : null;
        this.initials = resolveInitials(fullName);
    }

    public void setImage(ImageIcon icon, String fullName) {
        this.image    = (icon != null) ? icon.getImage() : null;
        this.initials = resolveInitials(fullName);
        repaint();
    }

    public static Image getRoundedImage(Image image, int diameter) {
        if (image == null) return null;
        BufferedImage master = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = master.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Double(0, 0, diameter, diameter));
        g2.drawImage(image, 0, 0, diameter, diameter, null);
        g2.dispose();
        return master;
    }

    public static ImageIcon createInitialsRoundIcon(String fullName, int diameter) {
        BufferedImage image = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawInitialsCircle(g2, resolveInitials(fullName), diameter);
        g2.dispose();
        return new ImageIcon(image);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(getRoundedImage(image, diameter), 0, 0, this);
        } else {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawInitialsCircle(g2, initials, diameter);
            g2.dispose();
        }
    }

    private static void drawInitialsCircle(Graphics2D g2, String initials, int diameter) {
        g2.setColor(ThemeConstants.PRIMARY_GREEN.darker());
        g2.fill(new Ellipse2D.Double(0, 0, diameter, diameter));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, diameter / 3));
        FontMetrics fm = g2.getFontMetrics();
        int x = (diameter - fm.stringWidth(initials)) / 2;
        int y = (diameter + fm.getAscent()) / 2 - fm.getDescent();
        g2.drawString(initials, x, y);
    }

    private static String resolveInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1)  return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        if (!parts[0].isEmpty()) return ("" + parts[0].charAt(0)).toUpperCase();
        return "?";
    }
}
