package com.rental.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rental.backend.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Récupérer les avis par Cible
    List<Review> findByCarId(Long carId);
    List<Review> findByDriverId(Long driverId);
    List<Review> findByAgencyId(Long agencyId);
    
    // Récupérer les avis d'un utilisateur spécifique (Mon historique d'avis)
    List<Review> findByUserId(Long userId);
}