package com.bakchodai.whatsapp.ai.connector;

import com.bakchodai.whatsapp.ai.model.AIResponse;
import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.bakchodai.whatsapp.ai.model.AIRequestDTO;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpAIConnector implements AIConnector {

    private static final Logger logger = LoggerFactory.getLogger(HttpAIConnector.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String aiBackendUrl;

    public HttpAIConnector(
            ObjectMapper objectMapper,
            @Value("${ai.backend.url}") String aiBackendUrl) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)  // Force HTTP/1.1
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.aiBackendUrl = aiBackendUrl;
        logger.info("AI Backend URL: {}", aiBackendUrl);
    }

    @Override
    public CompletableFuture<AIResponse> generateResponse(ConversationContext context) {
        try {
            // Convert ConversationContext to DTO to avoid circular reference issues
            AIRequestDTO requestDTO = convertToDTO(context);
            String requestBody = objectMapper.writeValueAsString(requestDTO);
            
            logger.info("Sending AI request to: {}", aiBackendUrl + "/api/v1/ai/generate-response");
            logger.debug("Request body: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiBackendUrl + "/api/v1/ai/generate-response"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Java-Client/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            logger.info("AI response status: {}, body: {}", response.statusCode(), response.body());
                            
                            if (response.statusCode() != 200) {
                                logger.error("AI backend returned non-200 status: {}", response.statusCode());
                                return createErrorResponse("AI backend returned status: " + response.statusCode());
                            }
                            
                            AIResponse aiResponse = objectMapper.readValue(response.body(), AIResponse.class);
                            logger.info("Successfully parsed AI response: content='{}', messageType='{}'", 
                                       aiResponse.getContent(), aiResponse.getMessageType());
                            return aiResponse;
                        } catch (Exception e) {
                            logger.error("Failed to parse AI response. Response body: {}", response.body(), e);
                            return createErrorResponse("Failed to parse AI response: " + e.getMessage());
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
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                // Parse JSON response to check status
                                String body = response.body();
                                return body.contains("\"status\":\"healthy\"") || 
                                       body.contains("\"status\": \"healthy\"");
                            } catch (Exception e) {
                                logger.warn("Failed to parse health check response: {}", response.body());
                                return false;
                            }
                        }
                        return false;
                    })
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
        response.setMessageType(Message.MessageType.TEXT);
        return response;
    }
    
    private AIRequestDTO convertToDTO(ConversationContext context) {
        AIRequestDTO dto = new AIRequestDTO();
        
        // Convert group
        if (context.getGroup() != null) {
            AIRequestDTO.GroupDTO groupDTO = new AIRequestDTO.GroupDTO();
            Group group = context.getGroup();
            groupDTO.setId(group.getId());
            groupDTO.setName(group.getName());
            groupDTO.setDescription(group.getDescription());
            groupDTO.setActive(group.getIsActive() != null ? group.getIsActive() : true);
            groupDTO.setCreatedAt(group.getCreatedAt());
            groupDTO.setUpdatedAt(group.getUpdatedAt());
            dto.setGroup(groupDTO);
        }
        
        // Convert current character
        if (context.getCurrentCharacter() != null) {
            AIRequestDTO.CharacterDTO characterDTO = new AIRequestDTO.CharacterDTO();
            characterDTO.setId(context.getCurrentCharacter().getId());
            characterDTO.setName(context.getCurrentCharacter().getName());
            characterDTO.setPersonalityTraits(context.getCurrentCharacter().getPersonalityTraits());
            characterDTO.setSystemPrompt(context.getCurrentCharacter().getSystemPrompt());
            characterDTO.setAvatarUrl(context.getCurrentCharacter().getAvatarUrl());
            characterDTO.setSpeakingStyle(context.getCurrentCharacter().getSpeakingStyle());
            characterDTO.setBackground(context.getCurrentCharacter().getBackground());
            characterDTO.setActive(context.getCurrentCharacter().getIsActive() != null ? context.getCurrentCharacter().getIsActive() : true);
            characterDTO.setCreatedAt(context.getCurrentCharacter().getCreatedAt());
            characterDTO.setUpdatedAt(context.getCurrentCharacter().getUpdatedAt());
            dto.setCurrentCharacter(characterDTO);
        }
        
        // Convert recent messages
        if (context.getRecentMessages() != null) {
            List<AIRequestDTO.MessageDTO> messageDTOs = context.getRecentMessages().stream()
                    .map(this::convertMessageToDTO)
                    .toList();
            dto.setRecentMessages(messageDTOs);
        }
        
        // Convert active characters
        if (context.getActiveCharacters() != null) {
            List<AIRequestDTO.CharacterDTO> characterDTOs = context.getActiveCharacters().stream()
                    .map(this::convertCharacterToDTO)
                    .toList();
            dto.setActiveCharacters(characterDTOs);
        }
        
        // Set other properties
        dto.setAdditionalContext(context.getAdditionalContext());
        dto.setConversationStartTime(context.getConversationStartTime());
        dto.setCurrentTopic(context.getCurrentTopic());
        dto.setMood(context.getMood() != null ? context.getMood().toString() : "CASUAL");
        
        return dto;
    }
    
    private AIRequestDTO.MessageDTO convertMessageToDTO(Message message) {
        AIRequestDTO.MessageDTO dto = new AIRequestDTO.MessageDTO();
        dto.setId(message.getId());
        dto.setGroupId(message.getGroup() != null ? message.getGroup().getId() : null);
        dto.setCharacterId(message.getCharacter() != null ? message.getCharacter().getId() : null);
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType() != null ? message.getMessageType().toString() : "TEXT");
        dto.setAiGenerated(message.getIsAiGenerated() != null ? message.getIsAiGenerated() : false);
        dto.setResponseTimeMs(message.getResponseTimeMs());
        dto.setTimestamp(message.getTimestamp());
        return dto;
    }
    
    private AIRequestDTO.CharacterDTO convertCharacterToDTO(Character character) {
        AIRequestDTO.CharacterDTO dto = new AIRequestDTO.CharacterDTO();
        dto.setId(character.getId());
        dto.setName(character.getName());
        dto.setPersonalityTraits(character.getPersonalityTraits());
        dto.setSystemPrompt(character.getSystemPrompt());
        dto.setAvatarUrl(character.getAvatarUrl());
        dto.setSpeakingStyle(character.getSpeakingStyle());
        dto.setBackground(character.getBackground());
        dto.setActive(character.getIsActive() != null ? character.getIsActive() : true);
        dto.setCreatedAt(character.getCreatedAt());
        dto.setUpdatedAt(character.getUpdatedAt());
        return dto;
    }
}
