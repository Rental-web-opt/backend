package com.rental.backend.service;

import com.rental.backend.model.Booking;
import com.rental.backend.model.BookingStatus;
import com.rental.backend.model.Car;
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private CarRepository carRepository;

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
     */
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
        booking.setStatus(BookingStatus.PENDING);
        booking.setCar(car);

        return bookingRepository.save(booking);
    }

    /**
     * Calcul du prix basé sur la durée exacte en heures
     */
    private double calculatePrice(Car car, Booking booking) {
        double price = 0.0;
        String rentalType = booking.getRentalType();

        // Calcul de la durée exacte en heures
        long totalHours = Duration.between(booking.getStartDate(), booking.getEndDate()).toHours();
        if (totalHours < 1) totalHours = 1;

        double dailyPrice = car.getPricePerDay() != null ? car.getPricePerDay() : 0.0;
        double hourlyPrice = car.getPricePerHour() != null ? car.getPricePerHour() : (dailyPrice / 10);
        double monthlyPrice = car.getMonthlyPrice() != null ? car.getMonthlyPrice() : (dailyPrice * 30 * 0.8);

        if ("HOURLY".equalsIgnoreCase(rentalType)) {
            // === CALCUL PAR HEURE ===
            price = hourlyPrice * totalHours;
            
        } else if ("MONTHLY".equalsIgnoreCase(rentalType)) {
            // === CALCUL PAR MOIS ===
            // Convertir les heures en jours (arrondi supérieur)
            long totalDays = (long) Math.ceil(totalHours / 24.0);
            
            long months = totalDays / 30;
            long remainingDays = totalDays % 30;
            
            if (months >= 1) {
                // Prix = mois complets + jours restants avec réduction
                price = (months * monthlyPrice) + (remainingDays * dailyPrice * 0.9);
            } else {
                // Moins d'un mois : tarif journalier avec petite réduction
                price = dailyPrice * totalDays * 0.95;
            }
            
        } else {
            // === CALCUL PAR JOUR (défaut) ===
            // Calcul précis : nombre d'heures / 24, arrondi supérieur
            long totalDays = (long) Math.ceil(totalHours / 24.0);
            if (totalDays < 1) totalDays = 1;
            
            price = dailyPrice * totalDays;
            
            // Réductions progressives pour longue durée
            if (totalDays >= 7 && totalDays < 14) {
                price = price * 0.95; // 5% de réduction
            } else if (totalDays >= 14 && totalDays < 30) {
                price = price * 0.90; // 10% de réduction
            } else if (totalDays >= 30) {
                price = price * 0.85; // 15% de réduction
            }
        }

        // === SUPPLÉMENT CHAUFFEUR ===
        if (booking.isWithDriver()) {
            // Calculer le nombre de jours pour le chauffeur
            long daysForDriver = (long) Math.ceil(totalHours / 24.0);
            if (daysForDriver < 1) daysForDriver = 1;
            price += (15000 * daysForDriver); // 15 000 CFA par jour
        }

        return price;
    }
    
    public List<Booking> getAllBookings() { 
        return bookingRepository.findAll(); 
    }
    
    public List<Booking> getUserBookings(Long userId) { 
        return bookingRepository.findByUserId(userId); 
    }
}