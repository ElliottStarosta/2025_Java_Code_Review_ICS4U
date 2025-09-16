package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for chat request messages in the Virtual Vet application.
 * 
 * This DTO represents a user's chat message request, including the session context,
 * message content, and optional image attachments. It is used for transferring chat
 * request data from the client to the server, enabling the AI to process user
 * messages and provide appropriate veterinary responses.
 * 
 * The class includes timestamp tracking for message ordering and a utility method
 * to check for image attachments. It uses Jackson annotations for proper JSON
 * serialization and deserialization.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class ChatRequest {
    
    /**
     * Unique session identifier that links this message to a specific conversation.
     * The session ID is used to maintain conversation context and retrieve
     * message history for continuity in the chat experience.
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * The text content of the user's message.
     * This is the primary content that the AI will process to provide
     * veterinary advice and responses.
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Optional base64-encoded image data attached to the message.
     * Images can be used for visual analysis of pet conditions and
     * provide additional context for veterinary consultations.
     */
    @JsonProperty("imageBase64")
    private String imageBase64;
    
    /**
     * Timestamp indicating when this chat request was created.
     * Used for message ordering, conversation history, and audit trails.
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Default constructor that initializes the request with current timestamp.
     * Creates a new chat request without any preset values.
     */
    public ChatRequest() {}
    
    /**
     * Constructor that creates a chat request with session ID and message content.
     * Automatically sets the timestamp to the current time.
     * 
     * @param sessionId the unique session identifier for the conversation
     * @param message the text content of the user's message
     */
    public ChatRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Gets the unique session identifier for this chat request.
     * 
     * @return the session ID that links this message to a conversation
     */
    public String getSessionId() { return sessionId; }
    
    /**
     * Sets the unique session identifier for this chat request.
     * 
     * @param sessionId the session ID to associate with this request
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    /**
     * Gets the text content of the user's message.
     * 
     * @return the message text content
     */
    public String getMessage() { return message; }
    
    /**
     * Sets the text content of the user's message.
     * 
     * @param message the message text content to set
     */
    public void setMessage(String message) { this.message = message; }
    
    /**
     * Gets the base64-encoded image data attached to this message.
     * 
     * @return the base64 image data, or null if no image is attached
     */
    public String getImageBase64() { return imageBase64; }
    
    /**
     * Sets the base64-encoded image data for this message.
     * 
     * @param imageBase64 the base64 image data to attach to the message
     */
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    
    /**
     * Gets the timestamp when this chat request was created.
     * 
     * @return the LocalDateTime timestamp of the request creation
     */
    public LocalDateTime getTimestamp() { return timestamp; }
    
    /**
     * Sets the timestamp for this chat request.
     * 
     * @param timestamp the LocalDateTime timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    /**
     * Checks if this chat request has an image attachment.
     * 
     * This utility method determines whether the request includes image data
     * by checking if the imageBase64 field is not null and not empty.
     * 
     * @return true if an image is attached, false otherwise
     */
    public boolean hasImage() {
        return imageBase64 != null && !imageBase64.trim().isEmpty();
    }
}
