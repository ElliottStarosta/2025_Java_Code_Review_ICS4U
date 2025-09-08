package com.virtualvet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.virtualvet.enums.model.UrgencyLevel;

import java.util.ArrayList;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private UrgencyLevel lastUrgencyLevel = UrgencyLevel.LOW;

    // Constructors
    public Conversation() {
    }

    public Conversation(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    // Getters and Setters
    public UrgencyLevel getLastUrgencyLevel() {
        return lastUrgencyLevel;
    }

    public void setLastUrgencyLevel(UrgencyLevel lastUrgencyLevel) {
        this.lastUrgencyLevel = lastUrgencyLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}