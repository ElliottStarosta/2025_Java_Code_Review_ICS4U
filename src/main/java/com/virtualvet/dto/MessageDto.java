package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.virtualvet.enums.entity.MessageType;
import java.time.LocalDateTime;

public class MessageDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("messageType")
    private MessageType messageType;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    @JsonProperty("urgencyLevel")
    private String urgencyLevel;
    
    public MessageDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
}
