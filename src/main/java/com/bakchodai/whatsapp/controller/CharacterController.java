package com.bakchodai.whatsapp.controller;

import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.service.CharacterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for character management operations
 * 
 * This controller provides endpoints for managing AI characters,
 * their personalities, and character operations.
 */
@RestController
@RequestMapping("/api/v1/characters")
@CrossOrigin(origins = "*")
public class CharacterController {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterController.class);
    
    private final CharacterService characterService;
    
    @Autowired
    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }
    
    /**
     * Create a new character
     */
    @PostMapping
    public ResponseEntity<Character> createCharacter(@Valid @RequestBody CreateCharacterRequest request) {
        logger.info("Creating character: {}", request.getName());
        
        try {
            Character character = characterService.createCharacter(
                request.getName(),
                request.getPersonalityTraits(),
                request.getSystemPrompt(),
                request.getSpeakingStyle(),
                request.getBackground(),
                request.getAvatarUrl()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(character);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create character: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get all characters
     */
    @GetMapping
    public ResponseEntity<List<Character>> getAllCharacters() {
        logger.info("Fetching all characters");
        List<Character> characters = characterService.getAllCharacters();
        return ResponseEntity.ok(characters);
    }
    
    /**
     * Get character by ID
     */
    @GetMapping("/{characterId}")
    public ResponseEntity<Character> getCharacter(@PathVariable UUID characterId) {
        logger.info("Fetching character: {}", characterId);
        
        return characterService.getCharacterById(characterId)
                .map(character -> ResponseEntity.ok(character))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get character with group memberships
     */
    @GetMapping("/{characterId}/groups")
    public ResponseEntity<Character> getCharacterWithGroups(@PathVariable UUID characterId) {
        logger.info("Fetching character with groups: {}", characterId);
        
        return characterService.getCharacterWithGroups(characterId)
                .map(character -> ResponseEntity.ok(character))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Search characters by personality
     */
    @GetMapping("/search")
    public ResponseEntity<List<Character>> searchCharacters(@RequestParam String keyword) {
        logger.info("Searching characters with keyword: {}", keyword);
        List<Character> characters = characterService.searchCharactersByPersonality(keyword);
        return ResponseEntity.ok(characters);
    }
    
    /**
     * Get characters by speaking style
     */
    @GetMapping("/by-speaking-style")
    public ResponseEntity<List<Character>> getCharactersBySpeakingStyle(@RequestParam String speakingStyle) {
        logger.info("Fetching characters by speaking style: {}", speakingStyle);
        List<Character> characters = characterService.getCharactersBySpeakingStyle(speakingStyle);
        return ResponseEntity.ok(characters);
    }
    
    /**
     * Update character
     */
    @PutMapping("/{characterId}")
    public ResponseEntity<Character> updateCharacter(@PathVariable UUID characterId,
                                                   @Valid @RequestBody UpdateCharacterRequest request) {
        logger.info("Updating character: {}", characterId);
        
        try {
            Character character = characterService.updateCharacter(
                characterId,
                request.getName(),
                request.getPersonalityTraits(),
                request.getSystemPrompt(),
                request.getSpeakingStyle(),
                request.getBackground(),
                request.getAvatarUrl()
            );
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update character: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete character
     */
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Void> deleteCharacter(@PathVariable UUID characterId) {
        logger.info("Deleting character: {}", characterId);
        
        try {
            characterService.deleteCharacter(characterId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete character: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get character count
     */
    @GetMapping("/count")
    public ResponseEntity<CharacterCountResponse> getCharacterCount() {
        logger.info("Fetching character count");
        long count = characterService.getCharacterCount();
        return ResponseEntity.ok(new CharacterCountResponse(count));
    }
    
    /**
     * Create predefined characters
     */
    @PostMapping("/predefined")
    public ResponseEntity<Void> createPredefinedCharacters() {
        logger.info("Creating predefined characters");
        characterService.createPredefinedCharacters();
        return ResponseEntity.ok().build();
    }
    
    // Request/Response DTOs
    
    public static class CreateCharacterRequest {
        @NotNull(message = "Character name is required")
        private String name;
        private String personalityTraits;
        private String systemPrompt;
        private String speakingStyle;
        private String background;
        private String avatarUrl;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPersonalityTraits() { return personalityTraits; }
        public void setPersonalityTraits(String personalityTraits) { this.personalityTraits = personalityTraits; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getSpeakingStyle() { return speakingStyle; }
        public void setSpeakingStyle(String speakingStyle) { this.speakingStyle = speakingStyle; }
        public String getBackground() { return background; }
        public void setBackground(String background) { this.background = background; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    public static class UpdateCharacterRequest {
        private String name;
        private String personalityTraits;
        private String systemPrompt;
        private String speakingStyle;
        private String background;
        private String avatarUrl;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPersonalityTraits() { return personalityTraits; }
        public void setPersonalityTraits(String personalityTraits) { this.personalityTraits = personalityTraits; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getSpeakingStyle() { return speakingStyle; }
        public void setSpeakingStyle(String speakingStyle) { this.speakingStyle = speakingStyle; }
        public String getBackground() { return background; }
        public void setBackground(String background) { this.background = background; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    public static class CharacterCountResponse {
        private long count;
        
        public CharacterCountResponse(long count) {
            this.count = count;
        }
        
        // Getters and setters
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}

