package com.virtualvet.controller;


import com.virtualvet.model.VetLocation;
import com.virtualvet.service.EmergencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class EmergencyController {

    @Autowired
    private EmergencyService emergencyService;

    @GetMapping("/vets")
    public ResponseEntity<List<VetLocation>> getNearbyVets(
            @RequestParam(defaultValue = "0.0") double lat,
            @RequestParam(defaultValue = "0.0") double lng,
            @RequestParam(defaultValue = "25") int radius) {
        
        try {
            List<VetLocation> nearbyVets = emergencyService.findNearbyVets(lat, lng, radius);
            return ResponseEntity.ok(nearbyVets);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of()); // Return empty list on error
        }
    }
    
    @GetMapping("/contact-info")
    public ResponseEntity<Map<String, Object>> getEmergencyContactInfo(
            @RequestParam(defaultValue = "0.0") double lat,
            @RequestParam(defaultValue = "0.0") double lng) {
        
        try {
            Map<String, Object> contactInfo = emergencyService.getEmergencyContactInfo(lat, lng);
            return ResponseEntity.ok(contactInfo);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", "Unable to fetch emergency contact information"));
        }
    }
    
    @GetMapping("/hotline")
    public ResponseEntity<Map<String, String>> getEmergencyHotline() {
        try {
            Map<String, String> response = Map.of(
                "emergencyHotline", emergencyService.getEmergencyHotline(),
                "poisonControl", "1-888-426-4435"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", "Unable to fetch hotline information"));
        }
    }
    
    @GetMapping("/preparation-tips")
    public ResponseEntity<List<String>> getPreparationTips() {
        try {
            List<String> tips = emergencyService.getEmergencyPreparationTips();
            return ResponseEntity.ok(tips);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of("Keep your vet's contact information readily available"));
        }
    }
}
