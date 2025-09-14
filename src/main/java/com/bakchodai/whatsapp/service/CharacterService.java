package com.bakchodai.whatsapp.service;

import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing AI characters
 * 
 * This service provides business logic for character management including
 * creation, personality configuration, and character operations.
 */
@Service
@Transactional
public class CharacterService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterService.class);
    
    private final CharacterRepository characterRepository;
    
    @Autowired
    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }
    
    /**
     * Create a new character
     * 
     * @param name Character name
     * @param personalityTraits Personality traits description
     * @param systemPrompt System prompt for AI behavior
     * @param speakingStyle Character's speaking style
     * @param background Character background
     * @param avatarUrl Avatar URL
     * @return Created character
     */
    public Character createCharacter(String name, String personalityTraits, String systemPrompt,
                                   String speakingStyle, String background, String avatarUrl) {
        logger.info("Creating new character: {}", name);
        
        // Check if character with same name already exists
        if (characterRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Character with name '" + name + "' already exists");
        }
        
        Character character = new Character(name, personalityTraits, systemPrompt);
        character.setSpeakingStyle(speakingStyle);
        character.setBackground(background);
        character.setAvatarUrl(avatarUrl);
        
        Character savedCharacter = characterRepository.save(character);
        
        logger.info("Successfully created character: {} with ID: {}", name, savedCharacter.getId());
        return savedCharacter;
    }
    
    /**
     * Get all active characters
     * 
     * @return List of active characters
     */
    @Transactional(readOnly = true)
    public List<Character> getAllCharacters() {
        return characterRepository.findByIsActiveTrue();
    }
    
    /**
     * Get character by ID
     * 
     * @param characterId Character ID
     * @return Optional containing the character if found
     */
    @Transactional(readOnly = true)
    public Optional<Character> getCharacterById(UUID characterId) {
        return characterRepository.findById(characterId);
    }
    
    /**
     * Get character with group memberships loaded
     * 
     * @param characterId Character ID
     * @return Optional containing the character with group memberships if found
     */
    @Transactional(readOnly = true)
    public Optional<Character> getCharacterWithGroups(UUID characterId) {
        return characterRepository.findByIdWithGroupMemberships(characterId);
    }
    
    /**
     * Search characters by personality traits
     * 
     * @param keyword Keyword to search for
     * @return List of characters with matching personality traits
     */
    @Transactional(readOnly = true)
    public List<Character> searchCharactersByPersonality(String keyword) {
        return characterRepository.findByPersonalityTraitsContaining(keyword);
    }
    
    /**
     * Get characters by speaking style
     * 
     * @param speakingStyle Speaking style to match
     * @return List of characters with matching speaking style
     */
    @Transactional(readOnly = true)
    public List<Character> getCharactersBySpeakingStyle(String speakingStyle) {
        return characterRepository.findBySpeakingStyleAndIsActiveTrue(speakingStyle);
    }
    
    /**
     * Update character information
     * 
     * @param characterId Character ID
     * @param name New character name
     * @param personalityTraits New personality traits
     * @param systemPrompt New system prompt
     * @param speakingStyle New speaking style
     * @param background New background
     * @param avatarUrl New avatar URL
     * @return Updated character
     */
    public Character updateCharacter(UUID characterId, String name, String personalityTraits,
                                   String systemPrompt, String speakingStyle, String background, String avatarUrl) {
        logger.info("Updating character: {}", characterId);
        
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
        
        if (name != null && !name.trim().isEmpty()) {
            // Check if new name conflicts with existing characters
            Optional<Character> existingCharacter = characterRepository.findByNameIgnoreCase(name);
            if (existingCharacter.isPresent() && !existingCharacter.get().getId().equals(characterId)) {
                throw new IllegalArgumentException("Character with name '" + name + "' already exists");
            }
            character.setName(name);
        }
        
        if (personalityTraits != null) {
            character.setPersonalityTraits(personalityTraits);
        }
        
        if (systemPrompt != null) {
            character.setSystemPrompt(systemPrompt);
        }
        
        if (speakingStyle != null) {
            character.setSpeakingStyle(speakingStyle);
        }
        
        if (background != null) {
            character.setBackground(background);
        }
        
        if (avatarUrl != null) {
            character.setAvatarUrl(avatarUrl);
        }
        
        Character savedCharacter = characterRepository.save(character);
        logger.info("Successfully updated character: {}", savedCharacter.getName());
        return savedCharacter;
    }
    
    /**
     * Delete character
     * 
     * @param characterId Character ID
     */
    public void deleteCharacter(UUID characterId) {
        logger.info("Deleting character: {}", characterId);
        
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
        
        // Soft delete
        character.setIsActive(false);
        characterRepository.save(character);
        
        logger.info("Successfully deleted character: {}", character.getName());
    }
    
    /**
     * Get character count
     * 
     * @return Number of active characters
     */
    @Transactional(readOnly = true)
    public long getCharacterCount() {
        return characterRepository.countByIsActiveTrue();
    }
    
    /**
     * Check if character exists and is active
     * 
     * @param characterId Character ID
     * @return True if character exists and is active
     */
    @Transactional(readOnly = true)
    public boolean characterExists(UUID characterId) {
        return characterRepository.existsByIdAndIsActiveTrue(characterId);
    }
    
    /**
     * Create predefined celebrity characters
     * 
     * This method creates some popular celebrity archetypes for demonstration
     */
    public void createPredefinedCharacters() {
        logger.info("Creating predefined celebrity characters");
        
        // Cricketer character
        if (!characterRepository.findByNameIgnoreCase("Virat Kohli").isPresent()) {
            createCharacter(
                "Virat Kohli",
                "Passionate, competitive, confident, aggressive, fitness enthusiast, team leader",
                "You are Virat Kohli, the legendary Indian cricketer and former captain. You're known for your aggressive batting style, fitness obsession, and leadership qualities. You speak with confidence, use cricket terminology, and often reference your achievements. You're passionate about the game and always ready to discuss cricket strategy, IPL, or team performance. You occasionally use Hindi phrases and are very patriotic about Indian cricket.",
                "Confident, aggressive, uses cricket terms, occasional Hindi phrases",
                "Former Indian cricket team captain, one of the greatest batsmen of all time, fitness icon",
                "https://example.com/avatars/virat-kohli.jpg"
            );
        }
        
        // Stand-up comedian character
        if (!characterRepository.findByNameIgnoreCase("Kapil Sharma").isPresent()) {
            createCharacter(
                "Kapil Sharma",
                "Humorous, witty, observational, relatable, family-oriented, down-to-earth",
                "You are Kapil Sharma, the popular Indian comedian and TV host. You're known for your observational humor, family jokes, and ability to make people laugh with everyday situations. You speak in a friendly, conversational tone, often make self-deprecating jokes, and love to roast your friends in a playful way. You use simple language and connect with people through relatable experiences.",
                "Friendly, humorous, conversational, self-deprecating, uses simple language",
                "Popular Indian comedian, TV host, known for observational humor and family comedy",
                "https://example.com/avatars/kapil-sharma.jpg"
            );
        }
        
        // Tech CEO character
        if (!characterRepository.findByNameIgnoreCase("Elon Musk").isPresent()) {
            createCharacter(
                "Elon Musk",
                "Innovative, ambitious, tech-savvy, sometimes controversial, future-focused, entrepreneurial",
                "You are Elon Musk, the CEO of Tesla and SpaceX. You're known for your innovative thinking, ambitious projects, and sometimes controversial tweets. You speak about technology, space exploration, electric vehicles, and future possibilities. You're passionate about solving global problems and often think big picture. You occasionally make bold predictions and aren't afraid to challenge conventional thinking.",
                "Innovative, ambitious, tech-focused, sometimes controversial, future-oriented",
                "CEO of Tesla and SpaceX, entrepreneur, known for innovative projects and bold vision",
                "https://example.com/avatars/elon-musk.jpg"
            );
        }
        
        // Bollywood actress character
        if (!characterRepository.findByNameIgnoreCase("Deepika Padukone").isPresent()) {
            createCharacter(
                "Deepika Padukone",
                "Elegant, graceful, confident, fashion-conscious, wellness-focused, articulate",
                "You are Deepika Padukone, the renowned Bollywood actress and mental health advocate. You're known for your elegance, grace, and strong opinions on social issues. You speak thoughtfully, often about mental health awareness, women's empowerment, and personal growth. You're fashion-conscious and occasionally discuss style and wellness. You use sophisticated language and are very articulate in your expressions.",
                "Elegant, thoughtful, articulate, sophisticated, wellness-focused",
                "Bollywood actress, mental health advocate, known for powerful performances and social activism",
                "https://example.com/avatars/deepika-padukone.jpg"
            );
        }
        
        // Food blogger character
        if (!characterRepository.findByNameIgnoreCase("Sanjeev Kapoor").isPresent()) {
            createCharacter(
                "Sanjeev Kapoor",
                "Passionate about food, knowledgeable, warm, encouraging, traditional yet modern",
                "You are Sanjeev Kapoor, the famous Indian chef and food personality. You're passionate about cooking, food culture, and sharing culinary knowledge. You speak warmly about food, often give cooking tips, and love to discuss different cuisines. You're encouraging and always ready to help with cooking questions. You occasionally share recipes and cooking techniques in your conversations.",
                "Warm, encouraging, food-focused, knowledgeable, traditional yet modern",
                "Famous Indian chef, food personality, known for popularizing Indian cuisine globally",
                "https://example.com/avatars/sanjeev-kapoor.jpg"
            );
        }
        
        logger.info("Successfully created predefined characters");
    }
}


