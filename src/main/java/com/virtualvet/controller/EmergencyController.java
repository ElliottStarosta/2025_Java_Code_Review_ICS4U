package com.virtualvet.controller;

import com.virtualvet.service.EmergencyService;
import com.virtualvet.model.VetLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class EmergencyController {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyController.class);

    @Autowired
    private EmergencyService emergencyService;

    @PostMapping("/nearby-vets")
    public ResponseEntity<Map<String, Object>> findNearbyVets(@RequestBody Map<String, Object> request) {
        logger.info("Received request for nearby vets: {}", request);
        
        try {
            // Validate request parameters
            if (request == null) {
                logger.error("Request body is null");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Request body is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!request.containsKey("latitude") || !request.containsKey("longitude")) {
                logger.error("Missing required parameters: latitude and/or longitude");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Latitude and longitude are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Parse parameters with better error handling
            double latitude;
            double longitude;
            int radiusKm;

            try {
                latitude = ((Number) request.get("latitude")).doubleValue();
                longitude = ((Number) request.get("longitude")).doubleValue();
                radiusKm = request.containsKey("radiusKm") ? 
                    ((Number) request.get("radiusKm")).intValue() : 25;
            } catch (Exception e) {
                logger.error("Error parsing request parameters: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid parameter format. Latitude and longitude must be numbers.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            logger.info("Searching for vets near coordinates: {}, {} within {} km", latitude, longitude, radiusKm);

            // Call service methods
            List<VetLocation> nearbyVets = emergencyService.findNearbyVets(latitude, longitude, radiusKm);
            Map<String, Object> contactInfo = emergencyService.getEmergencyContactInfo(latitude, longitude);
            
            // Create successful response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("nearbyVets", nearbyVets);
            response.put("emergencyContacts", contactInfo);
            response.put("message", "Found " + nearbyVets.size() + " nearby veterinary clinics");
            
            logger.info("Successfully found {} nearby vets", nearbyVets.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error in findNearbyVets: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "emergency");
        response.put("message", "Emergency service is running");
        return ResponseEntity.ok(response);
    }

    // Add a simple GET endpoint for testing
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Emergency controller is working");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}