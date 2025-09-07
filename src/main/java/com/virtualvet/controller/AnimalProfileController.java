package com.virtualvet.controller;


import com.virtualvet.dto.AnimalProfileDto;
import com.virtualvet.entity.AnimalProfile;
import com.virtualvet.service.AnimalProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class AnimalProfileController {

    @Autowired
    private AnimalProfileService animalProfileService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String sessionId) {
        try {
            AnimalProfile profile = animalProfileService.getProfileBySessionId(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            
            if (profile != null) {
                response.put("profile", convertToDto(profile));
                response.put("isComplete", animalProfileService.isProfileComplete(profile));
                response.put("summary", animalProfileService.generateProfileSummary(profile));
                response.put("missingFields", animalProfileService.getMissingProfileFields(profile));
            } else {
                response.put("profile", null);
                response.put("isComplete", false);
                response.put("summary", "No pet information available yet.");
                response.put("missingFields", List.of("All profile information"));
            }
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to retrieve profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PutMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable String sessionId, 
            @RequestBody AnimalProfileDto profileDto) {
        
        try {
            AnimalProfile profile = convertFromDto(profileDto);
            AnimalProfile updatedProfile = animalProfileService.updateProfile(sessionId, profile);
            
            Map<String, Object> response = new HashMap<>();
            
            if (updatedProfile != null) {
                response.put("profile", convertToDto(updatedProfile));
                response.put("isComplete", animalProfileService.isProfileComplete(updatedProfile));
                response.put("summary", animalProfileService.generateProfileSummary(updatedProfile));
                response.put("success", true);
            } else {
                response.put("success", false);
                response.put("error", "Failed to update profile");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private AnimalProfileDto convertToDto(AnimalProfile profile) {
        AnimalProfileDto dto = new AnimalProfileDto();
        dto.setId(profile.getId());
        dto.setAnimalType(profile.getAnimalType());
        dto.setBreed(profile.getBreed());
        dto.setAge(profile.getAge());
        dto.setWeight(profile.getWeight());
        
        if (profile.getSymptoms() != null && !profile.getSymptoms().trim().isEmpty()) {
            dto.setSymptoms(List.of(profile.getSymptoms().split(",\\s*")));
        }
        
        return dto;
    }
    
    private AnimalProfile convertFromDto(AnimalProfileDto dto) {
        AnimalProfile profile = new AnimalProfile();
        profile.setAnimalType(dto.getAnimalType());
        profile.setBreed(dto.getBreed());
        profile.setAge(dto.getAge());
        profile.setWeight(dto.getWeight());
        
        if (dto.getSymptoms() != null && !dto.getSymptoms().isEmpty()) {
            profile.setSymptoms(String.join(", ", dto.getSymptoms()));
        }
        
        return profile;
    }
}
