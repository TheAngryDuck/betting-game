package com.example.betting;

import com.example.betting.dto.Bet;
import com.example.betting.dto.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BettingApplicationTests {

    private GameService gameService;
    private WebSocketSession session;

    @BeforeEach
    void setUp() throws Exception {

        gameService = new GameService(new Random() {
            @Override
            public int nextInt(int origin, int bound) {
                return 5;
            }
        });

        session = MockWebSocketSessionFactory.create("ses1");
        gameService.addPlayer(session);
    }

    @Test
    void testAddPlayerAddsPlayerToList() {

        List<Player> players = gameService.getPlayers();
        assertEquals(1, players.size());
        assertEquals("ses", players.getFirst().getName());
    }

    @Test
    void testAddBetLowersPlayersBalance() {
        Bet bet = new Bet("ses1", 5, 100);
        gameService.addBet(bet);
        Player player = gameService.getPlayers().getFirst();

        assertEquals(900, player.getBalance());
    }

    @Test
    void testRunGameCycle_shouldApplyWinningsToPlayer() {
        Bet bet = new Bet("ses1", 5, 100);
        gameService.addBet(bet);
        gameService.runGameCycle(Set.of(session));

        List<Player> players = gameService.getPlayers();
        Player player = players.getFirst();

        assertTrue(player.getBalance() > 1000);
        assertEquals(99.0, player.getRtp());
    }

    @Test
    void testBetIsClearedAfterCycle() {
        gameService.runGameCycle(Set.of(session));

        List<Bet> remainingBets = gameService.getBets();
        assertTrue(remainingBets.isEmpty(), "Bets should be cleared after game cycle");
    }
}
