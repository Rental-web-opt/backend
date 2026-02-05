package com.rental.backend.service;

import com.rental.backend.model.Driver;
import com.rental.backend.model.User;
import com.rental.backend.model.Role;
import com.rental.backend.repository.DriverRepository;
import com.rental.backend.repository.UserRepository;
import com.rental.backend.dto.DriverCreationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DriverService {
    @Autowired private DriverRepository driverRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public List<Driver> getAllDrivers() { 
        return driverRepository.findAll(); 
    }
    
    public Optional<Driver> getDriverById(Long id) { 
        return driverRepository.findById(id); 
    }
    
    public Driver saveDriver(Driver driver) { 
        return driverRepository.save(driver); 
    }
    
    public List<Driver> getDriversByLocation(String location) {
        return driverRepository.findByLocationContainingIgnoreCase(location);
    }

    /**
     * Création d'un chauffeur avec génération automatique des identifiants de connexion
     * 
     * @param driver Les données du chauffeur à créer
     * @return DriverCreationResponse contenant le chauffeur + les identifiants générés
     */
    @Transactional
    public DriverCreationResponse createDriverWithCredentials(Driver driver) {
        // 1. Générer l'email de connexion à partir du nom du chauffeur
        String baseEmail = generateEmailFromName(driver.getFullName());
        String email = ensureUniqueEmail(baseEmail);
        
        // 2. Générer un mot de passe aléatoire sécurisé
        String rawPassword = generateSecurePassword();
        
        // 3. Créer le compte utilisateur pour le chauffeur
        User driverUser = new User();
        driverUser.setFullName(driver.getFullName());
        driverUser.setEmail(email);
        driverUser.setPassword(passwordEncoder.encode(rawPassword));
        driverUser.setRole(Role.DRIVER);
        
        User savedUser = userRepository.save(driverUser);
        System.out.println("👤 Compte chauffeur créé: " + email);
        
        // 4. Lier le chauffeur au compte utilisateur
        driver.setUserId(savedUser.getId());
        
        // 5. Si l'email du chauffeur n'est pas défini, utiliser l'email de connexion
        if (driver.getEmail() == null || driver.getEmail().isEmpty()) {
            driver.setEmail(email);
        }
        
        // 6. Sauvegarder le chauffeur
        Driver savedDriver = driverRepository.save(driver);
        System.out.println("🧑‍✈️ Chauffeur créé: " + savedDriver.getFullName() + " (ID: " + savedDriver.getId() + ")");
        
        // 7. Retourner la réponse avec les identifiants
        return new DriverCreationResponse(savedDriver, email, rawPassword);
    }

    @Transactional
    public void deleteDriver(Long id) {
        Optional<Driver> driverOpt = driverRepository.findById(id);
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();
            
            // Supprimer aussi le compte utilisateur associé
            if (driver.getUserId() != null) {
                userRepository.deleteById(driver.getUserId());
                System.out.println("👤 Compte utilisateur supprimé: " + driver.getUserId());
            }
            
            driverRepository.deleteById(id);
            System.out.println("🧑‍✈️ Chauffeur supprimé: " + driver.getFullName());
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Génère un email à partir du nom du chauffeur
     * Ex: "Emmanuel Eboué" -> "emmanuel.eboue@drivers.easyrent.cm"
     */
    private String generateEmailFromName(String name) {
        if (name == null || name.isEmpty()) {
            name = "driver";
        }
        String cleanName = name.toLowerCase()
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ç]", "c")
                .replaceAll("'", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", ".")
                .trim();
        
        if (cleanName.isEmpty()) {
            cleanName = "driver";
        }
        
        return cleanName + "@drivers.easyrent.cm";
    }

    /**
     * S'assure que l'email est unique en ajoutant un suffixe si nécessaire
     */
    private String ensureUniqueEmail(String email) {
        String baseEmail = email;
        int counter = 1;
        
        while (userRepository.findByEmail(email).isPresent()) {
            String[] parts = baseEmail.split("@");
            email = parts[0] + counter + "@" + parts[1];
            counter++;
        }
        
        return email;
    }

    /**
     * Génère un mot de passe sécurisé de 12 caractères
     */
    private String generateSecurePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        
        for (int i = 0; i < 4; i++) {
            password.append(digits.charAt((int) (Math.random() * digits.length())));
        }
        
        return password.toString();
    }
}