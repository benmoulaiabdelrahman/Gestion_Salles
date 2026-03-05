package com.gestion.salles.utils;

/******************************************************************************
 * UIUtils.java
 *
 * Utility class for common UI component creation and styling. Provides
 * factory methods for themed text fields, password fields, buttons, combo
 * boxes, labels, tables, panels, and toast notifications, ensuring visual
 * consistency across the application.
 ******************************************************************************/

import com.formdev.flatlaf.FlatClientProperties;
import com.gestion.salles.services.AuthService;
import com.gestion.salles.views.Login.LoginFrame;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UIUtils {
    private static final Logger LOGGER = Logger.getLogger(UIUtils.class.getName());

    private static final String HEX_DEFAULT_BORDER = colorToHex(ThemeConstants.DEFAULT_BORDER);
    private static final String HEX_FOCUS_BORDER   = colorToHex(ThemeConstants.FOCUS_BORDER);
    private static final String HEX_PRIMARY_TEXT   = colorToHex(ThemeConstants.PRIMARY_TEXT);
    private static final String HEX_MUTED_TEXT     = colorToHex(ThemeConstants.MUTED_TEXT);

    private static final String CLIENT_PROPERTY_PLACEHOLDER_FOREGROUND        = "JTextField.placeholderForeground";
    private static final String CLIENT_PROPERTY_TEXT_FIELD_SHOW_REVEAL_BUTTON = "JTextField.showRevealButton";
    private static final String CLIENT_PROPERTY_TEXT_FIELD_SHOW_CAPS_LOCK     = "JTextField.showCapsLock";

    public static final String SELECT_PLACEHOLDER = "Sélectionner...";

    private static final String[] APP_ICON_PATHS = {
        "/icons/University_of_Laghouat_logo_64x64.png",
        "/icons/University_of_Laghouat_logo.png"
    };

    private static volatile List<Image> appIconImages;

    public static void logout(JFrame currentFrame) {
        AuthService.getInstance().logout();
        currentFrame.dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    public static void applyAppIcon(Window window) {
        if (window == null) return;
        List<Image> icons = getAppIconImages();
        if (!icons.isEmpty()) {
            window.setIconImages(icons);
        }
    }

    public static List<Image> getAppIconImages() {
        if (appIconImages != null) return appIconImages;
        synchronized (UIUtils.class) {
            if (appIconImages != null) return appIconImages;
            List<Image> images = new ArrayList<>();
            for (String path : APP_ICON_PATHS) {
                URL url = UIUtils.class.getResource(path);
                if (url != null) {
                    ImageIcon icon = new ImageIcon(url);
                    if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                        images.add(icon.getImage());
                    }
                }
            }
            if (images.isEmpty()) {
                URL fallback = UIUtils.class.getResource("/icons/University_of_Laghouat_logo.png");
                if (fallback != null) {
                    images.add(new ImageIcon(fallback).getImage());
                }
            }
            appIconImages = Collections.unmodifiableList(images);
            return appIconImages;
        }
    }

    public static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;borderWidth:1;borderColor:" + HEX_DEFAULT_BORDER + ";focusedBorderColor:#0F6B3F");
        field.putClientProperty(CLIENT_PROPERTY_PLACEHOLDER_FOREGROUND, ThemeConstants.MUTED_TEXT);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 35));
        return field;
    }

    public static JTextArea createStyledTextArea(String placeholder, int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        textArea.putClientProperty(FlatClientProperties.STYLE, "");
        textArea.putClientProperty(CLIENT_PROPERTY_PLACEHOLDER_FOREGROUND, ThemeConstants.PRIMARY_TEXT);
        return textArea;
    }

    public static JPasswordField createStyledPasswordField(String placeholder, boolean showRevealButton, boolean showCapsLock) {
        JPasswordField field = new JPasswordField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;" +
            "borderWidth:1;" +
            "borderColor:" + HEX_DEFAULT_BORDER + ";" +
            "focusedBorderColor:#0F6B3F;" +
            "focusWidth:1;" +
            "innerFocusWidth:0;" +
                    "showRevealButton:" + showRevealButton + ";" + // FlatLaf client property to show/hide password reveal button
                    "showCapsLock:" + showCapsLock);             // FlatLaf client property to show Caps Lock indicator
                field.putClientProperty(CLIENT_PROPERTY_PLACEHOLDER_FOREGROUND, ThemeConstants.MUTED_TEXT);
                field.setPreferredSize(new Dimension(field.getPreferredSize().width, 35));
                return field;
            }
    public static JPasswordField createStyledPasswordField(String placeholder) {
        return createStyledPasswordField(placeholder, false, false);
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        stylePrimaryButton(button);
        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ThemeConstants.CARD_WHITE);
        button.setForeground(ThemeConstants.PRIMARY_TEXT);
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;borderWidth:1;borderColor:" + HEX_DEFAULT_BORDER + ";focusedBorderColor:#0F6B3F;");
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 35));
        return button;
    }

    public static void stylePrimaryButton(JButton button) {
        if (button == null) return;
        button.setBackground(ThemeConstants.PRIMARY_GREEN);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.putClientProperty("JButton.arc", 10);
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 35));
    }

    public static JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ThemeConstants.ERROR_RED);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.putClientProperty("JButton.arc", 10);
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 35));
        return button;
    }

    public static JButton createIconButton(String iconName, int width, int height) {
        JButton button = new JButton();
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (iconName != null && !iconName.trim().isEmpty()) {
            try {
                URL iconUrl = UIUtils.class.getResource("/icons/" + iconName);
                if (iconUrl != null) {
                    Image iconImage = new ImageIcon(iconUrl).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    button.setIcon(new ImageIcon(iconImage));
                } else {
                    System.err.println("Icon not found: /icons/" + iconName);
                }
            } catch (Exception e) {
                System.err.println("Error loading icon " + iconName + ": " + e.getMessage());
            }
        }
        return button;
    }

    public static JLabel createProfilePictureLabel(String fileName, int width, int height) {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(width, height));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);

        ImageIcon icon = getProfilePictureIcon(fileName);
        if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaled));
        } else {
            label.setOpaque(true);
            label.setBackground(ThemeConstants.NAV_BACKGROUND);
            label.setForeground(ThemeConstants.MUTED_TEXT);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
            label.setText("?");
        }
        label.setBorder(BorderFactory.createLineBorder(ThemeConstants.DEFAULT_BORDER, 1));
        return label;
    }

    public static <T> JComboBox<T> createStyledComboBox(JComboBox<T> comboBox) {
        comboBox.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;borderWidth:1;focusWidth:2;innerFocusWidth:0;" +
            "borderColor:" + HEX_DEFAULT_BORDER + ";focusedBorderColor:#0F6B3F");
        return comboBox;
    }

    public static JButton createLinkButton(String text) {
        return createLinkButton(text, null, Font.PLAIN, 13f);
    }

    public static JButton createLinkButton(String text, String baseIconName) {
        return createLinkButton(text, baseIconName, Font.PLAIN, 13f);
    }

    public static JButton createLinkButton(String text, String baseIconName, int fontStyle, float fontSize) {
        JButton button = new JButton(text);
        button.setForeground(ThemeConstants.PRIMARY_GREEN);
        button.setFont(button.getFont().deriveFont(fontStyle, fontSize));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ImageIcon darkIcon  = null;
        ImageIcon lightIcon = null;
        if (baseIconName != null && !baseIconName.isEmpty()) {
            try {
                int iconSize = 16;
                darkIcon  = new ImageIcon(new ImageIcon(UIUtils.class.getResource("/icons/" + baseIconName + "_dark.png")).getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
                lightIcon = new ImageIcon(new ImageIcon(UIUtils.class.getResource("/icons/" + baseIconName + "_light.png")).getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
                button.setIcon(darkIcon);
                button.setHorizontalTextPosition(SwingConstants.RIGHT);
                button.setIconTextGap(5);
            } catch (Exception e) {
                System.err.println("Error loading icons for " + baseIconName + ": " + e.getMessage());
            }
        }

        final ImageIcon finalDarkIcon  = darkIcon;
        final ImageIcon finalLightIcon = lightIcon;

        button.addMouseListener(new MouseAdapter() {
            private Timer animationTimer;
            private int   currentIconTextGap  = 5;
            private final int animationDistance = 5;
            private final int animationStep     = 1;
            private final int animationDelay    = 10;

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(ThemeConstants.PRIMARY_GREEN_HOVER);
                if (finalLightIcon != null) button.setIcon(finalLightIcon);
                stopTimer();
                animationTimer = new Timer(animationDelay, evt -> {
                    if (currentIconTextGap > (5 - animationDistance)) {
                        button.setIconTextGap(--currentIconTextGap);
                    } else {
                        ((Timer) evt.getSource()).stop();
                    }
                });
                animationTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(ThemeConstants.PRIMARY_GREEN);
                if (finalDarkIcon != null) button.setIcon(finalDarkIcon);
                stopTimer();
                animationTimer = new Timer(animationDelay, evt -> {
                    if (currentIconTextGap < 5) {
                        button.setIconTextGap(++currentIconTextGap);
                    } else {
                        ((Timer) evt.getSource()).stop();
                    }
                });
                animationTimer.start();
            }

            private void stopTimer() {
                if (animationTimer != null && animationTimer.isRunning()) animationTimer.stop();
            }
        });

        return button;
    }

    public static void setNullableInt(PreparedStatement pstmt, int parameterIndex, Integer value) throws SQLException {
        if (value != null) pstmt.setInt(parameterIndex, value);
        else               pstmt.setNull(parameterIndex, Types.INTEGER);
    }

    public static void showTemporaryMessage(Component parentComponent, String message, boolean isSuccess, int durationMillis) {
        SwingUtilities.invokeLater(() -> {
            Color bgColor = isSuccess ? new Color(76, 175, 80, 255) : new Color(244, 67, 54, 255);

            JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
            messageLabel.setOpaque(true);
            messageLabel.setBackground(bgColor);
            messageLabel.setForeground(Color.WHITE);
            messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 14f));
            messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            messageLabel.putClientProperty("FlatPanel.arc", 15);

            Dimension preferred      = messageLabel.getPreferredSize();
            int       notifWidth     = preferred.width  + 40;
            int       notifHeight    = preferred.height + 20;

            JRootPane rootPane = SwingUtilities.getRootPane(parentComponent);
            if (rootPane == null) {
                System.err.println("Error: Could not find JRootPane for parent component.");
                return;
            }
            JLayeredPane layeredPane = rootPane.getLayeredPane();
            messageLabel.setBounds(
                (layeredPane.getWidth()  - notifWidth)  / 2,
                layeredPane.getHeight() - notifHeight - 20,
                notifWidth, notifHeight
            );
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageLabel.setVerticalAlignment(SwingConstants.CENTER);
            layeredPane.add(messageLabel, JLayeredPane.POPUP_LAYER);

            Timer timer = new Timer(durationMillis, e -> {
                layeredPane.remove(messageLabel);
                layeredPane.revalidate();
                layeredPane.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    public static boolean isValidEmail(String email) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return false;
        if (email.contains(".. ") || email.startsWith(". ") || email.endsWith(". ")) return false;
        String[] parts = email.split("@");
        return parts.length == 2 && parts[1].split("\\.").length >= 2;
    }

    public static ImageIcon getProfilePictureIcon(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return null;

        String rawValue = fileName.trim();
        String baseName = extractBaseName(rawValue);
        Set<File> candidates = new LinkedHashSet<>();

        addPathCandidate(candidates, rawValue);
        addUriPathCandidate(candidates, rawValue);
        addRuntimeProfilePictureCandidates(candidates, rawValue, baseName);

        candidates.add(new File(AppConfig.getProfilePictureDirectory(), rawValue));
        if (!baseName.equals(rawValue)) {
            candidates.add(new File(AppConfig.getProfilePictureDirectory(), baseName));
        }

        candidates.add(new File("uploads/profile-pictures", rawValue));
        if (!baseName.equals(rawValue)) {
            candidates.add(new File("uploads/profile-pictures", baseName));
        }

        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isFile()) {
                ImageIcon icon = createValidIcon(candidate.getAbsolutePath());
                if (icon != null) return icon;
            }
        }

        URL profileResourceUrl = UIUtils.class.getResource("/profile-pictures/" + baseName);
        if (profileResourceUrl != null) {
            ImageIcon icon = createValidIcon(profileResourceUrl);
            if (icon != null) return icon;
        }

        URL resourceUrl = UIUtils.class.getResource("/icons/" + baseName);
        if (resourceUrl != null) {
            ImageIcon icon = createValidIcon(resourceUrl);
            if (icon != null) return icon;
        }

        if (LOGGER.isLoggable(Level.WARNING)) {
            StringBuilder sb = new StringBuilder("Profile image not found. raw='")
                .append(rawValue)
                .append("', base='")
                .append(baseName)
                .append("'. Checked candidates:");
            for (File candidate : candidates) {
                if (candidate != null) {
                    sb.append(System.lineSeparator()).append(" - ").append(candidate.getAbsolutePath());
                }
            }
            sb.append(System.lineSeparator()).append(" - classpath:/profile-pictures/").append(baseName);
            sb.append(System.lineSeparator()).append(" - classpath:/icons/").append(baseName);
            LOGGER.warning(sb.toString());
        }

        return null;
    }

    private static String extractBaseName(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return (lastSlash >= 0 && lastSlash + 1 < normalized.length())
            ? normalized.substring(lastSlash + 1)
            : normalized;
    }

    private static void addPathCandidate(Set<File> candidates, String value) {
        try {
            Path path = Paths.get(value);
            candidates.add(path.toFile());
        } catch (InvalidPathException ignored) {
            // Ignore malformed filesystem paths and continue other resolution strategies.
        }
    }

    private static void addUriPathCandidate(Set<File> candidates, String value) {
        if (!value.startsWith("file:")) return;
        try {
            candidates.add(Paths.get(new URI(value)).toFile());
        } catch (URISyntaxException | InvalidPathException ignored) {
            // Ignore malformed URI values and continue other resolution strategies.
        }
    }

    private static void addRuntimeProfilePictureCandidates(Set<File> candidates, String rawValue, String baseName) {
        File codeSourceDir = resolveCodeSourceDirectory();
        if (codeSourceDir == null) return;

        addDirectoryCandidates(candidates, codeSourceDir, rawValue, baseName);

        File current = codeSourceDir;
        for (int i = 0; i < 3; i++) {
            current = current.getParentFile();
            if (current == null) break;
            addDirectoryCandidates(candidates, current, rawValue, baseName);
        }
    }

    private static void addDirectoryCandidates(Set<File> candidates, File rootDir, String rawValue, String baseName) {
        candidates.add(new File(rootDir, "profile-pictures" + File.separator + rawValue));
        if (!baseName.equals(rawValue)) {
            candidates.add(new File(rootDir, "profile-pictures" + File.separator + baseName));
        }
    }

    private static File resolveCodeSourceDirectory() {
        try {
            URL location = UIUtils.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) return null;
            File source = Paths.get(location.toURI()).toFile();
            return source.isFile() ? source.getParentFile() : source;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ImageIcon createValidIcon(String absolutePath) {
        try {
            ImageIcon icon = new ImageIcon(absolutePath);
            return (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) ? icon : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ImageIcon createValidIcon(URL url) {
        try {
            ImageIcon icon = new ImageIcon(url);
            return (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) ? icon : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");
        return panel;
    }

    public static JPanel createLoginCardPanel(String insets, String columnConstraints) {
        JPanel panel = new JPanel(new MigLayout("wrap,fillx," + insets, columnConstraints));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");
        return panel;
    }

    public static JPanel createLoginHeaderPanel(String imagePath, int iconSize, String fallbackEmoji,
                                                String titleText, String descriptionHtml, String dynamicEmail) {
        JPanel headerPanel = new JPanel(new MigLayout("wrap,fillx,insets 0", "[center]"));
        headerPanel.setBackground(null);

        Font fallbackFont = UIManager.getDefaults().getFont("Label.font");
        if (fallbackFont == null || fallbackFont.getName() == null) fallbackFont = new Font("SansSerif", Font.PLAIN, 12);

        JLabel lblIcon = createIconLabel(imagePath, iconSize, fallbackEmoji, fallbackFont.deriveFont(Font.PLAIN, (float) iconSize));
        lblIcon.setOpaque(false);
        headerPanel.add(lblIcon, "alignx center,gapy 0 10");

        headerPanel.add(createStyledLabel(
            titleText,
            UIManager.getDefaults().getFont("Label.font").deriveFont(Font.BOLD, 24f),
            ThemeConstants.PRIMARY_TEXT, SwingConstants.CENTER), "alignx center");

        String finalDescription = (dynamicEmail != null) ? String.format(descriptionHtml, dynamicEmail) : descriptionHtml;
        headerPanel.add(createStyledLabel(
            finalDescription,
            UIManager.getDefaults().getFont("Label.font").deriveFont(Font.PLAIN, 14f),
            ThemeConstants.SECONDARY_TEXT, SwingConstants.CENTER), "alignx center,gapy 0 20");

        return headerPanel;
    }

    public static JPanel createCardDialogPanel(String insets, String layoutConstraints) {
        JPanel panel = new JPanel(new MigLayout("wrap, fill, " + insets, layoutConstraints));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");
        return panel;
    }

    public static JPanel createFeedbackPanel(String iconPath, int iconSize, String fallbackEmoji,
                                             String titleText, String messageHtml, Color iconColor) {
        JPanel panel = new JPanel(new MigLayout("wrap, fill, insets 50 45 40 45", "[center]", "[grow 0]20[grow 0]20[grow 0]"));
        panel.setBackground(ThemeConstants.CARD_WHITE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20");

        JLabel lblIcon = createIconLabel(iconPath, iconSize, fallbackEmoji,
            UIManager.getDefaults().getFont("Label.font").deriveFont(Font.PLAIN, (float) iconSize));
        if (iconColor != null) lblIcon.setForeground(iconColor);
        panel.add(lblIcon, "align center");

        JLabel lblTitle = new JLabel(titleText);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 28f));
        lblTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        panel.add(lblTitle, "align center");

        JLabel lblMessage = new JLabel(messageHtml);
        lblMessage.setFont(lblMessage.getFont().deriveFont(14f));
        lblMessage.setForeground(ThemeConstants.SECONDARY_TEXT);
        panel.add(lblMessage, "align center, gapy 0 30");

        return panel;
    }

    public static JTextArea createReadOnlyCopyableTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setFont(ThemeConstants.FONT_REGULAR_13);
        textArea.setForeground(ThemeConstants.SECONDARY_TEXT);
        textArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
        textArea.setOpaque(false);
        textArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        textArea.setCaret(new DefaultCaret() {
            @Override public void paint(Graphics g) {}
        });
        textArea.setFocusable(true);
        return textArea;
    }

    public static JLabel createIconLabel(String imagePath, int size, String fallbackText, Font fallbackFont) {
        JLabel label = new JLabel();
        Font safeFont = (fallbackFont != null && fallbackFont.getName() == null)
            ? new Font("SansSerif", Font.PLAIN, fallbackFont.getSize()) : fallbackFont;

        if (imagePath == null || imagePath.trim().isEmpty()) {
            label.setText(fallbackText);
            label.setFont(safeFont);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            return label;
        }

        try {
            URL imageUrl = UIUtils.class.getResource(imagePath);
            if (imageUrl != null) {
                label.setIcon(new ImageIcon(new ImageIcon(imageUrl).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH)));
            } else {
                System.err.println("Icon not found at path: " + imagePath);
                label.setText(fallbackText);
                label.setFont(safeFont);
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + imagePath + " - " + e.getMessage());
            label.setText(fallbackText);
            label.setFont(safeFont);
        }
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public static void openEmailClient(String recipientEmail) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            try {
                Desktop.getDesktop().mail(new URI("mailto:" + recipientEmail));
            } catch (Exception e) {
                System.err.println("Error opening email client: " + e.getMessage());
                JOptionPane.showMessageDialog(null,
                    "Impossible d'ouvrir le client de messagerie. Veuillez contacter " + recipientEmail + " manuellement.",
                    "Erreur d'ouverture d'e-mail", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.err.println("Desktop Mail action not supported on this platform.");
            JOptionPane.showMessageDialog(null,
                "L'ouverture du client de messagerie n'est pas prise en charge sur ce système. Veuillez contacter " + recipientEmail + " manuellement.",
                "Fonctionnalité non prise en charge", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static JScrollPane createStyledTable(TableModel tableModel) {
        JTable table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(table.getFont().deriveFont(13f));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.putClientProperty("FlatLaf.table.alternateRowColor", true);
        table.setShowGrid(true);
        table.setGridColor(ThemeConstants.DEFAULT_BORDER);

        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        header.setBackground(ThemeConstants.NAV_BACKGROUND);
        header.setForeground(ThemeConstants.PRIMARY_TEXT);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        header.putClientProperty("JTableHeader.hoverBackground", ThemeConstants.NAV_BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeConstants.DEFAULT_BORDER, 1));
        scrollPane.getViewport().setBackground(ThemeConstants.CARD_WHITE);
        return scrollPane;
    }

    public static JPanel createSearchActionBar(JTextField searchField, JButton... actionButtons) {
        JPanel toolbar = new JPanel(new MigLayout("insets 10 10 10 10, fillx, gap 5", "[fill,grow][]"));
        toolbar.setOpaque(false);
        toolbar.add(searchField, "growx, h 35!");
        for (JButton button : actionButtons) toolbar.add(button, "h 35!");
        return toolbar;
    }

    public static boolean isCardCurrentlyVisible(JPanel container, String cardName) {
        for (Component comp : container.getComponents()) {
            if (comp.isVisible() && cardName.equals(comp.getName())) return true;
        }
        return false;
    }

    public static JLabel createRequiredFieldIndicator() {
        JLabel label = new JLabel("*");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setForeground(ThemeConstants.ERROR_RED);
        label.setVisible(false);
        return label;
    }

    public static JLabel createStyledLabel(String text, Font font, Color foreground, int horizontalAlignment) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(foreground);
        label.setHorizontalAlignment(horizontalAlignment);
        return label;
    }

    public static JCheckBox createStyledCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setFont(checkBox.getFont().deriveFont(13f));
        checkBox.setForeground(ThemeConstants.SECONDARY_TEXT);
        checkBox.setBackground(null);
        checkBox.setFocusPainted(false);
        checkBox.putClientProperty(FlatClientProperties.STYLE,
            "icon.checkmarkColor:"      + colorToHex(ThemeConstants.CARD_WHITE) + ";" +
            "icon.focusedBackground:"   + colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
            "icon.selectedBackground:"  + colorToHex(ThemeConstants.PRIMARY_GREEN) + ";" +
            "icon.hoverBackground:"     + colorToHex(ThemeConstants.PRIMARY_GREEN_HOVER));
        return checkBox;
    }

    public static JLabel createErrorLabel() {
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(ThemeConstants.ERROR_RED);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public static void showTemporaryErrorMessage(JLabel errorLabel, String message, int durationMillis) {
        errorLabel.setText(message);
        Timer timer = new Timer(durationMillis, e -> errorLabel.setText(""));
        timer.setRepeats(false);
        timer.start();
    }

    public static void showTemporaryStatusMessage(JLabel statusLabel, String message, Color foreground, int durationMillis) {
        statusLabel.setText(message);
        statusLabel.setForeground(foreground);
        Timer timer = new Timer(durationMillis, e -> {
            statusLabel.setText("");
            statusLabel.setForeground(ThemeConstants.ERROR_RED);
        });
        timer.setRepeats(false);
        timer.start();
    }
}
