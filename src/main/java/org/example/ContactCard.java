package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URI;
import java.net.URL;

public class ContactCard {
    public static void main() {
        SwingUtilities.invokeLater(ContactCard::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Contact Developer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 680);
        frame.setLocationRelativeTo(null);
//        frame.setAlwaysOnTop(true);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(new Color(45, 45, 45));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title Panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Logo Panel (Replaced with your picture)
        JPanel logoPanel = createLogoPanel();
        mainPanel.add(logoPanel, BorderLayout.CENTER);

        // Button Panel (Updated to stack buttons vertically)
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(45, 45, 45));

        JLabel titleLabel = new JLabel("Let's Talk!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        panel.add(titleLabel);
        return panel;
    }
    private static JPanel createLogoPanel() {
        JPanel panel = new JPanel(new BorderLayout()); // Use BorderLayout to center the image
        panel.setBackground(new Color(45, 45, 45));

        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center the image horizontally
        logoLabel.setVerticalAlignment(SwingConstants.CENTER);   // Center the image vertically

        // Load the image at a fixed, reasonable size
        try {
            URL logoUrl = new URL("https://avatars.githubusercontent.com/u/150919474?v=4");
            ImageIcon originalIcon = new ImageIcon(logoUrl);

            // Set a fixed size for the image (e.g., 200x200 or any size you prefer)
            int width = 170; // Desired width
            int height = 170; // Desired height
            Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            System.err.println("Failed to load logo: " + e.getMessage());
            logoLabel.setText("Your Picture"); // Fallback text
            logoLabel.setForeground(Color.WHITE);
        }

        // Add margin around the logoLabel using an empty border
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // 20px margin at the bottom
        panel.add(logoLabel, BorderLayout.CENTER);

        return panel;
    }

    private static JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 0, 20)); // Vertical layout with 4 rows, 20px vertical spacing
        panel.setBackground(new Color(45, 45, 45));

        // Add margin around the button panel using an empty border
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // 20px margin at the top

        // Updated buttons with new labels and matching icons, stacked vertically
        JButton linkedinButton = createStyledButton("LinkedIn  ", "https://cdn-icons-png.flaticon.com/512/174/174857.png", new Color(22, 44, 113)); // Blue
        JButton emailButton = createStyledButton("Email Me  ", "https://cdn-icons-png.flaticon.com/512/561/561127.png", new Color(234, 67, 53)); // Green
        JButton whatsappButton = createStyledButton("WhatsApp Me  ", "https://i.imgur.com/k4U6Nb8.png", new Color(34, 193, 34)); // Red
        JButton githubButton = createStyledButton("GitHub  ", "https://cdn-icons-png.flaticon.com/512/25/25231.png", new Color(214, 157, 13)); // Gold

        // Add ActionListeners for the new buttons
        linkedinButton.addActionListener((ActionEvent e) -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI("https://www.linkedin.com/in/medaguili/")); // Replace with your LinkedIn URL
                } else {
                    JOptionPane.showMessageDialog(null, "Desktop operations are not supported on this platform.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Failed to open LinkedIn: " + ex.getMessage());
            }
        });

        emailButton.addActionListener((ActionEvent e) -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().mail(new URI("mailto:mohammedelaguili@outlook.com")); // Replace with your email
                } else {
                    JOptionPane.showMessageDialog(null, "Desktop operations are not supported on this platform.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Failed to open email client: " + ex.getMessage());
            }
        });

        whatsappButton.addActionListener((ActionEvent e) -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI("https://wa.me/212609983730")); // Replace with your phone number
                } else {
                    JOptionPane.showMessageDialog(null, "Desktop operations are not supported on this platform.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Failed to open WhatsApp: " + ex.getMessage());
            }
        });

        githubButton.addActionListener((ActionEvent e) -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI("https://github.com/medaguili")); // Replace with your GitHub username
                } else {
                    JOptionPane.showMessageDialog(null, "Desktop operations are not supported on this platform.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Failed to open GitHub: " + ex.getMessage());
            }
        });
        panel.add(linkedinButton);
        panel.add(emailButton);
        panel.add(whatsappButton);
        panel.add(githubButton);

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