package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;

public class LicensePopup {
    private static JDialog popupDialog;
    private static boolean licenseValidated = false;

    public static void showLicensePopup() {
        // Create modal dialog
        popupDialog = new JDialog((JFrame)null, "License Required", true);
        popupDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupDialog.setSize(500, 450);
        popupDialog.setLocationRelativeTo(null);
        popupDialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(new Color(45, 45, 45));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title Panel with warning logo
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(45, 45, 45));
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        // Trial Expired label
        JLabel titleLabel = new JLabel("Trial Expired", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Warning logo (yellow exclamation mark)
        JLabel warningLogo = new JLabel();
        warningLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            ImageIcon warningIcon = new ImageIcon(new URL("https://png.pngtree.com/png-vector/20230910/ourmid/pngtree-exclamation-mark-sign-design-png-image_9482265.png"));
            warningIcon = new ImageIcon(warningIcon.getImage().getScaledInstance(130, 140, Image.SCALE_SMOOTH));
            warningLogo.setIcon(warningIcon);
        } catch (Exception e) {
            warningLogo.setText("(!)");
            warningLogo.setForeground(Color.YELLOW);
            warningLogo.setFont(new Font("Arial", Font.BOLD, 24));
        }

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(10));
        titlePanel.add(warningLogo);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Message Panel
        JLabel message = new JLabel(
                "<html><center><br>Your Free trial period has ended.<br><br>Please purchase a license to continue.</center></html>",
                SwingConstants.CENTER
        );
        message.setFont(new Font("Arial", Font.PLAIN, 16));
        message.setForeground(Color.WHITE);
        mainPanel.add(message, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        buttonPanel.setBackground(new Color(45, 45, 45));

        // Contact Button - WhatsApp green with black text
        JButton contactButton = createStyledButton(
                "Contact Dev",
                "https://i.imgur.com/k4U6Nb8.png",
                new Color(37, 211, 102), // WhatsApp green color
                Color.BLACK // Text color
        );
        addHoverEffects(contactButton, new Color(37, 211, 102));
        contactButton.addActionListener(e -> openWhatsApp());

        // License Button - Original blue color
        JButton enterKeyButton = createStyledButton(
                "Enter License",
                "https://cdn-icons-png.flaticon.com/512/1457/1457030.png",
                new Color(70, 130, 180),
                Color.WHITE
        );
        addHoverEffects(enterKeyButton, new Color(70, 130, 180));
        enterKeyButton.addActionListener(e -> handleLicenseInput());

        buttonPanel.add(contactButton);
        buttonPanel.add(enterKeyButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        popupDialog.add(mainPanel);
        popupDialog.setVisible(true);

        if (!licenseValidated) {
            System.exit(0);
        }
    }

    private static void handleLicenseInput() {
        String licenseKey = JOptionPane.showInputDialog(
                popupDialog,
                "Enter your license key:",
                "License Activation",
                JOptionPane.PLAIN_MESSAGE
        );

        if (licenseKey == null) {
            return;
        }

        if (licenseKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    popupDialog,
                    "License key cannot be empty!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JobApplicationU.licenseKey = licenseKey.trim();
        org.example.auscrap.validateLicenseKey(JobApplicationU.licenseKey);
        licenseValidated = true;
        popupDialog.dispose();
        JobApplicationU.rn();
    }

    private static JButton createStyledButton(String text, String iconUrl, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(textColor); // Set text color parameter
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        try {
            ImageIcon icon = new ImageIcon(new URL(iconUrl));
            button.setIcon(new ImageIcon(icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            System.err.println("Error loading button icon: " + e.getMessage());
        }

        return button;
    }

    private static void addHoverEffects(JButton button, Color baseColor) {
        Color hoverColor = baseColor.brighter();
        Color clickColor = baseColor.darker();

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(clickColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(hoverColor);
            }
        });
    }

    private static void openWhatsApp() {
        try {
            String url = "https://wa.me/212620412354?text=Halo!!%20bghit%20nakhd%20version%20complet%20dyal%20AUTO-BEWERBER%20license.";
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(popupDialog, "Error opening WhatsApp.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}