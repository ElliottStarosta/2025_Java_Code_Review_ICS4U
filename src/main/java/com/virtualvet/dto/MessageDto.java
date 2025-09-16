package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.virtualvet.enums.entity.MessageType;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for individual chat messages in the Virtual Vet application.
 * 
 * This DTO represents a single message within a conversation, containing the message
 * content, type (user or AI), timestamp, optional image URL, and urgency level.
 * It provides a standardized format for transferring message data between the client
 * and server layers, enabling consistent message handling and display.
 * 
 * The class uses Jackson annotations for JSON serialization/deserialization and
 * includes all necessary metadata for message display and conversation management.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class MessageDto {
    
    /**
     * Unique identifier for this message.
     * This is typically set by the database when the message is persisted
     * and used for message tracking and management.
     */
    @JsonProperty("id")
    private Long id;
    
    /**
     * Type of message indicating the sender (USER or AI).
     * This field determines how the message is displayed in the chat interface
     * and helps maintain conversation flow and context.
     */
    @JsonProperty("messageType")
    private MessageType messageType;
    
    /**
     * The actual text content of the message.
     * This is the primary content that will be displayed to users in the chat interface.
     */
    @JsonProperty("content")
    private String content;
    
    /**
     * Timestamp indicating when this message was created or sent.
     * Used for message ordering, conversation history display, and audit trails.
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * URL or path to an image attached to this message.
     * Used for displaying images in the chat interface when messages include
     * visual content for veterinary consultation purposes.
     */
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    /**
     * Urgency level associated with this message content.
     * Indicates the priority or urgency of the information conveyed in the message,
     * helping users understand the importance of the veterinary advice provided.
     */
    @JsonProperty("urgencyLevel")
    private String urgencyLevel;
    
    /**
     * Default constructor that creates an empty message DTO.
     * Initializes a new message without any preset values.
     */
    public MessageDto() {}
    
    /**
     * Gets the unique identifier for this message.
     * 
     * @return the message ID, or null if not yet persisted
     */
    public Long getId() { return id; }
    
    /**
     * Sets the unique identifier for this message.
     * 
     * @param id the message ID to set
     */
    public void setId(Long id) { this.id = id; }
    
    /**
     * Gets the type of message indicating the sender.
     * 
     * @return the MessageType enum value (USER or AI)
     */
    public MessageType getMessageType() { return messageType; }
    
    /**
     * Sets the type of message indicating the sender.
     * 
     * @param messageType the MessageType enum value to set
     */
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    
    /**
     * Gets the text content of this message.
     * 
     * @return the message content text
     */
    public String getContent() { return content; }
    
    /**
     * Sets the text content of this message.
     * 
     * @param content the message content text to set
     */
    public void setContent(String content) { this.content = content; }
    
    /**
     * Gets the timestamp when this message was created.
     * 
     * @return the LocalDateTime timestamp of message creation
     */
    public LocalDateTime getTimestamp() { return timestamp; }
    
    /**
     * Sets the timestamp for this message.
     * 
     * @param timestamp the LocalDateTime timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    /**
     * Gets the URL or path to an image attached to this message.
     * 
     * @return the image URL, or null if no image is attached
     */
    public String getImageUrl() { return imageUrl; }
    
    /**
     * Sets the URL or path to an image attached to this message.
     * 
     * @param imageUrl the image URL to set
     */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    /**
     * Gets the urgency level associated with this message.
     * 
     * @return the urgency level string (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")
     */
    public String getUrgencyLevel() { return urgencyLevel; }
    
    /**
     * Sets the urgency level associated with this message.
     * 
     * @param urgencyLevel the urgency level string to set
     */
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
}