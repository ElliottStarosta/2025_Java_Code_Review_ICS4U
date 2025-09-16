package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.model.VetLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object (DTO) for chat response messages in the Virtual Vet application.
 * 
 * This DTO represents the AI's response to a user's chat message, including the
 * response text, urgency level, recommendations, nearby veterinary locations, and
 * conversation context. It provides a comprehensive structure for delivering
 * veterinary advice and emergency information to pet owners.
 * 
 * The class includes error handling capabilities, utility methods for adding
 * recommendations and context, and factory methods for creating error responses.
 * It uses Jackson annotations for proper JSON serialization and deserialization.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class ChatResponse {

    /**
     * The AI-generated response text that will be displayed to the user.
     * This is the primary content of the veterinary consultation response.
     */
    @JsonProperty("response")
    private String response;

    /**
     * The urgency level assessment for the pet's condition.
     * Indicates how quickly the pet owner should seek veterinary care.
     * Defaults to LOW if not specified.
     */
    @JsonProperty("urgencyLevel")
    private UrgencyLevel urgencyLevel;

    /**
     * List of veterinary recommendations based on the conversation.
     * These are actionable suggestions for the pet owner to follow.
     */
    @JsonProperty("recommendations")
    private List<String> recommendations;

    /**
     * List of nearby veterinary clinics and emergency services.
     * Provides location-based veterinary resources when needed.
     */
    @JsonProperty("nearbyVets")
    private List<VetLocation> nearbyVets;

    /**
     * Map containing conversation context and additional metadata.
     * Stores information that can be used for maintaining conversation flow
     * and providing contextual responses.
     */
    @JsonProperty("conversationContext")
    private Map<String, Object> conversationContext;

    /**
     * Flag indicating whether this response represents an error condition.
     * When true, the errorMessage field should contain the error details.
     */
    @JsonProperty("error")
    private boolean error;

    /**
     * Error message describing what went wrong during processing.
     * This field is populated when the error flag is set to true.
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Structured veterinary response containing detailed analysis and recommendations.
     * This provides a more organized format for complex veterinary consultations.
     */
    private StructuredVetResponse structuredData;

    /**
     * Default constructor that initializes all collections and sets default values.
     * Creates a new chat response with empty lists, default urgency level, and
     * error flag set to false.
     */
    public ChatResponse() {
        this.recommendations = new ArrayList<>();
        this.nearbyVets = new ArrayList<>();
        this.conversationContext = new HashMap<>();
        this.urgencyLevel = UrgencyLevel.LOW;
    }

    /**
     * Constructor that creates a chat response with the specified response text.
     * Initializes all other fields with default values.
     * 
     * @param response the AI-generated response text to include
     */
    public ChatResponse(String response) {
        this();
        this.response = response;
    }

    /**
     * Gets the AI-generated response text.
     * 
     * @return the response text content
     */
    public String getResponse() {
        return response;
    }

    /**
     * Sets the AI-generated response text.
     * 
     * @param response the response text content to set
     */
    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * Gets the urgency level assessment for the pet's condition.
     * 
     * @return the UrgencyLevel enum value (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    /**
     * Sets the urgency level assessment for the pet's condition.
     * 
     * @param urgencyLevel the UrgencyLevel to set
     */
    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    /**
     * Gets the list of veterinary recommendations.
     * 
     * @return the list of recommendation strings
     */
    public List<String> getRecommendations() {
        return recommendations;
    }

    /**
     * Sets the list of veterinary recommendations.
     * 
     * @param recommendations the list of recommendation strings to set
     */
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    /**
     * Gets the list of nearby veterinary locations.
     * 
     * @return the list of VetLocation objects
     */
    public List<VetLocation> getNearbyVets() {
        return nearbyVets;
    }

    /**
     * Sets the list of nearby veterinary locations.
     * 
     * @param nearbyVets the list of VetLocation objects to set
     */
    public void setNearbyVets(List<VetLocation> nearbyVets) {
        this.nearbyVets = nearbyVets;
    }

    /**
     * Gets the conversation context map.
     * 
     * @return the map containing conversation context and metadata
     */
    public Map<String, Object> getConversationContext() {
        return conversationContext;
    }

    /**
     * Sets the conversation context map.
     * 
     * @param conversationContext the map containing context information
     */
    public void setConversationContext(Map<String, Object> conversationContext) {
        this.conversationContext = conversationContext;
    }

    /**
     * Checks if this response represents an error condition.
     * 
     * @return true if this is an error response, false otherwise
     */
    public boolean isError() {
        return error;
    }

    /**
     * Sets the error flag for this response.
     * 
     * @param error true if this is an error response, false otherwise
     */
    public void setError(boolean error) {
        this.error = error;
    }

    /**
     * Gets the error message for this response.
     * 
     * @return the error message, or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for this response.
     * Automatically sets the error flag to true if a non-null message is provided.
     * 
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.error = errorMessage != null;
    }

    /**
     * Gets the structured veterinary response data.
     * 
     * @return the StructuredVetResponse object containing detailed analysis
     */
    public StructuredVetResponse getStructuredData() {
        return structuredData;
    }

    /**
     * Sets the structured veterinary response data.
     * 
     * @param structuredData the StructuredVetResponse object to set
     */
    public void setStructuredData(StructuredVetResponse structuredData) {
        this.structuredData = structuredData;
    }

    /**
     * Adds a recommendation to the list if it's not already present.
     * 
     * This utility method prevents duplicate recommendations by checking if
     * the recommendation already exists before adding it to the list.
     * 
     * @param recommendation the recommendation string to add
     */
    public void addRecommendation(String recommendation) {
        if (!recommendations.contains(recommendation)) {
            recommendations.add(recommendation);
        }
    }

    /**
     * Adds a key-value pair to the conversation context.
     * 
     * This utility method allows for easy addition of context information
     * that can be used in subsequent conversation interactions.
     * 
     * @param key the context key
     * @param value the context value
     */
    public void addContextValue(String key, Object value) {
        conversationContext.put(key, value);
    }

    /**
     * Factory method for creating an error response.
     * 
     * Creates a new ChatResponse instance with error information and a
     * user-friendly error message. This is useful for handling exceptions
     * and providing consistent error responses to clients.
     * 
     * @param message the error message describing what went wrong
     * @return a new ChatResponse instance configured as an error response
     */
    public static ChatResponse error(String message) {
        ChatResponse response = new ChatResponse();
        response.setErrorMessage(message);
        response.setResponse("I'm sorry, I encountered an error while processing your request. Please try again.");
        return response;
    }
}