package org.example;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xwpf.usermodel.*;
//import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.docx4j.wml.Fonts;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
//import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class auscrap {
    public static boolean arbeitsagenturSelected = true;
    public static boolean ausbildungSelected = true;
    public static boolean stepstoneSelected = true;
    public static boolean nrwSelected = true;
    public static boolean lehrstellenSelected = true;
    public static boolean azubiyoSelected = true;

    public static int f;

    public static boolean urlExists(Sheet sheet, String url) {
        int rows = sheet.getLastRowNum();
        for (int i = 1; i < rows; i++) { // Skip header row
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(4); // URL is in the 5th column (index 4)
                if (cell != null && cell.toString().trim().equalsIgnoreCase(url.trim())) {
                    System.out.println("OFFER ALREADY FOUND!");
                    return true;
                }
            }
        }
        return false;
    }

    public static synchronized void appendDataToExcel(String companyName, String hrName, String address, String email, String url, String date) {
            try (FileInputStream fis = new FileInputStream(EXCEL_FILE_PATH);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);


                if (!urlExists(sheet,url)){

// Check if the company name contains the HR name (case-insensitive)
            if (companyName != null && hrName != null && companyName.toLowerCase().equals(hrName.toLowerCase()) || companyName.toLowerCase().contains(hrName.toLowerCase()) ) {
                hrName = ""; // Set HR name to empty if it is contained within the company name
            }

            int rowNum = sheet.getLastRowNum() + 1;

            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(companyName);
            row.createCell(1).setCellValue(hrName);
            row.createCell(2).setCellValue(address);
            row.createCell(3).setCellValue(email);
            row.createCell(4).setCellValue(url);
            row.createCell(5).setCellValue(date);

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILE_PATH)) {
                workbook.write(fileOut);
                System.out.println("Appended data to Excel file: " + EXCEL_FILE_PATH);
            }

            }else System.err.println("URL already exist");
            } catch (IOException e) {
                System.err.println("Error appending data to Excel file: " + e.getMessage());
            }
    }


    public static String formatDate(LocalDate date) {
        // Format the date as DD-MM-YYYY
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }


    public static void createStyledHeaderRow(Workbook workbook, Sheet sheet) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Company", "HR", "Address", "Email", "URL", "Date"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static synchronized void appendVariableToNewSheet(String variableValue) {
        try (FileInputStream fis = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("Other Offers");
            if (sheet == null) {
                sheet = workbook.createSheet("Other Offers");
            }

            // Find the next empty row
            int rowNum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowNum);

            // Store the variable in the first column
            Cell cell = row.createCell(0);
            cell.setCellValue(variableValue);

            try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILE_PATH)) {
                workbook.write(fileOut);
                System.out.println("Added data to Processed Data sheet: " + variableValue);
            }

        } catch (IOException e) {
            System.err.println("Error writing to Excel file: " + e.getMessage());
        }
    }


    public static void initializeExcelFile() {
        File file = new File(EXCEL_FILE_PATH);
        if (file.exists()) {
            System.out.println("Excel file already exists: " + EXCEL_FILE_PATH);
            return; // File already exists, no need to initialize
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Job Data");
            createStyledHeaderRow(workbook, sheet);
            // Create second sheet for storing another variable



            try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILE_PATH)) {
                workbook.write(fileOut);
                System.out.println("Initialized Excel file with headers: " + EXCEL_FILE_PATH);
            }
        } catch (IOException e) {
            System.err.println("Error initializing Excel file: " + e.getMessage());
        }
    }

    public static String userN;

    public static LocalDate currentDate = LocalDate.now();
    public static String jobProfile;
    public static EdgeOptions options = new EdgeOptions();


    public static WebDriver driver ;
    // Create an Excel workbook and sheet
    public static Workbook workbook = new XSSFWorkbook();

    public static Sheet sheet = workbook.createSheet("Job Data");
    public static WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    public static int rowNum = 1;
    public static String templatePath;
    public static final String EXCEL_FILE_PATH = "Data.xlsx";


    public static final String KEY_FILE = ".license_key"; // Hidden file to store the key
    public static final String KEY_trial = ".t_key"; // Hidden file to store the key



    // First, create a method to load icons from URLs
    private static ImageIcon getResizedIconFromUrl(String url, int width, int height) {
        try {
            URL imageUrl = new URL(url);
            Image image = ImageIO.read(imageUrl).getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(image);
        } catch (Exception e) {
            System.err.println("Error loading icon from URL: " + url);
            return null;
        }
    }



    public static String[] showInputDialog() {
        final String[] results = new String[6]; // Increased size to include search type
        JDialog dialog = new JDialog((Frame) null, "Job Application Details", true);
        dialog.setSize(750, 600); // Adjusted height to accommodate the layout
        dialog.setLocationRelativeTo(null);

        dialog.setAlwaysOnTop(true);


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Enter Your Job Application Details");
        header.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Full Name
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(createLabeledField("Full Name:", nameField));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Job Profile
        JTextField jobProfileField = new JTextField();
        jobProfileField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(createLabeledField("Job Profile:", jobProfileField));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Search Type Radio Buttons
        JRadioButton todayOnlyRadio = new JRadioButton("Today Only");
        JRadioButton yesterdayOnlyRadio = new JRadioButton("Yesterday Only");
        JRadioButton globalSearchRadio = new JRadioButton("Global Search");
        todayOnlyRadio.setSelected(true); // Default selection

        ButtonGroup searchTypeGroup = new ButtonGroup();
        searchTypeGroup.add(todayOnlyRadio);
        searchTypeGroup.add(yesterdayOnlyRadio);
        searchTypeGroup.add(globalSearchRadio);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radioPanel.add(todayOnlyRadio);
        radioPanel.add(yesterdayOnlyRadio);
        radioPanel.add(globalSearchRadio);
        panel.add(createLeftAlignedLabeledField("Search Type:", radioPanel));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));



// Then modify your checkbox creation code:
        JPanel checkboxPanel = new JPanel(new GridLayout(0, 2));
        checkboxPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Job Websites Checkboxes
        JCheckBox arbeitsagenturCheckbox = new JCheckBox("Jobsuche ", true);
        JCheckBox AusbildungCheckbox = new JCheckBox("Ausbildung.de", true);
        JCheckBox stepstoneCheckbox = new JCheckBox("StepStone", true);
        JCheckBox nrwCheckbox = new JCheckBox("Ausbildung.nrw", true);
        JCheckBox LehrstCheckbox = new JCheckBox("Lehrstellen-radar", true);
        JCheckBox azubiyoCheckbox = new JCheckBox("AZUBIYO", true);



// Arbeitsagentur
        JPanel arbeitsagenturPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        arbeitsagenturPanel.add(new JLabel(getResizedIconFromUrl("https://upload.wikimedia.org/wikipedia/de/thumb/7/7c/Bundesagentur_f%C3%BCr_Arbeit_logo.svg/2048px-Bundesagentur_f%C3%BCr_Arbeit_logo.svg.png", 20, 20)));
        arbeitsagenturPanel.add(arbeitsagenturCheckbox);
        checkboxPanel.add(arbeitsagenturPanel);

// Ausbildung.de
        JPanel ausbildungPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        ausbildungPanel.add(new JLabel(getResizedIconFromUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSvGj0lJ_rMqZXszHYKh17He6gc-u9pH3hiLg&s", 20, 20)));
        ausbildungPanel.add(AusbildungCheckbox);
        checkboxPanel.add(ausbildungPanel);

// StepStone
        JPanel stepstonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        stepstonePanel.add(new JLabel(getResizedIconFromUrl("https://play-lh.googleusercontent.com/d-D62zPsG3lYYd2P1EBgCV4o2_PYkYrQHmQaHCELeXqjyL563HxJlKBGwtvWqNiwPeou", 20, 20)));
        stepstonePanel.add(stepstoneCheckbox);
        checkboxPanel.add(stepstonePanel);

// Ausbildung.nrw
        JPanel nrwPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        nrwPanel.add(new JLabel(getResizedIconFromUrl("https://suche.ausbildung.nrw/assets/platform_logo.png", 20, 20)));
        nrwPanel.add(nrwCheckbox);
        checkboxPanel.add(nrwPanel);

// AZUBIYO
        JPanel azubiyoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        azubiyoPanel.add(new JLabel(getResizedIconFromUrl("https://yt3.googleusercontent.com/5MgDA3gl_Stym4Bz-PJuiLGIA0aZtC5ATUVs5uS-yTq1jhYziesRaS-a0ihxjL6gAIVLRwfB=s900-c-k-c0x00ffffff-no-rj", 20, 20)));
        azubiyoPanel.add(azubiyoCheckbox);
        checkboxPanel.add(azubiyoPanel);

// Lehrstellen-radar
        JPanel lehrstPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        lehrstPanel.add(new JLabel(getResizedIconFromUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSaSYXtuLgsObooebb_sHyRyAtqFnvqnO5e1g&s", 20, 20)));
        lehrstPanel.add(LehrstCheckbox);
        checkboxPanel.add(lehrstPanel);

        panel.add(createLeftAlignedLabeledField("Select Job Websites:", checkboxPanel));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Search Type Radio Buttons
        JRadioButton Anschreiben  = new JRadioButton("Anschreiben Only");
        JRadioButton Bewerbungsunterlagen = new JRadioButton("Bewerbungsunterlagen");
        Anschreiben.setSelected(true); // Default selection

        ButtonGroup docTypeGroup = new ButtonGroup();
        docTypeGroup.add(Anschreiben);
        docTypeGroup.add(Bewerbungsunterlagen);

        JPanel docPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        docPanel.add(Anschreiben);
        docPanel.add(Bewerbungsunterlagen);
        panel.add(createLeftAlignedLabeledField("Document Type:", docPanel));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Select Document Template
        JTextField documentPathField = new JTextField();
        JButton browseButton = new JButton("Browse");
        documentPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.add(createLabeledField("Select Anschreiben / Bewerbungsunterlagen Document Template:", documentPathField, browseButton));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));






        // Buttons
        JButton submitButton = createButton("Submit", new Color(34, 193, 34), Color.BLACK);
        JButton cancelButton = createButton("Cancel", new Color(234, 67, 53), Color.BLACK);
        JButton clearButton = createButton("Clear", new Color(255, 204, 0), Color.BLACK);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(clearButton);
        panel.add(buttonPanel);

        // Event Listeners
        browseButton.addActionListener(e -> browseFile(documentPathField, dialog));

        submitButton.addActionListener(e -> handleSubmit(nameField, jobProfileField, documentPathField, todayOnlyRadio, yesterdayOnlyRadio, Anschreiben, results, dialog, new JCheckBox[]{arbeitsagenturCheckbox, AusbildungCheckbox,
                stepstoneCheckbox, nrwCheckbox,
                LehrstCheckbox, azubiyoCheckbox}));
        cancelButton.addActionListener(e -> dialog.dispose());
        clearButton.addActionListener(e -> {
            nameField.setText("");
            jobProfileField.setText("");
            documentPathField.setText("");
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
        return results[0] != null ? results : null;
    }

    // Add this new method
    private static JPanel createLeftAlignedLabeledField(String labelText, JPanel componentPanel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        componentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(componentPanel);
        return panel;
    }


    private static JPanel createLabeledField(String labelText, JTextField textField) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(textField);
        return panel;

    }

    private static JPanel createLabeledField(String labelText, JTextField textField, JButton button) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        textField.setPreferredSize(new Dimension(200, 30));
        fieldPanel.add(textField);
        fieldPanel.add(button);
        panel.add(fieldPanel);
        return panel;

    }

    private static JPanel createLabeledField(String labelText, JPanel componentPanel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(componentPanel);
        return panel;
    }

    private static JButton createButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        button.setPreferredSize(new Dimension(100, 40));
        return button;
    }

    private static void browseFile(JTextField documentPathField, Component parent) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Word Documents (*.docx)", "docx"));
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!validateDocument(selectedFile.getAbsolutePath())) {
                JOptionPane.showMessageDialog(parent, "Invalid document! Required placeholders: {{COMPANY}}, {{HR}}, {{ADDRESS}}, {{DATE}}", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            documentPathField.setText(selectedFile.getAbsolutePath());
        }
    }



    private static void handleSubmit(JTextField nameField, JTextField jobProfileField, JTextField documentPathField , JRadioButton todayOnlyRadio, JRadioButton yesterdayOnlyRadio,JRadioButton lettreRadio, String[] results, JDialog dialog, JCheckBox[] websiteCheckboxes) {
        String name = nameField.getText().trim();
        String jobProfile = jobProfileField.getText().trim();
        String documentpath = documentPathField.getText().trim();


        String searchType;

        if (todayOnlyRadio.isSelected()) {
            searchType = "Today Only";
        } else if (yesterdayOnlyRadio.isSelected()) {
            searchType = "Yesterday Only";
        } else {
            searchType = "Global Search";
        }

        // Store checkbox states
        arbeitsagenturSelected = websiteCheckboxes[0].isSelected();
        ausbildungSelected = websiteCheckboxes[1].isSelected();
        stepstoneSelected = websiteCheckboxes[2].isSelected();
        nrwSelected = websiteCheckboxes[3].isSelected();
        lehrstellenSelected = websiteCheckboxes[4].isSelected();
        azubiyoSelected = websiteCheckboxes[5].isSelected();


        // Get document type
        String docType = lettreRadio.isSelected() ? "Anschreiben only" : "Bewerbungsunterlagen";

        if (name.isEmpty() || jobProfile.isEmpty() || documentpath.isEmpty() ) {
            JOptionPane.showMessageDialog(dialog, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!validateDocument(documentpath)) {
            JOptionPane.showMessageDialog(dialog, "Invalid document! Required placeholders: {{COMPANY}}, {{HR}}, {{ADDRESS}}, {{DATE}}", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if at least one checkbox is selected
        boolean atLeastOneSelected = false;
        for (JCheckBox checkbox : websiteCheckboxes) {
            if (checkbox.isSelected()) {
                atLeastOneSelected = true;
                break;
            }
        }

        if (!atLeastOneSelected) {
            JOptionPane.showMessageDialog(dialog, "Please select at least one job website!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        results[0] = name;
        results[1] = jobProfile;
        results[2] = documentpath;
        results[3] = searchType; // Store the search type in the results array
        results[4] = String.format("%s,%s,%s,%s,%s,%s",
                arbeitsagenturSelected, ausbildungSelected,
                stepstoneSelected, nrwSelected,
                lehrstellenSelected, azubiyoSelected);
        results[5] = docType; // New field for document type



        dialog.dispose();
    }

    public static boolean validateDocument(String filePath) {
        Set<String> requiredPlaceholders = new HashSet<>(List.of("{{COMPANY}}", "{{HR}}", "{{ADDRESS}}", "{{DATE}}"));
        try (FileInputStream fis = new FileInputStream(filePath); XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder documentText = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                documentText.append(paragraph.getText()).append(" ");
            }
            return requiredPlaceholders.stream().allMatch(documentText.toString()::contains);
        } catch (IOException e) {
            System.err.println("Error reading document: " + e.getMessage());
            return false;
        }
    }
public static String docu;

    public static void main() {
        String[] results = showInputDialog();

        // Initialize WebDriverManager and set up Edge driver options
        WebDriverManager.edgedriver().setup();

        // Set EdgeOptions to open in private mode
        options.addArguments("disable-cache");
        options.addArguments("disable-application-cache");
        options.addArguments("--disable-infobars");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-webrtc");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-3d-apis");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--inprivate");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Initialize the static WebDriver field
        driver = new EdgeDriver(options);

        // Initialize WebDriverWait after driver
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        // Remove selenium detection and navigator.webdriver properties
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        Map<String, Object> params = new HashMap<>();
        params.put("source", "delete Object.getPrototypeOf(navigator).webdriver;");
        ((EdgeDriver) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);


        Set<String> uniqueEmails = new HashSet<>();

        if (results == null || results.length != 6) {
            System.out.println("User canceled the input or invalid data entered. Exiting program...");
            driver.close();
        }

        // Extract the inputs
        userN = results[0].trim(); // User's full name
        jobProfile = results[1].trim(); // Job profile
        String day = results[3].trim();
        templatePath = results[2].trim();
        // Extract checkbox states from results[4]
        String[] websiteSelections = results[4].split(",");
        arbeitsagenturSelected = Boolean.parseBoolean(websiteSelections[0]);
        ausbildungSelected = Boolean.parseBoolean(websiteSelections[1]);
        stepstoneSelected = Boolean.parseBoolean(websiteSelections[2]);
        nrwSelected = Boolean.parseBoolean(websiteSelections[3]);
        lehrstellenSelected = Boolean.parseBoolean(websiteSelections[4]);
        azubiyoSelected = Boolean.parseBoolean(websiteSelections[5]);

        String docType = results[5]; // "Anschreiben" or "Bewerbungsunterlagen"
        docu = "Bewerbungsunterlagen_"+userN;

        if (docType.equals("Anschreiben only")) {
            docu="Anschreiben";

            System.out.println("Generating Anschreiben only");}

        if (userN.isEmpty() || jobProfile.isEmpty() || templatePath.isEmpty()) {
            JOptionPane.showMessageDialog(null, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        String baseUrl = "https://www.arbeitsagentur.de/jobsuche/suche?angebotsart=4&was=";
        String searchUrl;
        if (day.equals("Today Only")) {
            searchUrl = baseUrl + jobProfile + "&veroeffentlichtseit=0";
            System.out.println("Searching Today Jobs");
        } else if (day.equals("Yesterday Only")) {
            searchUrl = baseUrl + jobProfile + "&veroeffentlichtseit=1";
            System.out.println("Searching Yesterday Jobs");
        } else {
            // Construct the dynamic URL
            searchUrl = baseUrl + jobProfile;
            System.out.println("Searching Global Jobs");

        }
        initializeExcelFile();

        try {
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(412, 915)); // Set to Pixel 7 screen size

            if(arbeitsagenturSelected){

            System.out.println("Searching Jobs for: " + jobProfile);
            driver.get(searchUrl);
            System.out.println("Driver  - Page title: " + driver.getTitle());

            // Wait for the page to load (you can use WebDriverWait for better handling)
            Thread.sleep(8000);
            // Wait for the modal to be visible

            try {
                WebElement shadowHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("bahf-cookie-disclaimer-dpl3")
                ));

                // Use JavaScript to access the Shadow DOM
                js = (JavascriptExecutor) driver;

                // Locate the modal inside the Shadow DOM
                WebElement modal = (WebElement) js.executeScript(
                        "return arguments[0].shadowRoot.querySelector('bahf-cd-modal')",
                        shadowHost
                );

                // Locate the button inside the modal
                WebElement acceptAllCookiesButton = (WebElement) js.executeScript(
                        "return arguments[0].querySelector(\"button[data-testid='bahf-cookie-disclaimer-btn-alle']\")",
                        modal
                );

                // Click the button
                acceptAllCookiesButton.click();

                System.out.println("Clicked the 'Alle Cookies akzeptieren' button successfully.");
            } catch (Exception e) {
                System.err.println("Failed to interact with the 'Alle Cookies akzeptieren' button: " + e.getMessage());
            }

            driver.manage().window().minimize();
            Thread.sleep(5000);
            System.out.println("Clicked 'Weitere Ergebnisse' button. Loading more results...");
            while (true) {

                try {

                    WebElement loadMoreButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(text(), 'Weitere Ergebnisse')]")
                    ));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", loadMoreButton);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loadMoreButton);
                    Thread.sleep(3000); // Wait for new results to load
                } catch (NoSuchElementException | TimeoutException e) {
                    System.out.println("No more results to load. 'Weitere Ergebnisse' button not found.");
                    break;
                }
            }


            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            System.out.println("Scrolled to the top of the page.");
            // Step 5: Get the page source (HTML content) after JavaScript rendering
            String pageSource = driver.getPageSource();

            // Step 6: Parse the HTML content using Jsoup
            Document document = Jsoup.parse(pageSource);


            Elements ulElement = document.getElementsByClass("container-fluid");
            if (ulElement.isEmpty()) {
                System.out.println("The <ul> element with class 'listenansicht-joblisten-container' was not found.");
                return;
            }

            // Step 8: Extract all <a> tags inside the <ul> element that match the pattern
            Elements links = ulElement.select("a[href^='https://www.arbeitsagentur.de/jobsuche/jobdetail/']");
// Method 1: Click using CSS Selector
            System.out.println("Found " + links.size() + " links.");
            // Step 9: Print the extracted links
            for (Element link : links) {
                String href = link.attr("href");
                System.out.println(href);
                // Navigate to the job detail page
                driver.get(href);

                // Wait for the page to load
                Thread.sleep(8000);


                // Parse the job detail page using Jsoup

                // Check for CAPTCHA
                if (isCaptchaPresent(driver)) {
                    handleCaptcha(driver);
                }

                String jobPageSource = driver.getPageSource();

                // Parse the job detail page using Jsoup
                Document jobDocument = Jsoup.parse(jobPageSource);
                // Get the page source for the job detail page
                // Locate the <p> element with the ID "detail-bewerbung-adresse"
                Element addressElement = jobDocument.getElementById("detail-bewerbung-adresse");

                // Locate the <p> element with the ID "detail-bewerbung-mail"
                Element emailElement = jobDocument.getElementById("detail-bewerbung-mail");

                if (addressElement != null && emailElement != null) {
                    // Extract the text content and split it by <br> tags
                    String[] lines = addressElement.html().split("<br>");

                    // Extract company name, HR name, and address
                    String companyName = lines.length > 0 ? lines[0].trim() : "N/A";
                    String hrName = lines.length > 1 ? lines[1].trim() : "N/A";

                    if (companyName == hrName) {
                        hrName = "";
                    }

                    // Replace "&amp;" with a space in the company name
                    companyName = companyName.replace("&amp; ", " ");

                    // Extract address (everything after the second <br> tag)
                    StringBuilder addressBuilder = new StringBuilder();
                    for (int i = 2; i < lines.length; i++) {
                        if (!lines[i].trim().isEmpty()) {
                            addressBuilder.append(lines[i].trim());
                            if (i < lines.length - 1) {
                                addressBuilder.append("\n"); // Add a newline between address lines
                            }
                        }
                    }
                    String address = addressBuilder.toString().isEmpty() ? "N/A" : addressBuilder.toString();

                    // Replace commas with newlines in the address
                    address = address.replace(",", "\n");
                    // Extract email address
                    String email = extractEmail(emailElement.text());

                    if (email.isEmpty() || email == "null" || email == "N/A" && uniqueEmails.add(email)) {
                        System.out.println("No email address found.");
                    } else {

                        // Create a folder for the company
                        String folderName = "JobDocuments/" + LocalDate.now() + "/" + email;
                        File folder = new File(folderName);
                        if (!folder.exists()) {
                            folder.mkdirs(); // Create the folder if it doesn't exist
                        }



                        // Generate a new Word document for this job
                        String docxPath = folderName + "/"+docu+".docx";
                        createWordDocument(templatePath, docxPath, companyName, hrName, address, formatDate(currentDate));

                        System.out.println("Created Word document: " + docxPath);

                        convertDocxToPdf(folderName, folderName);

                        appendDataToExcel(companyName, hrName, address, email, href, formatDate(currentDate));


                        System.out.println("Company: " + companyName);
                        System.out.println("HR: " + hrName);
                        System.out.println("Address: " + address);
                        System.out.println("Email: " + email);
                        System.out.println("----------------------------------------");
                        f++;
                        ShortcutCreator.main();

                    }
                } else {
                    appendVariableToNewSheet(href);
                    System.out.println("Address element or email not found on page: " + href);
                }
            }
        }

            if (day.equals("Global Search")) {
//                NWR.main();
                if(ausbildungSelected){
                aus_de.main();}
                if(lehrstellenSelected){
                driver.get("https://www.lehrstellen-radar.de/");
                RadarStl.main();}

                Thread.sleep(3000);
                if(stepstoneSelected){
                driver.get("https://www.stepstone.de/work/"+jobProfile+"?searchOrigin=Homepage_top-search");
                Thread.sleep(20000);}
                if(nrwSelected){
                NWR.main();}
                Thread.sleep(5000);
                if(azubiyoSelected) {
                    driver.get("https://www.azubiyo.de/stellenmarkt/?subject=" + jobProfile + "&radius=1000");
                    Thread.sleep(50000);
                }

                driver.quit();

            }
            Thread.sleep(3000);


        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        } finally {
            // Close the browser and workbook
            driver.quit();
            try {
                workbook.close();
            } catch (IOException e) {
                System.out.println("Failed to close workbook: " + e.getMessage());
            }
        }
        if (f == 0) {
            emailer.showNotification(f + " Offers Has been Found \n ", "Finished Process");
        } else {
         ShortcutCreator.main();
            emailer.showNotification(f + " Offers Has been Found \n Please Check The Created Documents Before Sending Emails!", "Finished Process");
        }
    }




    // Method to check if CAPTCHA is present
    private static boolean isCaptchaPresent(WebDriver driver) {
        try {


            // Look for CAPTCHA elements (adjust the selector as needed)
            driver.findElement(By.xpath("//*[contains(text(), 'Sicherheitsabfrage')]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    // Method to extract email address from text
    public static String extractEmail(String text) {
        // Regular expression to match email addresses
        String emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(); // Return the first email found
        } else {
            return "N/A"; // Return "N/A" if no email is found
        }

    }


    public static void handleCaptcha(WebDriver driver) throws InterruptedException {
        emailer.showNotification(" Please Solve Captcha !! \n ","Captcha");


        ((JavascriptExecutor) driver).executeScript("window.focus();");
        ((JavascriptExecutor) driver).executeScript("alert('CAPTCHA detected! Please solve it manually and click OK to continue.');");
        driver.switchTo().alert().accept();
        driver.switchTo().window(driver.getWindowHandle());
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(5)); // Adjust timeout if needed

        try {
            // Wait for CAPTCHA form to appear
            WebElement captchaForm = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("captchaForm")));

            // Scroll to the CAPTCHA form
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", captchaForm);
            System.out.println("CAPTCHA detected! Please solve it manually.");

            // Wait until the CAPTCHA form disappears
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("captchaForm")));

            System.out.println("CAPTCHA solved, refreshing the page...");
        } catch (Exception e) {
            System.out.println("CAPTCHA form not found or already disappeared.");
        }

        driver.navigate().refresh();
        Thread.sleep(3000);
        driver.manage().window().minimize();

    }




    public static void createWordDocument(String templatePath, String outputPath, String company, String hr, String address, String currendate) {
        try (FileInputStream fis = new FileInputStream(templatePath);
             FileOutputStream fos = new FileOutputStream(outputPath)) {
            // Open the Word template
            XWPFDocument document = new XWPFDocument(fis);

            if (company != null && hr != null && company.toLowerCase().equals(hr.toLowerCase()) || company.toLowerCase().contains(hr.toLowerCase()) ) {
                hr = ""; // Set HR name to empty if it is contained within the company name
            }

            // Replace placeholders with scraped data
            replacePlaceholder(document, "{{COMPANY}}", company);
            replacePlaceholder(document, "{{HR}}", hr);
            replacePlaceholder(document, "{{ADDRESS}}", address);
            replacePlaceholder(document, "{{DATE}}", currendate);

            // Save the modified document
            document.write(fos);
        } catch (IOException e) {
            System.out.println("An error occurred while creating the Word document: " + e.getMessage());
        }
    }

    public static void replacePlaceholder(XWPFDocument document, String placeholder, String value) {
        // Iterate through all paragraphs in the document
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null && text.contains(placeholder)) {
                    // Replace the placeholder with the value
                    text = text.replace(placeholder, value);
                    run.setText(text, 0);
                }
            }
        }

        // Iterate through all tables in the document
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        for (XWPFRun run : paragraph.getRuns()) {
                            String text = run.getText(0);
                            if (text != null && text.contains(placeholder)) {
                                // Replace the placeholder with the value
                                text = text.replace(placeholder, value);
                                run.setText(text, 0);
                            }
                        }
                    }
                }
            }
        }
    }
    public static void convertDocxToPdf(String folderName, String PDFout) {
        // Initialize Microsoft Word
        ActiveXComponent word = new ActiveXComponent("Word.Application");
        word.setProperty("Visible", new Variant(false)); // Run Word in the background

        try {
            // Get all .docx files in the input folder
            File folder = new File(folderName);
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".docx"));

            if (files != null && files.length > 0) {
                System.out.println("Found " + files.length + " .docx files in the input folder.");

                for (File file : files) {
                    String docxPath = file.getAbsolutePath();
                    String pdfPath = new File(PDFout, file.getName().replace(".docx", ".pdf")).getAbsolutePath();

                    System.out.println("Processing: " + docxPath);

                    // Open the .docx file
                    Dispatch documents = word.getProperty("Documents").toDispatch();
                    Dispatch document = Dispatch.call(documents, "Open", docxPath).toDispatch();
                    System.out.println("Opened document: " + docxPath);

                    try {
                        // Export the document as PDF
                        System.out.println("Exporting to PDF: " + pdfPath);
                        Dispatch.call(document, "ExportAsFixedFormat",
                                new Variant(pdfPath), // Output file path
                                new Variant(17) // 17 is the PDF format code
                        );
                        System.out.println("Exported PDF: " + pdfPath);
                    } catch (Exception e) {
                        System.out.println("Failed to export PDF: " + pdfPath);
                        System.out.println("Error: " + e.getMessage());
                        e.printStackTrace(); // Print the full stack trace for debugging
                    }

                    // Close the document
                    Dispatch.call(document, "Close", false);
                    System.out.println("Closed document: " + docxPath);
                }
            } else {
                System.out.println("No .docx files found in the input folder: " + folderName);
            }
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
            e.printStackTrace(); // Print the full stack trace for debugging
        } finally {
            // Quit Microsoft Word
            word.invoke("Quit", 0);
            System.out.println("Microsoft Word closed.");
        }
    }

    public static File getLicenseFile(String fileName) {
        // Determine the OS
        String os = System.getProperty("os.name").toLowerCase();
        File licenseFile;

        if (os.contains("win")) {
            // Windows: Use AppData folder
            String appData = System.getenv("APPDATA");
            File appDataDir = new File(appData, "Auto_bewerber"); // Replace "MyApp" with your app's name
            if (!appDataDir.exists()) {
                appDataDir.mkdirs();
            }
            licenseFile = new File(appDataDir, fileName);
        } else {
            // Unix-based systems: Use a hidden folder in the user's home directory
            String homeDir = System.getProperty("user.home");
            File hiddenDir = new File(homeDir, ".Auto_bewerber"); // Replace ".myapp" with your app's name
            if (!hiddenDir.exists()) {
                hiddenDir.mkdirs();
            }
            licenseFile = new File(hiddenDir, fileName);
        }

        return licenseFile;
    }

    // Method to check if the license file exists
    public static boolean isLicenseFilePresent(String fileName) {
        File licenseFile = getLicenseFile(fileName);
        return licenseFile.exists();
    }

    public static void makeFileHidden(String fileName, String fileContent) {
        try {
            // Determine the OS
            String os = System.getProperty("os.name").toLowerCase();
            File targetFile;

            if (os.contains("win")) {
                // Windows: Use AppData folder
                String appData = System.getenv("APPDATA");
                File appDataDir = new File(appData, "Auto_bewerber"); // Replace "MyApp" with your app's name
                if (!appDataDir.exists()) {
                    appDataDir.mkdirs();
                }
                targetFile = new File(appDataDir, fileName);

                // Write content to the file
                Files.writeString(targetFile.toPath(), fileContent);

                // Set the hidden attribute
                Files.setAttribute(targetFile.toPath(), "dos:hidden", true);
            } else {
                // Unix-based systems: Use a hidden folder in the user's home directory
                String homeDir = System.getProperty("user.home");
                File hiddenDir = new File(homeDir, ".Auto_bewerber"); // Replace ".myapp" with your app's name
                if (!hiddenDir.exists()) {
                    hiddenDir.mkdirs();
                }
                targetFile = new File(hiddenDir, fileName);

                // Write content to the file
                Files.writeString(targetFile.toPath(), fileContent);

                // Set restrictive permissions (hidden behavior is implicit with folder name starting with ".")
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(targetFile.toPath(), permissions);
            }

            System.out.println("File created and hidden successfully at: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error creating and hiding the file: " + e.getMessage());
        }
    }

    public static void validateLicenseKey(String licenseKey) {
        // Neon.tech PostgreSQL connection details
        String url = "jdbc:postgresql://ep-frosty-forest-a2cnn5a1-pooler.eu-central-1.aws.neon.tech/neondb?user=neondb_owner&password=npg_7qwDYyAS5PRO&sslmode=require";

        // SQL query to check if the key exists and is active
        String selectQuery = "SELECT active FROM license_keys WHERE key = ?";
        String deleteQuery = "DELETE FROM license_keys WHERE key = ?";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {

            // Set the license key as a parameter for the SELECT query
            selectStatement.setString(1, licenseKey);

            // Execute the SELECT query
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                boolean active = resultSet.getBoolean("active");

                if (active) {
                    JOptionPane.showConfirmDialog(null, "Key is Confirmed", "Activation", JOptionPane.DEFAULT_OPTION);

                    // Save the key to a hidden file
                    makeFileHidden(KEY_FILE, licenseKey);

                    // Delete the license key from the database
                    deleteStatement.setString(1, licenseKey);
                    int rowsDeleted = deleteStatement.executeUpdate();

                    if (rowsDeleted > 0) {
                        System.out.println("License key successfully deleted from the database.");
                    } else {
                        System.out.println("Failed to delete the license key from the database.");
                    }
                } else {
                    JOptionPane.showConfirmDialog(null, "Key is invalid", "Activation", JOptionPane.CLOSED_OPTION);
                    System.out.println("Key is inactive, closing program.");
                    driver.close();
                    System.exit(0);
                }
            } else {
                JOptionPane.showConfirmDialog(null, "Key is invalid", "Activation", JOptionPane.CLOSED_OPTION);
                System.out.println("Key not found, closing program.");
                driver.close();
                System.exit(0);
            }

        } catch (SQLException e) {
            System.out.println("Error validating license key: " + e.getMessage());
            driver.close();
            System.exit(0);
        }
    }



}


