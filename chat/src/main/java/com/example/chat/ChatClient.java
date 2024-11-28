package org.example.chat;

import java.io.*;
import java.net.*;

public class ChatClient {
    private static final ConfigReader reader = ConfigReader.getInstance();

    public static void main(String[] args) {
        try (Socket socket = new Socket(reader.getHost(), reader.getPort());
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the chat server");

            System.out.print(serverReader.readLine() + " ");
            String username = consoleReader.readLine();
            writer.println(username);

            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException ex) {
                    System.out.println("Server connection closed: " + ex.getMessage());
                }
            }).start();

            String userInput;
            do {
                System.out.println("Choose: 1) Broadcast 2) Private Message 3) Exit");
                String choice = consoleReader.readLine();
                if (choice.equals("1")) {
                    System.out.print("Enter message: ");
                    userInput = consoleReader.readLine();
                    writer.println(userInput);
                } else if (choice.equals("2")) {
                    System.out.print("Enter recipient username: ");
                    String recipient = consoleReader.readLine();
                    System.out.print("Enter message: ");
                    String privateMessage = consoleReader.readLine();
                    writer.println("@" + recipient + " " + privateMessage);
                } else if (choice.equals("3")) {
                    userInput = "exit";
                    writer.println(userInput);
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            } while (!"exit".equalsIgnoreCase(userInput));

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
