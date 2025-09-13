package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.ai.connector.HttpAIConnector;
import com.bakchodai.whatsapp.ai.model.AIResponse;
import com.bakchodai.whatsapp.ai.model.ConversationContext;
import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AI Connector functionality
 * 
 * These tests verify AI connector behavior with mock AI backend,
 * including successful responses, error handling, and fallback scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class AIConnectorIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    private WireMockServer wireMockServer;
    private HttpAIConnector httpAIConnector;
    private Group testGroup;
    private Character testCharacter;
    
    @BeforeEach
    void setUp() {
        // Initialize test data
        testDataInitializer.initializeTestData();
        
        // Get test entities
        testGroup = groupRepository.findByNameIgnoreCase("Test Group").orElse(null);
        testCharacter = characterRepository.findByNameIgnoreCase("Virat Kohli").orElse(null);
        
        // Setup WireMock server
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        
        // Setup AI connector
        WebClient.Builder webClientBuilder = WebClient.builder();
        ObjectMapper objectMapper = new ObjectMapper();
        httpAIConnector = new HttpAIConnector(webClientBuilder, objectMapper, "http://localhost:8089");
    }
    
    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    @Test
    void testGenerateResponse_Success() throws Exception {
        // Given
        String mockResponse = """
            {
                "content": "Hello! I'm Virat Kohli, ready to chat about cricket!",
                "messageType": "TEXT",
                "confidence": 0.9,
                "modelUsed": "gemini-pro",
                "responseTimeMs": 1500,
                "generatedAt": "2024-01-01T10:00:00",
                "isInterruption": false
            }
            """;
        
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/generate-response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));
        
        ConversationContext context = createTestConversationContext();
        
        // When
        CompletableFuture<AIResponse> responseFuture = httpAIConnector.generateResponse(context);
        AIResponse response = responseFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertEquals("Hello! I'm Virat Kohli, ready to chat about cricket!", response.getContent());
        assertEquals(0.9, response.getConfidence());
        assertEquals("gemini-pro", response.getModelUsed());
        assertEquals(1500L, response.getResponseTimeMs());
        assertFalse(response.isInterruption());
        
        // Verify request was made
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/ai/generate-response"))
                .withHeader("Content-Type", equalTo("application/json")));
    }
    
    @Test
    void testGenerateResponse_HttpError_ShouldReturnErrorResponse() throws Exception {
        // Given
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/generate-response"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal server error\"}")));
        
        ConversationContext context = createTestConversationContext();
        
        // When
        CompletableFuture<AIResponse> responseFuture = httpAIConnector.generateResponse(context);
        AIResponse response = responseFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.getContent().contains("trouble thinking"));
        assertTrue(response.getContent().contains("HTTP error"));
        assertEquals(0.0, response.getConfidence());
        assertEquals("error", response.getModelUsed());
    }
    
    @Test
    void testGenerateResponse_Timeout_ShouldReturnErrorResponse() throws Exception {
        // Given
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/generate-response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\": \"Response\"}")
                        .withFixedDelay(35000))); // Delay longer than timeout
        
        ConversationContext context = createTestConversationContext();
        
        // When
        CompletableFuture<AIResponse> responseFuture = httpAIConnector.generateResponse(context);
        AIResponse response = responseFuture.get(35, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.getContent().contains("trouble thinking"));
        assertTrue(response.getContent().contains("Unexpected error"));
        assertEquals(0.0, response.getConfidence());
        assertEquals("error", response.getModelUsed());
    }
    
    @Test
    void testGenerateResponse_InvalidJson_ShouldReturnErrorResponse() throws Exception {
        // Given
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/generate-response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json")));
        
        ConversationContext context = createTestConversationContext();
        
        // When
        CompletableFuture<AIResponse> responseFuture = httpAIConnector.generateResponse(context);
        AIResponse response = responseFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.getContent().contains("trouble thinking"));
        assertTrue(response.getContent().contains("Unexpected error"));
        assertEquals(0.0, response.getConfidence());
        assertEquals("error", response.getModelUsed());
    }
    
    @Test
    void testIsHealthy_Success() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("ok")));
        
        // When
        CompletableFuture<Boolean> healthFuture = httpAIConnector.isHealthy();
        Boolean isHealthy = healthFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertTrue(isHealthy);
        
        // Verify request was made
        wireMockServer.verify(getRequestedFor(urlEqualTo("/health")));
    }
    
    @Test
    void testIsHealthy_UnhealthyResponse_ShouldReturnFalse() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("unhealthy")));
        
        // When
        CompletableFuture<Boolean> healthFuture = httpAIConnector.isHealthy();
        Boolean isHealthy = healthFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertFalse(isHealthy);
    }
    
    @Test
    void testIsHealthy_HttpError_ShouldReturnFalse() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Internal server error")));
        
        // When
        CompletableFuture<Boolean> healthFuture = httpAIConnector.isHealthy();
        Boolean isHealthy = healthFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertFalse(isHealthy);
    }
    
    @Test
    void testIsHealthy_Timeout_ShouldReturnFalse() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("ok")
                        .withFixedDelay(10000))); // Delay longer than timeout
        
        // When
        CompletableFuture<Boolean> healthFuture = httpAIConnector.isHealthy();
        Boolean isHealthy = healthFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertFalse(isHealthy);
    }
    
    @Test
    void testGetConnectorName_ShouldReturnCorrectName() {
        // When
        String connectorName = httpAIConnector.getConnectorName();
        
        // Then
        assertEquals("HttpAIConnector", connectorName);
    }
    
    @Test
    void testGetPriority_ShouldReturnCorrectPriority() {
        // When
        int priority = httpAIConnector.getPriority();
        
        // Then
        assertEquals(1, priority);
    }
    
    @Test
    void testGenerateResponse_WithComplexContext_Success() throws Exception {
        // Given
        String mockResponse = """
            {
                "content": "That's an interesting point about cricket strategy!",
                "messageType": "TEXT",
                "confidence": 0.85,
                "modelUsed": "gemini-pro",
                "responseTimeMs": 2000,
                "generatedAt": "2024-01-01T10:00:00",
                "isInterruption": false,
                "reasoning": "Responding to cricket discussion with enthusiasm"
            }
            """;
        
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/generate-response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));
        
        ConversationContext context = createComplexConversationContext();
        
        // When
        CompletableFuture<AIResponse> responseFuture = httpAIConnector.generateResponse(context);
        AIResponse response = responseFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertEquals("That's an interesting point about cricket strategy!", response.getContent());
        assertEquals(0.85, response.getConfidence());
        assertEquals("gemini-pro", response.getModelUsed());
        assertEquals(2000L, response.getResponseTimeMs());
        assertEquals("Responding to cricket discussion with enthusiasm", response.getReasoning());
        
        // Verify request body contains context data
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/ai/generate-response"))
                .withRequestBody(containing("Virat Kohli"))
                .withRequestBody(containing("Test Group")));
    }
    
    // Helper methods
    private ConversationContext createTestConversationContext() {
        ConversationContext context = new ConversationContext();
        context.setGroup(testGroup);
        context.setCurrentCharacter(testCharacter);
        context.setRecentMessages(List.of());
        context.setActiveCharacters(List.of(testCharacter));
        context.setConversationStartTime(LocalDateTime.now());
        context.setCurrentTopic("General conversation");
        context.setMood(ConversationContext.ConversationMood.CASUAL);
        return context;
    }
    
    private ConversationContext createComplexConversationContext() {
        ConversationContext context = createTestConversationContext();
        
        // Add some recent messages
        Message message1 = new Message();
        message1.setContent("What do you think about the new cricket rules?");
        message1.setCharacter(testCharacter);
        message1.setTimestamp(LocalDateTime.now().minusMinutes(5));
        
        Message message2 = new Message();
        message2.setContent("I think they're great for the game!");
        message2.setCharacter(testCharacter);
        message2.setTimestamp(LocalDateTime.now().minusMinutes(3));
        
        context.setRecentMessages(List.of(message1, message2));
        context.setCurrentTopic("Cricket rules discussion");
        context.setMood(ConversationContext.ConversationMood.EXCITED);
        
        return context;
    }
}

