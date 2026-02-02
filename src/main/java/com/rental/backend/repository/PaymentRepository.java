package com.rental.backend.repository;

import com.rental.backend.model.Payment;
import com.rental.backend.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Paiements d'un utilisateur
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Paiement d'une réservation spécifique
    Optional<Payment> findByBookingId(Long bookingId);
    
    // Paiements par statut
    List<Payment> findByStatus(PaymentStatus status);
    
    // Recherche par référence de transaction
    Optional<Payment> findByTransactionReference(String transactionReference);
}
