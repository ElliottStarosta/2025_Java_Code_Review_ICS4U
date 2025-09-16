package com.virtualvet.service;

import com.virtualvet.entity.*;
import com.virtualvet.enums.entity.MessageType;
import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.model.*;
import com.virtualvet.dto.*;
import com.virtualvet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing chat conversations and message processing in the Virtual Vet application.
 * 
 * This service orchestrates the complete chat workflow, including session management,
 * message processing, image analysis integration, AI response generation, and conversation
 * history management. It serves as the central hub for all chat-related operations,
 * coordinating between various services to provide comprehensive veterinary consultation
 * capabilities.
 * 
 * The service handles user message processing, image upload and analysis, animal profile
 * updates, AI response generation with structured data, conversation context management,
 * and provides utilities for conversation history retrieval and message conversion.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Service
@Transactional
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AnimalProfileRepository animalProfileRepository;

    @Autowired
    private AIConversationService aiConversationService;

    @Autowired
    private ImageAnalysisService imageAnalysisService;

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private AnimalProfileService animalProfileService;

    public SessionStartResponse startNewConversation() {
        try {
            String sessionId = UUID.randomUUID().toString();
            Conversation conversation = new Conversation(sessionId);
            conversation = conversationRepository.save(conversation);

            // Send initial welcome message
            String welcomeMessage = "Hello! I'm your virtual veterinary assistant. I can help you assess your pet's health concerns, analyze symptoms, and provide guidance on when to seek veterinary care. Please tell me about your pet and what concerns you have today.";
            saveMessage(sessionId, welcomeMessage, MessageType.BOT);

            return new SessionStartResponse(sessionId, conversation.getId());
        } catch (Exception e) {
            return SessionStartResponse.error("Failed to start conversation: " + e.getMessage());
        }
    }

    public ChatResponse processMessage(String sessionId, String message, MultipartFile[] images) {
        try {
            Conversation conversation = getOrCreateConversation(sessionId);
            conversation.updateLastActivity();

            // Build context BEFORE processing the message to get full conversation history
            ConversationContext context = buildConversationContext(sessionId);

            conversationRepository.save(conversation);

            Message userMessage = saveMessage(sessionId, message, MessageType.USER);

            // Process multiple images if provided
            List<AnalysisResult> imageAnalyses = new ArrayList<>();
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        AnalysisResult imageAnalysis = imageAnalysisService.analyzeAnimalImage(image);
                        imageAnalyses.add(imageAnalysis);

                        String imageUrl = imageAnalysisService.saveImage(image, sessionId);
                        if (userMessage.getImageUrl() == null) {
                            userMessage.setImageUrl(imageUrl);
                        } else {
                            userMessage.setImageUrl(userMessage.getImageUrl() + "," + imageUrl);
                        }
                    }
                }
                messageRepository.save(userMessage);
            }

            AnimalProfile updatedProfile = updateAnimalProfileFromMessage(sessionId, message, imageAnalyses);

            if (updatedProfile != null) {
                // Refresh context with updated profile
                context = buildConversationContext(sessionId);
            }

            // Update context with all image analyses
            for (AnalysisResult imageAnalysis : imageAnalyses) {
                if (imageAnalysis != null && imageAnalysis.getObservedSymptoms() != null) {
                    context.addSymptoms(imageAnalysis.getObservedSymptoms());
                }
                // Add null check for urgency
                if (imageAnalysis != null && imageAnalysis.getUrgency() != null &&
                        imageAnalysis.getUrgency().ordinal() > context.getCurrentUrgency().ordinal()) {
                    context.setCurrentUrgency(imageAnalysis.getUrgency());
                }
            }

            // Generate structured AI response with full context
            StructuredVetResponse structuredResponse = aiConversationService.generateStructuredResponse(message,
                    context, imageAnalyses);

            updateProfileWithAISymptoms(sessionId, structuredResponse);

            UrgencyLevel urgencyLevel = UrgencyLevel.LOW; // Default value

            // Update conversation urgency level
            if (structuredResponse != null && structuredResponse.getUrgency() != null) {
                try {
                    urgencyLevel = UrgencyLevel.valueOf(structuredResponse.getUrgency());
                    conversation.setLastUrgencyLevel(urgencyLevel);
                    conversationRepository.save(conversation);
                } catch (IllegalArgumentException e) {
                    // Handle invalid urgency string
                    conversation.setLastUrgencyLevel(UrgencyLevel.LOW);
                    conversationRepository.save(conversation);
                }
            }

            // Convert structured response to display format
            String displayResponse = convertStructuredResponseToDisplay(structuredResponse);

            Message botMessage = saveMessage(sessionId, displayResponse, MessageType.BOT);
            botMessage.setUrgencyLevel(urgencyLevel.name());
            messageRepository.save(botMessage);

            ChatResponse response = new ChatResponse(displayResponse);
            // Add null checks for structured response
            if (structuredResponse != null) {
                response.setUrgencyLevel(urgencyLevel);
                response.setStructuredData(structuredResponse);
            } else {
                response.setUrgencyLevel(UrgencyLevel.LOW);
            }
            response.setRecommendations(generateRecommendations(response.getUrgencyLevel(), context, imageAnalyses));

           
            response.addContextValue("animalType",
                    context.getAnimalProfile() != null ? context.getAnimalProfile().getAnimalType() : null);
            response.addContextValue("symptomsIdentified", context.getIdentifiedSymptoms());
            response.addContextValue("urgencyLevel", urgencyLevel.getDisplayName());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.error("Failed to process message: " + e.getMessage());
        }
    }

    private AnimalProfile updateAnimalProfileFromMessage(String sessionId, String message,
            List<AnalysisResult> imageAnalyses) {
        try {
            // Update profile from user message
            AnimalProfile profile = animalProfileService.updateFromMessage(sessionId, message);

            if (profile == null) {
                // Create a new profile if it doesn't exist
                Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
                if (conversation.isPresent()) {
                    profile = new AnimalProfile(conversation.get());
                    profile = animalProfileRepository.save(profile);
                } else {
                    return null;
                }
            }

            // Update profile with symptoms from image analyses
            if (imageAnalyses != null && !imageAnalyses.isEmpty()) {
                List<String> imageSymptoms = new ArrayList<>();
                for (AnalysisResult analysis : imageAnalyses) {
                    if (analysis != null && analysis.getObservedSymptoms() != null) {
                        imageSymptoms.addAll(analysis.getObservedSymptoms());
                    }
                }

                if (!imageSymptoms.isEmpty()) {
                    profile = animalProfileService.addSymptomsToProfile(sessionId, imageSymptoms);
                }
            }

            return profile;
        } catch (Exception e) {
            System.err.println("Error updating animal profile: " + e.getMessage());
            return null;
        }
    }

    private void updateProfileWithAISymptoms(String sessionId, StructuredVetResponse structuredResponse) {
        try {
            if (structuredResponse == null) {
                return;
            }

            // Extract symptoms from structured response content
            List<String> aiIdentifiedSymptoms = extractSymptomsFromStructuredResponse(structuredResponse);

            if (!aiIdentifiedSymptoms.isEmpty()) {
                animalProfileService.addSymptomsToProfile(sessionId, aiIdentifiedSymptoms);
            }
        } catch (Exception e) {
            System.err.println("Error updating profile with AI symptoms: " + e.getMessage());
        }
    }

    private List<String> extractSymptomsFromStructuredResponse(StructuredVetResponse response) {
        List<String> symptoms = new ArrayList<>();

        if (response == null) {
            return symptoms;
        }

        // Check assessment for symptoms
        if (response.getAssessment() != null) {
            symptoms.addAll(aiConversationService.extractSymptomsFromText(response.getAssessment()));
        }

        // Check message content for symptoms
        if (response.getMessages() != null) {
            for (StructuredVetResponse.ResponseMessage message : response.getMessages()) {
                if (message != null && message.getContent() != null) {
                    symptoms.addAll(aiConversationService.extractSymptomsFromText(message.getContent()));
                }
            }
        }

        return symptoms.stream().distinct().collect(Collectors.toList());
    }

    private String convertStructuredResponseToDisplay(StructuredVetResponse structuredResponse) {
        if (structuredResponse == null) {
            return "I'm sorry, I couldn't generate a response at this time. Please try again.";
        }

        StringBuilder displayText = new StringBuilder();

        // Add message segments with split markers
        if (structuredResponse.getMessages() != null && !structuredResponse.getMessages().isEmpty()) {
            List<StructuredVetResponse.ResponseMessage> messages = structuredResponse.getMessages();
            for (int i = 0; i < messages.size(); i++) {
                StructuredVetResponse.ResponseMessage msg = messages.get(i);

                if (msg != null && msg.getContent() != null) {
                    String content = msg.getContent();
                    if ("urgent".equals(msg.getEmphasis()) || "bold".equals(msg.getEmphasis())) {
                        content = "**" + content + "**";
                    }

                    displayText.append(content);

                    // Add split marker between message segments (except last one)
                    if (i < messages.size() - 1) {
                        displayText.append("|||SPLIT|||");
                    }
                }
            }
        }

        // Add structured content as separate messages if present
        if ((structuredResponse.getLists() != null && !structuredResponse.getLists().isEmpty()) ||
                (structuredResponse.getWarnings() != null && !structuredResponse.getWarnings().isEmpty()) ||
                (structuredResponse.getQuestions() != null && !structuredResponse.getQuestions().isEmpty())) {

            displayText.append("|||SPLIT|||");

            // Add lists
            if (structuredResponse.getLists() != null) {
                for (StructuredVetResponse.ResponseList list : structuredResponse.getLists()) {
                    if (list != null) {
                        if (list.getTitle() != null && !list.getTitle().isEmpty()) {
                            displayText.append("**").append(list.getTitle()).append("**\n");
                        }

                        if (list.getItems() != null && !list.getItems().isEmpty()) {
                            if ("numbered".equals(list.getType())) {
                                for (int i = 0; i < list.getItems().size(); i++) {
                                    displayText.append((i + 1)).append(". ").append(list.getItems().get(i))
                                            .append("\n");
                                }
                            } else {
                                for (String item : list.getItems()) {
                                    displayText.append("• ").append(item).append("\n");
                                }
                            }
                            displayText.append("\n");
                        }
                    }
                }
            }

            // Add warnings
            if (structuredResponse.getWarnings() != null && !structuredResponse.getWarnings().isEmpty()) {
                displayText.append("|||SPLIT|||");
                displayText.append("**⚠️ Important:**\n");
                for (String warning : structuredResponse.getWarnings()) {
                    displayText.append("• ").append(warning).append("\n");
                }
                displayText.append("\n");
            }

            // Add questions
            if (structuredResponse.getQuestions() != null && !structuredResponse.getQuestions().isEmpty()) {
                displayText.append("|||SPLIT|||");
                displayText.append("**Questions to help assess further:**\n");
                for (String question : structuredResponse.getQuestions()) {
                    displayText.append("• ").append(question).append("\n");
                }
                displayText.append("\n");
            }
        }

        // Add next steps as final message if present
        if (structuredResponse.getNextSteps() != null && !structuredResponse.getNextSteps().isEmpty()) {
            displayText.append("|||SPLIT|||");
            displayText.append("**Next Steps:** ").append(structuredResponse.getNextSteps());
        }

        String result = displayText.toString().trim();
        return result.isEmpty()
                ? "I'm here to help with your pet's health concerns. Please tell me more about what you're observing."
                : result;
    }

    public ConversationHistoryResponse getConversationHistory(String sessionId) {
        try {
            Optional<Conversation> conversationOpt = conversationRepository.findBySessionId(sessionId);
            if (!conversationOpt.isPresent()) {
                ConversationHistoryResponse response = new ConversationHistoryResponse();
                response.setSuccess(false);
                return response;
            }

            Conversation conversation = conversationOpt.get();
            List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());

            ConversationHistoryResponse response = new ConversationHistoryResponse();
            response.setSessionId(sessionId);
            response.setTotalMessages(messages.size());

            // Convert messages to DTOs
            List<MessageDto> messageDtos = messages.stream()
                    .map(this::convertToMessageDto)
                    .collect(Collectors.toList());
            response.setMessages(messageDtos);

            // Get animal profile
            List<AnimalProfile> profiles = animalProfileRepository.findByConversationId(conversation.getId());
            if (!profiles.isEmpty()) {
                response.setAnimalProfile(convertToAnimalProfileDto(profiles.get(0)));
            }

            return response;

        } catch (Exception e) {
            ConversationHistoryResponse response = new ConversationHistoryResponse();
            response.setSuccess(false);
            return response;
        }
    }

    public Message saveMessage(String sessionId, String content, MessageType messageType) {
        Conversation conversation = getOrCreateConversation(sessionId);
        Message message = new Message(conversation, messageType, content);
        return messageRepository.save(message);
    }

    private Conversation getOrCreateConversation(String sessionId) {
        return conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Conversation newConversation = new Conversation(sessionId);
                    return conversationRepository.save(newConversation);
                });
    }

    public ConversationContext buildConversationContext(String sessionId) {
        ConversationContext context = new ConversationContext(sessionId);

        // Get conversation
        Optional<Conversation> conversationOpt = conversationRepository.findBySessionId(sessionId);
        if (!conversationOpt.isPresent()) {
            return context;
        }

        Conversation conversation = conversationOpt.get();

        if (conversation.getLastUrgencyLevel() != null) {
            context.setCurrentUrgency(conversation.getLastUrgencyLevel());
        }

        // Get ALL messages, not just recent ones
        // This ensures the AI remembers the entire conversation
        List<Message> allMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());
        context.setRecentHistory(allMessages); // Pass all messages, not limited to 10

        // Get animal profile
        List<AnimalProfile> profiles = animalProfileRepository.findByConversationId(conversation.getId());
        if (!profiles.isEmpty()) {
            context.setAnimalProfile(profiles.get(0));
            System.out.println("Animal Profile: " + profiles.get(0));

            // Parse symptoms from profile
            String symptoms = profiles.get(0).getSymptoms();
            if (symptoms != null && !symptoms.trim().isEmpty()) {
                context.setIdentifiedSymptoms(new ArrayList<>(Arrays.asList(symptoms.split(",\\s*"))));
            }
        }

        // Accumulate symptoms from ALL messages in conversation
        Set<String> accumulatedSymptoms = new HashSet<>();
        for (Message message : allMessages) {
            if (message.getMessageType() == MessageType.USER && message.getContent() != null) {
                List<String> messageSymptoms = aiConversationService.extractSymptomsFromText(message.getContent());
                accumulatedSymptoms.addAll(messageSymptoms);
            }
        }

        // Add accumulated symptoms to context
        if (!accumulatedSymptoms.isEmpty()) {
            List<String> currentSymptoms = new ArrayList<>(context.getIdentifiedSymptoms());
            currentSymptoms.addAll(accumulatedSymptoms);
            context.setIdentifiedSymptoms(currentSymptoms);
        }

        return context;
    }

    private List<String> generateRecommendations(UrgencyLevel urgency, ConversationContext context,
            List<AnalysisResult> imageAnalyses) {
        List<String> recommendations = new ArrayList<>();

        if (urgency != null) {
            recommendations.add(urgency.getRecommendation());
        } else {
            recommendations.add(UrgencyLevel.LOW.getRecommendation());
        }

        if (context != null && context.getIdentifiedSymptoms() != null) {
            if (context.getIdentifiedSymptoms().contains("vomiting")) {
                recommendations.add("Withhold food for 12 hours, provide small amounts of water");
            }

            if (context.getIdentifiedSymptoms().contains("limping")) {
                recommendations.add("Limit physical activity and observe for swelling");
            }
        }

        // Add recommendations from all image analyses
        if (imageAnalyses != null) {
            for (AnalysisResult imageAnalysis : imageAnalyses) {
                if (imageAnalysis != null && imageAnalysis.getConfidence() > 0.7) {
                    recommendations.add("Image analysis suggests: " +
                            (imageAnalysis.getDescription() != null ? imageAnalysis.getDescription()
                                    : "No description available"));
                }
            }
        }

        return recommendations;
    }

    private MessageDto convertToMessageDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setImageUrl(message.getImageUrl());
        dto.setUrgencyLevel(message.getUrgencyLevel());
        return dto;
    }

    private AnimalProfileDto convertToAnimalProfileDto(AnimalProfile profile) {
        AnimalProfileDto dto = new AnimalProfileDto();
        dto.setId(profile.getId());
        dto.setAnimalType(profile.getAnimalType());
        dto.setBreed(profile.getBreed());
        dto.setAge(profile.getAge());
        dto.setWeight(profile.getWeight());

        if (profile.getSymptoms() != null && !profile.getSymptoms().trim().isEmpty()) {
            dto.setSymptoms(Arrays.asList(profile.getSymptoms().split(",\\s*")));
        }

        return dto;
    }
}