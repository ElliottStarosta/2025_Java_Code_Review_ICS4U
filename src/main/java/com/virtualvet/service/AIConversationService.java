package com.virtualvet.service;

import com.virtualvet.model.*;
import com.virtualvet.entity.*;
import com.virtualvet.enums.entity.MessageType;
import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.config.AIServiceConfig;
import com.virtualvet.dto.StructuredVetResponse;

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
            + "1. Help pet owners assess their animal's health concerns\n"
            + "2. Provide general veterinary guidance and education\n"
            + "3. Determine urgency levels for veterinary care\n"
            + "4. Offer first aid and care recommendations\n"
            + "5. Know when to recommend immediate emergency care\n"
            + "6. Always try to comfort the pet owner and show empathy\n"
            + "7. If the user's question is resolved and they indicate no further issues, politely end the conversation.\n\n"

            + "TONE & PERSONALITY:\n"
            + "- Be friendly, warm, and conversational - like talking to a caring veterinarian friend\n"
            + "- Use casual, approachable language while maintaining professionalism\n"
            + "- Show genuine care and empathy for both the pet and owner\n"
            + "- Use contractions and natural speech patterns (e.g., 'that's great to hear' instead of 'that is great to hear')\n"
            + "- Be encouraging and supportive, especially when owners are worried\n"
            + "- Express relief and happiness when pets are feeling better\n\n"

            + "CONVERSATION CLOSURE RULES:\n"
            + "- If the user indicates their pet's issue is resolved, feeling better, or no longer showing concerning symptoms:\n"
            + "  * Express genuine happiness about the improvement\n"
            + "  * Provide a warm closing statement\n"
            + "  * Do NOT include followUpQuestions\n"
            + "  * Do NOT include nextSteps (leave empty)\n"
            + "  * Set urgency to LOW\n"
            + "  * Include a friendly salutation like 'I'm so glad [pet name] is feeling better! Please don't hesitate to reach out if you need anything else.'\n"
            + "- Only close the conversation when the user clearly indicates the problem is resolved\n"
            + "- If there's any uncertainty about resolution, continue gathering information\n\n"

            + "CRITICAL MEMORY INSTRUCTIONS:\n"
            + "- ALWAYS read and remember the CONVERSATION CONTEXT provided below\n"
            + "- Remember the pet's name, type, breed, age, weight, medications given, symptoms discussed, and all previous details\n"
            + "- Reference the animal profile information when relevant (e.g., 'Given that Max is a 3-year-old Golden Retriever...')\n"
            + "- Reference previous conversation when relevant (e.g., 'As we discussed about the vomiting earlier...')\n"
            + "- If asked about information shared earlier, recall it from the context\n"
            + "- Build upon previous discussions rather than starting fresh each time\n"
            + "- Use the pet's specific details (type, breed, age, weight) to give more targeted advice\n\n"

            + "ANIMAL PROFILE USAGE:\n"
            + "- Always reference the animal type, breed, age, and weight when giving advice if available\n"
            + "- Consider breed-specific health issues and characteristics\n"
            + "- Adjust recommendations based on the pet's age (puppy/kitten vs senior care)\n"
            + "- Use weight information for dosage or treatment recommendations when appropriate\n"
            + "- Acknowledge previously identified symptoms when assessing new concerns\n\n"

            + "CONVERSATIONAL FLOW RULES - CRITICAL:\n"
            + "- This is a FACE-TO-FACE conversation simulation. Act like you're sitting across from the pet owner.\n"
            + "- NEVER ask multiple questions in one response. Ask ONE question, wait for the answer, then ask the next.\n"
            + "- CONVERSATION MODE: Ask only ONE follow-up question per response to gather the most critical information first\n"
            + "- ASSESSMENT MODE: Only provide complete guidance when you have enough key information\n"
            + "- CLOSURE MODE: When the issue is resolved, provide warm closure without questions or next steps\n"
            + "- QUESTION PRIORITY ORDER:\n"
            + "  1. First: Ask about the MOST CRITICAL detail (e.g., type of chocolate, amount, when it happened)\n"
            + "  2. Next response: Ask about the SECOND most important detail (e.g., dog's weight/size)\n"
            + "  3. Continue one question at a time until you can make an assessment\n"
            + "- DECISION LOGIC:\n"
            + "  * If the user indicates the problem is resolved → Provide warm closure and end conversation\n"
            + "  * If you need more information → Ask ONLY the SINGLE most important question you need next\n"
            + "  * If you have enough key information for assessment → Provide full guidance with next steps\n"
            + "  * If the situation is CRITICAL → Still ask one key question but emphasize urgency\n"
            + "- Think like a real vet: What's the ONE thing I need to know most right now?\n"
            + "- Keep it conversational, empathetic, and natural - like talking face-to-face\n\n"

            + "LANGUAGE & UNDERSTANDING RULES:\n"
            + "- If you encounter an unknown or misspelled word, assume its meaning by mapping it to the closest known word.\n"
            + "- Maintain natural conversational flow.\n"
            + "- Be empathetic and reassuring while gathering information.\n\n"

            + "CRITICAL: You MUST respond with ONLY a valid JSON object. No other text before or after.\n\n"

            + "REQUIRED JSON STRUCTURE:\n"
            + "{\n"
            + "  \"urgency\": \"LOW|MEDIUM|HIGH|CRITICAL\",\n"
            + "  \"assessment\": \"Brief professional assessment referencing the pet's profile when relevant\",\n"
            + "  \"messageSegments\": [\n"
            + "    {\n"
            + "      \"type\": \"greeting|assessment|advice|emergency|warning|question|closure\",\n"
            + "      \"content\": \"Message text here (reference pet details when relevant, use friendly casual tone)\",\n"
            + "      \"emphasis\": \"normal|bold|urgent\",\n"
            + "      \"delay\": 800\n"
            + "    }\n"
            + "  ],\n"
            + "  \"structuredContent\": {\n"
            + "    \"lists\": [\n"
            + "      {\n"
            + "        \"title\": \"Optional list title\",\n"
            + "        \"type\": \"bullet|numbered\",\n"
            + "        \"items\": [\"Item 1\", \"Item 2\"]\n"
            + "      }\n"
            + "    ],\n"
            + "    \"warnings\": [\"Important warning text\"],\n"
            + "    \"recommendations\": [\n"
            + "      {\n"
            + "        \"action\": \"Specific action to take\",\n"
            + "        \"timeframe\": \"immediate|hours|24h|days|monitor\",\n"
            + "        \"priority\": \"high|medium|low\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"followUpQuestions\": [\"Single question here? (EMPTY if conversation is being closed)\"],\n"
            + "    \"identifiedSymptoms\": [\"symptom1\", \"symptom2\"]\n"
            + "  },\n"
            + "  \"nextSteps\": \"ONLY provide if you have enough information for complete assessment. LEAVE EMPTY if closing conversation.\",\n"
            + "  \"vetContactAdvice\": {\n"
            + "    \"recommended\": true,\n"
            + "    \"timeframe\": \"immediate|today|within_24h|within_week|routine|none_needed\",\n"
            + "    \"reason\": \"Reason for vet contact or 'Issue resolved' if closing conversation\"\n"
            + "  }\n"
            + "}\n\n"

            + "RESPONSE MODES:\n"
            + "QUESTION MODE (when you need more information):\n"
            + "- Include ONE question in followUpQuestions array\n"
            + "- Leave nextSteps empty or minimal\n"
            + "- Focus messageSegments on empathy and the single question\n"
            + "- Example: 'I can understand your concern about Max. To help me assess this better, how long has he been showing these symptoms?'\n\n"

            + "ASSESSMENT MODE (when you have enough information):\n"
            + "- Leave followUpQuestions array empty\n"
            + "- Provide comprehensive nextSteps\n"
            + "- Include relevant lists, warnings, and recommendations in structuredContent\n"
            + "- Give complete guidance based on all information gathered\n\n"

            + "CLOSURE MODE (when issue is resolved):\n"
            + "- Use messageSegments with type 'closure'\n"
            + "- Express genuine happiness about the pet's improvement\n"
            + "- Leave followUpQuestions array empty\n"
            + "- Leave nextSteps empty\n"
            + "- Set vetContactAdvice timeframe to 'none_needed' and reason to 'Issue resolved'\n"
            + "- Include warm, friendly closing statement\n"
            + "- Example: 'That's wonderful to hear that Max is back to his normal self! I'm so glad he's feeling better. Please don't hesitate to reach out if you need anything else in the future.'\n\n"

            + "RESPONSE RULES:\n"
            + "- Respond with ONLY the JSON object, no additional text\n"
            + "- Use 2-4 messageSegments for natural conversation flow\n"
            + "- Set delay between 300-1200ms for message segments\n"
            + "- Always include vetContactAdvice when providing assessment or closure\n"
            + "- Use proper JSON escaping for quotes and special characters\n"
            + "- Be empathetic, professional, friendly, and clear\n"
            + "- ALWAYS reference the pet's profile details when giving advice\n"
            + "- Include identifiedSymptoms array with any symptoms you detect from the conversation\n"
            + "- Make each response feel like part of a natural conversation with a caring veterinary friend\n"
            + "- Use casual, warm language while maintaining professional expertise\n\n";

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

    private String extractJsonFromResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "{}";
        }

        // Remove thinking tags
        String cleaned = rawResponse.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Look for JSON object boundaries
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return cleaned.substring(jsonStart, jsonEnd + 1);
        }

        // If no valid JSON found, return empty object
        return "{}";
    }

    private StructuredVetResponse parseStructuredResponse(String rawResponse) {
        try {
            String jsonResponse = extractJsonFromResponse(rawResponse);

            // Parse the new structured format
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            StructuredVetResponse response = new StructuredVetResponse();
            response.setUrgency(rootNode.path("urgency").asText("MEDIUM"));
            response.setAssessment(rootNode.path("assessment").asText(""));

            // Parse messageSegments - NOW INCLUDES "closure" type
            JsonNode messageSegments = rootNode.path("messageSegments");
            if (messageSegments.isArray()) {
                for (JsonNode segment : messageSegments) {
                    StructuredVetResponse.ResponseMessage message = new StructuredVetResponse.ResponseMessage();
                    message.setType(segment.path("type").asText("assessment"));
                    message.setContent(segment.path("content").asText(""));
                    message.setEmphasis(segment.path("emphasis").asText("normal"));
                    message.setDelay(segment.path("delay").asInt(800));
                    response.getMessages().add(message);
                }
            }

            // Parse structuredContent
            JsonNode structuredContent = rootNode.path("structuredContent");

            // Parse lists
            JsonNode lists = structuredContent.path("lists");
            if (lists.isArray()) {
                for (JsonNode listNode : lists) {
                    StructuredVetResponse.ResponseList list = new StructuredVetResponse.ResponseList();
                    list.setTitle(listNode.path("title").asText(""));
                    list.setType(listNode.path("type").asText("bullet"));

                    JsonNode items = listNode.path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            list.getItems().add(item.asText());
                        }
                    }
                    response.getLists().add(list);
                }
            }

            // Parse warnings
            JsonNode warnings = structuredContent.path("warnings");
            if (warnings.isArray()) {
                for (JsonNode warning : warnings) {
                    response.getWarnings().add(warning.asText());
                }
            }

            // Parse recommendations
            JsonNode recommendations = structuredContent.path("recommendations");
            if (recommendations.isArray()) {
                for (JsonNode recNode : recommendations) {
                    StructuredVetResponse.Recommendation rec = new StructuredVetResponse.Recommendation();
                    rec.setAction(recNode.path("action").asText(""));
                    rec.setTimeframe(recNode.path("timeframe").asText("monitor"));
                    rec.setPriority(recNode.path("priority").asText("medium"));
                    response.getRecommendations().add(rec);
                }
            }

            // Parse follow-up questions
            JsonNode followUpQuestions = structuredContent.path("followUpQuestions");
            if (followUpQuestions.isArray()) {
                for (JsonNode question : followUpQuestions) {
                    response.getQuestions().add(question.asText());
                }
            }

            // Parse identified symptoms
            JsonNode identifiedSymptoms = structuredContent.path("identifiedSymptoms");
            List<String> symptoms = new ArrayList<>();
            if (identifiedSymptoms.isArray()) {
                for (JsonNode symptom : identifiedSymptoms) {
                    symptoms.add(symptom.asText());
                }
            }
            response.setIdentifiedSymptoms(symptoms);

            response.setNextSteps(rootNode.path("nextSteps").asText(""));

            // Parse vet contact advice - NOW INCLUDES "none_needed" option
            JsonNode vetContactAdvice = rootNode.path("vetContactAdvice");
            response.setVetContactRecommended(vetContactAdvice.path("recommended").asBoolean(false));
            response.setVetContactTimeframe(vetContactAdvice.path("timeframe").asText("routine"));
            response.setVetContactReason(vetContactAdvice.path("reason").asText(""));

            return response;

        } catch (Exception e) {
            System.err.println("Failed to parse structured response: " + e.getMessage());
            return generateFallbackStructuredResponse("", null, null);
        }
    }

    private StructuredVetResponse generateFallbackStructuredResponse(String userMessage, ConversationContext context,
            List<AnalysisResult> imageAnalyses) {
        StructuredVetResponse response = new StructuredVetResponse();

        // Check if this looks like a resolution message
        if (isResolutionMessage(userMessage)) {
            // CONVERSATION CLOSURE
            response.setUrgency("LOW");
            response.setAssessment("Pet's condition appears to have improved");

            StructuredVetResponse.ResponseMessage closureMsg = new StructuredVetResponse.ResponseMessage();
            closureMsg.setType("closure");
            closureMsg.setContent(
                    "That's wonderful to hear that your pet is feeling better! I'm so glad the situation has improved. Please don't hesitate to reach out if you need anything else in the future.");
            closureMsg.setEmphasis("normal");
            response.getMessages().add(closureMsg);

            response.setVetContactRecommended(false);
            response.setVetContactTimeframe("none_needed");
            response.setVetContactReason("Issue resolved");
            response.setNextSteps(""); // Empty for closure

            return response;
        }

        // Determine urgency from keywords
        String lowerMessage = userMessage.toLowerCase();
        if (containsEmergencyKeywords(lowerMessage)) {
            response.setUrgency("CRITICAL");
            response.setAssessment("Emergency situation detected based on your description");

            StructuredVetResponse.ResponseMessage emergencyMsg = new StructuredVetResponse.ResponseMessage();
            emergencyMsg.setType("emergency");
            emergencyMsg.setContent(
                    "Based on what you've described, this sounds like it could be a serious emergency. Please contact your veterinarian immediately or visit the nearest emergency animal clinic.");
            emergencyMsg.setEmphasis("urgent");
            response.getMessages().add(emergencyMsg);

            response.getWarnings().add("Time is critical in emergency situations");
            response.setNextSteps("Contact emergency veterinary services immediately");

        } else if (lowerMessage.contains("vomiting")) {
            response.setUrgency("MEDIUM");
            response.setAssessment("Vomiting episode requiring monitoring");

            StructuredVetResponse.ResponseMessage assessmentMsg = new StructuredVetResponse.ResponseMessage();
            assessmentMsg.setType("assessment");
            assessmentMsg.setContent(
                    "Vomiting can have many causes in pets, ranging from dietary indiscretion to more serious conditions.");
            assessmentMsg.setEmphasis("normal");
            response.getMessages().add(assessmentMsg);

            // Add immediate care steps
            StructuredVetResponse.ResponseList careSteps = new StructuredVetResponse.ResponseList();
            careSteps.setTitle("Immediate Care Steps");
            careSteps.setType("numbered");
            careSteps.getItems().add("Withhold food for 12 hours but provide small amounts of water");
            careSteps.getItems().add("Monitor for other symptoms like lethargy, blood, or continued vomiting");
            careSteps.getItems().add("Contact your vet if vomiting continues or worsens");
            response.getLists().add(careSteps);

            response.getQuestions().add("How long has the vomiting been going on?");
            response.getQuestions().add("Have you noticed any other symptoms?");
            response.setNextSteps("Monitor closely and contact vet if symptoms persist or worsen");

        } else {
            response.setUrgency("LOW");
            response.setAssessment("General health inquiry");

            StructuredVetResponse.ResponseMessage greetingMsg = new StructuredVetResponse.ResponseMessage();
            greetingMsg.setType("greeting");
            greetingMsg.setContent(
                    "Thank you for sharing your concerns about your pet. I'm here to help assess the situation and provide guidance.");
            greetingMsg.setEmphasis("normal");
            response.getMessages().add(greetingMsg);

            response.getQuestions().add("Can you describe the specific symptoms you've observed?");
            response.getQuestions().add("When did these symptoms first start?");
            response.setNextSteps("Please provide more details so I can give you targeted advice");
        }

        return response;
    }

    public StructuredVetResponse generateStructuredResponse(String userMessage, ConversationContext context,
            List<AnalysisResult> imageAnalyses) {
        try {
            System.out.println("DEBUG: generateStructuredResponse called");
            System.out.println("DEBUG: userMessage = " + userMessage);
            System.out.println("DEBUG: context = " + context);
            System.out.println("DEBUG: imageAnalyses = " + (imageAnalyses != null ? imageAnalyses.size() : "null"));

            if (context != null) {
                System.out.println("DEBUG: context.getAnimalProfile() = " + context.getAnimalProfile());
                System.out.println("DEBUG: context.getIdentifiedSymptoms() = " + context.getIdentifiedSymptoms());
            }

            String contextPrompt = buildContextPrompt(context, imageAnalyses);
            System.out.println("DEBUG: contextPrompt built successfully");

            String fullPrompt = SYSTEM_PROMPT + "\n\n" + contextPrompt + "\n\nUser: " + userMessage;
            System.out.println("DEBUG: fullPrompt length = " + fullPrompt.length());

            String rawResponse = callHackClubAPI(fullPrompt);
            System.out.println("DEBUG: rawResponse received");

            return parseStructuredResponse(rawResponse);

        } catch (Exception e) {
            System.err.println("ERROR in generateStructuredResponse: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackStructuredResponse(userMessage, context, imageAnalyses);
        }
    }

    public String buildContextPrompt(ConversationContext context, List<AnalysisResult> imageAnalyses) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("CONVERSATION CONTEXT:\n");
        System.out.println("Building context prompt with conversation context: " + context.getAnimalProfile());

        if (context == null) {
            context = new ConversationContext("unknown-session");
        }

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
            if (profile.getAnimalType() != null) {
                contextBuilder.append("- Type: ").append(profile.getAnimalType()).append("\n");
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
            contextBuilder.append("\nIMAGE ANALYSIS RESULTS: \n");
            contextBuilder.append("I have analyzed ").append(imageAnalyses.size())
                    .append(" image(s) of the animal. Here are the findings:\n\n");

            for (int i = 0; i < imageAnalyses.size(); i++) {
                AnalysisResult imageAnalysis = imageAnalyses.get(i);
                contextBuilder.append("IMAGE ").append(i + 1).append(" ANALYSIS:\n");

                // Condition and confidence
                contextBuilder.append("Overall Assessment: ").append(imageAnalysis.getCondition()).append("\n");
                contextBuilder.append("Confidence Level: ")
                        .append(String.format("%.1f%%", imageAnalysis.getConfidence() * 100)).append("\n");
                contextBuilder.append("Urgency Level: ").append(imageAnalysis.getUrgency().getDisplayName())
                        .append("\n");

                // Observed symptoms
                if (!imageAnalysis.getObservedSymptoms().isEmpty()) {
                    contextBuilder.append("Observed Symptoms: ");
                    contextBuilder.append(String.join(", ", imageAnalysis.getObservedSymptoms())).append("\n");
                }

                // Detailed description
                if (imageAnalysis.getDescription() != null && !imageAnalysis.getDescription().trim().isEmpty()) {
                    contextBuilder.append("Detailed Findings: ").append(imageAnalysis.getDescription()).append("\n");
                }

                contextBuilder.append("\n");
            }

            // Summary of all image analyses
            contextBuilder.append("SUMMARY OF VISUAL FINDINGS:\n");
            boolean hasCriticalFindings = imageAnalyses.stream()
                    .anyMatch(analysis -> analysis.getUrgency() == UrgencyLevel.CRITICAL);
            boolean hasHighFindings = imageAnalyses.stream()
                    .anyMatch(analysis -> analysis.getUrgency() == UrgencyLevel.HIGH);

            if (hasCriticalFindings) {
                contextBuilder
                        .append("CRITICAL: Image analysis reveals urgent concerns requiring immediate attention.\n");
            } else if (hasHighFindings) {
                contextBuilder
                        .append("URGENT: Image analysis shows concerning findings that need prompt evaluation.\n");
            } else {
                contextBuilder.append("Image analysis did not detect any urgent visual concerns.\n");
            }

            contextBuilder.append("\n");
        }

        // Recent conversation history
        if (!context.getRecentHistory().isEmpty()) {
            contextBuilder.append("\nRecent Conversation:\n");
            List<Message> recentMessages = context.getRecentHistory();
            System.out.println("Recent messages: " + recentMessages.stream()
                    .map(msg -> msg.getMessageType() + ": " + msg.getContent())
                    .collect(Collectors.joining(" | ")));

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

        // Check for resolution message first
        if (isResolutionMessage(userMessage)) {
            return "That's wonderful to hear that your pet is feeling better! I'm so glad the situation has improved. Please don't hesitate to reach out if you need anything else in the future.";
        }

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

    // Helper method to detect resolution messages
    private boolean isResolutionMessage(String message) {
        String lowerMessage = message.toLowerCase();
        List<String> resolutionKeywords = Arrays.asList(
                "better", "fine now", "resolved", "no more", "stopped", "all good",
                "feeling better", "back to normal", "problem solved", "issue resolved",
                "seems fine", "looks better", "acting normal", "no longer", "much better");

        return resolutionKeywords.stream().anyMatch(lowerMessage::contains);
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