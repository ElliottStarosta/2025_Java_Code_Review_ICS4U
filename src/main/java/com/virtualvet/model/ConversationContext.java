package com.virtualvet.model;

import com.virtualvet.entity.AnimalProfile;
import com.virtualvet.entity.Message;
import com.virtualvet.enums.model.UrgencyLevel;

import java.util.List;
import java.util.ArrayList;

/**
 * Model class representing the context and state of a conversation in the Virtual Vet application.
 * 
 * This class maintains the comprehensive context of an ongoing veterinary consultation,
 * including animal profile information, identified symptoms, current urgency level,
 * and recent conversation history. It serves as the primary data structure for
 * maintaining conversation continuity and providing context-aware AI responses.
 * 
 * The class provides utility methods for managing symptoms and includes defensive
 * programming practices to prevent null pointer exceptions. It supports both
 * individual symptom addition and bulk symptom management for comprehensive
 * conversation state tracking.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class ConversationContext {
    /**
     * Unique session identifier that links this context to a specific conversation.
     * Used for maintaining conversation continuity and session management.
     */
    private String sessionId;
    
    /**
     * Animal profile information associated with this conversation.
     * Contains the pet's details that have been collected during the consultation,
     * providing essential context for AI responses and veterinary advice.
     */
    private AnimalProfile animalProfile;
    
    /**
     * List of symptoms identified during the conversation.
     * Tracks all symptoms mentioned or detected throughout the consultation
     * to build a comprehensive understanding of the pet's condition.
     */
    private List<String> identifiedSymptoms;
    
    /**
     * Current urgency level assessment for the conversation.
     * Represents the highest urgency level encountered so far in the consultation,
     * helping prioritize the conversation and determine appropriate response urgency.
     */
    private UrgencyLevel currentUrgency;
    
    /**
     * Recent message history for context awareness.
     * Contains the most recent messages from the conversation to provide
     * context for AI responses and maintain conversation flow.
     */
    private List<Message> recentHistory;

    /**
     * Default constructor that initializes the conversation context with default values.
     * Creates a new ConversationContext with empty collections, LOW urgency level,
     * and no session ID to ensure safe initialization.
     */
    public ConversationContext() {
        this.identifiedSymptoms = new ArrayList<>();
        this.recentHistory = new ArrayList<>();
        this.currentUrgency = UrgencyLevel.LOW;
    }

    /**
     * Constructor that creates a conversation context with the specified session ID.
     * 
     * @param sessionId the unique session identifier for this conversation context
     */
    public ConversationContext(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    /**
     * Gets the unique session identifier for this conversation context.
     * 
     * @return the session ID that identifies this conversation
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the unique session identifier for this conversation context.
     * 
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the animal profile associated with this conversation context.
     * 
     * Returns a new empty AnimalProfile if none is set to prevent null pointer exceptions.
     * This defensive programming approach ensures safe access to profile information.
     * 
     * @return the animal profile entity, or a new empty profile if none is set
     */
    public AnimalProfile getAnimalProfile() {
        return animalProfile != null ? animalProfile : new AnimalProfile();
    }

    /**
     * Sets the animal profile for this conversation context.
     * 
     * @param animalProfile the animal profile entity to associate with this context
     */
    public void setAnimalProfile(AnimalProfile animalProfile) {
        this.animalProfile = animalProfile;
    }

    /**
     * Gets the list of identified symptoms in this conversation.
     * 
     * Returns a new empty list if none is set to prevent null pointer exceptions.
     * This defensive programming approach ensures safe access to symptoms list.
     * 
     * @return the list of identified symptoms, or an empty list if none is set
     */
    public List<String> getIdentifiedSymptoms() {
        return identifiedSymptoms != null ? identifiedSymptoms : new ArrayList<>();
    }

    /**
     * Sets the list of identified symptoms for this conversation context.
     * 
     * @param identifiedSymptoms the list of symptom strings to set
     */
    public void setIdentifiedSymptoms(List<String> identifiedSymptoms) {
        this.identifiedSymptoms = identifiedSymptoms;
    }

    /**
     * Gets the current urgency level for this conversation.
     * 
     * Returns LOW urgency level if none is set to prevent null pointer exceptions.
     * This defensive programming approach ensures safe access to urgency information.
     * 
     * @return the current urgency level, or LOW if none is set
     */
    public UrgencyLevel getCurrentUrgency() {
        return currentUrgency != null ? currentUrgency : UrgencyLevel.LOW;
    }

    /**
     * Sets the current urgency level for this conversation context.
     * 
     * @param currentUrgency the urgency level to set
     */
    public void setCurrentUrgency(UrgencyLevel currentUrgency) {
        this.currentUrgency = currentUrgency;
    }

    /**
     * Gets the recent message history for this conversation context.
     * 
     * Returns a new empty list if none is set to prevent null pointer exceptions.
     * This defensive programming approach ensures safe access to message history.
     * 
     * @return the list of recent messages, or an empty list if none is set
     */
    public List<Message> getRecentHistory() {
        return recentHistory != null ? recentHistory : new ArrayList<>();
    }

    /**
     * Sets the recent message history for this conversation context.
     * 
     * @param recentHistory the list of recent messages to set
     */
    public void setRecentHistory(List<Message> recentHistory) {
        this.recentHistory = recentHistory;
    }

    /**
     * Adds a single symptom to the identified symptoms list if not already present.
     * 
     * This utility method prevents duplicate symptoms by checking if the symptom
     * already exists before adding it to the list.
     * 
     * @param symptom the symptom string to add to the identified symptoms list
     */
    public void addSymptom(String symptom) {
        if (!identifiedSymptoms.contains(symptom)) {
            identifiedSymptoms.add(symptom);
        }
    }

    /**
     * Adds multiple symptoms to the identified symptoms list.
     * 
     * This utility method processes a list of symptoms and adds each one
     * individually, ensuring no duplicates are added to the symptoms list.
     * 
     * @param symptoms the list of symptom strings to add
     */
    public void addSymptoms(List<String> symptoms) {
        for (String symptom : symptoms) {
            addSymptom(symptom);
        }
    }

    /**
     * Generates a comprehensive string representation of this conversation context.
     * 
     * Creates a detailed string representation including all context information
     * with proper null checking to prevent null pointer exceptions. The output
     * includes session ID, animal profile, symptoms, urgency level, and message history.
     * 
     * @return a formatted string representation of the conversation context
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConversationContext {")
                .append("sessionId='").append(sessionId).append('\'')
                .append(", animalProfile=").append(animalProfile != null ? animalProfile.toString() : "null")
                .append(", identifiedSymptoms=").append(identifiedSymptoms)
                .append(", currentUrgency=").append(currentUrgency)
                .append(", recentHistory=");

        // Safely format the recent history list
        if (recentHistory != null && !recentHistory.isEmpty()) {
            sb.append("[");
            for (int i = 0; i < recentHistory.size(); i++) {
                sb.append(recentHistory.get(i) != null ? recentHistory.get(i).toString() : "null");
                if (i < recentHistory.size() - 1)
                    sb.append(", ");
            }
            sb.append("]");
        } else {
            sb.append("[]");
        }

        sb.append("}");
        return sb.toString();
    }

}