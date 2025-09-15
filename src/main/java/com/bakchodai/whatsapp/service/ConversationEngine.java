package com.bakchodai.whatsapp.service;

import com.bakchodai.whatsapp.ai.connector.AIConnector;
import com.bakchodai.whatsapp.ai.model.AIResponse;
import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for managing conversation flow and AI response generation
 * 
 * This service implements advanced conversation management including:
 * - Turn-based conversation with intelligent interruption handling
 * - Context-aware response generation
 * - Conversation mood detection and adaptation
 * - Burst reply handling for natural conversation flow
 */
@Service
public class ConversationEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationEngine.class);
    private static final int MAX_RECENT_MESSAGES = 20;
    private static final int MAX_RESPONSE_TIME_MS = 30000;
    
    private final List<AIConnector> aiConnectors;
    private final MessageRepository messageRepository;
    private final Map<UUID, ConversationState> activeConversations = new ConcurrentHashMap<>();
    private final AtomicInteger conversationCounter = new AtomicInteger(0);
    
    @Autowired
    public ConversationEngine(List<AIConnector> aiConnectors, MessageRepository messageRepository) {
        this.aiConnectors = aiConnectors.stream()
                .sorted(Comparator.comparing(AIConnector::getPriority))
                .toList();
        this.messageRepository = messageRepository;
        logger.info("Initialized ConversationEngine with {} AI connectors", aiConnectors.size());
    }
    
    /**
     * Start a conversation in a group
     * 
     * @param group The group to start conversation in
     * @return CompletableFuture that completes when conversation starts
     */
    @Async
    public CompletableFuture<Void> startConversation(Group group) {
        logger.info("Starting conversation in group: {}", group.getName());
        
        ConversationState state = new ConversationState(group);
        activeConversations.put(group.getId(), state);
        
        // Start the conversation loop
        return processConversationTurn(group.getId());
    }
    
    /**
     * Stop a conversation in a group
     * 
     * @param groupId The group ID to stop conversation in
     */
    public void stopConversation(UUID groupId) {
        logger.info("Stopping conversation in group: {}", groupId);
        ConversationState state = activeConversations.remove(groupId);
        if (state != null) {
            state.setActive(false);
        }
    }
    
    /**
     * Process a single conversation turn
     * 
     * @param groupId The group ID to process
     * @return CompletableFuture that completes when turn is processed
     */
    @Async
    public CompletableFuture<Void> processConversationTurn(UUID groupId) {
        logger.info("Processing conversation turn for group: {}", groupId);
        ConversationState state = activeConversations.get(groupId);
        if (state == null || !state.isActive()) {
            logger.debug("No active conversation found for group: {}", groupId);
            return CompletableFuture.completedFuture(null);
        }
        
        Group group = state.getGroup();
        Character currentCharacter = state.getCurrentCharacter();
        logger.info("Current character for turn {}: {} (group: {})", 
                   state.getTurnNumber(), currentCharacter.getName(), group.getName());
        
        try {
            // Build conversation context
            ConversationContext context = buildConversationContext(group, currentCharacter, state);
            
            // Generate AI response
            CompletableFuture<AIResponse> responseFuture = generateAIResponse(context);
            
            return responseFuture.thenCompose(response -> {
                logger.info("Received AI response for group {}: content='{}', valid={}", 
                           groupId, response != null ? response.getContent() : "null", 
                           response != null ? response.isValid() : false);
                
                if (response != null && response.isValid()) {
                    return saveAndProcessResponse(group, currentCharacter, response, state);
                } else {
                    logger.warn("Invalid AI response received for group: {} - response: {}", groupId, response);
                    return CompletableFuture.completedFuture(null);
                }
            }).thenCompose(v -> {
                // Schedule next turn if conversation is still active
                if (state.isActive()) {
                    return scheduleNextTurn(groupId, state);
                }
                return CompletableFuture.completedFuture(null);
            });
            
        } catch (Exception e) {
            logger.error("Error processing conversation turn for group: {}", groupId, e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Build conversation context for AI response generation
     */
    private ConversationContext buildConversationContext(Group group, Character character, ConversationState state) {
        ConversationContext context = new ConversationContext();
        context.setGroup(group);
        context.setCurrentCharacter(character);
        context.setActiveCharacters(new ArrayList<>(group.getMembers().stream()
                .map(member -> member.getCharacter())
                .toList()));
        context.setConversationStartTime(state.getStartTime());
        context.setCurrentTopic(state.getCurrentTopic());
        context.setMood(state.getMood());
        
        // Get recent messages for context
        List<Message> recentMessages = messageRepository.findRecentMessagesByGroupId(
                group.getId(), PageRequest.of(0, MAX_RECENT_MESSAGES));
        context.setRecentMessages(recentMessages);
        
        // Add additional context
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("turnNumber", state.getTurnNumber());
        additionalContext.put("characterCount", group.getMembers().size());
        additionalContext.put("conversationDuration", 
                java.time.Duration.between(state.getStartTime(), LocalDateTime.now()).toMinutes());
        context.setAdditionalContext(additionalContext);
        
        return context;
    }
    
    /**
     * Generate AI response using available connectors
     */
    private CompletableFuture<AIResponse> generateAIResponse(ConversationContext context) {
        logger.debug("Attempting to generate AI response with {} connectors", aiConnectors.size());
        
        for (AIConnector connector : aiConnectors) {
            try {
                logger.debug("Checking health of connector: {}", connector.getConnectorName());
                CompletableFuture<Boolean> healthCheck = connector.isHealthy();
                Boolean isHealthy = healthCheck.get(5, TimeUnit.SECONDS);
                
                if (Boolean.TRUE.equals(isHealthy)) {
                    logger.info("Using healthy AI connector: {}", connector.getConnectorName());
                    return connector.generateResponse(context)
                            .exceptionally(throwable -> {
                                logger.error("Error generating response from connector {}: {}", 
                                           connector.getConnectorName(), throwable.getMessage());
                                return createFallbackResponse();
                            });
                } else {
                    logger.warn("AI connector {} is not healthy", connector.getConnectorName());
                }
            } catch (Exception e) {
                logger.warn("AI connector {} health check failed: {}", connector.getConnectorName(), e.getMessage());
            }
        }
        
        logger.warn("No healthy AI connectors available, using fallback response");
        return CompletableFuture.completedFuture(createFallbackResponse());
    }
    
    /**
     * Save AI response and process it
     */
    private CompletableFuture<Void> saveAndProcessResponse(Group group, Character character, AIResponse response, ConversationState state) {
        logger.info("Saving AI response from {}: '{}'", character.getName(), response.getContent());
        
        Message message = new Message(group, character, response.getContent(), response.getMessageType());
        message.setIsAiGenerated(true);
        message.setResponseTimeMs(response.getResponseTimeMs());
        
        Message savedMessage = messageRepository.save(message);
        logger.info("Saved message with ID: {} for group: {}", savedMessage.getId(), group.getId());
        
        // Update conversation state
        state.incrementTurnNumber();
        state.setLastMessageTime(LocalDateTime.now());
        
        // Update conversation mood based on response
        updateConversationMood(state, response);
        
        logger.info("Successfully processed AI response from {}: {}", character.getName(), response.getTruncatedContent(50));
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Schedule the next conversation turn
     */
    private CompletableFuture<Void> scheduleNextTurn(UUID groupId, ConversationState state) {
        // Calculate delay based on conversation state and character personality
        long delayMs = calculateTurnDelay(state);
        
        logger.info("Scheduling next turn for group {} in {}ms (turn number: {})", 
                   groupId, delayMs, state.getTurnNumber());
        
        CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (activeConversations.containsKey(groupId)) {
                        logger.info("Executing scheduled turn for group {} (turn number: {})", 
                                   groupId, state.getTurnNumber());
                        processConversationTurn(groupId);
                    } else {
                        logger.warn("Conversation {} no longer active, skipping scheduled turn", groupId);
                    }
                });
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Calculate delay for next turn based on conversation state
     */
    private long calculateTurnDelay(ConversationState state) {
        // Base delay between 2-8 seconds
        long baseDelay = 2000 + (long) (Math.random() * 6000);
        
        // Adjust based on conversation mood
        switch (state.getMood()) {
            case EXCITED:
                return baseDelay / 2; // Faster responses when excited
            case CALM:
                return baseDelay * 2; // Slower responses when calm
            case DEBATE:
                return baseDelay / 3; // Very fast responses in debates
            default:
                return baseDelay;
        }
    }
    
    /**
     * Update conversation mood based on AI response
     */
    private void updateConversationMood(ConversationState state, AIResponse response) {
        String content = response.getContent().toLowerCase();
        
        if (content.contains("!") || content.contains("excited") || content.contains("amazing")) {
            state.setMood(ConversationContext.ConversationMood.EXCITED);
        } else if (content.contains("?") || content.contains("debate") || content.contains("argue")) {
            state.setMood(ConversationContext.ConversationMood.DEBATE);
        } else if (content.contains("plan") || content.contains("trip") || content.contains("organize")) {
            state.setMood(ConversationContext.ConversationMood.PLANNING);
        } else if (content.contains("gossip") || content.contains("rumor") || content.contains("heard")) {
            state.setMood(ConversationContext.ConversationMood.GOSSIP);
        } else {
            state.setMood(ConversationContext.ConversationMood.CASUAL);
        }
    }
    
    /**
     * Create fallback response when AI is unavailable
     */
    private AIResponse createFallbackResponse() {
        AIResponse response = new AIResponse();
        
        // More engaging fallback messages
        String[] fallbackMessages = {
            "Hmm, let me think about that...",
            "That's an interesting point! Let me process this...",
            "I'm having a bit of trouble connecting to my thoughts right now, but I'm still here!",
            "Give me a moment to gather my thoughts...",
            "I'm processing this conversation, but my AI brain seems to be taking a coffee break!",
            "Let me think about this more carefully...",
            "I'm here, just need a moment to think through this properly."
        };
        
        // Randomly select a fallback message
        String selectedMessage = fallbackMessages[new java.util.Random().nextInt(fallbackMessages.length)];
        
        response.setContent(selectedMessage);
        response.setConfidence(0.1);
        response.setModelUsed("fallback");
        response.setResponseTimeMs(0L);
        response.setMessageType(Message.MessageType.TEXT);
        
        logger.info("Created fallback response: {}", selectedMessage);
        return response;
    }
    
    /**
     * Get conversation state for a group
     */
    public Optional<ConversationState> getConversationState(UUID groupId) {
        return Optional.ofNullable(activeConversations.get(groupId));
    }
    
    /**
     * Check if a group has an active conversation
     */
    public boolean isConversationActive(UUID groupId) {
        ConversationState state = activeConversations.get(groupId);
        return state != null && state.isActive();
    }
    
    /**
     * Inner class representing conversation state
     */
    public static class ConversationState {
        private final Group group;
        private final LocalDateTime startTime;
        private final AtomicInteger turnNumber = new AtomicInteger(0);
        private volatile boolean active = true;
        private volatile LocalDateTime lastMessageTime;
        private volatile String currentTopic;
        private volatile ConversationContext.ConversationMood mood = ConversationContext.ConversationMood.CASUAL;
        
        public ConversationState(Group group) {
            this.group = group;
            this.startTime = LocalDateTime.now();
            this.lastMessageTime = startTime;
        }
        
        // Getters and setters
        public Group getGroup() { return group; }
        public LocalDateTime getStartTime() { return startTime; }
        public int getTurnNumber() { return turnNumber.get(); }
        public void incrementTurnNumber() { turnNumber.incrementAndGet(); }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }
        public String getCurrentTopic() { return currentTopic; }
        public void setCurrentTopic(String currentTopic) { this.currentTopic = currentTopic; }
        public ConversationContext.ConversationMood getMood() { return mood; }
        public void setMood(ConversationContext.ConversationMood mood) { this.mood = mood; }
        
        public Character getCurrentCharacter() {
            // Simple round-robin character selection
            List<Character> characters = group.getMembers().stream()
                    .map(member -> member.getCharacter())
                    .toList();
            if (characters.isEmpty()) return null;
            return characters.get(turnNumber.get() % characters.size());
        }
    }
}
