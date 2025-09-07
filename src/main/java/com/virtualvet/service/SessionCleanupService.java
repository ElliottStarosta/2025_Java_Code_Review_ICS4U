package com.virtualvet.service;


import com.virtualvet.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SessionCleanupService {

    @Autowired
    private ConversationRepository conversationRepository;
    
    @Value("${session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;

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

