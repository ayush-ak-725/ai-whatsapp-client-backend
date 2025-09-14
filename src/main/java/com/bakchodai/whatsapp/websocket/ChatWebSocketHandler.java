package com.bakchodai.whatsapp.websocket;

import com.bakchodai.whatsapp.domain.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat communication
 *
 * This handler manages WebSocket connections and broadcasts messages
 * to connected clients in real-time.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map of groupId -> connected WebSocket sessions
    private final Map<UUID, Set<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("New WebSocket connection: {}", session.getId());
        // By default, don’t assign to a group until client sends groupId
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Received message from {}: {}", session.getId(), message.getPayload());

        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) payload.get("action");

            if ("ping".equals(action)) {
                // Handle ping messages
                logger.debug("Received ping from session: {}", session.getId());
                return;
            }

            if (payload.containsKey("groupId")) {
                String groupIdStr = (String) payload.get("groupId");
                if (groupIdStr != null && !groupIdStr.trim().isEmpty()) {
                    try {
                        UUID groupId = UUID.fromString(groupIdStr);
                        
                        // Register session in group
                        groupSessions.computeIfAbsent(groupId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);

                        // Build a Message object from JSON if needed
                        if (payload.containsKey("content")) {
                            Message chatMessage = new Message();
                            chatMessage.setContent((String) payload.get("content"));
                            broadcastToGroup(groupId, chatMessage);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid UUID string received: '{}'", groupIdStr);
                    }
                } else {
                    logger.warn("Empty or null groupId received from session: {}", session.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing incoming message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("WebSocket closed: {}", session.getId());
        // Remove from all groups
        groupSessions.values().forEach(sessions -> sessions.remove(session));
    }

    /**
     * Broadcast a message to all connected clients in a group
     */
    public void broadcastToGroup(UUID groupId, Message message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            logger.info("Broadcasting message to group {}: {}", groupId, message.getContent());

            Set<WebSocketSession> sessions = groupSessions.getOrDefault(groupId, Collections.emptySet());
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(messageJson));
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting message to group {}: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast conversation status to all connected clients
     */
    public void broadcastConversationStatus(UUID groupId, boolean isActive) {
        try {
            String statusMessage = String.format(
                "{\"type\":\"conversation_status\",\"groupId\":\"%s\",\"isActive\":%s}",
                groupId, isActive
            );
            logger.info("Broadcasting conversation status for group {}: {}", groupId, isActive);

            Set<WebSocketSession> sessions = groupSessions.getOrDefault(groupId, Collections.emptySet());
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(statusMessage));
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting conversation status for group {}: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * Get the number of active sessions
     */
    public int getActiveSessionCount() {
        return groupSessions.values().stream().mapToInt(Set::size).sum();
    }
}
