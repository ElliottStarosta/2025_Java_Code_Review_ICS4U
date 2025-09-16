package com.virtualvet.controller;


import com.virtualvet.model.AnalysisResult;
import com.virtualvet.service.ImageAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for handling image analysis operations in the Virtual Vet application.
 * 
 * This controller provides endpoints for analyzing animal images to detect health conditions,
 * validate image formats, and provide veterinary insights. It supports various image formats
 * and returns structured analysis results including condition detection, confidence levels,
 * and urgency assessments.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class ImageAnalysisController {

    /**
     * Service for managing image analysis operations and AI-powered veterinary insights.
     * Handles business logic for image processing, condition detection, and analysis result generation.
     */
    @Autowired
    private ImageAnalysisService imageAnalysisService;

    /**
     * Analyzes an uploaded animal image to detect potential health conditions.
     * 
     * This endpoint accepts an image file and processes it through AI analysis to detect
     * potential health conditions, symptoms, and urgency levels. The analysis provides
     * veterinary insights that can help pet owners understand their pet's condition
     * and determine if immediate veterinary attention is required.
     * 
     * @param image the MultipartFile containing the animal image to analyze
     * @return ResponseEntity containing:
     *         - condition: detected health condition or "Normal" if no issues found
     *         - confidence: confidence level of the analysis (0.0 to 1.0)
     *         - urgencyLevel: urgency assessment (LOW, MEDIUM, HIGH, CRITICAL)
     *         - description: detailed description of findings
     *         - observedSymptoms: list of observed symptoms in the image
     *         - requiresImmediate: boolean indicating if immediate veterinary care is needed
     *         - success: boolean indicating if analysis was successful
     * @throws Exception if image analysis fails due to service errors
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestParam("image") MultipartFile image) {
        // Attempt to analyze the provided image
        try {
            // Validate that an image file was provided
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Image file is required"));
            }
            
            // Process the image through the analysis service
            AnalysisResult result = imageAnalysisService.analyzeAnimalImage(image);
            
            // Build response map with analysis results
            Map<String, Object> response = new HashMap<>();
            response.put("condition", result.getCondition());
            response.put("confidence", result.getConfidence());
            response.put("urgencyLevel", result.getUrgency());
            response.put("description", result.getDescription());
            response.put("observedSymptoms", result.getObservedSymptoms());
            response.put("requiresImmediate", result.isRequiresImmediate());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Handle any errors during image analysis
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Image analysis failed: " + e.getMessage()));
        }
    }
    
    /**
     * Validates an uploaded image file format and properties.
     * 
     * This endpoint checks if the provided image file is in a supported format and
     * validates its basic properties such as size and content type. It helps ensure
     * that only appropriate image files are processed by the analysis service.
     * 
     * @param image the MultipartFile containing the image to validate
     * @return ResponseEntity containing:
     *         - valid: boolean indicating if the image format is supported
     *         - filename: original filename of the uploaded image
     *         - size: file size in bytes
     *         - contentType: MIME type of the image file
     *         - error: error message if validation fails
     * @throws Exception if image validation fails due to service errors
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateImage(@RequestParam("image") MultipartFile image) {
        // Attempt to validate the provided image
        try {
            // Initialize response map for validation results
            Map<String, Object> response = new HashMap<>();
            
            // Check if image file was provided
            if (image == null || image.isEmpty()) {
                response.put("valid", false);
                response.put("error", "No image provided");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Extract filename and validate format
            String filename = image.getOriginalFilename();
            boolean isValid = imageAnalysisService.isValidImageFile(filename);
            
            // Populate response with file information
            response.put("valid", isValid);
            response.put("filename", filename);
            response.put("size", image.getSize());
            response.put("contentType", image.getContentType());
            
            // Add error message if validation failed
            if (!isValid) {
                response.put("error", "Invalid image format. Supported formats: jpg, jpeg, png, gif, bmp");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Handle any errors during image validation
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Image validation failed: " + e.getMessage()));
        }
    }
    
    /**
     * Creates a standardized error response map for consistent error handling.
     * 
     * This utility method creates a uniform error response structure that includes
     * success status and error message. It ensures consistent error formatting
     * across all controller endpoints.
     * 
     * @param message the error message to include in the response
     * @return Map containing standardized error response structure
     */
    private Map<String, Object> createErrorResponse(String message) {
        // Create error response map with standard structure
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}