package com.virtualvet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.virtualvet.enums.entity.MessageType;

/**
 * JPA Entity representing an individual message in a conversation within the Virtual Vet application.
 * 
 * This entity represents a single message exchange between a user and the AI veterinary
 * assistant. It contains the message content, type (user or AI), timestamp, optional
 * image URL, and urgency level assessment. Messages are linked to conversations to
 * maintain proper conversation context and history.
 * 
 * The entity uses JPA annotations for database mapping, including proper column
 * specifications and relationship mappings. It features automatic timestamp management
 * and supports both text and image content for comprehensive veterinary consultations.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Entity
@Table(name = "messages")
public class Message {

    /**
     * Unique identifier for this message.
     * Generated automatically by the database using identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The conversation this message belongs to.
     * Establishes a many-to-one relationship where multiple messages
     * belong to a single conversation for proper conversation threading.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * Type of message indicating the sender (USER or AI).
     * This field determines how the message is displayed and processed,
     * helping maintain conversation flow and context awareness.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    /**
     * The actual text content of the message.
     * Stored as LOB (Large Object) to accommodate potentially lengthy
     * message content from AI responses or detailed user descriptions.
     */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * Timestamp indicating when this message was created or sent.
     * Used for message ordering, conversation history display, and audit trails.
     * Automatically set when the message is created.
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * URL or path to an image attached to this message.
     * Used for displaying images in the chat interface when messages include
     * visual content for veterinary consultation purposes.
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Urgency level associated with this message content.
     * Indicates the priority or urgency of the information conveyed in the message,
     * helping users and the system understand the importance of the veterinary advice provided.
     */
    @Column(name = "urgency_level")
    private String urgencyLevel;

    /**
     * Default constructor required by JPA.
     * Creates a new Message instance without any preset values.
     */
    public Message() {
    }

    /**
     * Constructor that creates a message with the specified conversation, type, and content.
     * Automatically sets the timestamp to the current time.
     * 
     * @param conversation the conversation this message belongs to
     * @param messageType the type of message (USER or AI)
     * @param content the text content of the message
     */
    public Message(Conversation conversation, MessageType messageType, String content) {
        this.conversation = conversation;
        this.messageType = messageType;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Gets the unique identifier for this message.
     * 
     * @return the message ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this message.
     * 
     * @param id the message ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the conversation this message belongs to.
     * 
     * @return the conversation entity this message is associated with
     */
    public Conversation getConversation() {
        return conversation;
    }

    /**
     * Sets the conversation for this message.
     * 
     * @param conversation the conversation entity to associate with this message
     */
    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    /**
     * Gets the type of message indicating the sender.
     * 
     * @return the MessageType enum value (USER or AI)
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Sets the type of message indicating the sender.
     * 
     * @param messageType the MessageType enum value to set
     */
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Gets the text content of this message.
     * 
     * @return the message content text
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the text content of this message.
     * 
     * @param content the message content text to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the timestamp when this message was created.
     * 
     * @return the LocalDateTime timestamp of message creation
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for this message.
     * 
     * @param timestamp the LocalDateTime timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the URL or path to an image attached to this message.
     * 
     * @return the image URL, or null if no image is attached
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the URL or path to an image attached to this message.
     * 
     * @param imageUrl the image URL to set
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gets the urgency level associated with this message.
     * 
     * @return the urgency level string (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")
     */
    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    /**
     * Sets the urgency level associated with this message.
     * 
     * @param urgencyLevel the urgency level string to set
     */
    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    /**
     * Generates a string representation of this message.
     * 
     * Creates a formatted string containing all message information including
     * the conversation ID for reference. Uses safe null checking to prevent
     * null pointer exceptions in the toString() output.
     * 
     * @return a formatted string representation of the message
     */
    @Override
    public String toString() {
        return "Message {" +
                "id=" + id +
                ", conversationId=" + (conversation != null ? conversation.getId() : "null") +
                ", messageType=" + messageType +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", imageUrl='" + imageUrl + '\'' +
                ", urgencyLevel='" + urgencyLevel + '\'' +
                '}';
    }

}