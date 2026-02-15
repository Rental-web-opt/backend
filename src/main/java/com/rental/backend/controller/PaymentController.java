package com.rental.backend.controller;

import com.rental.backend.model.*;
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.repository.PaymentRepository;
import com.rental.backend.repository.UserRepository;
import com.rental.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationController notificationController;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Récupérer les paiements d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getUserPayments(@PathVariable Long userId) {
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Alias pour "my-payments"
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
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            Double amount = Double.valueOf(request.get("amount").toString());
            String currency = (String) request.getOrDefault("currency", "xaf");

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
            
            // === ENVOYER LES NOTIFICATIONS ===
            sendPaymentConfirmationNotifications(booking, payment);
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

        // === ENVOYER LES NOTIFICATIONS ===
        sendPaymentConfirmationNotifications(booking, payment);

        return ResponseEntity.ok(Map.of(
            "message", "Paiement confirmé et réservation validée",
            "payment", payment,
            "booking", booking
        ));
    }

    /**
     * Envoie les notifications après confirmation d'un paiement.
     * Notifie: 1) Le client, 2) Tous les admins, 3) L'agence propriétaire du véhicule
     */
    private void sendPaymentConfirmationNotifications(Booking booking, Payment payment) {
        try {
            Car car = booking.getCar();
            String vehicleName = car.getBrand() + " " + car.getModel();
            String startDate = booking.getStartDate().format(DATE_FORMATTER);
            String endDate = booking.getEndDate().format(DATE_FORMATTER);
            String agencyName = car.getAgency() != null ? car.getAgency().getName() : "Agence";

            // Récupérer le nom de l'utilisateur
            String userName = "Utilisateur";
            var userOpt = userRepository.findById(booking.getUserId());
            if (userOpt.isPresent()) {
                userName = userOpt.get().getFullName();
            }

            // === 1. Notifier le CLIENT ===
            String clientMessage = String.format(
                "Paiement de %,.0f CFA confirmé ! Votre réservation du %s du %s au %s est validée. Bonne route !",
                payment.getAmount(), vehicleName, startDate, endDate
            );
            notificationService.createNotification(
                booking.getUserId(),
                "✅ Paiement confirmé & Réservation validée",
                clientMessage,
                NotificationType.SUCCESS
            );
            notificationController.sendNotification(booking.getUserId(), "payment_confirmed", clientMessage);
            System.out.println("💾 Notification paiement client persistée pour user " + booking.getUserId());

            // === 2. Notifier tous les ADMINS ===
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                String adminMessage = String.format(
                    "💰 Paiement reçu: %s a payé %,.0f CFA pour %s (%s). Réservation confirmée.",
                    userName, payment.getAmount(), vehicleName, agencyName
                );
                notificationService.createNotification(
                    admin.getId(),
                    "💰 Paiement reçu",
                    adminMessage,
                    NotificationType.PAYMENT
                );
                notificationController.sendNotification(admin.getId(), "payment_confirmed", adminMessage);
                System.out.println("💾 Notification paiement admin persistée pour admin " + admin.getId());
            }

            // === 3. Notifier l'AGENCE propriétaire du véhicule ===
            if (car.getAgency() != null && car.getAgency().getUserId() != null) {
                Long agencyUserId = car.getAgency().getUserId();
                String agencyMessage = String.format(
                    "💰 Paiement reçu ! %s a payé %,.0f CFA pour votre véhicule %s (du %s au %s). Préparez le véhicule !",
                    userName, payment.getAmount(), vehicleName, startDate, endDate
                );
                notificationService.createNotification(
                    agencyUserId,
                    "💰 Paiement reçu pour votre véhicule",
                    agencyMessage,
                    NotificationType.PAYMENT
                );
                notificationController.sendNotification(agencyUserId, "payment_confirmed", agencyMessage);
                System.out.println("💾 Notification paiement agence persistée pour userId " + agencyUserId);
            }

            System.out.println("✅ Toutes les notifications de paiement envoyées et persistées");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur envoi notifications paiement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Endpoint legacy pour compatibilité
     */
    @GetMapping("/methods")
    public ResponseEntity<?> getSavedMethods() {
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
