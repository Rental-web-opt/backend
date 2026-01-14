package com.rental.backend.service;

import com.rental.backend.model.Booking;
import com.rental.backend.model.BookingStatus;
import com.rental.backend.model.Car;
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration; // Important pour les heures
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private CarRepository carRepository;

    public Booking createBooking(Booking booking) {
        Car car = carRepository.findById(booking.getCar().getId())
            .orElseThrow(() -> new RuntimeException("Voiture introuvable"));

        double price = 0.0;

        // LOGIQUE PAR HEURE OU PAR JOUR
        if ("HOURLY".equalsIgnoreCase(booking.getRentalType())) {
            // Calcul en heures
            long hours = Duration.between(booking.getStartDate(), booking.getEndDate()).toHours();
            if (hours < 1) hours = 1;
            
            Double hourlyPrice = car.getPricePerHour() != null ? car.getPricePerHour() : (car.getPricePerDay() / 10); // Fallback si pas de prix heure
            price = hourlyPrice * hours;
            
        } else {
            // Calcul par défaut (JOUR)
            long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
            if (days < 1) days = 1;
            
            Double dailyPrice = car.getPricePerDay() != null ? car.getPricePerDay() : 0.0;
            price = dailyPrice * days;
        }

        // Ajout Chauffeur (Si demandé)
        if (booking.isWithDriver()) {
            long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
            if (days < 1) days = 1;
            price += (15000 * days);
        }

        booking.setTotalPrice(price);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCar(car);

        return bookingRepository.save(booking);
    }
    
    // ... autres méthodes (getAllBookings, getUserBookings) inchangées
    public List<Booking> getAllBookings() { return bookingRepository.findAll(); }
    public List<Booking> getUserBookings(Long userId) { return bookingRepository.findByUserId(userId); }
}