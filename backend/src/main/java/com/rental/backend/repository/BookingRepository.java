package com.rental.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rental.backend.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 1. Récupérer toutes les réservations d'un utilisateur (pour son historique)
    List<Booking> findByUserId(Long userId);

    // 2. Récupérer les réservations par statut (ex: "PENDING" pour les admins)
    List<Booking> findByStatus(String status);

    // 3. Récupérer les réservations d'une voiture spécifique (pour vérifier la disponibilité)
    List<Booking> findByCarId(Long carId);
}