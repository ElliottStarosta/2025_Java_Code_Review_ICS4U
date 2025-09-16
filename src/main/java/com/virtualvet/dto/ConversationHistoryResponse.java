package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Transfer Object (DTO) for conversation history responses in the Virtual Vet application.
 * 
 * This DTO represents the complete conversation history for a chat session, including
 * all messages exchanged between the user and the AI, associated animal profile
 * information, and session metadata. It provides a comprehensive view of the
 * conversation context for both display purposes and conversation continuity.
 * 
 * The class includes message count tracking, success status indication, and proper
 * initialization of collections to prevent null pointer exceptions. It uses Jackson
 * annotations for JSON serialization and deserialization.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class ConversationHistoryResponse {
    
    /**
     * List of all messages in the conversation, ordered chronologically.
     * Each MessageDto contains the message content, type, timestamp, and other metadata
     * necessary for displaying the complete conversation history.
     */
    @JsonProperty("messages")
    private List<MessageDto> messages;
    
    /**
     * Animal profile information associated with this conversation session.
     * Contains the pet's details that were collected during the conversation,
     * providing context for the veterinary advice given.
     */
    @JsonProperty("animalProfile")
    private AnimalProfileDto animalProfile;
    
    /**
     * Unique session identifier that links this conversation to a specific chat session.
     * Used for maintaining conversation continuity and session management.
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * Total number of messages in the conversation.
     * Provides a quick count of conversation activity and can be used for
     * pagination or display purposes.
     */
    @JsonProperty("totalMessages")
    private int totalMessages;
    
    /**
     * Flag indicating whether the history retrieval was successful.
     * Used to distinguish between successful responses and error conditions
     * in the API response handling.
     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * Default constructor that initializes the messages list and sets success to true.
     * Creates a new conversation history response with empty message list and
     * successful status by default.
     */
    public ConversationHistoryResponse() {
        this.messages = new ArrayList<>();
        this.success = true;
    }
    
    /**
     * Gets the list of messages in this conversation.
     * 
     * @return the list of MessageDto objects representing the conversation history
     */
    public List<MessageDto> getMessages() { return messages; }
    
    /**
     * Sets the list of messages for this conversation history.
     * 
     * @param messages the list of MessageDto objects to set
     */
    public void setMessages(List<MessageDto> messages) { this.messages = messages; }
    
    /**
     * Gets the animal profile associated with this conversation.
     * 
     * @return the AnimalProfileDto containing pet information, or null if not available
     */
    public AnimalProfileDto getAnimalProfile() { return animalProfile; }
    
    /**
     * Sets the animal profile for this conversation history.
     * 
     * @param animalProfile the AnimalProfileDto to associate with this conversation
     */
    public void setAnimalProfile(AnimalProfileDto animalProfile) { this.animalProfile = animalProfile; }
    
    /**
     * Gets the unique session identifier for this conversation.
     * 
     * @return the session ID that identifies this conversation
     */
    public String getSessionId() { return sessionId; }
    
    /**
     * Sets the unique session identifier for this conversation.
     * 
     * @param sessionId the session ID to set for this conversation
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    /**
     * Gets the total number of messages in this conversation.
     * 
     * @return the count of messages in the conversation
     */
    public int getTotalMessages() { return totalMessages; }
    
    /**
     * Sets the total number of messages in this conversation.
     * 
     * @param totalMessages the message count to set
     */
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }
    
    /**
     * Checks if the history retrieval was successful.
     * 
     * @return true if the operation was successful, false if an error occurred
     */
    public boolean isSuccess() { return success; }
    
    /**
     * Sets the success status for this conversation history response.
     * 
     * @param success true if the operation was successful, false if an error occurred
     */
    public void setSuccess(boolean success) { this.success = success; }
}