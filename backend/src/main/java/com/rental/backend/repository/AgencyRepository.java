package com.rental.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rental.backend.model.Agency;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, Long> {
    List<Agency> findByCity(String city);
}