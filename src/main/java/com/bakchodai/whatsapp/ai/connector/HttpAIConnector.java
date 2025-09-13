package com.bakchodai.whatsapp.ai.connector;

import com.bakchodai.whatsapp.ai.model.AIResponse;
import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-based AI connector implementation
 * 
 * This connector communicates with the Python AI backend service via HTTP.
 * It implements the Strategy pattern for AI integration and provides
 * fallback mechanisms for reliability.
 */
@Component
public class HttpAIConnector implements AIConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpAIConnector.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String aiBackendUrl;
    
    public HttpAIConnector(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ai.backend.url:http://localhost:8000}") String aiBackendUrl) {
        this.webClient = webClientBuilder
                .baseUrl(aiBackendUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.aiBackendUrl = aiBackendUrl;
    }
    
    @Override
    public CompletableFuture<AIResponse> generateResponse(ConversationContext context) {
        logger.debug("Generating AI response for character: {} in group: {}", 
                    context.getCurrentCharacter().getName(), 
                    context.getGroup().getName());
        
        return webClient.post()
                .uri("/api/v1/ai/generate-response")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(context)
                .retrieve()
                .bodyToMono(AIResponse.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> {
                    logger.debug("Successfully generated AI response: {}", response.getTruncatedContent(100));
                })
                .doOnError(error -> {
                    logger.error("Error generating AI response: {}", error.getMessage());
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("HTTP error generating AI response: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.just(createErrorResponse("HTTP error: " + ex.getStatusCode()));
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Unexpected error generating AI response", ex);
                    return Mono.just(createErrorResponse("Unexpected error: " + ex.getMessage()));
                })
                .toFuture();
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> "ok".equalsIgnoreCase(response))
                .doOnSuccess(healthy -> {
                    if (healthy) {
                        logger.debug("AI backend health check passed");
                    } else {
                        logger.warn("AI backend health check failed");
                    }
                })
                .doOnError(error -> {
                    logger.error("AI backend health check error: {}", error.getMessage());
                })
                .onErrorReturn(false)
                .toFuture();
    }
    
    @Override
    public String getConnectorName() {
        return "HttpAIConnector";
    }
    
    @Override
    public int getPriority() {
        return 1; // High priority for HTTP connector
    }
    
    /**
     * Create an error response when AI generation fails
     * 
     * @param errorMessage Error message
     * @return AIResponse with error content
     */
    private AIResponse createErrorResponse(String errorMessage) {
        AIResponse response = new AIResponse();
        response.setContent("Sorry, I'm having trouble thinking right now. " + errorMessage);
        response.setConfidence(0.0);
        response.setModelUsed("error");
        response.setResponseTimeMs(0L);
        return response;
    }
}
