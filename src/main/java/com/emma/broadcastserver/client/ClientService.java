package com.emma.broadcastserver.client;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

@Service
public class ClientService {

    public void startClient(String url) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        Scanner scanner = null;

        try {
            WebSocketSession session = client.execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(@NonNull WebSocketSession webSocketSession,@NonNull TextMessage message) {
                    System.out.println("\n[BroadCast Server] : " + message.getPayload());
                    System.out.print(" > ");
                }

                @Override
                public void handleTransportError(@NonNull WebSocketSession session,@NonNull Throwable exception) {
                    System.err.println("Erreur WebSocket : " + exception.getMessage());
                }
            }, url).get();

            System.out.println("Connecté au serveur! Tapez vos messages:");
            System.out.print(" > ");

            scanner = new Scanner(System.in);

            while (session.isOpen()) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    session.close();
                    break;
                }
                session.sendMessage(new TextMessage(message));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[BroadCast Server] Connexion interrompue : " + e.getMessage());
        } catch (ExecutionException | IOException exception) {
            System.err.println("[BroadCast Server] Erreur de connexion : " + exception.getMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}