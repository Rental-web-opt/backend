package com.rental.backend.repository;

import com.rental.backend.model.EventRequest;
import com.rental.backend.model.EventRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRequestRepository extends JpaRepository<EventRequest, Long> {
    
    List<EventRequest> findByStatus(EventRequestStatus status);
    
    List<EventRequest> findByAgencyId(Long agencyId);
    
    List<EventRequest> findByStatusOrderByCreatedAtDesc(EventRequestStatus status);
}
