package com.rental.backend.service;

import com.rental.backend.model.Agency;
import com.rental.backend.repository.AgencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AgencyService {
    @Autowired private AgencyRepository agencyRepository;

    public List<Agency> getAllAgencies() { return agencyRepository.findAll(); }
    public Optional<Agency> getAgencyById(Long id) { return agencyRepository.findById(id); }
    public Agency saveAgency(Agency agency) { return agencyRepository.save(agency); }
    public void deleteAgency(Long id) { agencyRepository.deleteById(id); }
}