package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Transfer Object (DTO) for structured veterinary responses in the Virtual Vet application.
 * 
 * This DTO represents a comprehensive, structured response from the AI veterinary system,
 * containing detailed analysis, recommendations, warnings, and actionable advice. It provides
 * a rich format for delivering complex veterinary consultations with organized information
 * including urgency assessments, symptom identification, treatment recommendations, and
 * veterinary contact guidance.
 * 
 * The class includes nested classes for ResponseMessage, ResponseList, and Recommendation
 * to provide structured data organization. It features comprehensive toString() methods
 * for debugging and logging purposes, and uses Jackson annotations for JSON serialization.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class StructuredVetResponse {

    /**
     * Overall urgency assessment of the pet's condition.
     * Indicates the priority level for veterinary attention (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL").
     */
    @JsonProperty("urgency")
    private String urgency;

    /**
     * Comprehensive assessment of the pet's condition.
     * Contains the AI's analysis and evaluation of the reported symptoms and situation.
     */
    @JsonProperty("assessment")
    private String assessment;

    /**
     * List of structured response messages for display.
     * Each ResponseMessage contains type, content, emphasis, and timing information
     * for organized presentation to the user.
     */
    @JsonProperty("messages")
    private List<ResponseMessage> messages = new ArrayList<>();

    /**
     * List of structured lists for organizing information.
     * Each ResponseList contains a title, type, and items for presenting
     * categorized information such as symptoms, treatments, or precautions.
     */
    @JsonProperty("lists")
    private List<ResponseList> lists = new ArrayList<>();

    /**
     * List of detailed treatment recommendations.
     * Each Recommendation includes action, timeframe, and priority information
     * for actionable veterinary advice.
     */
    @JsonProperty("recommendations")
    private List<Recommendation> recommendations = new ArrayList<>();

    /**
     * List of follow-up questions for the pet owner.
     * Questions that can help gather additional information or clarify symptoms.
     */
    @JsonProperty("questions")
    private List<String> questions = new ArrayList<>();

    /**
     * List of important warnings or cautions.
     * Critical information that pet owners need to be aware of immediately.
     */
    @JsonProperty("warnings")
    private List<String> warnings = new ArrayList<>();

    /**
     * Description of the next steps the pet owner should take.
     * Clear, actionable guidance for immediate or future care.
     */
    @JsonProperty("nextSteps")
    private String nextSteps;

    /**
     * Flag indicating whether veterinary contact is recommended.
     * Determines if the pet owner should seek professional veterinary care.
     */
    @JsonProperty("vetContactRecommended")
    private boolean vetContactRecommended = false;

    /**
     * Timeframe within which veterinary contact should be made.
     * Provides guidance on urgency of seeking professional care (e.g., "immediately", "within 24 hours").
     */
    @JsonProperty("vetContactTimeframe")
    private String vetContactTimeframe;

    /**
     * Reason why veterinary contact is recommended.
     * Explanation of the specific concerns that warrant professional veterinary attention.
     */
    @JsonProperty("vetContactReason")
    private String vetContactReason;

    /**
     * List of symptoms identified by the AI analysis.
     * Symptoms that have been detected or mentioned during the consultation.
     */
    private List<String> identifiedSymptoms = new ArrayList<>();

    /**
     * Checks if veterinary contact is recommended for this case.
     * 
     * @return true if professional veterinary care is recommended, false otherwise
     */
    public boolean isVetContactRecommended() {
        return vetContactRecommended;
    }

    /**
     * Sets whether veterinary contact is recommended for this case.
     * 
     * @param vetContactRecommended true if veterinary care is recommended, false otherwise
     */
    public void setVetContactRecommended(boolean vetContactRecommended) {
        this.vetContactRecommended = vetContactRecommended;
    }

    /**
     * Gets the timeframe for veterinary contact.
     * 
     * @return the timeframe string indicating when to seek veterinary care
     */
    public String getVetContactTimeframe() {
        return vetContactTimeframe;
    }

    /**
     * Sets the timeframe for veterinary contact.
     * 
     * @param vetContactTimeframe the timeframe string to set
     */
    public void setVetContactTimeframe(String vetContactTimeframe) {
        this.vetContactTimeframe = vetContactTimeframe;
    }

    /**
     * Gets the reason for recommending veterinary contact.
     * 
     * @return the reason string explaining why veterinary care is needed
     */
    public String getVetContactReason() {
        return vetContactReason;
    }

    /**
     * Sets the reason for recommending veterinary contact.
     * 
     * @param vetContactReason the reason string to set
     */
    public void setVetContactReason(String vetContactReason) {
        this.vetContactReason = vetContactReason;
    }

    /**
     * Gets the urgency assessment of the pet's condition.
     * 
     * @return the urgency level string
     */
    public String getUrgency() {
        return urgency;
    }

    /**
     * Sets the urgency assessment of the pet's condition.
     * 
     * @param urgency the urgency level string to set
     */
    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    /**
     * Gets the comprehensive assessment of the pet's condition.
     * 
     * @return the assessment text
     */
    public String getAssessment() {
        return assessment;
    }

    /**
     * Sets the comprehensive assessment of the pet's condition.
     * 
     * @param assessment the assessment text to set
     */
    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    /**
     * Gets the list of structured response messages.
     * 
     * @return the list of ResponseMessage objects
     */
    public List<ResponseMessage> getMessages() {
        return messages;
    }

    /**
     * Sets the list of structured response messages.
     * 
     * @param messages the list of ResponseMessage objects to set
     */
    public void setMessages(List<ResponseMessage> messages) {
        this.messages = messages;
    }

    /**
     * Gets the list of structured response lists.
     * 
     * @return the list of ResponseList objects
     */
    public List<ResponseList> getLists() {
        return lists;
    }

    /**
     * Sets the list of structured response lists.
     * 
     * @param lists the list of ResponseList objects to set
     */
    public void setLists(List<ResponseList> lists) {
        this.lists = lists;
    }

    /**
     * Gets the list of treatment recommendations.
     * 
     * @return the list of Recommendation objects
     */
    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    /**
     * Sets the list of treatment recommendations.
     * 
     * @param recommendations the list of Recommendation objects to set
     */
    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    /**
     * Gets the list of follow-up questions.
     * 
     * @return the list of question strings
     */
    public List<String> getQuestions() {
        return questions;
    }

    /**
     * Sets the list of follow-up questions.
     * 
     * @param questions the list of question strings to set
     */
    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    /**
     * Gets the list of important warnings.
     * 
     * @return the list of warning strings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Sets the list of important warnings.
     * 
     * @param warnings the list of warning strings to set
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * Gets the description of next steps.
     * 
     * @return the next steps text
     */
    public String getNextSteps() {
        return nextSteps;
    }

    /**
     * Sets the description of next steps.
     * 
     * @param nextSteps the next steps text to set
     */
    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }

    /**
     * Gets the list of identified symptoms.
     * 
     * @return the list of symptom strings
     */
    public List<String> getIdentifiedSymptoms() {
        return identifiedSymptoms;
    }

    /**
     * Sets the list of identified symptoms.
     * 
     * @param identifiedSymptoms the list of symptom strings to set
     */
    public void setIdentifiedSymptoms(List<String> identifiedSymptoms) {
        this.identifiedSymptoms = identifiedSymptoms;
    }

    /**
     * Generates a comprehensive string representation of this structured response.
     * 
     * Creates a detailed string representation including all fields and nested objects,
     * formatted for debugging and logging purposes. This method is useful for
     * troubleshooting and monitoring the AI's response generation.
     * 
     * @return a formatted string representation of the entire response structure
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StructuredVetResponse{");
        sb.append("\n  urgency='").append(urgency).append("'");
        sb.append("\n  assessment='").append(assessment).append("'");

        // Format messages list
        if (!messages.isEmpty()) {
            sb.append("\n  messages=[");
            for (int i = 0; i < messages.size(); i++) {
                sb.append("\n    ").append(messages.get(i).toString());
                if (i < messages.size() - 1)
                    sb.append(",");
            }
            sb.append("\n  ]");
        } else {
            sb.append("\n  messages=[]");
        }

        // Format lists
        if (!lists.isEmpty()) {
            sb.append("\n  lists=[");
            for (int i = 0; i < lists.size(); i++) {
                sb.append("\n    ").append(lists.get(i).toString());
                if (i < lists.size() - 1)
                    sb.append(",");
            }
            sb.append("\n  ]");
        } else {
            sb.append("\n  lists=[]");
        }

        // Format recommendations
        if (!recommendations.isEmpty()) {
            sb.append("\n  recommendations=[");
            for (int i = 0; i < recommendations.size(); i++) {
                sb.append("\n    ").append(recommendations.get(i).toString());
                if (i < recommendations.size() - 1)
                    sb.append(",");
            }
            sb.append("\n  ]");
        } else {
            sb.append("\n  recommendations=[]");
        }

        // Add remaining fields
        sb.append("\n  questions=").append(questions);
        sb.append("\n  warnings=").append(warnings);
        sb.append("\n  nextSteps='").append(nextSteps).append("'");
        sb.append("\n  vetContactRecommended=").append(vetContactRecommended);
        sb.append("\n  vetContactTimeframe='").append(vetContactTimeframe).append("'");
        sb.append("\n  vetContactReason='").append(vetContactReason).append("'");
        sb.append("\n}");

        return sb.toString();
    }

    /**
     * Nested class representing a structured response message.
     * 
     * This class encapsulates individual messages within the structured response,
     * including message type, content, emphasis level, and display timing information.
     * It provides flexibility in how messages are presented to users in the chat interface.
     */
    public static class ResponseMessage {
        /**
         * Type of message (e.g., "info", "warning", "success", "error").
         * Used for styling and presentation in the user interface.
         */
        @JsonProperty("type")
        private String type;

        /**
         * The actual message content to display.
         * Contains the text that will be shown to the user.
         */
        @JsonProperty("content")
        private String content;

        /**
         * Emphasis level for the message (e.g., "normal", "high", "urgent").
         * Affects how prominently the message is displayed.
         */
        @JsonProperty("emphasis")
        private String emphasis;

        /**
         * Delay in milliseconds before displaying this message.
         * Used for creating sequential or timed message presentations.
         * Defaults to 800 milliseconds.
         */
        @JsonProperty("delay")
        private int delay = 800; // default delay in milliseconds

        /**
         * Gets the message type.
         * 
         * @return the message type string
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the message type.
         * 
         * @param type the message type string to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Gets the message content.
         * 
         * @return the message content text
         */
        public String getContent() {
            return content;
        }

        /**
         * Sets the message content.
         * 
         * @param content the message content text to set
         */
        public void setContent(String content) {
            this.content = content;
        }

        /**
         * Gets the emphasis level.
         * 
         * @return the emphasis level string
         */
        public String getEmphasis() {
            return emphasis;
        }

        /**
         * Sets the emphasis level.
         * 
         * @param emphasis the emphasis level string to set
         */
        public void setEmphasis(String emphasis) {
            this.emphasis = emphasis;
        }

        /**
         * Gets the display delay in milliseconds.
         * 
         * @return the delay in milliseconds
         */
        public int getDelay() {
            return delay;
        }

        /**
         * Sets the display delay in milliseconds.
         * 
         * @param delay the delay in milliseconds to set
         */
        public void setDelay(int delay) {
            this.delay = delay;
        }

        /**
         * Generates a string representation of this response message.
         * 
         * @return a formatted string containing all message properties
         */
        @Override
        public String toString() {
            return "ResponseMessage{" +
                    "type='" + type + "', " +
                    "content='" + content + "', " +
                    "emphasis='" + emphasis + "', " +
                    "delay=" + delay +
                    "}";
        }
    }

    /**
     * Nested class representing a structured list within the response.
     * 
     * This class encapsulates organized lists of information such as symptoms,
     * treatments, or precautions, providing a title and type for categorization.
     */
    public static class ResponseList {
        /**
         * Title or heading for this list.
         * Provides context and categorization for the list items.
         */
        @JsonProperty("title")
        private String title;

        /**
         * Type of list (e.g., "symptoms", "treatments", "precautions").
         * Used for styling and understanding the list's purpose.
         */
        @JsonProperty("type")
        private String type;

        /**
         * List of items in this structured list.
         * Contains the actual content items to be displayed.
         */
        @JsonProperty("items")
        private List<String> items = new ArrayList<>();

        /**
         * Gets the list title.
         * 
         * @return the title string
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the list title.
         * 
         * @param title the title string to set
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Gets the list type.
         * 
         * @return the type string
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the list type.
         * 
         * @param type the type string to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Gets the list of items.
         * 
         * @return the list of item strings
         */
        public List<String> getItems() {
            return items;
        }

        /**
         * Sets the list of items.
         * 
         * @param items the list of item strings to set
         */
        public void setItems(List<String> items) {
            this.items = items;
        }

        /**
         * Generates a string representation of this response list.
         * 
         * @return a formatted string containing all list properties
         */
        @Override
        public String toString() {
            return "ResponseList{" +
                    "title='" + title + "', " +
                    "type='" + type + "', " +
                    "items=" + items +
                    "}";
        }
    }

    /**
     * Nested class representing a treatment recommendation.
     * 
     * This class encapsulates detailed treatment recommendations including
     * the action to take, timeframe for implementation, and priority level.
     */
    public static class Recommendation {
        /**
         * The specific action or treatment to be taken.
         * Contains the detailed recommendation for the pet owner.
         */
        @JsonProperty("action")
        private String action;

        /**
         * Timeframe within which the action should be taken.
         * Provides guidance on timing (e.g., "immediately", "within 24 hours").
         */
        @JsonProperty("timeframe")
        private String timeframe;

        /**
         * Priority level of this recommendation.
         * Indicates the importance (e.g., "high", "medium", "low").
         */
        @JsonProperty("priority")
        private String priority;

        /**
         * Gets the recommended action.
         * 
         * @return the action string
         */
        public String getAction() {
            return action;
        }

        /**
         * Sets the recommended action.
         * 
         * @param action the action string to set
         */
        public void setAction(String action) {
            this.action = action;
        }

        /**
         * Gets the timeframe for the action.
         * 
         * @return the timeframe string
         */
        public String getTimeframe() {
            return timeframe;
        }

        /**
         * Sets the timeframe for the action.
         * 
         * @param timeframe the timeframe string to set
         */
        public void setTimeframe(String timeframe) {
            this.timeframe = timeframe;
        }

        /**
         * Gets the priority level.
         * 
         * @return the priority string
         */
        public String getPriority() {
            return priority;
        }

        /**
         * Sets the priority level.
         * 
         * @param priority the priority string to set
         */
        public void setPriority(String priority) {
            this.priority = priority;
        }

        /**
         * Generates a string representation of this recommendation.
         * 
         * @return a formatted string containing all recommendation properties
         */
        @Override
        public String toString() {
            return "Recommendation{" +
                    "action='" + action + "', " +
                    "timeframe='" + timeframe + "', " +
                    "priority='" + priority + "'" +
                    "}";
        }
    }
}