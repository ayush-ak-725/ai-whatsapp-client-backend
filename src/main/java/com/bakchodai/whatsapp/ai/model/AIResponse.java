package com.bakchodai.whatsapp.ai.model;

import com.bakchodai.whatsapp.domain.model.Message;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response object containing AI-generated content and metadata
 * 
 * This class encapsulates the AI response along with metadata about
 * the generation process, confidence scores, and additional context.
 */
public class AIResponse {
    
    private String content;
    private Message.MessageType messageType;
    private Double confidence;
    private String modelUsed;
    private Long responseTimeMs;
    private LocalDateTime generatedAt;
    private Map<String, Object> metadata;
    private boolean isInterruption;
    private String reasoning;
    
    // Constructors
    public AIResponse() {
        this.generatedAt = LocalDateTime.now();
    }
    
    public AIResponse(String content) {
        this();
        this.content = content;
        this.messageType = Message.MessageType.TEXT;
    }
    
    public AIResponse(String content, Message.MessageType messageType) {
        this(content);
        this.messageType = messageType;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Message.MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(Message.MessageType messageType) {
        this.messageType = messageType;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getModelUsed() {
        return modelUsed;
    }
    
    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public boolean isInterruption() {
        return isInterruption;
    }
    
    public void setInterruption(boolean interruption) {
        isInterruption = interruption;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Check if the response is valid
     * 
     * @return True if the response has valid content
     */
    public boolean isValid() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * Get a truncated version of the content for logging
     * 
     * @param maxLength Maximum length of the truncated content
     * @return Truncated content
     */
    public String getTruncatedContent(int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
    
    @Override
    public String toString() {
        return "AIResponse{" +
                "content='" + getTruncatedContent(50) + '\'' +
                ", messageType=" + messageType +
                ", confidence=" + confidence +
                ", modelUsed='" + modelUsed + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                ", isInterruption=" + isInterruption +
                '}';
    }
}


