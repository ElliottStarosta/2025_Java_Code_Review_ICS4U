package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

public class ConversationHistoryResponse {
    
    @JsonProperty("messages")
    private List<MessageDto> messages;
    
    @JsonProperty("animalProfile")
    private AnimalProfileDto animalProfile;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("totalMessages")
    private int totalMessages;
    
    @JsonProperty("success")
    private boolean success;
    
    public ConversationHistoryResponse() {
        this.messages = new ArrayList<>();
        this.success = true;
    }
    
    // Getters and Setters
    public List<MessageDto> getMessages() { return messages; }
    public void setMessages(List<MessageDto> messages) { this.messages = messages; }
    
    public AnimalProfileDto getAnimalProfile() { return animalProfile; }
    public void setAnimalProfile(AnimalProfileDto animalProfile) { this.animalProfile = animalProfile; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public int getTotalMessages() { return totalMessages; }
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}

