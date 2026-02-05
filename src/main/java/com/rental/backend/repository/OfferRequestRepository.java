package com.rental.backend.repository;

import com.rental.backend.model.OfferRequest;
import com.rental.backend.model.OfferRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRequestRepository extends JpaRepository<OfferRequest, Long> {
    
    // Demandes en attente
    List<OfferRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    
    // Demandes par agence
    List<OfferRequest> findByAgencyIdOrderByCreatedAtDesc(Long agencyId);
    
    // Demandes en attente count
    long countByStatus(RequestStatus status);
}
