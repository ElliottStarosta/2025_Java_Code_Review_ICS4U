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

            // Extract and add new symptoms from current message
            List<String> newSymptoms = aiConversationService.extractSymptomsFromText(message);
            context.addSymptoms(newSymptoms);

            // Generate structured AI response with full context
            StructuredVetResponse structuredResponse = aiConversationService.generateStructuredResponse(message,
                    context, imageAnalyses);

            // Update conversation urgency level
            UrgencyLevel urgency = UrgencyLevel.valueOf(structuredResponse.getUrgency());
            conversation.setLastUrgencyLevel(urgency);
            conversationRepository.save(conversation);

            // Convert structured response to display format
            String displayResponse = convertStructuredResponseToDisplay(structuredResponse);

            updateAnimalProfileFromConversation(sessionId, message, context);

            Message botMessage = saveMessage(sessionId, displayResponse, MessageType.BOT);
            botMessage.setUrgencyLevel(urgency.name());
            messageRepository.save(botMessage);

            ChatResponse response = new ChatResponse(displayResponse);
            response.setUrgencyLevel(urgency);
            response.setStructuredData(structuredResponse);
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

    private String convertStructuredResponseToDisplay(StructuredVetResponse structuredResponse) {
        StringBuilder displayText = new StringBuilder();

        // Add message segments with split markers
        List<StructuredVetResponse.ResponseMessage> messages = structuredResponse.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            StructuredVetResponse.ResponseMessage msg = messages.get(i);

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

        // Add structured content as separate messages if present
        if (!structuredResponse.getLists().isEmpty() ||
                !structuredResponse.getWarnings().isEmpty() ||
                !structuredResponse.getQuestions().isEmpty()) {

            displayText.append("|||SPLIT|||");

            // Add lists
            for (StructuredVetResponse.ResponseList list : structuredResponse.getLists()) {
                if (list.getTitle() != null && !list.getTitle().isEmpty()) {
                    displayText.append("**").append(list.getTitle()).append("**\n");
                }

                if ("numbered".equals(list.getType())) {
                    for (int i = 0; i < list.getItems().size(); i++) {
                        displayText.append((i + 1)).append(". ").append(list.getItems().get(i)).append("\n");
                    }
                } else {
                    for (String item : list.getItems()) {
                        displayText.append("• ").append(item).append("\n");
                    }
                }
                displayText.append("\n");
            }

            // Add warnings
            if (!structuredResponse.getWarnings().isEmpty()) {
                displayText.append("**⚠️ Important:**\n");
                for (String warning : structuredResponse.getWarnings()) {
                    displayText.append("• ").append(warning).append("\n");
                }
                displayText.append("\n");
            }

            // Add questions
            if (!structuredResponse.getQuestions().isEmpty()) {
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

        return displayText.toString().trim();
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
                context.setIdentifiedSymptoms(Arrays.asList(symptoms.split(",\\s*")));
            }
        }

        // Accumulate symptoms from ALL messages in conversation
        Set<String> accumulatedSymptoms = new HashSet<>();
        for (Message message : allMessages) {
            if (message.getMessageType() == MessageType.USER) {
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