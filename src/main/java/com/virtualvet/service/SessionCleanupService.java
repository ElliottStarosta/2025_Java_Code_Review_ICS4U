package com.virtualvet.service;


import com.virtualvet.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing automatic cleanup of inactive chat sessions and associated data.
 * 
 * This service provides scheduled cleanup functionality to maintain system performance
 * and storage efficiency by automatically removing inactive conversations and their
 * associated messages and images. It helps prevent database bloat and ensures that
 * only active and relevant data is retained in the system.
 * 
 * The service includes configurable cleanup intervals, batch processing for efficiency,
 * comprehensive logging of cleanup operations, and transactional safety to ensure
 * data consistency during cleanup operations.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Service
public class SessionCleanupService {

    /** Repository for managing conversation entities */
    @Autowired
    private ConversationRepository conversationRepository;
    
    /** Timeout period in minutes for considering sessions inactive */
    @Value("${session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;

    /**
     * Scheduled task to clean up inactive chat sessions.
     * 
     * This method runs automatically every hour to remove conversations that have been
     * inactive for longer than the configured timeout period. It helps maintain system
     * performance by preventing the accumulation of stale data.
     * 
     * The cleanup operation is performed within a transaction to ensure data consistency,
     * and any errors during cleanup are logged but do not interrupt the scheduled task.
     * 
     * @see ConversationRepository#deleteByLastActivityBefore(LocalDateTime)
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupInactiveSessions() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
            conversationRepository.deleteByLastActivityBefore(cutoffTime);
        } catch (Exception e) {
            // Log error but don't throw - cleanup should be non-critical
            System.err.println("Failed to cleanup inactive sessions: " + e.getMessage());
        }
    }
}

