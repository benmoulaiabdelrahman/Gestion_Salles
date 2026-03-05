package com.gestion.salles.utils;

/******************************************************************************
 * AnimatedIconButton.java
 *
 * Custom JButton that renders a circular icon button with animated hover and
 * press feedback. The hover state paints a semi-transparent dark circle;
 * the press state deepens that circle and scales the icon down to
 * SCALE_DOWN_FACTOR, then animates it back to full size on release.
 * Hit detection is circular via contains(). Swing Timer is used explicitly
 * to avoid ambiguity with java.awt.Timer.
 ******************************************************************************/

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AnimatedIconButton extends JButton {

    private static final long serialVersionUID   = 1L;
    private static final int  ANIMATION_DURATION = 100; // ms
    private static final float SCALE_DOWN_FACTOR = 0.8f;

    private static final Color HOVER_COLOR = new Color(0, 0, 0,  50);
    private static final Color PRESS_COLOR = new Color(0, 0, 0, 100);

    private ImageIcon originalIcon;
    private ImageIcon scaledIcon;
    private boolean   hovered = false;
    private boolean   pressed = false;
    private float     currentScale = 1.0f;
    private Timer     animationTimer;

    public AnimatedIconButton(ImageIcon icon, int size) {
        this.originalIcon = icon;
        this.scaledIcon   = scaleIcon(icon, size - 10);

        setPreferredSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        initListeners();
        initAnimationTimer();
    }

    private ImageIcon scaleIcon(ImageIcon icon, int targetSize) {
        Image img = icon.getImage().getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void initListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered      = false;
                pressed      = false;
                currentScale = 1.0f;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                startScaleAnimation(SCALE_DOWN_FACTOR);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                if (hovered) {
                    startScaleAnimation(1.0f);
                } else {
                    currentScale = 1.0f;
                }
                repaint();
            }
        });
    }

    private void initAnimationTimer() {
        float step = (1.0f - SCALE_DOWN_FACTOR) / (ANIMATION_DURATION / 10.0f);
        animationTimer = new Timer(10, e -> {
            if (pressed) {
                animationTimer.stop();
                return;
            }
            currentScale += step;
            if (currentScale >= 1.0f) {
                currentScale = 1.0f;
                animationTimer.stop();
            }
            repaint();
        });
    }

    private void startScaleAnimation(float targetScale) {
        if (animationTimer.isRunning()) animationTimer.stop();
        if (targetScale == 1.0f) {
            currentScale = SCALE_DOWN_FACTOR;
            animationTimer.start();
        } else {
            currentScale = targetScale;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        int diameter = Math.min(getWidth(), getHeight());
        int x        = (getWidth()  - diameter) / 2;
        int y        = (getHeight() - diameter) / 2;

        if (hovered || pressed) {
            g2.setColor(pressed ? PRESS_COLOR : HOVER_COLOR);
            g2.fillOval(x, y, diameter, diameter);
        }

        if (scaledIcon != null) {
            Image img       = scaledIcon.getImage();
            int   iconWidth  = (int) (img.getWidth(this)  * currentScale);
            int   iconHeight = (int) (img.getHeight(this) * currentScale);
            int   iconX      = (getWidth()  - iconWidth)  / 2;
            int   iconY      = (getHeight() - iconHeight) / 2;
            g2.drawImage(img, iconX, iconY, iconWidth, iconHeight, this);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public boolean contains(int x, int y) {
        int   radius   = Math.min(getWidth(), getHeight()) / 2;
        Point center   = new Point(getWidth() / 2, getHeight() / 2);
        return center.distance(x, y) < radius;
    }

    public void setIcon(ImageIcon icon) {
        this.originalIcon = icon;
        this.scaledIcon   = scaleIcon(icon, Math.min(getWidth(), getHeight()) - 10);
        repaint();
    }
}
