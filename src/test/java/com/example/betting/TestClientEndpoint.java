package com.example.betting;


import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@ClientEndpoint
public class TestClientEndpoint {
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    @OnMessage
    public void onMessage(String message) {
        messages.offer(message);
    }

    public BlockingQueue<String> getMessages() {
        return messages;
    }
}
