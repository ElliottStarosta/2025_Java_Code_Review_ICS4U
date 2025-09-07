package com.virtualvet.util;


import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[a-fA-F0-9-]{36}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static boolean isValidSessionId(String sessionId) {
        return sessionId != null && SESSION_ID_PATTERN.matcher(sessionId).matches();
    }

    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidMessage(String message) {
        return message != null && !message.trim().isEmpty() && message.length() <= 2000;
    }

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

