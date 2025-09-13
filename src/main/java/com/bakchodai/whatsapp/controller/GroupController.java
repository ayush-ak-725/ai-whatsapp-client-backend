package com.bakchodai.whatsapp.controller;

import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for group management operations
 * 
 * This controller provides endpoints for managing groups, adding/removing members,
 * and controlling conversations.
 */
@RestController
@RequestMapping("/api/v1/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);
    
    private final GroupService groupService;
    
    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }
    
    /**
     * Create a new group
     */
    @PostMapping
    public ResponseEntity<Group> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        logger.info("Creating group: {}", request.getName());
        
        try {
            Group group = groupService.createGroup(request.getName(), request.getDescription());
            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create group: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get all groups
     */
    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        logger.info("Fetching all groups");
        List<Group> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Get group by ID
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroup(@PathVariable UUID groupId) {
        logger.info("Fetching group: {}", groupId);
        
        return groupService.getGroupById(groupId)
                .map(group -> ResponseEntity.ok(group))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get group with members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<Group> getGroupWithMembers(@PathVariable UUID groupId) {
        logger.info("Fetching group with members: {}", groupId);
        
        return groupService.getGroupWithMembers(groupId)
                .map(group -> ResponseEntity.ok(group))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update group
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<Group> updateGroup(@PathVariable UUID groupId, 
                                           @Valid @RequestBody UpdateGroupRequest request) {
        logger.info("Updating group: {}", groupId);
        
        try {
            Group group = groupService.updateGroup(groupId, request.getName(), request.getDescription());
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update group: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete group
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
        logger.info("Deleting group: {}", groupId);
        
        try {
            groupService.deleteGroup(groupId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete group: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Add character to group
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Group> addMember(@PathVariable UUID groupId, 
                                         @Valid @RequestBody AddMemberRequest request) {
        logger.info("Adding character {} to group {}", request.getCharacterId(), groupId);
        
        try {
            Group group = groupService.addCharacterToGroup(groupId, request.getCharacterId());
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to add member: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Remove character from group
     */
    @DeleteMapping("/{groupId}/members/{characterId}")
    public ResponseEntity<Group> removeMember(@PathVariable UUID groupId, 
                                            @PathVariable UUID characterId) {
        logger.info("Removing character {} from group {}", characterId, groupId);
        
        try {
            Group group = groupService.removeCharacterFromGroup(groupId, characterId);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to remove member: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get group members
     */
    @GetMapping("/{groupId}/characters")
    public ResponseEntity<List<Character>> getGroupMembers(@PathVariable UUID groupId) {
        logger.info("Fetching members for group: {}", groupId);
        List<Character> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }
    
    /**
     * Get available characters (not in group)
     */
    @GetMapping("/{groupId}/available-characters")
    public ResponseEntity<List<Character>> getAvailableCharacters(@PathVariable UUID groupId) {
        logger.info("Fetching available characters for group: {}", groupId);
        List<Character> availableCharacters = groupService.getAvailableCharacters(groupId);
        return ResponseEntity.ok(availableCharacters);
    }
    
    /**
     * Start conversation in group
     */
    @PostMapping("/{groupId}/conversation/start")
    public ResponseEntity<Void> startConversation(@PathVariable UUID groupId) {
        logger.info("Starting conversation in group: {}", groupId);
        
        try {
            groupService.startConversation(groupId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to start conversation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Stop conversation in group
     */
    @PostMapping("/{groupId}/conversation/stop")
    public ResponseEntity<Void> stopConversation(@PathVariable UUID groupId) {
        logger.info("Stopping conversation in group: {}", groupId);
        
        try {
            groupService.stopConversation(groupId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            logger.warn("Failed to stop conversation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Check if conversation is active
     */
    @GetMapping("/{groupId}/conversation/status")
    public ResponseEntity<ConversationStatusResponse> getConversationStatus(@PathVariable UUID groupId) {
        logger.info("Checking conversation status for group: {}", groupId);
        boolean isActive = groupService.isConversationActive(groupId);
        return ResponseEntity.ok(new ConversationStatusResponse(isActive));
    }
    
    /**
     * Get group messages
     */
    @GetMapping("/{groupId}/messages")
    public ResponseEntity<Page<Message>> getGroupMessages(@PathVariable UUID groupId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        logger.info("Fetching messages for group: {} (page: {}, size: {})", groupId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = groupService.getGroupMessages(groupId, pageable);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Get recent messages
     */
    @GetMapping("/{groupId}/messages/recent")
    public ResponseEntity<List<Message>> getRecentMessages(@PathVariable UUID groupId,
                                                         @RequestParam(defaultValue = "10") int limit) {
        logger.info("Fetching recent messages for group: {} (limit: {})", groupId, limit);
        
        List<Message> messages = groupService.getRecentMessages(groupId, limit);
        return ResponseEntity.ok(messages);
    }
    
    // Request/Response DTOs
    
    public static class CreateGroupRequest {
        @NotNull(message = "Group name is required")
        private String name;
        private String description;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class UpdateGroupRequest {
        private String name;
        private String description;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class AddMemberRequest {
        @NotNull(message = "Character ID is required")
        private UUID characterId;
        
        // Getters and setters
        public UUID getCharacterId() { return characterId; }
        public void setCharacterId(UUID characterId) { this.characterId = characterId; }
    }
    
    public static class ConversationStatusResponse {
        private boolean active;
        
        public ConversationStatusResponse(boolean active) {
            this.active = active;
        }
        
        // Getters and setters
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}

