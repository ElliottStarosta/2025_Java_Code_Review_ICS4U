package com.virtualvet.service;

import com.virtualvet.model.*;
import com.virtualvet.config.AIServiceConfig;
import com.virtualvet.enums.model.UrgencyLevel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.Duration;

/**
 * Service for analyzing animal images using AI-powered visual assessment tools.
 * 
 * This service provides comprehensive image analysis capabilities for veterinary
 * consultation purposes, including AI-powered visual assessment, symptom detection,
 * and health condition identification. It integrates with both local VQA services
 * and external AI APIs to provide detailed analysis of pet images.
 * 
 * The service includes multiple analysis methods with fallback mechanisms, image
 * validation and storage, urgency level assessment, and comprehensive reporting
 * of visual findings to assist in veterinary decision-making.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Service
public class ImageAnalysisService {

    @Autowired
    private AIServiceConfig aiServiceConfig;

    @Value("${image.storage.path:./uploads}")
    private String imageStoragePath;

    @Value("${ai.vqa.service.url:http://127.0.0.1:5000}")
    private String vqaServiceUrl;

    @Value("${ai.vqa.enabled:true}")
    private boolean vqaEnabled;

    @Value("${ai.vqa.timeout:25}")
    private int vqaTimeoutSeconds;

    @Value("${ai.image-analysis.api-token:}")
    private String huggingFaceToken;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public ImageAnalysisService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(vqaTimeoutSeconds))
                .build();
    }

    public AnalysisResult analyzeAnimalImage(MultipartFile image) {
        try {
            validateImage(image);
            System.out.println("Starting comprehensive veterinary image analysis...");

            if (vqaEnabled) {
                try {
                    // First check if VQA service is available
                    if (isVQAServiceHealthy()) {
                        return analyzeWithLocalVQA(image);
                    } else {
                        System.err.println("Local VQA service is not healthy - falling back to Hugging Face");
                        return analyzeWithHuggingFace(image);
                    }
                } catch (Exception e) {
                    System.err.println("Local VQA analysis failed: " + e.getMessage());
                    return analyzeWithHuggingFace(image);
                }
            } else {
                System.out.println("VQA disabled - using Hugging Face fallback");
                return analyzeWithHuggingFace(image);
            }

        } catch (Exception e) {
            System.err.println("Complete image analysis failed: " + e.getMessage());
            return createFallbackResult(e.getMessage());
        }
    }

    private boolean isVQAServiceHealthy() {
        try {
            String healthUrl = vqaServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode healthData = objectMapper.readTree(response.getBody());
                return "healthy".equals(healthData.path("status").asText()) &&
                        healthData.path("models_loaded").asBoolean();
            }
            return false;
        } catch (Exception e) {
            System.err.println("VQA service health check failed: " + e.getMessage());
            return false;
        }
    }

    private AnalysisResult analyzeWithLocalVQA(MultipartFile image) throws Exception {
        System.out.println("Starting local VQA analysis...");

        String base64Image = Base64.getEncoder().encodeToString(image.getBytes());

        // Call the comprehensive analysis endpoint
        String analyzeUrl = vqaServiceUrl + "/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_base64", base64Image);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    analyzeUrl, HttpMethod.POST, request, String.class);

            System.out.println("VQA Response Status: " + response.getStatusCode());
            System.out.println("VQA Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                AnalysisResult result = parseLocalVQAResponse(response.getBody());
                System.out.println("Parsed Analysis Result: " + result.getCondition());
                System.out.println("Observed Symptoms: " + result.getObservedSymptoms());
                return result;
            } else {
                throw new RuntimeException("Local VQA service failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Local VQA service call failed: " + e.getMessage());
            throw e;
        }
    }

    private AnalysisResult parseLocalVQAResponse(String responseBody) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);

        if (!jsonResponse.path("success").asBoolean()) {
            String error = jsonResponse.path("error").asText("Unknown error");
            throw new RuntimeException("VQA analysis failed: " + error);
        }

        JsonNode results = jsonResponse.path("results");
        JsonNode assessment = results.path("overall_assessment");

        AnalysisResult result = new AnalysisResult();

        // Parse overall assessment
        String condition = assessment.path("condition").asText("Assessment completed");
        double confidence = assessment.path("confidence").asDouble(0.6);
        String urgencyString = assessment.path("urgency_level").asText("LOW");
        String summary = assessment.path("summary").asText("");

        result.setCondition(condition);
        result.setConfidence(confidence);
        result.setUrgency(mapUrgencyLevel(urgencyString));

        // Extract concerns and symptoms
        List<String> concerns = new ArrayList<>();
        List<String> positiveFindings = new ArrayList<>();

        // Process critical findings
        JsonNode criticalFindings = results.path("critical_findings");
        if (criticalFindings.isArray()) {
            for (JsonNode finding : criticalFindings) {
                if (finding.path("is_concerning").asBoolean()) {
                    String concern = extractConcernFromFinding(finding);
                    concerns.add(concern);
                    result.addObservedSymptom(concern);
                }
            }
        }

        // Process priority findings
        JsonNode priorityFindings = results.path("priority_findings");
        if (priorityFindings.isArray()) {
            for (JsonNode finding : priorityFindings) {
                if (finding.path("is_concerning").asBoolean()) {
                    String concern = extractConcernFromFinding(finding);
                    concerns.add(concern);
                    result.addObservedSymptom(concern);
                }
            }
        }

        // Process health findings
        JsonNode healthFindings = results.path("health_findings");
        if (healthFindings.isArray()) {
            for (JsonNode finding : healthFindings) {
                if (finding.path("is_positive").asBoolean()) {
                    String positive = extractPositiveFromFinding(finding);
                    positiveFindings.add(positive);
                }
            }
        }

        // Create comprehensive description
        result.setDescription(createVQAAssessmentDescription(
                condition, concerns, positiveFindings, summary,
                results.path("processing_time").asDouble(0),
                results.path("model_used").asText("VQA")));

        // Add recommendations from assessment
        JsonNode recommendations = assessment.path("recommendations");
        if (recommendations.isArray()) {
            for (JsonNode rec : recommendations) {
                result.addObservedSymptom("Recommendation: " + rec.asText());
            }
        }

        System.out.println("Local VQA Analysis Complete:");
        System.out.println("  Condition: " + result.getCondition());
        System.out.println("  Confidence: " + result.getConfidence());
        System.out.println("  Urgency: " + result.getUrgency());
        System.out.println("  Concerns: " + concerns.size());
        System.out.println("  Processing time: " + results.path("processing_time").asDouble() + "s");

        return result;
    }

    private String extractConcernFromFinding(JsonNode finding) {
        String question = finding.path("question").asText("");
        String answer = finding.path("answer").asText("");

        // Map VQA questions to veterinary concerns
        if (question.toLowerCase().contains("wounds") || question.toLowerCase().contains("bleeding")) {
            return "Visible wounds or bleeding detected";
        }
        if (question.toLowerCase().contains("distress") || question.toLowerCase().contains("pain")) {
            return "Signs of distress or pain observed";
        }
        if (question.toLowerCase().contains("unconscious") || question.toLowerCase().contains("unresponsive")) {
            return "Animal appears unconscious or unresponsive";
        }
        if (question.toLowerCase().contains("swollen")) {
            return "Swelling detected";
        }
        if (question.toLowerCase().contains("discharge")) {
            return "Abnormal discharge observed";
        }
        if (question.toLowerCase().contains("lethargic") || question.toLowerCase().contains("weak")) {
            return "Lethargy or weakness detected";
        }
        if (question.toLowerCase().contains("limping")) {
            return "Limping or mobility issues observed";
        }

        return "Health concern detected: " + answer;
    }

    private String extractPositiveFromFinding(JsonNode finding) {
        String question = finding.path("question").asText("");

        if (question.toLowerCase().contains("eyes")) {
            return "Clear, healthy eyes";
        }
        if (question.toLowerCase().contains("coat")) {
            return "Healthy coat condition";
        }
        if (question.toLowerCase().contains("alert")) {
            return "Alert and responsive behavior";
        }

        return "Normal health indicator";
    }

    private UrgencyLevel mapUrgencyLevel(String urgencyString) {
        switch (urgencyString.toUpperCase()) {
            case "CRITICAL":
                return UrgencyLevel.CRITICAL;
            case "HIGH":
                return UrgencyLevel.HIGH;
            case "MEDIUM":
                return UrgencyLevel.MEDIUM;
            case "LOW":
            default:
                return UrgencyLevel.LOW;
        }
    }

    private String createVQAAssessmentDescription(String condition, List<String> concerns,
            List<String> positiveFindings, String summary,
            double processingTime, String modelUsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Visual Veterinary Assessment Report\n");
        sb.append("Model: ").append(modelUsed).append(" | Processing time: ")
                .append(String.format("%.1f", processingTime)).append("s\n\n");

        sb.append("OVERALL STATUS: ").append(condition).append("\n\n");

        if (!concerns.isEmpty()) {
            sb.append("CONCERNS IDENTIFIED:\n");
            for (String concern : concerns) {
                sb.append("• ").append(concern).append("\n");
            }
            sb.append("\n");
        }

        if (!positiveFindings.isEmpty()) {
            sb.append("POSITIVE FINDINGS:\n");
            for (String finding : positiveFindings) {
                sb.append("• ").append(finding).append("\n");
            }
            sb.append("\n");
        }

        if (summary != null && !summary.trim().isEmpty()) {
            sb.append("SUMMARY: ").append(summary).append("\n\n");
        }

        // Add recommendations based on urgency
        sb.append("RECOMMENDATIONS:\n");
        if (concerns.size() > 2) {
            sb.append("• URGENT: Seek immediate veterinary attention\n");
            sb.append("• Multiple concerns detected requiring professional evaluation\n");
        } else if (concerns.size() >= 1) {
            sb.append("• Schedule veterinary consultation within 24-48 hours\n");
            sb.append("• Monitor pet closely for any changes\n");
        } else {
            sb.append("• Continue regular care and monitoring\n");
            sb.append("• Maintain current diet and exercise routine\n");
        }

        sb.append("\nIMPORTANT: This AI assessment provides preliminary visual analysis only. ");
        sb.append("Professional veterinary examination is recommended for proper diagnosis and treatment.");

        return sb.toString();
    }

    // Single question endpoint for quick queries
    public VQAResult askQuickQuestion(MultipartFile image, String question) throws Exception {
        if (!vqaEnabled || !isVQAServiceHealthy()) {
            throw new RuntimeException("VQA service is not available");
        }

        String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        String quickQuestionUrl = vqaServiceUrl + "/quick-question";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_base64", base64Image);
        requestBody.put("question", question);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                quickQuestionUrl, HttpMethod.POST, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (jsonResponse.path("success").asBoolean()) {
                return new VQAResult(
                        jsonResponse.path("question").asText(),
                        jsonResponse.path("answer").asText(),
                        jsonResponse.path("confidence").asDouble());
            } else {
                throw new RuntimeException("Quick question failed: " + jsonResponse.path("error").asText());
            }
        } else {
            throw new RuntimeException("Quick question API failed: " + response.getStatusCode());
        }
    }

    // Fallback to Hugging Face if local VQA fails
    private AnalysisResult analyzeWithHuggingFace(MultipartFile image) throws Exception {
        System.out.println("Using Hugging Face fallback analysis...");

        if (huggingFaceToken == null || huggingFaceToken.isEmpty()) {
            return analyzeWithBasicDetection(image);
        }

        // Try image classification
        String apiUrl = "https://api-inference.huggingface.co/models/microsoft/resnet-50";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Authorization", "Bearer " + huggingFaceToken);
        headers.add("User-Agent", "VirtualVet/1.0");

        HttpEntity<byte[]> request = new HttpEntity<>(image.getBytes(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseHuggingFaceResponse(response.getBody());
            } else {
                throw new RuntimeException("Hugging Face API failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Hugging Face fallback failed: " + e.getMessage());
            return analyzeWithBasicDetection(image);
        }
    }

    private AnalysisResult parseHuggingFaceResponse(String responseBody) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);

        AnalysisResult result = new AnalysisResult();

        if (jsonResponse.isArray() && jsonResponse.size() > 0) {
            JsonNode topResult = jsonResponse.get(0);
            String label = topResult.get("label").asText();
            double confidence = topResult.get("score").asDouble();

            if (label.toLowerCase().contains("dog") || label.toLowerCase().contains("cat") ||
                    label.toLowerCase().contains("animal")) {
                result.setCondition("Pet identified - visual health monitoring recommended");
                result.setConfidence(confidence * 0.7); // Reduce confidence for basic classification
                result.setUrgency(UrgencyLevel.LOW);
                result.addObservedSymptom("Animal identified as " + label);

                result.setDescription(
                        "Basic Image Classification Report\n\n" +
                                "DETECTED: " + label + " (confidence: " + String.format("%.1f%%", confidence * 100)
                                + ")\n\n" +
                                "This basic classification has identified your pet but cannot detect specific health conditions.\n\n"
                                +
                                "For comprehensive health assessment:\n" +
                                "• Describe any symptoms you've observed\n" +
                                "• Note behavioral changes\n" +
                                "• Consider veterinary consultation if concerned\n\n" +
                                "Note: Advanced AI visual analysis is temporarily unavailable.");
            } else {
                result.setCondition("Image processed - manual review recommended");
                result.setConfidence(0.3);
                result.setUrgency(UrgencyLevel.LOW);
                result.setDescription("Basic image processing completed. Manual veterinary review recommended.");
            }
        }

        return result;
    }

    private AnalysisResult analyzeWithBasicDetection(MultipartFile image) throws Exception {
        System.out.println("Using basic detection fallback...");

        AnalysisResult result = new AnalysisResult();
        BufferedImage bufferedImage = ImageIO.read(image.getInputStream());

        if (bufferedImage == null) {
            throw new IOException("Invalid image format");
        }

        result.setCondition("Image received - veterinary consultation recommended");
        result.setConfidence(0.3);
        result.setUrgency(UrgencyLevel.LOW);
        result.addObservedSymptom("Image uploaded for assessment");

        result.setDescription(String.format(
                "Basic Image Processing Report\n\n" +
                        "IMAGE DETAILS:\n" +
                        "• Resolution: %d x %d pixels\n" +
                        "• File size: %.1f KB\n" +
                        "• Format: %s\n\n" +
                        "STATUS: Image successfully processed but AI health analysis is currently unavailable.\n\n" +
                        "NEXT STEPS:\n" +
                        "• Describe any symptoms or concerns you've observed\n" +
                        "• Note any changes in your pet's behavior\n" +
                        "• Consider scheduling a veterinary consultation\n\n" +
                        "The system can provide better guidance with symptom descriptions.",
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                image.getSize() / 1024.0,
                getFileExtension(image.getOriginalFilename()).toUpperCase()));

        return result;
    }

    private AnalysisResult createFallbackResult(String errorMessage) {
        AnalysisResult result = new AnalysisResult();
        result.setCondition("Image analysis unavailable");
        result.setConfidence(0.0);
        result.setUrgency(UrgencyLevel.LOW);
        result.setDescription(
                "Image Analysis Error\n\n" +
                        "Unable to perform automated analysis: " + errorMessage + "\n\n" +
                        "ALTERNATIVE APPROACH:\n" +
                        "• Describe your pet's symptoms in text\n" +
                        "• Note any behavioral changes\n" +
                        "• Contact your veterinarian for guidance\n\n" +
                        "The AI can still provide veterinary guidance based on symptom descriptions.");
        return result;
    }

    // Helper classes
    public static class VQAResult {
        public final String question;
        public final String answer;
        public final double confidence;

        public VQAResult(String question, String answer, double confidence) {
            this.question = question;
            this.answer = answer;
            this.confidence = confidence;
        }
    }

    // Keep existing utility methods
    public UrgencyLevel determineUrgency(AnalysisResult result, List<String> additionalSymptoms) {
        UrgencyLevel imageUrgency = result.getUrgency();
        UrgencyLevel symptomUrgency = determineUrgencyFromSymptoms(additionalSymptoms);

        return imageUrgency.ordinal() > symptomUrgency.ordinal() ? imageUrgency : symptomUrgency;
    }

    private UrgencyLevel determineUrgencyFromSymptoms(List<String> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return UrgencyLevel.LOW;
        }

        for (String symptom : symptoms) {
            String lower = symptom.toLowerCase();
            if (containsAny(lower, Arrays.asList("bleeding", "unconscious", "seizure", "choking", "not breathing"))) {
                return UrgencyLevel.CRITICAL;
            }
            if (containsAny(lower, Arrays.asList("severe pain", "difficulty breathing", "won't eat for days"))) {
                return UrgencyLevel.HIGH;
            }
            if (containsAny(lower, Arrays.asList("limping", "lethargy", "discharge", "coughing"))) {
                return UrgencyLevel.MEDIUM;
            }
        }

        return UrgencyLevel.LOW;
    }

    public String saveImage(MultipartFile image, String sessionId) throws IOException {
        validateImage(image);

        Path sessionDir = Paths.get(imageStoragePath, sessionId);
        Files.createDirectories(sessionDir);

        String originalFilename = image.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "vet_image_" + System.currentTimeMillis() + "." + extension;

        Path filePath = sessionDir.resolve(filename);

        try (InputStream inputStream = image.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Image saved: " + filePath.toString());
        return "/images/" + sessionId + "/" + filename;
    }

    private void validateImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IOException("Image file is empty");
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Image file too large. Maximum size is 5MB");
        }

        String filename = image.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("Invalid filename");
        }

        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IOException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            if (bufferedImage == null) {
                throw new IOException("Invalid image format or corrupted file");
            }
        } catch (Exception e) {
            throw new IOException("Unable to process image file: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    public boolean isValidImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        String extension = getFileExtension(filename);
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    public void cleanupOldImages(String sessionId, int maxFiles) {
        try {
            Path sessionDir = Paths.get(imageStoragePath, sessionId);
            if (!Files.exists(sessionDir)) {
                return;
            }

            File[] files = sessionDir.toFile().listFiles();
            if (files != null && files.length > maxFiles) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

                int filesToDelete = files.length - maxFiles;
                for (int i = 0; i < filesToDelete; i++) {
                    files[i].delete();
                }

                System.out.println("Cleaned up " + filesToDelete + " old images for session " + sessionId);
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup old images for session " + sessionId + ": " + e.getMessage());
        }
    }
}