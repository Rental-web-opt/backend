package com.rental.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferTarget target;  // Qui reçoit l'offre

    @Column
    private Long createdByAdminId;  // L'admin qui a publié

    @Column
    private Long requestedByAgencyId;  // Si l'offre a été demandée par une agence

    @Column
    private Boolean approved = true;  // Une offre de l'admin est auto-approuvée

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime expiresAt;  // Date d'expiration optionnelle

    @Column
    private Boolean active = true;

    // Constructors
    public Offer() {}

    public Offer(String title, String message, OfferTarget target, Long createdByAdminId) {
        this.title = title;
        this.message = message;
        this.target = target;
        this.createdByAdminId = createdByAdminId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public OfferTarget getTarget() { return target; }
    public void setTarget(OfferTarget target) { this.target = target; }

    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }

    public Long getRequestedByAgencyId() { return requestedByAgencyId; }
    public void setRequestedByAgencyId(Long requestedByAgencyId) { this.requestedByAgencyId = requestedByAgencyId; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
