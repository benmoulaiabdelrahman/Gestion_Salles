package com.gestion.salles.views.shared.management;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class TablePrintableBase implements Printable {

    protected final JTable tableToPrint;
    protected final String universityTitle;
    protected final String dynamicFilterInfo;
    protected final Image logo;
    protected final List<String> visibleColumns;

    // Constants for printing
    protected static final int FIXED_ROW_HEIGHT = 20;
    protected static final int CELL_PADDING_X = 5;
    protected static final int MIN_COLUMN_WIDTH = 30;
    protected static final int MARGIN = 20;

    protected TablePrintableBase(JTable tableToPrint, String universityTitle, String dynamicFilterInfo, List<String> visibleColumns) {
        this.tableToPrint = tableToPrint;
        this.universityTitle = universityTitle;
        this.dynamicFilterInfo = dynamicFilterInfo;
        this.visibleColumns = visibleColumns != null ? visibleColumns : new ArrayList<>();
        this.logo = loadLogo(getLogoPath());
    }

    protected String getLogoPath() {
        return "/icons/University_of_Laghouat_logo.png";
    }

    private Image loadLogo(String path) {
        try {
            URL imageUrl = getClass().getResource(path);
            if (imageUrl != null) {
                return new ImageIcon(imageUrl).getImage();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        Font universityTitleFont = new Font("SansSerif", Font.BOLD, 16);
        Font filterInfoFont = new Font("SansSerif", Font.PLAIN, 12);
        Font tableHeaderFont = new Font("SansSerif", Font.BOLD, 10);
        Font tableCellFont = new Font("SansSerif", Font.PLAIN, 9);

        int headerAreaHeight = 0;
        int logoHeight = 0;
        int logoWidth = 0;
        if (logo != null) {
            logoHeight = 50;
            logoWidth = (int) ((double) logo.getWidth(null) / logo.getHeight(null) * logoHeight);
            headerAreaHeight += logoHeight + MARGIN / 2;
        }

        g2d.setFont(universityTitleFont);
        headerAreaHeight += g2d.getFontMetrics().getHeight() + MARGIN / 4;

        if (dynamicFilterInfo != null && !dynamicFilterInfo.isEmpty()) {
            g2d.setFont(filterInfoFont);
            headerAreaHeight += g2d.getFontMetrics().getHeight() + MARGIN;
        }

        int tableHeaderHeight = FIXED_ROW_HEIGHT + 4;

        double printableHeight = pageFormat.getImageableHeight();
        double availableTableRowsHeight = printableHeight - headerAreaHeight - tableHeaderHeight - (MARGIN * 2);
        int rowsPerPage = (int) Math.floor(availableTableRowsHeight / FIXED_ROW_HEIGHT);
        if (rowsPerPage <= 0) rowsPerPage = 1;

        int totalRows = tableToPrint.getRowCount();
        int totalPages = (int) Math.ceil((double) totalRows / rowsPerPage);
        if (pageIndex >= totalPages) return NO_SUCH_PAGE;

        int currentY = 0;

        if (logo != null) {
            int logoX = (int) (pageFormat.getImageableWidth() - logoWidth) / 2;
            g2d.drawImage(logo, logoX, currentY + MARGIN / 2, logoWidth, logoHeight, null);
            currentY += logoHeight + MARGIN / 2;
        }

        if (universityTitle != null && !universityTitle.isEmpty()) {
            g2d.setFont(universityTitleFont);
            g2d.setColor(Color.BLACK);
            FontMetrics currentMetrics = g2d.getFontMetrics();
            int titleX = (int) (pageFormat.getImageableWidth() - currentMetrics.stringWidth(universityTitle)) / 2;
            g2d.drawString(universityTitle, titleX, currentY + currentMetrics.getAscent());
            currentY += currentMetrics.getHeight() + MARGIN / 4;
        }

        if (dynamicFilterInfo != null && !dynamicFilterInfo.isEmpty()) {
            g2d.setFont(filterInfoFont);
            FontMetrics currentMetrics = g2d.getFontMetrics();
            int filterX = (int) (pageFormat.getImageableWidth() - currentMetrics.stringWidth(dynamicFilterInfo)) / 2;
            g2d.drawString(dynamicFilterInfo, filterX, currentY + currentMetrics.getAscent());
            currentY += currentMetrics.getHeight() + MARGIN;
        }

        currentY += MARGIN / 2;

        List<Integer> columnWidths = getColumnWidths(g2d, tableHeaderFont, tableCellFont, tableToPrint, (int) pageFormat.getImageableWidth(), visibleColumns);
        int totalColumnsWidth = columnWidths.stream().mapToInt(Integer::intValue).sum();
        double columnScaleFactor = totalColumnsWidth > pageFormat.getImageableWidth()
            ? pageFormat.getImageableWidth() / totalColumnsWidth
            : 1.0;

        g2d.setFont(tableHeaderFont);
        int currentTableX = 0;

        for (int i = 0; i < visibleColumns.size(); i++) {
            String colName = visibleColumns.get(i);
            int colIndex = tableToPrint.getColumnModel().getColumnIndex(colName);
            int columnWidth = (int) (columnWidths.get(i) * columnScaleFactor);
            if (columnWidth <= 0) continue;

            g2d.setColor(Color.WHITE);
            g2d.fillRect(currentTableX, currentY, columnWidth, tableHeaderHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(currentTableX, currentY, columnWidth, tableHeaderHeight);

            FontMetrics headerFontMetrics = g2d.getFontMetrics(tableHeaderFont);
            String headerText = tableToPrint.getColumnName(colIndex);
            int textX = currentTableX + (columnWidth - headerFontMetrics.stringWidth(headerText)) / 2;
            int textY = currentY + (tableHeaderHeight - headerFontMetrics.getHeight()) / 2 + headerFontMetrics.getAscent();
            g2d.drawString(headerText, textX, textY);

            currentTableX += columnWidth;
        }
        currentY += tableHeaderHeight;

        int startRow = pageIndex * rowsPerPage;
        int endRow = Math.min(startRow + rowsPerPage, totalRows);

        for (int row = startRow; row < endRow; row++) {
            g2d.setFont(tableCellFont);
            int cellX = 0;
            for (int i = 0; i < visibleColumns.size(); i++) {
                String colName = visibleColumns.get(i);
                int colIndex = tableToPrint.getColumnModel().getColumnIndex(colName);
                int columnWidth = (int) (columnWidths.get(i) * columnScaleFactor);
                if (columnWidth <= 0) continue;

                g2d.setColor(Color.WHITE);
                g2d.fillRect(cellX, currentY, columnWidth, FIXED_ROW_HEIGHT);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(cellX, currentY, columnWidth, FIXED_ROW_HEIGHT);

                Object value = tableToPrint.getValueAt(row, colIndex);
                String text = (value == null) ? "" : value.toString();

                FontMetrics cellFontMetrics = g2d.getFontMetrics(tableCellFont);
                int textX = cellX + CELL_PADDING_X;
                int textY = currentY + (FIXED_ROW_HEIGHT - cellFontMetrics.getHeight()) / 2 + cellFontMetrics.getAscent();
                String clippedText = clipString(g2d, text, columnWidth - 2 * CELL_PADDING_X);
                g2d.drawString(clippedText, textX, textY);

                cellX += columnWidth;
            }
            currentY += FIXED_ROW_HEIGHT;
        }

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String footerText = String.format("Page %d / %d", pageIndex + 1, totalPages);
        FontMetrics footerMetrics = g2d.getFontMetrics();
        int footerX = (int) (pageFormat.getImageableWidth() - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = (int) printableHeight - footerMetrics.getDescent();
        g2d.drawString(footerText, footerX, footerY);

        return PAGE_EXISTS;
    }

    protected List<Integer> getColumnWidths(Graphics2D g2d, Font headerFont, Font cellFont, JTable table,
                                            int printableWidth, List<String> visibleColumns) {
        List<Integer> widths = new ArrayList<>();
        int totalPreferredWidth = 0;

        for (String colName : visibleColumns) {
            int col = table.getColumnModel().getColumnIndex(colName);
            int maxWidth = MIN_COLUMN_WIDTH;

            g2d.setFont(headerFont);
            FontMetrics headerMetrics = g2d.getFontMetrics();
            String headerText = table.getColumnName(col);
            maxWidth = Math.max(maxWidth, headerMetrics.stringWidth(headerText) + 2 * CELL_PADDING_X);

            g2d.setFont(cellFont);
            FontMetrics cellMetrics = g2d.getFontMetrics();
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, col);
                String cellText = (value == null) ? "" : value.toString();
                maxWidth = Math.max(maxWidth, cellMetrics.stringWidth(cellText) + 2 * CELL_PADDING_X);
            }
            widths.add(maxWidth);
            totalPreferredWidth += maxWidth;
        }

        double scaleFactor = totalPreferredWidth > 0 ? (double) printableWidth / totalPreferredWidth : 1.0;
        for (int i = 0; i < widths.size(); i++) {
            widths.set(i, Math.max(MIN_COLUMN_WIDTH, (int) (widths.get(i) * scaleFactor)));
        }
        return widths;
    }

    protected String clipString(Graphics2D g2d, String text, int maxWidth) {
        FontMetrics metrics = g2d.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }

        String clip = "...";
        int clipWidth = metrics.stringWidth(clip);
        if (maxWidth <= clipWidth) {
            return clip;
        }

        int charWidth = 0;
        int i = 0;
        for (; i < text.length(); i++) {
            charWidth += metrics.charWidth(text.charAt(i));
            if (charWidth + clipWidth > maxWidth) {
                break;
            }
        }
        return text.substring(0, i) + clip;
    }
}
