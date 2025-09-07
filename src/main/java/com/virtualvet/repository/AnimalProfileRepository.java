package com.virtualvet.repository;

import com.virtualvet.entity.AnimalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalProfileRepository extends JpaRepository<AnimalProfile, Long> {
    
    List<AnimalProfile> findByConversationId(Long conversationId);
    
    Optional<AnimalProfile> findByConversationSessionId(String sessionId);
    
    @Query("SELECT ap FROM AnimalProfile ap WHERE ap.conversation.sessionId = :sessionId")
    Optional<AnimalProfile> findBySessionId(@Param("sessionId") String sessionId);
    
    @Query("SELECT ap FROM AnimalProfile ap WHERE ap.animalType = :animalType")
    List<AnimalProfile> findByAnimalType(@Param("animalType") String animalType);
    
    @Query("SELECT DISTINCT ap.animalType FROM AnimalProfile ap WHERE ap.animalType IS NOT NULL")
    List<String> findDistinctAnimalTypes();
    
    void deleteByConversationId(Long conversationId);
}