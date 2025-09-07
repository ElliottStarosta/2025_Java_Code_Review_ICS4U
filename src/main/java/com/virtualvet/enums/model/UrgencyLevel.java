package com.virtualvet.enums.model;

public enum UrgencyLevel {
    LOW("Low", "#4CAF50", "Monitor and schedule routine visit if symptoms persist"),
    MEDIUM("Medium", "#FF9800", "Schedule veterinary visit within 24-48 hours"),
    HIGH("High", "#F44336", "Seek veterinary attention within 2-6 hours"),
    CRITICAL("Critical", "#D32F2F", "Seek immediate emergency veterinary care");
    
    private final String displayName;
    private final String colorCode;
    private final String recommendation;
    
    UrgencyLevel(String displayName, String colorCode, String recommendation) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.recommendation = recommendation;
    }
    
    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public String getRecommendation() { return recommendation; }
    
    public boolean isEmergency() {
        return this == HIGH || this == CRITICAL;
    }
    
    public boolean requiresImmediateCare() {
        return this == CRITICAL;
    }
}
