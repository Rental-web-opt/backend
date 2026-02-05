package com.rental.backend.service;

import com.rental.backend.model.*;
import com.rental.backend.repository.*;
import com.rental.backend.kafka.KafkaProducerService;
import com.rental.backend.controller.NotificationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private KafkaProducerService kafkaProducerService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationController notificationController;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Vérifie si une voiture est disponible pendant une période donnée
     */
    public boolean isCarAvailable(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
            carId, startDate, endDate
        );
        return conflictingBookings.isEmpty();
    }

    /**
     * Récupère les réservations qui occupent une voiture pendant une période
     */
    public List<Booking> getConflictingBookings(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findConflictingBookings(carId, startDate, endDate);
    }

    /**
     * Crée une réservation avec vérification de disponibilité
     * Statut initial: PENDING (en attente de paiement)
     * Notifie l'admin et l'agence concernée
     */
    @Transactional
    public Booking createBooking(Booking booking) {
        Car car = carRepository.findById(booking.getCar().getId())
            .orElseThrow(() -> new RuntimeException("Voiture introuvable"));

        // Vérification de la disponibilité
        if (!isCarAvailable(car.getId(), booking.getStartDate(), booking.getEndDate())) {
            throw new RuntimeException("Ce véhicule n'est pas disponible pour la période sélectionnée");
        }

        // Calcul du prix
        double price = calculatePrice(car, booking);

        booking.setTotalPrice(price);
        booking.setStatus(BookingStatus.PENDING); // En attente de paiement
        booking.setCar(car);

        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("📅 Nouvelle réservation créée (PENDING): #" + savedBooking.getId());
        
        // === NOTIFICATIONS ===
        sendNewBookingNotifications(savedBooking, car);
        
        return savedBooking;
    }

    /**
     * Envoie les notifications lors d'une nouvelle réservation
     * PERSISTANCE DIRECTE + SSE + Kafka (optionnel pour broadcast)
     */
    private void sendNewBookingNotifications(Booking booking, Car car) {
        try {
            // Récupérer les informations de l'utilisateur
            String userName = "Utilisateur";
            Optional<User> userOpt = userRepository.findById(booking.getUserId());
            if (userOpt.isPresent()) {
                userName = userOpt.get().getFullName();
            }

            String vehicleName = car.getBrand() + " " + car.getModel();
            String startDate = booking.getStartDate().format(DATE_FORMATTER);
            String endDate = booking.getEndDate().format(DATE_FORMATTER);
            String agencyName = car.getAgency() != null ? car.getAgency().getName() : "Agence";

            // === 1. Notifier le CLIENT (celui qui a fait la réservation) ===
            String clientMessage = String.format(
                "Votre réservation du véhicule %s du %s au %s est en attente de paiement. Montant: %,.0f CFA",
                vehicleName, startDate, endDate, booking.getTotalPrice()
            );
            Notification clientNotif = notificationService.createNotification(
                booking.getUserId(),
                "📦 Réservation créée",
                clientMessage,
                NotificationType.BOOKING
            );
            notificationController.sendNotification(booking.getUserId(), "booking", clientMessage);
            System.out.println("💾 Notification client persistée pour user " + booking.getUserId());

            // === 2. Notifier tous les ADMINS ===
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                String adminMessage = String.format(
                    "Nouvelle réservation: %s a réservé %s (%s) pour %,.0f CFA",
                    userName, vehicleName, agencyName, booking.getTotalPrice()
                );
                notificationService.createNotification(
                    admin.getId(),
                    "📦 Nouvelle réservation",
                    adminMessage,
                    NotificationType.BOOKING
                );
                notificationController.sendNotification(admin.getId(), "booking", adminMessage);
                System.out.println("💾 Notification admin persistée pour admin " + admin.getId());
            }

            // === 3. Notifier l'AGENCE concernée (si elle a un userId) ===
            if (car.getAgency() != null && car.getAgency().getUserId() != null) {
                Long agencyUserId = car.getAgency().getUserId();
                String agencyMessage = String.format(
                    "%s a réservé votre véhicule %s du %s au %s. Montant: %,.0f CFA",
                    userName, vehicleName, startDate, endDate, booking.getTotalPrice()
                );
                notificationService.createNotification(
                    agencyUserId,
                    "🚗 Nouvelle réservation sur votre véhicule",
                    agencyMessage,
                    NotificationType.BOOKING
                );
                notificationController.sendNotification(agencyUserId, "booking", agencyMessage);
                System.out.println("💾 Notification agence persistée pour userId " + agencyUserId);
            }

            System.out.println("✅ Notifications de nouvelle réservation envoyées et persistées");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de l'envoi des notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Confirme une réservation après paiement
     * Notifie l'utilisateur avec PERSISTANCE DIRECTE
     */
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Cette réservation ne peut pas être confirmée (statut actuel: " + booking.getStatus() + ")");
        }
        
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        System.out.println("✅ Réservation #" + bookingId + " CONFIRMÉE");
        
        // === NOTIFICATION UTILISATEUR AVEC PERSISTANCE ===
        try {
            Car car = booking.getCar();
            String vehicleName = car.getBrand() + " " + car.getModel();
            String startDate = booking.getStartDate().format(DATE_FORMATTER);
            String endDate = booking.getEndDate().format(DATE_FORMATTER);
            
            String message = String.format(
                "Votre réservation du véhicule %s du %s au %s est confirmée. Montant: %,.0f CFA",
                vehicleName, startDate, endDate, booking.getTotalPrice()
            );
            
            notificationService.createNotification(
                booking.getUserId(),
                "🎉 Réservation confirmée !",
                message,
                NotificationType.SUCCESS
            );
            notificationController.sendNotification(booking.getUserId(), "booking_confirmed", message);
            System.out.println("💾 Notification confirmation persistée pour user " + booking.getUserId());
        } catch (Exception e) {
            System.err.println("⚠️ Erreur notification confirmation: " + e.getMessage());
        }
        
        return saved;
    }

    /**
     * Annule une réservation par l'utilisateur
     * L'utilisateur ne peut annuler que SES propres réservations qui sont PENDING ou CONFIRMED
     * PERSISTANCE DIRECTE des notifications
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));
        
        // Vérifier que l'utilisateur est bien le propriétaire de la réservation
        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez pas annuler une réservation qui ne vous appartient pas");
        }
        
        // Vérifier que la réservation peut être annulée
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Une réservation terminée ne peut pas être annulée");
        }
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Cette réservation est déjà annulée");
        }
        
        // Vérifier si la réservation a déjà commencé
        if (booking.getStartDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Impossible d'annuler une réservation déjà commencée");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        System.out.println("❌ Réservation #" + bookingId + " ANNULÉE par l'utilisateur #" + userId);
        
        // === NOTIFICATIONS AVEC PERSISTANCE ===
        try {
            Car car = booking.getCar();
            String vehicleName = car.getBrand() + " " + car.getModel();
            User user = userRepository.findById(userId).orElse(null);
            String userName = user != null ? user.getFullName() : "Utilisateur #" + userId;
            String agencyName = car.getAgency() != null ? car.getAgency().getName() : "Agence";
            
            // 1. Notification à l'utilisateur (qui a annulé)
            String clientMessage = String.format("Votre réservation du véhicule %s a été annulée avec succès.", vehicleName);
            notificationService.createNotification(userId, "❌ Réservation annulée", clientMessage, NotificationType.ERROR);
            notificationController.sendNotification(userId, "booking_cancelled", clientMessage);
            System.out.println("💾 Notification annulation client persistée");
            
            // 2. Notification à tous les admins
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                String adminMessage = String.format("Annulation: %s a annulé sa réservation du %s (%s). Montant: %,.0f CFA",
                    userName, vehicleName, agencyName, booking.getTotalPrice());
                notificationService.createNotification(admin.getId(), "❌ Réservation annulée", adminMessage, NotificationType.ERROR);
                notificationController.sendNotification(admin.getId(), "booking_cancelled", adminMessage);
            }
            
            // 3. Notification à l'agence
            Agency agency = car.getAgency();
            if (agency != null && agency.getUserId() != null) {
                String agencyMessage = String.format("%s a annulé sa réservation du %s. Montant: %,.0f CFA",
                    userName, vehicleName, booking.getTotalPrice());
                notificationService.createNotification(agency.getUserId(), "❌ Réservation annulée", agencyMessage, NotificationType.ERROR);
                notificationController.sendNotification(agency.getUserId(), "booking_cancelled", agencyMessage);
                System.out.println("💾 Notification annulation agence persistée");
            }
            
            System.out.println("✅ Notifications d'annulation envoyées et persistées");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur notification annulation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return saved;
    }

    /**
     * Tâche planifiée: Passe automatiquement les réservations CONFIRMED à COMPLETED
     * quand la date de fin est passée
     * S'exécute toutes les 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void autoCompleteBookings() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);
        
        int completed = 0;
        for (Booking booking : confirmedBookings) {
            if (booking.getEndDate().isBefore(now)) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
                completed++;
                System.out.println("🏁 Réservation #" + booking.getId() + " automatiquement TERMINÉE");
                
                // === NOTIFICATION UTILISATEUR ===
                try {
                    Car car = booking.getCar();
                    String vehicleName = car.getBrand() + " " + car.getModel();
                    kafkaProducerService.notifyBookingCompleted(booking.getUserId(), vehicleName);
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur notification completion: " + e.getMessage());
                }
            }
        }
        
        if (completed > 0) {
            System.out.println("🔄 Auto-complete: " + completed + " réservation(s) terminée(s)");
        }
    }

    /**
     * Calcul du prix basé sur les règles métier:
     * - HOURLY: Pas de réduction, prix à l'heure
     * - DAILY: Réduction sur les jours complets uniquement (les heures restantes ne comptent pas pour la réduction)
     * - MONTHLY: Réduction sur les mois complets uniquement
     * 
     * Exemple Daily: Du 7-02 8h au 10-02 22h = 86h total
     * - Jours complets: 3 jours (72h) → appliqué la réduction
     * - Heures restantes: 14h → facturées au tarif horaire sans réduction
     */
    private double calculatePrice(Car car, Booking booking) {
        double price = 0.0;
        String rentalType = booking.getRentalType();

        // Calcul de la durée exacte en heures
        long totalHours = Duration.between(booking.getStartDate(), booking.getEndDate()).toHours();
        if (totalHours < 1) totalHours = 1;

        // Prix de base
        double dailyPrice = car.getPricePerDay() != null ? car.getPricePerDay() : 0.0;
        double hourlyPrice = car.getPricePerHour() != null ? car.getPricePerHour() : (dailyPrice / 10);
        double monthlyPrice = car.getMonthlyPrice() != null ? car.getMonthlyPrice() : (dailyPrice * 30 * 0.8);

        if ("HOURLY".equalsIgnoreCase(rentalType)) {
            // === CALCUL PAR HEURE (Aucune réduction) ===
            price = hourlyPrice * totalHours;
            
        } else if ("MONTHLY".equalsIgnoreCase(rentalType)) {
            // === CALCUL PAR MOIS ===
            // Réduction uniquement sur les mois complets
            long totalDays = totalHours / 24; // Jours complets
            long remainingHours = totalHours % 24; // Heures restantes
            
            long months = totalDays / 30;
            long remainingDays = totalDays % 30;
            
            // Prix des mois complets (avec réduction)
            price = months * monthlyPrice;
            
            // Prix des jours restants (avec réduction si >= 7 jours)
            if (remainingDays >= 7) {
                price += remainingDays * dailyPrice * 0.90; // 10% de réduction
            } else if (remainingDays > 0) {
                price += remainingDays * dailyPrice; // Pas de réduction
            }
            
            // Prix des heures restantes (sans réduction)
            price += remainingHours * hourlyPrice;
            
        } else {
            // === CALCUL PAR JOUR (défaut) ===
            // Réduction uniquement sur les jours complets, pas les heures restantes
            long fullDays = totalHours / 24; // Jours complets (ex: 72h = 3 jours)
            long remainingHours = totalHours % 24; // Heures restantes (ex: 14h)
            
            // Si aucun jour complet mais des heures, compter au minimum 1 jour
            if (fullDays == 0 && remainingHours > 0) {
                fullDays = 1;
                remainingHours = 0;
            }
            
            double dayPrice = fullDays * dailyPrice;
            
            // Réductions progressives sur les jours complets
            if (fullDays >= 30) {
                dayPrice = dayPrice * 0.85; // 15% de réduction
            } else if (fullDays >= 14) {
                dayPrice = dayPrice * 0.90; // 10% de réduction
            } else if (fullDays >= 7) {
                dayPrice = dayPrice * 0.95; // 5% de réduction
            }
            // Moins de 7 jours: pas de réduction
            
            price = dayPrice;
            
            // Les heures restantes sont facturées AU TARIF HORAIRE SANS RÉDUCTION
            if (remainingHours > 0) {
                price += remainingHours * hourlyPrice;
            }
        }

        // === SUPPLÉMENT CHAUFFEUR ===
        if (booking.isWithDriver()) {
            // Calculer le nombre de jours pour le chauffeur
            long daysForDriver = (long) Math.ceil(totalHours / 24.0);
            if (daysForDriver < 1) daysForDriver = 1;
            price += (15000 * daysForDriver); // 15 000 CFA par jour
        }

        System.out.println("💰 Calcul prix: " + totalHours + "h → " + price + " CFA (" + rentalType + ")");
        return price;
    }
    
    public List<Booking> getAllBookings() { 
        return bookingRepository.findAll(); 
    }
    
    public List<Booking> getUserBookings(Long userId) { 
        return bookingRepository.findByUserId(userId); 
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    /**
     * Récupère les réservations par statut
     */
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }
}