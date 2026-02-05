package com.rental.backend.service;

import com.rental.backend.controller.NotificationController;
import com.rental.backend.model.*;
import com.rental.backend.model.OfferRequest.RequestStatus;
import com.rental.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de gestion des offres/promotions et demandes de publication
 */
@Service
public class OfferService {

    @Autowired private OfferRepository offerRepository;
    @Autowired private OfferRequestRepository offerRequestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationController notificationController;

    // ==========================================
    // 📢 PUBLICATION D'OFFRES (PAR ADMIN)
    // ==========================================

    /**
     * Admin publie une offre et notifie les utilisateurs ciblés
     */
    @Transactional
    public Offer publishOffer(Long adminId, String title, String message, OfferTarget target, LocalDateTime expiresAt) {
        // Créer l'offre
        Offer offer = new Offer(title, message, target, adminId);
        offer.setExpiresAt(expiresAt);
        offer = offerRepository.save(offer);
        
        System.out.println("📢 Offre publiée: " + title + " -> " + target);
        
        // Déterminer les destinataires et envoyer les notifications
        List<User> recipients = getRecipientsByTarget(target);
        
        int count = 0;
        for (User user : recipients) {
            notificationService.createNotification(
                user.getId(),
                "🎁 " + title,
                message,
                NotificationType.PROMO
            );
            notificationController.sendNotification(user.getId(), "promo", message);
            count++;
        }
        
        System.out.println("✅ " + count + " notifications d'offre envoyées à " + target);
        return offer;
    }

    /**
     * Récupère les utilisateurs par cible
     */
    private List<User> getRecipientsByTarget(OfferTarget target) {
        switch (target) {
            case CLIENTS:
                return userRepository.findByRole(Role.USER);  // USER = clients normaux
            case AGENCIES:
                return userRepository.findByRole(Role.AGENCY);
            case DRIVERS:
                return userRepository.findByRole(Role.DRIVER);
            case ALL:
            default:
                return userRepository.findAll();
        }
    }

    /**
     * Récupère toutes les offres (pour admin)
     */
    public List<Offer> getAllOffers() {
        return offerRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Récupère les offres actives pour un type d'utilisateur
     */
    public List<Offer> getActiveOffersForTarget(OfferTarget target) {
        return offerRepository.findActiveOffersByTarget(target, LocalDateTime.now());
    }

    /**
     * Désactive une offre
     */
    @Transactional
    public Offer deactivateOffer(Long offerId) {
        Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        offer.setActive(false);
        return offerRepository.save(offer);
    }

    // ==========================================
    // 📋 DEMANDES DE PUBLICATION (PAR AGENCES)
    // ==========================================

    /**
     * Une agence soumet une demande de publication
     */
    @Transactional
    public OfferRequest submitRequest(Long agencyId, String title, String message, OfferTarget target) {
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new RuntimeException("Agence introuvable"));
        
        if (agency.getUserId() == null) {
            throw new RuntimeException("Cette agence n'a pas de compte utilisateur associé");
        }
        
        OfferRequest request = new OfferRequest(agencyId, agency.getUserId(), title, message, target);
        request = offerRequestRepository.save(request);
        
        System.out.println("📩 Demande de publication soumise par agence #" + agencyId);
        
        // Notifier tous les admins
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            String notifMessage = String.format("L'agence %s souhaite publier une offre: \"%s\"", agency.getName(), title);
            notificationService.createNotification(
                admin.getId(),
                "📩 Demande de publication",
                notifMessage,
                NotificationType.SYSTEM
            );
            notificationController.sendNotification(admin.getId(), "offer_request", notifMessage);
        }
        
        return request;
    }

    /**
     * Admin approuve une demande
     */
    @Transactional
    public Offer approveRequest(Long requestId, Long adminId) {
        OfferRequest request = offerRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Demande introuvable"));
        
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }
        
        // Marquer comme approuvée
        request.setStatus(RequestStatus.APPROVED);
        request.setProcessedByAdminId(adminId);
        request.setProcessedAt(LocalDateTime.now());
        offerRequestRepository.save(request);
        
        // Publier l'offre
        Offer offer = new Offer(request.getTitle(), request.getMessage(), request.getTarget(), adminId);
        offer.setRequestedByAgencyId(request.getAgencyId());
        offer = offerRepository.save(offer);
        
        // Envoyer les notifications aux destinataires
        List<User> recipients = getRecipientsByTarget(request.getTarget());
        for (User user : recipients) {
            notificationService.createNotification(
                user.getId(),
                "🎁 " + request.getTitle(),
                request.getMessage(),
                NotificationType.PROMO
            );
            notificationController.sendNotification(user.getId(), "promo", request.getMessage());
        }
        
        // Notifier l'agence
        String approvalMessage = "Votre demande de publication \"" + request.getTitle() + "\" a été approuvée et publiée !";
        notificationService.createNotification(
            request.getAgencyUserId(),
            "✅ Demande approuvée",
            approvalMessage,
            NotificationType.SUCCESS
        );
        notificationController.sendNotification(request.getAgencyUserId(), "request_approved", approvalMessage);
        
        System.out.println("✅ Demande #" + requestId + " approuvée. Offre publiée à " + recipients.size() + " utilisateurs.");
        return offer;
    }

    /**
     * Admin rejette une demande
     */
    @Transactional
    public OfferRequest rejectRequest(Long requestId, Long adminId, String reason) {
        OfferRequest request = offerRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Demande introuvable"));
        
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }
        
        request.setStatus(RequestStatus.REJECTED);
        request.setProcessedByAdminId(adminId);
        request.setRejectionReason(reason);
        request.setProcessedAt(LocalDateTime.now());
        offerRequestRepository.save(request);
        
        // Notifier l'agence
        String rejectionMessage = "Votre demande de publication \"" + request.getTitle() + "\" a été refusée. Raison: " + reason;
        notificationService.createNotification(
            request.getAgencyUserId(),
            "❌ Demande refusée",
            rejectionMessage,
            NotificationType.ERROR
        );
        notificationController.sendNotification(request.getAgencyUserId(), "request_rejected", rejectionMessage);
        
        System.out.println("❌ Demande #" + requestId + " rejetée.");
        return request;
    }

    /**
     * Récupère les demandes en attente (pour admin)
     */
    public List<OfferRequest> getPendingRequests() {
        return offerRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING);
    }

    /**
     * Récupère les demandes d'une agence
     */
    public List<OfferRequest> getAgencyRequests(Long agencyId) {
        return offerRequestRepository.findByAgencyIdOrderByCreatedAtDesc(agencyId);
    }

    /**
     * Compte les demandes en attente
     */
    public long countPendingRequests() {
        return offerRequestRepository.countByStatus(RequestStatus.PENDING);
    }
}
