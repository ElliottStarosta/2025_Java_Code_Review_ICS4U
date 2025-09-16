package com.virtualvet.enums.model;

/**
 * Enumeration representing urgency levels for veterinary situations in the Virtual Vet application.
 * 
 * This enum defines four levels of urgency for pet health conditions, ranging from low
 * priority situations that can be monitored at home to critical emergencies requiring
 * immediate veterinary attention. Each urgency level includes display information,
 * color coding for UI presentation, and specific recommendations for pet owners.
 * 
 * The enum provides utility methods for determining if a situation constitutes an
 * emergency or requires immediate care, making it useful for automated decision-making
 * and user interface prioritization in the veterinary consultation system.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public enum UrgencyLevel {
    /**
     * Low urgency level for minor concerns that can be monitored.
     * Represents situations that are not immediately concerning and can be
     * observed at home with routine veterinary follow-up if symptoms persist.
     */
    LOW("Low", "#4CAF50", "Monitor and schedule routine visit if symptoms persist"),
    
    /**
     * Medium urgency level for situations requiring veterinary attention within 24-48 hours.
     * Represents conditions that should be evaluated by a veterinarian but do not
     * require immediate emergency care.
     */
    MEDIUM("Medium", "#FF9800", "Schedule veterinary visit within 24-48 hours"),
    
    /**
     * High urgency level for situations requiring prompt veterinary attention.
     * Represents conditions that need veterinary evaluation within a few hours
     * but may not require emergency room treatment.
     */
    HIGH("High", "#F44336", "Seek veterinary attention within 2-6 hours"),
    
    /**
     * Critical urgency level for emergency situations requiring immediate care.
     * Represents life-threatening conditions that require immediate emergency
     * veterinary intervention without delay.
     */
    CRITICAL("Critical", "#D32F2F", "Seek immediate emergency veterinary care");
    
    /**
     * Human-readable display name for this urgency level.
     * Used in user interfaces and reports for clear communication to pet owners.
     */
    private final String displayName;
    
    /**
     * Hexadecimal color code for UI presentation.
     * Used for visual indicators, status displays, and highlighting
     * urgency levels in the user interface.
     */
    private final String colorCode;
    
    /**
     * Specific recommendation text for this urgency level.
     * Provides clear guidance to pet owners about the appropriate
     * course of action for their pet's condition.
     */
    private final String recommendation;
    
    /**
     * Constructor for UrgencyLevel enum values.
     * 
     * @param displayName the human-readable display name
     * @param colorCode the hexadecimal color code for UI presentation
     * @param recommendation the specific recommendation text for pet owners
     */
    UrgencyLevel(String displayName, String colorCode, String recommendation) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.recommendation = recommendation;
    }
    
    /**
     * Gets the human-readable display name for this urgency level.
     * 
     * @return the display name string
     */
    public String getDisplayName() { return displayName; }
    
    /**
     * Gets the hexadecimal color code for this urgency level.
     * 
     * @return the color code string in hexadecimal format
     */
    public String getColorCode() { return colorCode; }
    
    /**
     * Gets the specific recommendation text for this urgency level.
     * 
     * @return the recommendation string with guidance for pet owners
     */
    public String getRecommendation() { return recommendation; }
    
    /**
     * Determines if this urgency level constitutes an emergency situation.
     * 
     * Emergency situations are defined as HIGH or CRITICAL urgency levels
     * that require prompt veterinary attention.
     * 
     * @return true if this is an emergency situation (HIGH or CRITICAL), false otherwise
     */
    public boolean isEmergency() {
        return this == HIGH || this == CRITICAL;
    }
    
    /**
     * Determines if this urgency level requires immediate veterinary care.
     * 
     * Only CRITICAL urgency level requires immediate care, while other levels
     * can be scheduled or monitored as appropriate.
     * 
     * @return true if immediate care is required (CRITICAL only), false otherwise
     */
    public boolean requiresImmediateCare() {
        return this == CRITICAL;
    }
}