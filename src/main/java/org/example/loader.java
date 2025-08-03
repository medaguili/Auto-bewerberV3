package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

public class loader extends JComponent {
    private float angle = 0;
    private final Timer timer;
    private final Color circleColor = new Color(0, 120, 215); // Windows 11 accent blue
    private int circleSize = 70;  // Increased from original 64


    public loader() {
        setPreferredSize(new Dimension(circleSize, circleSize));

        timer = new Timer(15, e -> {
            angle = (angle + 5) % 360;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Center the circle in the component
        int x = (getWidth() - circleSize) / 2;
        int y = (getHeight() - circleSize) / 2;

        // Draw with larger size and thicker stroke
        g2.setColor(new Color(45, 45, 45));
        g2.fillOval(x, y, circleSize, circleSize);

        g2.setColor(circleColor);
        g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Thicker
        g2.draw(new Arc2D.Float(x+3, y+3, circleSize-6, circleSize-6, angle, 90, Arc2D.OPEN));

        g2.dispose();
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }
}