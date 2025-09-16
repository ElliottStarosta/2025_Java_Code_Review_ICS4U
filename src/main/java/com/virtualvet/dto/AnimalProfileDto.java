package com.virtualvet.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Transfer Object (DTO) for animal profile information in the Virtual Vet application.
 * 
 * This DTO represents the essential information about a pet including species, breed,
 * age, weight, and observed symptoms. It is used for transferring animal profile data
 * between the client and server layers, providing a clean interface for profile
 * management operations.
 * 
 * The class uses Jackson annotations for JSON serialization/deserialization and
 * includes proper initialization of collections to prevent null pointer exceptions.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class AnimalProfileDto {
    
    /**
     * Unique identifier for the animal profile.
     * This is typically set by the database when the profile is persisted.
     */
    @JsonProperty("id")
    private Long id;
    
    /**
     * Type or species of the animal (e.g., "Dog", "Cat", "Bird", "Rabbit").
     * This field helps the AI provide species-specific veterinary advice.
     */
    @JsonProperty("animalType")
    private String animalType;
    
    /**
     * Breed of the animal (e.g., "Golden Retriever", "Persian", "Budgie").
     * Breed information can be important for breed-specific health considerations.
     */
    @JsonProperty("breed")
    private String breed;
    
    /**
     * Age of the animal in years.
     * Age is crucial for determining appropriate care recommendations and
     * understanding age-related health concerns.
     */
    @JsonProperty("age")
    private Integer age;
    
    /**
     * Weight of the animal in appropriate units (typically kg or lbs).
     * Weight is important for dosage calculations and health assessments.
     * Uses BigDecimal for precise decimal handling.
     */
    @JsonProperty("weight")
    private BigDecimal weight;
    
    /**
     * List of observed symptoms or health concerns reported by the pet owner.
     * Each symptom is stored as a separate string entry in the list.
     * This information helps the AI provide targeted health advice.
     */
    @JsonProperty("symptoms")
    private List<String> symptoms;
    
    /**
     * Default constructor that initializes the symptoms list.
     * Ensures that the symptoms list is never null, preventing null pointer exceptions
     * when adding or accessing symptoms.
     */
    public AnimalProfileDto() {
        this.symptoms = new ArrayList<>();
    }
    
    /**
     * Gets the unique identifier for this animal profile.
     * 
     * @return the profile ID, or null if not yet persisted
     */
    public Long getId() { return id; }
    
    /**
     * Sets the unique identifier for this animal profile.
     * 
     * @param id the profile ID to set
     */
    public void setId(Long id) { this.id = id; }
    
    /**
     * Gets the type or species of the animal.
     * 
     * @return the animal type (e.g., "Dog", "Cat", "Bird")
     */
    public String getAnimalType() { return animalType; }
    
    /**
     * Sets the type or species of the animal.
     * 
     * @param animalType the animal type to set
     */
    public void setAnimalType(String animalType) { this.animalType = animalType; }
    
    /**
     * Gets the breed of the animal.
     * 
     * @return the breed name, or null if not specified
     */
    public String getBreed() { return breed; }
    
    /**
     * Sets the breed of the animal.
     * 
     * @param breed the breed name to set
     */
    public void setBreed(String breed) { this.breed = breed; }
    
    /**
     * Gets the age of the animal in years.
     * 
     * @return the age in years, or null if not specified
     */
    public Integer getAge() { return age; }
    
    /**
     * Sets the age of the animal in years.
     * 
     * @param age the age in years to set
     */
    public void setAge(Integer age) { this.age = age; }
    
    /**
     * Gets the weight of the animal.
     * 
     * @return the weight as BigDecimal for precise decimal handling
     */
    public BigDecimal getWeight() { return weight; }
    
    /**
     * Sets the weight of the animal.
     * 
     * @param weight the weight to set as BigDecimal
     */
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    /**
     * Gets the list of observed symptoms.
     * 
     * @return the list of symptoms, never null (initialized as empty list)
     */
    public List<String> getSymptoms() { return symptoms; }
    
    /**
     * Sets the list of observed symptoms.
     * 
     * @param symptoms the list of symptoms to set
     */
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }
}