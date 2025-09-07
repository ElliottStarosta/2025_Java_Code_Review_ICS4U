package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ChatRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("imageBase64")
    private String imageBase64;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    public ChatRequest() {}
    
    public ChatRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean hasImage() {
        return imageBase64 != null && !imageBase64.trim().isEmpty();
    }
}

