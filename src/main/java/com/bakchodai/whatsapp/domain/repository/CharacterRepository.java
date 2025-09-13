package com.bakchodai.whatsapp.domain.repository;

import com.bakchodai.whatsapp.domain.model.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Character entity
 * 
 * Provides data access methods for managing AI characters and their properties.
 */
@Repository
public interface CharacterRepository extends JpaRepository<Character, UUID> {
    
    /**
     * Find all active characters
     * 
     * @return List of active characters
     */
    List<Character> findByIsActiveTrue();
    
    /**
     * Find character by name (case-insensitive)
     * 
     * @param name Character name
     * @return Optional containing the character if found
     */
    Optional<Character> findByNameIgnoreCase(String name);
    
    /**
     * Find characters by personality traits containing specific keywords
     * 
     * @param keyword Keyword to search in personality traits
     * @return List of characters with matching personality traits
     */
    @Query("SELECT c FROM Character c WHERE LOWER(c.personalityTraits) LIKE LOWER(CONCAT('%', :keyword, '%')) AND c.isActive = true")
    List<Character> findByPersonalityTraitsContaining(@Param("keyword") String keyword);
    
    /**
     * Find characters by speaking style
     * 
     * @param speakingStyle Speaking style to match
     * @return List of characters with matching speaking style
     */
    List<Character> findBySpeakingStyleAndIsActiveTrue(String speakingStyle);
    
    /**
     * Find characters that are members of a specific group
     * 
     * @param groupId Group ID
     * @return List of characters that are members of the group
     */
    @Query("SELECT c FROM Character c JOIN c.groupMemberships gm WHERE gm.group.id = :groupId AND c.isActive = true")
    List<Character> findByGroupId(@Param("groupId") UUID groupId);
    
    /**
     * Find characters that are NOT members of a specific group
     * 
     * @param groupId Group ID
     * @return List of characters that are not members of the group
     */
    @Query("SELECT c FROM Character c WHERE c.isActive = true AND c NOT IN " +
           "(SELECT c2 FROM Character c2 JOIN c2.groupMemberships gm WHERE gm.group.id = :groupId)")
    List<Character> findNotInGroup(@Param("groupId") UUID groupId);
    
    /**
     * Check if character exists and is active
     * 
     * @param characterId Character ID
     * @return True if character exists and is active
     */
    boolean existsByIdAndIsActiveTrue(UUID characterId);
    
    /**
     * Count active characters
     * 
     * @return Number of active characters
     */
    long countByIsActiveTrue();
    
    /**
     * Find characters with their group memberships
     * 
     * @param characterId Character ID
     * @return Optional containing the character with loaded group memberships
     */
    @Query("SELECT c FROM Character c LEFT JOIN FETCH c.groupMemberships gm LEFT JOIN FETCH gm.group WHERE c.id = :characterId")
    Optional<Character> findByIdWithGroupMemberships(@Param("characterId") UUID characterId);
}

