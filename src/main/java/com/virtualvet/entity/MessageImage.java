package com.virtualvet.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "message_images")
public class MessageImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_order")
    private Integer imageOrder;

    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult; // JSON string of AnalysisResult

    // --- Constructors ---
    public MessageImage() {
    }

    public MessageImage(Message message, String imageUrl) {
        this.message = message;
        this.imageUrl = imageUrl;
    }

    public MessageImage(Message message, String imageUrl, Integer imageOrder, String analysisResult) {
        this.message = message;
        this.imageUrl = imageUrl;
        this.imageOrder = imageOrder;
        this.analysisResult = analysisResult;
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getImageOrder() {
        return imageOrder;
    }

    public void setImageOrder(Integer imageOrder) {
        this.imageOrder = imageOrder;
    }

    public String getAnalysisResult() {
        return analysisResult;
    }

    public void setAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }

    // --- Convenience ---
    @Override
    public String toString() {
        return "MessageImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageOrder=" + imageOrder +
                ", analysisResult='" + (analysisResult != null ? analysisResult : null) + '\'' +
                '}';
    }
}
