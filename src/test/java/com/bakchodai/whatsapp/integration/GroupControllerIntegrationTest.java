package com.bakchodai.whatsapp.integration;

import com.bakchodai.whatsapp.config.TestConfig;
import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GroupController
 * 
 * These tests verify the complete flow of group management operations
 * including REST API endpoints, database interactions, and business logic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class GroupControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private TestConfig.TestDataInitializer testDataInitializer;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private Group testGroup;
    private Character testCharacter1;
    private Character testCharacter2;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        
        // Initialize test data
        testDataInitializer.initializeTestData();
        
        // Get test entities
        testGroup = groupRepository.findByNameIgnoreCase("Test Group").orElse(null);
        testCharacter1 = characterRepository.findByNameIgnoreCase("Virat Kohli").orElse(null);
        testCharacter2 = characterRepository.findByNameIgnoreCase("Kapil Sharma").orElse(null);
    }
    
    @Test
    void testCreateGroup_Success() throws Exception {
        // Given
        String groupName = "New Test Group";
        String groupDescription = "A new group for testing";
        
        String requestBody = objectMapper.writeValueAsString(new CreateGroupRequest(groupName, groupDescription));
        
        // When & Then
        mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(groupName)))
                .andExpect(jsonPath("$.description", is(groupDescription)))
                .andExpect(jsonPath("$.isActive", is(true)))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        
        // Verify group was saved to database
        Group savedGroup = groupRepository.findByNameIgnoreCase(groupName).orElse(null);
        assert savedGroup != null;
        assert savedGroup.getName().equals(groupName);
        assert savedGroup.getDescription().equals(groupDescription);
    }
    
    @Test
    void testCreateGroup_DuplicateName_ShouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(
                new CreateGroupRequest("Test Group", "Duplicate group"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCreateGroup_InvalidInput_ShouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(new CreateGroupRequest("", "Invalid group"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetAllGroups_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].isActive", is(true)));
    }
    
    @Test
    void testGetGroupById_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", testGroup.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testGroup.getId().toString())))
                .andExpect(jsonPath("$.name", is(testGroup.getName())))
                .andExpect(jsonPath("$.description", is(testGroup.getDescription())));
    }
    
    @Test
    void testGetGroupById_NotFound_ShouldReturnNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testGetGroupWithMembers_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}/members", testGroup.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testGroup.getId().toString())))
                .andExpect(jsonPath("$.members", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.members[0].name", notNullValue()));
    }
    
    @Test
    void testUpdateGroup_Success() throws Exception {
        // Given
        String newName = "Updated Test Group";
        String newDescription = "Updated description";
        
        String requestBody = objectMapper.writeValueAsString(
                new UpdateGroupRequest(newName, newDescription));
        
        // When & Then
        mockMvc.perform(put("/api/v1/groups/{groupId}", testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(newName)))
                .andExpect(jsonPath("$.description", is(newDescription)));
        
        // Verify group was updated in database
        Group updatedGroup = groupRepository.findById(testGroup.getId()).orElse(null);
        assert updatedGroup != null;
        assert updatedGroup.getName().equals(newName);
        assert updatedGroup.getDescription().equals(newDescription);
    }
    
    @Test
    void testAddCharacterToGroup_Success() throws Exception {
        // Given
        Character newCharacter = createTestCharacter("New Character", "Test traits", "Test prompt");
        String requestBody = objectMapper.writeValueAsString(new AddMemberRequest(newCharacter.getId()));
        
        // When & Then
        mockMvc.perform(post("/api/v1/groups/{groupId}/members", testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(greaterThanOrEqualTo(1))));
        
        // Verify character was added to group
        Group updatedGroup = groupRepository.findByIdWithMembers(testGroup.getId()).orElse(null);
        assert updatedGroup != null;
        assert updatedGroup.hasMember(newCharacter);
    }
    
    @Test
    void testAddCharacterToGroup_CharacterAlreadyMember_ShouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(new AddMemberRequest(testCharacter1.getId()));
        
        // When & Then
        mockMvc.perform(post("/api/v1/groups/{groupId}/members", testGroup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testRemoveCharacterFromGroup_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{characterId}", 
                testGroup.getId(), testCharacter1.getId()))
                .andDo(print())
                .andExpect(status().isOk());
        
        // Verify character was removed from group
        Group updatedGroup = groupRepository.findByIdWithMembers(testGroup.getId()).orElse(null);
        assert updatedGroup != null;
        assert !updatedGroup.hasMember(testCharacter1);
    }
    
    @Test
    void testGetGroupMembers_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}/characters", testGroup.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].isActive", is(true)));
    }
    
    @Test
    void testGetAvailableCharacters_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}/available-characters", testGroup.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }
    
    @Test
    void testGetGroupMessages_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}/messages", testGroup.getId())
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].content", notNullValue()))
                .andExpect(jsonPath("$.content[0].characterName", notNullValue()));
    }
    
    @Test
    void testGetRecentMessages_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}/messages/recent", testGroup.getId())
                .param("limit", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].content", notNullValue()));
    }
    
    @Test
    void testDeleteGroup_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/groups/{groupId}", testGroup.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());
        
        // Verify group was soft deleted
        Group deletedGroup = groupRepository.findById(testGroup.getId()).orElse(null);
        assert deletedGroup != null;
        assert !deletedGroup.getIsActive();
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
    private static class CreateGroupRequest {
        public String name;
        public String description;
        
        public CreateGroupRequest(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    private static class UpdateGroupRequest {
        public String name;
        public String description;
        
        public UpdateGroupRequest(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    private static class AddMemberRequest {
        public UUID characterId;
        
        public AddMemberRequest(UUID characterId) {
            this.characterId = characterId;
        }
    }
}

