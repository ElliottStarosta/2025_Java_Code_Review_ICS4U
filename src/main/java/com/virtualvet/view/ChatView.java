package com.virtualvet.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.virtualvet.dto.StructuredVetResponse;
import com.virtualvet.model.ConversationContext;
import com.virtualvet.service.ChatService;
import com.virtualvet.util.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Chat | Novavet")
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
    private AtomicLong lastEnterTime = new AtomicLong(0);
    private long debounceMillis = 300; // adjust to taste (300ms = 0.3 sec)
    private String temporaryMessage = null;

    private List<Div> currentMessageGroup = new ArrayList<>(); // Track current message group

    private StructuredVetResponse lastStructuredResponse;

    private Div imagePreviewContainer;

    @Autowired
    private ChatService chatService;

    private double userLatitude = 0.0;
    private double userLongitude = 0.0;

    private String currentSessionId;
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
        Shortcuts.addShortcutListener(
                messageInput,
                event -> {
                    long now = System.currentTimeMillis();
                    long last = lastEnterTime.get();

                    if (now - last > debounceMillis) {
                        lastEnterTime.set(now);

                        // Use JavaScript to handle the Enter key properly
                        messageInput.getElement().executeJs("""
                                    // Prevent default behavior and stop propagation
                                    if (event) {
                                        event.preventDefault();
                                        event.stopPropagation();
                                        event.stopImmediatePropagation();
                                    }

                                    // Get the textarea element
                                    const textarea = this.querySelector('textarea');
                                    if (!textarea) return;

                                    // Get current cursor position
                                    const cursorPos = textarea.selectionStart;
                                    const textBefore = textarea.value.substring(0, cursorPos);
                                    const textAfter = textarea.value.substring(cursorPos);

                                    // Check if Shift is pressed - allow new line
                                    if (event.shiftKey) {
                                        // Insert new line at cursor position
                                        textarea.value = textBefore + '\\n' + textAfter;
                                        // Move cursor to after the new line
                                        textarea.selectionStart = cursorPos + 1;
                                        textarea.selectionEnd = cursorPos + 1;
                                        // Trigger input event to update Vaadin
                                        textarea.dispatchEvent(new Event('input', { bubbles: true }));
                                    } else {
                                        // Regular Enter - send message
                                        const message = textarea.value.trim();
                                        if (message) {
                                            // Clear the textarea
                                            textarea.value = '';
                                            textarea.dispatchEvent(new Event('input', { bubbles: true }));
                                            // Send to server
                                            $0.$server.sendMessageFromJS(message);
                                        }
                                    }
                                """, getElement());
                    }
                },
                Key.ENTER).listenOn(messageInput);

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
            Notification.show("File rejected: " + event.getErrorMessage(), 3000, Notification.Position.BOTTOM_START);
        });

        upload.addFailedListener(event -> {
            Notification.show("Upload failed: " + event.getReason().getMessage(), 3000,
                    Notification.Position.BOTTOM_START);
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

    @ClientCallable
    private void sendMessageFromJS(String message) {
        this.temporaryMessage = message;
        sendMessage();
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
        messagesArea.add(messagesScroller);
        messagesArea.setFlexGrow(1, messagesScroller);

        // Add messages area to main layout
        add(messagesArea);
        setFlexGrow(1, messagesArea);

        // Create input area separately - this will be sticky at bottom
        createInputArea();
    }

    private void addWelcomeMessage() {
        addBotMessages(List.of(
                "👋 Hello! I'm your Virtual Vet Assistant, Novavet.",
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
        String message;

        if (temporaryMessage != null) {
            message = temporaryMessage;
            temporaryMessage = null;
        } else {
            message = messageInput.getValue().trim();
            messageInput.clear();
        }

        if (message.isEmpty() || isWaitingForResponse) {
            return;
        }

        conversationHistory.add(new ConversationMessage("user", message));

        if (currentSessionId == null) {
            return;
        }

        // Add user message with attachments
        addUserMessage(message, new ArrayList<>(uploadedFiles));
        messageInput.clear();
        clearImagePreviews();
        scrollToBottom();

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
                .set("position", "relative")
                .set("opacity", "0")
                .set("transform", "translateY(10px)")
                .set("transition", "opacity 0.3s ease, transform 0.3s ease");

        // Message content container
        Div messageContent = new Div();
        messageContent.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "flex-end")
                .set("max-width", "80%")
                .set("margin-right", "10px");

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

        // Create action buttons container (positioned below the message)
        Div actionButtons = new Div();
        actionButtons.getStyle()
                .set("display", "none") // Start hidden
                .set("gap", "8px")
                .set("margin-top", "4px")
                .set("justify-content", "flex-end")
                .set("height", "0") // No invisible space
                .set("overflow", "hidden")
                .set("opacity", "0") // Start fully transparent
                .set("transform", "translateY(-10px)") // Start slightly above
                .set("transition", "all 0.3s ease"); // Smooth transition for all properties

        // Edit button
        Button editButton = new Button("Edit");
        editButton.getStyle()
                .set("font-size", "12px")
                .set("padding", "4px 8px")
                .set("background", "transparent")
                .set("border", "1px solid #d1d5db")
                .set("border-radius", "4px")
                .set("color", "#6b7280")
                .set("cursor", "pointer")
                .set("transition", "all 0.2s ease");

        // Delete button
        Button deleteButton = new Button("Delete");
        deleteButton.getStyle()
                .set("font-size", "12px")
                .set("padding", "4px 8px")
                .set("background", "transparent")
                .set("border", "1px solid #fecaca")
                .set("border-radius", "4px")
                .set("color", "#dc2626")
                .set("cursor", "pointer")
                .set("transition", "all 0.2s ease");

        actionButtons.add(editButton, deleteButton);

        // Show action buttons on hover with smooth animation
        messageContent.getElement().addEventListener("mouseenter", e -> {
            actionButtons.getStyle()
                    .set("display", "flex")
                    .set("height", "auto")
                    .set("opacity", "1")
                    .set("transform", "translateY(0)");
        });

        messageContent.getElement().addEventListener("mouseleave", e -> {
            actionButtons.getStyle()
                    .set("display", "none")
                    .set("height", "0")
                    .set("opacity", "0")
                    .set("transform", "translateY(-10px)");
        });

        // Add hover effects for individual buttons
        editButton.getElement().addEventListener("mouseenter", e -> {
            editButton.getStyle()
                    .set("background-color", "#f3f4f6")
                    .set("border-color", "#9ca3af");
        });

        editButton.getElement().addEventListener("mouseleave", e -> {
            editButton.getStyle()
                    .set("background-color", "transparent")
                    .set("border-color", "#d1d5db");
        });

        deleteButton.getElement().addEventListener("mouseenter", e -> {
            deleteButton.getStyle()
                    .set("background-color", "#fef2f2")
                    .set("border-color", "#f87171");
        });

        deleteButton.getElement().addEventListener("mouseleave", e -> {
            deleteButton.getStyle()
                    .set("background-color", "transparent")
                    .set("border-color", "#fecaca");
        });

        // Edit functionality
        editButton.addClickListener(e -> {
            // Create edit dialog
            Dialog editDialog = new Dialog();
            editDialog.setHeaderTitle("Edit Message");
            editDialog.setWidth("400px");

            TextArea editField = new TextArea();
            editField.setValue(text);
            editField.setWidthFull();
            editField.setMinHeight("120px");

            Button saveButton = new Button("Save & Update", event -> {
                String newText = editField.getValue().trim();
                if (!newText.isEmpty() && !newText.equals(text)) {
                    // Update the message content
                    messageBubble.getElement().setProperty("innerHTML", formatText(newText));

                    // Find the position of this message
                    int messageIndex = messagesContainer.getChildren()
                            .collect(Collectors.toList())
                            .indexOf(messageRow);

                    // Remove all AI responses that came after this message
                    List<Component> children = new ArrayList<>(messagesContainer.getChildren()
                            .collect(Collectors.toList()));

                    for (int i = children.size() - 1; i > messageIndex; i--) {
                        Component child = children.get(i);
                        if (child instanceof Div) {
                            Div div = (Div) child;
                            // Check if this is an AI message (has left alignment/style)
                            boolean isBotMessage = div.getStyle().get("justify-content") != null &&
                                    div.getStyle().get("justify-content").equals("flex-start");

                            if (isBotMessage) {
                                messagesContainer.remove(child);
                            }
                        }
                    }

                    // Remove bot messages from conversation history that came after this message
                    int userMessageIndex = -1;
                    for (int i = 0; i < conversationHistory.size(); i++) {
                        ConversationMessage msg = conversationHistory.get(i);
                        if (msg.getRole().equals("user") && msg.getContent().equals(text)) {
                            userMessageIndex = i;
                            break;
                        }
                    }

                    if (userMessageIndex != -1) {
                        // Remove all messages after this user message
                        while (conversationHistory.size() > userMessageIndex + 1) {
                            conversationHistory.remove(conversationHistory.size() - 1);
                        }

                        // Update the user message with correction instruction
                        String correctionMessage = "CORRECTION: I previously said \"" + text +
                                "\" but I meant to say \"" + newText + "\". Please respond to the corrected version.";
                        conversationHistory.set(userMessageIndex, new ConversationMessage("user", correctionMessage));
                    }

                    editDialog.close();
                    Notification.show("Message updated", 2000, Notification.Position.BOTTOM_START);

                    // Resend the corrected message to AI
                    if (currentSessionId != null && !isWaitingForResponse) {
                        // Show typing indicator and disable input
                        showTypingIndicator();
                        setInputEnabled(false);
                        scrollToBottom();

                        // Process the corrected message
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
                                requestBody.add("sessionId", currentSessionId);

                                // Send the correction instruction
                                String correctionInstruction = "CORRECTION: The user previously said \"" + text +
                                        "\" but corrected it to \"" + newText +
                                        "\". Please respond to the corrected version and ignore the previous message.";
                                requestBody.add("message", correctionInstruction);

                                ConversationContext context = chatService.buildConversationContext(currentSessionId);
                                requestBody.add("conversationHistory", context);

                                String response = ApiClient.postMultipart("http://localhost:8080/api/chat/message",
                                        requestBody);
                                JsonNode jsonResponse = objectMapper.readTree(response);
                                return jsonResponse.path("response").asText("Sorry, I couldn't generate a response.");
                            } catch (Exception ex) {
                                return "I'm having trouble connecting right now. Please try again in a moment.";
                            }
                        }).thenAccept(responseText -> {
                            UI ui = getUI().orElse(null);
                            if (ui != null) {
                                ui.access(() -> {
                                    removeTypingIndicator();
                                    List<String> messages = splitIntoMessages(responseText);
                                    addBotMessages(messages);
                                    setInputEnabled(true);
                                    messageInput.focus();
                                    scrollToBottom();
                                    ui.push();
                                });
                            }
                        });
                    }
                } else if (newText.equals(text)) {
                    Notification.show("No changes made", 2000, Notification.Position.BOTTOM_START);
                    editDialog.close();
                }
            });
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button cancelButton = new Button("Cancel", event -> editDialog.close());

            HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
            buttons.setSpacing(true);

            VerticalLayout dialogContent = new VerticalLayout(editField, buttons);
            dialogContent.setSpacing(true);
            dialogContent.setPadding(false);

            editDialog.add(dialogContent);
            editDialog.open();
        });

        // Delete functionality
        // Delete functionality
        deleteButton.addClickListener(e -> {
            // Create confirmation dialog
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Delete Message");
            confirmDialog.setWidth("300px");

            Span confirmText = new Span(
                    "Are you sure you want to delete this message? All messages after this will also be removed.");

            Button confirmButton = new Button("Delete", event -> {
                // Find the position of this message
                int messageIndex = messagesContainer.getChildren()
                        .collect(Collectors.toList())
                        .indexOf(messageRow);

                // Remove all messages from this point onward (including this one)
                List<Component> children = new ArrayList<>(messagesContainer.getChildren()
                        .collect(Collectors.toList()));

                for (int i = children.size() - 1; i >= messageIndex; i--) {
                    Component child = children.get(i);
                    messagesContainer.remove(child);
                }

                // Find the position in conversation history and remove all subsequent messages
                int userMessageIndex = -1;
                for (int i = 0; i < conversationHistory.size(); i++) {
                    ConversationMessage msg = conversationHistory.get(i);
                    if (msg.getRole().equals("user") && msg.getContent().equals(text)) {
                        userMessageIndex = i;
                        break;
                    }
                }

                if (userMessageIndex != -1) {
                    // Remove all messages from this point onward
                    while (conversationHistory.size() > userMessageIndex) {
                        conversationHistory.remove(conversationHistory.size() - 1);
                    }
                }

                confirmDialog.close();
                Notification.show("Message and all subsequent messages deleted", 2000,
                        Notification.Position.BOTTOM_START);

                // Don't send any instruction to AI - just remove the content
                // The conversation will continue from the remaining messages
            });
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

            Button cancelButton = new Button("Cancel", event -> confirmDialog.close());

            HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
            buttons.setSpacing(true);

            VerticalLayout dialogContent = new VerticalLayout(confirmText, buttons);
            dialogContent.setSpacing(true);
            dialogContent.setPadding(false);

            confirmDialog.add(dialogContent);
            confirmDialog.open();
        });

        messageContent.add(messageBubble, actionButtons);
        messageRow.add(messageContent);
        messagesContainer.add(messageRow);
        messageElements.add(messageRow);
        currentMessageGroup.add(messageRow);

        // Add to conversation history
        conversationHistory.add(new ConversationMessage("user", text));

        // Animate the message in
        getUI().ifPresent(ui -> ui.access(() -> {
            ui.getPage().executeJs("""
                        setTimeout(() => {
                            const message = arguments[0];
                            if (message) {
                                message.style.opacity = '1';
                                message.style.transform = 'translateY(0)';
                            }
                        }, 10);
                    """, messageRow.getElement());
        }));

        scrollToBottom();
    }

    private void addBotMessage(String text, boolean showAvatar) {
        // Update message group tracking
        if (!"bot".equals(lastMessageSender)) {
            // Starting a new bot message group
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
                .set("position", "relative")
                .set("opacity", "0")
                .set("transform", "translateY(10px)")
                .set("transition", "opacity 0.3s ease, transform 0.3s ease");

        // Bot avatar - only show if this is the last message in the group
        Div botAvatar = new Div();

        Image botGif = new Image("images/robot-icon.gif", "bot-icon");
        botGif.getStyle()
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "50%");
        botAvatar.add(botGif);

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

        messageRow.add(botAvatar, messageBubble);
        messagesContainer.add(messageRow);
        messageElements.add(messageRow);
        currentMessageGroup.add(messageRow);

        // Add to conversation history
        conversationHistory.add(new ConversationMessage("bot", text));

        // Animate the message in
        getUI().ifPresent(ui -> ui.access(() -> {
            ui.getPage().executeJs("""
                        setTimeout(() => {
                            const message = arguments[0];
                            if (message) {
                                message.style.opacity = '1';
                                message.style.transform = 'translateY(0)';
                            }
                        }, 10);
                    """, messageRow.getElement());
        }));

        scrollToBottom();
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
                .set("padding", "0 4px")
                .set("opacity", "0")
                .set("transform", "translateY(10px)")
                .set("transition", "opacity 0.3s ease, transform 0.3s ease");

        // Bot avatar for typing indicator
        Div botAvatar = new Div();
        Image botGif = new Image("images/robot-icon.gif", "bot-icon");
        botGif.getStyle()
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "50%");
        botAvatar.add(botGif);
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

        // Create dots with unique IDs for JavaScript animation
        for (int i = 0; i < 3; i++) {
            Div dot = new Div();
            dot.setId("typing-dot-" + i);
            dot.getStyle()
                    .set("width", "8px")
                    .set("height", "8px")
                    .set("background", "#9ca3af")
                    .set("border-radius", "50%")
                    .set("opacity", "0.4")
                    .set("transition", "transform 0.3s ease, opacity 0.3s ease");
            typingBubble.add(dot);
        }

        typingRow.add(botAvatar, typingBubble);
        messagesContainer.add(typingRow);
        scrollToBottom();

        // Animate in the typing indicator
        getUI().ifPresent(ui -> ui.access(() -> {
            ui.getPage().executeJs("""
                        setTimeout(() => {
                            const indicator = document.getElementById('typing-indicator');
                            if (indicator) {
                                indicator.style.opacity = '1';
                                indicator.style.transform = 'translateY(0)';
                            }
                        }, 10);
                    """);
        }));

        // Start JavaScript animation for wave effect
        getUI().ifPresent(ui -> {
            ui.getPage().executeJs("""
                        // Function to animate dots in wave pattern
                        function animateTypingDots() {
                            const dots = [
                                document.getElementById('typing-dot-0'),
                                document.getElementById('typing-dot-1'),
                                document.getElementById('typing-dot-2')
                            ];

                            if (!dots[0] || !dots[1] || !dots[2]) return;

                            let currentDot = 0;

                            function animateDot(index) {
                                // Reset all dots to default state
                                dots.forEach((dot, i) => {
                                    dot.style.transform = 'translateY(0px)';
                                    dot.style.opacity = '0.4';
                                    dot.style.transition = 'all 0.3s ease';
                                });

                                // Animate the current dot
                                dots[index].style.transform = 'translateY(-4px)';
                                dots[index].style.opacity = '1';

                                // Move to next dot
                                currentDot = (index + 1) % 3;

                                // Schedule next animation
                                setTimeout(() => animateDot(currentDot), 300);
                            }

                            // Start the animation after a short delay
                            setTimeout(() => animateDot(0), 100);
                        }

                        // Start animation
                        animateTypingDots();
                    """);
        });
    }

    private void removeTypingIndicator() {
        messagesContainer.getChildren()
                .filter(component -> component.getId().isPresent() &&
                        component.getId().get().equals("typing-indicator"))
                .findFirst()
                .ifPresent(indicator -> {
                    // Add fade out animation before removing
                    indicator.getStyle()
                            .set("opacity", "0")
                            .set("transform", "translateY(10px)")
                            .set("transition", "opacity 0.3s ease, transform 0.3s ease");

                    // Remove after animation completes
                    getUI().ifPresent(ui -> ui.access(() -> {
                        ui.setPollInterval(50);
                        CompletableFuture.delayedExecutor(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                                .execute(() -> {
                                    ui.access(() -> {
                                        messagesContainer.remove(indicator);
                                        scrollToBottom();
                                        ui.setPollInterval(-1);
                                        ui.push();
                                    });
                                });
                    }));
                });
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

    public void findNearbyEmergencyVets() {
        System.out.println("Finding nearby vets with coordinates: " + userLatitude + ", " + userLongitude);

        if (userLatitude == 0.0 && userLongitude == 0.0) {
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

                    Notification.show(
                            "Unable to find nearby emergency vets. Please search online or call your regular vet.",
                            1000, Notification.Position.BOTTOM_START);
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
            dialog.setWidth("800px"); // Slightly wider for better spacing
            dialog.setMaxWidth("95vw");
            dialog.setHeight("700px"); // Taller for better content flow
            dialog.setMaxHeight("90vh");
            dialog.setResizable(true);
            dialog.setDraggable(true);

            // Modern header with better spacing
            HorizontalLayout headerLayout = new HorizontalLayout();
            headerLayout.setWidthFull();
            headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            headerLayout.setPadding(true);
            headerLayout.getStyle()
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("background", "var(--lumo-base-color)");

            // Header left side with icon and title
            HorizontalLayout titleLayout = new HorizontalLayout();
            titleLayout.setSpacing(true);
            titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            Icon emergencyIcon = new Icon(VaadinIcon.HOSPITAL);
            emergencyIcon.addClassNames(LumoUtility.TextColor.ERROR);
            emergencyIcon.getStyle()
                    .set("font-size", "1.75rem");

            H3 headerTitle = new H3("Emergency Veterinary Clinics");
            headerTitle.addClassNames(
                    LumoUtility.TextColor.HEADER,
                    LumoUtility.Margin.NONE,
                    LumoUtility.FontWeight.BOLD,
                    LumoUtility.FontSize.LARGE);

            titleLayout.add(emergencyIcon, headerTitle);

            // Header right side with close button
            Button closeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            closeButton.addClickListener(e -> dialog.close());
            closeButton.getStyle()
                    .set("border-radius", "50%")
                    .set("padding", "0.5rem");

            headerLayout.add(titleLayout, closeButton);
            dialog.getHeader().add(headerLayout);

            // Modern content area with better spacing
            VerticalLayout content = new VerticalLayout();
            content.setPadding(true);
            content.setSpacing(false);
            content.setWidthFull();

            // Location info with modern design
            Div locationInfo = new Div();
            locationInfo.addClassNames(
                    LumoUtility.Background.SUCCESS_10,
                    LumoUtility.BorderRadius.LARGE,
                    LumoUtility.Padding.LARGE,
                    LumoUtility.Margin.Bottom.LARGE);

            locationInfo.getStyle()
                    .set("border", "1px solid var(--lumo-success-color-10pct)")
                    .set("backdrop-filter", "blur(10px)");

            HorizontalLayout locationLayout = new HorizontalLayout();
            locationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            locationLayout.setSpacing(true);

            Icon locationIcon = new Icon(VaadinIcon.LOCATION_ARROW);
            locationIcon.addClassNames(LumoUtility.TextColor.SUCCESS);
            locationIcon.getStyle().set("font-size", "1.25rem");

            VerticalLayout locationTextLayout = new VerticalLayout();
            locationTextLayout.setSpacing(false);
            locationTextLayout.setPadding(false);

            Span locationTitle = new Span("Current Location");
            locationTitle.addClassNames(
                    LumoUtility.FontWeight.SEMIBOLD,
                    LumoUtility.TextColor.SUCCESS,
                    LumoUtility.FontSize.SMALL);

            Span locationSubtitle = new Span("📍 Based on your device location");
            locationSubtitle.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL);

            locationTextLayout.add(locationTitle, locationSubtitle);
            locationLayout.add(locationIcon, locationTextLayout);
            locationInfo.add(locationLayout);

            content.add(locationInfo);

            // Results section
            if (nearbyVets.isArray() && nearbyVets.size() > 0) {
                // Results count with modern styling
                Div resultsHeader = new Div();
                resultsHeader.addClassNames(
                        LumoUtility.Margin.Bottom.MEDIUM,
                        LumoUtility.Padding.Horizontal.SMALL);

                Span resultCount = new Span("Found " + nearbyVets.size() + " emergency clinics nearby");
                resultCount.addClassNames(
                        LumoUtility.FontWeight.MEDIUM,
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.FontSize.SMALL);

                resultsHeader.add(resultCount);
                content.add(resultsHeader);

                // Vet cards container with grid layout
                Div cardsContainer = new Div();
                cardsContainer.getStyle()
                        .set("display", "grid")
                        .set("gap", "1rem")
                        .set("grid-template-columns", "1fr")
                        .set("max-height", "400px")
                        .set("overflow-y", "auto")
                        .set("padding", "0.5rem");

                for (JsonNode vetNode : nearbyVets) {
                    Div vetCard = createModernVetCard(vetNode);
                    cardsContainer.add(vetCard);
                }
                content.add(cardsContainer);
            } else {
                // Modern no results design
                Div noResultsCard = new Div();
                noResultsCard.addClassNames(
                        LumoUtility.Background.CONTRAST_5,
                        LumoUtility.BorderRadius.LARGE,
                        LumoUtility.Padding.XLARGE);
                noResultsCard.getStyle()
                        .set("text-align", "center")
                        .set("border", "1px solid var(--lumo-contrast-10pct)");

                Icon warningIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE);
                warningIcon.addClassNames(
                        LumoUtility.TextColor.TERTIARY,
                        LumoUtility.IconSize.LARGE);
                warningIcon.getStyle()
                        .set("margin-bottom", "1.5rem")
                        .set("opacity", "0.7");

                H4 noResultsTitle = new H4("No Emergency Clinics Found");
                noResultsTitle.addClassNames(
                        LumoUtility.TextColor.TERTIARY,
                        LumoUtility.Margin.NONE,
                        LumoUtility.Margin.Bottom.SMALL,
                        LumoUtility.FontWeight.MEDIUM);

                Paragraph suggestion = new Paragraph(
                        "We couldn't find emergency veterinary clinics in your immediate area. " +
                                "Try expanding your search radius or contact your regular veterinarian.");
                suggestion.addClassNames(
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.FontSize.SMALL);
                suggestion.getStyle()
                        .set("margin", "0")
                        .set("line-height", "1.6");

                noResultsCard.add(warningIcon, noResultsTitle, suggestion);
                content.add(noResultsCard);
            }

            // Modern footer
            HorizontalLayout footer = new HorizontalLayout();
            footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            footer.setWidthFull();
            footer.setPadding(true);
            footer.getStyle()
                    .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                    .set("background", "var(--lumo-base-color)");

            Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
            refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            refreshButton.setTooltipText("Search again");
            refreshButton.addClickListener(e -> {
                dialog.close();
                findNearbyEmergencyVets();
            });

            Button closeFooterButton = new Button("Close", new Icon(VaadinIcon.CLOSE));
            closeFooterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            closeFooterButton.addClickListener(e -> dialog.close());

            footer.add(refreshButton, closeFooterButton);
            dialog.getFooter().add(footer);

            // Make content scrollable with modern scroller
            Scroller scroller = new Scroller(content);
            scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
            scroller.getStyle()
                    .set("max-height", "500px")
                    .set("padding", "0")
                    .set("background", "var(--lumo-base-color)");

            dialog.add(scroller);

            // Modern dialog styling
            dialog.getElement().getStyle()
                    .set("border-radius", "16px")
                    .set("box-shadow", "0 20px 60px rgba(0, 0, 0, 0.15)")
                    .set("border", "none")
                    .set("backdrop-filter", "blur(20px)")
                    .set("background", "rgba(var(--lumo-base-color-rgb), 0.95)");

            dialog.open();

            // Smooth entrance animation
            dialog.getElement().executeJs(
                    "this.style.opacity = '0';" +
                            "this.style.transform = 'scale(0.96) translateY(20px)';" +
                            "this.animate([" +
                            "  { opacity: '0', transform: 'scale(0.96) translateY(20px)' }," +
                            "  { opacity: '1', transform: 'scale(1) translateY(0)' }" +
                            "], { " +
                            "  duration: 300, " +
                            "  easing: 'cubic-bezier(0.16, 1, 0.3, 1)' " +
                            "}).finished.then(() => {" +
                            "  this.style.opacity = '';" +
                            "  this.style.transform = '';" +
                            "});");

        } catch (Exception e) {
            System.err.println("Error parsing nearby vets response: " + e.getMessage());

            Notification notification = Notification.show(
                    "Unable to display emergency clinics. Please try again.",
                    4000,
                    Notification.Position.BOTTOM_START);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Div createModernVetCard(JsonNode vetNode) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE);

        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("transition", "all 0.2s ease")
                .set("cursor", "pointer")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)")
                .set("position", "relative");

        // Hover effects
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("transform", "translateY(-2px)")
                    .set("box-shadow", "0 8px 24px rgba(0,0,0,0.12)")
                    .set("border-color", "var(--lumo-primary-color-30pct)");
        });

        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)")
                    .set("border-color", "var(--lumo-contrast-10pct)");
        });

        // Emergency badge floating top right
        Span emergencyBadge = new Span("🚨 EMERGENCY");
        emergencyBadge.addClassNames(
                LumoUtility.Background.ERROR,
                LumoUtility.TextColor.ERROR_CONTRAST,
                LumoUtility.FontSize.XXSMALL,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BorderRadius.MEDIUM);
        emergencyBadge.getStyle()
                .set("position", "absolute")
                .set("top", "0.75rem")
                .set("right", "0.75rem")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        card.add(emergencyBadge);

        // Clinic name
        H4 name = new H4(vetNode.path("name").asText("Unknown Clinic"));
        name.addClassNames(
                LumoUtility.TextColor.HEADER,
                LumoUtility.Margin.NONE,
                LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.FontSize.MEDIUM);

        card.add(name);

        // Details container
        VerticalLayout details = new VerticalLayout();
        details.setSpacing(true);
        details.setPadding(false);

        // Address
        if (!vetNode.path("address").isMissingNode()) {
            HorizontalLayout addressLayout = new HorizontalLayout();
            addressLayout.setSpacing(true);
            addressLayout.setAlignItems(FlexComponent.Alignment.START);

            Icon addressIcon = new Icon(VaadinIcon.MAP_MARKER);
            addressIcon.addClassNames(LumoUtility.TextColor.SECONDARY);
            addressIcon.getStyle()
                    .set("font-size", "0.875rem")
                    .set("flex-shrink", "0");

            Span address = new Span(vetNode.path("address").asText());
            address.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            address.getStyle().set("line-height", "1.4");

            addressLayout.add(addressIcon, address);
            details.add(addressLayout);
        }

        // Phone
        if (!vetNode.path("phone").isMissingNode()) {
            HorizontalLayout phoneLayout = new HorizontalLayout();
            phoneLayout.setSpacing(true);
            phoneLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            Icon phoneIcon = new Icon(VaadinIcon.PHONE);
            phoneIcon.addClassNames(LumoUtility.TextColor.SUCCESS);
            phoneIcon.getStyle()
                    .set("font-size", "0.875rem");

            String phoneNumber = vetNode.path("phone").asText();
            Anchor phoneLink = new Anchor("tel:" + phoneNumber, phoneNumber);
            phoneLink.addClassNames(
                    LumoUtility.TextColor.SUCCESS,
                    LumoUtility.FontWeight.MEDIUM,
                    LumoUtility.FontSize.SMALL);
            phoneLink.getStyle()
                    .set("text-decoration", "none");

            phoneLayout.add(phoneIcon, phoneLink);
            details.add(phoneLayout);
        }

        // Distance
        if (!vetNode.path("distance").isMissingNode()) {
            HorizontalLayout distanceLayout = new HorizontalLayout();
            distanceLayout.setSpacing(true);
            distanceLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            Icon distanceIcon = new Icon(VaadinIcon.ROAD);
            distanceIcon.addClassNames(LumoUtility.TextColor.PRIMARY);
            distanceIcon.getStyle()
                    .set("font-size", "0.875rem");

            Span distance = new Span(vetNode.path("distance").asText() + " away");
            distance.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontSize.SMALL);

            distanceLayout.add(distanceIcon, distance);
            details.add(distanceLayout);
        }

        card.add(details);
        return card;
    }

    @ClientCallable
    public void triggerEmergency() {
        findNearbyEmergencyVets();
    }

    private void scrollToBottom() {
        getUI().ifPresent(ui -> ui.access(() -> {
            // Use a more reliable scrolling method with smooth animation
            messagesScroller.getElement().executeJs("""
                        // Function to scroll smoothly to bottom
                        function smoothScrollToBottom() {
                            const scroller = this;
                            const targetScrollTop = scroller.scrollHeight;
                            const startingScrollTop = scroller.scrollTop;
                            const distance = targetScrollTop - startingScrollTop;
                            const duration = 300; // ms
                            const startTime = performance.now();

                            function scrollStep(timestamp) {
                                const currentTime = timestamp || performance.now();
                                const timeElapsed = currentTime - startTime;
                                const progress = Math.min(timeElapsed / duration, 1);

                                // Easing function (easeOutQuad)
                                const ease = progress * (2 - progress);

                                scroller.scrollTop = startingScrollTop + (distance * ease);

                                if (timeElapsed < duration) {
                                    requestAnimationFrame(scrollStep);
                                } else {
                                    scroller.scrollTop = targetScrollTop;
                                }
                            }

                            requestAnimationFrame(scrollStep);
                        }

                        // Start smooth scroll
                        smoothScrollToBottom.call(this);
                    """);
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
                        Notification.show("Failed to start session. Please refresh the page.", 3000,
                                Notification.Position.BOTTOM_START);
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
                            "    resizeTextarea();" +
                            "  }" +
                            "}, 100);");
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
                            "    resizeTextarea();" +
                            "  }" +
                            "}, 100);");
        });
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