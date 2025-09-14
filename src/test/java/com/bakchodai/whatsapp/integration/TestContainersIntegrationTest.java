package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests using TestContainers with real PostgreSQL database
 * 
 * These tests verify the application works correctly with a real database
 * instance, providing more realistic testing scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class TestContainersIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    @BeforeEach
    void setUp() {
        // Initialize test data with real database
        testDataInitializer.initializeTestData();
    }
    
    @Test
    void testDatabaseConnection_ShouldWorkWithRealPostgreSQL() {
        // Given
        String groupName = "TestContainers Group";
        String characterName = "TestContainers Character";
        
        // When
        Group group = createTestGroup(groupName, "Group created with TestContainers");
        Character character = createTestCharacter(characterName, "Test traits", "Test prompt");
        
        // Then
        assertNotNull(group.getId());
        assertNotNull(character.getId());
        
        // Verify entities were persisted
        Group savedGroup = groupRepository.findById(group.getId()).orElse(null);
        Character savedCharacter = characterRepository.findById(character.getId()).orElse(null);
        
        assertNotNull(savedGroup);
        assertNotNull(savedCharacter);
        assertEquals(groupName, savedGroup.getName());
        assertEquals(characterName, savedCharacter.getName());
    }
    
    @Test
    void testComplexDatabaseOperations_ShouldWorkWithRealPostgreSQL() {
        // Given
        Group group = createTestGroup("Complex Group", "Group for complex operations");
        Character character1 = createTestCharacter("Character 1", "Traits 1", "Prompt 1");
        Character character2 = createTestCharacter("Character 2", "Traits 2", "Prompt 2");
        
        // When
        group.addMember(character1);
        group.addMember(character2);
        Group savedGroup = groupRepository.save(group);
        
        // Then
        Group retrievedGroup = groupRepository.findByIdWithMembers(savedGroup.getId()).orElse(null);
        assertNotNull(retrievedGroup);
        assertEquals(2, retrievedGroup.getMembers().size());
        assertTrue(retrievedGroup.hasMember(character1));
        assertTrue(retrievedGroup.hasMember(character2));
    }
    
    @Test
    void testDatabaseTransactions_ShouldWorkWithRealPostgreSQL() {
        // Given
        Group group = createTestGroup("Transaction Group", "Group for transaction testing");
        Character character = createTestCharacter("Transaction Character", "Traits", "Prompt");
        
        // When
        group.addMember(character);
        Group savedGroup = groupRepository.save(group);
        
        // Simulate a transaction rollback scenario
        try {
            group.setName(null); // This should cause a constraint violation
            groupRepository.save(group);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Expected exception
        }
        
        // Then
        // Verify the group still exists with original data
        Group retrievedGroup = groupRepository.findById(savedGroup.getId()).orElse(null);
        assertNotNull(retrievedGroup);
        assertEquals("Transaction Group", retrievedGroup.getName());
    }
    
    @Test
    void testDatabaseQueries_ShouldWorkWithRealPostgreSQL() {
        // Given
        createTestGroup("Query Group 1", "Description 1");
        createTestGroup("Query Group 2", "Description 2");
        createTestCharacter("Query Character 1", "Traits 1", "Prompt 1");
        createTestCharacter("Query Character 2", "Traits 2", "Prompt 2");
        
        // When
        List<Group> allGroups = groupRepository.findByIsActiveTrue();
        List<Character> allCharacters = characterRepository.findByIsActiveTrue();
        
        // Then
        assertTrue(allGroups.size() >= 2);
        assertTrue(allCharacters.size() >= 2);
        
        // Test specific queries
        Group foundGroup = groupRepository.findByNameIgnoreCase("Query Group 1").orElse(null);
        assertNotNull(foundGroup);
        assertEquals("Query Group 1", foundGroup.getName());
        
        Character foundCharacter = characterRepository.findByNameIgnoreCase("Query Character 1").orElse(null);
        assertNotNull(foundCharacter);
        assertEquals("Query Character 1", foundCharacter.getName());
    }
    
    @Test
    void testDatabasePerformance_ShouldWorkWithRealPostgreSQL() {
        // Given
        int numberOfGroups = 100;
        int numberOfCharacters = 50;
        
        // When
        long startTime = System.currentTimeMillis();
        
        // Create groups
        for (int i = 0; i < numberOfGroups; i++) {
            createTestGroup("Performance Group " + i, "Description " + i);
        }
        
        // Create characters
        for (int i = 0; i < numberOfCharacters; i++) {
            createTestCharacter("Performance Character " + i, "Traits " + i, "Prompt " + i);
        }
        
        long creationTime = System.currentTimeMillis() - startTime;
        
        // Query all groups
        startTime = System.currentTimeMillis();
        List<Group> allGroups = groupRepository.findByIsActiveTrue();
        long queryTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertTrue(creationTime < 5000, "Creation should complete within 5 seconds");
        assertTrue(queryTime < 1000, "Query should complete within 1 second");
        assertTrue(allGroups.size() >= numberOfGroups);
    }
    
    @Test
    void testDatabaseConstraints_ShouldWorkWithRealPostgreSQL() {
        // Given
        Group group = createTestGroup("Constraint Group", "Description");
        
        // When & Then
        // Test unique constraint on group name
        Group duplicateGroup = new Group();
        duplicateGroup.setName("Constraint Group");
        duplicateGroup.setDescription("Duplicate");
        duplicateGroup.setIsActive(true);
        duplicateGroup.setCreatedAt(LocalDateTime.now());
        
        assertThrows(Exception.class, () -> {
            groupRepository.save(duplicateGroup);
        });
    }
    
    // Helper methods
    private Group createTestGroup(String name, String description) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setIsActive(true);
        group.setCreatedAt(LocalDateTime.now());
        return groupRepository.save(group);
    }
    
    private Character createTestCharacter(String name, String traits, String systemPrompt) {
        Character character = new Character();
        character.setName(name);
        character.setPersonalityTraits(traits);
        character.setSystemPrompt(systemPrompt);
        character.setSpeakingStyle("casual");
        character.setBackground("Test character");
        character.setIsActive(true);
        character.setCreatedAt(LocalDateTime.now());
        return characterRepository.save(character);
    }
}



