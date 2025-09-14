package com.bakchodai.whatsapp.ai.connector;

import com.bakchodai.whatsapp.ai.model.AIResponse;
import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public class HttpAIConnector implements AIConnector {

    private static final Logger logger = LoggerFactory.getLogger(HttpAIConnector.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String aiBackendUrl;

    public HttpAIConnector(
            ObjectMapper objectMapper,
            @Value("${ai.backend.url:http://localhost:8000}") String aiBackendUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.aiBackendUrl = aiBackendUrl;
    }

    @Override
    public CompletableFuture<AIResponse> generateResponse(ConversationContext context) {
        try {
            String requestBody = objectMapper.writeValueAsString(context);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiBackendUrl + "/api/v1/ai/generate-response"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            return objectMapper.readValue(response.body(), AIResponse.class);
                        } catch (Exception e) {
                            logger.error("Failed to parse AI response", e);
                            return createErrorResponse("Failed to parse AI response");
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Error generating AI response", ex);
                        return createErrorResponse(ex.getMessage());
                    });

        } catch (Exception e) {
            logger.error("Unexpected error preparing AI request", e);
            return CompletableFuture.completedFuture(createErrorResponse(e.getMessage()));
        }
    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiBackendUrl + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> "ok".equalsIgnoreCase(response.body()))
                    .exceptionally(ex -> {
                        logger.error("AI backend health check failed", ex);
                        return false;
                    });

        } catch (Exception e) {
            logger.error("Error preparing health check request", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public String getConnectorName() {
        return "HttpAIConnector";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private AIResponse createErrorResponse(String errorMessage) {
        AIResponse response = new AIResponse();
        response.setContent("Sorry, I'm having trouble thinking right now. " + errorMessage);
        response.setConfidence(0.0);
        response.setModelUsed("error");
        response.setResponseTimeMs(0L);
        return response;
    }
}
