package com.example.serv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatServer {
    private static final ConfigReader configReader = ConfigReader.getInstance();

    static Set<ClientHandler> clientHandlers = new CopyOnWriteArraySet<>();

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(configReader.getPort())) {
            logger.info("Server started, waiting for clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException ex) {
            logger.error("Server error: {}", ex.getMessage());
        }
    }

    static void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != excludeUser) {
                clientHandler.sendMessage(message);
            }
        }
    }

    static void sendMessageToUser(String message, String username) {
        Optional<ClientHandler> recipient = clientHandlers.stream()
                .filter(client -> client.getUsername().equals(username))
                .findFirst();
        if (recipient.isPresent()) {
            recipient.get().sendMessage(message);
        } else {
            logger.warn("User {} not found. Message not delivered.", username);
        }
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private String username;

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            this.writer = writer;
            writer.println("Enter your username:");
            this.username = reader.readLine().trim();
            logger.info("Client connected with username: {}", username);

            writer.println("Welcome to the chat, " + username + "!");

            String text;
            do {
                text = reader.readLine();
                if (text.equalsIgnoreCase("/list")) {
                    writer.println("Connected users: " + ChatServer.clientHandlers.stream()
                            .map(ClientHandler::getUsername)
                            .filter(name -> !name.equals(username))
                            .toList());
                } else if (text.startsWith("@")) {
                    int spaceIndex = text.indexOf(' ');
                    if (spaceIndex == -1) {
                        writer.println("Invalid message format. Use @<username> <message>");
                        continue;
                    }
                    String recipient = text.substring(1, spaceIndex);
                    String message = text.substring(spaceIndex + 1);

                    logger.info("Private message from {} to {}: {}", username, recipient, message);
                    ChatServer.sendMessageToUser(username + " (private): " + message, recipient);
                } else {
                    logger.info("Broadcast message from {}: {}", username, text);
                    ChatServer.broadcast(username + ": " + text, this);
                }
            } while (!text.equalsIgnoreCase("exit"));

            socket.close();
        } catch (IOException ex) {
            logger.error("Error handling client {}: {}", username, ex.getMessage());
        } finally {
            ChatServer.clientHandlers.remove(this);
            ChatServer.broadcast(username + " has left the chat.", this);
            logger.info("Client {} disconnected", username);
        }
    }

    void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    String getUsername() {
        return username;
    }
}
