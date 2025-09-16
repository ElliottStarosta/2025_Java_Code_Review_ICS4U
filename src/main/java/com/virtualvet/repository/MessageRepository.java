package com.virtualvet.repository;

import com.virtualvet.entity.Message;
import com.virtualvet.enums.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing Message entities in the Virtual Vet application.
 * 
 * This repository provides data access methods for message operations including
 * CRUD operations inherited from JpaRepository, as well as custom query methods for
 * finding messages by conversation, session, message type, and timestamp criteria.
 * It supports both simple property-based queries and complex JPQL queries for
 * advanced message retrieval and analysis.
 * 
 * The repository includes methods for retrieving conversation history, filtering
 * messages by type (user vs. AI), finding recent messages, and performing
 * message counting operations for analytics and conversation management.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Finds all messages for a specific conversation ordered by timestamp in ascending order.
     * 
     * Retrieves all messages that belong to the conversation with the specified ID,
     * ordered chronologically from oldest to newest. This method is commonly used
     * for displaying complete conversation history in the correct order.
     * 
     * @param conversationId the ID of the conversation to retrieve messages for
     * @return a list of Message entities ordered by timestamp in ascending order
     */
    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);
    
    /**
     * Finds messages for a specific conversation and message type, ordered by timestamp in descending order.
     * 
     * Retrieves messages that belong to the specified conversation and match the
     * specified message type (USER or BOT), ordered from newest to oldest.
     * This method is useful for finding the most recent messages of a particular type.
     * 
     * @param conversationId the ID of the conversation to search in
     * @param messageType the type of message to filter by (USER or BOT)
     * @return a list of Message entities matching the criteria, ordered by timestamp descending
     */
    List<Message> findByConversationIdAndMessageTypeOrderByTimestampDesc(Long conversationId, MessageType messageType);
    
    /**
     * Finds a limited number of recent messages for a specific conversation.
     * 
     * Retrieves the most recent messages for a conversation, limited to the
     * specified number. The messages are ordered by timestamp in descending order,
     * so the most recent messages appear first in the result list.
     * 
     * @param conversationId the ID of the conversation to retrieve recent messages for
     * @param limit the maximum number of recent messages to retrieve
     * @return a list of Message entities limited to the specified count, ordered by timestamp descending
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.timestamp DESC LIMIT :limit")
    List<Message> findRecentMessagesByConversationId(@Param("conversationId") Long conversationId, @Param("limit") int limit);
    
    /**
     * Finds all messages for a conversation identified by session ID, ordered by timestamp.
     * 
     * Retrieves all messages that belong to a conversation with the specified session ID,
     * ordered chronologically from oldest to newest. This method is useful when working
     * with session-based operations and need to access the complete message history.
     * 
     * @param sessionId the session ID of the conversation to retrieve messages for
     * @return a list of Message entities ordered by timestamp in ascending order
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.sessionId = :sessionId ORDER BY m.timestamp ASC")
    List<Message> findBySessionIdOrderByTimestamp(@Param("sessionId") String sessionId);
    
    /**
     * Finds messages for a conversation by session ID and message type, ordered by timestamp.
     * 
     * Retrieves messages that belong to a conversation with the specified session ID
     * and match the specified message type, ordered from newest to oldest.
     * This method is useful for filtering conversation history by message sender.
     * 
     * @param sessionId the session ID of the conversation to search in
     * @param messageType the type of message to filter by (USER or BOT)
     * @return a list of Message entities matching the criteria, ordered by timestamp descending
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.sessionId = :sessionId AND m.messageType = :messageType ORDER BY m.timestamp DESC")
    List<Message> findBySessionIdAndMessageTypeOrderByTimestampDesc(@Param("sessionId") String sessionId, @Param("messageType") MessageType messageType);
    
    /**
     * Counts messages in a conversation that were created after a specified timestamp.
     * 
     * Returns the number of messages in the specified conversation that have
     * timestamps after the given cutoff time. This method is useful for
     * analytics, determining conversation activity levels, and identifying
     * recent message activity.
     * 
     * @param conversationId the ID of the conversation to count messages for
     * @param timestamp the timestamp after which messages should be counted
     * @return the count of messages created after the specified timestamp
     */
    long countByConversationIdAndTimestampAfter(Long conversationId, LocalDateTime timestamp);
    
    /**
     * Deletes all messages associated with a specific conversation.
     * 
     * Removes all messages that belong to the conversation with the specified ID.
     * This method is typically used during conversation cleanup or when a
     * conversation and its associated messages need to be removed from the system.
     * 
     * @param conversationId the ID of the conversation whose messages should be deleted
     */
    void deleteByConversationId(Long conversationId);
}