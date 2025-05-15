package com.example.betting;

import com.example.betting.dto.Bet;
import com.example.betting.dto.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BettingIntegrationTests {


    @LocalServerPort
    private int port;

    private WebSocketContainer container;
    private String wsUri;
    private TestClientEndpoint client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        container = ContainerProvider.getWebSocketContainer();
        wsUri = "ws://localhost:" + port + "/ws";
        client = new TestClientEndpoint();
    }

    @Test
    public void testPlayerGetsWelcomeMessageAfterConnectingToGame() throws Exception {

        Session session = connect(client);

        session.getAsyncRemote().sendText(getHelloMessage());

        String response = client.getMessages().poll(3, TimeUnit.SECONDS);

        assertNotNull(response, "No response received from server");
        assertTrue(response.contains("Welcome"));

        session.close();
    }

    @Test
    public void testIncorrectBetTest() throws Exception {

        Session session = connect(client);

        session.getAsyncRemote().sendText(getBet(13));

        client.getMessages().poll(3, TimeUnit.SECONDS);
        String response = client.getMessages().poll(11, TimeUnit.SECONDS);

        assertNotNull(response, "No response received from server");
        assertTrue(response.contains("Invalid bet"));

        session.close();
    }

    @Test
    public void testCorrectBetReturnsSystemMessage() throws Exception {

        Session session = connect(client);
        session.getAsyncRemote().sendText(getBet(7));
        client.getMessages().poll(5, TimeUnit.SECONDS);

        String message2 = client.getMessages().poll(11, TimeUnit.SECONDS);

        assertNotNull(message2);
        assertTrue(message2.contains("You have"));

        session.close();
    }

    private String getHelloMessage() throws JsonProcessingException {
        return objectMapper.writeValueAsString(new Message("Test","Hello"));
    }

    private String getBet(int number) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new Bet("id",number, 100.0));
    }

    private Session connect(TestClientEndpoint client) throws Exception {
        return container.connectToServer(client, URI.create(wsUri));
    }
}
