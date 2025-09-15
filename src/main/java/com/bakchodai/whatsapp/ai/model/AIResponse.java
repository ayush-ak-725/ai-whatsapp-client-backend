package com.bakchodai.whatsapp.ai.model;

import com.bakchodai.whatsapp.domain.model.Message;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response object containing AI-generated content and metadata
 * 
 * This class encapsulates the AI response along with metadata about
 * the generation process, confidence scores, and additional context.
 */
public class AIResponse {
    @JsonProperty("content")
    private String content;
    @JsonProperty("message_type")
    private Message.MessageType messageType;
    @JsonProperty("confidence")
    private Double confidence;
    @JsonProperty("model_used")
    private String modelUsed;
    @JsonProperty("response_time_ms")
    private Long responseTimeMs;
    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    @JsonProperty("is_interruption")
    private boolean isInterruption;
    @JsonProperty("reasoning")
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


