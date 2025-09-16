package com.virtualvet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.virtualvet.enums.model.UrgencyLevel;

import java.util.ArrayList;

/**
 * JPA Entity representing a conversation session in the Virtual Vet application.
 * 
 * This entity represents a complete chat conversation between a user and the AI
 * veterinary assistant. It maintains session information, tracks conversation
 * activity, stores all related messages, and monitors the urgency level of
 * the conversation for appropriate handling and prioritization.
 * 
 * The entity uses JPA annotations for database mapping and includes proper
 * relationship mappings with Message entities. It features automatic timestamp
 * management and urgency level tracking for conversation prioritization.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    /**
     * Unique identifier for this conversation.
     * Generated automatically by the database using identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique session identifier that links this conversation to a specific chat session.
     * This ID is used by clients to maintain conversation context and is unique
     * across all conversations to prevent session conflicts.
     */
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    /**
     * Timestamp indicating when this conversation was created.
     * Automatically set when the conversation is first created and used for
     * conversation management and cleanup operations.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating the last activity in this conversation.
     * Updated whenever a new message is added to track conversation activity
     * and determine if cleanup is needed for inactive sessions.
     */
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    /**
     * List of all messages in this conversation.
     * Establishes a one-to-many relationship where each conversation can have
     * multiple messages. Uses lazy loading for performance and cascade all
     * for proper cleanup when conversations are deleted.
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();

    /**
     * The highest urgency level encountered in this conversation.
     * Tracks the most critical urgency level from all messages to help
     * prioritize conversations that may require immediate attention.
     * Defaults to LOW when conversation is created.
     */
    @Enumerated(EnumType.STRING)
    private UrgencyLevel lastUrgencyLevel = UrgencyLevel.LOW;

    /**
     * Default constructor required by JPA.
     * Creates a new Conversation instance without any preset values.
     */
    public Conversation() {
    }

    /**
     * Constructor that creates a conversation with the specified session ID.
     * Automatically sets creation and last activity timestamps to the current time.
     * 
     * @param sessionId the unique session identifier for this conversation
     */
    public Conversation(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Gets the highest urgency level encountered in this conversation.
     * 
     * @return the UrgencyLevel enum value (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public UrgencyLevel getLastUrgencyLevel() {
        return lastUrgencyLevel;
    }

    /**
     * Sets the highest urgency level for this conversation.
     * 
     * @param lastUrgencyLevel the UrgencyLevel to set
     */
    public void setLastUrgencyLevel(UrgencyLevel lastUrgencyLevel) {
        this.lastUrgencyLevel = lastUrgencyLevel;
    }

    /**
     * Gets the unique identifier for this conversation.
     * 
     * @return the conversation ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this conversation.
     * 
     * @param id the conversation ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the unique session identifier for this conversation.
     * 
     * @return the session ID that identifies this conversation
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the unique session identifier for this conversation.
     * 
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the timestamp when this conversation was created.
     * 
     * @return the LocalDateTime timestamp of conversation creation
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this conversation was created.
     * 
     * @param createdAt the LocalDateTime timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp of the last activity in this conversation.
     * 
     * @return the LocalDateTime timestamp of the last activity
     */
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    /**
     * Sets the timestamp of the last activity in this conversation.
     * 
     * @param lastActivity the LocalDateTime timestamp to set
     */
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    /**
     * Gets the list of messages in this conversation.
     * 
     * @return the list of Message entities belonging to this conversation
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Sets the list of messages for this conversation.
     * 
     * @param messages the list of Message entities to set
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Updates the last activity timestamp to the current time.
     * 
     * This utility method should be called whenever activity occurs in the
     * conversation, such as when new messages are added. It helps track
     * conversation activity for cleanup and management purposes.
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}