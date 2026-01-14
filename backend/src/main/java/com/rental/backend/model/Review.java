package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Infos de l'auteur (pour l'affichage)
    private String userName; 
    private String userAvatar;
    
    // --- CORRECTION : AJOUT DU CHAMP MANQUANT ---
    private Long userId; // C'est ce champ que le Repository cherchait !
    // --------------------------------------------

    @Column(length = 1000)
    private String comment;
    private Integer rating; // 1 à 5
    
    private LocalDate date;

    // Cibles (Sur quoi porte l'avis ?)
    private Long carId;
    private Long driverId;
    private Long agencyId;
}