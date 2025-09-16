package com.virtualvet.repository;

import com.virtualvet.entity.AnimalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing AnimalProfile entities in the Virtual Vet application.
 * 
 * This repository provides data access methods for animal profile operations including
 * CRUD operations inherited from JpaRepository, as well as custom query methods for
 * finding profiles by conversation, session, and animal type. It supports both simple
 * property-based queries and complex JPQL queries for advanced data retrieval.
 * 
 * The repository uses Spring Data JPA conventions for automatic query generation
 * and provides type-safe query methods with proper parameter binding. It includes
 * methods for finding profiles by various criteria and supports bulk operations
 * for data management and cleanup.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@Repository
public interface AnimalProfileRepository extends JpaRepository<AnimalProfile, Long> {
    
    /**
     * Finds all animal profiles associated with a specific conversation.
     * 
     * Retrieves all animal profiles that belong to the conversation with the
     * specified ID. This method is useful for getting all pet information
     * associated with a particular consultation session.
     * 
     * @param conversationId the ID of the conversation to search for
     * @return a list of AnimalProfile entities associated with the conversation
     */
    List<AnimalProfile> findByConversationId(Long conversationId);
    
    /**
     * Finds an animal profile by the session ID of its associated conversation.
     * 
     * Retrieves the animal profile that belongs to a conversation with the
     * specified session ID. This method is commonly used when working with
     * session-based operations and need to access the pet's profile information.
     * 
     * @param sessionId the session ID of the conversation to search for
     * @return an Optional containing the AnimalProfile if found, empty otherwise
     */
    Optional<AnimalProfile> findByConversationSessionId(String sessionId);
    
    /**
     * Finds an animal profile by session ID using a custom JPQL query.
     * 
     * This method provides an alternative implementation for finding profiles
     * by session ID using an explicit JPQL query. It joins with the conversation
     * entity to access the session ID field directly.
     * 
     * @param sessionId the session ID to search for
     * @return an Optional containing the AnimalProfile if found, empty otherwise
     */
    @Query("SELECT ap FROM AnimalProfile ap WHERE ap.conversation.sessionId = :sessionId")
    Optional<AnimalProfile> findBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * Finds all animal profiles of a specific animal type.
     * 
     * Retrieves all animal profiles that match the specified animal type
     * (e.g., "Dog", "Cat", "Bird"). This method is useful for analytics,
     * reporting, and finding similar cases for veterinary reference.
     * 
     * @param animalType the animal type to search for (e.g., "Dog", "Cat")
     * @return a list of AnimalProfile entities matching the animal type
     */
    @Query("SELECT ap FROM AnimalProfile ap WHERE ap.animalType = :animalType")
    List<AnimalProfile> findByAnimalType(@Param("animalType") String animalType);
    
    /**
     * Retrieves all distinct animal types from existing profiles.
     * 
     * Gets a list of all unique animal types that have been recorded in
     * the system. This method is useful for populating dropdown lists,
     * generating statistics, and understanding the types of animals
     * using the veterinary service.
     * 
     * @return a list of distinct animal type strings, excluding null values
     */
    @Query("SELECT DISTINCT ap.animalType FROM AnimalProfile ap WHERE ap.animalType IS NOT NULL")
    List<String> findDistinctAnimalTypes();
    
    /**
     * Deletes all animal profiles associated with a specific conversation.
     * 
     * Removes all animal profiles that belong to the conversation with the
     * specified ID. This method is typically used during conversation cleanup
     * or when a conversation and its associated data need to be removed.
     * 
     * @param conversationId the ID of the conversation whose profiles should be deleted
     */
    void deleteByConversationId(Long conversationId);
}