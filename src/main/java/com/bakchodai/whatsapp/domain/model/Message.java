package com.bakchodai.whatsapp.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a message in a group chat
 * 
 * Messages are sent by characters in groups and contain the conversation content.
 * Each message has a type (text, image, etc.) and maintains relationships to its sender and group.
 */
@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @NotNull(message = "Group is required")
    @JsonBackReference
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    @NotNull(message = "Character is required")
    @JsonBackReference
    private Character character;
    
    @NotBlank(message = "Message content is required")
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "is_ai_generated", nullable = false)
    private Boolean isAiGenerated = false;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "next_turn", nullable = true)
    private String nextTurn = null;
    
    // Constructors
    public Message() {}
    
    public Message(Group group, Character character, String content) {
        this.group = group;
        this.character = character;
        this.content = content;
    }
    
    public Message(Group group, Character character, String content, MessageType messageType) {
        this.group = group;
        this.character = character;
        this.content = content;
        this.messageType = messageType;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Group getGroup() {
        return group;
    }
    
    public void setGroup(Group group) {
        this.group = group;
    }
    
    public Character getCharacter() {
        return character;
    }
    
    public void setCharacter(Character character) {
        this.character = character;
    }
    
    public UUID getCharacterId() {
        return character != null ? character.getId() : null;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getIsAiGenerated() {
        return isAiGenerated;
    }
    
    public void setIsAiGenerated(Boolean isAiGenerated) {
        this.isAiGenerated = isAiGenerated;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getNextTurn() {
        return nextTurn;
    }

    public void setNextTurn(String nextTurn) {
        this.nextTurn = nextTurn;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id != null && id.equals(message.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", character=" + (character != null ? character.getName() : "null") +
                ", content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") + '\'' +
                ", messageType=" + messageType +
                ", timestamp=" + timestamp +
                ", isAiGenerated=" + isAiGenerated +
                '}';
    }
    
    /**
     * Enum representing different types of messages
     */
    public enum MessageType {
        TEXT,
        IMAGE,
        AUDIO,
        VIDEO,
        DOCUMENT,
        EMOJI,
        SYSTEM
    }
}
