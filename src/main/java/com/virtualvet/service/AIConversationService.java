package com.virtualvet.service;

import com.virtualvet.model.*;
import com.virtualvet.entity.*;
import com.virtualvet.enums.entity.MessageType;
import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.config.AIServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIConversationService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AIServiceConfig aiServiceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "You are a professional virtual veterinary assistant. Your role is to:\n"
            +
            "1. Help pet owners assess their animal's health concerns\n" +
            "2. Provide general veterinary guidance and education\n" +
            "3. Determine urgency levels for veterinary care\n" +
            "4. Offer first aid and care recommendations\n" +
            "5. Know when to recommend immediate emergency care\n" +
            "6. Always try to comfort the pet owner and show empathy\n\n" +

            "CRITICAL RESPONSE FORMATTING RULES:\n" +
            "- DO NOT include <think> tags or any internal reasoning in your response\n" +
            "- Respond DIRECTLY to the pet owner with your assessment and advice\n" +
            "- You MAY use **bold text** for emphasis when appropriate\n" +
            "- You MAY use newlines (\\n) for paragraph breaks and formatting\n" +
            "- If you need to create numbered lists, wrap them in <list> tags like this:\n" +
            "  <list>\n" +
            "  1. First item\n" +
            "  2. Second item\n" +
            "  3. Third item\n" +
            "  </list>\n" +
            "- DO NOT MAKE BULLET POINTS ONLY MAKE NUMBERED LISTS\n" +

            "MULTI-PART RESPONSES:\n" +
            "- For longer responses, break your answer into logical parts (2-4 parts maximum)\n" +
            "- Each part should be a complete thought or section (e.g., initial assessment, questions to ask, immediate care steps, when to seek help)\n"
            +
            "- Separate each part with exactly this marker: |||SPLIT|||\n" +
            "- Example format:\n" +
            "  First part of your response here...\n" +
            "  |||SPLIT|||\n" +
            "  Second part of your response here...\n" +
            "  |||SPLIT|||\n" +
            "  Third part of your response here...\n" +
            "- Do NOT use the split marker for short responses (under 200 words)\n" +
            "- Only split when it makes the response more readable and digestible\n\n" +

            "Guidelines:\n" +
            "- Always be empathetic and professional\n" +
            "- Ask relevant follow-up questions to better assess the situation\n" +
            "- Never provide definitive diagnoses - only suggest possibilities\n" +
            "- Always recommend professional veterinary care for serious concerns\n" +
            "- Provide practical, actionable advice when appropriate\n" +
            "- Be clear about limitations of remote assessment\n" +
            "- Use clear, non-technical language that pet owners can understand\n" +
            "- Show concern for both the pet's welfare and the owner's emotional state\n" +
            "- Provide specific next steps when possible\n" +
            "- Include timeframes for when to seek care (immediate, within hours, within days)\n";

    public String generateResponse(String userMessage, ConversationContext context,
            List<AnalysisResult> imageAnalyses) {
        try {
            String contextPrompt = buildContextPrompt(context, imageAnalyses);
            String fullPrompt = SYSTEM_PROMPT + "\n\n" + contextPrompt + "\n\nUser: " + userMessage;

            String rawResponse = callHackClubAPI(fullPrompt);
            return cleanAIResponse(rawResponse);

        } catch (Exception e) {
            return generateFallbackResponse(userMessage, context, imageAnalyses);
        }
    }

    public String generateResponse(String userMessage, ConversationContext context, AnalysisResult imageAnalysis) {
        List<AnalysisResult> imageAnalyses = new ArrayList<>();
        if (imageAnalysis != null) {
            imageAnalyses.add(imageAnalysis);
        }
        return generateResponse(userMessage, context, imageAnalyses);
    }

    private String cleanAIResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "I'm having trouble processing your request right now. Please try again or contact your veterinarian directly if this is urgent.";
        }

        // Only remove <think> tags if they exist - don't touch anything else
        String cleaned = rawResponse.replaceAll("(?s)<think>.*?</think>", "").trim();

        // If the response is empty after removing think tags, provide fallback
        if (cleaned.isEmpty()) {
            return "Thank you for sharing this information about your pet. I recommend monitoring your pet closely and contacting your veterinarian if symptoms persist or worsen. Would you like to share any additional details about your pet's condition?";
        }

        return cleaned;
    }

    private String callHackClubAPI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    aiServiceConfig.getHackClub().getApiUrl(),
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.path("choices").get(0).path("message").path("content").asText();
            } else {
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to call AI API: " + e.getMessage(), e);
        }
    }

    public String buildContextPrompt(ConversationContext context, List<AnalysisResult> imageAnalyses) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("CONVERSATION CONTEXT:\n");

        // Animal profile information
        if (context.getAnimalProfile() != null) {
            AnimalProfile profile = context.getAnimalProfile();
            contextBuilder.append("Animal Information:\n");
            if (profile.getAnimalType() != null) {
                contextBuilder.append("- Type: ").append(profile.getAnimalType()).append("\n");
            }
            if (profile.getBreed() != null) {
                contextBuilder.append("- Breed: ").append(profile.getBreed()).append("\n");
            }
            if (profile.getAge() != null) {
                contextBuilder.append("- Age: ").append(profile.getAge()).append(" years\n");
            }
            if (profile.getWeight() != null) {
                contextBuilder.append("- Weight: ").append(profile.getWeight()).append(" kg\n");
            }
        }

        // Current symptoms
        if (!context.getIdentifiedSymptoms().isEmpty()) {
            contextBuilder.append("\nIdentified Symptoms:\n");
            for (String symptom : context.getIdentifiedSymptoms()) {
                contextBuilder.append("- ").append(symptom).append("\n");
            }
        }

        // Current urgency level
        contextBuilder.append("\nCurrent Urgency Level: ").append(context.getCurrentUrgency().getDisplayName())
                .append("\n");

        // Multiple image analysis results
        if (imageAnalyses != null && !imageAnalyses.isEmpty()) {
            contextBuilder.append("\nImage Analysis Results (").append(imageAnalyses.size()).append(" images):\n");

            for (int i = 0; i < imageAnalyses.size(); i++) {
                AnalysisResult imageAnalysis = imageAnalyses.get(i);
                contextBuilder.append("Image ").append(i + 1).append(":\n");
                contextBuilder.append("  - Condition: ").append(imageAnalysis.getCondition()).append("\n");
                contextBuilder.append("  - Confidence: ")
                        .append(String.format("%.1f%%", imageAnalysis.getConfidence() * 100)).append("\n");
                contextBuilder.append("  - Urgency: ").append(imageAnalysis.getUrgency().getDisplayName()).append("\n");

                if (!imageAnalysis.getObservedSymptoms().isEmpty()) {
                    contextBuilder.append("  - Observed symptoms: ")
                            .append(String.join(", ", imageAnalysis.getObservedSymptoms())).append("\n");
                }
                if (imageAnalysis.getDescription() != null) {
                    contextBuilder.append("  - Description: ").append(imageAnalysis.getDescription()).append("\n");
                }
                contextBuilder.append("\n");
            }
        }

        // Recent conversation history
        if (!context.getRecentHistory().isEmpty()) {
            contextBuilder.append("\nRecent Conversation (last 5 messages):\n");
            List<Message> recentMessages = context.getRecentHistory().stream()
                    .limit(5)
                    .collect(Collectors.toList());

            for (Message msg : recentMessages) {
                String role = msg.getMessageType() == MessageType.USER ? "Owner" : "Vet Assistant";
                contextBuilder.append(role).append(": ").append(msg.getContent()).append("\n");
            }
        }

        return contextBuilder.toString();
    }

    public String buildContextPrompt(ConversationContext context, AnalysisResult imageAnalysis) {
        List<AnalysisResult> imageAnalyses = new ArrayList<>();
        if (imageAnalysis != null) {
            imageAnalyses.add(imageAnalysis);
        }
        return buildContextPrompt(context, imageAnalyses);
    }

    private String generateFallbackResponse(String userMessage, ConversationContext context,
            List<AnalysisResult> imageAnalyses) {
        // Generate a rule-based response when AI service is unavailable
        String lowerMessage = userMessage.toLowerCase();

        // Emergency situations
        if (containsEmergencyKeywords(lowerMessage)) {
            return "**Emergency Detected**\n\nBased on what you've described, this sounds like it could be a serious emergency. Please contact your veterinarian immediately or visit the nearest emergency animal clinic. Time is critical in these situations.\n\nIf you need help finding an emergency clinic, I can assist with that.";
        }

        // Check if any image analysis shows high urgency
        if (imageAnalyses != null && !imageAnalyses.isEmpty()) {
            UrgencyLevel highestImageUrgency = imageAnalyses.stream()
                    .map(AnalysisResult::getUrgency)
                    .max(Comparator.comparingInt(Enum::ordinal))
                    .orElse(UrgencyLevel.LOW);

            if (highestImageUrgency == UrgencyLevel.CRITICAL || highestImageUrgency == UrgencyLevel.HIGH) {
                StringBuilder imageResponse = new StringBuilder();
                imageResponse.append("**Image Analysis Results**\n\n");
                imageResponse.append("I've analyzed ").append(imageAnalyses.size()).append(" image(s) you shared. ");

                boolean hasHighUrgencyImage = false;
                for (int i = 0; i < imageAnalyses.size(); i++) {
                    AnalysisResult analysis = imageAnalyses.get(i);
                    if (analysis.getUrgency() == UrgencyLevel.CRITICAL || analysis.getUrgency() == UrgencyLevel.HIGH) {
                        if (!hasHighUrgencyImage) {
                            imageResponse.append("Some concerning signs were detected:\n\n");
                            hasHighUrgencyImage = true;
                        }
                        imageResponse.append("Image ").append(i + 1).append(": ").append(analysis.getDescription())
                                .append("\n");
                    }
                }

                if (hasHighUrgencyImage) {
                    imageResponse.append(
                            "\nI recommend contacting your veterinarian as soon as possible for proper evaluation and treatment. The overall urgency level appears to be **")
                            .append(highestImageUrgency.getDisplayName()).append("**.");
                    return imageResponse.toString();
                }
            }
        }

        // Common symptoms with advice
        if (lowerMessage.contains("vomiting")) {
            return "**Vomiting Assessment**\n\nVomiting can have many causes in pets. Here's what I recommend:\n\n<list>\n1. Withhold food for 12 hours but provide small amounts of water\n2. Monitor for other symptoms like lethargy, blood, or continued vomiting\n3. Contact your vet if vomiting continues or worsens\n</list>\n\nCan you tell me how long this has been going on and if you've noticed any other symptoms?";
        }

        if (lowerMessage.contains("limping") || lowerMessage.contains("leg")) {
            return "**Limping Evaluation**\n\nLimping can indicate injury or pain. Here's what to do:\n\n<list>\n1. Keep your pet calm and limit their movement\n2. Gently check the affected leg for obvious injuries or swelling\n3. Look for foreign objects in paw pads\n4. Apply ice if there's visible swelling\n</list>\n\nIf the limping is severe or doesn't improve within 24 hours, veterinary examination is recommended. How long has your pet been limping?";
        }

        if (lowerMessage.contains("eating") || lowerMessage.contains("appetite")) {
            return "**Appetite Changes**\n\nChanges in appetite can indicate various health issues. Please monitor for:\n\n<list>\n1. Other symptoms like vomiting, diarrhea, or lethargy\n2. Behavioral changes\n3. Changes in drinking habits\n4. Any signs of pain or discomfort\n</list>\n\nIf your pet hasn't eaten for more than 24 hours, please consult with your veterinarian. How long has this been going on?";
        }

        // General response with image acknowledgment
        String baseResponse = "Thank you for sharing your concerns about your pet.";
        if (imageAnalyses != null && !imageAnalyses.isEmpty()) {
            baseResponse += " I've received " + imageAnalyses.size() + " image(s) that will help with the assessment.";
        }

        baseResponse += " To provide you with the best guidance, could you please tell me more about the specific symptoms you've observed?\n\n**Helpful details include:**\n<list>\n1. When the symptoms started\n2. How severe they appear\n3. Changes in behavior, eating, or bathroom habits\n4. Any recent changes in routine or environment\n</list>\n\nThis information will help me give you more targeted advice.";

        return baseResponse;
    }

    private String generateFallbackResponse(String userMessage, ConversationContext context,
            AnalysisResult imageAnalysis) {
        List<AnalysisResult> imageAnalyses = new ArrayList<>();
        if (imageAnalysis != null) {
            imageAnalyses.add(imageAnalysis);
        }
        return generateFallbackResponse(userMessage, context, imageAnalyses);
    }


    private boolean containsEmergencyKeywords(String message) {
        List<String> emergencyKeywords = Arrays.asList(
                "not breathing", "can't breathe", "unconscious", "bleeding heavily", "blood",
                "convulsing", "seizure", "choking", "collapsed", "emergency", "dying",
                "severe pain", "won't wake up", "hit by car", "poisoned");

        return emergencyKeywords.stream().anyMatch(message::contains);
    }

    public String buildSystemPromptWithProfile(AnimalProfile profile) {
        StringBuilder promptBuilder = new StringBuilder(SYSTEM_PROMPT);

        if (profile != null) {
            promptBuilder.append("\nCURRENT PATIENT PROFILE:\n");
            if (profile.getAnimalType() != null) {
                promptBuilder.append("Animal Type: ").append(profile.getAnimalType()).append("\n");
            }
            if (profile.getBreed() != null) {
                promptBuilder.append("Breed: ").append(profile.getBreed()).append("\n");
            }
            if (profile.getAge() != null) {
                promptBuilder.append("Age: ").append(profile.getAge()).append(" years\n");
            }
            if (profile.getWeight() != null) {
                promptBuilder.append("Weight: ").append(profile.getWeight()).append(" kg\n");
            }
            if (profile.getSymptoms() != null && !profile.getSymptoms().trim().isEmpty()) {
                promptBuilder.append("Known Symptoms: ").append(profile.getSymptoms()).append("\n");
            }
        }

        return promptBuilder.toString();
    }

    public List<String> extractSymptomsFromText(String text) {
        List<String> symptoms = new ArrayList<>();
        String lowerText = text.toLowerCase();

        // Common symptom keywords
        Map<String, String> symptomKeywords = new HashMap<>();
        symptomKeywords.put("vomit", "vomiting");
        symptomKeywords.put("throw up", "vomiting");
        symptomKeywords.put("limp", "limping");
        symptomKeywords.put("diarrhea", "diarrhea");
        symptomKeywords.put("loose stool", "diarrhea");
        symptomKeywords.put("not eating", "loss of appetite");
        symptomKeywords.put("won't eat", "loss of appetite");
        symptomKeywords.put("lethargic", "lethargy");
        symptomKeywords.put("tired", "lethargy");
        symptomKeywords.put("scratch", "scratching");
        symptomKeywords.put("itch", "itching");
        symptomKeywords.put("cough", "coughing");
        symptomKeywords.put("sneez", "sneezing");
        symptomKeywords.put("discharge", "discharge");
        symptomKeywords.put("swelling", "swelling");
        symptomKeywords.put("swollen", "swelling");
        symptomKeywords.put("pain", "pain");
        symptomKeywords.put("hurt", "pain");
        symptomKeywords.put("breathing", "breathing difficulty");
        symptomKeywords.put("panting", "excessive panting");

        for (Map.Entry<String, String> entry : symptomKeywords.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                symptoms.add(entry.getValue());
            }
        }

        return symptoms;
    }

    public UrgencyLevel assessUrgencyFromResponse(String aiResponse) {
        String lowerResponse = aiResponse.toLowerCase();

        if (lowerResponse.contains("emergency") || lowerResponse.contains("immediately") ||
                lowerResponse.contains("critical") || lowerResponse.contains("urgent care")) {
            return UrgencyLevel.CRITICAL;
        }

        if (lowerResponse.contains("soon") || lowerResponse.contains("within hours") ||
                lowerResponse.contains("concerning") || lowerResponse.contains("worrisome")) {
            return UrgencyLevel.HIGH;
        }

        if (lowerResponse.contains("monitor") || lowerResponse.contains("watch") ||
                lowerResponse.contains("within a day") || lowerResponse.contains("24 hours")) {
            return UrgencyLevel.MEDIUM;
        }

        return UrgencyLevel.LOW;
    }
}