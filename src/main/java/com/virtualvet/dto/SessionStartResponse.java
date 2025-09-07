package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionStartResponse {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("conversationId")
    private Long conversationId;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public SessionStartResponse() {}
    
    public SessionStartResponse(String sessionId, Long conversationId) {
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.success = true;
        this.message = "Session started successfully";
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public static SessionStartResponse error(String message) {
        SessionStartResponse response = new SessionStartResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}