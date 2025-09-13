package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CharacterController
 * 
 * These tests verify the complete flow of character management operations
 * including REST API endpoints, database interactions, and business logic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class CharacterControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private Character testCharacter;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        
        // Initialize test data
        testDataInitializer.initializeTestData();
        
        // Get test character
        testCharacter = characterRepository.findByNameIgnoreCase("Virat Kohli").orElse(null);
    }
    
    @Test
    void testCreateCharacter_Success() throws Exception {
        // Given
        String characterName = "Test Character";
        String personalityTraits = "Funny, witty, intelligent";
        String systemPrompt = "You are a test character for integration testing.";
        String speakingStyle = "casual";
        String background = "Test background";
        String avatarUrl = "https://example.com/avatar.jpg";
        
        String requestBody = objectMapper.writeValueAsString(new CreateCharacterRequest(
                characterName, personalityTraits, systemPrompt, speakingStyle, background, avatarUrl));
        
        // When & Then
        mockMvc.perform(post("/api/v1/characters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(characterName)))
                .andExpect(jsonPath("$.personalityTraits", is(personalityTraits)))
                .andExpect(jsonPath("$.systemPrompt", is(systemPrompt)))
                .andExpect(jsonPath("$.speakingStyle", is(speakingStyle)))
                .andExpect(jsonPath("$.background", is(background)))
                .andExpect(jsonPath("$.avatarUrl", is(avatarUrl)))
                .andExpect(jsonPath("$.isActive", is(true)))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        
        // Verify character was saved to database
        Character savedCharacter = characterRepository.findByNameIgnoreCase(characterName).orElse(null);
        assert savedCharacter != null;
        assert savedCharacter.getName().equals(characterName);
        assert savedCharacter.getPersonalityTraits().equals(personalityTraits);
    }
    
    @Test
    void testCreateCharacter_DuplicateName_ShouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(new CreateCharacterRequest(
                "Virat Kohli", "Test traits", "Test prompt", "casual", "Test background", null));
        
        // When & Then
        mockMvc.perform(post("/api/v1/characters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCreateCharacter_InvalidInput_ShouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(new CreateCharacterRequest(
                "", "Test traits", "Test prompt", "casual", "Test background", null));
        
        // When & Then
        mockMvc.perform(post("/api/v1/characters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetAllCharacters_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/characters"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].isActive", is(true)));
    }
    
    @Test
    void testGetCharacterById_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/characters/{characterId}", testCharacter.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testCharacter.getId().toString())))
                .andExpect(jsonPath("$.name", is(testCharacter.getName())))
                .andExpect(jsonPath("$.personalityTraits", is(testCharacter.getPersonalityTraits())));
    }
    
    @Test
    void testGetCharacterById_NotFound_ShouldReturnNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        
        // When & Then
        mockMvc.perform(get("/api/v1/characters/{characterId}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testGetCharacterWithGroups_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/characters/{characterId}/groups", testCharacter.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testCharacter.getId().toString())))
                .andExpect(jsonPath("$.name", is(testCharacter.getName())));
    }
    
    @Test
    void testSearchCharacters_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/characters/search")
                .param("keyword", "Cricketer"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }
    
    @Test
    void testGetCharactersBySpeakingStyle_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/characters/by-speaking-style")
                .param("speakingStyle", "casual"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].speakingStyle", is("casual")));
    }
    
    @Test
    void testUpdateCharacter_Success() throws Exception {
        // Given
        String newName = "Updated Virat Kohli";
        String newTraits = "Updated personality traits";
        
        String requestBody = objectMapper.writeValueAsString(new UpdateCharacterRequest(
                newName, newTraits, null, null, null, null));
        
        // When & Then
        mockMvc.perform(put("/api/v1/characters/{characterId}", testCharacter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(newName)))
                .andExpect(jsonPath("$.personalityTraits", is(newTraits)));
        
        // Verify character was updated in database
        Character updatedCharacter = characterRepository.findById(testCharacter.getId()).orElse(null);
        assert updatedCharacter != null;
        assert updatedCharacter.getName().equals(newName);
        assert updatedCharacter.getPersonalityTraits().equals(newTraits);
    }
    
    @Test
    void testUpdateCharacter_DuplicateName_ShouldReturnBadRequest() throws Exception {
        // Given
        Character anotherCharacter = createTestCharacter("Another Character", "Traits", "Prompt");
        String requestBody = objectMapper.writeValueAsString(new UpdateCharacterRequest(
                "Virat Kohli", "Updated traits", null, null, null, null));
        
        // When & Then
        mockMvc.perform(put("/api/v1/characters/{characterId}", anotherCharacter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testDeleteCharacter_Success() throws Exception {
        // Given
        Character characterToDelete = createTestCharacter("Character to Delete", "Traits", "Prompt");
        
        // When & Then
        mockMvc.perform(delete("/api/v1/characters/{characterId}", characterToDelete.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());
        
        // Verify character was soft deleted
        Character deletedCharacter = characterRepository.findById(characterToDelete.getId()).orElse(null);
        assert deletedCharacter != null;
        assert !deletedCharacter.getIsActive();
    }
    
    @Test
    void testGetCharacterCount_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/characters/count"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }
    
    @Test
    void testCreatePredefinedCharacters_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/characters/predefined"))
                .andDo(print())
                .andExpect(status().isOk());
        
        // Verify predefined characters were created
        long characterCount = characterRepository.countByIsActiveTrue();
        assert characterCount >= 3; // At least 3 predefined characters
    }
    
    // Helper methods
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
    
    // Request DTOs
    private static class CreateCharacterRequest {
        public String name;
        public String personalityTraits;
        public String systemPrompt;
        public String speakingStyle;
        public String background;
        public String avatarUrl;
        
        public CreateCharacterRequest(String name, String personalityTraits, String systemPrompt,
                                   String speakingStyle, String background, String avatarUrl) {
            this.name = name;
            this.personalityTraits = personalityTraits;
            this.systemPrompt = systemPrompt;
            this.speakingStyle = speakingStyle;
            this.background = background;
            this.avatarUrl = avatarUrl;
        }
    }
    
    private static class UpdateCharacterRequest {
        public String name;
        public String personalityTraits;
        public String systemPrompt;
        public String speakingStyle;
        public String background;
        public String avatarUrl;
        
        public UpdateCharacterRequest(String name, String personalityTraits, String systemPrompt,
                                   String speakingStyle, String background, String avatarUrl) {
            this.name = name;
            this.personalityTraits = personalityTraits;
            this.systemPrompt = systemPrompt;
            this.speakingStyle = speakingStyle;
            this.background = background;
            this.avatarUrl = avatarUrl;
        }
    }
}

