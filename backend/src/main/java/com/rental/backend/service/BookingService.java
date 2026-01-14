package com.rental.backend.service;

import com.rental.backend.dto.BookingRequest;
import com.rental.backend.model.Booking;
import com.rental.backend.model.BookingStatus;
import com.rental.backend.model.Car;
import com.rental.backend.model.User; // <--- IMPORT AJOUTÉ
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.repository.CarRepository;
import com.rental.backend.repository.UserRepository; // <--- IMPORT AJOUTÉ
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private UserRepository userRepository; // <--- INJECTION DU REPO USER

    public Booking createBooking(BookingRequest request) {
        // 1. Récupérer la voiture via l'ID
        Car car = carRepository.findById(request.getCarId())
            .orElseThrow(() -> new RuntimeException("Voiture introuvable"));

        // 2. Convertir les dates (String -> LocalDateTime)
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(request.getStartDate(), formatter);
        LocalDateTime end = LocalDateTime.parse(request.getEndDate(), formatter);

        // 3. Calculer le prix
        double price = 0.0;

        if ("MONTHLY".equalsIgnoreCase(request.getRentalType())) {
            long months = ChronoUnit.MONTHS.between(start, end);
            if (months < 1) months = 1;
            Double monthlyPrice = car.getMonthlyPrice() != null ? car.getMonthlyPrice() : (car.getPricePerDay() * 30);
            price = monthlyPrice * months;

        } else if ("HOURLY".equalsIgnoreCase(request.getRentalType())) {
            long hours = Duration.between(start, end).toHours();
            if (hours < 1) hours = 1;
            Double hourlyPrice = car.getPricePerHour() != null ? car.getPricePerHour() : (car.getPricePerDay() / 10);
            price = hourlyPrice * hours;

        } else {
            // Par défaut : DAILY
            long days = ChronoUnit.DAYS.between(start, end);
            if (days < 1) days = 1;
            Double dailyPrice = car.getPricePerDay() != null ? car.getPricePerDay() : 0.0;
            price = dailyPrice * days;
        }

        // 4. Créer l'objet Booking
        Booking booking = new Booking();
        booking.setCar(car);
        booking.setUserId(request.getUserId());
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setRentalType(request.getRentalType());
        booking.setWithDriver(request.isWithDriver());
        booking.setStatus(BookingStatus.PENDING);

        // --- GESTION DU CHAUFFEUR ---
        if (request.isWithDriver()) {
            // Calcul du prix chauffeur
            long days = ChronoUnit.DAYS.between(start, end);
            if (days < 1) days = 1;
            price += (15000 * days);

            // Si un ID de chauffeur est fourni, on l'associe
            if (request.getDriverId() != null) {
                User driver = userRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Chauffeur introuvable"));
                booking.setDriver(driver); // <--- ASSIGNATION DU CHAUFFEUR
            }
        }

        booking.setTotalPrice(price);

        return bookingRepository.save(booking);
    }

    public Booking updateBookingStatus(Long bookingId, String status) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        try {
            BookingStatus newStatus = BookingStatus.valueOf(status.toUpperCase());
            booking.setStatus(newStatus);
            return bookingRepository.save(booking);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + status);
        }
    }

    public List<Booking> getAllBookings() { return bookingRepository.findAll(); }
    public List<Booking> getUserBookings(Long userId) { return bookingRepository.findByUserId(userId); }
}