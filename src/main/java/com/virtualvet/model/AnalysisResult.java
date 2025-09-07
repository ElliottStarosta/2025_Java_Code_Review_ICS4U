package com.virtualvet.model;

import java.util.List;

import com.virtualvet.enums.model.UrgencyLevel;

import java.util.ArrayList;

public class AnalysisResult {
    private String condition;
    private double confidence;
    private UrgencyLevel urgency;
    private List<String> observedSymptoms;
    private String description;
    private boolean requiresImmediate;
    private Integer imageIndex;

    public AnalysisResult() {
        this.observedSymptoms = new ArrayList<>();
        this.urgency = UrgencyLevel.LOW;
        this.confidence = 0.0;
    }

    public AnalysisResult(String condition, double confidence, UrgencyLevel urgency) {
        this();
        this.condition = condition;
        this.confidence = confidence;
        this.urgency = urgency;
    }

    // Getters and Setters
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public List<String> getObservedSymptoms() {
        return observedSymptoms;
    }

    public void setObservedSymptoms(List<String> observedSymptoms) {
        this.observedSymptoms = observedSymptoms;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequiresImmediate() {
        return requiresImmediate;
    }

    public void setRequiresImmediate(boolean requiresImmediate) {
        this.requiresImmediate = requiresImmediate;
    }

    public void addObservedSymptom(String symptom) {
        if (!observedSymptoms.contains(symptom)) {
            observedSymptoms.add(symptom);
        }
    }

    public Integer getImageIndex() {
        return imageIndex;
    }

    public void setImageIndex(Integer imageIndex) {
        this.imageIndex = imageIndex;
    }

}
