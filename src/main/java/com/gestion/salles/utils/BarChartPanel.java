package com.gestion.salles.utils;

/******************************************************************************
 * BarChartPanel.java
 *
 * Lightweight JPanel that renders a bar chart from a String→Integer map.
 * Bars are drawn with rounded corners and colour-cycled from COLORS. The
 * Y-axis shows up to six evenly spaced labels; each bar displays its value
 * above it and its category label below. Defaults to the title "Statistiques"
 * when none is supplied. Renders "Aucune donnée à afficher" when data is
 * null or empty.
 ******************************************************************************/

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

public class BarChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color[] COLORS = {
        ThemeConstants.PRIMARY_GREEN,
        Color.decode("#3B82F6"),
        Color.decode("#10B981"),
        ThemeConstants.ERROR_RED,
        Color.decode("#F59E0B"),
        Color.decode("#8B5CF6"),
        Color.decode("#6B7280")
    };

    private static final int BAR_PADDING    = 10;
    private static final int TOP_PADDING    = 90;
    private static final int BOTTOM_PADDING = 40;
    private static final int LEFT_PADDING   = 50;
    private static final int RIGHT_PADDING  = 20;

    private final Map<String, Integer> data;
    private final String               chartTitle;

    public BarChartPanel(Map<String, Integer> data) {
        this(data, "Statistiques");
    }

    public BarChartPanel(Map<String, Integer> data, String chartTitle) {
        this.data       = data;
        this.chartTitle = chartTitle;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (data == null || data.isEmpty()) {
            g.setColor(ThemeConstants.SECONDARY_TEXT);
            g.drawString("Aucune donnée à afficher", getWidth() / 2 - 60, getHeight() / 2);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width      = getWidth();
        int height     = getHeight();
        int drawWidth  = width  - LEFT_PADDING - RIGHT_PADDING;
        int drawHeight = height - TOP_PADDING  - BOTTOM_PADDING;

        // Title
        g2.setColor(ThemeConstants.PRIMARY_TEXT);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        FontMetrics titleFm = g2.getFontMetrics();
        g2.drawString(chartTitle, (width - titleFm.stringWidth(chartTitle)) / 2, TOP_PADDING - 60);

        // Y-axis scaling
        int    maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double scale    = (double) drawHeight / maxValue;
        int    step     = Math.max(1, maxValue / 5);

        // Y-axis line and labels
        g2.setColor(ThemeConstants.MUTED_TEXT);
        g2.setFont(g2.getFont().deriveFont(10f));
        g2.drawLine(LEFT_PADDING, height - BOTTOM_PADDING, LEFT_PADDING, TOP_PADDING);

        FontMetrics axFm = g2.getFontMetrics();
        for (int i = 0; i <= maxValue; i += step) {
            int    yPos  = height - BOTTOM_PADDING - (int) (i * scale);
            String label = String.valueOf(i);
            g2.drawString(label, LEFT_PADDING - axFm.stringWidth(label) - 5, yPos + axFm.getAscent() / 2);
            g2.drawLine(LEFT_PADDING - 3, yPos, LEFT_PADDING, yPos);
        }

        // Bars
        int barFullWidth   = drawWidth / data.size();
        int barActualWidth = barFullWidth - BAR_PADDING;
        int xPos           = LEFT_PADDING + BAR_PADDING / 2;
        int colorIndex     = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int value     = entry.getValue();
            int barHeight = (int) (value * scale);
            int yBar      = height - BOTTOM_PADDING - barHeight;

            // Bar
            g2.setColor(COLORS[colorIndex % COLORS.length]);
            g2.fillRoundRect(xPos, yBar, barActualWidth, barHeight, 5, 5);

            // Value label above bar
            g2.setColor(ThemeConstants.PRIMARY_TEXT);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            String valueStr     = String.valueOf(value);
            FontMetrics valueFm = g2.getFontMetrics();
            g2.drawString(valueStr, xPos + (barActualWidth - valueFm.stringWidth(valueStr)) / 2, yBar - 5);

            // Category label below bar
            g2.setColor(ThemeConstants.SECONDARY_TEXT);
            g2.setFont(g2.getFont().deriveFont(10f));
            String      catStr = entry.getKey();
            FontMetrics catFm  = g2.getFontMetrics();
            g2.drawString(catStr, xPos + (barActualWidth - catFm.stringWidth(catStr)) / 2, height - BOTTOM_PADDING + 20);

            xPos += barFullWidth;
            colorIndex++;
        }

        g2.dispose();
    }
}
