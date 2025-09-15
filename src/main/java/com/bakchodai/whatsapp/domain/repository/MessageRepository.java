package com.bakchodai.whatsapp.domain.repository;

import com.bakchodai.whatsapp.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Message entity
 * 
 * Provides data access methods for managing messages and conversation history.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    /**
     * Find messages by group ID, ordered by timestamp (oldest first for chat UI)
     * 
     * @param groupId Group ID
     * @param pageable Pagination information
     * @return Page of messages for the group
     */
    Page<Message> findByGroupIdOrderByTimestampAsc(UUID groupId, Pageable pageable);
    
    /**
     * Find recent messages by group ID (oldest first for chat UI)
     * 
     * @param groupId Group ID
     * @param pageable Pagination information
     * @return List of recent messages
     */
    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId ORDER BY m.timestamp ASC")
    List<Message> findRecentMessagesByGroupId(@Param("groupId") UUID groupId, Pageable pageable);
    
    /**
     * Find recent messages by group ID with limit (oldest first for chat UI)
     * 
     * @param groupId Group ID
     * @param limit Maximum number of messages
     * @return List of recent messages
     */
    @Query(value = "SELECT * FROM messages WHERE group_id = :groupId ORDER BY timestamp ASC LIMIT :limit", nativeQuery = true)
    List<Message> findRecentMessagesByGroupIdWithLimit(@Param("groupId") UUID groupId, @Param("limit") int limit);
    
    /**
     * Find messages by group and character
     * 
     * @param groupId Group ID
     * @param characterId Character ID
     * @param pageable Pagination information
     * @return Page of messages from the character in the group
     */
    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId AND m.character.id = :characterId ORDER BY m.timestamp DESC")
    Page<Message> findByGroupIdAndCharacterIdOrderByTimestampDesc(@Param("groupId") UUID groupId, @Param("characterId") UUID characterId, Pageable pageable);
    
    /**
     * Find messages by group within a time range
     * 
     * @param groupId Group ID
     * @param startTime Start time
     * @param endTime End time
     * @return List of messages within the time range
     */
    List<Message> findByGroupIdAndTimestampBetweenOrderByTimestampAsc(UUID groupId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find AI-generated messages by group
     * 
     * @param groupId Group ID
     * @param pageable Pagination information
     * @return Page of AI-generated messages
     */
    Page<Message> findByGroupIdAndIsAiGeneratedTrueOrderByTimestampDesc(UUID groupId, Pageable pageable);
    
    /**
     * Count messages by group
     * 
     * @param groupId Group ID
     * @return Number of messages in the group
     */
    long countByGroupId(UUID groupId);
    
    /**
     * Count messages by group and character
     * 
     * @param groupId Group ID
     * @param characterId Character ID
     * @return Number of messages from the character in the group
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.group.id = :groupId AND m.character.id = :characterId")
    long countByGroupIdAndCharacterId(@Param("groupId") UUID groupId, @Param("characterId") UUID characterId);
    
    /**
     * Find messages containing specific text
     * 
     * @param groupId Group ID
     * @param searchText Text to search for
     * @param pageable Pagination information
     * @return Page of messages containing the search text
     */
    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchText, '%')) ORDER BY m.timestamp DESC")
    Page<Message> findByGroupIdAndContentContainingIgnoreCase(@Param("groupId") UUID groupId, @Param("searchText") String searchText, Pageable pageable);
    
    /**
     * Find the last message in a group
     * 
     * @param groupId Group ID
     * @return Optional containing the last message if found
     */
    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId ORDER BY m.timestamp DESC")
    Message findTopByGroupIdOrderByTimestampDesc(@Param("groupId") UUID groupId);
    
    /**
     * Find messages with their characters loaded
     * 
     * @param groupId Group ID
     * @param pageable Pagination information
     * @return Page of messages with loaded character information
     */
    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.character WHERE m.group.id = :groupId ORDER BY m.timestamp DESC")
    Page<Message> findByGroupIdWithCharacter(@Param("groupId") UUID groupId, Pageable pageable);
    
    /**
     * Delete messages older than specified date
     * 
     * @param cutoffDate Messages older than this date will be deleted
     * @return Number of messages deleted
     */
    long deleteByTimestampBefore(LocalDateTime cutoffDate);
}


