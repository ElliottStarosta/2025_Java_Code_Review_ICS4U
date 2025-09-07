package com.virtualvet.repository;

import com.virtualvet.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    Optional<Conversation> findBySessionId(String sessionId);
    
    @Query("SELECT c FROM Conversation c WHERE c.lastActivity < :cutoffTime")
    List<Conversation> findInactiveConversations(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT c FROM Conversation c JOIN FETCH c.messages m WHERE c.sessionId = :sessionId ORDER BY m.timestamp ASC")
    Optional<Conversation> findBySessionIdWithMessages(@Param("sessionId") String sessionId);
    
    void deleteByLastActivityBefore(LocalDateTime cutoffTime);
}
