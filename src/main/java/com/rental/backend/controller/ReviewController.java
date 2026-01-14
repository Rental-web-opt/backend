package com.rental.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rental.backend.model.Review;
import com.rental.backend.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:3000") // Connexion avec Next.js
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // POST : Ajouter un commentaire
    // Le JSON attendu doit contenir l'ID de l'user et l'ID de l'objet noté (car, driver OU agency)
    @PostMapping
    public Review addReview(@RequestBody Review review) {
        return reviewService.createReview(review);
    }

    // GET : Avis d'une Voiture
    @GetMapping("/car/{id}")
    public List<Review> getCarReviews(@PathVariable Long id) {
        return reviewService.getReviewsByCar(id);
    }

    // GET : Avis d'un Chauffeur
    @GetMapping("/driver/{id}")
    public List<Review> getDriverReviews(@PathVariable Long id) {
        return reviewService.getReviewsByDriver(id);
    }

    // GET : Avis d'une Agence
    @GetMapping("/agency/{id}")
    public List<Review> getAgencyReviews(@PathVariable Long id) {
        return reviewService.getReviewsByAgency(id);
    }
    
    // DELETE : Supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}