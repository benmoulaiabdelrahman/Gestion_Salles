package com.gestion.salles.views.shared.management;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ManagementExportUtils {

    public static final String UNIVERSITY_TITLE = "Université Amar Telidji de Laghouat";

    private ManagementExportUtils() {}

    public static String buildFilterInfo(String subject, String extra) {
        if (extra == null || extra.isBlank()) {
            return subject;
        }
        return subject + " - " + extra;
    }

    public static void exportTableToExcel(Component parent,
                                          JTable table,
                                          String sheetName,
                                          String subjectTitle,
                                          String filterInfo,
                                          Predicate<String> includeColumn) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Enregistrer sous");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers Excel", "xlsx"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getAbsolutePath().endsWith(".xlsx")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sheetName);

            List<String> visibleColumnNames = new ArrayList<>();
            List<Integer> visibleColumnModelIndices = new ArrayList<>();
            for (int i = 0; i < table.getColumnCount(); i++) {
                TableColumn tableColumn = table.getColumnModel().getColumn(i);
                String colName = tableColumn.getHeaderValue().toString();
                if (tableColumn.getWidth() <= 0) continue;
                if (includeColumn != null && !includeColumn.test(colName)) continue;
                visibleColumnNames.add(colName);
                visibleColumnModelIndices.add(tableColumn.getModelIndex());
            }

            int numberOfColumns = visibleColumnNames.size();
            if (numberOfColumns == 0) {
                JOptionPane.showMessageDialog(parent, "Aucune colonne visible à exporter.", "Avertissement", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int currentRowIndex = 0;

            Row universityTitleRow = sheet.createRow(currentRowIndex++);
            Cell universityTitleCell = universityTitleRow.createCell(0);
            universityTitleCell.setCellValue(UNIVERSITY_TITLE);
            if (numberOfColumns > 1) {
                sheet.addMergedRegion(new CellRangeAddress(universityTitleRow.getRowNum(), universityTitleRow.getRowNum(), 0, numberOfColumns - 1));
            }

            CellStyle universityTitleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font universityTitleFont = workbook.createFont();
            universityTitleFont.setBold(true);
            universityTitleFont.setFontHeightInPoints((short) 16);
            universityTitleStyle.setFont(universityTitleFont);
            universityTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            universityTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            universityTitleCell.setCellStyle(universityTitleStyle);

            String subtitle = buildFilterInfo(subjectTitle, filterInfo);
            if (subtitle != null && !subtitle.isBlank()) {
                Row filterTitleRow = sheet.createRow(currentRowIndex++);
                Cell filterTitleCell = filterTitleRow.createCell(0);
                filterTitleCell.setCellValue(subtitle);
                if (numberOfColumns > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(filterTitleRow.getRowNum(), filterTitleRow.getRowNum(), 0, numberOfColumns - 1));
                }

                CellStyle filterTitleStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font filterTitleFont = workbook.createFont();
                filterTitleFont.setFontHeightInPoints((short) 12);
                filterTitleStyle.setFont(filterTitleFont);
                filterTitleStyle.setAlignment(HorizontalAlignment.CENTER);
                filterTitleCell.setCellStyle(filterTitleStyle);
            }

            Row headerRow = sheet.createRow(currentRowIndex + 1);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < numberOfColumns; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(visibleColumnNames.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowNum = headerRow.getRowNum() + 1;
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < table.getRowCount(); i++) {
                Row row = sheet.createRow(rowNum++);
                for (int j = 0; j < visibleColumnModelIndices.size(); j++) {
                    int modelIndex = visibleColumnModelIndices.get(j);
                    Cell cell = row.createCell(j);
                    Object value = table.getModel().getValueAt(i, modelIndex);
                    cell.setCellValue(value != null ? value.toString() : "");
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < numberOfColumns; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(fileToSave)) {
                workbook.write(fileOut);
                JOptionPane.showMessageDialog(parent, "Fichier Excel enregistré avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Erreur lors de l'enregistrement du fichier Excel.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}
