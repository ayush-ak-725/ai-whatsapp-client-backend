package com.bakchodai.whatsapp.service;

import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.GroupMember;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing groups and their operations
 * 
 * This service provides business logic for group management including
 * creation, member management, and conversation control.
 */
@Service
@Transactional
public class GroupService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    
    private final GroupRepository groupRepository;
    private final CharacterRepository characterRepository;
    private final MessageRepository messageRepository;
    private final ConversationEngine conversationEngine;
    
    @Autowired
    public GroupService(GroupRepository groupRepository, 
                       CharacterRepository characterRepository,
                       MessageRepository messageRepository,
                       ConversationEngine conversationEngine) {
        this.groupRepository = groupRepository;
        this.characterRepository = characterRepository;
        this.messageRepository = messageRepository;
        this.conversationEngine = conversationEngine;
    }
    
    /**
     * Create a new group
     * 
     * @param name Group name
     * @param description Group description
     * @return Created group
     */
    public Group createGroup(String name, String description) {
        logger.info("Creating new group: {}", name);
        
        // Check if group with same name already exists
        if (groupRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Group with name '" + name + "' already exists");
        }
        
        Group group = new Group(name, description);
        Group savedGroup = groupRepository.save(group);
        
        logger.info("Successfully created group: {} with ID: {}", name, savedGroup.getId());
        return savedGroup;
    }
    
    /**
     * Get all active groups
     * 
     * @return List of active groups
     */
    @Transactional(readOnly = true)
    public List<Group> getAllGroups() {
        return groupRepository.findByIsActiveTrue();
    }
    
    /**
     * Get group by ID
     * 
     * @param groupId Group ID
     * @return Optional containing the group if found
     */
    @Transactional(readOnly = true)
    public Optional<Group> getGroupById(UUID groupId) {
        return groupRepository.findById(groupId);
    }
    
    /**
     * Get group with members loaded
     * 
     * @param groupId Group ID
     * @return Optional containing the group with members if found
     */
    @Transactional(readOnly = true)
    public Optional<Group> getGroupWithMembers(UUID groupId) {
        return groupRepository.findByIdWithMembers(groupId);
    }
    
    /**
     * Add character to group
     * 
     * @param groupId Group ID
     * @param characterId Character ID
     * @return Updated group
     */
    public Group addCharacterToGroup(UUID groupId, UUID characterId) {
        logger.info("Adding character {} to group {}", characterId, groupId);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
        
        if (group.hasMember(character)) {
            throw new IllegalArgumentException("Character is already a member of this group");
        }
        
        group.addMember(character);
        Group savedGroup = groupRepository.save(group);
        
        logger.info("Successfully added character {} to group {}", character.getName(), group.getName());
        return savedGroup;
    }
    
    /**
     * Remove character from group
     * 
     * @param groupId Group ID
     * @param characterId Character ID
     * @return Updated group
     */
    public Group removeCharacterFromGroup(UUID groupId, UUID characterId) {
        logger.info("Removing character {} from group {}", characterId, groupId);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
        
        if (!group.hasMember(character)) {
            throw new IllegalArgumentException("Character is not a member of this group");
        }
        
        group.removeMember(character);
        Group savedGroup = groupRepository.save(group);
        
        logger.info("Successfully removed character {} from group {}", character.getName(), group.getName());
        return savedGroup;
    }
    
    /**
     * Get group members
     * 
     * @param groupId Group ID
     * @return List of characters in the group
     */
    @Transactional(readOnly = true)
    public List<Character> getGroupMembers(UUID groupId) {
        return characterRepository.findByGroupId(groupId);
    }
    
    /**
     * Get available characters (not in group)
     * 
     * @param groupId Group ID
     * @return List of characters not in the group
     */
    @Transactional(readOnly = true)
    public List<Character> getAvailableCharacters(UUID groupId) {
        return characterRepository.findNotInGroup(groupId);
    }
    
    /**
     * Start conversation in group
     * 
     * @param groupId Group ID
     */
    public void startConversation(UUID groupId) {
        logger.info("Starting conversation in group: {}", groupId);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        if (group.getMembers().isEmpty()) {
            throw new IllegalStateException("Cannot start conversation: group has no members");
        }
        
        if (conversationEngine.isConversationActive(groupId)) {
            throw new IllegalStateException("Conversation is already active in this group");
        }
        
        conversationEngine.startConversation(group);
        logger.info("Conversation started in group: {}", group.getName());
    }
    
    /**
     * Stop conversation in group
     * 
     * @param groupId Group ID
     */
    public void stopConversation(UUID groupId) {
        logger.info("Stopping conversation in group: {}", groupId);
        
        if (!conversationEngine.isConversationActive(groupId)) {
            throw new IllegalStateException("No active conversation in this group");
        }
        
        conversationEngine.stopConversation(groupId);
        logger.info("Conversation stopped in group: {}", groupId);
    }
    
    /**
     * Check if conversation is active in group
     * 
     * @param groupId Group ID
     * @return True if conversation is active
     */
    @Transactional(readOnly = true)
    public boolean isConversationActive(UUID groupId) {
        return conversationEngine.isConversationActive(groupId);
    }
    
    /**
     * Get group messages
     * 
     * @param groupId Group ID
     * @param pageable Pagination information
     * @return Page of messages
     */
    @Transactional(readOnly = true)
    public Page<Message> getGroupMessages(UUID groupId, Pageable pageable) {
        return messageRepository.findByGroupIdOrderByTimestampAsc(groupId, pageable);
    }
    
    /**
     * Get recent messages for group
     * 
     * @param groupId Group ID
     * @param limit Maximum number of messages
     * @return List of recent messages
     */
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(UUID groupId, int limit) {
        return messageRepository.findRecentMessagesByGroupId(groupId, 
                org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    /**
     * Delete group
     * 
     * @param groupId Group ID
     */
    public void deleteGroup(UUID groupId) {
        logger.info("Deleting group: {}", groupId);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        // Stop conversation if active
        if (conversationEngine.isConversationActive(groupId)) {
            conversationEngine.stopConversation(groupId);
        }
        
        // Soft delete
        group.setIsActive(false);
        groupRepository.save(group);
        
        logger.info("Successfully deleted group: {}", group.getName());
    }
    
    /**
     * Update group information
     * 
     * @param groupId Group ID
     * @param name New group name
     * @param description New group description
     * @return Updated group
     */
    public Group updateGroup(UUID groupId, String name, String description) {
        logger.info("Updating group: {}", groupId);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        if (name != null && !name.trim().isEmpty()) {
            // Check if new name conflicts with existing groups
            Optional<Group> existingGroup = groupRepository.findByNameIgnoreCase(name);
            if (existingGroup.isPresent() && !existingGroup.get().getId().equals(groupId)) {
                throw new IllegalArgumentException("Group with name '" + name + "' already exists");
            }
            group.setName(name);
        }
        
        if (description != null) {
            group.setDescription(description);
        }
        
        Group savedGroup = groupRepository.save(group);
        logger.info("Successfully updated group: {}", savedGroup.getName());
        return savedGroup;
    }
}


