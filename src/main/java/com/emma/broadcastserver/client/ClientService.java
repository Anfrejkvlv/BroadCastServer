package com.emma.broadcastserver.client;

import com.emma.broadcastserver.model.ChatMessage;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ClientService {

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean connected = false;

    public void startClient(String url) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.print("Entrez votre pseudo: ");
        String pseudo = scanner.nextLine();

        StandardWebSocketClient client = new StandardWebSocketClient();

        CountDownLatch connectionLatch = new CountDownLatch(1);
        AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();

        try {
            client.execute(new TextWebSocketHandler() {

                @Override
                public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
                    System.out.println(" Connexion établie avec le serveur!");
                    sessionRef.set(session);
                    connected = true;
                    connectionLatch.countDown();
                }

                @Override
                protected void handleTextMessage(@NonNull WebSocketSession webSocketSession,
                                                 @NonNull TextMessage message) {
                    try {
                        ChatMessage chatMessage = mapper.readValue(message.getPayload(), ChatMessage.class);

                        // Affichage selon le type
                        if ("INFO".equalsIgnoreCase(chatMessage.getType())) {
                            System.out.println("\n[ " + chatMessage.getSender() + "] : "
                                    + chatMessage.getContent());
                        } else {
                            System.out.println("\n[" + chatMessage.getSender() + "] : "
                                    + chatMessage.getContent());
                        }
                        System.out.print(" > ");

                    } catch (Exception e) {
                        System.out.println("\n Message brut : " + message.getPayload());
                        System.out.print(" > ");
                    }
                }

                @Override
                public void handleTransportError(@NonNull WebSocketSession session,
                                                 @NonNull Throwable exception) {
                    System.err.println("\n Erreur WebSocket : " + exception.getMessage());
                    connected = false;
                }

                @Override
                public void afterConnectionClosed(@NonNull WebSocketSession session,
                                                  @NonNull CloseStatus status) {
                    System.out.println("\n Déconnecté du serveur : " + status);
                    connected = false;
                }

            }, url);

            if (!connectionLatch.await(10, TimeUnit.SECONDS)) {
                throw new TimeoutException("Timeout lors de la connexion au serveur");
            }

            WebSocketSession session = sessionRef.get();

            if (session == null || !session.isOpen()) {
                throw new IllegalStateException("Impossible d'établir la connexion");
            }

            //  Envoyer le message JOIN après confirmation de connexion
            ChatMessage joinMessage = new ChatMessage("JOIN", pseudo, null);
            session.sendMessage(new TextMessage(mapper.writeValueAsString(joinMessage)));

            System.out.println(" Connecté ! Tapez vos messages (tapez 'exit' pour quitter)...");
            System.out.print(" > ");

            // Boucle de lecture des messages
            while (connected && session.isOpen()) {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();

                    if ("exit".equalsIgnoreCase(input.trim())) {
                        System.out.println(" Déconnexion...");
                        session.close(CloseStatus.NORMAL);
                        break;
                    }

                    if (!input.trim().isEmpty() && session.isOpen()) {
                        ChatMessage chatMessage = new ChatMessage("CHAT", pseudo, input);
                        String jsonResponse = mapper.writeValueAsString(chatMessage);
                        session.sendMessage(new TextMessage(jsonResponse));
                        System.out.print(" > ");
                    } else if (!session.isOpen()) {
                        System.out.println(" Session fermée, impossible d'envoyer le message");
                        break;
                    }
                }

                // Pause pour éviter de surcharger le CPU
                Thread.sleep(50);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(" Connexion interrompue : " + e.getMessage());
        } catch (TimeoutException e) {
            System.err.println(" Timeout : Le serveur ne répond pas");
        } catch (IOException e) {
            System.err.println(" Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            System.out.println("\n Au revoir !");
        }
    }
}