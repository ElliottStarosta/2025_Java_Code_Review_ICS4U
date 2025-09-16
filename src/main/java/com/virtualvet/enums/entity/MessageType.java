package com.virtualvet.enums.entity;

/**
 * Enumeration representing the type of message sender in the Virtual Vet application.
 * 
 * This enum defines the two possible types of message senders in the chat system:
 * USER for messages sent by pet owners and BOT for messages sent by the AI
 * veterinary assistant. It is used throughout the application to distinguish
 * between user input and AI responses for proper message handling, display,
 * and conversation flow management.
 * 
 * The enum values are used in JPA entities, DTOs, and service layers to
 * maintain consistency in message type identification across the application.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public enum MessageType {
    /**
     * Message sent by the pet owner (user).
     * Represents input from the pet owner including questions, descriptions
     * of symptoms, or responses to AI inquiries.
     */
    USER,
    
    /**
     * Message sent by the AI veterinary assistant (bot).
     * Represents responses from the AI system including veterinary advice,
     * questions, recommendations, and analysis results.
     */
    BOT
}