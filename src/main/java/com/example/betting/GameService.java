package com.example.betting;

import com.example.betting.dto.Bet;
import com.example.betting.dto.Message;
import com.example.betting.dto.Player;
import com.example.betting.dto.Winner;
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
    private final Random random;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> gameLoop;

    public GameService() {
        this.random = new Random();
    }

    public GameService(Random random) {
        this.random = random;
    }

    public void startGame(Set<WebSocketSession> sessions) {
        if (!gameRunning) {
            gameRunning = true;
            startGameCycle(sessions);
            broadcastSystemMessage(sessions, "Game started!");
        }
    }

    public void endGame() {
        if (gameRunning) {
            gameRunning = false;
            stopGameLoop();
            players.clear();
        }
    }

    public void addPlayer(WebSocketSession session) throws IOException {
        Player player = new Player(session.getId().substring(0, 3),session.getId(),0,1000,1000,0.0);
        players.add(player);
        String json = objectMapper.writeValueAsString(new Message("System", "Welcome to the game!"));
        session.sendMessage(new TextMessage(json));
    }

    public void addBet(Bet bet) {
        if (!bets.contains(bet)) {
            Player player = players.stream().filter(p -> p.getId()
                    .equals(bet.getId())).findFirst().orElse(null);
            if (player != null) {
                bets.add(bet);
                player.setBalance(player.getBalance() - bet.getAmount());
                updatePlayer(player);
            }
        }
    }

    public void sendRtp(Set<WebSocketSession> sessions, String id){
        players.stream().filter(p -> p.getId().equals(id))
                .findFirst().ifPresent(player -> sendMessage(sessions, id, String.valueOf(player.getRtp())));
    }

    // For testing
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    // For testing
    public List<Bet> getBets() {
        return List.copyOf(bets);
    }

    private void startGameCycle(Set<WebSocketSession> sessions){
        gameLoop = scheduler.scheduleAtFixedRate(() -> {
            if (!gameRunning) return;
            runGameCycle(sessions);
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void runGameCycle(Set<WebSocketSession> sessions) {
        try {
            List<Winner> winners = findWinners(sessions);
            if (!winners.isEmpty()) {
                broadcastSystemMessage(sessions, objectMapper.writeValueAsString(winners));
            }
            bets.clear();
        } catch (Exception e) {
            System.err.println("Game cycle error: " + e.getMessage());
        }
    }

    private List<Winner> findWinners(Set<WebSocketSession> sessions) {
        int winningNumber = random.nextInt(1, 11);

        List<Winner> winners = new ArrayList<>();

        for (Bet bet : new ArrayList<>(bets)) {
            Player player = getPlayerById(bet.getId());

            if (player == null) continue;

            if (bet.getNumber() == winningNumber) {
                double winnings = bet.getAmount() * 9.9;
                applyWinnings(player, winnings);
                winners.add(new Winner(player.getName(), winnings));
                sendMessage(sessions, player.getId(), "You have won: " + winnings + "!");
            } else {
                sendMessage(sessions, player.getId(), "You have lost.");
            }
        }

        return winners;
    }

    private Player getPlayerById(String id) {
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void applyWinnings(Player player, double winnings) {
        player.setWinnings(player.getWinnings() + winnings);
        player.setBalance(player.getBalance() + winnings);
        player.setRtp(player.getWinnings() / player.getOriginalBalance() * 100);
        updatePlayer(player);
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
