package com.virtualvet.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.virtualvet.util.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private List<UploadedFileData> uploadedFiles = new ArrayList<>(); // Changed from uploadedImageData

    private Div imagePreviewContainer;

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
        setupLayout(); // This should call createInputArea() internally
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
        // .set("overflow", "hidden");

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
        // Image preview container (above input area)
        imagePreviewContainer = new Div();
        imagePreviewContainer.setWidthFull();
        imagePreviewContainer.getStyle()
                .set("padding", "8px 16px 0 16px")
                .set("background", "white")
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "8px")
                .set("min-height", "0")
                .set("transition", "all 0.2s ease");
        imagePreviewContainer.setVisible(false); // Hidden by default

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

        messageInput.addKeyDownListener(e -> {
            if (e.getKey().equals("Enter") && !isWaitingForResponse) {
                // Use JavaScript to check if Shift key is pressed
                getElement().executeJs("return $0.shiftKey;", e)
                        .then(Boolean.class, isShiftPressed -> {
                            if (!isShiftPressed) {
                                sendMessage();
                            }
                        });
            }
        });

        // File upload component using new UploadHandler API
        multiFileBuffer = new MultiFileMemoryBuffer();

        upload = new Upload(multiFileBuffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
        upload.setMaxFiles(5); // Allow up to five images
        upload.setMaxFileSize(10 * 1024 * 1024); // 10MB per file
        upload.setDropAllowed(false); // Disable drag-and-drop
        upload.addSucceededListener(event -> {
            try {
                String fileName = event.getFileName();
                System.out.println("File upload succeeded: " + fileName);
                InputStream inputStream = multiFileBuffer.getInputStream(fileName);
                byte[] data = inputStream.readAllBytes();
                String contentType = event.getMIMEType();
                System.out.println("File size: " + data.length + " bytes, type: " + contentType);

                UploadedFileData fileData = new UploadedFileData(fileName, contentType, data);
                uploadedFiles.add(fileData);

                // Add to UI on the UI thread
                getUI().ifPresent(ui -> ui.access(() -> {
                    addImagePreview(fileName, fileData.getBase64Data(), uploadedFiles.size() - 1);
                    showNotification("File uploaded: " + fileName, false);
                    ui.push();
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        upload.addStartedListener(event -> {
            System.out.println("Upload started: " + event.getFileName());
        });

        upload.addFinishedListener(event -> {
            System.out.println("Upload finished: " + event.getFileName());
        });

        // Hide the default upload component completely
        upload.getStyle()
                .set("position", "absolute")
                .set("right", "44px")
                .set("top", "50%")
                .set("transform", "translateY(-50%)")
                .set("width", "32px")
                .set("height", "32px")
                .set("opacity", "0")
                .set("z-index", "20")
                .set("pointer-events", "none"); // Disable pointer events on the hidden upload

        // Upload button (paperclip icon) - this will trigger the file dialog
        uploadButton = new Button();
        Icon uploadIcon = new Icon(VaadinIcon.PAPERCLIP);
        uploadIcon.setSize("20px");
        uploadIcon.setColor("#6b7280");
        uploadButton.setIcon(uploadIcon);
        uploadButton.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("border-radius", "50%")
                .set("width", "32px")
                .set("height", "32px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("position", "absolute")
                .set("right", "44px")
                .set("top", "50%")
                .set("transform", "translateY(-50%)")
                .set("z-index", "10")
                .set("transition", "all 0.2s ease");

        // Add click listener to trigger file upload
        uploadButton.addClickListener(e -> {
            // Trigger the file dialog by clicking the upload component
            upload.getElement().executeJs("this.shadowRoot.querySelector('input[type=file]').click()");
        });

        // Send button
        sendButton = new Button();
        Icon sendIcon = new Icon(VaadinIcon.PAPERPLANE);
        sendIcon.setSize("18px");
        sendIcon.setColor("#2563eb");
        sendButton.setIcon(sendIcon);
        sendButton.getStyle()
                .set("background", "transparent")
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
                .set("transition", "all 0.2s ease");

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
        inputArea.add(imagePreviewContainer, inputContainer);
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

    // Add method to handle image preview
    private void addImagePreview(String fileName, String base64Data, int fileIndex) {
        Div previewItem = new Div();
        previewItem.getStyle()
                .set("position", "relative")
                .set("display", "inline-block")
                .set("border-radius", "8px")
                .set("overflow", "hidden")
                .set("background", "#f3f4f6")
                .set("border", "1px solid #e5e7eb");

        // Image element
        Image previewImage = new Image();
        previewImage.setSrc("data:image/png;base64," + base64Data);
        previewImage.setAlt(fileName);
        previewImage.getStyle()
                .set("width", "80px")
                .set("height", "80px")
                .set("object-fit", "cover")
                .set("display", "block");

        // Remove button (X)
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
            // Remove from uploadedFiles list
            if (fileIndex < uploadedFiles.size()) {
                uploadedFiles.remove(fileIndex);
            }
            previewItem.removeFromParent();
            updatePreviewContainerVisibility();
            // Refresh preview container to update indices
            refreshImagePreviews();
        });

        previewItem.add(previewImage, removeButton);
        imagePreviewContainer.add(previewItem);
        updatePreviewContainerVisibility();

    }

    // Add method to refresh image previews after removal:
    private void refreshImagePreviews() {
        imagePreviewContainer.removeAll();
        for (int i = 0; i < uploadedFiles.size(); i++) {
            UploadedFileData fileData = uploadedFiles.get(i);
            addImagePreview(fileData.getFilename(), fileData.getBase64Data(), i);
        }
    }

    // Method to show/hide preview container
    private void updatePreviewContainerVisibility() {
        boolean hasImages = imagePreviewContainer.getChildren().count() > 0;
        imagePreviewContainer.setVisible(hasImages);

        if (hasImages) {
            imagePreviewContainer.getStyle().set("padding", "8px 16px");
        } else {
            imagePreviewContainer.getStyle().set("padding", "0");
        }
    }

    private void setupLayout() {
        getStyle()
                .set("height", "100vh")
                .set("max-height", "100vh")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("background", "white")
                .set("padding", "0")
                .set("margin", "0");

        // Create main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setPadding(false);
        mainContainer.setSpacing(false);
        mainContainer.setSizeFull();
        mainContainer.getStyle()
                .set("height", "100%")
                .set("overflow", "hidden");

        // Add emergency banner and messages scroller
        mainContainer.add(emergencyBanner, messagesScroller);
        mainContainer.setFlexGrow(1, messagesScroller);

        // Add main container first, then input will be added at bottom
        add(mainContainer);

        // Create input area (this will automatically go to bottom)
        createInputArea();
    }

    private void addWelcomeMessage() {
        addBotMessages(List.of(
                "👋 Hello! I'm your Virtual Vet Assistant.",
                "I'm here to help you with:\n🔸 Pet health concerns and symptoms\n🔸 Emergency guidance and triage\n🔸 General veterinary advice",
                "⚠️ **For true emergencies, please contact your local veterinary clinic immediately!**",
                "How can I help you and your pet today?"));
    }

   
    @ClientCallable
    private void sendMessage() {
        String message = messageInput.getValue().trim();
        if (message.isEmpty() || isWaitingForResponse) {
            return;
        }

        if (currentSessionId == null) {
            showNotification("Please wait, initializing session...", false);
            return;
        }

        // Add user message
        addUserMessage(message);
        messageInput.clear();

        // Show typing indicator and disable input
        showTypingIndicator();
        setInputEnabled(false);
        scrollToBottom();

        // First analyze images if any, then send chat message
        analyzeImagesAsync(new ArrayList<>(uploadedFiles))
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

                            // Still include the actual image files for the chat API if needed
                            for (UploadedFileData fileData : uploadedFiles) {
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

                                // Split response into natural message chunks
                                List<String> messages = splitIntoMessages(responseText);
                                addBotMessages(messages);

                                checkForEmergency(responseText);
                                setInputEnabled(true);
                                clearImagePreviews();
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
                                addBotMessage("I'm experiencing technical difficulties. Please try again.");
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

    // Clear after sending a msg
    private void clearImagePreviews() {
        imagePreviewContainer.removeAll();
        uploadedFiles.clear(); // Changed from uploadedImageData
        updatePreviewContainerVisibility();
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
            // No split marker found - use fallback logic for backwards compatibility

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

                // Look for natural breaking points
                String[] paragraphs = cleanedText.split("\\n\\s*\\n");

                StringBuilder currentPart = new StringBuilder();

                for (String paragraph : paragraphs) {
                    paragraph = paragraph.trim();
                    if (paragraph.isEmpty())
                        continue;

                    // Check if this is a good breaking point
                    boolean isBreakPoint = false;

                    // Break before common phrases
                    if (paragraph.toLowerCase().startsWith("for now") ||
                            paragraph.toLowerCase().startsWith("you can try") ||
                            paragraph.toLowerCase().startsWith("here are") ||
                            paragraph.toLowerCase().startsWith("recommendations") ||
                            paragraph.toLowerCase().startsWith("**when to seek")) {
                        isBreakPoint = true;
                    }

                    // Break if current part is getting long (over 400 chars) and this is a new
                    // section
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

                // If we didn't get good splits, fall back to simpler approach
                if (parts.size() <= 1) {
                    // Try splitting at double line breaks
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
                // For regular text without lists, split into reasonable chunks
                int maxLength = 300;
                if (cleanedText.length() <= maxLength) {
                    messages.add(cleanedText);
                } else {
                    // Split by sentences where possible
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

            // Add small delay between messages for natural feel
            UI ui = getUI().orElse(null);
            if (ui != null) {
                ui.access(() -> {
                    try {
                        Thread.sleep(index * 800 + 300); // Stagger messages
                        ui.access(() -> {
                            addBotMessage(message);
                            scrollToBottom();
                            ui.push();
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        addBotMessage(message);
                        scrollToBottom();
                        ui.push();
                    }
                });
            }
        }
    }

    private void addUserMessage(String text) {
        Div messageRow = new Div();
        messageRow.setWidthFull();
        messageRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("margin-bottom", "8px")
                .set("padding", "0 4px");

        Div messageBubble = new Div();
        messageBubble.getElement().setProperty("innerHTML", formatText(text));
        messageBubble.getStyle()
                .set("background", "#2563eb")
                .set("color", "white")
                .set("padding", "12px 16px")
                .set("border-radius", "18px 18px 6px 18px")
                .set("max-width", "80%")
                .set("min-width", "20px")
                .set("font-size", "16px")
                .set("font-family", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif")
                .set("line-height", "1.4")
                .set("word-wrap", "break-word")
                .set("white-space", "pre-wrap");

        messageRow.add(messageBubble);
        messagesContainer.add(messageRow);
        messageElements.add(messageRow);
    }

    private void addBotMessage(String text) {
        Div messageRow = new Div();
        messageRow.setWidthFull();
        messageRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-start")
                .set("margin-bottom", "8px")
                .set("padding", "0 4px");

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
                .set("white-space", "pre-wrap");

        messageRow.add(messageBubble);
        messagesContainer.add(messageRow);
        messageElements.add(messageRow);
    }

    private String formatText(String text) {
        if (text == null || text.isEmpty())
            return "";

        // Normalize quotes and apostrophes
        text = text.replace("’", "'").replace("“", "\"").replace("”", "\"");

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
                // Skip empty lines inside lists
                continue;
            } else {
                // Only add one empty line for paragraph breaks
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
                .set("margin-bottom", "8px")
                .set("padding", "0 4px");

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

        typingRow.add(typingBubble);
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
            sendButton.getStyle().set("opacity", "1");
            uploadButton.getStyle().set("opacity", "1");
            messageInput.getStyle().set("opacity", "1");
        } else {
            sendButton.getStyle().set("opacity", "0.6");
            uploadButton.getStyle().set("opacity", "0.6");
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
            showNotification("Searching for emergency veterinarians near you...", false);
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
                addWelcomeMessage();
            });

            ui.getPage().executeJs(
                    "const style = document.createElement('style');" +
                            "style.textContent = `" +

            // Fix layout for the entire view
                            "vaadin-vertical-layout[width='100%'] {" +
                            "  display: flex !important;" +
                            "  flex-direction: column !important;" +
                            "  height: 100vh !important;" +
                            "  overflow: hidden !important;" +
                            "}" +

            // Ensure messages container scrolls properly
                            "vaadin-scroller {" +
                            "  flex: 1 1 auto !important;" +
                            "  min-height: 0 !important;" +
                            "  height: 100% !important;" +
                            "  overflow-y: auto !important;" +
                            "  overflow-x: hidden !important;" +
                            "}" +

            // Hide scrollbars completely
                            "vaadin-scroller::-webkit-scrollbar {" +
                            "  width: 0 !important;" +
                            "  background: transparent !important;" +
                            "}" +

                            "vaadin-scroller {" +
                            "  -ms-overflow-style: none !important;" +
                            "  scrollbar-width: none !important;" +
                            "}" +

            // Fix text area styling - PREVENT DOUBLE SCROLLBARS
                            "vaadin-text-area::part(input-field) {" +
                            "  background: transparent !important;" +
                            "  border: none !important;" +
                            "  box-shadow: none !important;" +
                            "  overflow: hidden !important;" + // KEY FIX: Prevent scrollbar on container
                            "}" +

                            "vaadin-text-area[focus-ring]::part(input-field) {" +
                            "  box-shadow: none !important;" +
                            "}" +

            // Style the actual textarea to handle scrolling properly
                            "vaadin-text-area textarea {" +
                            "  overflow-y: auto !important;" +
                            "  overflow-x: hidden !important;" +
                            "  resize: none !important;" +
                            "  scrollbar-width: thin !important;" + // Make scrollbar thinner
                            "}" +

            // Optional: Hide textarea scrollbar on webkit browsers
                            "vaadin-text-area textarea::-webkit-scrollbar {" +
                            "  width: 4px !important;" +
                            "}" +

                            "vaadin-text-area textarea::-webkit-scrollbar-track {" +
                            "  background: transparent !important;" +
                            "}" +

                            "vaadin-text-area textarea::-webkit-scrollbar-thumb {" +
                            "  background: rgba(0,0,0,0.2) !important;" +
                            "  border-radius: 4px !important;" +
                            "}" +

                            "`;" +
                            "document.head.appendChild(style);" +

            // Enhanced textarea auto-resize logic
                            "const textarea = document.querySelector('vaadin-text-area textarea');" +
                            "if (textarea) {" +
                            "  const minHeight = 44;" + // Your minimum height
                            "  const maxHeight = 120;" + // Your maximum height
                            "  " +
                            "  function resizeTextarea() {" +
                            "    textarea.style.height = minHeight + 'px';" + // Reset to min height
                            "    const scrollHeight = textarea.scrollHeight;" +
                            "    " +
                            "    if (scrollHeight <= maxHeight) {" +
                            "      textarea.style.height = scrollHeight + 'px';" +
                            "      textarea.style.overflowY = 'hidden';" + // Hide scrollbar when not needed
                            "    } else {" +
                            "      textarea.style.height = maxHeight + 'px';" +
                            "      textarea.style.overflowY = 'auto';" + // Show scrollbar only when needed
                            "    }" +
                            "  }" +
                            "  " +
                            "  textarea.addEventListener('input', resizeTextarea);" +
                            "  textarea.addEventListener('focus', resizeTextarea);" +
                            "  textarea.addEventListener('blur', resizeTextarea);" +
                            "  " +
                            "  resizeTextarea();" +
                            "}");
        });
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

}
