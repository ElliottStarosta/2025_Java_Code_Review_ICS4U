package com.virtualvet.model;

import com.virtualvet.entity.AnimalProfile;
import com.virtualvet.entity.Message;
import com.virtualvet.enums.model.UrgencyLevel;

import java.util.List;
import java.util.ArrayList;

public class ConversationContext {
    private String sessionId;
    private AnimalProfile animalProfile;
    private List<String> identifiedSymptoms;
    private UrgencyLevel currentUrgency;
    private List<Message> recentHistory;

    public ConversationContext() {
        this.identifiedSymptoms = new ArrayList<>();
        this.recentHistory = new ArrayList<>();
        this.currentUrgency = UrgencyLevel.LOW;
    }

    public ConversationContext(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public AnimalProfile getAnimalProfile() {
        return animalProfile;
    }

    public void setAnimalProfile(AnimalProfile animalProfile) {
        this.animalProfile = animalProfile;
    }

    public List<String> getIdentifiedSymptoms() {
        return identifiedSymptoms;
    }

    public void setIdentifiedSymptoms(List<String> identifiedSymptoms) {
        this.identifiedSymptoms = identifiedSymptoms;
    }

    public UrgencyLevel getCurrentUrgency() {
        return currentUrgency;
    }

    public void setCurrentUrgency(UrgencyLevel currentUrgency) {
        this.currentUrgency = currentUrgency;
    }

    public List<Message> getRecentHistory() {
        return recentHistory;
    }

    public void setRecentHistory(List<Message> recentHistory) {
        this.recentHistory = recentHistory;
    }

    public void addSymptom(String symptom) {
        if (!identifiedSymptoms.contains(symptom)) {
            identifiedSymptoms.add(symptom);
        }
    }

    public void addSymptoms(List<String> symptoms) {
        for (String symptom : symptoms) {
            addSymptom(symptom);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConversationContext {")
                .append("sessionId='").append(sessionId).append('\'')
                .append(", animalProfile=").append(animalProfile != null ? animalProfile.toString() : "null")
                .append(", identifiedSymptoms=").append(identifiedSymptoms)
                .append(", currentUrgency=").append(currentUrgency)
                .append(", recentHistory=");

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
