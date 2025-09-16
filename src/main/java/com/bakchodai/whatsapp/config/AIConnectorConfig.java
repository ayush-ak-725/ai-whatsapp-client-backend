package com.bakchodai.whatsapp.config;

import com.bakchodai.whatsapp.ai.connector.AIConnector;
import com.bakchodai.whatsapp.ai.connector.HttpAIConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for AI Connectors
 */
@Configuration
public class AIConnectorConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AIConnectorConfig.class);
    
    @Bean
    public AIConnector httpAIConnector(ObjectMapper objectMapper, 
                                     @Value("${ai.backend.url}") String aiBackendUrl) {
        logger.info("Creating HttpAIConnector bean with URL: {}", aiBackendUrl);
        return new HttpAIConnector(objectMapper, aiBackendUrl);
    }
}
