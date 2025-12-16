package com.emma.broadcastserver.handler;

import com.emma.broadcastserver.model.ChatMessage;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BroadcastHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<WebSocketSession, String> sessions = new ConcurrentHashMap<>();
    private final List<ChatMessage> history = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        System.out.println("Nouvelle connexion : " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatMessage msg = mapper.readValue(message.getPayload(), ChatMessage.class);

            if ("JOIN".equalsIgnoreCase(msg.getType())) {
                String pseudo = msg.getSender();
                sessions.put(session, pseudo);

                System.out.println("👤 " + pseudo + " a rejoint le chat");

                // Envoyer l'historique au nouveau membre
                for (ChatMessage oldMsg : history) {
                    String historyMessage = mapper.writeValueAsString(oldMsg);
                    session.sendMessage(new TextMessage(historyMessage));
                }

                // Notifier tout le monde
                broadcast(new ChatMessage("INFO", "Serveur", pseudo + " a rejoint la discussion"));

            } else if ("CHAT".equalsIgnoreCase(msg.getType())) {
                String sender = sessions.get(session);

//                if (sender == null) {
//
//                    session.sendMessage(new TextMessage(
//                            mapper.writeValueAsString(
//                                    new ChatMessage("INFO", "Serveur", "Erreur: JOIN requis d'abord")
//                            )
//                    ));
//                    return;
//                }

                msg.setSender(sender);
                addToHistory(msg);
                broadcast(msg);
            }

        } catch (Exception e) {
            System.err.println(" Erreur traitement message : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws IOException {
        String pseudo = sessions.remove(session);
        if (pseudo != null) {
            System.out.println("👋 " + pseudo + " a quitté le chat");
            broadcast(new ChatMessage("INFO", "Serveur", pseudo + " a quitté la discussion"));
        }
    }

    private void broadcast(ChatMessage msg) throws IOException {
        String jsonMsg = mapper.writeValueAsString(msg);

        //  Iterator pour éviter ConcurrentModificationException
        Iterator<Map.Entry<WebSocketSession, String>> iterator = sessions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<WebSocketSession, String> entry = iterator.next();
            WebSocketSession session = entry.getKey();

            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMsg));
                } else {
                    iterator.remove(); // Nettoie les sessions fermées
                }
            } catch (IOException e) {
                System.err.println(" Erreur envoi à " + entry.getValue() + " : " + e.getMessage());
                iterator.remove();
            }
        }
    }

    private void addToHistory(ChatMessage msg) {
        if (history.size() >= 35) {
            history.removeFirst();
        }
        history.add(msg);
    }
}
