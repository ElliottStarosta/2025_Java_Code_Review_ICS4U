package com.virtualvet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * JPA Entity representing an animal profile in the Virtual Vet application.
 * 
 * This entity stores comprehensive information about a pet including species, breed,
 * age, weight, and observed symptoms. It is associated with a conversation to maintain
 * context throughout the veterinary consultation session. The profile information
 * helps the AI provide species-specific and breed-appropriate veterinary advice.
 * 
 * The entity uses JPA annotations for database mapping, including proper column
 * specifications for data types and precision. The symptoms field uses LOB (Large Object)
 * type to accommodate potentially lengthy symptom descriptions.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Entity
@Table(name = "animal_profiles")
public class AnimalProfile {
    
    /**
     * Unique identifier for this animal profile.
     * Generated automatically by the database using identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The conversation this animal profile is associated with.
     * Establishes a many-to-one relationship where multiple profiles
     * could theoretically belong to one conversation, though typically
     * there will be one profile per conversation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    /**
     * Type or species of the animal (e.g., "Dog", "Cat", "Bird", "Rabbit").
     * This field is crucial for providing species-specific veterinary advice
     * and determining appropriate treatment recommendations.
     */
    @Column(name = "animal_type")
    private String animalType;
    
    /**
     * Breed of the animal (e.g., "Golden Retriever", "Persian", "Budgie").
     * Breed information can be important for breed-specific health considerations,
     * genetic predispositions, and appropriate care guidelines.
     */
    @Column(name = "breed")
    private String breed;
    
    /**
     * Age of the animal in years.
     * Age is a critical factor for determining appropriate care recommendations,
     * understanding age-related health concerns, and adjusting treatment protocols.
     */
    @Column(name = "age")
    private Integer age;
    
    /**
     * Weight of the animal with precision for decimal values.
     * Weight is important for dosage calculations, health assessments,
     * and determining appropriate medication dosages. Uses BigDecimal
     * for precise decimal handling with 5 total digits and 2 decimal places.
     */
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;
    
    /**
     * Observed symptoms or health concerns reported by the pet owner.
     * Stored as a LOB (Large Object) to accommodate potentially lengthy
     * symptom descriptions. Symptoms are typically stored as comma-separated
     * values or in a structured format for easy parsing.
     */
    @Lob
    @Column(name = "symptoms")
    private String symptoms;
    
    /**
     * Default constructor required by JPA.
     * Creates a new AnimalProfile instance without any preset values.
     */
    public AnimalProfile() {}
    
    /**
     * Constructor that creates an animal profile associated with a specific conversation.
     * 
     * @param conversation the conversation this profile belongs to
     */
    public AnimalProfile(Conversation conversation) {
        this.conversation = conversation;
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
     * Gets the conversation associated with this animal profile.
     * 
     * @return the conversation entity this profile belongs to
     */
    public Conversation getConversation() { return conversation; }
    
    /**
     * Sets the conversation for this animal profile.
     * 
     * @param conversation the conversation entity to associate with this profile
     */
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    
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
     * Gets the observed symptoms for this animal.
     * 
     * @return the symptoms as a string, typically comma-separated values
     */
    public String getSymptoms() { return symptoms; }
    
    /**
     * Sets the observed symptoms for this animal.
     * 
     * @param symptoms the symptoms string to set
     */
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    /**
     * Generates a string representation of this animal profile.
     * 
     * Creates a formatted string containing all profile information except
     * the conversation reference to avoid potential circular references
     * in toString() output.
     * 
     * @return a formatted string representation of the animal profile
     */
    @Override
    public String toString() {
        return "AnimalProfile{" +
                "id=" + id +
                ", animalType='" + animalType + '\'' +
                ", breed='" + breed + '\'' +
                ", age=" + age +
                ", weight=" + weight +
                ", symptoms='" + symptoms + '\'' +
                '}';
    }
}