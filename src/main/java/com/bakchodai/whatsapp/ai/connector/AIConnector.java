package com.bakchodai.whatsapp.ai.connector;

import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.bakchodai.whatsapp.ai.model.AIResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for AI connectors
 * 
 * This interface defines the contract for different AI integration strategies.
 * Implementations can use HTTP calls to external services or direct integration
 * depending on the deployment architecture.
 */
public interface AIConnector {
    
    /**
     * Generate AI response for the given conversation context
     * 
     * @param context The conversation context containing group, character, and message history
     * @return CompletableFuture containing the AI response
     */
    CompletableFuture<AIResponse> generateResponse(ConversationContext context);
    
    /**
     * Check if the AI connector is available and healthy
     * 
     * @return CompletableFuture containing true if healthy, false otherwise
     */
    CompletableFuture<Boolean> isHealthy();
    
    /**
     * Get the name of the AI connector implementation
     * 
     * @return The connector name
     */
    String getConnectorName();
    
    /**
     * Get the priority of this connector (lower number = higher priority)
     * 
     * @return The priority value
     */
    int getPriority();
}


