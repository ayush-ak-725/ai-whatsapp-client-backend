package com.bakchodai.whatsapp.domain.repository;

import com.bakchodai.whatsapp.domain.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Group entity
 * 
 * Provides data access methods for managing groups and their relationships.
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    
    /**
     * Find all active groups
     * 
     * @return List of active groups
     */
    List<Group> findByIsActiveTrue();
    
    /**
     * Find group by name (case-insensitive)
     * 
     * @param name Group name
     * @return Optional containing the group if found
     */
    Optional<Group> findByNameIgnoreCase(String name);
    
    /**
     * Find groups containing characters with specific IDs
     * 
     * @param characterIds List of character IDs
     * @return List of groups containing any of the specified characters
     */
    @Query("SELECT DISTINCT g FROM Group g JOIN g.members m WHERE m.character.id IN :characterIds AND g.isActive = true")
    List<Group> findByCharacterIds(@Param("characterIds") List<UUID> characterIds);
    
    /**
     * Find group with its members and characters
     * 
     * @param groupId Group ID
     * @return Optional containing the group with loaded members
     */
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.character WHERE g.id = :groupId")
    Optional<Group> findByIdWithMembers(@Param("groupId") UUID groupId);
    
    /**
     * Find group with recent messages
     * 
     * @param groupId Group ID
     * @param limit Maximum number of messages to fetch
     * @return Optional containing the group with recent messages
     */
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.messages m LEFT JOIN FETCH m.character WHERE g.id = :groupId ORDER BY m.timestamp DESC")
    Optional<Group> findByIdWithRecentMessages(@Param("groupId") UUID groupId);
    
    /**
     * Check if group exists and is active
     * 
     * @param groupId Group ID
     * @return True if group exists and is active
     */
    boolean existsByIdAndIsActiveTrue(UUID groupId);
    
    /**
     * Count active groups
     * 
     * @return Number of active groups
     */
    long countByIsActiveTrue();
}

