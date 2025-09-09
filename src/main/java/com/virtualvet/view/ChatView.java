package com.virtualvet.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.virtualvet.dto.StructuredVetResponse;
import com.virtualvet.model.ConversationContext;
import com.virtualvet.service.ChatService;
import com.virtualvet.util.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.function.SerializableRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Chat | Virtual Vet")
public class ChatView extends VerticalLayout {

    private VerticalLayout messagesContainer;
    private Scroller messagesScroller;
    private TextArea messageInput;
    private Button sendButton;

    private Button uploadButton;
    private Upload upload;
    private MultiFileMemoryBuffer multiFileBuffer;
    private List<UploadedFileData> uploadedFiles = new ArrayList<>();

    private String lastMessageSender = null; // Track who sent the last message
    private List<Div> currentMessageGroup = new ArrayList<>(); // Track current message group

    private StructuredVetResponse lastStructuredResponse;

    private Div imagePreviewContainer;

    @Autowired
    private ChatService chatService;

    private double userLatitude = 0.0;
    private double userLongitude = 0.0;

    private String currentSessionId;
    private Div emergencyBanner;
    private boolean isWaitingForResponse = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Div> messageElements = new ArrayList<>();

    public ChatView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        createComponents();
        setupLayout();
        startNewSession();

    }

    private void createComponents() {
        // Emergency banner (hidden by default)
        emergencyBanner = new Div();
        emergencyBanner.setVisible(false);

        // Messages container - phone-like chat
        messagesContainer = new VerticalLayout();
        messagesContainer.setPadding(false);
        messagesContainer.setSpacing(false);
        messagesContainer.setWidthFull();
        messagesContainer.getStyle()
                .set("padding", "1rem")
                .set("min-height", "0")
                .set("padding-bottom", "80px")
                .set("flex", "1");

        messagesScroller = new Scroller(messagesContainer);
        messagesScroller.setSizeFull();
        messagesScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        messagesScroller.getStyle()
                .set("flex", "1")
                .set("height", "100%")
                .set("min-height", "0")
                .set("background", "white")
                .set("overflow-x", "hidden")
                .set("overflow-y", "auto");
    }

    private void createInputArea() {
        // Message input with phone-like styling
        messageInput = new TextArea();
        messageInput.setPlaceholder("Type a message...");
        messageInput.setMaxLength(2000);
        messageInput.setWidth("calc(100% - 90px)"); // Space for both buttons
        messageInput.setMinHeight("44px");
        messageInput.setMaxHeight("120px");
        messageInput.getElement().setAttribute("has-value", "");
        messageInput.getElement().getStyle()
                .set("--vaadin-text-area-max-height", "120px")
                .set("overflow", "hidden");

        messageInput.getStyle()
                .set("border-radius", "22px")
                .set("border", "1px solid #d1d5db")
                .set("background", "#f9fafb")
                .set("color", "#1f2937")
                .set("font-size", "16px")
                .set("font-family", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif")
                .set("resize", "none")
                .set("padding", "12px 90px 12px 16px") // Right padding for both buttons
                .set("transition", "all 0.2s ease");

        messageInput.addFocusListener(e -> messageInput.getStyle()
                .set("border-color", "#2563eb")
                .set("background", "white"));

        messageInput.addBlurListener(e -> messageInput.getStyle()
                .set("border-color", "#d1d5db")
                .set("background", "#f9fafb"));

        // messageInput.addKeyDownListener(Key.ENTER, e -> {
        // if (!e.isShiftKey()) {
        // sendMessage();
        // }
        // });

        // File upload component
        multiFileBuffer = new MultiFileMemoryBuffer();

        upload = new Upload(multiFileBuffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
        upload.setMaxFiles(5);
        upload.setMaxFileSize(10 * 1024 * 1024);
        upload.setDropAllowed(false);

        upload.addSucceededListener(event -> {
            try {
                String fileName = event.getFileName();
                InputStream inputStream = multiFileBuffer.getInputStream(fileName);
                byte[] data = inputStream.readAllBytes();
                String contentType = event.getMIMEType();

                UploadedFileData fileData = new UploadedFileData(fileName, contentType, data);
                uploadedFiles.add(fileData);

                getUI().ifPresent(ui -> ui.access(() -> {
                    showImagePreview(fileData);
                    showNotification("Image attached: " + fileName, false);
                    ui.push();
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Hide the default upload component
        upload.getStyle()
                .set("position", "absolute")
                .set("right", "50px")
                .set("top", "75%")
                .set("transform", "translateY(-50%)")
                .set("width", "32px")
                .set("height", "32px")
                .set("opacity", "0")
                .set("z-index", "20")
                .set("pointer-events", "none");

        // Upload button with blue background
        uploadButton = new Button();
        Icon uploadIcon = new Icon(VaadinIcon.PAPERCLIP);
        uploadIcon.setSize("18px");
        uploadIcon.setColor("white");
        uploadButton.setIcon(uploadIcon);
        uploadButton.getStyle()
                .set("background", "#2563eb")
                .set("border", "none")
                .set("border-radius", "50%")
                .set("width", "32px")
                .set("height", "32px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("position", "absolute")
                .set("right", "50px")
                .set("top", "50%")
                .set("transform", "translateY(-50%)")
                .set("z-index", "10")
                .set("transition", "all 0.2s ease")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        uploadButton.addClickListener(e -> {
            upload.getElement().executeJs("this.shadowRoot.querySelector('input[type=file]').click()");
        });

        // Send button with blue background
        sendButton = new Button();
        Icon sendIcon = new Icon(VaadinIcon.PAPERPLANE);
        sendIcon.setSize("16px");
        sendIcon.setColor("white");
        sendButton.setIcon(sendIcon);
        sendButton.getStyle()
                .set("background", "#2563eb")
                .set("border", "none")
                .set("border-radius", "50%")
                .set("width", "32px")
                .set("height", "32px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("position", "absolute")
                .set("right", "8px")
                .set("top", "50%")
                .set("transform", "translateY(-50%)")
                .set("z-index", "10")
                .set("transition", "all 0.2s ease")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        sendButton.addClickListener(e -> sendMessage());

        // File upload event handlers
        upload.addFileRejectedListener(event -> {
            showNotification("File rejected: " + event.getErrorMessage(), true);
        });

        upload.addFailedListener(event -> {
            showNotification("Upload failed: " + event.getReason().getMessage(), true);
        });

        // Input container with buttons
        Div inputContainer = new Div();
        inputContainer.getStyle()
                .set("position", "relative")
                .set("width", "100%")
                .set("display", "flex")
                .set("align-items", "center");

        inputContainer.add(messageInput, uploadButton, sendButton, upload);

        // Bottom input area
        VerticalLayout inputArea = new VerticalLayout();
        inputArea.setPadding(false);
        inputArea.setSpacing(false);
        inputArea.setWidthFull();
        inputArea.add(inputContainer);
        inputArea.getStyle()
                .set("background", "white")
                .set("border-top", "1px solid #e5e7eb")
                .set("padding", "12px 16px")
                .set("flex-shrink", "0");

        // Create sticky container
        Div bottomStickyContainer = new Div(inputArea);
        bottomStickyContainer.getStyle()
                .set("position", "sticky")
                .set("bottom", "0")
                .set("z-index", "100")
                .set("background", "white")
                .set("width", "100%");

        add(bottomStickyContainer);
    }

    private void showImagePreview(UploadedFileData fileData) {
        if (imagePreviewContainer == null) {
            imagePreviewContainer = new Div();
            imagePreviewContainer.getStyle()
                    .set("display", "flex")
                    .set("gap", "8px")
                    .set("flex-wrap", "wrap")
                    .set("margin-bottom", "8px")
                    .set("padding", "0 16px");

            // Insert before the input area
            getChildren().forEach(component -> {
                if (component.getElement().getTag().equals("div") &&
                        component.getElement().getStyle().get("position").equals("sticky")) {
                    getElement().insertChild(getElement().getChildCount() - 1, imagePreviewContainer.getElement());
                }
            });
        }

        Div preview = new Div();
        preview.getStyle()
                .set("position", "relative")
                .set("display", "inline-block");

        Image thumbnail = new Image("data:" + fileData.getContentType() + ";base64," + fileData.getBase64Data(),
                "Preview");
        thumbnail.getStyle()
                .set("width", "60px")
                .set("height", "60px")
                .set("object-fit", "cover")
                .set("border-radius", "8px")
                .set("border", "2px solid #e5e7eb");

        Button removeButton = new Button();
        Icon removeIcon = new Icon(VaadinIcon.CLOSE_SMALL);
        removeIcon.setSize("16px");
        removeIcon.getStyle().set("margin-top", "-4px");
        removeIcon.setColor("white");
        removeButton.setIcon(removeIcon);
        removeButton.getStyle()
                .set("position", "absolute")
                .set("top", "4px")
                .set("right", "4px")
                .set("width", "20px")
                .set("height", "20px")
                .set("min-width", "20px")
                .set("min-height", "20px")
                .set("max-width", "20px")
                .set("max-height", "20px")
                .set("box-sizing", "border-box")
                .set("border-radius", "50%")
                .set("background", "rgba(0,0,0,0.6)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("padding", "0")
                .set("margin", "0")
                .set("line-height", "1");

        removeButton.addClickListener(e -> {
            uploadedFiles.removeIf(f -> f.getFilename().equals(fileData.getFilename()));
            imagePreviewContainer.remove(preview);
            if (uploadedFiles.isEmpty()) {
                remove(imagePreviewContainer);
                imagePreviewContainer = null;
            }
        });

        preview.add(thumbnail, removeButton);
        imagePreviewContainer.add(preview);
    }

    private void setupLayout() {
        // Force the main ChatView to have proper constraints
        getStyle()
                .set("height", "100vh")
                .set("max-height", "100vh")
                .set("overflow", "hidden") // Critical: prevent any scrolling on main container
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("background", "white")
                .set("padding", "0")
                .set("margin", "0");

        // Ensure Vaadin's VerticalLayout doesn't add its own spacing/padding
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        // Create messages container that takes up remaining space
        VerticalLayout messagesArea = new VerticalLayout();
        messagesArea.setPadding(false);
        messagesArea.setSpacing(false);
        messagesArea.setSizeFull();
        messagesArea.getStyle()
                .set("flex", "1 1 0") // Flex grow, shrink, and base 0
                .set("min-height", "0") // Critical: allows flex child to shrink below content size
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "0"); // Force height calculation from flex

        // Add emergency banner and messages scroller to messages area
        messagesArea.add(emergencyBanner, messagesScroller);
        messagesArea.setFlexGrow(1, messagesScroller);

        // Add messages area to main layout
        add(messagesArea);
        setFlexGrow(1, messagesArea);

        // Create input area separately - this will be sticky at bottom
        createInputArea();
    }

    private void addWelcomeMessage() {
        addBotMessages(List.of(
                "👋 Hello! I'm your Virtual Vet Assistant.",
                "I'm here to help you with:\n🔸 Pet health concerns and symptoms\n🔸 Emergency guidance and triage\n🔸 General veterinary advice",
                "⚠️ **For true emergencies, please contact your local veterinary clinic immediately!**",
                "How can I help you and your pet today?"));
    }

    private void clearImagePreviews() {
        if (imagePreviewContainer != null) {
            remove(imagePreviewContainer);
            imagePreviewContainer = null;
        }
    }

    @ClientCallable
    private void sendMessage() {
        String message = messageInput.getValue().trim();
        if (message.isEmpty() || isWaitingForResponse) {
            return;
        }

        conversationHistory.add(new ConversationMessage("user", message));

        if (currentSessionId == null) {
            showNotification("Please wait, initializing session...", false);
            return;
        }

        // Add user message with attachments
        addUserMessage(message, new ArrayList<>(uploadedFiles));
        messageInput.clear();
        clearImagePreviews();

        // Clear uploaded files immediately
        List<UploadedFileData> filesToProcess = new ArrayList<>(uploadedFiles);
        uploadedFiles.clear();

        // Show typing indicator and disable input
        showTypingIndicator();
        setInputEnabled(false);
        scrollToBottom();

        // Process the message
        analyzeImagesAsync(filesToProcess)
                .thenCompose(imageAnalysisResult -> {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();

                            requestBody.add("sessionId", currentSessionId);

                            // Combine user message with image analysis results
                            String finalMessage = message;
                            if (!imageAnalysisResult.isEmpty()) {
                                finalMessage = message + "\n\nImage analysis results:\n" + imageAnalysisResult;
                            }
                            requestBody.add("message", finalMessage);
                            ConversationContext context = chatService.buildConversationContext(currentSessionId);
                            System.out.println("Built context: " + context);
                            requestBody.add("conversationHistory", context);

                            // Include the actual image files
                            for (UploadedFileData fileData : filesToProcess) {
                                ByteArrayResource fileResource = new ByteArrayResource(fileData.getData()) {
                                    @Override
                                    public String getFilename() {
                                        return fileData.getFilename();
                                    }
                                };
                                requestBody.add("images", fileResource);
                            }

                            String response = ApiClient.postMultipart("http://localhost:8080/api/chat/message",
                                    requestBody);
                            JsonNode jsonResponse = objectMapper.readTree(response);
                            System.out.println("JSON RESPONSE: " + jsonResponse);
                            return jsonResponse.path("response").asText("Sorry, I couldn't generate a response.");

                        } catch (Exception e) {
                            System.err.println("API Error: " + e.getMessage());
                            return "I'm having trouble connecting right now. Please try again in a moment.";
                        }
                    });
                })
                .thenAccept(responseText -> {
                    UI ui = getUI().orElse(null);
                    if (ui != null) {
                        ui.access(() -> {
                            try {
                                removeTypingIndicator();
                                System.out.println("Bot response: " + responseText);
                                List<String> messages = splitIntoMessages(responseText);
                                addBotMessages(messages);
                                checkForEmergency(responseText);
                                setInputEnabled(true);
                                messageInput.focus();
                                scrollToBottom();
                                ui.push();
                            } catch (Exception e) {
                                System.err.println("UI update error: " + e.getMessage());
                            }
                        });
                    }
                })
                .exceptionally(throwable -> {
                    UI ui = getUI().orElse(null);
                    if (ui != null) {
                        ui.access(() -> {
                            try {
                                removeTypingIndicator();
                                addBotMessage("I'm experiencing technical difficulties. Please try again.", true);
                                setInputEnabled(true);
                                messageInput.focus();
                                scrollToBottom();
                                ui.push();
                            } catch (Exception e) {
                                System.err.println("Error handling UI update: " + e.getMessage());
                            }
                        });
                    }
                    return null;
                });
    }

    private CompletableFuture<String> analyzeImagesAsync(List<UploadedFileData> images) {
        if (images.isEmpty()) {
            return CompletableFuture.completedFuture("");
        }

        return CompletableFuture.supplyAsync(() -> {
            StringBuilder analysisResults = new StringBuilder();

            for (int i = 0; i < images.size(); i++) {
                try {
                    UploadedFileData fileData = images.get(i);

                    // Create multipart request for image analysis
                    MultiValueMap<String, Object> analysisRequest = new LinkedMultiValueMap<>();
                    ByteArrayResource imageResource = new ByteArrayResource(fileData.getData()) {
                        @Override
                        public String getFilename() {
                            return fileData.getFilename();
                        }
                    };
                    analysisRequest.add("image", imageResource);

                    // Call analysis API
                    String analysisResponse = ApiClient.postMultipart(
                            "http://localhost:8080/api/analysis/image",
                            analysisRequest);

                    // Parse the analysis response
                    JsonNode analysisJson = objectMapper.readTree(analysisResponse);

                    if (analysisJson.path("success").asBoolean(false)) {
                        String condition = analysisJson.path("condition").asText("Unknown");
                        String description = analysisJson.path("description").asText("No description available");
                        String confidence = analysisJson.path("confidence").asText("Unknown");
                        String urgencyLevel = analysisJson.path("urgencyLevel").asText("Unknown");

                        // Format the analysis result
                        String imagePrefix = images.size() > 1
                                ? "Image " + (i + 1) + " (" + fileData.getFilename() + ")"
                                : "The uploaded image";
                        analysisResults.append(String.format(
                                "%s shows: %s (Confidence: %s, Urgency: %s) - %s",
                                imagePrefix, condition, confidence, urgencyLevel, description));

                        if (i < images.size() - 1) {
                            analysisResults.append("\n\n");
                        }
                    } else {
                        String error = analysisJson.path("message").asText("Analysis failed");
                        analysisResults.append(String.format("Image analysis failed for %s: %s",
                                fileData.getFilename(), error));
                    }

                } catch (Exception e) {
                    System.err
                            .println("Image analysis error for " + images.get(i).getFilename() + ": " + e.getMessage());
                    analysisResults.append(String.format("Could not analyze image %s: %s",
                            images.get(i).getFilename(), e.getMessage()));
                }
            }

            return analysisResults.toString();
        });
    }

    private List<String> splitIntoMessages(String text) {
        List<String> messages = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            messages.add("No response received.");
            return messages;
        }

        // Clean up whitespace first
        String cleanedText = text
                .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n") // Max 2 consecutive line breaks
                .replaceAll("(?m)^\\s+", "") // Remove leading spaces on each line
                .replaceAll("(?m)\\s+$", "") // Remove trailing spaces on each line
                .trim();

        // Check if the response contains our split marker
        if (cleanedText.contains("|||SPLIT|||")) {
            // Split on the marker and clean each part
            String[] parts = cleanedText.split("\\|\\|\\|SPLIT\\|\\|\\|");

            for (String part : parts) {
                String cleanedPart = part
                        .trim()
                        .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n") // Clean up extra line breaks
                        .replaceAll("(?m)^\\s+", "") // Remove leading spaces
                        .replaceAll("(?m)\\s+$", ""); // Remove trailing spaces

                if (!cleanedPart.isEmpty()) {
                    messages.add(cleanedPart);
                }
            }
        } else {
            // For shorter responses, just return as single message
            if (cleanedText.length() <= 400) {
                messages.add(cleanedText);
                return messages;
            }

            // Check if text contains organized content (lists, numbered items)
            boolean hasNumberedList = cleanedText.matches("(?s).*\\d+\\.\\s+.*");
            boolean hasBulletList = cleanedText.matches("(?s).*[•\\-\\*]\\s+.*");
            boolean hasOrganizedContent = hasNumberedList || hasBulletList;

            if (hasOrganizedContent) {
                // Split at logical breakpoints for organized content
                List<String> parts = new ArrayList<>();
                String[] paragraphs = cleanedText.split("\\n\\s*\\n");
                StringBuilder currentPart = new StringBuilder();

                for (String paragraph : paragraphs) {
                    paragraph = paragraph.trim();
                    if (paragraph.isEmpty())
                        continue;

                    boolean isBreakPoint = false;

                    if (paragraph.toLowerCase().startsWith("for now") ||
                            paragraph.toLowerCase().startsWith("you can try") ||
                            paragraph.toLowerCase().startsWith("here are") ||
                            paragraph.toLowerCase().startsWith("recommendations") ||
                            paragraph.toLowerCase().startsWith("**when to seek")) {
                        isBreakPoint = true;
                    }

                    if (currentPart.length() > 400 &&
                            (paragraph.startsWith("•") || paragraph.matches("^\\d+\\..*") ||
                                    paragraph.startsWith("**"))) {
                        isBreakPoint = true;
                    }

                    if (isBreakPoint && currentPart.length() > 0) {
                        parts.add(currentPart.toString().trim());
                        currentPart = new StringBuilder(paragraph);
                    } else {
                        if (currentPart.length() > 0) {
                            currentPart.append("\n\n");
                        }
                        currentPart.append(paragraph);
                    }
                }

                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString().trim());
                }

                if (parts.size() <= 1) {
                    String[] simpleParts = cleanedText.split("\\n\\s*\\n");
                    for (String part : simpleParts) {
                        part = part.trim();
                        if (!part.isEmpty()) {
                            messages.add(part);
                        }
                    }
                } else {
                    messages.addAll(parts);
                }

            } else {
                int maxLength = 300;
                if (cleanedText.length() <= maxLength) {
                    messages.add(cleanedText);
                } else {
                    String[] sentences = cleanedText.split("(?<=[.!?])\\s+");
                    StringBuilder currentMessage = new StringBuilder();

                    for (String sentence : sentences) {
                        if (currentMessage.length() + sentence.length() > maxLength && currentMessage.length() > 0) {
                            messages.add(currentMessage.toString().trim());
                            currentMessage = new StringBuilder(sentence);
                        } else {
                            if (currentMessage.length() > 0) {
                                currentMessage.append(" ");
                            }
                            currentMessage.append(sentence);
                        }
                    }

                    if (currentMessage.length() > 0) {
                        messages.add(currentMessage.toString().trim());
                    }
                }
            }
        }

        return messages;
    }

    private void addBotMessages(List<String> messages) {
        for (int i = 0; i < messages.size(); i++) {
            final int index = i;
            final String message = messages.get(i);
            final boolean isLastInGroup = (i == messages.size() - 1);

            // Use custom delay if available from structured response
            final int delayFinal;
            if (lastStructuredResponse != null &&
                    index < lastStructuredResponse.getMessages().size()) {
                delayFinal = lastStructuredResponse.getMessages().get(index).getDelay();
            } else {
                delayFinal = 800; // default
            }

            // Calculate total delay for this message
            int totalDelay = (index * delayFinal) + 300;

            // Schedule the message addition using CompletableFuture with proper delay
            CompletableFuture.delayedExecutor(totalDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        UI ui = getUI().orElse(null);
                        if (ui != null) {
                            ui.access(() -> {
                                addBotMessage(message, isLastInGroup);
                                scrollToBottom();
                                ui.push();
                            });
                        }
                    });
        }
    }

    private void addUserMessage(String text, List<UploadedFileData> attachments) {
        // Update message group tracking
        if (!"user".equals(lastMessageSender)) {
            // Starting a new user message group
            hideAvatarsInPreviousGroup();
            currentMessageGroup.clear();
            lastMessageSender = "user";
        }

        Div messageRow = new Div();
        messageRow.setWidthFull();
        messageRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("margin-bottom", "12px")
                .set("padding", "0 4px")
                .set("position", "relative");

        // User avatar - initially hidden, will be shown only on the last message
        Div userAvatar = new Div();
        userAvatar.setText("👤");
        userAvatar.getStyle()
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "50%")
                .set("background", "#e5e7eb")
                .set("display", "none") // Initially hidden
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "12px")
                .set("margin-left", "8px")
                .set("margin-top", "auto")
                .set("flex-shrink", "0");

        // Add class for easier identification
        userAvatar.addClassName("user-avatar");

        // Message content container
        Div messageContent = new Div();
        messageContent.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "flex-end")
                .set("max-width", "80%")
                .set("margin-right", "32px"); // Add right margin for avatar space

        // Image attachments (above message bubble)
        if (!attachments.isEmpty()) {
            Div attachmentContainer = new Div();
            attachmentContainer.getStyle()
                    .set("display", "flex")
                    .set("flex-wrap", "wrap")
                    .set("gap", "4px")
                    .set("margin-bottom", "4px")
                    .set("justify-content", "flex-end");

            for (UploadedFileData attachment : attachments) {
                Div attachmentChip = new Div();
                attachmentChip.getStyle()
                        .set("background", "#dbeafe")
                        .set("color", "#1e40af")
                        .set("padding", "4px 8px")
                        .set("border-radius", "12px")
                        .set("font-size", "12px")
                        .set("display", "flex")
                        .set("align-items", "center")
                        .set("gap", "4px")
                        .set("border", "1px solid #bfdbfe");

                Icon fileIcon = new Icon(VaadinIcon.FILE_PICTURE);
                fileIcon.setSize("12px");
                fileIcon.setColor("#1e40af");

                Span fileName = new Span(attachment.getFilename());
                fileName.getStyle().set("font-size", "11px");

                attachmentChip.add(fileIcon, fileName);
                attachmentContainer.add(attachmentChip);
            }
            messageContent.add(attachmentContainer);
        }

        // Message bubble
        Div messageBubble = new Div();
        messageBubble.getElement().setProperty("innerHTML", formatText(text));
        messageBubble.getStyle()
                .set("background", "#2563eb")
                .set("color", "white")
                .set("padding", "12px 16px")
                .set("border-radius", "18px 18px 6px 18px")
                .set("min-width", "20px")
                .set("font-size", "16px")
                .set("font-family", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif")
                .set("line-height", "1.4")
                .set("word-wrap", "break-word")
                .set("white-space", "pre-wrap")
                .set("position", "relative");

        // Edit button (3 dots)
        Button editButton = new Button();
        Icon dotsIcon = new Icon(VaadinIcon.ELLIPSIS_DOTS_H);
        dotsIcon.setSize("14px");
        dotsIcon.setColor("#9ca3af");
        editButton.setIcon(dotsIcon);
        editButton.getStyle()
                .set("position", "absolute")
                .set("top", "-8px")
                .set("right", "-8px")
                .set("width", "24px")
                .set("height", "24px")
                .set("min-width", "24px")
                .set("background", "white")
                .set("border", "1px solid #e5e7eb")
                .set("border-radius", "50%")
                .set("display", "none")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("z-index", "10");

        editButton.addClickListener(e -> {
            showNotification("Edit functionality coming soon!", false);
        });

        messageBubble.add(editButton);

        // Show edit button on hover
        messageBubble.getElement().addEventListener("mouseenter", e -> {
            editButton.getStyle().set("display", "flex");
        });

        messageBubble.getElement().addEventListener("mouseleave", e -> {
            editButton.getStyle().set("display", "none");
        });

        messageContent.add(messageBubble);
        messageRow.add(messageContent, userAvatar);
        messagesContainer.add(messageRow);
        messageElements.add(messageRow);
        currentMessageGroup.add(messageRow);

        // Show avatar on this message (it's the current last user message)
        showUserAvatarOnLastMessage();
    }

    // Helper method to show user avatar only on the last message
    private void showUserAvatarOnLastMessage() {
        // Hide all user avatars in current group
        for (Div messageRow : currentMessageGroup) {
            messageRow.getChildren()
                    .filter(component -> component instanceof Div)
                    .forEach(component -> {
                        Div div = (Div) component;
                        if ("👤".equals(div.getText())) {
                            div.getStyle().set("display", "none");
                        }
                    });
        }

        // Show avatar only on the last message in the group
        if (!currentMessageGroup.isEmpty()) {
            Div lastMessageRow = currentMessageGroup.get(currentMessageGroup.size() - 1);
            lastMessageRow.getChildren()
                    .filter(component -> component instanceof Div)
                    .forEach(component -> {
                        Div div = (Div) component;
                        if ("👤".equals(div.getText())) {
                            div.getStyle().set("display", "flex");
                            // Adjust message content margin
                            lastMessageRow.getChildren()
                                    .filter(comp -> comp instanceof Div &&
                                            comp != div &&
                                            ((Div) comp).getStyle().get("flex-direction") != null)
                                    .findFirst()
                                    .ifPresent(content -> ((Div) content).getStyle().set("margin-right", "0"));
                        }
                    });
        }
    }

    // Helper method to hide avatars in previous message group
    private void hideAvatarsInPreviousGroup() {
        // This method ensures that when switching between bot/user messages,
        // the previous group's avatars are properly managed
        if ("user".equals(lastMessageSender)) {
            // We're switching from user to bot, make sure last user message shows avatar
            showUserAvatarOnLastMessage();
        }
        // For bot messages, the avatar visibility is already handled in addBotMessage
    }

    private void addBotMessage(String text, boolean showAvatar) {
        // Update message group tracking
        if (!"bot".equals(lastMessageSender)) {
            // Starting a new bot message group
            hideAvatarsInPreviousGroup();
            currentMessageGroup.clear();
            lastMessageSender = "bot";
        }

        Div messageRow = new Div();
        messageRow.setWidthFull();
        messageRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-start")
                .set("margin-bottom", "12px")
                .set("padding", "0 4px")
                .set("position", "relative");

        // Bot avatar - only show if this is the last message in the group
        Div botAvatar = new Div();
        botAvatar.setText("🤖");
        botAvatar.getStyle()
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "50%")
                .set("background", "#f3f4f6")
                .set("display", showAvatar ? "flex" : "none") // Hide/show based on parameter
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "12px")
                .set("margin-right", "8px")
                .set("margin-top", "auto")
                .set("flex-shrink", "0");

        // Add class for easier identification
        botAvatar.addClassName("bot-avatar");

        // Message bubble - adjust margin when no avatar
        Div messageBubble = new Div();
        messageBubble.getElement().setProperty("innerHTML", formatText(text));
        messageBubble.getStyle()
                .set("background", "#f3f4f6")
                .set("color", "#1f2937")
                .set("padding", "12px 16px")
                .set("border-radius", "18px 18px 18px 6px")
                .set("max-width", "80%")
                .set("min-width", "20px")
                .set("font-size", "16px")
                .set("font-family", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif")
                .set("line-height", "1.4")
                .set("word-wrap", "break-word")
                .set("white-space", "pre-wrap")
                .set("position", "relative")
                .set("margin-left", showAvatar ? "0" : "32px"); // Add left margin when no avatar

        // Edit button (3 dots) for bot messages
        Button editButton = new Button();
        Icon dotsIcon = new Icon(VaadinIcon.ELLIPSIS_DOTS_H);
        dotsIcon.setSize("14px");
        dotsIcon.setColor("#9ca3af");
        editButton.setIcon(dotsIcon);
        editButton.getStyle()
                .set("position", "absolute")
                .set("top", "-8px")
                .set("left", "-8px")
                .set("width", "24px")
                .set("height", "24px")
                .set("min-width", "24px")
                .set("background", "white")
                .set("border", "1px solid #e5e7eb")
                .set("border-radius", "50%")
                .set("display", "none")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("z-index", "10");

        editButton.addClickListener(e -> {
            showNotification("Bot message options coming soon!", false);
        });

        messageBubble.add(editButton);

        // Show edit button on hover
        messageBubble.getElement().addEventListener("mouseenter", e -> {
            editButton.getStyle().set("display", "flex");
        });

        messageBubble.getElement().addEventListener("mouseleave", e -> {
            editButton.getStyle().set("display", "none");
        });

        messageRow.add(botAvatar, messageBubble);
        messagesContainer.add(messageRow);
        messageElements.add(messageRow);
        currentMessageGroup.add(messageRow);
    }

    private String formatText(String text) {
        if (text == null || text.isEmpty())
            return "";

        // Normalize quotes and apostrophes
        text = text.replace("'", "&apos;").replace("\"", "&quot;").replace("\"", "&quot;");
        // Convert **bold** to <strong>
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // Convert *italic* to <em>
        text = text.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");

        // Handle <bullets> and <list> blocks
        text = formatBullets(text);

        // Handle stray bullets or ? at start of line
        text = text.replaceAll("(?m)^\\?\\s*[•\\-]\\s*(.*)$",
                "<div style='margin:0; padding:0; line-height:1.2;'>• $1</div>");

        text = text.replaceAll("(?m)^[•\\-]\\s*(.*)$",
                "<div style='margin:0; padding:0; line-height:1.2;'>• $1</div>");

        // Convert numbered lists to HTML <ol>
        text = convertNumberedListToHtml(text);

        // Convert remaining newlines to <br>
        text = text.replaceAll("(?<!>)\\n(?!<)", "<br>");

        // Remove excessive line breaks
        text = text.replaceAll("(<br>\\s*){3,}", "<br><br>");

        // Remove leading ? if left anywhere
        text = text.replaceAll("(?m)^\\?\\s+", "");

        return text.trim();
    }

    private String formatBullets(String text) {
        // Handle <bullets>...</bullets>
        Pattern bulletsPattern = Pattern.compile("(?s)<bullets>\\s*(.*?)\\s*</bullets>");
        Matcher bulletsMatcher = bulletsPattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (bulletsMatcher.find()) {
            String content = bulletsMatcher.group(1).trim();
            String[] lines = content.split("\\n");
            StringBuilder replacement = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    line = line.replaceFirst("^\\?\\s*", ""); // Remove leading ?
                    replacement.append("<div style='margin:0; padding:0; line-height:1.2;'>• ")
                            .append(line)
                            .append("</div>");
                }
            }
            bulletsMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        bulletsMatcher.appendTail(sb);
        text = sb.toString();

        // Handle <list>...</list> similarly
        Pattern listPattern = Pattern.compile("(?s)<list>\\s*(.*?)\\s*</list>");
        Matcher listMatcher = listPattern.matcher(text);
        sb = new StringBuffer();
        while (listMatcher.find()) {
            String content = listMatcher.group(1).trim();
            String[] lines = content.split("\\n");
            StringBuilder replacement = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    line = line.replaceFirst("^\\d+\\.\\s*", ""); // Remove numbering
                    line = line.replaceFirst("^\\?\\s*", ""); // Remove leading ?
                    replacement.append("<div style='margin:0; padding:0; line-height:1.2;'>• ")
                            .append(line)
                            .append("</div>");
                }
            }
            listMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        listMatcher.appendTail(sb);
        return sb.toString();
    }

    private String convertNumberedListToHtml(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inList = false;
        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.matches("^\\d+\\.\\s+.*")) {
                if (!inList) {
                    result.append("<ol style='padding-left: 20px; margin: 4px 0; line-height: 1.4;'>");
                    inList = true;
                }
                String item = trimmedLine.replaceFirst("^\\d+\\.\\s+", "");
                result.append("<li style='margin: 0; padding: 0;'>").append(item.trim()).append("</li>");
            } else if (!trimmedLine.isEmpty()) {
                if (inList) {
                    result.append("</ol>");
                    inList = false;
                }
                result.append(line).append("\n");
            } else if (inList) {
                continue;
            } else {
                if (result.length() > 0 && !result.toString().endsWith("\n\n")) {
                    result.append("\n");
                }
            }
        }

        if (inList) {
            result.append("</ol>");
        }

        return result.toString().trim();
    }

    private void showTypingIndicator() {
        Div typingRow = new Div();
        typingRow.setWidthFull();
        typingRow.setId("typing-indicator");
        typingRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-start")
                .set("margin-bottom", "12px")
                .set("padding", "0 4px");

        // Bot avatar for typing indicator
        Div botAvatar = new Div();
        botAvatar.setText("🤖");
        botAvatar.getStyle()
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "50%")
                .set("background", "#f3f4f6")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "12px")
                .set("margin-right", "8px")
                .set("margin-top", "auto")
                .set("flex-shrink", "0");

        Div typingBubble = new Div();
        typingBubble.getStyle()
                .set("background", "#f3f4f6")
                .set("padding", "16px 20px")
                .set("border-radius", "18px 18px 18px 6px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "4px");

        for (int i = 0; i < 3; i++) {
            Div dot = new Div();
            dot.getStyle()
                    .set("width", "8px")
                    .set("height", "8px")
                    .set("background", "#9ca3af")
                    .set("border-radius", "50%")
                    .set("animation", "typingDot 1.4s infinite ease-in-out")
                    .set("animation-delay", (i * 0.2) + "s");
            typingBubble.add(dot);
        }

        typingRow.add(botAvatar, typingBubble);
        messagesContainer.add(typingRow);
        scrollToBottom();
    }

    private void removeTypingIndicator() {
        messagesContainer.getChildren()
                .filter(component -> component.getId().isPresent() &&
                        component.getId().get().equals("typing-indicator"))
                .findFirst()
                .ifPresent(messagesContainer::remove);
    }

    private void setInputEnabled(boolean enabled) {
        messageInput.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        uploadButton.setEnabled(enabled);
        isWaitingForResponse = !enabled;

        if (enabled) {
            sendButton.getStyle()
                    .set("opacity", "1")
                    .set("background", "#2563eb");
            uploadButton.getStyle()
                    .set("opacity", "1")
                    .set("background", "#2563eb");
            messageInput.getStyle().set("opacity", "1");
        } else {
            sendButton.getStyle()
                    .set("opacity", "0.6")
                    .set("background", "#9ca3af");
            uploadButton.getStyle()
                    .set("opacity", "0.6")
                    .set("background", "#9ca3af");
            messageInput.getStyle().set("opacity", "0.8");
        }
    }

    private void checkForEmergency(String responseText) {
        if (responseText != null && (responseText.toLowerCase().contains("emergency") ||
                responseText.toLowerCase().contains("urgent") ||
                responseText.toLowerCase().contains("immediately"))) {
            showEmergencyBanner();
        }
    }

    private void showEmergencyBanner() {
        emergencyBanner.removeAll();
        emergencyBanner.setVisible(true);

        Div bannerContent = new Div();
        bannerContent.setText("🚨 Emergency Situation Detected");
        bannerContent.getStyle()
                .set("color", "white")
                .set("font-weight", "600")
                .set("font-size", "16px");

        Div subText = new Div();
        subText.setText("Contact your veterinarian immediately!");
        subText.getStyle()
                .set("color", "rgba(255, 255, 255, 0.9)")
                .set("font-size", "14px")
                .set("margin-top", "4px");

        Button findVetsBtn = new Button("Find Emergency Vets");
        findVetsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        findVetsBtn.getStyle()
                .set("background", "white")
                .set("color", "#dc2626")
                .set("border", "none")
                .set("font-weight", "600")
                .set("border-radius", "8px")
                .set("padding", "8px 16px");

        findVetsBtn.addClickListener(e -> {
            findNearbyEmergencyVets();
        });

        VerticalLayout bannerText = new VerticalLayout(bannerContent, subText);
        bannerText.setPadding(false);
        bannerText.setSpacing(false);

        HorizontalLayout banner = new HorizontalLayout(bannerText, findVetsBtn);
        banner.setWidthFull();
        banner.setAlignItems(FlexComponent.Alignment.CENTER);
        banner.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        banner.setPadding(true);

        emergencyBanner.add(banner);
        emergencyBanner.getStyle()
                .set("background", "#dc2626")
                .set("color", "white")
                .set("flex-shrink", "0");
    }

    public void findNearbyEmergencyVets() {
        System.out.println("Finding nearby vets with coordinates: " + userLatitude + ", " + userLongitude);

        if (userLatitude == 0.0 && userLongitude == 0.0) {
            showNotification("Location not available. Please enable location access and refresh the page.", true);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Making API call to find nearby vets...");
                String response = ApiClient.postJson(
                        "http://localhost:8080/api/emergency/nearby-vets",
                        Map.of(
                                "latitude", userLatitude,
                                "longitude", userLongitude,
                                "radiusKm", 25));
                System.out.println("API Response: " + response);
                return response;
            } catch (Exception ex) {
                System.err.println("Error finding nearby vets: " + ex.getMessage());
                ex.printStackTrace(); // Add full stack trace
                return null;
            }
        }).thenAccept(response -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                if (response != null) {
                    System.out.println("Showing dialog with response: " + response);
                    showNearbyVetsDialog(response);
                } else {
                    System.out.println("No response received, showing error notification");
                    showNotification(
                            "Unable to find nearby emergency vets. Please search online or call your regular vet.",
                            true);
                }
                ui.push();
            }));
        });
    }

    private void showNearbyVetsDialog(String response) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode nearbyVets = jsonResponse.path("nearbyVets");

            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Nearby Emergency Veterinary Clinics");
            dialog.setWidth("600px");
            dialog.setHeight("500px");

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);

            if (nearbyVets.isArray() && nearbyVets.size() > 0) {
                for (JsonNode vetNode : nearbyVets) {
                    Div vetCard = createVetCard(vetNode);
                    content.add(vetCard);
                }
            } else {
                content.add(new Span(
                        "No emergency veterinary clinics found nearby. Please search online or contact your regular veterinarian."));
            }

            Button closeButton = new Button("Close", e -> dialog.close());
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            dialog.getFooter().add(closeButton);
            dialog.add(content);
            dialog.open();

        } catch (Exception e) {
            showNotification("Error displaying vet information", true);
        }
    }

    @ClientCallable
    public void triggerEmergency() {
        findNearbyEmergencyVets();
    }

    private Div createVetCard(JsonNode vetNode) {
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid #e5e7eb")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("margin-bottom", "12px")
                .set("background", "white");

        H4 name = new H4(vetNode.path("name").asText("Veterinary Clinic"));
        name.getStyle().set("margin", "0 0 8px 0").set("color", "#1f2937");

        Span address = new Span(vetNode.path("address").asText("Address not available"));
        address.getStyle().set("color", "#6b7280").set("font-size", "14px");

        Span phone = new Span("📞 " + vetNode.path("phoneNumber").asText("Contact for phone"));
        phone.getStyle().set("color", "#374151").set("font-size", "14px");

        double distance = vetNode.path("distanceKm").asDouble(0);
        Span distanceSpan = new Span(String.format("📍 %.1f km away", distance));
        distanceSpan.getStyle().set("color", "#059669").set("font-weight", "600").set("font-size", "14px");

        HorizontalLayout info = new HorizontalLayout(phone, distanceSpan);
        info.setSpacing(true);
        info.setAlignItems(FlexComponent.Alignment.CENTER);

        card.add(name, address, info);

        return card;
    }

    private void showNotification(String message, boolean isError) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    private void scrollToBottom() {
        getUI().ifPresent(ui -> ui.access(() -> {
            // Only scroll if user is near the bottom
            messagesScroller.getElement().executeJs(
                    "const threshold = 100; " + // pixels from bottom
                            "if (this.scrollHeight - this.scrollTop - this.clientHeight <= threshold) { " +
                            "  this.scrollTop = this.scrollHeight; " +
                            "}");
        }));
    }

    private void startNewSession() {
        CompletableFuture.supplyAsync(() -> {
            try {
                String response = ApiClient.postJson(
                        "http://localhost:8080/api/chat/start",
                        Map.of());
                JsonNode jsonResponse = objectMapper.readTree(response);
                return jsonResponse.path("sessionId").asText();
            } catch (Exception e) {
                System.err.println("Session start error: " + e.getMessage());
                return null;
            }
        }).thenAccept(sessionId -> {
            UI ui = getUI().orElse(null);
            if (ui != null) {
                ui.access(() -> {
                    if (sessionId != null) {
                        this.currentSessionId = sessionId;
                        System.out.println("Session started successfully: " + sessionId);
                    } else {
                        showNotification("Failed to start session. Please refresh the page.", true);
                    }
                    ui.push();
                });
            }
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> {
            ui.access(() -> {
                messageInput.focus();

                requestUserLocation();

                // Add welcome message after a slight delay to ensure layout is ready
                ui.getPage().executeJs("setTimeout(() => {}, 50);").then(ignore -> {
                    addWelcomeMessage();
                    ui.push();
                });
            });

            // Split the JavaScript into smaller, manageable parts
            ui.getPage().executeJs(
                    "const style = document.createElement('style');" +
                            "document.head.appendChild(style);");

            // Add CSS styles in separate execution
            ui.getPage().executeJs(
                    "const styleSheet = document.styleSheets[document.styleSheets.length - 1];" +
                            "styleSheet.insertRule('vaadin-vertical-layout[width=\"100%\"] { display: flex !important; flex-direction: column !important; height: 100vh !important; overflow: hidden !important; min-width: 0 !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-scroller { flex: 1 1 auto !important; min-height: 0 !important; height: 100% !important; overflow-y: auto !important; overflow-x: hidden !important; min-width: 0 !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-scroller::-webkit-scrollbar { width: 0 !important; background: transparent !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-scroller { -ms-overflow-style: none !important; scrollbar-width: none !important; }');");

            ui.getPage().executeJs(
                    "const styleSheet = document.styleSheets[document.styleSheets.length - 1];" +
                            "styleSheet.insertRule('vaadin-text-area::part(input-field) { background: transparent !important; border: none !important; box-shadow: none !important; overflow: hidden !important; min-width: 0 !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-text-area[focus-ring]::part(input-field) { box-shadow: none !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-text-area textarea { overflow-y: auto !important; overflow-x: hidden !important; resize: none !important; scrollbar-width: thin !important; min-width: 0 !important; word-wrap: break-word !important; white-space: pre-wrap !important; }');");

            ui.getPage().executeJs(
                    "const styleSheet = document.styleSheets[document.styleSheets.length - 1];" +
                            "styleSheet.insertRule('vaadin-text-area textarea::-webkit-scrollbar { width: 4px !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-text-area textarea::-webkit-scrollbar-track { background: transparent !important; }');"
                            +
                            "styleSheet.insertRule('vaadin-text-area textarea::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.2) !important; border-radius: 4px !important; }');"
                            +
                            "styleSheet.insertRule('div[style*=\"word-wrap: break-word\"] { overflow-wrap: break-word !important; word-break: break-word !important; hyphens: auto !important; max-width: 100% !important; }');");

            // Add keyframe animation
            ui.getPage().executeJs(
                    "const styleSheet = document.styleSheets[document.styleSheets.length - 1];" +
                            "styleSheet.insertRule('@keyframes typingDot { 0%, 60%, 100% { opacity: 0.3; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-10px); } }');");

            // Add textarea functionality
            ui.getPage().executeJs(
                    "setTimeout(() => {" +
                            "  const textarea = document.querySelector('vaadin-text-area textarea');" +
                            "  if (textarea) {" +
                            "    const minHeight = 44;" +
                            "    const maxHeight = 120;" +
                            "    " +
                            "    function resizeTextarea() {" +
                            "      textarea.style.height = minHeight + 'px';" +
                            "      const scrollHeight = textarea.scrollHeight;" +
                            "      " +
                            "      if (scrollHeight <= maxHeight) {" +
                            "        textarea.style.height = scrollHeight + 'px';" +
                            "        textarea.style.overflowY = 'hidden';" +
                            "      } else {" +
                            "        textarea.style.height = maxHeight + 'px';" +
                            "        textarea.style.overflowY = 'auto';" +
                            "      }" +
                            "    }" +
                            "    " +
                            "    textarea.addEventListener('input', resizeTextarea);" +
                            "    textarea.addEventListener('focus', resizeTextarea);" +
                            "    textarea.addEventListener('blur', resizeTextarea);" +
                            "    " +
                            "    textarea.addEventListener('keydown', function(e) {" +
                            "      if (e.key === 'Enter' && !e.shiftKey) {" +
                            "        e.preventDefault();" +
                            "        const event = new KeyboardEvent('keydown', {" +
                            "          key: 'Enter'," +
                            "          keyCode: 13," +
                            "          which: 13," +
                            "          shiftKey: false," +
                            "          bubbles: true" +
                            "        });" +
                            "        textarea.dispatchEvent(event);" +
                            "        return false;" +
                            "      }" +
                            "    });" +
                            "    " +
                            "    resizeTextarea();" +
                            "  }" +
                            "}, 100);");
        });
    }

    private void showLocationPermissionDialog() {
        Dialog locationDialog = new Dialog();
        locationDialog.setHeaderTitle("Location Access Required");
        locationDialog.setModal(true);
        locationDialog.setCloseOnEsc(false);
        locationDialog.setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        Icon locationIcon = VaadinIcon.LOCATION_ARROW.create();
        locationIcon.setSize("40px");
        locationIcon.setColor("#2563eb");

        H3 title = new H3("Enable Location Access");
        Paragraph description = new Paragraph(
                "Virtual Vet needs your location to find nearby emergency veterinary clinics " +
                        "in case of urgent situations. Your location data is only used for this purpose " +
                        "and is not stored on our servers.");
        description.getStyle().set("max-width", "400px");

        Button enableButton = new Button("Enable Location", VaadinIcon.CHECK.create());
        enableButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        enableButton.addClickListener(e -> {
            locationDialog.close();
            requestUserLocation();
        });

        Button skipButton = new Button("Skip for Now", VaadinIcon.CLOCK.create());
        skipButton.addClickListener(e -> {
            locationDialog.close();
            // Set default coordinates if user skips
            this.userLatitude = 45.4215;
            this.userLongitude = -75.6972;
            showNotification("Using default location. You can enable location access later.", false);
        });

        HorizontalLayout buttons = new HorizontalLayout(enableButton, skipButton);
        buttons.setSpacing(true);

        content.add(locationIcon, title, description, buttons);
        content.setAlignItems(Alignment.CENTER);
        locationDialog.add(content);

        locationDialog.open();
    }

    private void requestUserLocation() {
        System.out.println("Requesting user location...");
        getUI().ifPresent(ui -> {
            ui.getPage().executeJs(
                    """
                            if (navigator.geolocation) {
                                navigator.geolocation.getCurrentPosition(
                                    function(position) {
                                        $0.$server.onLocationReceived(position.coords.latitude, position.coords.longitude);
                                    },
                                    function(error) {
                                        console.log('Location error:', error);
                                        let errorMsg = "Unable to access your location";
                                        if (error.code === error.PERMISSION_DENIED) {
                                            errorMsg = "Location access was denied. Please enable location permissions in your browser settings.";
                                        } else if (error.code === error.POSITION_UNAVAILABLE) {
                                            errorMsg = "Location information is unavailable.";
                                        } else if (error.code === error.TIMEOUT) {
                                            errorMsg = "Location request timed out.";
                                        }
                                        $0.$server.onLocationError(errorMsg);
                                    },
                                    {
                                        enableHighAccuracy: true,
                                        timeout: 15000,
                                        maximumAge: 300000
                                    }
                                );
                            } else {
                                $0.$server.onLocationError("Geolocation is not supported by this browser");
                            }
                            """,
                    getElement());
        });
    }

    @ClientCallable
    public void onLocationReceived(double latitude, double longitude) {
        // Store user location for emergency vet searches
        this.userLatitude = latitude;
        this.userLongitude = longitude;

        showNotification("Location detected for emergency vet search", false);
        System.out.println("User location: " + latitude + ", " + longitude);
    }

    @ClientCallable
    public void onLocationError(String error) {
        System.err.println("Location error: " + error);

        // Set default coordinates (you can customize this for your area)
        this.userLatitude = 45.4215; // Ottawa coordinates as default
        this.userLongitude = -75.6972;
    }

    private static class UploadedFileData {
        private final String filename;
        private final String contentType;
        private final byte[] data;

        public UploadedFileData(String filename, String contentType, byte[] data) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            return data;
        }

        public String getBase64Data() {
            return java.util.Base64.getEncoder().encodeToString(data);
        }
    }

    // Add these fields to ChatView class
    private List<ConversationMessage> conversationHistory = new ArrayList<>();

    // Add this inner class
    private static class ConversationMessage {
        public String role; // "user" or "bot"
        public String content;
        public long timestamp;

        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "ConversationMessage{" +
                    "role='" + role + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}