package com.rental.backend.repository;

import com.rental.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Récupérer toutes les notifications d'un utilisateur (ordonnées par date décroissante)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Récupérer les notifications non lues d'un utilisateur
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    
    // Compter les notifications non lues
    long countByUserIdAndReadFalse(Long userId);
    
    // Marquer toutes les notifications d'un utilisateur comme lues
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);
    
    // Supprimer toutes les notifications d'un utilisateur
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
