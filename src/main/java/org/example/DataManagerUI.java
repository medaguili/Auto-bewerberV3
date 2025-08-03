package org.example;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class DataManagerUI {
    public static void main() {
        SwingUtilities.invokeLater(DataManagerUI::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Data Manager");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(45, 45, 45));
        mainPanel.setLayout(new GridLayout(3, 1, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // Buttons
        JButton deleteTodayButton = createStyledButton("Delete Today's DATA", new Color(15, 80, 141));
        JButton deleteYesterdayButton = createStyledButton("Delete Yesterday's DATA", new Color(255, 186, 8));
        JButton deleteAllButton = createStyledButton("Delete ALL DATA", new Color(164, 0, 0));

        // Action Listeners with Confirmation Dialogs
        deleteTodayButton.addActionListener((ActionEvent e) -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to delete today's data?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    deleteTodaysFolder();
                    JOptionPane.showMessageDialog(frame,
                            "Today's data has been deleted.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Error deleting today's data: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        mainPanel.add(deleteTodayButton);
        frame.add(mainPanel);
        frame.setVisible(true);

        deleteYesterdayButton.addActionListener((ActionEvent e) -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to delete yesterday's data?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    deleteYesterdayFolder();
                    JOptionPane.showMessageDialog(frame,
                            "Yesterday's data has been deleted.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Error deleting today's data: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);}


            }
        });

        deleteAllButton.addActionListener((ActionEvent e) -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to delete ALL data?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try{
                    deleteAllFolders();

                JOptionPane.showMessageDialog(frame,
                        "All data has been deleted.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);}
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Error deleting today's data: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);}

            }
        });

        // Add Buttons to Panel
        mainPanel.add(deleteTodayButton);
        mainPanel.add(deleteYesterdayButton);
        mainPanel.add(deleteAllButton);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    // Method to Delete Today's Folder in JobDocuments
    private static void deleteTodaysFolder() throws IOException {
         String folderPath = "JobDocuments/" + java.time.LocalDate.now();
        Path path = Paths.get(folderPath);

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder()) // Ensures deletion starts from files, not the parent directory
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.deleteIfExists(path); // Delete the main folder
            System.out.println("Deleted folder: " + folderPath);
        } else {
            System.out.println("No folder found for today: " + folderPath);
        }
    }

    // Method to Delete Today's Data from Excel
// Method to Delete Today's Data from Excel

    public static Date yest = new Date();
    private static String yesterday() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
         yest = cal.getTime();
        return dateFormat.format(yest).toString();
    }

    // Method to Delete Today's Folder in JobDocuments
    private static void deleteYesterdayFolder() throws IOException {

        String folderPath = "JobDocuments/" + yesterday();
        Path path = Paths.get(folderPath);

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder()) // Ensures deletion starts from files, not the parent directory
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.deleteIfExists(path); // Delete the main folder
            System.out.println("Deleted folder: " + folderPath);
        } else {
            System.out.println("No folder found for today: " + folderPath);
        }
    }
    private static void deleteYesterdaysDataFromExcel() throws IOException {
        // Get yesterday's date in the correct format
        LocalDate y = yest.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        String yesterday = y.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        String excelFilePath = "DATA.xlsx";

        try (FileInputStream fis = new FileInputStream(excelFilePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0); // Assumes data is in the first sheet
            List<Integer> rowsToDelete = new ArrayList<>();

            // Collect Rows to Delete
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell dateCell = row.getCell(5); // Assuming Date is in the 6th column (index 5)

                    if (dateCell != null && dateCell.getCellType() == CellType.STRING) {
                        String dateValue = dateCell.getStringCellValue().trim();

                        // Direct String comparison
                        if (dateValue.equals(yesterday)) {
                            rowsToDelete.add(i);
                        }
                    }
                }
            }

            // Delete Rows from Bottom to Top
            for (int i = rowsToDelete.size() - 1; i >= 0; i--) {
                int rowIndex = rowsToDelete.get(i);
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    sheet.removeRow(row);
                    if (rowIndex < sheet.getLastRowNum()) {
                        sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
                    }
                    System.out.println("Deleted row: " + (rowIndex + 1));
                }
            }

            // Save Changes
            try (FileOutputStream fos = new FileOutputStream(excelFilePath)) {
                workbook.write(fos);
            }
        }
    }

    private static void deleteAllFolders() {
        String folderPath = "JobDocuments";
        Path path = Paths.get(folderPath);

        if (Files.exists(path) && Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()) // Reverse order to delete files first
                        .map(Path::toFile)
                        .forEach(file -> {
                            if (!file.delete()) {
                                System.err.println("Failed to delete: " + file.getAbsolutePath());
                            }
                        });

                deleteExcelFileAndShortcut(); // Delete Excel and shortcut if needed
                deleteDesktopShortcuts(); // Delete the folder

                System.out.println("All folders and files deleted in: " + folderPath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Error deleting all data: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("No JobDocuments directory found.");
        }
    }


//    // Method to Clear All Data in Excel
//    private static void clearExcelData() {
//        String excelFilePath = "DATA.xlsx";
//        Path path = Paths.get(excelFilePath);
//
//        try {
//            if (Files.exists(path)) {
//                Files.delete(path);
//                System.out.println("Deleted Excel file: " + excelFilePath);
//                // Delete the Excel shortcut on the desktop
//                String desktopPath = System.getProperty("user.home") + "\\Desktop";
//                File excelShortcut = new File(desktopPath + "\\DATA.xlsx.lnk");
//                if (excelShortcut.exists()) {
//                    if (excelShortcut.delete()) {
//                        System.out.println("Deleted Excel shortcut: " + excelShortcut.getAbsolutePath());
//                    } else {
//                        System.err.println("Failed to delete Excel shortcut: " + excelShortcut.getAbsolutePath());
//                    }
//                } else {
//                    System.out.println("Excel shortcut not found at: " + excelShortcut.getAbsolutePath());
//                }
//
//
//        } else{
//            System.out.println("No Excel file found at: " + excelFilePath);
//        }
//
//        } catch (IOException e) {
//            JOptionPane.showMessageDialog(null,
//                    "Error deleting Excel file: " + e.getMessage(),
//                    "Error",
//                    JOptionPane.ERROR_MESSAGE);
//        }





    // Method to delete the desktop shortcuts
    private static void deleteDesktopShortcuts() {
        String desktopPath = System.getProperty("user.home") + "\\Desktop";

        File folderShortcut = new File(desktopPath + "\\Ausbildungsplatzangebote.lnk");
        if (folderShortcut.exists()) {
            if (folderShortcut.delete()) {
                System.out.println("Deleted folder shortcut: " + folderShortcut.getAbsolutePath());
            } else {
                System.err.println("Failed to delete folder shortcut: " + folderShortcut.getAbsolutePath());
            }
        } else {
            System.out.println("Folder shortcut not found at: " + folderShortcut.getAbsolutePath());
        }

    }


    private static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(200, 50));

        // Hover Effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private static void deleteExcelFileAndShortcut() {
        String excelFilePath = "DATA.xlsx";
        String desktopShortcutPath = System.getProperty("user.home") + "\\Desktop\\DATA.xlsx .lnk";

        File excelFile = new File(excelFilePath);
        File shortcutFile = new File(desktopShortcutPath);

        if (excelFile.exists()) {
            if (excelFile.delete()) {
                System.out.println("Excel file deleted: " + excelFilePath);
            } else {
                System.out.println("Failed to delete Excel file.");
            }
        } else {
            System.out.println("Excel file does not exist.");
        }

        if (shortcutFile.exists()) {
            if (shortcutFile.delete()) {
                System.out.println("Shortcut deleted: " + desktopShortcutPath);
            } else {
                System.out.println("Failed to delete shortcut.");
            }
        } else {
            System.out.println("Shortcut does not exist.");
        }
    }

}

