package com.virtualvet.repository;

import com.virtualvet.entity.Message;
import com.virtualvet.enums.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);
    
    List<Message> findByConversationIdAndMessageTypeOrderByTimestampDesc(Long conversationId, MessageType messageType);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.timestamp DESC LIMIT :limit")
    List<Message> findRecentMessagesByConversationId(@Param("conversationId") Long conversationId, @Param("limit") int limit);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.sessionId = :sessionId ORDER BY m.timestamp ASC")
    List<Message> findBySessionIdOrderByTimestamp(@Param("sessionId") String sessionId);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.sessionId = :sessionId AND m.messageType = :messageType ORDER BY m.timestamp DESC")
    List<Message> findBySessionIdAndMessageTypeOrderByTimestampDesc(@Param("sessionId") String sessionId, @Param("messageType") MessageType messageType);
    
    long countByConversationIdAndTimestampAfter(Long conversationId, LocalDateTime timestamp);
    
    void deleteByConversationId(Long conversationId);
}
