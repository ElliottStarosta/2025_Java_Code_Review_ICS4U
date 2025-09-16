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

/**
 * REST Controller for handling emergency veterinary services in the Virtual Vet application.
 * 
 * This controller provides endpoints for finding nearby veterinary clinics and emergency
 * contact information based on geographical coordinates. It supports emergency situations
 * where pet owners need immediate access to veterinary care and contact information.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class EmergencyController {

    /**
     * Logger instance for tracking emergency service operations and errors.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmergencyController.class);

    /**
     * Service for managing emergency veterinary operations and location-based searches.
     * Handles business logic for finding nearby vets and emergency contact information.
     */
    @Autowired
    private EmergencyService emergencyService;

    /**
     * Finds nearby veterinary clinics and emergency contact information based on coordinates.
     * 
     * This endpoint accepts geographical coordinates (latitude and longitude) and searches
     * for veterinary clinics within a specified radius. It also provides emergency contact
     * information relevant to the specified location. The method includes comprehensive
     * input validation and error handling.
     * 
     * @param request Map containing:
     *               - latitude: double value representing the latitude coordinate
     *               - longitude: double value representing the longitude coordinate
     *               - radiusKm: optional integer representing search radius in kilometers (default: 25)
     * @return ResponseEntity containing:
     *         - success: boolean indicating if the search was successful
     *         - nearbyVets: List of VetLocation objects representing found clinics
     *         - emergencyContacts: Map containing emergency contact information
     *         - message: descriptive message about the search results
     * @throws Exception if the search operation fails due to service errors
     */
    @PostMapping("/nearby-vets")
    public ResponseEntity<Map<String, Object>> findNearbyVets(@RequestBody Map<String, Object> request) {
        // Log the incoming request for monitoring and debugging
        logger.info("Received request for nearby vets: {}", request);
        
        try {
            // Validate that request body is not null
            if (request == null) {
                logger.error("Request body is null");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Request body is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate that required coordinate parameters are present
            if (!request.containsKey("latitude") || !request.containsKey("longitude")) {
                logger.error("Missing required parameters: latitude and/or longitude");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Latitude and longitude are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Parse and validate coordinate parameters with proper error handling
            double latitude;
            double longitude;
            int radiusKm;

            try {
                // Extract and convert latitude from request
                latitude = ((Number) request.get("latitude")).doubleValue();
                // Extract and convert longitude from request
                longitude = ((Number) request.get("longitude")).doubleValue();
                // Extract radius with default value of 25km if not provided
                radiusKm = request.containsKey("radiusKm") ? 
                    ((Number) request.get("radiusKm")).intValue() : 25;
            } catch (Exception e) {
                logger.error("Error parsing request parameters: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid parameter format. Latitude and longitude must be numbers.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Log the search parameters for monitoring
            logger.info("Searching for vets near coordinates: {}, {} within {} km", latitude, longitude, radiusKm);

            // Call service methods to find nearby veterinary clinics and emergency contacts
            List<VetLocation> nearbyVets = emergencyService.findNearbyVets(latitude, longitude, radiusKm);
            Map<String, Object> contactInfo = emergencyService.getEmergencyContactInfo(latitude, longitude);
            
            // Create successful response with all relevant information
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("nearbyVets", nearbyVets);
            response.put("emergencyContacts", contactInfo);
            response.put("message", "Found " + nearbyVets.size() + " nearby veterinary clinics");
            
            // Log successful completion
            logger.info("Successfully found {} nearby vets", nearbyVets.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Handle unexpected errors and log them for debugging
            logger.error("Unexpected error in findNearbyVets: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for the emergency service.
     * 
     * This endpoint provides a simple health check to verify that the emergency service
     * is running and responsive. It returns service status information that can be used
     * by monitoring systems and load balancers.
     * 
     * @return ResponseEntity containing:
     *         - status: health status indicator ("healthy")
     *         - service: service name ("emergency")
     *         - message: descriptive health message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        // Create health status response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "emergency");
        response.put("message", "Emergency service is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Simple test endpoint for verifying emergency controller functionality.
     * 
     * This endpoint provides a basic test to confirm that the emergency controller
     * is properly configured and responding to requests. It's useful for debugging
     * and initial service verification.
     * 
     * @return ResponseEntity containing:
     *         - success: boolean indicating controller is working
     *         - message: confirmation message
     *         - timestamp: current system timestamp
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        // Create test response with current timestamp
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Emergency controller is working");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}