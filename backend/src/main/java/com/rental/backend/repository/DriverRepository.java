package com.rental.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rental.backend.model.Driver;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    // Trouver les chauffeurs par ville
    List<Driver> findByLocationContainingIgnoreCase(String location);
    
    // Trouver les chauffeurs parlant une langue spécifique (Requête JPQL plus avancée si besoin, 
    // mais ici on peut filtrer côté service ou utiliser une convention de nommage simple si possible)
}