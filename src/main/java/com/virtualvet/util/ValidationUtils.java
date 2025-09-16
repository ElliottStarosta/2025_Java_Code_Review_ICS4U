package com.virtualvet.util;


import java.util.regex.Pattern;

/**
 * Utility class for validating and sanitizing user input in the Virtual Vet application.
 * 
 * This class provides comprehensive validation methods for various types of user input
 * including session IDs, phone numbers, email addresses, and messages. It also includes
 * input sanitization methods to clean and normalize user-provided text to prevent
 * security issues and ensure data consistency.
 * 
 * The class uses pre-compiled regular expressions for efficient validation and includes
 * methods for detecting emergency keywords in user messages to prioritize urgent cases.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class ValidationUtils {

    /** Pattern for validating session ID format (UUID format with hyphens) */
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[a-fA-F0-9-]{36}$");
    
    /** Pattern for validating phone number format (international format) */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    /** Pattern for validating email address format */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Validates whether a session ID matches the expected UUID format.
     * 
     * @param sessionId the session ID string to validate
     * @return true if the session ID is valid, false otherwise
     */
    public static boolean isValidSessionId(String sessionId) {
        return sessionId != null && SESSION_ID_PATTERN.matcher(sessionId).matches();
    }

    /**
     * Validates whether a phone number matches the expected international format.
     * 
     * @param phone the phone number string to validate
     * @return true if the phone number is valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validates whether an email address matches the expected email format.
     * 
     * @param email the email address string to validate
     * @return true if the email address is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates whether a message is valid for processing.
     * 
     * A message is considered valid if it is not null, not empty after trimming,
     * and does not exceed the maximum length limit.
     * 
     * @param message the message string to validate
     * @return true if the message is valid, false otherwise
     */
    public static boolean isValidMessage(String message) {
        return message != null && !message.trim().isEmpty() && message.length() <= 2000;
    }

    /**
     * Sanitizes user input by removing potentially harmful content and normalizing whitespace.
     * 
     * This method removes HTML tags, normalizes whitespace characters, and trims the input
     * to create a clean, safe string for processing. It helps prevent injection attacks
     * and ensures consistent data formatting.
     * 
     * @param input the input string to sanitize
     * @return the sanitized input string, or null if the input was null
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input
            .trim()
            .replaceAll("<[^>]*>", "") // Remove HTML tags
            .replaceAll("[\\r\\n\\t]+", " ") // Replace multiple whitespace with single space
            .replaceAll("\\s+", " "); // Normalize spaces
    }

    /**
     * Checks whether a message contains emergency keywords that indicate urgent veterinary care is needed.
     * 
     * This method analyzes the message content for specific keywords that typically indicate
     * emergency situations requiring immediate veterinary attention. It helps prioritize
     * urgent cases in the chat system.
     * 
     * @param message the message string to check for emergency keywords
     * @return true if emergency keywords are found, false otherwise
     */
    public static boolean isEmergencyKeyword(String message) {
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        String[] emergencyKeywords = {
            "emergency", "urgent", "dying", "unconscious", "bleeding", 
            "choking", "seizure", "convulsing", "not breathing", "collapsed"
        };
        
        for (String keyword : emergencyKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}

