package com.virtualvet.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

public class AnimalProfileDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("animalType")
    private String animalType;
    
    @JsonProperty("breed")
    private String breed;
    
    @JsonProperty("age")
    private Integer age;
    
    @JsonProperty("weight")
    private BigDecimal weight;
    
    @JsonProperty("symptoms")
    private List<String> symptoms;
    
    public AnimalProfileDto() {
        this.symptoms = new ArrayList<>();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAnimalType() { return animalType; }
    public void setAnimalType(String animalType) { this.animalType = animalType; }
    
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }
}