package com.bakchodai.whatsapp.dto;

import com.bakchodai.whatsapp.domain.model.Message;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Message response to include full group and character objects
 */
public class MessageResponseDTO {
    
    private UUID id;
    private String groupId;
    private String groupName;
    private String characterId;
    private String characterName;
    private String content;
    private String messageType;
    private LocalDateTime timestamp;
    private Boolean isAiGenerated;
    private Long responseTimeMs;
    private String nextTurn;
    
    // Constructors
    public MessageResponseDTO() {}
    
    public MessageResponseDTO(Message message) {
        this.id = message.getId();
        this.groupId = message.getGroup() != null ? message.getGroup().getId().toString() : null;
        this.groupName = message.getGroup() != null ? message.getGroup().getName() : null;
        this.characterId = message.getCharacter() != null ? message.getCharacter().getId().toString() : null;
        this.characterName = message.getCharacter() != null ? message.getCharacter().getName() : null;
        this.content = message.getContent();
        this.messageType = message.getMessageType() != null ? message.getMessageType().toString() : "TEXT";
        this.timestamp = message.getTimestamp();
        this.isAiGenerated = message.getIsAiGenerated();
        this.responseTimeMs = message.getResponseTimeMs();
        this.nextTurn = message.getNextTurn();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getCharacterId() { return characterId; }
    public void setCharacterId(String characterId) { this.characterId = characterId; }
    
    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Boolean getIsAiGenerated() { return isAiGenerated; }
    public void setIsAiGenerated(Boolean isAiGenerated) { this.isAiGenerated = isAiGenerated; }
    
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public String getNextTurn() { return nextTurn; }
}
