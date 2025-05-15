package com.example.betting;

import com.example.betting.dto.Bet;
import com.example.betting.dto.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
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
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        if (sessions.isEmpty()){
            gameService.startGame(sessions);
        }
        sessions.add(session);
        gameService.addPlayer(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try{
            Bet incoming = objectMapper.readValue(message.getPayload(), Bet.class);
            incoming.setId(session.getId());
            if (validateBet(incoming)) {
                gameService.addBet(incoming);
            }else{
                session.sendMessage(new TextMessage("Invalid bet"));
            }
            if (incoming.getNumber() == 11){
                gameService.sendRtp(sessions, session.getId());
            }
        } catch (JsonProcessingException e) {
            String json = objectMapper.writeValueAsString(new Message("System", "Invalid message content: "+message.getPayload()));
            session.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);

        if (sessions.isEmpty()) {
            gameService.endGame();
        }
    }

    private boolean validateBet(Bet bet) {
        return bet.getNumber() > 0 && bet.getAmount() > 0 && !bet.getId().isEmpty() && bet.getNumber() < 11;
    }
}