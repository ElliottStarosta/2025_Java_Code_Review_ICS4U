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

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = {"http://localhost:8080", "https://localhost:8080"})
public class ImageAnalysisController {

    @Autowired
    private ImageAnalysisService imageAnalysisService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Image file is required"));
            }
            
            AnalysisResult result = imageAnalysisService.analyzeAnimalImage(image);
            
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Image analysis failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateImage(@RequestParam("image") MultipartFile image) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            if (image == null || image.isEmpty()) {
                response.put("valid", false);
                response.put("error", "No image provided");
                return ResponseEntity.badRequest().body(response);
            }
            
            String filename = image.getOriginalFilename();
            boolean isValid = imageAnalysisService.isValidImageFile(filename);
            
            response.put("valid", isValid);
            response.put("filename", filename);
            response.put("size", image.getSize());
            response.put("contentType", image.getContentType());
            
            if (!isValid) {
                response.put("error", "Invalid image format. Supported formats: jpg, jpeg, png, gif, bmp");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Image validation failed: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
