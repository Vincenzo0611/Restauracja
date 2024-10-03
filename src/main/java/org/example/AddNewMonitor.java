package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Enumeration;
import java.util.List;

public class AddNewMonitor {

    private boolean running = true; // Flaga do kontrolowania nasłuchu
    private DatagramSocket socket;

    JFrame okno = new JFrame();
    List<Paragon> paragons_waiting;

    public void start(List<ObjectOutputStream> monitory, List<Paragon> paragons_waiting)
    {
        this.paragons_waiting = paragons_waiting;
        // Ustawienia okna
        okno.setTitle("Broadcast Serwer");
        okno.setSize(300, 150);
        okno.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        okno.setLayout(new FlowLayout());

        // Przycisk "Anuluj"
        JButton cancelButton = new JButton("Anuluj nasłuch");
        okno.add(cancelButton);

        // Obsługa przycisku anulowania
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopListening();
                okno.dispose();
            }
        });

        // Uruchom serwer broadcastu w osobnym wątku
        new Thread(() -> startListening(monitory)).start();
        okno.setVisible(true);
    }

    private void startListening(List<ObjectOutputStream> monitory) {
        try {
            // Nasłuchiwanie na broadcastowe wiadomości na porcie 8888
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);  // Umożliwia odbiór broadcastów

            System.out.println("Serwer gotowy do wykrycia broadcastów...");

            while (running) {
                // Odbieranie broadcastu
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData()).trim();
                System.out.println("Odebrano wiadomość: " + message);

                // Jeśli klient szuka serwera, odpowiedz
                if ("DISCOVER_SERVER".equals(message)) {
                    // Pobieranie adresu IP serwera (lokalny IP)
                    String serverIP = null;
                    try {

                        // Przejrzyj wszystkie interfejsy sieciowe
                        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

                        while (interfaces.hasMoreElements()) {
                            NetworkInterface networkInterface = interfaces.nextElement();

                            // Pomiń interfejsy, które są wyłączone lub nie mają IP
                            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                                continue;
                            }

                            // Przejrzyj wszystkie adresy przypisane do tego interfejsu
                            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                            while (addresses.hasMoreElements()) {
                                InetAddress inetAddress = addresses.nextElement();

                                // Sprawdź czy jest to adres IPv4
                                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                                    serverIP = inetAddress.getHostAddress(); // Zapisz ostatni napotkany adres
                                }
                            }
                        }

                        // Wyświetl ostatni napotkany adres IP
                        if (serverIP != null) {
                            System.out.println("Ostatni adres IP: " + serverIP);
                        } else {
                            System.out.println("Nie znaleziono adresu IP.");
                        }

                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    byte[] sendData = serverIP.getBytes();

                    // Wysyłanie odpowiedzi do klienta
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    System.out.println("Wysłano adres IP serwera: " + serverIP);
                    stopListeningSucces(monitory);
                }
            }

            // Zamknięcie gniazda, jeśli nasłuch zostanie anulowany
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Nasłuch został zatrzymany.");
            }

        } catch (SocketException e) {
            if (!running) {
                System.out.println("Nasłuch został anulowany.");
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopListening() {
        running = false; // Ustawienie flagi, aby zatrzymać pętlę nasłuchu
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Zamknięcie socketu przerywa socket.receive()
        }
    }

    private void stopListeningSucces(List<ObjectOutputStream> monitory)
    {
        okno.dispose();
        running = false; // Ustawienie flagi, aby zatrzymać pętlę nasłuchu
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Zamknięcie socketu przerywa socket.receive()
        }
        try (ServerSocket serverSocket = new ServerSocket(12345)) { // Nasłuchuje na porcie 12345
            System.out.println("Serwer TCP uruchomiony, oczekuje na połączenie...");

            while (true) {
                Socket socket = serverSocket.accept(); // Akceptuje połączenie od klienta
                System.out.println("Klient połączony: " + socket.getInetAddress());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                monitory.add(out);
                for(Paragon p : paragons_waiting)
                {
                    out.writeUTF("ADD_PARAGON"); // Typ danych
                    out.writeObject(p); // Serializacja obiektu
                    out.flush();/*
                    for(Produkt_na_paragonie product : p.getProducts())
                    {
                        if(product.getCzas_wydania() != null)
                        {
                            try {
                                out.writeUTF("DELETE_PRODUCT"); // Typ danych
                                out.writeObject(p); // Serializacja obiektu
                                out.flush();
                            } catch (IOException er) {
                                er.printStackTrace();
                            }
                        }
                    }*/
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
