package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object (DTO) for session start responses in the Virtual Vet application.
 * 
 * This DTO represents the response returned when a new chat session is successfully
 * initiated. It contains the unique session identifier, conversation ID, success status,
 * and descriptive message. It provides clients with the necessary information to
 * continue the conversation and track the session state.
 * 
 * The class includes a factory method for creating error responses and uses Jackson
 * annotations for proper JSON serialization and deserialization. It serves as the
 * confirmation mechanism for session initialization operations.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class SessionStartResponse {
    
    /**
     * Unique session identifier generated for this new chat session.
     * This ID is used by clients to maintain conversation context and
     * associate subsequent messages with this specific session.
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * Database identifier for the conversation record.
     * This ID links the session to the persistent conversation entity
     * stored in the database for history and management purposes.
     */
    @JsonProperty("conversationId")
    private Long conversationId;
    
    /**
     * Flag indicating whether the session start operation was successful.
     * Used by clients to determine if they should proceed with the session
     * or handle an error condition.
     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * Descriptive message providing information about the session start result.
     * Contains either a success confirmation or error details depending on
     * the operation outcome.
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Default constructor that creates an empty session start response.
     * Initializes a new response without any preset values.
     */
    public SessionStartResponse() {}
    
    /**
     * Constructor that creates a successful session start response.
     * 
     * Creates a new SessionStartResponse with the provided session ID and conversation ID,
     * setting the success flag to true and including a default success message.
     * 
     * @param sessionId the unique session identifier for the new session
     * @param conversationId the database ID of the conversation record
     */
    public SessionStartResponse(String sessionId, Long conversationId) {
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.success = true;
        this.message = "Session started successfully";
    }
    
    /**
     * Gets the unique session identifier for this response.
     * 
     * @return the session ID that identifies the new chat session
     */
    public String getSessionId() { return sessionId; }
    
    /**
     * Sets the unique session identifier for this response.
     * 
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    /**
     * Gets the database conversation identifier.
     * 
     * @return the conversation ID that links to the database record
     */
    public Long getConversationId() { return conversationId; }
    
    /**
     * Sets the database conversation identifier.
     * 
     * @param conversationId the conversation ID to set
     */
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    
    /**
     * Checks if the session start operation was successful.
     * 
     * @return true if the session was started successfully, false otherwise
     */
    public boolean isSuccess() { return success; }
    
    /**
     * Sets the success status for this response.
     * 
     * @param success true if the operation was successful, false if an error occurred
     */
    public void setSuccess(boolean success) { this.success = success; }
    
    /**
     * Gets the descriptive message for this response.
     * 
     * @return the message describing the session start result
     */
    public String getMessage() { return message; }
    
    /**
     * Sets the descriptive message for this response.
     * 
     * @param message the message to set describing the session start result
     */
    public void setMessage(String message) { this.message = message; }
    
    /**
     * Factory method for creating an error response.
     * 
     * Creates a new SessionStartResponse instance configured as an error response
     * with the specified error message. This is useful for handling exceptions
     * and providing consistent error responses to clients when session creation fails.
     * 
     * @param message the error message describing what went wrong during session creation
     * @return a new SessionStartResponse instance configured as an error response
     */
    public static SessionStartResponse error(String message) {
        SessionStartResponse response = new SessionStartResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}