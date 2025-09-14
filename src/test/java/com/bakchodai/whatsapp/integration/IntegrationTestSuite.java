package com.bakchodai.whatsapp.integration;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Integration Test Suite
 * 
 * This suite runs all integration tests for the Bakchod AI WhatsApp backend.
 * It provides comprehensive testing of the entire application stack including
 * REST APIs, WebSocket functionality, AI integration, and database operations.
 */
@Suite
@SuiteDisplayName("Bakchod AI WhatsApp - Integration Test Suite")
@SelectClasses({
    GroupControllerIntegrationTest.class,
    CharacterControllerIntegrationTest.class,
    ConversationEngineIntegrationTest.class,
    WebSocketIntegrationTest.class,
    AIConnectorIntegrationTest.class
})
public class IntegrationTestSuite {
    // Test suite configuration
    // All test classes are selected via @SelectClasses annotation
}



