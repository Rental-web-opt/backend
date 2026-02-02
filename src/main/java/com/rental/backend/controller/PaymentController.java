package com.rental.backend.controller;

import com.rental.backend.model.*;
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Récupérer les paiements d'un utilisateur
     * GET /api/payments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getUserPayments(@PathVariable Long userId) {
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Alias pour "my-payments" - utilise le paramètre userId
     * GET /api/payments/my-payments?userId=1
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<Payment>> getMyPayments(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Créer un paiement pour une réservation
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> request) {
        try {
            Long bookingId = Long.valueOf(request.get("bookingId").toString());
            String paymentMethod = (String) request.getOrDefault("paymentMethod", "CARD");
            
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                return ResponseEntity.badRequest().body(Map.of("error", true, "message", "Réservation introuvable"));
            }

            // Vérifier si un paiement existe déjà
            if (paymentRepository.findByBookingId(bookingId).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", true, "message", "Un paiement existe déjà pour cette réservation"));
            }

            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setUserId(booking.getUserId());
            payment.setAmount(booking.getTotalPrice());
            payment.setCurrency("XAF");
            payment.setPaymentMethod(paymentMethod);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setDescription("Paiement pour " + booking.getCar().getName());
            payment.setTransactionReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payment.setCreatedAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);
            
            return ResponseEntity.ok(Map.of(
                "payment", saved,
                "message", "Paiement initié"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Simuler la création d'un PaymentIntent (pour Stripe)
     * POST /api/payment/create-payment-intent
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            Double amount = Double.valueOf(request.get("amount").toString());
            String currency = (String) request.getOrDefault("currency", "xaf");

            // En production, ici on appellerait Stripe
            // Pour la démo, on simule un clientSecret
            String fakeClientSecret = "pi_" + UUID.randomUUID().toString().replace("-", "") + "_secret_demo";

            return ResponseEntity.ok(Map.of(
                "clientSecret", fakeClientSecret,
                "amount", amount,
                "currency", currency
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Confirmer un paiement (marquer comme complété)
     * PUT /api/payments/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id).orElse(null);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Confirmer aussi la réservation associée
        Booking booking = payment.getBooking();
        if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        return ResponseEntity.ok(Map.of(
            "message", "Paiement confirmé",
            "payment", payment
        ));
    }

    /**
     * Confirmer un paiement par ID de réservation
     * PUT /api/payments/booking/{bookingId}/confirm
     */
    @PutMapping("/booking/{bookingId}/confirm")
    public ResponseEntity<?> confirmPaymentByBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, Object> request) {
        
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        // Vérifier ou créer le paiement
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseGet(() -> {
            Payment newPayment = new Payment();
            newPayment.setBooking(booking);
            newPayment.setUserId(booking.getUserId());
            newPayment.setAmount(booking.getTotalPrice());
            newPayment.setCurrency("XAF");
            newPayment.setPaymentMethod(request != null ? (String) request.getOrDefault("paymentMethod", "CARD") : "CARD");
            newPayment.setDescription("Paiement pour " + booking.getCar().getName());
            newPayment.setTransactionReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            newPayment.setCreatedAt(LocalDateTime.now());
            return newPayment;
        });

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Confirmer la réservation
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
            "message", "Paiement confirmé et réservation validée",
            "payment", payment,
            "booking", booking
        ));
    }

    /**
     * Endpoint legacy pour compatibilité
     */
    @GetMapping("/methods")
    public ResponseEntity<?> getSavedMethods() {
        // Pour la démo, retourner une liste vide
        return ResponseEntity.ok(List.of());
    }

    /**
     * Tous les paiements (admin)
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }
}
