package com.bakchodai.whatsapp.ai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for AI request to avoid circular reference issues with JPA entities
 */
public class AIRequestDTO {
    
    @JsonProperty("group")
    private GroupDTO group;
    
    @JsonProperty("current_character")
    private CharacterDTO currentCharacter;
    
    @JsonProperty("recent_messages")
    private List<MessageDTO> recentMessages;
    
    @JsonProperty("active_characters")
    private List<CharacterDTO> activeCharacters;
    
    @JsonProperty("additional_context")
    private Map<String, Object> additionalContext;
    
    @JsonProperty("conversation_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime conversationStartTime;
    
    @JsonProperty("current_topic")
    private String currentTopic;
    
    @JsonProperty("mood")
    private String mood;
    
    // Constructors
    public AIRequestDTO() {}
    
    // Getters and Setters
    public GroupDTO getGroup() {
        return group;
    }
    
    public void setGroup(GroupDTO group) {
        this.group = group;
    }
    
    public CharacterDTO getCurrentCharacter() {
        return currentCharacter;
    }
    
    public void setCurrentCharacter(CharacterDTO currentCharacter) {
        this.currentCharacter = currentCharacter;
    }
    
    public List<MessageDTO> getRecentMessages() {
        return recentMessages;
    }
    
    public void setRecentMessages(List<MessageDTO> recentMessages) {
        this.recentMessages = recentMessages;
    }
    
    public List<CharacterDTO> getActiveCharacters() {
        return activeCharacters;
    }
    
    public void setActiveCharacters(List<CharacterDTO> activeCharacters) {
        this.activeCharacters = activeCharacters;
    }
    
    public Map<String, Object> getAdditionalContext() {
        return additionalContext;
    }
    
    public void setAdditionalContext(Map<String, Object> additionalContext) {
        this.additionalContext = additionalContext;
    }
    
    public LocalDateTime getConversationStartTime() {
        return conversationStartTime;
    }
    
    public void setConversationStartTime(LocalDateTime conversationStartTime) {
        this.conversationStartTime = conversationStartTime;
    }
    
    public String getCurrentTopic() {
        return currentTopic;
    }
    
    public void setCurrentTopic(String currentTopic) {
        this.currentTopic = currentTopic;
    }
    
    public String getMood() {
        return mood;
    }
    
    public void setMood(String mood) {
        this.mood = mood;
    }
    
    // Inner DTO classes
    public static class GroupDTO {
        private UUID id;
        private String name;
        private String description;
        
        @JsonProperty("is_active")
        private boolean isActive;
        
        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;
        
        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime updatedAt;
        
        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class CharacterDTO {
        private UUID id;
        private String name;
        
        @JsonProperty("personality_traits")
        private String personalityTraits;
        
        @JsonProperty("system_prompt")
        private String systemPrompt;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
        
        @JsonProperty("speaking_style")
        private String speakingStyle;
        
        private String background;
        
        @JsonProperty("is_active")
        private boolean isActive;
        
        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;
        
        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime updatedAt;
        
        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPersonalityTraits() { return personalityTraits; }
        public void setPersonalityTraits(String personalityTraits) { this.personalityTraits = personalityTraits; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getSpeakingStyle() { return speakingStyle; }
        public void setSpeakingStyle(String speakingStyle) { this.speakingStyle = speakingStyle; }
        public String getBackground() { return background; }
        public void setBackground(String background) { this.background = background; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class MessageDTO {
        private UUID id;
        
        @JsonProperty("group_id")
        private UUID groupId;
        
        @JsonProperty("character_id")
        private UUID characterId;
        
        private String content;
        
        @JsonProperty("message_type")
        private String messageType;
        
        @JsonProperty("is_ai_generated")
        private boolean isAiGenerated;
        
        @JsonProperty("response_time_ms")
        private Long responseTimeMs;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime timestamp;
        
        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getGroupId() { return groupId; }
        public void setGroupId(UUID groupId) { this.groupId = groupId; }
        public UUID getCharacterId() { return characterId; }
        public void setCharacterId(UUID characterId) { this.characterId = characterId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public boolean isAiGenerated() { return isAiGenerated; }
        public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }
        public Long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
