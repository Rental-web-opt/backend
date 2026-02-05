package com.rental.backend.controller;

import com.rental.backend.model.EventRequest;
import com.rental.backend.model.EventRequestStatus;
import com.rental.backend.model.Agency;
import com.rental.backend.model.User;
import com.rental.backend.model.Role;
import com.rental.backend.repository.EventRequestRepository;
import com.rental.backend.repository.AgencyRepository;
import com.rental.backend.repository.UserRepository;
import com.rental.backend.kafka.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
public class EventController {

    @Autowired
    private EventRequestRepository eventRequestRepository;
    
    @Autowired
    private AgencyRepository agencyRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KafkaProducerService kafkaProducerService;

    // ==================== ADMIN: PUBLICATION DIRECTE ====================

    /**
     * L'admin publie un événement directement à tous les utilisateurs
     * POST /api/events/publish
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishEvent(
            @RequestParam Long adminId,
            @RequestBody Map<String, String> eventData) {
        
        // Vérifier que c'est bien un admin
        Optional<User> adminOpt = userRepository.findById(adminId);
        if (adminOpt.isEmpty() || adminOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of(
                "error", true,
                "message", "Seul un administrateur peut publier des événements"
            ));
        }

        String title = eventData.get("title");
        String message = eventData.get("message");

        if (title == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Le titre et le message sont requis"
            ));
        }

        // Publier via Kafka à tous les utilisateurs (sauf l'admin)
        kafkaProducerService.broadcastEvent(title, message, adminId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Événement publié à tous les utilisateurs"
        ));
    }

    // ==================== AGENCE: DEMANDE DE PUBLICATION ====================

    /**
     * Une agence demande la publication d'un événement (nécessite approbation admin)
     * POST /api/events/request
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestEventPublication(
            @RequestParam Long agencyId,
            @RequestBody Map<String, String> eventData) {
        
        // Récupérer l'agence
        Optional<Agency> agencyOpt = agencyRepository.findById(agencyId);
        if (agencyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Agence introuvable"
            ));
        }
        
        Agency agency = agencyOpt.get();
        String title = eventData.get("title");
        String message = eventData.get("message");

        if (title == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Le titre et le message sont requis"
            ));
        }

        // Créer la demande
        EventRequest request = new EventRequest();
        request.setTitle(title);
        request.setMessage(message);
        request.setAgencyId(agencyId);
        request.setAgencyName(agency.getName());
        request.setStatus(EventRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        
        EventRequest saved = eventRequestRepository.save(request);
        System.out.println("📝 Nouvelle demande d'événement créée par " + agency.getName());

        // Notifier tous les admins
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            kafkaProducerService.notifyAdminEventRequest(
                admin.getId(),
                agency.getName(),
                title,
                message,
                saved.getId()
            );
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Demande de publication envoyée à l'administrateur",
            "requestId", saved.getId()
        ));
    }

    // ==================== ADMIN: GÉRER LES DEMANDES ====================

    /**
     * Récupérer toutes les demandes d'événements en attente
     * GET /api/events/requests/pending
     */
    @GetMapping("/requests/pending")
    public List<EventRequest> getPendingRequests() {
        return eventRequestRepository.findByStatusOrderByCreatedAtDesc(EventRequestStatus.PENDING);
    }

    /**
     * Récupérer toutes les demandes d'événements
     * GET /api/events/requests
     */
    @GetMapping("/requests")
    public List<EventRequest> getAllRequests() {
        return eventRequestRepository.findAll();
    }

    /**
     * Approuver une demande d'événement et la publier
     * PUT /api/events/requests/{id}/approve
     */
    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<?> approveEventRequest(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        
        // Vérifier que c'est bien un admin
        Optional<User> adminOpt = userRepository.findById(adminId);
        if (adminOpt.isEmpty() || adminOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of(
                "error", true,
                "message", "Seul un administrateur peut approuver des événements"
            ));
        }

        Optional<EventRequest> requestOpt = eventRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EventRequest request = requestOpt.get();
        
        if (request.getStatus() != EventRequestStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Cette demande a déjà été traitée"
            ));
        }

        // Marquer comme approuvée
        request.setStatus(EventRequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(adminId);
        eventRequestRepository.save(request);

        // Publier l'événement à tous les utilisateurs (sauf admin et agence qui a demandé)
        // L'agence a un userId associé
        Agency agency = agencyRepository.findById(request.getAgencyId()).orElse(null);
        Long agencyUserId = agency != null ? agency.getUserId() : null;
        
        kafkaProducerService.broadcastEvent(
            request.getTitle(),
            request.getMessage() + " (par " + request.getAgencyName() + ")",
            agencyUserId // Exclure l'agence qui a demandé
        );

        // Notifier l'agence que c'est approuvé
        if (agencyUserId != null) {
            kafkaProducerService.notifyAgencyEventApproved(agencyUserId, request.getTitle());
        }

        System.out.println("✅ Événement approuvé et publié: " + request.getTitle());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Événement approuvé et publié"
        ));
    }

    /**
     * Refuser une demande d'événement
     * PUT /api/events/requests/{id}/reject
     */
    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectEventRequest(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {
        
        // Vérifier que c'est bien un admin
        Optional<User> adminOpt = userRepository.findById(adminId);
        if (adminOpt.isEmpty() || adminOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of(
                "error", true,
                "message", "Seul un administrateur peut refuser des événements"
            ));
        }

        Optional<EventRequest> requestOpt = eventRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EventRequest request = requestOpt.get();
        
        if (request.getStatus() != EventRequestStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Cette demande a déjà été traitée"
            ));
        }

        // Marquer comme refusée
        request.setStatus(EventRequestStatus.REJECTED);
        request.setRejectionReason(reason);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(adminId);
        eventRequestRepository.save(request);

        // Notifier l'agence du refus
        Agency agency = agencyRepository.findById(request.getAgencyId()).orElse(null);
        if (agency != null && agency.getUserId() != null) {
            kafkaProducerService.notifyAgencyEventRejected(
                agency.getUserId(),
                request.getTitle(),
                reason
            );
        }

        System.out.println("❌ Événement refusé: " + request.getTitle());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Demande d'événement refusée"
        ));
    }

    /**
     * Récupérer les demandes d'une agence spécifique
     * GET /api/events/agency/{agencyId}
     */
    @GetMapping("/agency/{agencyId}")
    public List<EventRequest> getAgencyRequests(@PathVariable Long agencyId) {
        return eventRequestRepository.findByAgencyId(agencyId);
    }
}
