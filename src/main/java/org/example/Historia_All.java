package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class Historia_All extends JFrame {

    List<Paragon> paragons_all;
    Obsluga okno_glowne;

    public Historia_All(String nazwa, List<Paragon> paragons_all, Obsluga obsluga) {
        super(nazwa);
        this.paragons_all = paragons_all;
        this.okno_glowne = obsluga;

        // Ustawienie pełnego ekranu
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Tworzymy główny panel z siatką (GridLayout z 6 panelami w wierszu)
        JPanel mainPanel = new JPanel(new GridLayout(0, 6, 10, 10)); // 6 kolumn, dynamiczna liczba wierszy

        // Dodajemy panele dla każdego paragonu
        for (Paragon paragon : paragons_all) {
            JPanel paragonPanel = createParagonPanel(paragon, mainPanel);
            mainPanel.add(paragonPanel);
        }

        // JScrollPane dla przewijania w pionie
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);  // Zwiększenie szybkości przewijania

        // Dodanie JScrollPane do okna
        add(scrollPane, BorderLayout.CENTER);

        // Ustawienie okna jako widocznego
        setVisible(true);
    }

    private JPanel createParagonPanel(Paragon paragon, JPanel parentPanel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Utworzenie obramowania z większą czcionką
        Font titleFont = new Font("Arial", Font.BOLD, 28);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Stolik: " + paragon.getNumer_stolika()+ " " + paragon.getKelner());
        border.setTitleFont(titleFont);
        panel.setBorder(border);

        // Tworzenie subtytułu
        JLabel subtitleLabel = new JLabel(paragon.getCreate_time()+ " - " + paragon.getDestroy_time());
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);  // Wyrównanie do środka

        // Dodanie subtytułu do panelu (na dole panelu)
        panel.add(subtitleLabel, BorderLayout.NORTH);
        // Panel dla produktów
        JPanel productsPanel = new JPanel();
        productsPanel.setLayout(new GridLayout(paragon.getProducts().size(), 1)); // GridLayout dla produktów

        // Dodanie produktów do panelu
        for (Produkt_na_paragonie produkt : paragon.getProducts()) {
            JPanel productPanel = new JPanel();
            productPanel.setLayout(new BorderLayout());

            // Tworzenie tekstu dla produktu
            String productInfo = produkt.getNazwa() + " - Ilość: " + produkt.getIlosc() + "\n" + produkt.getNotatka();
            JTextArea productTextArea = new JTextArea(productInfo);
            productTextArea.setFont(new Font("Arial", Font.PLAIN, 20));
            productTextArea.setEditable(false);
            productTextArea.setWrapStyleWord(true);
            productTextArea.setLineWrap(true);
            productTextArea.setBackground(Color.WHITE);

            if(produkt.getCzas_wydania() != null)
            {
                productTextArea.setForeground(Color.WHITE);
                productTextArea.setBackground(Color.decode("#FF7F7F"));
                productTextArea.append(produkt.getCzas_wydania());
            }


            // Dodanie komponentów do panelu produktu
            productPanel.add(productTextArea, BorderLayout.CENTER);

            productsPanel.add(productPanel);
        }

        // Dodanie produktów do głównego panelu
        panel.add(productsPanel, BorderLayout.CENTER);

        // Dodanie przycisku usuwania paragonu
        JButton removeButton = new JButton("Cofnij paragon");
        removeButton.setFont(new Font("Arial", Font.BOLD, 16));
        removeButton.setPreferredSize(new Dimension(180, 40)); // Ustawienie stałej wysokości
        removeButton.setBackground(Color.GRAY);
        removeButton.setForeground(Color.lightGray);

        removeButton.addActionListener(e -> {
            okno_glowne.addParagonFromHistory(paragon);
            dispose();
        });

        JPanel buttonPanelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // FlowLayout dla przycisku usuwania
        buttonPanelBottom.add(removeButton);
        panel.add(buttonPanelBottom, BorderLayout.SOUTH); // Dodanie panelu z przyciskiem na dół
        return panel;
    }
}
