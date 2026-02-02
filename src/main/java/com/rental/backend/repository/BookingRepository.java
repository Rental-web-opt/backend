package com.rental.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rental.backend.model.Booking;
import com.rental.backend.model.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 1. Récupérer toutes les réservations d'un utilisateur (pour son historique)
    List<Booking> findByUserId(Long userId);

    // 2. Récupérer les réservations par statut
    List<Booking> findByStatus(BookingStatus status);

    // 3. Récupérer les réservations d'une voiture spécifique
    List<Booking> findByCarId(Long carId);

    // 4. Vérifier les conflits de réservation (chevauchement de dates)
    // Retourne les réservations qui se chevauchent avec la période demandée
    // Exclut les réservations annulées
    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId " +
           "AND b.status != 'CANCELLED' " +
           "AND ((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Booking> findConflictingBookings(
        @Param("carId") Long carId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // 5. Récupérer les réservations à venir pour une voiture
    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND b.endDate >= :now " +
           "ORDER BY b.startDate ASC")
    List<Booking> findUpcomingBookingsForCar(
        @Param("carId") Long carId,
        @Param("now") LocalDateTime now
    );
}