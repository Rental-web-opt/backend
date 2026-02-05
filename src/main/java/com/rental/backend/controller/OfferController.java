package com.rental.backend.controller;

import com.rental.backend.model.*;
import com.rental.backend.service.OfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class OfferController {

    @Autowired
    private OfferService offerService;

    // ==========================================
    // 📢 ENDPOINTS ADMIN - PUBLICATION D'OFFRES
    // ==========================================

    /**
     * Admin publie une nouvelle offre
     * POST /api/offers/publish
     * Body: { adminId, title, message, target, expiresAt (optionnel) }
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishOffer(@RequestBody Map<String, Object> request) {
        try {
            Long adminId = Long.valueOf(request.get("adminId").toString());
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            OfferTarget target = OfferTarget.valueOf(request.get("target").toString().toUpperCase());
            
            LocalDateTime expiresAt = null;
            if (request.containsKey("expiresAt") && request.get("expiresAt") != null) {
                expiresAt = LocalDateTime.parse((String) request.get("expiresAt"));
            }
            
            Offer offer = offerService.publishOffer(adminId, title, message, target, expiresAt);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offer", offer,
                "message", "Offre publiée avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère toutes les offres (admin)
     * GET /api/offers
     */
    @GetMapping
    public ResponseEntity<List<Offer>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    /**
     * Récupère les offres actives pour un type d'utilisateur
     * GET /api/offers/active?target=CLIENTS
     */
    @GetMapping("/active")
    public ResponseEntity<List<Offer>> getActiveOffers(@RequestParam(required = false) String target) {
        if (target != null) {
            OfferTarget offerTarget = OfferTarget.valueOf(target.toUpperCase());
            return ResponseEntity.ok(offerService.getActiveOffersForTarget(offerTarget));
        }
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    /**
     * Désactive une offre
     * DELETE /api/offers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateOffer(@PathVariable Long id) {
        try {
            Offer offer = offerService.deactivateOffer(id);
            return ResponseEntity.ok(Map.of("success", true, "offer", offer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 📋 ENDPOINTS AGENCE - DEMANDES DE PUBLICATION
    // ==========================================

    /**
     * Agence soumet une demande de publication
     * POST /api/offers/request
     * Body: { agencyId, title, message, target }
     */
    @PostMapping("/request")
    public ResponseEntity<?> submitRequest(@RequestBody Map<String, Object> request) {
        try {
            Long agencyId = Long.valueOf(request.get("agencyId").toString());
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            OfferTarget target = OfferTarget.CLIENTS; // Par défaut aux clients
            
            if (request.containsKey("target") && request.get("target") != null) {
                target = OfferTarget.valueOf(request.get("target").toString().toUpperCase());
            }
            
            OfferRequest offerRequest = offerService.submitRequest(agencyId, title, message, target);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "request", offerRequest,
                "message", "Demande soumise. En attente d'approbation par l'admin."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère les demandes d'une agence
     * GET /api/offers/request/agency/{agencyId}
     */
    @GetMapping("/request/agency/{agencyId}")
    public ResponseEntity<List<OfferRequest>> getAgencyRequests(@PathVariable Long agencyId) {
        return ResponseEntity.ok(offerService.getAgencyRequests(agencyId));
    }

    // ==========================================
    // 📋 ENDPOINTS ADMIN - GESTION DES DEMANDES
    // ==========================================

    /**
     * Récupère les demandes en attente (admin)
     * GET /api/offers/requests/pending
     */
    @GetMapping("/requests/pending")
    public ResponseEntity<List<OfferRequest>> getPendingRequests() {
        return ResponseEntity.ok(offerService.getPendingRequests());
    }

    /**
     * Compte les demandes en attente
     * GET /api/offers/requests/pending/count
     */
    @GetMapping("/requests/pending/count")
    public ResponseEntity<Long> countPendingRequests() {
        return ResponseEntity.ok(offerService.countPendingRequests());
    }

    /**
     * Admin approuve une demande
     * POST /api/offers/request/{id}/approve
     * Body: { adminId }
     */
    @PostMapping("/request/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long adminId = Long.valueOf(request.get("adminId").toString());
            Offer offer = offerService.approveRequest(id, adminId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "offer", offer,
                "message", "Demande approuvée et offre publiée"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin rejette une demande
     * POST /api/offers/request/{id}/reject
     * Body: { adminId, reason }
     */
    @PostMapping("/request/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long adminId = Long.valueOf(request.get("adminId").toString());
            String reason = (String) request.getOrDefault("reason", "Aucune raison spécifiée");
            OfferRequest offerRequest = offerService.rejectRequest(id, adminId, reason);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "request", offerRequest,
                "message", "Demande rejetée"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
