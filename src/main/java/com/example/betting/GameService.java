package com.example.betting;

import com.example.betting.dto.Bet;
import com.example.betting.dto.Message;
import com.example.betting.dto.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class GameService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean gameRunning = false;
    private final List<Player> players = new ArrayList<>();
    private final List<Bet> bets = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> gameLoop;

    public void startGame(Set<WebSocketSession> sessions) {
        if (!gameRunning) {
            gameRunning = true;
            startGameCycle(sessions);
            System.out.println("Game started");
            broadcastSystemMessage(sessions, "Game started!");
        }
    }

    public void endGame(Set<WebSocketSession> sessions) {
        if (gameRunning) {
            gameRunning = false;
            stopGameLoop();
            System.out.println("Game ended");
            players.clear();
            broadcastSystemMessage(sessions, "Game ended!");
        }
    }

    public void addPlayer(String id) {
        System.out.println("Player added: " + id);
        Player player = new Player(id.substring(0, 3),id,0,1000,1000,0.0);

        players.add(player);
    }

    public void addBet(Bet bet) {
        if (!bets.contains(bet)) {
            Player player = players.stream().filter(p -> p.getId()
                    .equals(bet.getId())).findFirst().orElse(null);
            if (player != null) {
                bets.add(bet);
                player.setBalance(player.getBalance() - bet.getAmount());
                updatePlayer(player);
                System.out.println("New balance: " + player.getBalance());
            }
        }
    }

    private void startGameCycle(Set<WebSocketSession> sessions){
        gameLoop = scheduler.scheduleAtFixedRate(() -> {
            if (!gameRunning) return;
            try {
                int number = new Random().nextInt(1,11);
                System.out.println(number);
                List<String> winners = findWinners(number, sessions);
                System.out.println("🎯 Game tick: doing something...");
                if (!winners.isEmpty()) {
                    broadcastSystemMessage(sessions,winners.toString());
                }
                bets.clear();
            } catch (Exception e) {
                System.err.println("Game tick error: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private List<String> findWinners(int number, Set<WebSocketSession> sessions){
        List<String> winners = new ArrayList<>();
        for (Bet bet : bets) {
            if (bet.getNumber() == number) {
                Player player = players.stream().filter(p -> p.getId()
                        .equals(bet.getId())).findFirst().orElse(null);
                if (player != null) {
                    double winnings = bet.getAmount()* 9.9;
                    double newBalance = player.getBalance() + winnings;

                    player.setWinnings(player.getWinnings() + winnings);
                    player.setBalance(newBalance);
                    player.setRtp(player.getWinnings() / player.getOriginalBalance());
                    updatePlayer(player);
                    winners.add(player.getName());
                    sendMessage(sessions, player.getId(), "You have won: " + winnings + "!");
                }
            }else {
                sendMessage(sessions, bet.getId(), "You have lost.");
            }
        }
        return winners;
    }

    private void updatePlayer(Player player) {
        int index = IntStream.range(0, players.size())
                .filter(i -> players.get(i).getId().equals(player.getId()))
                .findFirst()
                .orElse(-1);

        if (index != -1) {
            players.set(index, player);
        }
    }

    private void stopGameLoop() {
        if (gameLoop != null && !gameLoop.isCancelled()) {
            gameLoop.cancel(true);
        }
    }

    private void sendMessage(Set<WebSocketSession> sessions ,String sessionId, String message) {
        sessions.stream().filter(p -> p.getId().equals(sessionId))
                .findFirst().ifPresent(p -> {
            if (p.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(new Message("System", message));
                    p.sendMessage(new TextMessage(json));
                    System.out.println(p.getId() + " "+message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void broadcastSystemMessage(Set<WebSocketSession> sessions, String content) {
        try {
            String json = objectMapper.writeValueAsString(new Message("System", content));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send system message: " + e.getMessage());
        }
    }
}
