package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
import com.bakchodai.whatsapp.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebSocket functionality
 * 
 * These tests verify real-time communication, message broadcasting,
 * and WebSocket connection management.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class WebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    private Group testGroup;
    private Character testCharacter;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Initialize test data
        testDataInitializer.initializeTestData();
        
        // Get test entities
        testGroup = groupRepository.findByNameIgnoreCase("Test Group").orElse(null);
        testCharacter = characterRepository.findByNameIgnoreCase("Virat Kohli").orElse(null);
        
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testWebSocketConnection_Success() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        String[] receivedMessage = new String[1];
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                latch.countDown();
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                receivedMessage[0] = (String) message.getPayload();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS), "WebSocket connection should be established");
        assertNotNull(session);
        assertTrue(session.isOpen());
        
        // Cleanup
        session.close();
    }
    
    @Test
    void testWebSocketWelcomeMessage_Success() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        String[] receivedMessage = new String[1];
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // Connection established
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                receivedMessage[0] = (String) message.getPayload();
                latch.countDown();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Welcome message should be received");
        assertNotNull(receivedMessage[0]);
        assertTrue(receivedMessage[0].contains("welcome") || receivedMessage[0].contains("Connected"));
        
        // Cleanup
        session.close();
    }
    
    @Test
    void testWebSocketJoinGroup_Success() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(2); // Welcome + join confirmation
        String[] receivedMessages = new String[2];
        int messageIndex = 0;
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // Send join group message
                String joinMessage = objectMapper.writeValueAsString(new JoinGroupMessage(testGroup.getId().toString()));
                session.sendMessage(new TextMessage(joinMessage));
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                if (messageIndex < receivedMessages.length) {
                    receivedMessages[messageIndex] = (String) message.getPayload();
                    messageIndex++;
                }
                latch.countDown();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Join group messages should be received");
        assertTrue(receivedMessages[1].contains("joined_group") || receivedMessages[1].contains(testGroup.getId().toString()));
        
        // Cleanup
        session.close();
    }
    
    @Test
    void testWebSocketPingPong_Success() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(2); // Welcome + pong
        String[] receivedMessages = new String[2];
        int messageIndex = 0;
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // Send ping message
                String pingMessage = objectMapper.writeValueAsString(new PingMessage());
                session.sendMessage(new TextMessage(pingMessage));
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                if (messageIndex < receivedMessages.length) {
                    receivedMessages[messageIndex] = (String) message.getPayload();
                    messageIndex++;
                }
                latch.countDown();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Ping pong messages should be received");
        assertTrue(receivedMessages[1].contains("pong"));
        
        // Cleanup
        session.close();
    }
    
    @Test
    void testWebSocketBroadcastMessage_Success() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(2); // Welcome + broadcast
        String[] receivedMessages = new String[2];
        int messageIndex = 0;
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // Join group first
                String joinMessage = objectMapper.writeValueAsString(new JoinGroupMessage(testGroup.getId().toString()));
                session.sendMessage(new TextMessage(joinMessage));
                
                // Wait a bit then broadcast a message
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Message testMessage = createTestMessage();
                        webSocketHandler.broadcastToGroup(testGroup.getId(), testMessage);
                    } catch (Exception e) {
                        fail("Error broadcasting message: " + e.getMessage());
                    }
                }).start();
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                if (messageIndex < receivedMessages.length) {
                    receivedMessages[messageIndex] = (String) message.getPayload();
                    messageIndex++;
                }
                latch.countDown();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Broadcast message should be received");
        assertTrue(receivedMessages[1].contains("message") || receivedMessages[1].contains("Test message"));
        
        // Cleanup
        session.close();
    }
    
    @Test
    void testWebSocketConversationStatusBroadcast_Success() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(2); // Welcome + status
        String[] receivedMessages = new String[2];
        int messageIndex = 0;
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // Join group first
                String joinMessage = objectMapper.writeValueAsString(new JoinGroupMessage(testGroup.getId().toString()));
                session.sendMessage(new TextMessage(joinMessage));
                
                // Wait a bit then broadcast conversation status
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        webSocketHandler.broadcastConversationStatus(testGroup.getId(), true);
                    } catch (Exception e) {
                        fail("Error broadcasting conversation status: " + e.getMessage());
                    }
                }).start();
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                if (messageIndex < receivedMessages.length) {
                    receivedMessages[messageIndex] = (String) message.getPayload();
                    messageIndex++;
                }
                latch.countDown();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Conversation status should be received");
        assertTrue(receivedMessages[1].contains("conversation_status") || receivedMessages[1].contains("isActive"));
        
        // Cleanup
        session.close();
    }
    
    @Test
    void testWebSocketInvalidMessage_ShouldHandleGracefully() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(2); // Welcome + error
        String[] receivedMessages = new String[2];
        int messageIndex = 0;
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        String uri = "ws://localhost:" + port + "/ws";
        
        // When
        WebSocketSession session = client.doHandshake(new org.springframework.web.socket.WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // Send invalid message
                session.sendMessage(new TextMessage("invalid json message"));
            }
            
            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                if (messageIndex < receivedMessages.length) {
                    receivedMessages[messageIndex] = (String) message.getPayload();
                    messageIndex++;
                }
                latch.countDown();
            }
            
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                fail("WebSocket transport error: " + exception.getMessage());
            }
            
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                // Connection closed
            }
            
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, null, URI.create(uri)).get();
        
        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Error message should be received");
        assertTrue(receivedMessages[1].contains("error") || receivedMessages[1].contains("Invalid"));
        
        // Cleanup
        session.close();
    }
    
    // Helper methods
    private Message createTestMessage() {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setGroup(testGroup);
        message.setCharacter(testCharacter);
        message.setContent("Test message");
        message.setMessageType(Message.MessageType.TEXT);
        message.setTimestamp(LocalDateTime.now());
        message.setIsAiGenerated(true);
        message.setResponseTimeMs(1000L);
        return message;
    }
    
    // Message DTOs
    private static class JoinGroupMessage {
        public String type = "join_group";
        public String groupId;
        
        public JoinGroupMessage(String groupId) {
            this.groupId = groupId;
        }
    }
    
    private static class PingMessage {
        public String type = "ping";
    }
}

