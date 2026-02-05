package com.rental.backend.repository;

import com.rental.backend.model.Offer;
import com.rental.backend.model.OfferTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    // Offres actives par cible
    List<Offer> findByActiveAndTargetOrderByCreatedAtDesc(Boolean active, OfferTarget target);
    
    // Offres actives non expirées
    @Query("SELECT o FROM Offer o WHERE o.active = true AND (o.expiresAt IS NULL OR o.expiresAt > :now) ORDER BY o.createdAt DESC")
    List<Offer> findActiveOffers(LocalDateTime now);
    
    // Offres par cible non expirées
    @Query("SELECT o FROM Offer o WHERE o.active = true AND o.target = :target AND (o.expiresAt IS NULL OR o.expiresAt > :now) ORDER BY o.createdAt DESC")
    List<Offer> findActiveOffersByTarget(OfferTarget target, LocalDateTime now);
    
    // Offres créées par un admin
    List<Offer> findByCreatedByAdminIdOrderByCreatedAtDesc(Long adminId);
    
    // Toutes les offres triées par date
    List<Offer> findAllByOrderByCreatedAtDesc();
}
