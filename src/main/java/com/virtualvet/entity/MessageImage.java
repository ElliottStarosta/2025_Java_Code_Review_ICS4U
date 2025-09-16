package com.virtualvet.entity;

import jakarta.persistence.*;

/**
 * JPA Entity representing an image attachment to a message in the Virtual Vet application.
 * 
 * This entity stores information about images attached to messages during veterinary
 * consultations. It maintains the relationship between images and their parent messages,
 * tracks image ordering for multiple images, and stores analysis results from AI
 * image processing for veterinary assessment purposes.
 * 
 * The entity uses JPA annotations for database mapping and supports multiple images
 * per message with ordering capabilities. It stores analysis results as JSON text
 * for flexible data storage and retrieval of AI-generated veterinary insights.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Entity
@Table(name = "message_images")
public class MessageImage {

    /**
     * Unique identifier for this message image.
     * Generated automatically by the database using identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The message this image is attached to.
     * Establishes a many-to-one relationship where multiple images
     * can be attached to a single message for comprehensive visual consultations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    /**
     * URL or file path to the stored image.
     * Points to the actual image file location for retrieval and display
     * in the chat interface during veterinary consultations.
     */
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /**
     * Order of this image among multiple images attached to the same message.
     * Used for maintaining proper sequence when displaying multiple images
     * in the correct order for veterinary assessment.
     */
    @Column(name = "image_order")
    private Integer imageOrder;

    /**
     * JSON string containing the analysis results from AI image processing.
     * Stores structured data about the image analysis including detected conditions,
     * confidence levels, urgency assessments, and veterinary insights.
     */
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult; // JSON string of AnalysisResult

    /**
     * Default constructor required by JPA.
     * Creates a new MessageImage instance without any preset values.
     */
    public MessageImage() {
    }

    /**
     * Constructor that creates a message image with the specified message and image URL.
     * 
     * @param message the message this image is attached to
     * @param imageUrl the URL or path to the image file
     */
    public MessageImage(Message message, String imageUrl) {
        this.message = message;
        this.imageUrl = imageUrl;
    }

    /**
     * Constructor that creates a message image with all specified parameters.
     * 
     * @param message the message this image is attached to
     * @param imageUrl the URL or path to the image file
     * @param imageOrder the order of this image among multiple images
     * @param analysisResult the JSON string containing analysis results
     */
    public MessageImage(Message message, String imageUrl, Integer imageOrder, String analysisResult) {
        this.message = message;
        this.imageUrl = imageUrl;
        this.imageOrder = imageOrder;
        this.analysisResult = analysisResult;
    }

    /**
     * Gets the unique identifier for this message image.
     * 
     * @return the image ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this message image.
     * 
     * @param id the image ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the message this image is attached to.
     * 
     * @return the message entity this image belongs to
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Sets the message for this image.
     * 
     * @param message the message entity to associate with this image
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Gets the URL or file path to the stored image.
     * 
     * @return the image URL or file path
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the URL or file path to the stored image.
     * 
     * @param imageUrl the image URL or file path to set
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gets the order of this image among multiple images attached to the same message.
     * 
     * @return the image order number, or null if not specified
     */
    public Integer getImageOrder() {
        return imageOrder;
    }

    /**
     * Sets the order of this image among multiple images attached to the same message.
     * 
     * @param imageOrder the image order number to set
     */
    public void setImageOrder(Integer imageOrder) {
        this.imageOrder = imageOrder;
    }

    /**
     * Gets the JSON string containing analysis results from AI image processing.
     * 
     * @return the analysis result JSON string, or null if no analysis has been performed
     */
    public String getAnalysisResult() {
        return analysisResult;
    }

    /**
     * Sets the JSON string containing analysis results from AI image processing.
     * 
     * @param analysisResult the analysis result JSON string to set
     */
    public void setAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }

    /**
     * Generates a string representation of this message image.
     * 
     * Creates a formatted string containing all image information including
     * the image URL, order, and analysis result status. Uses safe null checking
     * to prevent null pointer exceptions in the toString() output.
     * 
     * @return a formatted string representation of the message image
     */
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