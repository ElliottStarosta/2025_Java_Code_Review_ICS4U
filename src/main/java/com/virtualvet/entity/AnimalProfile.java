package com.virtualvet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "animal_profiles")
public class AnimalProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Column(name = "animal_type")
    private String animalType;
    
    @Column(name = "breed")
    private String breed;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;
    
    @Lob
    @Column(name = "symptoms")
    private String symptoms;
    
    // Constructors
    public AnimalProfile() {}
    
    public AnimalProfile(Conversation conversation) {
        this.conversation = conversation;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    
    public String getAnimalType() { return animalType; }
    public void setAnimalType(String animalType) { this.animalType = animalType; }
    
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
}