package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agencies")
public class Agency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String city;
    private String location; // Adresse précise
    
    @Column(length = 2000)
    private String description;

    // Visuels
    private String logo;
    private String coverImage;

    // Contact
    private String phone;
    private String email;
    private String website;

    // Infos Pratiques
    private String openingHours; // "Lun-Ven: 8h-18h"
    private boolean isOpen;
    
    // Social
    private Double rating;
    private Integer reviewCount;

    @ElementCollection
    private List<String> tags; // ["Luxe", "Aéroport", "24/7"]

    // Liste des voitures de l'agence
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL)
    @JsonIgnore // Pour éviter la récursion infinie dans l'API
    private List<Car> vehicles;
}