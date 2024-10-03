package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) {

        // Utworzenie ramki
        JFrame frame = new JFrame("Start");
        frame.setSize(500, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        // Ustawienie menedżera układu na FlowLayout
        frame.setLayout(new FlowLayout());

        // Utworzenie pierwszego przycisku
        JButton button = new JButton("Start");
        frame.getContentPane().add(button);

        // Utworzenie drugiego przycisku
        JButton button2 = new JButton("Ustawienia menu");
        frame.getContentPane().add(button2);

        // Utworzenie trzeciego przycisku
        JButton button3 = new JButton("Stany");
        frame.getContentPane().add(button3);

        // Dodanie akcji do przycisków
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
                Obsluga o = new Obsluga();
                try {
                    o.start();
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "Przycisk Ustawienia menu został kliknięty!");
            }
        });

        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "Przycisk Stany został kliknięty!");
            }
        });

        // Wyświetlenie ramki
        frame.setVisible(true);
    }
}
