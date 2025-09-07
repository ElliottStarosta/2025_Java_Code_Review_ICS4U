package com.virtualvet.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.virtualvet.dto.ChatResponse;
import com.virtualvet.dto.SessionStartResponse;
import com.virtualvet.entity.AnimalProfile;

@Service
public class ChatUIService {

    @Autowired
    private ChatService chatService;

    
    @Autowired
    private AnimalProfileService animalProfileService;

    public String startNewConversation() {
        try {
            SessionStartResponse response = chatService.startNewConversation();
            return response.getSessionId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start conversation: " + e.getMessage());
        }
    }

    public String sendMessage(String sessionId, String message) {
        try {
            ChatResponse response = chatService.processMessage(sessionId, message, null);
            return response.getResponse();
        } catch (Exception e) {
            return "I apologize, but I'm experiencing technical difficulties. Please try again in a moment.";
        }
    }

    // public String sendMessageWithImage(String sessionId, String message, MultipartFile image) {
    //     try {
    //         ChatResponse response = chatService.processMessage(sessionId, message, image);
    //         return response.getResponse();
    //     } catch (Exception e) {
    //         return "I apologize, but I'm having trouble processing your image. Please try again or describe the issue in words.";
    //     }
    // }

    public String getAnimalProfileSummary(String sessionId) {
        try {
            AnimalProfile profile = animalProfileService.getProfileBySessionId(sessionId);
            return animalProfileService.generateProfileSummary(profile);
        } catch (Exception e) {
            return "Unable to retrieve pet profile information.";
        }
    }

    public boolean updateAnimalProfile(String sessionId, String animalType, String breed, Integer age, Double weight, String symptoms) {
        try {
            AnimalProfile updatedProfile = new AnimalProfile();
            updatedProfile.setAnimalType(animalType);
            updatedProfile.setBreed(breed);
            updatedProfile.setAge(age);
            updatedProfile.setWeight(weight != null ? BigDecimal.valueOf(weight) : null);
            updatedProfile.setSymptoms(symptoms);
            
            AnimalProfile result = animalProfileService.updateProfile(sessionId, updatedProfile);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }
}
