package com.virtualvet.repository;

import com.virtualvet.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for managing Conversation entities in the Virtual Vet application.
 * 
 * This repository provides data access methods for conversation operations including
 * CRUD operations inherited from JpaRepository, as well as custom query methods for
 * finding conversations by session ID, managing inactive conversations, and retrieving
 * conversations with their associated messages. It supports both simple property-based
 * queries and complex JPQL queries with JOIN FETCH for optimized data retrieval.
 * 
 * The repository includes methods for conversation lifecycle management, cleanup operations
 * for inactive sessions, and efficient retrieval of conversations with their message history
 * for comprehensive conversation context.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Finds a conversation by its unique session ID.
     * 
     * Retrieves the conversation that matches the specified session ID.
     * This is the primary method for finding conversations in session-based
     * operations and is commonly used throughout the application.
     * 
     * @param sessionId the unique session identifier to search for
     * @return an Optional containing the Conversation if found, empty otherwise
     */
    Optional<Conversation> findBySessionId(String sessionId);
    
    /**
     * Finds conversations that have been inactive since a specified cutoff time.
     * 
     * Retrieves all conversations where the last activity timestamp is before
     * the specified cutoff time. This method is used for identifying conversations
     * that may be candidates for cleanup or archival due to inactivity.
     * 
     * @param cutoffTime the timestamp before which conversations are considered inactive
     * @return a list of Conversation entities that have been inactive since the cutoff time
     */
    @Query("SELECT c FROM Conversation c WHERE c.lastActivity < :cutoffTime")
    List<Conversation> findInactiveConversations(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Finds a conversation by session ID and eagerly loads its associated messages.
     * 
     * Retrieves a conversation along with all its messages in a single query using
     * JOIN FETCH to avoid N+1 query problems. The messages are ordered by timestamp
     * in ascending order to maintain chronological conversation history.
     * 
     * @param sessionId the unique session identifier to search for
     * @return an Optional containing the Conversation with messages if found, empty otherwise
     */
    @Query("SELECT c FROM Conversation c JOIN FETCH c.messages m WHERE c.sessionId = :sessionId ORDER BY m.timestamp ASC")
    Optional<Conversation> findBySessionIdWithMessages(@Param("sessionId") String sessionId);
    
    /**
     * Deletes all conversations that have been inactive since a specified cutoff time.
     * 
     * Removes all conversations where the last activity timestamp is before the
     * specified cutoff time. This method is typically used for automated cleanup
     * of old, inactive conversations to manage database storage and maintain
     * system performance.
     * 
     * @param cutoffTime the timestamp before which conversations should be deleted
     */
    void deleteByLastActivityBefore(LocalDateTime cutoffTime);
}