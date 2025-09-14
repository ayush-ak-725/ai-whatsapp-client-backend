package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.ai.connector.AIConnector;
import com.bakchodai.whatsapp.ai.model.AIResponse;
import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
import com.bakchodai.whatsapp.service.ConversationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for ConversationEngine
 * 
 * These tests verify the conversation flow, AI response generation,
 * and message persistence in a realistic scenario.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ConversationEngineIntegrationTest {
    
    @Autowired
    private ConversationEngine conversationEngine;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    @MockBean
    private AIConnector mockAIConnector;
    
    private Group testGroup;
    private Character testCharacter1;
    private Character testCharacter2;
    private Character testCharacter3;
    
    @BeforeEach
    void setUp() {
        // Initialize test data
        testDataInitializer.initializeTestData();
        
        // Get test entities
        testGroup = groupRepository.findByNameIgnoreCase("Test Group").orElse(null);
        testCharacter1 = characterRepository.findByNameIgnoreCase("Virat Kohli").orElse(null);
        testCharacter2 = characterRepository.findByNameIgnoreCase("Kapil Sharma").orElse(null);
        testCharacter3 = characterRepository.findByNameIgnoreCase("Elon Musk").orElse(null);
        
        // Setup mock AI connector
        setupMockAIConnector();
    }
    
    @Test
    void testStartConversation_Success() throws Exception {
        // Given
        assertNotNull(testGroup);
        assertNotNull(testCharacter1);
        assertNotNull(testCharacter2);
        assertNotNull(testCharacter3);
        
        // When
        CompletableFuture<Void> conversationFuture = conversationEngine.startConversation(testGroup);
        
        // Wait for conversation to start and process a few turns
        Thread.sleep(2000);
        
        // Then
        assertTrue(conversationEngine.isConversationActive(testGroup.getId()));
        
        // Verify messages were created
        List<Message> messages = messageRepository.findRecentMessagesByGroupIdWithLimit(testGroup.getId(), 10);
        assertFalse(messages.isEmpty());
        
        // Verify messages are from AI characters
        boolean hasAiMessages = messages.stream()
                .anyMatch(Message::getIsAiGenerated);
        assertTrue(hasAiMessages);
    }
    
    @Test
    void testStopConversation_Success() throws Exception {
        // Given
        conversationEngine.startConversation(testGroup);
        Thread.sleep(1000); // Let conversation start
        
        // When
        conversationEngine.stopConversation(testGroup.getId());
        
        // Then
        assertFalse(conversationEngine.isConversationActive(testGroup.getId()));
    }
    
    @Test
    void testProcessConversationTurn_Success() throws Exception {
        // Given
        conversationEngine.startConversation(testGroup);
        Thread.sleep(1000); // Let conversation start
        
        // When
        CompletableFuture<Void> turnFuture = conversationEngine.processConversationTurn(testGroup.getId());
        
        // Wait for turn to complete
        Thread.sleep(2000);
        
        // Then
        List<Message> messages = messageRepository.findRecentMessagesByGroupIdWithLimit(testGroup.getId(), 10);
        assertFalse(messages.isEmpty());
        
        // Verify message content
        Message latestMessage = messages.get(0);
        assertNotNull(latestMessage.getContent());
        assertTrue(latestMessage.getContent().length() > 0);
        assertTrue(latestMessage.getIsAiGenerated());
    }
    
    @Test
    void testConversationWithMultipleCharacters_Success() throws Exception {
        // Given
        conversationEngine.startConversation(testGroup);
        Thread.sleep(3000); // Let multiple turns process
        
        // When
        List<Message> messages = messageRepository.findRecentMessagesByGroupIdWithLimit(testGroup.getId(), 20);
        
        // Then
        assertFalse(messages.isEmpty());
        
        // Verify different characters participated
        long uniqueCharacters = messages.stream()
                .map(Message::getCharacterId)
                .distinct()
                .count();
        assertTrue(uniqueCharacters > 1, "Multiple characters should have participated");
    }
    
    @Test
    void testConversationContextBuilding_Success() throws Exception {
        // Given
        conversationEngine.startConversation(testGroup);
        Thread.sleep(1000);
        
        // When
        var conversationState = conversationEngine.getConversationState(testGroup.getId());
        
        // Then
        assertTrue(conversationState.isPresent());
        var state = conversationState.get();
        assertEquals(testGroup, state.getGroup());
        assertNotNull(state.getCurrentCharacter());
        assertTrue(state.getTurnNumber() > 0);
    }
    
    @Test
    void testConversationMoodDetection_Success() throws Exception {
        // Given
        conversationEngine.startConversation(testGroup);
        Thread.sleep(2000);
        
        // When
        var conversationState = conversationEngine.getConversationState(testGroup.getId());
        
        // Then
        assertTrue(conversationState.isPresent());
        var state = conversationState.get();
        assertNotNull(state.getMood());
    }
    
    @Test
    void testConversationWithEmptyGroup_ShouldNotStart() {
        // Given
        Group emptyGroup = createTestGroup("Empty Group", "Group with no members");
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            conversationEngine.startConversation(emptyGroup);
        });
    }
    
    @Test
    void testConversationWithInactiveGroup_ShouldNotStart() {
        // Given
        testGroup.setIsActive(false);
        groupRepository.save(testGroup);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            conversationEngine.startConversation(testGroup);
        });
    }
    
    @Test
    void testConversationTurnWithNoActiveConversation_ShouldCompleteGracefully() throws Exception {
        // Given
        UUID nonExistentGroupId = UUID.randomUUID();
        
        // When
        CompletableFuture<Void> turnFuture = conversationEngine.processConversationTurn(nonExistentGroupId);
        
        // Then
        assertDoesNotThrow(() -> turnFuture.get());
    }
    
    @Test
    void testConversationEngineWithUnhealthyAIConnector_ShouldUseFallback() throws Exception {
        // Given
        when(mockAIConnector.isHealthy()).thenReturn(CompletableFuture.completedFuture(false));
        
        conversationEngine.startConversation(testGroup);
        Thread.sleep(2000);
        
        // When
        List<Message> messages = messageRepository.findRecentMessagesByGroupIdWithLimit(testGroup.getId(), 10);
        
        // Then
        assertFalse(messages.isEmpty());
        
        // Verify fallback response was used
        Message latestMessage = messages.get(0);
        assertTrue(latestMessage.getContent().contains("trouble thinking") || 
                  latestMessage.getContent().contains("Hmm, let me think"));
    }
    
    @Test
    void testConversationEngineWithAIConnectorError_ShouldUseFallback() throws Exception {
        // Given
        when(mockAIConnector.isHealthy()).thenReturn(CompletableFuture.completedFuture(true));
        when(mockAIConnector.generateResponse(any(ConversationContext.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AI service error")));
        
        conversationEngine.startConversation(testGroup);
        Thread.sleep(2000);
        
        // When
        List<Message> messages = messageRepository.findRecentMessagesByGroupIdWithLimit(testGroup.getId(), 10);
        
        // Then
        assertFalse(messages.isEmpty());
        
        // Verify fallback response was used
        Message latestMessage = messages.get(0);
        assertTrue(latestMessage.getContent().contains("trouble thinking") || 
                  latestMessage.getContent().contains("Hmm, let me think"));
    }
    
    @Test
    void testConversationEngineResponseTimeTracking_Success() throws Exception {
        // Given
        when(mockAIConnector.isHealthy()).thenReturn(CompletableFuture.completedFuture(true));
        when(mockAIConnector.generateResponse(any(ConversationContext.class)))
                .thenReturn(CompletableFuture.completedFuture(createMockAIResponse("Test response", 1500L)));
        
        conversationEngine.startConversation(testGroup);
        Thread.sleep(2000);
        
        // When
        List<Message> messages = messageRepository.findRecentMessagesByGroupIdWithLimit(testGroup.getId(), 10);
        
        // Then
        assertFalse(messages.isEmpty());
        
        // Verify response time was tracked
        Message latestMessage = messages.get(0);
        assertNotNull(latestMessage.getResponseTimeMs());
        assertTrue(latestMessage.getResponseTimeMs() > 0);
    }
    
    // Helper methods
    private void setupMockAIConnector() {
        when(mockAIConnector.getConnectorName()).thenReturn("MockAIConnector");
        when(mockAIConnector.getPriority()).thenReturn(1);
        when(mockAIConnector.isHealthy()).thenReturn(CompletableFuture.completedFuture(true));
        when(mockAIConnector.generateResponse(any(ConversationContext.class)))
                .thenReturn(CompletableFuture.completedFuture(createMockAIResponse("Mock AI response", 1000L)));
    }
    
    private AIResponse createMockAIResponse(String content, Long responseTimeMs) {
        AIResponse response = new AIResponse();
        response.setContent(content);
        response.setConfidence(0.8);
        response.setModelUsed("mock-model");
        response.setResponseTimeMs(responseTimeMs);
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }
    
    private Group createTestGroup(String name, String description) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setIsActive(true);
        group.setCreatedAt(LocalDateTime.now());
        return groupRepository.save(group);
    }
}



