package com.virtualvet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

public class StructuredVetResponse {

    @JsonProperty("urgency")
    private String urgency;

    @JsonProperty("assessment")
    private String assessment;

    @JsonProperty("messages")
    private List<ResponseMessage> messages = new ArrayList<>();

    @JsonProperty("lists")
    private List<ResponseList> lists = new ArrayList<>();

    @JsonProperty("recommendations")
    private List<Recommendation> recommendations = new ArrayList<>();

    @JsonProperty("questions")
    private List<String> questions = new ArrayList<>();

    @JsonProperty("warnings")
    private List<String> warnings = new ArrayList<>();

    @JsonProperty("nextSteps")
    private String nextSteps;

    @JsonProperty("vetContactRecommended")
    private boolean vetContactRecommended = false;

    @JsonProperty("vetContactTimeframe")
    private String vetContactTimeframe;

    @JsonProperty("vetContactReason")
    private String vetContactReason;

    // Add getters and setters
    public boolean isVetContactRecommended() {
        return vetContactRecommended;
    }

    public void setVetContactRecommended(boolean vetContactRecommended) {
        this.vetContactRecommended = vetContactRecommended;
    }

    public String getVetContactTimeframe() {
        return vetContactTimeframe;
    }

    public void setVetContactTimeframe(String vetContactTimeframe) {
        this.vetContactTimeframe = vetContactTimeframe;
    }

    public String getVetContactReason() {
        return vetContactReason;
    }

    public void setVetContactReason(String vetContactReason) {
        this.vetContactReason = vetContactReason;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getAssessment() {
        return assessment;
    }

    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    public List<ResponseMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ResponseMessage> messages) {
        this.messages = messages;
    }

    public List<ResponseList> getLists() {
        return lists;
    }

    public void setLists(List<ResponseList> lists) {
        this.lists = lists;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StructuredVetResponse{");
        sb.append("\n  urgency='").append(urgency).append("'");
        sb.append("\n  assessment='").append(assessment).append("'");
        
        if (!messages.isEmpty()) {
            sb.append("\n  messages=[");
            for (int i = 0; i < messages.size(); i++) {
                sb.append("\n    ").append(messages.get(i).toString());
                if (i < messages.size() - 1) sb.append(",");
            }
            sb.append("\n  ]");
        } else {
            sb.append("\n  messages=[]");
        }
        
        if (!lists.isEmpty()) {
            sb.append("\n  lists=[");
            for (int i = 0; i < lists.size(); i++) {
                sb.append("\n    ").append(lists.get(i).toString());
                if (i < lists.size() - 1) sb.append(",");
            }
            sb.append("\n  ]");
        } else {
            sb.append("\n  lists=[]");
        }
        
        if (!recommendations.isEmpty()) {
            sb.append("\n  recommendations=[");
            for (int i = 0; i < recommendations.size(); i++) {
                sb.append("\n    ").append(recommendations.get(i).toString());
                if (i < recommendations.size() - 1) sb.append(",");
            }
            sb.append("\n  ]");
        } else {
            sb.append("\n  recommendations=[]");
        }
        
        sb.append("\n  questions=").append(questions);
        sb.append("\n  warnings=").append(warnings);
        sb.append("\n  nextSteps='").append(nextSteps).append("'");
        sb.append("\n  vetContactRecommended=").append(vetContactRecommended);
        sb.append("\n  vetContactTimeframe='").append(vetContactTimeframe).append("'");
        sb.append("\n  vetContactReason='").append(vetContactReason).append("'");
        sb.append("\n}");
        
        return sb.toString();
    }

    public static class ResponseMessage {
        @JsonProperty("type")
        private String type;

        @JsonProperty("content")
        private String content;

        @JsonProperty("emphasis")
        private String emphasis;

        @JsonProperty("delay")
        private int delay = 800; // default delay in milliseconds

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getEmphasis() {
            return emphasis;
        }

        public void setEmphasis(String emphasis) {
            this.emphasis = emphasis;
        }

        public int getDelay() {
            return delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }

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

    public static class ResponseList {
        @JsonProperty("title")
        private String title;

        @JsonProperty("type")
        private String type;

        @JsonProperty("items")
        private List<String> items = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }

        @Override
        public String toString() {
            return "ResponseList{" +
                   "title='" + title + "', " +
                   "type='" + type + "', " +
                   "items=" + items +
                   "}";
        }
    }

    public static class Recommendation {
        @JsonProperty("action")
        private String action;

        @JsonProperty("timeframe")
        private String timeframe;

        @JsonProperty("priority")
        private String priority;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getTimeframe() {
            return timeframe;
        }

        public void setTimeframe(String timeframe) {
            this.timeframe = timeframe;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

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