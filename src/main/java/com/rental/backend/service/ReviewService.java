package com.rental.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rental.backend.model.Review;
import com.rental.backend.repository.ReviewRepository;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    // Ajouter un avis
    public Review createReview(Review review) {
        // On fixe la date à aujourd'hui si elle n'est pas envoyée
        if (review.getDate() == null) {
            review.setDate(LocalDate.now());
        }
        return reviewRepository.save(review);
    }

    // Récupérer les avis selon le type
    public List<Review> getReviewsByCar(Long carId) {
        return reviewRepository.findByCarId(carId);
    }

    public List<Review> getReviewsByDriver(Long driverId) {
        return reviewRepository.findByDriverId(driverId);
    }

    public List<Review> getReviewsByAgency(Long agencyId) {
        return reviewRepository.findByAgencyId(agencyId);
    }
    
    // Supprimer un avis (ex: modération)
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }
}