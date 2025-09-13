package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
import com.bakchodai.whatsapp.service.ConversationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Performance integration tests
 * 
 * These tests verify the application performance under various load conditions
 * including concurrent operations, large datasets, and stress testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class PerformanceIntegrationTest {
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationEngine conversationEngine;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    @MockBean
    private com.bakchodai.whatsapp.ai.connector.AIConnector mockAIConnector;
    
    @BeforeEach
    void setUp() {
        // Initialize test data
        testDataInitializer.initializeTestData();
        
        // Setup mock AI connector for performance tests
        setupMockAIConnector();
    }
    
    @Test
    void testConcurrentGroupCreation_Performance() throws Exception {
        // Given
        int numberOfThreads = 10;
        int groupsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < groupsPerThread; j++) {
                    Group group = createTestGroup("Concurrent Group " + threadId + "-" + j, "Description");
                    assertNotNull(group.getId());
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then
        assertTrue(totalTime < 10000, "Concurrent group creation should complete within 10 seconds");
        
        // Verify all groups were created
        List<Group> allGroups = groupRepository.findByIsActiveTrue();
        assertTrue(allGroups.size() >= numberOfThreads * groupsPerThread);
        
        executor.shutdown();
    }
    
    @Test
    void testConcurrentCharacterCreation_Performance() throws Exception {
        // Given
        int numberOfThreads = 10;
        int charactersPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < charactersPerThread; j++) {
                    Character character = createTestCharacter("Concurrent Character " + threadId + "-" + j, "Traits", "Prompt");
                    assertNotNull(character.getId());
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then
        assertTrue(totalTime < 10000, "Concurrent character creation should complete within 10 seconds");
        
        // Verify all characters were created
        List<Character> allCharacters = characterRepository.findByIsActiveTrue();
        assertTrue(allCharacters.size() >= numberOfThreads * charactersPerThread);
        
        executor.shutdown();
    }
    
    @Test
    void testLargeDatasetQuery_Performance() throws Exception {
        // Given
        int numberOfGroups = 1000;
        int numberOfCharacters = 500;
        int messagesPerGroup = 100;
        
        // Create large dataset
        createLargeDataset(numberOfGroups, numberOfCharacters, messagesPerGroup);
        
        // When
        long startTime = System.currentTimeMillis();
        
        // Query all groups
        List<Group> allGroups = groupRepository.findByIsActiveTrue();
        
        // Query all characters
        List<Character> allCharacters = characterRepository.findByIsActiveTrue();
        
        // Query messages with pagination
        List<Message> messages = messageRepository.findByGroupIdOrderByTimestampDesc(
                allGroups.get(0).getId(), PageRequest.of(0, 20));
        
        long endTime = System.currentTimeMillis();
        long queryTime = endTime - startTime;
        
        // Then
        assertTrue(queryTime < 5000, "Large dataset queries should complete within 5 seconds");
        assertTrue(allGroups.size() >= numberOfGroups);
        assertTrue(allCharacters.size() >= numberOfCharacters);
        assertTrue(messages.size() <= 20); // Pagination limit
    }
    
    @Test
    void testConcurrentMessageCreation_Performance() throws Exception {
        // Given
        Group group = createTestGroup("Message Performance Group", "Description");
        Character character = createTestCharacter("Message Character", "Traits", "Prompt");
        group.addMember(character);
        groupRepository.save(group);
        
        int numberOfThreads = 5;
        int messagesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    Message message = createTestMessage(group, character, "Message " + threadId + "-" + j);
                    assertNotNull(message.getId());
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then
        assertTrue(totalTime < 15000, "Concurrent message creation should complete within 15 seconds");
        
        // Verify all messages were created
        List<Message> allMessages = messageRepository.findByGroupIdOrderByTimestampDesc(
                group.getId(), PageRequest.of(0, 1000));
        assertTrue(allMessages.size() >= numberOfThreads * messagesPerThread);
        
        executor.shutdown();
    }
    
    @Test
    void testConversationEnginePerformance_UnderLoad() throws Exception {
        // Given
        Group group = createTestGroup("Performance Group", "Description");
        Character character1 = createTestCharacter("Character 1", "Traits 1", "Prompt 1");
        Character character2 = createTestCharacter("Character 2", "Traits 2", "Prompt 2");
        group.addMember(character1);
        group.addMember(character2);
        groupRepository.save(group);
        
        // When
        long startTime = System.currentTimeMillis();
        
        conversationEngine.startConversation(group);
        
        // Let conversation run for a while
        Thread.sleep(5000);
        
        conversationEngine.stopConversation(group.getId());
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then
        assertTrue(totalTime < 10000, "Conversation should complete within 10 seconds");
        
        // Verify messages were created
        List<Message> messages = messageRepository.findByGroupIdOrderByTimestampDesc(
                group.getId(), PageRequest.of(0, 100));
        assertFalse(messages.isEmpty());
    }
    
    @Test
    void testDatabaseConnectionPool_Performance() throws Exception {
        // Given
        int numberOfConcurrentOperations = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentOperations);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfConcurrentOperations; i++) {
            final int operationId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // Perform various database operations
                Group group = createTestGroup("Pool Group " + operationId, "Description");
                Character character = createTestCharacter("Pool Character " + operationId, "Traits", "Prompt");
                group.addMember(character);
                groupRepository.save(group);
                
                // Query operations
                groupRepository.findById(group.getId());
                characterRepository.findById(character.getId());
            }, executor);
            futures.add(future);
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then
        assertTrue(totalTime < 20000, "Database operations should complete within 20 seconds");
        
        executor.shutdown();
    }
    
    @Test
    void testMemoryUsage_UnderLoad() throws Exception {
        // Given
        int numberOfGroups = 100;
        int numberOfCharacters = 200;
        int messagesPerGroup = 50;
        
        // When
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        createLargeDataset(numberOfGroups, numberOfCharacters, messagesPerGroup);
        
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = endMemory - startMemory;
        
        // Then
        // Memory usage should be reasonable (less than 100MB for this test)
        assertTrue(memoryUsed < 100 * 1024 * 1024, "Memory usage should be reasonable");
        
        // Force garbage collection and check memory again
        System.gc();
        Thread.sleep(1000);
        
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long finalMemoryUsed = finalMemory - startMemory;
        
        // Memory should be cleaned up after GC
        assertTrue(finalMemoryUsed < memoryUsed, "Memory should be cleaned up after GC");
    }
    
    // Helper methods
    private void createLargeDataset(int numberOfGroups, int numberOfCharacters, int messagesPerGroup) {
        // Create characters
        List<Character> characters = new ArrayList<>();
        for (int i = 0; i < numberOfCharacters; i++) {
            Character character = createTestCharacter("Character " + i, "Traits " + i, "Prompt " + i);
            characters.add(character);
        }
        
        // Create groups and messages
        for (int i = 0; i < numberOfGroups; i++) {
            Group group = createTestGroup("Group " + i, "Description " + i);
            
            // Add some characters to the group
            for (int j = 0; j < Math.min(5, characters.size()); j++) {
                group.addMember(characters.get(j));
            }
            groupRepository.save(group);
            
            // Create messages for the group
            for (int j = 0; j < messagesPerGroup; j++) {
                Character character = characters.get(j % characters.size());
                createTestMessage(group, character, "Message " + j);
            }
        }
    }
    
    private Group createTestGroup(String name, String description) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setIsActive(true);
        group.setCreatedAt(LocalDateTime.now());
        return groupRepository.save(group);
    }
    
    private Character createTestCharacter(String name, String traits, String systemPrompt) {
        Character character = new Character();
        character.setName(name);
        character.setPersonalityTraits(traits);
        character.setSystemPrompt(systemPrompt);
        character.setSpeakingStyle("casual");
        character.setBackground("Test character");
        character.setIsActive(true);
        character.setCreatedAt(LocalDateTime.now());
        return characterRepository.save(character);
    }
    
    private Message createTestMessage(Group group, Character character, String content) {
        Message message = new Message();
        message.setGroup(group);
        message.setCharacter(character);
        message.setContent(content);
        message.setMessageType(Message.MessageType.TEXT);
        message.setTimestamp(LocalDateTime.now());
        message.setIsAiGenerated(true);
        message.setResponseTimeMs(1000L);
        return messageRepository.save(message);
    }
    
    private void setupMockAIConnector() {
        when(mockAIConnector.getConnectorName()).thenReturn("MockAIConnector");
        when(mockAIConnector.getPriority()).thenReturn(1);
        when(mockAIConnector.isHealthy()).thenReturn(CompletableFuture.completedFuture(true));
        when(mockAIConnector.generateResponse(any(com.bakchodai.whatsapp.ai.model.ConversationContext.class)))
                .thenReturn(CompletableFuture.completedFuture(createMockAIResponse()));
    }
    
    private com.bakchodai.whatsapp.ai.model.AIResponse createMockAIResponse() {
        com.bakchodai.whatsapp.ai.model.AIResponse response = new com.bakchodai.whatsapp.ai.model.AIResponse();
        response.setContent("Mock AI response for performance testing");
        response.setConfidence(0.8);
        response.setModelUsed("mock-model");
        response.setResponseTimeMs(100L);
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }
}

