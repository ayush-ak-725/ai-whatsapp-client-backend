package com.bakchodai.whatsapp.websocket;

import com.bakchodai.whatsapp.domain.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat communication
 *
 * Minimal raw WebSocket implementation that:
 * - Sends a welcome message on connect
 * - Allows clients to join a group via {"type":"join_group","groupId":"..."}
 * - Responds to {"type":"ping"} with {"type":"pong"}
 * - Broadcasts messages to sessions joined to the specified group
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Maps groupId -> sessions
    private final Map<UUID, Set<WebSocketSession>> groupIdToSessions = new ConcurrentHashMap<>();
    // Maps sessionId -> set of groupIds
    private final Map<String, Set<UUID>> sessionIdToGroupIds = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connected: {}", session.getId());
        session.sendMessage(new TextMessage("{\"type\":\"welcome\",\"message\":\"Connected\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText("");
            switch (type) {
                case "join_group" -> handleJoinGroup(session, root);
                case "ping" -> session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                default -> session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Invalid message type\"}"));
            }
        } catch (Exception ex) {
            logger.warn("Invalid message from {}: {}", session.getId(), ex.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Invalid message format\"}"));
        }
    }

    private void handleJoinGroup(WebSocketSession session, JsonNode root) throws IOException {
        String groupIdStr = root.path("groupId").asText(null);
        if (groupIdStr == null) {
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"groupId is required\"}"));
            return;
        }

        UUID groupId = UUID.fromString(groupIdStr);

        groupIdToSessions.computeIfAbsent(groupId, key -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
        sessionIdToGroupIds.computeIfAbsent(session.getId(), key -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(groupId);

        session.sendMessage(new TextMessage("{\"type\":\"joined_group\",\"groupId\":\"" + groupId + "\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket closed: {} - {}", session.getId(), status);
        removeSessionFromAllGroups(session);
    }

    private void removeSessionFromAllGroups(WebSocketSession session) {
        Set<UUID> groupIds = sessionIdToGroupIds.remove(session.getId());
        if (groupIds == null) {
            return;
        }
        for (UUID groupId : groupIds) {
            Set<WebSocketSession> sessions = groupIdToSessions.get(groupId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    groupIdToSessions.remove(groupId);
                }
            }
        }
    }

    /**
     * Broadcast a message to all connected clients in a group
     */
    public void broadcastToGroup(UUID groupId, Message message) {
        try {
            JsonNode payload = objectMapper.createObjectNode()
                    .put("type", "message")
                    .put("groupId", groupId.toString())
                    .put("characterId", message.getCharacterId() != null ? message.getCharacterId().toString() : null)
                    .put("content", message.getContent());
            String json = objectMapper.writeValueAsString(payload);
            sendToGroup(groupId, json);
        } catch (Exception e) {
            logger.error("Error broadcasting message to group {}: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast conversation status to all connected clients
     */
    public void broadcastConversationStatus(UUID groupId, boolean isActive) {
        try {
            String json = "{\"type\":\"conversation_status\",\"groupId\":\"" + groupId + "\",\"isActive\":" + isActive + "}";
            sendToGroup(groupId, json);
        } catch (Exception e) {
            logger.error("Error broadcasting conversation status for group {}: {}", groupId, e.getMessage(), e);
        }
    }

    private void sendToGroup(UUID groupId, String json) {
        Set<WebSocketSession> sessions = groupIdToSessions.get(groupId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                logger.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    /**
     * Get the number of active sessions
     */
    public int getActiveSessionCount() {
        return sessionIdToGroupIds.size();
    }
}