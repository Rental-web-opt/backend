package com.rental.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Demande de publication d'offre par une agence
 * L'agence soumet une demande, l'admin l'approuve ou la rejette
 */
@Entity
@Table(name = "offer_requests")
public class OfferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long agencyId;  // L'agence qui fait la demande

    @Column(nullable = false)
    private Long agencyUserId;  // Le userId de l'agence (pour notifications)

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferTarget target = OfferTarget.CLIENTS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column
    private Long processedByAdminId;  // L'admin qui a traité la demande

    @Column
    private String rejectionReason;  // Raison du refus si rejeté

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime processedAt;  // Date de traitement

    public enum RequestStatus {
        PENDING,    // En attente
        APPROVED,   // Approuvée
        REJECTED    // Rejetée
    }

    // Constructors
    public OfferRequest() {}

    public OfferRequest(Long agencyId, Long agencyUserId, String title, String message, OfferTarget target) {
        this.agencyId = agencyId;
        this.agencyUserId = agencyUserId;
        this.title = title;
        this.message = message;
        this.target = target;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAgencyId() { return agencyId; }
    public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }

    public Long getAgencyUserId() { return agencyUserId; }
    public void setAgencyUserId(Long agencyUserId) { this.agencyUserId = agencyUserId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public OfferTarget getTarget() { return target; }
    public void setTarget(OfferTarget target) { this.target = target; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Long getProcessedByAdminId() { return processedByAdminId; }
    public void setProcessedByAdminId(Long processedByAdminId) { this.processedByAdminId = processedByAdminId; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
