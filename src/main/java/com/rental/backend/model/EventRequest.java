package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Demande de publication d'événement par une agence
 * L'admin doit approuver avant la diffusion
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_requests")
public class EventRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(length = 2000)
    private String message;
    
    // Agence qui demande la publication
    private Long agencyId;
    private String agencyName;
    
    // Statut de la demande
    @Enumerated(EnumType.STRING)
    private EventRequestStatus status = EventRequestStatus.PENDING;
    
    // Raison du refus (si refusé)
    private String rejectionReason;
    
    // Timestamps
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime processedAt;
    
    // Admin qui a traité la demande
    private Long processedByAdminId;
}
