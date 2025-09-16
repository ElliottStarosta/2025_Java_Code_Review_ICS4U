package com.virtualvet.controller;

import com.virtualvet.dto.*;
import com.virtualvet.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
/**
 * Controller for managing chat-related endpoints.
 * Handles chat requests, message history, and session management.
 */
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/start")
    /**
     * Starts a new chat conversation session.
     * @return ResponseEntity with session start response
     */
    public ResponseEntity<SessionStartResponse> startConversation() {
        try {
            SessionStartResponse response = chatService.startNewConversation();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SessionStartResponse.error("Failed to start conversation: " + e.getMessage()));
        }
    }

    @PostMapping("/message")
    /**
     * Processes a chat message from the user, with optional images.
     * @param sessionId the session identifier
     * @param message the user's message
     * @param images optional image attachments
     * @return ResponseEntity with chat response
     */
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("message") String message,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ChatResponse.error("Session ID is required"));
            }
            
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ChatResponse.error("Message cannot be empty"));
            }
            
            ChatResponse response = chatService.processMessage(sessionId, message, images);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ChatResponse.error("Failed to process message: " + e.getMessage()));
        }
    /**
     * Retrieves the conversation history for a session.
     * @param sessionId the session identifier
     * @return ResponseEntity with conversation history
     */
    }



    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ConversationHistoryResponse> getConversationHistory(@PathVariable String sessionId) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                ConversationHistoryResponse errorResponse = new ConversationHistoryResponse();
                errorResponse.setSuccess(false);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            ConversationHistoryResponse response = chatService.getConversationHistory(sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ConversationHistoryResponse errorResponse = new ConversationHistoryResponse();
    /**
     * Health check endpoint for the chat service.
     * @return ResponseEntity with health status
     */
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chat service is running");
    }
}
