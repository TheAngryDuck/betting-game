package com.example.betting;

import com.example.betting.dto.Bet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameService gameService;

    public MyWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (sessions.isEmpty()){
            gameService.startGame(sessions);
        }
        sessions.add(session);
        gameService.addPlayer(session.getId());
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Deserialize JSON
        Bet incoming = objectMapper.readValue(message.getPayload(), Bet.class);
        incoming.setId(session.getId());
        System.out.println("Received: " + incoming.getNumber() + " from " + incoming.getId());
        gameService.addBet(incoming);

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("Client disconnected: " + session.getId());

        if (sessions.isEmpty()) {
            gameService.endGame(sessions);
        }
    }
}