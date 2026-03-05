package com.gestion.salles.views.shared.users;

import com.gestion.salles.utils.RoundImage;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class UserTableCellRenderers {

    private UserTableCellRenderers() {}

    public static class ProfileImageRenderer extends DefaultTableCellRenderer {
        private final int imageSize;
        private final String nomColumn;
        private final String prenomColumn;
        private final Map<String, ImageIcon> imageCache = new HashMap<>();
        private final Map<String, ImageIcon> initialsCache = new HashMap<>();
        private boolean indicesCached = false;
        private int nomColumnIndex = -1;
        private int prenomColumnIndex = -1;

        public ProfileImageRenderer(String nomColumn, String prenomColumn, int imageSize) {
            this.nomColumn = nomColumn;
            this.prenomColumn = prenomColumn;
            this.imageSize = imageSize;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(null);
            label.setHorizontalAlignment(CENTER);
            label.setOpaque(true);

            if (!indicesCached) {
                try {
                    nomColumnIndex = table.getColumn(nomColumn).getModelIndex();
                    prenomColumnIndex = table.getColumn(prenomColumn).getModelIndex();
                    indicesCached = true;
                } catch (Exception e) {
                    return label;
                }
            }

            String imagePath = value instanceof String ? (String) value : null;
            ImageIcon finalIcon = null;

            if (imagePath != null && !imagePath.trim().isEmpty()) {
                finalIcon = imageCache.get(imagePath);
            }

            if (finalIcon == null) {
                ImageIcon originalIcon = UIUtils.getProfilePictureIcon(imagePath);
                if (originalIcon != null && originalIcon.getImage() != null) {
                    Image roundedActualImage = RoundImage.getRoundedImage(originalIcon.getImage(), imageSize);
                    finalIcon = new ImageIcon(roundedActualImage);
                    if (imagePath != null && !imagePath.trim().isEmpty()) {
                        imageCache.put(imagePath, finalIcon);
                    }
                } else {
                    int modelRow = table.convertRowIndexToModel(row);
                    String nom = safeString(table.getModel().getValueAt(modelRow, nomColumnIndex));
                    String prenom = safeString(table.getModel().getValueAt(modelRow, prenomColumnIndex));
                    String fullName = (prenom + " " + nom).trim();

                    finalIcon = initialsCache.get(fullName);
                    if (finalIcon == null) {
                        finalIcon = RoundImage.createInitialsRoundIcon(fullName, imageSize);
                        initialsCache.put(fullName, finalIcon);
                    }
                }
            }

            label.setIcon(finalIcon);
            label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return label;
        }

        private String safeString(Object value) {
            return value == null ? "" : value.toString();
        }
    }

    public static class DashCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            boolean isEmpty = value == null || value.toString().trim().isEmpty();
            if (isEmpty) {
                setText("-");
            }

            setOpaque(true);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
                return this;
            }

            setBackground(table.getBackground());
            setForeground(isEmpty ? ThemeConstants.MUTED_TEXT : ThemeConstants.PRIMARY_TEXT);
            return this;
        }
    }
}
