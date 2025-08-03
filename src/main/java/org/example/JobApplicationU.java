package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.example.auscrap.*;


public class JobApplicationU {

    public static void startLoader() {
        loadingCircle.setVisible(true);
        loadingCircle.start();
        frame.setEnabled(false);
    }

    public static void stopLoader() {
        loadingCircle.stop();
        loadingCircle.setVisible(false);
        frame.setEnabled(true);
        frame.toFront();
    }
    private static JFrame frame; // Add this line at class level

    public static void rn() {

        SwingUtilities.invokeLater(JobApplicationU::createAndShowUI);
    }

   public static String licenseKey = "";

    public static void main(String[] args) {

        if (!isLicenseFilePresent(KEY_FILE)) {
            int choice = 2;
            // If the key file does not exist, prompt the user for a license key
            if(!isLicenseFilePresent(KEY_trial)) {
                // Create custom buttons
                Object[] options = {"Free Trial", "Activate Key"};

                // Show the dialog with custom buttons
                choice = JOptionPane.showOptionDialog(
                        null, // parent component
                        "No license found. Would you like to start a Free trial period or activate with a license key?", // message
                        "Activation Required", // title
                        JOptionPane.DEFAULT_OPTION, // option type
                        JOptionPane.QUESTION_MESSAGE, // message type
                        null, // icon
                        options, // options
                        options[0] // default option (48h Trial)
                );
                if (choice == 0) { // 48h Trial selected
                    // Store current timestamp in trial file
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    makeFileHidden(KEY_trial, timestamp);

                    // Calculate expiration date for user info
                    Date expirationDate = new Date(System.currentTimeMillis() + 168 * 60 * 60 * 1000);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    JOptionPane.showMessageDialog(null,
                            "Free trial period started!\nExpires: " + sdf.format(expirationDate),
                            "Trial Activated",
                            JOptionPane.INFORMATION_MESSAGE);
                    rn();
                    return;
                } else if (choice == -1) { // Dialog closed
                    System.out.println("Activation canceled!");
                    System.exit(0);
                } else {
                    // License key activation path
                    licenseKey = JOptionPane.showInputDialog(null, "Enter your license key:");

                    if (licenseKey == null || licenseKey.trim().isEmpty()) {
                        System.out.println(licenseKey == null ? "Activation canceled!" : "Key is empty!");
                        System.exit(0);
                    } else {
                        validateLicenseKey(licenseKey);
                        rn();
                    }
                }


            }
            else {
                // Trial file exists - check if expired
                try {
                    File trialFile = getLicenseFile(KEY_trial);
                    String timestampStr = Files.readString(trialFile.toPath());
                    long activationTime = Long.parseLong(timestampStr);
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - activationTime > 168 * 60 * 60 * 1000) {

                        LicensePopup.showLicensePopup();
                        // License key activation path
//                        licenseKey = JOptionPane.showInputDialog(null, "Enter your license key:");

                        if (licenseKey == null || licenseKey.trim().isEmpty()) {
                            System.out.println(licenseKey == null ? "Activation canceled!" : "Key is empty!");
                            System.exit(0);
                        } else {
                            validateLicenseKey(licenseKey);
                            rn();
                        }
                    } else {
                        // Show remaining time
//                        long remainingHours = TimeUnit.MILLISECONDS.toHours(
//                                (activationTime + 48 * 60 * 60 * 1000) - currentTime);
//                        JOptionPane.showMessageDialog(null,
//                                "Trial active (" + remainingHours + " hours remaining)",
//                                "Trial Mode",
//                                JOptionPane.INFORMATION_MESSAGE);
                        rn();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }}else{
            // Already activated
            System.out.println("Program is already activated. Proceeding...");
            rn();}}

    private static loader loadingCircle;

    private static void createAndShowUI() {
        frame = new JFrame("Job Application Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLocationRelativeTo(null);
//        frame.setAlwaysOnTop(true);


        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(new Color(45, 45, 45));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title Panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Create center container panel
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setBackground(new Color(45, 45, 45));

        // Logo Panel (your original unchanged version)
        JPanel logoPanel = createLogoPanel();
        centerContainer.add(logoPanel);

        // Add spacing between logo and loading circle
        centerContainer.add(Box.createVerticalStrut(2));

        // Create and add loading circle (initially hidden)
        loadingCircle = new loader();
        loadingCircle.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadingCircle.setVisible(false);
        centerContainer.add(loadingCircle);

        mainPanel.add(centerContainer, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(45, 45, 45));

        JLabel titleLabel = new JLabel("Welcome to AUTO-BEWERBER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        panel.add(titleLabel);
        return panel;

    }

    private static JPanel createLogoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(45, 45, 45));


        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            URL logoUrl = new URL("https://i.imgur.com/HzSVPAP.png");
            ImageIcon logoIcon = new ImageIcon(new ImageIcon(logoUrl).getImage().getScaledInstance(120, 200, Image.SCALE_SMOOTH));
            logoLabel.setIcon(logoIcon);
        } catch (Exception e) {
            System.err.println("Failed to load logo: " + e.getMessage());
            logoLabel.setText("LOGO"); // Fallback text
            logoLabel.setForeground(Color.WHITE);
        }

        panel.add(logoLabel);
        return panel;
    }
    private static JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 20, 20));
        panel.setBackground(new Color(45, 45, 45));

        JButton searchButton = createStyledButton("Search", "https://cdn-icons-png.flaticon.com/512/622/622669.png", new Color(70, 130, 180));
        JButton sendButton = createStyledButton("Send", "https://cdn-icons-png.flaticon.com/512/561/561127.png", new Color(34, 193, 34));
        JButton dataManagerButton = createStyledButton("Data Manager", "https://cdn-icons-png.flaticon.com/512/2602/2602732.png", new Color(234, 67, 53));
        JButton contactDevButton = createStyledButton("Contact Dev", "https://i.imgur.com/k4U6Nb8.png",  new Color(214, 157, 13));


        // Add ActionListener for Search Button
        searchButton.addActionListener((ActionEvent e) -> {
            SwingUtilities.invokeLater(() -> {

                try {
                    loadingCircle.setVisible(true);
                    loadingCircle.start();
                    // Run the operation in background
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            auscrap.main();
                            return null;
                        }

                        @Override
                        protected void done() {
                            loadingCircle.stop();
                            loadingCircle.setVisible(false);
                        }
                    }.execute();
                } catch (Exception ex) {
                    loadingCircle.stop();
                    loadingCircle.setVisible(false);

            }});
        });

        sendButton.addActionListener((ActionEvent e) -> {
            // Show loading circle immediately
            loadingCircle.setVisible(true);
            loadingCircle.start();
            frame.setEnabled(false);

            // Set the close callback before showing UserInputForm
            UserInputForm.setOnCloseCallback(() -> {
                loadingCircle.stop();
                loadingCircle.setVisible(false);
                frame.setEnabled(true);
                frame.toFront();
            });

            // Show the form
            try {
                UserInputForm.main();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });

        dataManagerButton.addActionListener((ActionEvent e) -> {

            try {
                // Call the auscrap class
                DataManagerUI.main();
            } catch (Exception ex) {

            }
        });

        contactDevButton.addActionListener((ActionEvent e) -> {

            try {
                // Call the auscrap class
                ContactCard.main();
            } catch (Exception ex) {

            }
        });
        panel.add(searchButton);
        panel.add(sendButton);
        panel.add(dataManagerButton);
        panel.add(contactDevButton);

        return panel;
    }

    private static JButton createStyledButton(String text, String iconUrl, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(200, 70));

        try {
            URL url = new URL(iconUrl);
            ImageIcon icon = new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
            button.setIcon(icon);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + e.getMessage());
        }

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


}
