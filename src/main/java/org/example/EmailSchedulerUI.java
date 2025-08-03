package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.SpinnerDateModel;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

public class EmailSchedulerUI {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Email Scheduler");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        // Create a label for instructions
        JLabel label = new JLabel("Select Sending Option:");
        frame.add(label);

        // Button to send today only
        JButton sendTodayButton = new JButton("Send Today Only");
        frame.add(sendTodayButton);

        // Button to send yesterday only
        JButton sendYesterdayButton = new JButton("Send Yesterday Only");
        frame.add(sendYesterdayButton);

        // Create a spinner for sending specific date
        JLabel dateLabel = new JLabel("Select Date:");
        frame.add(dateLabel);

        // Set the spinner for date selection
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        frame.add(dateSpinner);

        // Button to send specific date
        JButton sendSpecificDateButton = new JButton("Send Specific Date");
        frame.add(sendSpecificDateButton);

        // Button to send globally
        JButton sendGloballyButton = new JButton("Send Globally");
        frame.add(sendGloballyButton);

        // Action listeners for buttons
        sendTodayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Sending today only...");
                processEmailsForDate("today");
            }
        });

        sendYesterdayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Sending yesterday only...");
                processEmailsForDate("yesterday");
            }
        });

        sendSpecificDateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Date selectedDate = (Date) dateSpinner.getValue();
                if (selectedDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    String formattedDate = sdf.format(selectedDate);
                    System.out.println("Sending for specific date: " + formattedDate);
                    processEmailsForDate(formattedDate);
                } else {
                    JOptionPane.showMessageDialog(frame, "Please select a date.");
                }
            }
        });

        sendGloballyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Sending globally...");
                processEmailsForDate("all");
            }
        });

        // Make the frame visible
        frame.setVisible(true);
    }

    // Placeholder method for processing emails based on the selected date
    public static void processEmailsForDate(String selectedDate) {
        // Here you can call the method to filter and send emails based on the selected option
        System.out.println("Processing emails for: " + selectedDate);
        // You would call your existing logic for filtering emails and sending them based on the date
    }
}
