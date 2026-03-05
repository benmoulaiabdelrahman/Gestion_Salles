package com.gestion.salles.views.Enseignant;

import com.gestion.salles.dao.ScheduleDAO;
import com.gestion.salles.models.ScheduleEntry;
import com.gestion.salles.models.User;
import com.gestion.salles.utils.RefreshablePanel;
import com.gestion.salles.utils.ThemeConstants;
import com.gestion.salles.utils.UIUtils; // Import UIUtils for styling
import com.gestion.salles.views.Enseignant.ScheduleTableCellRenderer; // Added missing import
import com.gestion.salles.views.shared.schedule.SchedulePanelBase;
import com.gestion.salles.views.shared.schedule.ScheduleUiText;
import net.miginfocom.swing.MigLayout;

import java.awt.print.PrinterException; // Added import
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File; // Added import
import javax.swing.filechooser.FileNameExtensionFilter; // Added import
import org.apache.pdfbox.pdmodel.PDDocument; // Added import
import org.apache.pdfbox.pdmodel.PDPage; // Added import
import org.apache.pdfbox.pdmodel.PDPageContentStream; // Added import
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font; // Added import
import org.apache.pdfbox.pdmodel.common.PDRectangle; // Added import
import javax.imageio.ImageIO;
import java.net.URL;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level; // Added for LOGGER.log(Level.SEVERE, ...)
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.table.JTableHeader;


public class MySchedulePanel extends SchedulePanelBase implements RefreshablePanel {

    private static final Logger LOGGER = Logger.getLogger(MySchedulePanel.class.getName());

    private final User currentUser;
    private final boolean embedded;
    private ScheduleDAO scheduleDAO;

    private JCheckBox allDatesCheckBox;

    private LocalDate startDate; // Start date of the current view (e.g., start of the week/month)
    private LocalDate endDate;   // End date of the current view


    public MySchedulePanel(User currentUser) {
        this(currentUser, false);
    }

    public MySchedulePanel(User currentUser, boolean embedded) {
        this.currentUser = currentUser;
        this.embedded = embedded;
        this.scheduleDAO = new ScheduleDAO();
        this.startDate = LocalDate.now();
        this.endDate = LocalDate.now();
        initComponents();
        refreshData();
    }

    private void initComponents() {
        String insets = embedded ? "insets 0, fill" : "insets 20, fill";
        setLayout(new MigLayout(insets, "[grow]", "[][grow, fill]"));
        if (embedded) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        } else {
            setBackground(ThemeConstants.CARD_WHITE);
            putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:20");
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        JPanel controlsContainer = new JPanel(new MigLayout("insets 0, fillx", "[left][grow, right]", "[]"));
        controlsContainer.setOpaque(false);

        JLabel scheduleTitle = new JLabel("Mon emploi du temps");
        scheduleTitle.setFont(scheduleTitle.getFont().deriveFont(Font.BOLD, 18f));
        scheduleTitle.setForeground(ThemeConstants.PRIMARY_TEXT);
        controlsContainer.add(scheduleTitle, "align left");

        JPanel detailsFilterRow = new JPanel(new MigLayout("insets 0, gap 15", "[][]"));
        detailsFilterRow.setOpaque(false);

        allDatesCheckBox = UIUtils.createStyledCheckBox(ScheduleUiText.CHECKBOX_FULL_WEEK);
        allDatesCheckBox.addActionListener(e -> {
            loadScheduleData();
        });
        detailsFilterRow.add(allDatesCheckBox, "h 38!");

        JButton btnSavePdf = UIUtils.createSecondaryButton(ScheduleUiText.BUTTON_EXPORT_PDF);
        btnSavePdf.addActionListener(e -> onSaveScheduleAsPdf());
        detailsFilterRow.add(btnSavePdf, "h 38!");

        JButton btnPrint = UIUtils.createSecondaryButton(ScheduleUiText.BUTTON_PRINT);
        btnPrint.addActionListener(e -> onPrintSchedule());
        detailsFilterRow.add(btnPrint, "h 38!");

        controlsContainer.add(detailsFilterRow, "align right");
        add(controlsContainer, "wrap, growx");

        // Schedule View (CENTER) - JTable
        JScrollPane scrollPane = createScheduleTable(new ScheduleTableCellRenderer(this));

        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> adjustRowHeight());
            }
        });

        add(scrollPane, "grow, push");
    }

    @Override
    public void refreshData() {
        loadScheduleData();
    }

    private void loadScheduleData() {
        // Enseignant's schedule is filtered by teacher ID
        Integer teacherId = currentUser.getIdUtilisateur();
        if (teacherId == null) {
            JOptionPane.showMessageDialog(this, "Erreur: ID Enseignant non disponible.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final LocalDate loadStartDate;
        final LocalDate loadEndDate;
        if (allDatesCheckBox.isSelected()) {
            loadStartDate = calculateStartOfWeek(startDate);
            loadEndDate = loadStartDate.plusDays(6);
        } else {
            loadStartDate = startDate;
            loadEndDate = startDate;
        }

        loadScheduleAsync(
            () -> {
                try {
                    return scheduleDAO.getScheduleEntriesForTeacher(teacherId, loadStartDate, loadEndDate);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            false,
            null,
            e -> JOptionPane.showMessageDialog(MySchedulePanel.this,
                "Erreur lors du chargement de l'horaire: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void populateScheduleGrid(List<ScheduleEntry> entries) {
        populateScheduleGrid(entries, false, null);
    }

    // Since this is for a single teacher, conflict should not ideally occur.
    // However, the ScheduleGridCellPanel handles conflicts if they somehow appear.

    protected void adjustRowHeight() {
        super.adjustRowHeight();
    }

    private LocalDate calculateStartOfWeek(LocalDate date) {
        LocalDate d = date;
        while (d.getDayOfWeek() != DayOfWeek.SATURDAY) {
            d = d.minusDays(1);
        }
        return d;
    }

    // Date picker intentionally omitted for Enseignant view.

    private void onPrintSchedule() {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        if (printerJob.printDialog()) {
            try {
                printerJob.setPrintable(new Printable() {
                    @Override
                    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                        if (pageIndex > 0) {
                            return NO_SUCH_PAGE;
                        }

                        Graphics2D g2d = (Graphics2D) graphics;
                        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        double yPosition = 0;
                        double pageWidth = pageFormat.getImageableWidth();
                        double pageHeight = pageFormat.getImageableHeight();

                        // --- Header: Logo + Titles ---
                        Image logo = getPrintableLogo();
                        if (logo != null) {
                            int logoHeight = 40;
                            int logoWidth = logo.getWidth(null) * logoHeight / logo.getHeight(null);
                            double logoX = (pageWidth - logoWidth) / 2;
                            g2d.drawImage(logo, (int) logoX, (int) yPosition, logoWidth, logoHeight, null);
                            yPosition += logoHeight + 10;
                        }

                        String universityTitle = "Université Amar Telidji de Laghouat";
                        g2d.setFont(new Font("Helvetica", Font.BOLD, 14));
                        FontMetrics titleMetrics = g2d.getFontMetrics();
                        double titleX = (pageWidth - titleMetrics.stringWidth(universityTitle)) / 2;
                        yPosition += titleMetrics.getAscent();
                        g2d.drawString(universityTitle, (int) titleX, (int) yPosition);

                        String dynamicFilterInfo = getDynamicFilterInfo();
                        g2d.setFont(new Font("Helvetica", Font.PLAIN, 10));
                        FontMetrics filterMetrics = g2d.getFontMetrics();
                        double filterX = (pageWidth - filterMetrics.stringWidth(dynamicFilterInfo)) / 2;
                        yPosition += filterMetrics.getAscent() + 5;
                        g2d.drawString(dynamicFilterInfo, (int) filterX, (int) yPosition);

                        yPosition += 15;

                        // --- Screenshot table ---
                        int tableWidth = scheduleTable.getWidth();
                        int tableHeight = scheduleTable.getHeight();
                        int headerHeight = scheduleTable.getTableHeader().getHeight();
                        int totalHeight = tableHeight + headerHeight;

                        BufferedImage tableImage = new BufferedImage(tableWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D imageGraphics = tableImage.createGraphics();
                        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        imageGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        imageGraphics.setColor(ThemeConstants.CARD_WHITE);
                        imageGraphics.fillRect(0, 0, tableWidth, totalHeight);
                        scheduleTable.getTableHeader().paint(imageGraphics);
                        imageGraphics.translate(0, headerHeight);
                        scheduleTable.paint(imageGraphics);
                        imageGraphics.dispose();

                        double availableHeight = pageHeight - yPosition;
                        double scale = Math.min(pageWidth / tableWidth, availableHeight / totalHeight);
                        int scaledWidth = (int) (tableWidth * scale);
                        int scaledHeight = (int) (totalHeight * scale);
                        int imageX = (int) ((pageWidth - scaledWidth) / 2);

                        g2d.drawImage(tableImage, imageX, (int) yPosition, scaledWidth, scaledHeight, null);
                        return PAGE_EXISTS;
                    }
                });
                printerJob.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(MySchedulePanel.this, ScheduleUiText.PRINT_ERROR_MESSAGE, ScheduleUiText.PRINT_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.SEVERE, "Error printing schedule table", e);
            }
        }
    }

    private void onSaveScheduleAsPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(ScheduleUiText.PDF_DIALOG_TITLE);
        fileChooser.setFileFilter(new FileNameExtensionFilter(ScheduleUiText.PDF_FILTER_LABEL, "pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        File fileToSave = fileChooser.getSelectedFile();
        String filePath = fileToSave.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".pdf")) filePath += ".pdf";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); // Landscape
            document.addPage(page);

            PDPageContentStream contents = new PDPageContentStream(document, page);

            float margin = 30;
            float pageHeight = page.getMediaBox().getHeight();
            float pageWidth = page.getMediaBox().getWidth();
            float yPosition = pageHeight - margin;

            float logoHeight = 50;
            PDImageXObject uniLogo = null;
            try {
                uniLogo = loadImage(document, "/icons/University_of_Laghouat_logo.png");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading logo: " + e.getMessage(), e);
            }

            if (uniLogo != null) {
                float logoWidth = uniLogo.getWidth() * (logoHeight / uniLogo.getHeight());
                float logoX = (pageWidth - logoWidth) / 2;
                contents.drawImage(uniLogo, logoX, yPosition - logoHeight, logoWidth, logoHeight);
                yPosition -= logoHeight + 20;
            }

            String universityTitle = "Université Amar Telidji de Laghouat";
            String dynamicFilterInfo = getDynamicFilterInfo();

            contents.beginText();
            contents.setFont(PDType1Font.HELVETICA_BOLD, 16);
            float uniTitleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(universityTitle) / 1000 * 16;
            contents.newLineAtOffset((pageWidth - uniTitleWidth) / 2, yPosition);
            contents.showText(universityTitle);
            contents.endText();
            yPosition -= 20;

            contents.beginText();
            contents.setFont(PDType1Font.HELVETICA, 12);
            float filterInfoWidth = PDType1Font.HELVETICA.getStringWidth(dynamicFilterInfo) / 1000 * 12;
            contents.newLineAtOffset((pageWidth - filterInfoWidth) / 2, yPosition);
            contents.showText(dynamicFilterInfo);
            contents.endText();
            yPosition -= 30;

            int tableWidth = scheduleTable.getWidth();
            int tableHeight = scheduleTable.getHeight();
            int headerHeight = scheduleTable.getTableHeader().getHeight();
            int totalHeight = tableHeight + headerHeight;

            BufferedImage tableImage = new BufferedImage(tableWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D imageGraphics = tableImage.createGraphics();
            imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            imageGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            imageGraphics.setColor(ThemeConstants.CARD_WHITE);
            imageGraphics.fillRect(0, 0, tableWidth, totalHeight);
            scheduleTable.getTableHeader().paint(imageGraphics);
            imageGraphics.translate(0, headerHeight);
            scheduleTable.paint(imageGraphics);
            imageGraphics.dispose();

            PDImageXObject tablePdImage = PDImageXObject.createFromByteArray(document, toByteArray(tableImage), "png");

            float availableWidth = pageWidth - 2 * margin;
            float availableHeight = yPosition - margin;
            float scale = Math.min(availableWidth / tableWidth, availableHeight / totalHeight);
            float scaledWidth = tableWidth * scale;
            float scaledHeight = totalHeight * scale;

            float imageX = margin + (availableWidth - scaledWidth) / 2;
            float imageY = yPosition - scaledHeight;

            contents.drawImage(tablePdImage, imageX, imageY, scaledWidth, scaledHeight);

            contents.close();
            document.save(filePath);
            JOptionPane.showMessageDialog(this, ScheduleUiText.PDF_SUCCESS_MESSAGE, ScheduleUiText.PDF_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, ScheduleUiText.buildPdfErrorMessage(e.getMessage()), ScheduleUiText.PDF_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Error exporting schedule to PDF", e);
        }
    }

    private PDImageXObject loadImage(PDDocument document, String path) throws IOException {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            throw new IOException("Image not found: " + path);
        }
        BufferedImage bImage = ImageIO.read(imageUrl);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String formatName = "png";
        String fileName = imageUrl.getFile();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            formatName = fileName.substring(dotIndex + 1);
        }
        ImageIO.write(bImage, formatName, bos);
        return PDImageXObject.createFromByteArray(document, bos.toByteArray(), formatName);
    }

    private Image getPrintableLogo() {
        URL imageUrl = getClass().getResource("/icons/University_of_Laghouat_logo.png");
        if (imageUrl != null) {
            return new ImageIcon(imageUrl).getImage();
        }
        return null;
    }

    private String getDynamicFilterInfo() {
        String base = "Emploi du temps de l'enseignant : " + currentUser.getNom() + " " + currentUser.getPrenom();
        if (allDatesCheckBox.isSelected()) {
            return base;
        }
        return base + " - " + startDate;
    }

    private byte[] toByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
