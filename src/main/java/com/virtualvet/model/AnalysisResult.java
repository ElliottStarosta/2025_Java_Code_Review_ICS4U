package com.virtualvet.model;

import java.util.List;

import com.virtualvet.enums.model.UrgencyLevel;

import java.util.ArrayList;

/**
 * Model class representing the result of AI image analysis in the Virtual Vet application.
 * 
 * This class encapsulates the comprehensive results from AI-powered image analysis
 * of pet photos, including detected conditions, confidence levels, urgency assessments,
 * observed symptoms, and treatment recommendations. It serves as the primary data
 * structure for conveying veterinary insights derived from visual analysis.
 * 
 * The class provides utility methods for managing symptoms and includes proper
 * initialization of collections to prevent null pointer exceptions. It supports
 * both individual symptom addition and comprehensive result construction.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class AnalysisResult {
    /**
     * The primary health condition detected in the image analysis.
     * Represents the AI's assessment of what condition or issue
     * was identified in the analyzed pet image.
     */
    private String condition;
    
    /**
     * Confidence level of the analysis result (0.0 to 1.0).
     * Indicates how certain the AI is about its assessment,
     * with 1.0 representing complete confidence and 0.0
     * representing no confidence in the result.
     */
    private double confidence;
    
    /**
     * Urgency level assessment for the detected condition.
     * Indicates how quickly the pet owner should seek veterinary
     * care based on the visual analysis results.
     */
    private UrgencyLevel urgency;
    
    /**
     * List of specific symptoms or signs observed in the image.
     * Contains detailed descriptions of visual indicators that
     * led to the condition assessment and urgency determination.
     */
    private List<String> observedSymptoms;
    
    /**
     * Detailed description of the analysis findings.
     * Provides comprehensive explanation of what was observed,
     * the reasoning behind the assessment, and additional context.
     */
    private String description;
    
    /**
     * Flag indicating whether immediate veterinary attention is required.
     * Determines if the condition warrants emergency care or if
     * routine veterinary consultation is sufficient.
     */
    private boolean requiresImmediate;
    
    /**
     * Index of the image in a multi-image analysis scenario.
     * Used when multiple images are analyzed together to track
     * which specific image this result corresponds to.
     */
    private Integer imageIndex;

    /**
     * Default constructor that initializes the analysis result with default values.
     * Creates a new AnalysisResult with empty symptoms list, LOW urgency level,
     * and zero confidence to ensure safe initialization.
     */
    public AnalysisResult() {
        this.observedSymptoms = new ArrayList<>();
        this.urgency = UrgencyLevel.LOW;
        this.confidence = 0.0;
    }

    /**
     * Constructor that creates an analysis result with the specified core parameters.
     * 
     * @param condition the health condition detected in the analysis
     * @param confidence the confidence level of the analysis (0.0 to 1.0)
     * @param urgency the urgency level assessment for the condition
     */
    public AnalysisResult(String condition, double confidence, UrgencyLevel urgency) {
        this();
        this.condition = condition;
        this.confidence = confidence;
        this.urgency = urgency;
    }

    /**
     * Gets the primary health condition detected in the analysis.
     * 
     * @return the condition string describing the detected health issue
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Sets the primary health condition detected in the analysis.
     * 
     * @param condition the condition string to set
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Gets the confidence level of the analysis result.
     * 
     * @return the confidence level as a double (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Sets the confidence level of the analysis result.
     * 
     * @param confidence the confidence level to set (0.0 to 1.0)
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /**
     * Gets the urgency level assessment for the detected condition.
     * 
     * @return the UrgencyLevel enum value (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public UrgencyLevel getUrgency() {
        return urgency;
    }

    /**
     * Sets the urgency level assessment for the detected condition.
     * 
     * @param urgency the UrgencyLevel enum value to set
     */
    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    /**
     * Gets the list of observed symptoms from the image analysis.
     * 
     * @return the list of symptom strings describing visual indicators
     */
    public List<String> getObservedSymptoms() {
        return observedSymptoms;
    }

    /**
     * Sets the list of observed symptoms from the image analysis.
     * 
     * @param observedSymptoms the list of symptom strings to set
     */
    public void setObservedSymptoms(List<String> observedSymptoms) {
        this.observedSymptoms = observedSymptoms;
    }

    /**
     * Gets the detailed description of the analysis findings.
     * 
     * @return the description string with comprehensive analysis details
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the detailed description of the analysis findings.
     * 
     * @param description the description string to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if immediate veterinary attention is required.
     * 
     * @return true if immediate care is needed, false otherwise
     */
    public boolean isRequiresImmediate() {
        return requiresImmediate;
    }

    /**
     * Sets whether immediate veterinary attention is required.
     * 
     * @param requiresImmediate true if immediate care is needed, false otherwise
     */
    public void setRequiresImmediate(boolean requiresImmediate) {
        this.requiresImmediate = requiresImmediate;
    }

    /**
     * Adds a single symptom to the observed symptoms list if not already present.
     * 
     * This utility method prevents duplicate symptoms by checking if the symptom
     * already exists before adding it to the list.
     * 
     * @param symptom the symptom string to add to the observed symptoms list
     */
    public void addObservedSymptom(String symptom) {
        if (!observedSymptoms.contains(symptom)) {
            observedSymptoms.add(symptom);
        }
    }

    /**
     * Gets the index of the image in a multi-image analysis scenario.
     * 
     * @return the image index, or null if not applicable
     */
    public Integer getImageIndex() {
        return imageIndex;
    }

    /**
     * Sets the index of the image in a multi-image analysis scenario.
     * 
     * @param imageIndex the image index to set
     */
    public void setImageIndex(Integer imageIndex) {
        this.imageIndex = imageIndex;
    }

}