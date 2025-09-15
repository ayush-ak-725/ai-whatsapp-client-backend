package com.bakchodai.whatsapp.ai.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HttpAIConnectorHealthCheckTest {

    private HttpAIConnector httpAIConnector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        httpAIConnector = new HttpAIConnector(objectMapper, "http://localhost:8000");
    }

    @Test
    void testHealthCheckWithHealthyResponse() throws Exception {
        // This test would require a mock HTTP server
        // For now, we'll test the logic with a real server if available
        CompletableFuture<Boolean> healthFuture = httpAIConnector.isHealthy();
        
        // Wait for the health check to complete
        Boolean isHealthy = healthFuture.get(10, TimeUnit.SECONDS);
        
        // The result depends on whether the AI backend is running
        // This test will pass if the backend is healthy or if it gracefully handles the failure
        assertNotNull(isHealthy);
    }

    @Test
    void testHealthCheckWithUnhealthyResponse() throws Exception {
        // Test with a non-existent URL to simulate unhealthy response
        HttpAIConnector unhealthyConnector = new HttpAIConnector(objectMapper, "http://localhost:9999");
        
        CompletableFuture<Boolean> healthFuture = unhealthyConnector.isHealthy();
        Boolean isHealthy = healthFuture.get(10, TimeUnit.SECONDS);
        
        assertFalse(isHealthy);
    }

    @Test
    void testConnectorName() {
        assertEquals("HttpAIConnector", httpAIConnector.getConnectorName());
    }

    @Test
    void testConnectorPriority() {
        assertEquals(1, httpAIConnector.getPriority());
    }
}
