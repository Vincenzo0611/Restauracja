package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Obsluga {

    JFrame frame;
    JFrame okno_paragony;
    JPanel mainPanel_paragons;
    private boolean wasPopUpClosed = false;
    private static List<JPanel> productPanels = new ArrayList<>();
    private static List<JTextArea> notes_of_actual_products = new ArrayList<>();
    private static JPanel productsPanel; // Panel przechowujący wszystkie produkty
    private static JScrollPane scrollPane;
    private List<Dzial_menu> menu = new ArrayList<>();

    private Paragon aktualny = new Paragon(0);

    int id_paragonow = 1;
    int ilosc_produktow_na_paragonie = 0;

    List<Paragon> paragons_all = new ArrayList<>();
    List<Paragon> paragons_waiting = new ArrayList<>();

    List<ObjectOutputStream> monitory = new ArrayList<>();

    public void start() throws FileNotFoundException {

        // Wczytywanie pliku z menu
        File file = new File("m.txt");
        Scanner scanner = new Scanner(file);

        int id = 0;
        Dzial_menu aktualny_dzial = null;

        // Parsowanie pliku i tworzenie działów
        while (scanner.hasNextLine()) {
            String nazwa = scanner.nextLine();
            String k = scanner.nextLine();
            int kod = Integer.parseInt(k);

            if (kod == 0) {
                if (aktualny_dzial != null) {
                    menu.add(aktualny_dzial);
                }
                aktualny_dzial = new Dzial_menu(nazwa);
            } else {
                aktualny_dzial.produkty.add(new Produkt_z_menu(nazwa, id, kod));
            }
            id++;
        }
        scanner.close();
        menu.add(aktualny_dzial);

        // Tworzenie okna podzielonego na pół
        frame = new JFrame("Host");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();

        int font_size = 28;

        JMenu historyMenu = new JMenu("Historia");

        historyMenu.setFont(new Font("Arial", Font.PLAIN, font_size)); // Zmieniona czcionka

        JMenuItem history_not_paid = new JMenuItem("Do Płacenia");

        history_not_paid.addActionListener(e -> new Platnosc("Płatność", paragons_all, this));
        JMenuItem history_all = new JMenuItem("Wszystkie");
        history_all.addActionListener(e -> new Historia_All("Historia", paragons_all, this));

        history_not_paid.setFont(new Font("Arial", Font.PLAIN, font_size));
        history_all.setFont(new Font("Arial", Font.PLAIN, font_size));

        JMenu exitMenu = new JMenu("Zamknij");

        exitMenu.setFont(new Font("Arial", Font.PLAIN, font_size));

        JMenuItem exitItem = new JMenuItem("Zamknij aplikację");
        exitItem.addActionListener(e -> {
            // Wyświetlenie okna dialogowego z pytaniem
            int response = JOptionPane.showConfirmDialog(null,
                    "Czy na pewno chcesz zamknąć aplikację?",
                    "Potwierdzenie zamknięcia",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            // Sprawdzamy odpowiedź użytkownika
            if (response == JOptionPane.YES_OPTION) {

                try {
                    String fileName = generateFileNameWithDateTime();
                    zapiszParagonyDoPliku(paragons_all, fileName);
                } catch (IOException error) {
                    error.printStackTrace();
                }


                System.exit(0);  // Zamknięcie aplikacji
            }
            // Jeśli wybrano NO_OPTION lub okno zostało zamknięte, aplikacja kontynuuje działanie
        });

        exitItem.setFont(new Font("Arial", Font.PLAIN, font_size));

        JMenu addDeviceMenu = new JMenu("Dodaj urządzenie");

        addDeviceMenu.setFont(new Font("Arial", Font.PLAIN, font_size));

        JMenuItem monitorItem = new JMenuItem("Dodaj monitor");

        monitorItem.setFont(new Font("Arial", Font.PLAIN, font_size));

        monitorItem.addActionListener(e -> new AddNewMonitor().start(monitory, paragons_waiting));

        JMenuItem tabletItem = new JMenuItem("Dodaj tablet");

        tabletItem.setFont(new Font("Arial", Font.PLAIN, font_size));

        //exitItem.addActionListener(e -> new AddNewMonitor().start());

        exitMenu.add(exitItem);

        historyMenu.add(history_not_paid);
        historyMenu.add(history_all);

        addDeviceMenu.add(monitorItem);
        addDeviceMenu.add(tabletItem);

        menuBar.add(historyMenu);
        menuBar.add(exitMenu);
        menuBar.add(addDeviceMenu);

        menuBar.setPreferredSize(new Dimension(200, 40));

        frame.setJMenuBar(menuBar);
        // Uzyskiwanie rozmiarów ekranu
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        // Ustawienie rozmiaru okna na połowę ekranu
        int width = screenSize.width / 2;
        int height = screenSize.height;
        frame.setSize(width, height);
        frame.setLocation(0, 0); // Umiejscowienie okna

        // Ustawienie menedżera układu
        frame.setLayout(new GridLayout(1, 2)); // Podział na dwie kolumny

        // Tworzenie panelu dla działów menu
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());

        // Panel siatki z przyciskami dla działów menu
        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(menu.size(), 1)); // Siatka z przyciskami działów

        // Tworzenie przycisków działów
        for (Dzial_menu dzial : menu) {
            JButton button = new JButton(dzial.getNazwa());
            button.setFont(new Font("Arial", Font.PLAIN, 20)); // Zwiększenie czcionki przycisku
            gridPanel.add(button);

            // ActionListener dla przycisków działów
            button.addActionListener(e -> {
                // Po kliknięciu w przycisk działu, pokaż produkty działu
                pokazProdukty(panel1, dzial);
            });
        }

        // Dodanie siatki przycisków działów do panelu
        panel1.add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        // Panel na przyciski "Dodaj" i "Anuluj" na dole
        JPanel bottomButtonsPanel = new JPanel();
        bottomButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JButton addButton = new JButton("Dodaj");
        addButton.setBackground(Color.GREEN);
        addButton.setForeground(Color.WHITE); // Ustawienie koloru tekstu
        addButton.setPreferredSize(new Dimension(150, 50)); // Większy rozmiar przycisku
        addButton.setFont(new Font("Arial", Font.BOLD, 20)); // Większa czcionka dla "Dodaj"

        JButton cancelButton = new JButton("Anuluj");
        cancelButton.setBackground(Color.RED);
        cancelButton.setForeground(Color.WHITE); // Ustawienie koloru tekstu
        cancelButton.setPreferredSize(new Dimension(150, 50)); // Większy rozmiar przycisku
        cancelButton.setFont(new Font("Arial", Font.BOLD, 20)); // Większa czcionka dla "Anuluj"

        bottomButtonsPanel.add(addButton);
        bottomButtonsPanel.add(cancelButton);

        // Dodanie panelu z przyciskami na dole panelu
        panel1.add(bottomButtonsPanel, BorderLayout.SOUTH);

        // Panel na produkty
        productsPanel = new JPanel();
        productsPanel.setLayout(new BoxLayout(productsPanel, BoxLayout.Y_AXIS)); // Produkty w pionie

        // Dodanie scrollowania
        scrollPane = new JScrollPane(productsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Ustawienie szybszego scrollowania
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);  // Zwiększenie szybkości przewijania

        // Pobieranie paska przewijania
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

        // Ustawienie nowej szerokości paska przewijania (np. na 30 pikseli)
        verticalScrollBar.setPreferredSize(new Dimension(50, Integer.MAX_VALUE));


        // Dodanie paneli do okna
        frame.add(panel1);
        frame.add(scrollPane);

        // Wyświetlenie okna
        frame.setVisible(true);

        // Dodanie akcji dla przycisków "Dodaj" i "Anuluj"
        addButton.addActionListener(e -> {
            addButtonLogic();
        });

        cancelButton.addActionListener(e -> {
            CancelButtonLogic();
        });

        okno_paragony = new JFrame("Paragony");
        okno_paragony.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        okno_paragony.setSize(width, height);
        okno_paragony.setLocation(width, 0); // Umiejscowienie okna

        //okno_paragony.setLayout(new GridLayout(0, 3, 10, 10));

        mainPanel_paragons = new JPanel();
        mainPanel_paragons.setLayout(new GridLayout(0, 3, 10, 10)); // Układ siatki

        // Scrollowalny panel
        JScrollPane scrollPane = new JScrollPane(mainPanel_paragons);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        scrollPane.addMouseWheelListener(e -> {
            JScrollBar verticalScrollBar1 = scrollPane.getVerticalScrollBar();

            // Zmieniamy prędkość przewijania - np. 3 razy szybciej
            int scrollAmount = e.getUnitsToScroll() * verticalScrollBar1.getUnitIncrement() * 30;

            // Przewijamy o odpowiednią ilość
            verticalScrollBar1.setValue(verticalScrollBar1.getValue() + scrollAmount);
        });


        // Dodanie scrollowanego panelu do okna
        okno_paragony.add(scrollPane);

        okno_paragony.setVisible(true);
    }

    // Metoda do wyświetlania produktów z danego działu
    private void pokazProdukty(JPanel panel1, Dzial_menu dzial) {
        // Usunięcie poprzedniej zawartości panelu
        panel1.removeAll();
        panel1.setLayout(new BorderLayout());

        // Panel z siatką produktów (2 kolumny)
        JPanel productGridPanel = new JPanel();
        productGridPanel.setLayout(new GridLayout(0, 2)); // Dynamiczna liczba wierszy, 2 kolumny

        // Tworzenie przycisków produktów
        for (Produkt_z_menu produkt : dzial.produkty) {
            String buttonText = "<html><div style='text-align: center;'>" + produkt.getNazwa().replace(" ", "<br>") + "</div></html>";
            JButton productButton = new JButton(buttonText);
            //productButton.setFont(new Font("Arial", Font.PLAIN, 20)); // Zwiększenie czcionki przycisku
            productGridPanel.add(productButton);

            // ActionListener dla przycisków produktów
            productButton.addActionListener(e -> {
                // Akcja po kliknięciu w przycisk produktu
                aktualny.addProduct(new Produkt_na_paragonie(produkt, ilosc_produktow_na_paragonie));
                ilosc_produktow_na_paragonie++;
                addProduct(aktualny.getProducts().get(aktualny.getProducts().size() - 1));
            });
        }

        // Dodanie siatki z produktami do panelu
        panel1.add(new JScrollPane(productGridPanel), BorderLayout.CENTER);

        // Tworzenie przycisku "Cofnij"
        JButton backButton = new JButton("Cofnij");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.setBackground(Color.RED);
        backButton.setForeground(Color.WHITE);

        // Dodanie akcji dla przycisku "Cofnij"
        backButton.addActionListener(e -> {
            // Powrót do listy działów
            pokazDzialy(panel1);
        });

        // Panel na przycisk "Cofnij" oraz na stałe przyciski "Dodaj" i "Anuluj"
        JPanel bottomButtonsPanel = new JPanel();
        bottomButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // Dodanie przycisku "Cofnij"
        bottomButtonsPanel.add(backButton);

        // Dodanie przycisków "Dodaj" i "Anuluj"
        JButton addButton = new JButton("Dodaj");
        addButton.setBackground(Color.GREEN);
        addButton.setForeground(Color.WHITE);
        //addButton.setPreferredSize(new Dimension(150, 50));
        addButton.setFont(new Font("Arial", Font.BOLD, 20));
        bottomButtonsPanel.add(addButton);

        JButton cancelButton = new JButton("Anuluj");
        cancelButton.setBackground(Color.RED);
        cancelButton.setForeground(Color.WHITE);
        //cancelButton.setPreferredSize(new Dimension(150, 50));
        cancelButton.setFont(new Font("Arial", Font.BOLD, 20));
        bottomButtonsPanel.add(cancelButton);

        addButton.addActionListener(e -> {
            addButtonLogic();
        });

        cancelButton.addActionListener(e -> {
            CancelButtonLogic();
        });

        // Dodanie panelu na dół
        panel1.add(bottomButtonsPanel, BorderLayout.SOUTH);

        // Odświeżenie panelu
        panel1.revalidate();
        panel1.repaint();
    }

    // Metoda do wyświetlania działów (powrót)
    private void pokazDzialy(JPanel panel1) {
        // Usunięcie poprzedniej zawartości panelu
        panel1.removeAll();
        panel1.setLayout(new BorderLayout());

        // Panel siatki z przyciskami dla działów menu
        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(menu.size(), 1)); // Siatka z przyciskami działów

        // Tworzenie przycisków działów
        for (Dzial_menu dzial : menu) {
            JButton button = new JButton(dzial.getNazwa());
            button.setFont(new Font("Arial", Font.PLAIN, 20)); // Zwiększenie czcionki przycisku
            gridPanel.add(button);

            // ActionListener dla przycisków działów
            button.addActionListener(e -> {
                // Po kliknięciu w przycisk działu, pokaż produkty działu
                pokazProdukty(panel1, dzial);
            });
        }

        // Dodanie siatki z produktami do panelu
        panel1.add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        // Panel na przycisk "Cofnij" oraz na stałe przyciski "Dodaj" i "Anuluj"
        JPanel bottomButtonsPanel = new JPanel();
        bottomButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // Dodanie przycisków "Dodaj" i "Anuluj"
        JButton addButton = new JButton("Dodaj");
        addButton.setBackground(Color.GREEN);
        addButton.setForeground(Color.WHITE);
        addButton.setPreferredSize(new Dimension(150, 50));
        addButton.setFont(new Font("Arial", Font.BOLD, 20));
        bottomButtonsPanel.add(addButton);

        JButton cancelButton = new JButton("Anuluj");
        cancelButton.setBackground(Color.RED);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(150, 50));
        cancelButton.setFont(new Font("Arial", Font.BOLD, 20));
        bottomButtonsPanel.add(cancelButton);

        addButton.addActionListener(e -> {
            addButtonLogic();
        });

        cancelButton.addActionListener(e -> {
            CancelButtonLogic();
        });

        // Dodanie panelu na dół
        panel1.add(bottomButtonsPanel, BorderLayout.SOUTH);

        // Odświeżenie panelu
        panel1.revalidate();
        panel1.repaint();
    }

    private void addProduct(Produkt_na_paragonie product) {
        // Główny panel dla produktu
        JPanel productPanel = new JPanel();
        productPanel.setLayout(new BoxLayout(productPanel, BoxLayout.Y_AXIS)); // Ustawienie pionowego układu

        // Panel dla nazwy produktu, ilości i przycisku usuń (wszystko w jednej linii)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS)); // Ustawienie poziomego układu

        // Nazwa produktu
        JLabel nameLabel = new JLabel(product.getNazwa());

        // Pole do wyświetlania ilości
        JButton decreaseButton = new JButton("-");
        if (product.getIlosc() == 0)
            product.setIlosc(1);
        // Pole do wyświetlania ilości
        JTextField quantityField = new JTextField("" + product.getIlosc(), 2);
        quantityField.setHorizontalAlignment(JTextField.CENTER);

        // Ustawianie stałej wysokości i szerokości
        Dimension size = new Dimension(40, 30); // Szerokość 40 i wysokość 30, można dostosować
        quantityField.setPreferredSize(size);
        quantityField.setMinimumSize(size);
        quantityField.setMaximumSize(size);

        JButton increaseButton = new JButton("+");
        increaseButton.addActionListener(e -> {
            int currentQty = Integer.parseInt(quantityField.getText());
            quantityField.setText(String.valueOf(currentQty + 1));
            product.setIlosc(Integer.parseInt(quantityField.getText()));
        });

        decreaseButton.addActionListener(e -> {
            int currentQty = Integer.parseInt(quantityField.getText());
            if (currentQty > 1) {
                quantityField.setText(String.valueOf(currentQty - 1));
                product.setIlosc(Integer.parseInt(quantityField.getText()));
            }
        });

        // Przycisk "Usuń"
        JButton removeButton = new JButton("Usuń");

        // Dodawanie komponentów do topPanel (wszystko w jednej linii)
        topPanel.add(nameLabel);
        topPanel.add(Box.createHorizontalStrut(10)); // Dodanie odstępu między nazwą a przyciskami
        topPanel.add(decreaseButton);
        topPanel.add(quantityField);
        topPanel.add(increaseButton);
        topPanel.add(Box.createHorizontalGlue()); // Wypchnięcie przycisku "Usuń" na prawo
        topPanel.add(removeButton);

        // Pole notatki
        JTextArea noteArea = new JTextArea(product.getNotatka() != null ? product.getNotatka() : "", 3, 20); // Mniejsza liczba kolumn dla mniej rozciągniętego pola
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(noteArea); // Dodanie przewijania do notatki

        // Ustawianie stałej wysokości i szerokości
        Dimension sizen = new Dimension(150, 30); // Szerokość 40 i wysokość 30, można dostosować
        noteArea.setPreferredSize(sizen);
        noteArea.setMinimumSize(sizen);
        noteArea.setMaximumSize(sizen);


        removeButton.addActionListener(e -> removeProduct(productPanel, noteArea, product.getNumer_na_paragonie()));

        notes_of_actual_products.add(noteArea);

        // Dodanie paneli do głównego panelu
        productPanel.add(topPanel); // Pierwsza linia: nazwa, ilość, przyciski
        productPanel.add(scrollPane); // Druga linia: pole notatki

        // Dodanie głównego panelu produktu do głównego panelu produktów
        productsPanel.add(productPanel);
        productPanels.add(productPanel);

        // Aktualizacja widoku
        productsPanel.revalidate();
        productsPanel.repaint();
    }



    private void removeProduct(JPanel productPanel, JTextArea noteArea, int numer_produktu) {
        aktualny.removeProduct_number(numer_produktu);
        productsPanel.remove(productPanel);
        productPanels.remove(productPanel);
        notes_of_actual_products.remove(noteArea);

        // Aktualizacja widoku
        productsPanel.revalidate();
        productsPanel.repaint();
    }

    private void addButtonLogic()
    {
        if(aktualny.getProducts().isEmpty())
            return;
        showPopup(frame);
        if(!wasPopUpClosed)
        {
            return;
        }

        wasPopUpClosed = false;

        for (int i = 0; i < notes_of_actual_products.size(); i++) {
            JTextArea note = notes_of_actual_products.get(i);
            aktualny.getProducts().get(i).setNotatka(note.getText());
        }

        LocalTime currentTime = LocalTime.now();

        // Ustalenie formatu HH:mm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // Sformatowanie czasu do stringa
        String formattedTime = currentTime.format(formatter);

        if(aktualny.getCreate_time() == null) {
            aktualny.setCreate_time(formattedTime);
        }

        aktualny.getProducts().sort(Comparator.comparingInt(Produkt_na_paragonie::getId));

        paragons_all.add(aktualny);
        paragons_waiting.add(aktualny);

        paragons_waiting.sort(Comparator.comparingInt(Paragon::getId));
        paragons_all.sort(Comparator.comparingInt(Paragon::getId));

        // wysylka
        for(ObjectOutputStream out : monitory)
        {
            try {
                out.writeUTF("ADD_PARAGON"); // Typ danych
                out.writeObject(aktualny); // Serializacja obiektu
                out.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }


        aktualny = new Paragon(id_paragonow);
        id_paragonow++;

        //czyszczenie
        productsPanel.removeAll();
        productPanels.clear();
        notes_of_actual_products.clear();
        productsPanel.revalidate();
        productsPanel.repaint();

        mainPanel_paragons.removeAll();

        //na okno obok
        for (Paragon paragon : paragons_waiting) {
            JPanel paragonPanel = createParagonPanel(paragon, mainPanel_paragons);
            mainPanel_paragons.add(paragonPanel);
        }

        mainPanel_paragons.revalidate();
        mainPanel_paragons.repaint();

    }

    private void CancelButtonLogic()
    {
        aktualny.removeAllProducts();
        int id = aktualny.getId();
        aktualny = new Paragon(id);
        productsPanel.removeAll();
        productPanels.clear();
        notes_of_actual_products.clear();
        productsPanel.revalidate();
        productsPanel.repaint();
    }

    // Metoda tworząca pop-up
    private void showPopup(JFrame parentFrame)
    {
        // Tworzymy dialog, który jest powiązany z głównym oknem
        JDialog popupDialog = new JDialog(parentFrame, "Zakonczenie paragonu", true);
        popupDialog.setSize(500, 400);
        popupDialog.setLayout(new GridLayout(4, 1));  // Ustawienie układu w postaci siatki (4 wiersze, 1 kolumna)

        // Pole tekstowe do wpisania liczby
        JPanel numberPanel = new JPanel(new FlowLayout());
        JLabel numberLabel = new JLabel("Numer stolika:");
        numberLabel.setFont(new Font("Arial", Font.PLAIN, 30)); // Zwiększenie rozmiaru tekstu
        JTextField numberField;
        if(aktualny.getNumer_stolika() != 0)
        {
            numberField = new JTextField(""+ aktualny.getNumer_stolika(),4 );

        }
        else
        {
            numberField = new JTextField(4 );
        }
        numberField.setFont(new Font("Arial", Font.PLAIN, 30)); // Zwiększenie rozmiaru pola tekstowego
        numberPanel.add(numberLabel);
        numberPanel.add(numberField);

        JPanel kelnerPanel = new JPanel(new FlowLayout());
        JLabel kelnerLabel = new JLabel("Kelner:");
        numberLabel.setFont(new Font("Arial", Font.PLAIN, 30)); // Zwiększenie rozmiaru tekstu
        JTextField kelnerField;
        if(aktualny.getKelner() != null)
        {
            kelnerField = new JTextField(""+ aktualny.getKelner(),4 );

        }
        else
        {
            kelnerField = new JTextField(4 );
        }
        kelnerField.setFont(new Font("Arial", Font.PLAIN, 30)); // Zwiększenie rozmiaru pola tekstowego
        kelnerPanel.add(kelnerLabel);
        kelnerPanel.add(kelnerField);



        // Radio buttons (Tak/Nie)
        JPanel radioPanel = new JPanel(new FlowLayout());
        JLabel radioLabel = new JLabel("Zapłacone?");
        radioLabel.setFont(new Font("Arial", Font.PLAIN, 30));
        JRadioButton yesButton = new JRadioButton("Tak");
        yesButton.setFont(new Font("Arial", Font.PLAIN, 30)); // Zwiększenie rozmiaru przycisku
        JRadioButton noButton = new JRadioButton("Nie");
        noButton.setFont(new Font("Arial", Font.PLAIN, 30));  // Zwiększenie rozmiaru przycisku
        noButton.setSelected(true);
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(yesButton);
        radioGroup.add(noButton);
        radioPanel.add(radioLabel);
        radioPanel.add(yesButton);
        radioPanel.add(noButton);

        // Przycisk Zatwierdzający
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton confirmButton = new JButton("Zatwierdź");
        confirmButton.setFont(new Font("Arial", Font.PLAIN, 18)); // Zwiększenie rozmiaru przycisku
        confirmButton.setPreferredSize(new Dimension(150, 40));
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Pobieranie wartości z pola tekstowego i radiobuttonów
                String numberInput = numberField.getText();
                String kelnerInput = kelnerField.getText();
                boolean isYesSelected = yesButton.isSelected();
                boolean isNoSelected = noButton.isSelected();

                if (numberInput.isEmpty() || !isInteger(numberInput)) {
                    JOptionPane.showMessageDialog(popupDialog, "Musisz wpisać liczbę!", "Błąd", JOptionPane.ERROR_MESSAGE);
                } else if (!isYesSelected && !isNoSelected) {
                    JOptionPane.showMessageDialog(popupDialog, "Musisz wybrać opcję Tak lub Nie!", "Błąd", JOptionPane.ERROR_MESSAGE);
                }else if (kelnerInput.isEmpty()) {
                    JOptionPane.showMessageDialog(popupDialog, "Musisz wpisać kelnera!", "Błąd", JOptionPane.ERROR_MESSAGE);
                } else {

                    // Możemy wyświetlić to w konsoli (lub wykonać inne akcje)
                    aktualny.setNumer_stolika(Integer.parseInt(numberInput));
                    aktualny.setZaplacony(isYesSelected);
                    aktualny.setKelner(kelnerInput);

                    wasPopUpClosed = true;

                    // Zamknięcie okna po zatwierdzeniu
                    popupDialog.dispose();
                }
            }
        });

        // Dodanie komponentów do okna dialogowego
        popupDialog.add(numberPanel);
        popupDialog.add(kelnerPanel);
        popupDialog.add(radioPanel);
        popupDialog.add(confirmButton);

        popupDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Flaga ustawiona na true, gdy okno zostanie zamknięte przez "X"
                wasPopUpClosed = true;
            }
        });

        // Wyświetlenie okna
        popupDialog.setVisible(true);
    }

    private JPanel createParagonPanel(Paragon paragon, JPanel parentPanel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Utworzenie obramowania z większą czcionką
        Font titleFont = new Font("Arial", Font.BOLD, 28);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Stolik: " + paragon.getNumer_stolika() + " " + paragon.getKelner());
        border.setTitleFont(titleFont);
        panel.setBorder(border);

        // Tworzenie subtytułu
        JLabel subtitleLabel = new JLabel(paragon.getCreate_time());
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

            // Przycisk do zmiany koloru
            JButton changeColorButton = new JButton("X");
            changeColorButton.setPreferredSize(new Dimension(50, 100)); // Ustawienie stałej szerokości i wysokości

            // Użycie GridBagLayout, aby wyśrodkować przycisk w pionie
            JPanel buttonPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER; // Wyśrodkowanie w pionie
            buttonPanel.add(changeColorButton, gbc);

            // Dodanie akcji zmiany koloru tekstu i tła
            changeColorButton.addActionListener(e -> {
                if(produkt.getCzas_wydania() == null) {
                    productTextArea.setForeground(Color.WHITE);
                    productTextArea.setBackground(Color.decode("#FF7F7F"));
                    LocalTime currentTime = LocalTime.now();

                    // Ustalenie formatu HH:mm
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

                    // Sformatowanie czasu do stringa
                    String formattedTime = currentTime.format(formatter);
                    productTextArea.append(formattedTime);
                    produkt.setCzas_wydania(formattedTime);

                    for (ObjectOutputStream out : monitory) {
                        try {
                            out.reset();
                            out.writeUTF("DELETE_PRODUCT"); // Typ danych
                            out.writeObject(paragon); // Serializacja obiektu
                            out.flush();
                        } catch (IOException er) {
                            er.printStackTrace();
                        }
                    }
                }
                else
                {
                    productTextArea.setForeground(Color.BLACK);
                    productTextArea.setBackground(Color.WHITE);
                    if (productTextArea.getText().length() >= 5) {
                        // Pobierz aktualny tekst
                        String currentText = productTextArea.getText();

                        // Usuń ostatnie 5 znaków
                        productTextArea.replaceRange("", currentText.length() - 5, currentText.length());
                    }
                    produkt.setCzas_wydania(null);
                    for (ObjectOutputStream out : monitory) {
                        try {
                            out.reset();
                            out.writeUTF("DELETE_PRODUCT"); // Typ danych
                            out.writeObject(paragon); // Serializacja obiektu
                            out.flush();
                        } catch (IOException er) {
                            er.printStackTrace();
                        }
                    }
                }

            });

            // Dodanie komponentów do panelu produktu
            productPanel.add(productTextArea, BorderLayout.CENTER);
            productPanel.add(buttonPanel, BorderLayout.EAST); // Dodanie panelu z przyciskiem

            productsPanel.add(productPanel);
        }

        // Dodanie produktów do głównego panelu
        panel.add(productsPanel, BorderLayout.CENTER);

        // Dodanie przycisku usuwania paragonu
        JButton removeButton = new JButton("Usuń paragon");
        removeButton.setFont(new Font("Arial", Font.BOLD, 16));
        //removeButton.setPreferredSize(new Dimension(180, 40)); // Ustawienie stałej wysokości
        removeButton.setBackground(Color.RED);

        removeButton.addActionListener(e -> {
            paragons_waiting.remove(paragon);
            parentPanel.remove(panel);
            parentPanel.revalidate();
            parentPanel.repaint();

            LocalTime currentTime = LocalTime.now();

            // Ustalenie formatu HH:mm
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            // Sformatowanie czasu do stringa
            String formattedTime = currentTime.format(formatter);


            paragon.setDestroy_time(formattedTime);

            for(ObjectOutputStream out : monitory)
            {
                try {
                    out.reset();
                    out.writeUTF("DELETE_PARAGON"); // Typ danych
                    out.writeObject(paragon.getId()); // Serializacja obiektu
                    out.flush();
                }
                catch (IOException er)
                {
                    er.printStackTrace();
                }
            }

        });
        JButton editButton = new JButton("Edytuj");
        editButton.setFont(new Font("Arial", Font.BOLD, 16));
        //editButton.setPreferredSize(new Dimension(100, 40)); // Ustawienie stałej wysokości
        editButton.setBackground(Color.GREEN);

        editButton.addActionListener(e -> {

            paragons_waiting.remove(paragon);
            paragons_all.remove(paragon);
            parentPanel.remove(panel);
            parentPanel.revalidate();
            parentPanel.repaint();

            for(ObjectOutputStream out : monitory)
            {
                try {
                    out.writeUTF("DELETE_PARAGON"); // Typ danych
                    out.writeObject(paragon.getId()); // Serializacja obiektu
                    out.flush();
                }
                catch (IOException er)
                {
                    er.printStackTrace();
                }
            }

            CancelButtonLogic();
            aktualny = new Paragon(aktualny.getId());

            aktualny.setId(paragon.getId());
            aktualny.setNumer_stolika(paragon.getNumer_stolika());
            aktualny.setCreate_time(paragon.getCreate_time());
            aktualny.setKelner(paragon.getKelner());

            for(ObjectOutputStream out : monitory)
            {
                try {
                    out.reset();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            for(Produkt_na_paragonie p : paragon.getProducts())
            {
                aktualny.addProduct(p);
                addProduct(p);
            }

        });
        // Ustawienie przycisku na dole
        JPanel buttonPanelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // FlowLayout dla przycisku usuwania
        buttonPanelBottom.add(editButton);
        buttonPanelBottom.add(removeButton);
        panel.add(buttonPanelBottom, BorderLayout.SOUTH); // Dodanie panelu z przyciskiem na dół

        return panel;
    }

    public void addParagonFromHistory(Paragon p)
    {
        p.setDestroy_time(null);
        paragons_waiting.add(p);
        paragons_waiting.sort(Comparator.comparingInt(Paragon::getId));
        // wysylka
        for(ObjectOutputStream out : monitory)
        {
            try {
                out.writeUTF("ADD_PARAGON"); // Typ danych
                out.writeObject(p); // Serializacja obiektu
                out.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        mainPanel_paragons.removeAll();

        //na okno obok
        for (Paragon paragon : paragons_waiting) {
            JPanel paragonPanel = createParagonPanel(paragon, mainPanel_paragons);
            mainPanel_paragons.add(paragonPanel);
        }

        mainPanel_paragons.revalidate();
        mainPanel_paragons.repaint();

    }


    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true; // Jeśli parsowanie się powiedzie, zwracamy true
        } catch (NumberFormatException e) {
            return false; // Jeśli wystąpi wyjątek, zwracamy false
        }
    }

    public static void zapiszParagonyDoPliku(List<Paragon> paragony, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Paragon paragon : paragony) {
                writer.write(paragonToString(paragon));
                writer.newLine();  // Przejście do nowej linii po każdym paragonie
            }
        }
    }

    public static String paragonToString(Paragon paragon) {
        StringBuilder sb = new StringBuilder();
        sb.append("Numer stolika: ").append(paragon.getNumer_stolika()).append("\n");
        sb.append("Czas utworzenia: ").append(paragon.getCreate_time()).append("\n");
        sb.append("Czas zamknięcia: ").append(paragon.getDestroy_time()).append("\n");
        sb.append("Zaplacony: ").append(paragon.isZaplacony() ? "Tak" : "Nie").append("\n");
        sb.append("Kelner: ").append(paragon.getKelner()).append("\n");
        sb.append("Produkty:\n");


        for (Produkt_na_paragonie produkt : paragon.getProducts()) {
            sb.append("\tNazwa: ").append(produkt.getNazwa()).append("\n");
            sb.append("\tIlosc: ").append(produkt.getIlosc()).append("\n");
            sb.append("\tNotatka: ").append(produkt.getNotatka()).append("\n");
            sb.append("\tCzas wydania: ").append(produkt.getCzas_wydania() != null ? produkt.getCzas_wydania() : "N/A").append("\n");
            sb.append("\n");
        }
        sb.append("-------------------------------\n");
        return sb.toString();
    }

    public static String generateFileNameWithDateTime() {
        // Pobieramy bieżącą datę i godzinę
        LocalDateTime now = LocalDateTime.now();

        // Formatujemy datę na styl: "yyyyMMdd_HHmmss" (np. 20240929_143500)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm");

        // Zwracamy nazwę pliku z rozszerzeniem .txt
        return "historia\\" + now.format(formatter) + ".txt";
    }

}

