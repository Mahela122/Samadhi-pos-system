/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
/**
 *
 * @author tharu
 */
public class NavBar {
    
// Makes a thin bar with one Back button on the left
    public static JPanel create(String text, Runnable onBack) {
        JButton backBtn = new JButton(text);
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setForeground(new Color(11, 107, 109));
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> onBack.run());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(new Color(245, 243, 240));
        bar.add(backBtn);
        return bar;
    }

    // Puts the Back bar on top of a whole window (for screens that open in their own window)
    public static void addTo(JFrame frame, String text, Runnable onBack) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(create(text, onBack), BorderLayout.NORTH);
        wrapper.add(frame.getContentPane(), BorderLayout.CENTER);
        frame.setContentPane(wrapper);
        frame.pack();
    }    
}
