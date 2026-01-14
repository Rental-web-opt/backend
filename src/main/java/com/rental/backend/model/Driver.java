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

    private String name;
    private Integer age;
    private String experience; // "12 ans"
    private String location;   // Ville de résidence
    private Double pricePerDay;
    
    private String image;      // Photo de profil
    private Double rating;
    private Integer reviewCount;

    @Column(length = 2000)
    private String bio;        // Biographie détaillée

    // Contact (Peut être masqué selon les droits)
    private String phone;
    private String email;

    @ElementCollection
    private List<String> languages; // ["Français", "Anglais", "Pidgin"]
}