package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lien vers le compte utilisateur du chauffeur
    private Long userId;

    private String fullName;
    private String name; // Garder pour compatibilité
    private Integer age;
    private Integer experience; // Années d'expérience
    private String location;   // Ville de résidence
    private Double pricePerDay;
    private boolean available = true;
    private String licenseNumber;
    
    private String image;      // Photo de profil
    private Double rating;
    private Integer reviewCount;

    @Column(length = 2000)
    private String bio;        // Biographie détaillée

    // Contact
    private String phone;
    private String email;

    @ElementCollection
    private List<String> languages; // ["Français", "Anglais", "Pidgin"]
    
    // Helper pour obtenir le nom (fullName ou name)
    public String getFullName() {
        return fullName != null ? fullName : name;
    }
}