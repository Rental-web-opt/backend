package com.rental.backend.service;

import com.rental.backend.model.Agency;
import com.rental.backend.model.User;
import com.rental.backend.model.Role;
import com.rental.backend.repository.AgencyRepository;
import com.rental.backend.repository.UserRepository;
import com.rental.backend.dto.AgencyCreationResponse;
import com.rental.backend.elasticsearch.AgencyDocument;
import com.rental.backend.elasticsearch.AgencySearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgencyService {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired(required = false) private AgencySearchRepository agencySearchRepository;

    public List<Agency> getAllAgencies() { 
        return agencyRepository.findAll(); 
    }
    
    public Optional<Agency> getAgencyById(Long id) { 
        return agencyRepository.findById(id); 
    }

    public Optional<Agency> getAgencyByUserId(Long userId) {
        return agencyRepository.findByUserId(userId);
    }

    /**
     * Création d'une agence avec génération automatique des identifiants de connexion
     * 
     * @param agency Les données de l'agence à créer
     * @return AgencyCreationResponse contenant l'agence + les identifiants générés
     */
    @Transactional
    public AgencyCreationResponse createAgencyWithCredentials(Agency agency) {
        // 1. Générer l'email de connexion à partir du nom de l'agence
        String baseEmail = generateEmailFromName(agency.getName());
        String email = ensureUniqueEmail(baseEmail);
        
        // 2. Générer un mot de passe aléatoire sécurisé
        String rawPassword = generateSecurePassword();
        
        // 3. Créer le compte utilisateur pour l'agence
        User agencyUser = new User();
        agencyUser.setFullName(agency.getName());
        agencyUser.setEmail(email);
        agencyUser.setPassword(passwordEncoder.encode(rawPassword));
        agencyUser.setRole(Role.AGENCY);
        
        User savedUser = userRepository.save(agencyUser);
        System.out.println("👤 Compte agence créé: " + email);
        
        // 4. Lier l'agence au compte utilisateur
        agency.setUserId(savedUser.getId());
        
        // 5. Si l'email de l'agence n'est pas défini, utiliser l'email de connexion
        if (agency.getEmail() == null || agency.getEmail().isEmpty()) {
            agency.setEmail(email);
        }
        
        // 6. Sauvegarder l'agence
        Agency savedAgency = agencyRepository.save(agency);
        System.out.println("🏢 Agence créée: " + savedAgency.getName() + " (ID: " + savedAgency.getId() + ")");
        
        // 7. Indexer dans Elasticsearch
        indexInElasticsearch(savedAgency);
        
        // 8. Retourner la réponse avec les identifiants
        return new AgencyCreationResponse(savedAgency, email, rawPassword);
    }

    /**
     * Mise à jour simple d'une agence (sans modification des identifiants)
     */
    public Agency saveAgency(Agency agency) {
        Agency savedAgency = agencyRepository.save(agency);
        indexInElasticsearch(savedAgency);
        return savedAgency;
    }

    @Transactional
    public void deleteAgency(Long id) {
        Optional<Agency> agencyOpt = agencyRepository.findById(id);
        if (agencyOpt.isPresent()) {
            Agency agency = agencyOpt.get();
            
            // Supprimer aussi le compte utilisateur associé
            if (agency.getUserId() != null) {
                userRepository.deleteById(agency.getUserId());
                System.out.println("👤 Compte utilisateur supprimé: " + agency.getUserId());
            }
            
            agencyRepository.deleteById(id);
            System.out.println("🏢 Agence supprimée: " + agency.getName());
            
            if (agencySearchRepository != null) {
                agencySearchRepository.deleteById(id.toString());
            }
        }
    }

    /**
     * Crée une agence pour un utilisateur existant et met à jour son rôle
     */
    @Transactional
    public Agency createAgencyForUser(Long userId, Agency agency) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        User user = userOpt.get();

        // Vérifier si l'utilisateur a déjà un rôle autre que USER
        if (user.getRole() != Role.USER) {
            throw new RuntimeException("L'utilisateur a déjà un rôle spécial: " + user.getRole());
        }

        // Lier l'agence
        agency.setUserId(userId);
        if (agency.getEmail() == null || agency.getEmail().isEmpty()) {
            agency.setEmail(user.getEmail());
        }

        Agency savedAgency = agencyRepository.save(agency);
        indexInElasticsearch(savedAgency);

        // Mettre à jour le rôle
        user.setRole(Role.AGENCY);
        userRepository.save(user);

        System.out.println("✅ Utilisateur " + userId + " promu AGENCE: " + savedAgency.getName());
        return savedAgency;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Génère un email à partir du nom de l'agence
     * Ex: "AutoLux Douala" -> "autolux.douala@easyrent.cm"
     */
    private String generateEmailFromName(String name) {
        if (name == null || name.isEmpty()) {
            name = "agency";
        }
        String cleanName = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // Enlever caractères spéciaux
                .replaceAll("\\s+", ".")        // Espaces -> points
                .trim();
        
        if (cleanName.isEmpty()) {
            cleanName = "agency";
        }
        
        return cleanName + "@easyrent.cm";
    }

    /**
     * S'assure que l'email est unique en ajoutant un suffixe si nécessaire
     */
    private String ensureUniqueEmail(String email) {
        String baseEmail = email;
        int counter = 1;
        
        while (userRepository.findByEmail(email).isPresent()) {
            // Ajouter un suffixe numérique avant le @
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
        // Format: 8 caractères aléatoires + 4 chiffres
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        
        StringBuilder password = new StringBuilder();
        
        // 8 lettres aléatoires
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        
        // 4 chiffres
        for (int i = 0; i < 4; i++) {
            password.append(digits.charAt((int) (Math.random() * digits.length())));
        }
        
        return password.toString();
    }

    private void indexInElasticsearch(Agency agency) {
        if (agencySearchRepository != null) {
            try {
                AgencyDocument doc = mapToDocument(agency);
                agencySearchRepository.save(doc);
                System.out.println("🔄 Agence indexée dans ES: " + doc.getName());
            } catch (Exception e) {
                System.err.println("❌ Erreur indexation ES Agence: " + e.getMessage());
            }
        }
    }

    private AgencyDocument mapToDocument(Agency agency) {
        AgencyDocument doc = new AgencyDocument();
        doc.setId(agency.getId().toString());
        doc.setName(agency.getName());
        doc.setCity(agency.getCity());
        doc.setLocation(agency.getLocation());
        doc.setIsOpen(agency.isOpen());
        return doc;
    }
}
