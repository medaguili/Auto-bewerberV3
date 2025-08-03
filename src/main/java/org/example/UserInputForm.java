package org.example;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;


public class UserInputForm {


    // Add these new fields
    private static JFrame formFrame;
    private static Runnable onCloseCallback;
    public static String selectedOption;
    public static boolean S=false;
    public static List<String> filePaths = new ArrayList<>();
    public static void setOnCloseCallback(Runnable callback) {
        onCloseCallback = callback;
    }
    // New method for folder browsing
    private static void browseFolder(JTextField textField, Component parent) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setDialogTitle("Select Folder Containing Other Documents");

        if (folderChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            textField.setText(folderChooser.getSelectedFile().getAbsolutePath());
        }
    }
    public static void main()throws InterruptedException {
        // Create a JFrame for better control over the layout
        formFrame = new JFrame("Email Authentication Form");
        formFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        formFrame.setSize(440, 720); // Set the size of the frame
        formFrame.setLocationRelativeTo(null); // Center the window
        formFrame.setAlwaysOnTop(true);


        // Create the JPanel for the form
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Stack components vertically
        panel.setBorder(new EmptyBorder(20, 20, 20, 20)); // Padding around panel

        // Create and add the Gmail logo at the top
        JLabel logoLabel = new JLabel(new ImageIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/7/7e/Gmail_icon_%282020%29.svg/120px-Gmail_icon_%282020%29.svg.png"));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the logo
        panel.add(logoLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add space below the logo

        // Add "Enter Your Email Authentication Information" header
        JLabel header = new JLabel("Enter Your Email Authentication Information");
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the header
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Add space below the header

        // Create text fields for input
        JTextField emailField = new JTextField(25);
        JPasswordField passwordField = new JPasswordField(25);
        JTextField subjectField = new JTextField(25);
        JTextArea messageArea = new JTextArea(10, 25); // Larger message box (8 rows)
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));

        // Set input field font size and styling
        Font inputFont = new Font("Arial", Font.PLAIN, 14);
        emailField.setFont(inputFont);
        passwordField.setFont(inputFont);
        subjectField.setFont(inputFont);
        messageArea.setFont(inputFont);

        // Create labels for each input
        JLabel emailLabel = new JLabel("Email:");
        JLabel passwordLabel = new JLabel("Password:");
        JLabel subjectLabel = new JLabel("Subject:");
        JLabel messageLabel = new JLabel("Message:");

        // Align labels to the left
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subjectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Use a JPanel for the input fields and labels to align them side by side
        JPanel emailPanel = new JPanel(new BorderLayout());
        emailPanel.add(emailLabel, BorderLayout.NORTH);
        emailPanel.add(emailField, BorderLayout.CENTER);

        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.add(passwordLabel, BorderLayout.NORTH);
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        JPanel subjectPanel = new JPanel(new BorderLayout());
        subjectPanel.add(subjectLabel, BorderLayout.NORTH);
        subjectPanel.add(subjectField, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageLabel, BorderLayout.NORTH);
        messagePanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        // Add panels with input fields to the main panel
        panel.add(emailPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Space between fields

        panel.add(passwordPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(subjectPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(messagePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 30))); // Space before the buttons



        JPanel attachTogglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel attachLabel = new JLabel("Attachments:");
        JRadioButton enableAttach = new JRadioButton("Enable", true);
        JRadioButton disableAttach = new JRadioButton("Disable");

        ButtonGroup attachGroup = new ButtonGroup();
        attachGroup.add(enableAttach);
        attachGroup.add(disableAttach);

        attachTogglePanel.add(attachLabel);
        attachTogglePanel.add(enableAttach);
        attachTogglePanel.add(disableAttach);

        panel.add(attachTogglePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));


        // Select PDF Folder (input)
        JTextField documentPathField = new JTextField();

        // NEW: Select PDF Folder (input)
        JTextField pdfFolderPathField = new JTextField();
        JButton browsePdfFolderButton = new JButton("Browse");
        documentPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(createLabeledField("Select Other Documents Folder (lebenslauf , Zeugnisse , Medias...etc):", pdfFolderPathField, browsePdfFolderButton));
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        browsePdfFolderButton.addActionListener(e -> browseFolder(pdfFolderPathField, formFrame));




        // Create dropdown for sending options
        String[] options = {"Send Today Only", "Send Yesterday Only", "Send Globally"};
        JComboBox<String> sendOptions = new JComboBox<>(options);
        sendOptions.setFont(inputFont);
        panel.add(sendOptions);
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Space before buttons// Create dropdown for sending options



        // Create Submit button (Green)
        JButton submitButton = new JButton("Submit");
        submitButton.setBackground(new Color(34, 193, 34)); // Gmail green color
        submitButton.setForeground(Color.BLACK);
        submitButton.setFont(new Font("Arial", Font.BOLD, 12));
        submitButton.setPreferredSize(new Dimension(100, 40)); // Adjust size of button

        // Create Cancel button (Red)
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(234, 67, 53)); // Gmail red color
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFont(new Font("Arial", Font.BOLD, 12));
        cancelButton.setPreferredSize(new Dimension(100, 40)); // Adjust size of button

        // Create Clear button (Yellow)
        JButton clearButton = new JButton("Clear");
        clearButton.setBackground(new Color(255, 204, 0)); // Gmail yellow color
        clearButton.setForeground(Color.BLACK);
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setPreferredSize(new Dimension(100, 40)); // Adjust size of button

        // Create a JPanel for the buttons and set its layout to FlowLayout for horizontal alignment
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Center buttons with some spacing
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(clearButton);

        // Add the button panel to the main form panel
        panel.add(buttonPanel);
        // Add window listener to handle closing
        formFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            }
        });
        // Set the frame's content pane to the panel
        formFrame.setContentPane(panel);

        // Show the frame
        formFrame.setVisible(true);




        // Handle the submit button's action
        submitButton.addActionListener(e -> {
                    JobApplicationU.startLoader();


//            String pdfFolderPath = pdfFolderPathField.getText().trim();
//
//
//            File pdfFolder = new File(pdfFolderPath);
//            if (!pdfFolder.isDirectory()) {
//                JOptionPane.showMessageDialog(frame,
//                        "Please select a valid folder",
//                        "Invalid Folder",
//                        JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//            filePaths.clear(); // ✅ Clear previous entries before adding new ones
//
//            // Retrieve all file paths in the folder
//            File[] files = pdfFolder.listFiles(File::isFile);
//            if (files != null) {
//                for (File file : files) {
//                    filePaths.add(file.getAbsolutePath());
//                }
//            }
//            if (filePaths.isEmpty()) {
//                JOptionPane.showMessageDialog(frame, "No files found in the selected folder", "Warning", JOptionPane.WARNING_MESSAGE);
//                // Proceed even if no files are found, adjust as needed
//            }

                    boolean attachmentsEnabled = enableAttach.isSelected();
                    if (attachmentsEnabled) {
                        // Keep all your existing attachment code
                        String pdfFolderPath = pdfFolderPathField.getText().trim();
                        File pdfFolder = new File(pdfFolderPath);
                        if (!pdfFolder.isDirectory()) {
                            JOptionPane.showMessageDialog(formFrame,
                                    "Please select a valid folder",
                                    "Invalid Folder",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        filePaths.clear();
                        File[] files = pdfFolder.listFiles(File::isFile);
                        if (files != null) {
                            for (File file : files) {
                                filePaths.add(file.getAbsolutePath());
                            }
                        }
                    } else {
                        filePaths.clear(); // Clear attachments if disabled
                    }


            String email = emailField.getText();
                    String pass = new String(passwordField.getPassword());
                    String sub = subjectField.getText();
                    String msgbody = messageArea.getText();

            // Check if any field is empty
            if (email.isEmpty() || pass.isEmpty() || sub.isEmpty() || msgbody.isEmpty()) {
                JOptionPane.showMessageDialog(formFrame, "Please Fill all fields.", "Empty", JOptionPane.ERROR_MESSAGE);
                return; // Stop execution
            }
                    if (!email.isEmpty() || !pass.isEmpty() || !sub.isEmpty() || !msgbody.isEmpty()) {

                        // Call the method to validate the email
                        if (isValidEmailDomain(email)) {
                            // Get the entered data
                            emailer.email = email;
                            emailer.pass = pass;
                            emailer.sub = sub;
                            emailer.msgbody = msgbody;
                            S = true;

                            // Retrieve the selected option from the dropdown
                            selectedOption = (String) sendOptions.getSelectedItem();
                            System.out.println("Selected Option: " + selectedOption); // This will print the selected option

                            System.out.println(emailer.email);
                            System.out.println(emailer.pass);
                            System.out.println(emailer.sub);
                            System.out.println(emailer.msgbody);

                            // Show a success message dialog
                            JOptionPane.showMessageDialog(formFrame, "Data has been processed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                            formFrame.dispose();
                            emailer.main();
                        } else {
                            // Show error message
                            JOptionPane.showMessageDialog(formFrame, "PLEASE USE GMAIL FOR BETTER RESULTS . HOTMAIL/OUTLOOK , ICLOUD & YAHOO ARE MORE LIKELY TO GO SPAM AND HR RECRUITERS LIKELY WON'T SEE YOUR EMAIl .", "Invalid Email", JOptionPane.ERROR_MESSAGE);

                        }
                    } else {
                        formFrame.dispose();
                    }
                }
        );

        // Handle the cancel button's action
        cancelButton.addActionListener(e -> {
            // Close the form (exit the application)
            formFrame.dispose();
        });


        // Handle the clear button's action
        clearButton.addActionListener(e -> {
            // Clear all the fields
            emailField.setText("");
            passwordField.setText("");
            subjectField.setText("");
            messageArea.setText("");

        });




    }
    // Method to validate the email domain
    public static boolean isValidEmailDomain(String email) {
        // List of disallowed domains
        String[] allowedDomains = {"gmail", "googlemail", "usmba", "ofppt","est","fst","fsjes","um6"};

        // Extract the domain part of the email
        String emailDomain = email.split("@")[1].toLowerCase(); // Extract domain after '@' and convert to lowercase

        // Check if the domain includes any disallowed keyword
        for (String domain : allowedDomains) {
            if (emailDomain.contains(domain)) {
                return true; // Email is valid if it contains a disallowed domain
            }
        }
        return false; // Email is invalid if it doesn't contain a disallowed domain
    }

    public static String[] showInputDialog() {
        final String[] results = new String[4]; // Increased size to include search type
        JDialog dialog = new JDialog((Frame) null, "Job Application Details", true);
        dialog.setSize(450, 420); // Adjusted height to accommodate the layout
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
        panel.add(createLabeledField("Search Type:", radioPanel));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Select Document Template
        JTextField documentPathField = new JTextField();
        JButton browseButton = new JButton("Browse");
        documentPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(createLabeledField("Select Document Template:", documentPathField, browseButton));
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
        submitButton.addActionListener(e -> handleSubmit(nameField, jobProfileField, documentPathField, todayOnlyRadio, yesterdayOnlyRadio, results, dialog));
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
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
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

    private static void handleSubmit(JTextField nameField, JTextField jobProfileField, JTextField documentPathField, JRadioButton todayOnlyRadio, JRadioButton yesterdayOnlyRadio, String[] results, JDialog dialog) {
        String name = nameField.getText().trim();
        String jobProfile = jobProfileField.getText().trim();
        String documentPath = documentPathField.getText().trim();

        String searchType;

        if (todayOnlyRadio.isSelected()) {
            searchType = "Today Only";
        } else if (yesterdayOnlyRadio.isSelected()) {
            searchType = "Yesterday Only";
        } else {
            searchType = "Global Search";
        }


        if (name.isEmpty() || jobProfile.isEmpty() || documentPath.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!validateDocument(documentPath)) {
            JOptionPane.showMessageDialog(dialog, "Invalid document! Required placeholders: {{COMPANY}}, {{HR}}, {{ADDRESS}}, {{DATE}}", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        results[0] = name;
        results[1] = jobProfile;
        results[2] = documentPath;
        results[3] = searchType; // Store the search type in the results array
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

//    public static String[] showInputDialog() {
//
//
//        final String[] results = new String[3];
//        JDialog dialog = new JDialog((Frame) null, "Job Application Details", true);
//        dialog.setSize(450, 350);
//        dialog.setLocationRelativeTo(null);
//
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
//
//        JLabel header = new JLabel("Enter Your Job Application Details");
//        header.setFont(new Font("Arial", Font.BOLD, 16));
//        header.setAlignmentX(Component.CENTER_ALIGNMENT);
//        panel.add(header);
//        panel.add(Box.createRigidArea(new Dimension(0, 10)));
//
//        JTextField nameField = new JTextField();
//        JTextField jobProfileField = new JTextField();
//        JTextField documentPathField = new JTextField();
//        JButton browseButton = new JButton("Browse");
//
//        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
//        jobProfileField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
//        documentPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
//
//        panel.add(createLabeledField("Full Name:", nameField));
//        panel.add(createLabeledField("Job Profile:", jobProfileField));
//        panel.add(createLabeledField("Select Document Template:", documentPathField, browseButton));
//        panel.add(Box.createRigidArea(new Dimension(0, 10)));
//
//        JButton submitButton = createButton("Submit", new Color(34, 193, 34), Color.WHITE);
//        JButton cancelButton = createButton("Cancel", new Color(234, 67, 53), Color.WHITE);
//        JButton clearButton = createButton("Clear", new Color(255, 204, 0), Color.BLACK);
//
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
//        buttonPanel.add(submitButton);
//        buttonPanel.add(cancelButton);
//        buttonPanel.add(clearButton);
//        panel.add(buttonPanel);
//
//        browseButton.addActionListener(e -> browseFile(documentPathField, dialog));
//        submitButton.addActionListener(e -> handleSubmit(nameField, jobProfileField, documentPathField, results, dialog));
//        cancelButton.addActionListener(e -> dialog.dispose());
//        clearButton.addActionListener(e -> {
//            nameField.setText("");
//            jobProfileField.setText("");
//            documentPathField.setText("");
//        });
//
//        dialog.setContentPane(panel);
//        dialog.setVisible(true);
//        return results[0] != null ? results : null;
//    }
//
//    private static JPanel createLabeledField(String labelText, JTextField textField) {
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        JLabel label = new JLabel(labelText);
//        label.setAlignmentX(Component.LEFT_ALIGNMENT);
//        panel.add(label);
//        panel.add(Box.createRigidArea(new Dimension(0, 5)));
//        panel.add(textField);
//        return panel;
//    }
//
//    private static JPanel createLabeledField(String labelText, JTextField textField, JButton button) {
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        JLabel label = new JLabel(labelText);
//        label.setAlignmentX(Component.LEFT_ALIGNMENT);
//        panel.add(label);
//        panel.add(Box.createRigidArea(new Dimension(0, 5)));
//        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        textField.setPreferredSize(new Dimension(200, 30));
//        fieldPanel.add(textField);
//        fieldPanel.add(button);
//        panel.add(fieldPanel);
//        return panel;
//    }
//
//    private static JButton createButton(String text, Color bg, Color fg) {
//        JButton button = new JButton(text);
//        button.setBackground(bg);
//        button.setForeground(fg);
//        button.setFont(new Font("Arial", Font.BOLD, 12));
//        button.setPreferredSize(new Dimension(100, 40));
//        return button;
//    }
//
//    private static void browseFile(JTextField documentPathField, Component parent) {
//
//
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setFileFilter(new FileNameExtensionFilter("Word Documents (*.docx)", "docx"));
//        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
//            File selectedFile = fileChooser.getSelectedFile();
//            if (!validateDocument(selectedFile.getAbsolutePath())) {
//                JOptionPane.showMessageDialog(parent, "Invalid document! Required placeholders: {{COMPANY}}, {{HR}}, {{ADDRESS}}, {{DATE}}", "Error", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//            documentPathField.setText(selectedFile.getAbsolutePath());
//        }
//    }
//
//    private static void handleSubmit(JTextField nameField, JTextField jobProfileField, JTextField documentPathField, String[] results, JDialog dialog) {
//        String name = nameField.getText().trim();
//        String jobProfile = jobProfileField.getText().trim();
//        String documentPath = documentPathField.getText().trim();
//
//        if (name.isEmpty() || jobProfile.isEmpty() || documentPath.isEmpty()) {
//            JOptionPane.showMessageDialog(dialog, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        if (!validateDocument(documentPath)) {
//            JOptionPane.showMessageDialog(dialog, "Invalid document! Required placeholders: {{COMPANY}}, {{HR}}, {{ADDRESS}}, {{DATE}}", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        results[0] = name;
//        results[1] = jobProfile;
//        results[2] = documentPath;
//        auscrap.userN = name;
//        auscrap.jobProfile = jobProfile;
//        auscrap.templatePath = documentPath;
//        dialog.dispose();
//    }
//
//    public static boolean validateDocument(String filePath) {
//        Set<String> requiredPlaceholders = new HashSet<>(List.of("{{COMPANY}}", "{{HR}}", "{{ADDRESS}}", "{{DATE}}"));
//        try (FileInputStream fis = new FileInputStream(filePath); XWPFDocument document = new XWPFDocument(fis)) {
//            StringBuilder documentText = new StringBuilder();
//            for (XWPFParagraph paragraph : document.getParagraphs()) {
//                documentText.append(paragraph.getText()).append(" ");
//            }
//            return requiredPlaceholders.stream().allMatch(documentText.toString()::contains);
//        } catch (IOException e) {
//            System.err.println("Error reading document: " + e.getMessage());
//            return false;
//        }
//    }
}