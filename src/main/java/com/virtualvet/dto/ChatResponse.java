package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.model.VetLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ChatResponse {

    @JsonProperty("response")
    private String response;

    @JsonProperty("urgencyLevel")
    private UrgencyLevel urgencyLevel;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    @JsonProperty("needsEmergencyCare")
    private boolean needsEmergencyCare;

    @JsonProperty("nearbyVets")
    private List<VetLocation> nearbyVets;

    @JsonProperty("conversationContext")
    private Map<String, Object> conversationContext;

    @JsonProperty("error")
    private boolean error;

    @JsonProperty("errorMessage")
    private String errorMessage;

    private StructuredVetResponse structuredData;

    public ChatResponse() {
        this.recommendations = new ArrayList<>();
        this.nearbyVets = new ArrayList<>();
        this.conversationContext = new HashMap<>();
        this.urgencyLevel = UrgencyLevel.LOW;
    }

    public ChatResponse(String response) {
        this();
        this.response = response;
    }

    // Getters and Setters
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
        this.needsEmergencyCare = urgencyLevel.isEmergency();
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public boolean isNeedsEmergencyCare() {
        return needsEmergencyCare;
    }

    public void setNeedsEmergencyCare(boolean needsEmergencyCare) {
        this.needsEmergencyCare = needsEmergencyCare;
    }

    public List<VetLocation> getNearbyVets() {
        return nearbyVets;
    }

    public void setNearbyVets(List<VetLocation> nearbyVets) {
        this.nearbyVets = nearbyVets;
    }

    public Map<String, Object> getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(Map<String, Object> conversationContext) {
        this.conversationContext = conversationContext;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.error = errorMessage != null;
    }

    public StructuredVetResponse getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(StructuredVetResponse structuredData) {
        this.structuredData = structuredData;
    }

    public void addRecommendation(String recommendation) {
        if (!recommendations.contains(recommendation)) {
            recommendations.add(recommendation);
        }
    }

    public void addContextValue(String key, Object value) {
        conversationContext.put(key, value);
    }

    public static ChatResponse error(String message) {
        ChatResponse response = new ChatResponse();
        response.setErrorMessage(message);
        response.setResponse("I'm sorry, I encountered an error while processing your request. Please try again.");
        return response;
    }
}
