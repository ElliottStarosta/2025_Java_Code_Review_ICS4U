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
                        // For multiple images, you might want to store them differently
                        // or update your Message entity to support multiple image URLs
                        if (userMessage.getImageUrl() == null) {
                            userMessage.setImageUrl(imageUrl);
                        } else {
                            userMessage.setImageUrl(userMessage.getImageUrl() + "," + imageUrl);
                        }
                    }
                }
                messageRepository.save(userMessage);
            }

            ConversationContext context = buildConversationContext(sessionId);
            UrgencyLevel messageUrgency = analyzeMessageUrgency(message);

            if (messageUrgency.ordinal() > context.getCurrentUrgency().ordinal()) {
                context.setCurrentUrgency(messageUrgency);
            }

            // Update context with all image analyses
            for (AnalysisResult imageAnalysis : imageAnalyses) {
                context.addSymptoms(imageAnalysis.getObservedSymptoms());
                if (imageAnalysis.getUrgency().ordinal() > context.getCurrentUrgency().ordinal()) {
                    context.setCurrentUrgency(imageAnalysis.getUrgency());
                }
            }

            // Generate AI response with multiple image analyses
            String aiResponse = aiConversationService.generateResponse(message, context, imageAnalyses);

            UrgencyLevel urgency = context.getCurrentUrgency();
            updateAnimalProfileFromConversation(sessionId, message, context);

            Message botMessage = saveMessage(sessionId, aiResponse, MessageType.BOT);
            botMessage.setUrgencyLevel(urgency.name());
            messageRepository.save(botMessage);

            ChatResponse response = new ChatResponse(aiResponse);
            response.setUrgencyLevel(urgency);
            response.setRecommendations(generateRecommendations(urgency, context, imageAnalyses));

            if (urgency.isEmergency()) {
                response.setNearbyVets(emergencyService.findNearbyVets(0.0, 0.0, 25));
            }

            response.addContextValue("animalType",
                    context.getAnimalProfile() != null ? context.getAnimalProfile().getAnimalType() : null);
            response.addContextValue("symptomsIdentified", context.getIdentifiedSymptoms());
            response.addContextValue("urgencyLevel", urgency.getDisplayName());

            return response;

        } catch (Exception e) {
            return ChatResponse.error("Failed to process message: " + e.getMessage());
        }
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

    private ConversationContext buildConversationContext(String sessionId) {
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

        // Get recent messages (last 10)
        List<Message> recentMessages = messageRepository.findRecentMessagesByConversationId(
                conversation.getId(), 10);
        context.setRecentHistory(recentMessages);

        // Get animal profile
        List<AnimalProfile> profiles = animalProfileRepository.findByConversationId(conversation.getId());
        if (!profiles.isEmpty()) {
            context.setAnimalProfile(profiles.get(0));

            // Parse symptoms from profile
            String symptoms = profiles.get(0).getSymptoms();
            if (symptoms != null && !symptoms.trim().isEmpty()) {
                context.setIdentifiedSymptoms(Arrays.asList(symptoms.split(",\\s*")));
            }
        }

        return context;
    }

    private UrgencyLevel determineOverallUrgency(ConversationContext context, AnalysisResult imageAnalysis,
            String message) {
        UrgencyLevel messageUrgency = analyzeMessageUrgency(message);
        UrgencyLevel contextUrgency = context.getCurrentUrgency();
        UrgencyLevel imageUrgency = imageAnalysis != null ? imageAnalysis.getUrgency() : UrgencyLevel.LOW;

        // Return the highest urgency level
        return Collections.max(Arrays.asList(messageUrgency, contextUrgency, imageUrgency));
    }

    private UrgencyLevel analyzeMessageUrgency(String message) {
        String lowerMessage = message.toLowerCase();

        // Critical keywords
        if (containsAny(lowerMessage, Arrays.asList("not breathing", "unconscious", "bleeding heavily",
                "convulsing", "seizure", "choking", "collapsed", "emergency"))) {
            return UrgencyLevel.CRITICAL;
        }

        // High urgency keywords
        if (containsAny(lowerMessage, Arrays.asList("vomiting blood", "difficulty breathing",
                "severe pain", "won't eat", "lethargic", "diarrhea", "limping badly"))) {
            return UrgencyLevel.HIGH;
        }

        // Medium urgency keywords
        if (containsAny(lowerMessage, Arrays.asList("vomiting", "limping", "not eating well",
                "scratching a lot", "seems uncomfortable", "discharge"))) {
            return UrgencyLevel.MEDIUM;
        }

        return UrgencyLevel.LOW;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private void updateAnimalProfileFromConversation(String sessionId, String message, ConversationContext context) {
        animalProfileService.updateFromMessage(sessionId, message);
    }

    private List<String> generateRecommendations(UrgencyLevel urgency, ConversationContext context,
        List<AnalysisResult> imageAnalyses) {
    List<String> recommendations = new ArrayList<>();
    
    recommendations.add(urgency.getRecommendation());

    if (context.getIdentifiedSymptoms().contains("vomiting")) {
        recommendations.add("Withhold food for 12 hours, provide small amounts of water");
    }

    if (context.getIdentifiedSymptoms().contains("limping")) {
        recommendations.add("Limit physical activity and observe for swelling");
    }

    // Add recommendations from all image analyses
    for (AnalysisResult imageAnalysis : imageAnalyses) {
        if (imageAnalysis != null && imageAnalysis.getConfidence() > 0.7) {
            recommendations.add("Image analysis suggests: " + imageAnalysis.getDescription());
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