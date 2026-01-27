package com.rental.backend.service;

import com.rental.backend.model.Agency;
import com.rental.backend.repository.AgencyRepository;
import com.rental.backend.elasticsearch.AgencyDocument;
import com.rental.backend.elasticsearch.AgencySearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AgencyService {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired(required = false) private AgencySearchRepository agencySearchRepository;

    public List<Agency> getAllAgencies() { return agencyRepository.findAll(); }
    public Optional<Agency> getAgencyById(Long id) { return agencyRepository.findById(id); }

    public Agency saveAgency(Agency agency) {
        Agency savedAgency = agencyRepository.save(agency);
        if (agencySearchRepository != null) {
            try {
                AgencyDocument doc = mapToDocument(savedAgency);
                agencySearchRepository.save(doc);
                System.out.println("🔄 Agence indexée dans ES : " + doc.getName());
            } catch (Exception e) {
                System.err.println("❌ Erreur indexation ES Agence : " + e.getMessage());
            }
        }
        return savedAgency;
    }

    public void deleteAgency(Long id) {
        agencyRepository.deleteById(id);
        if (agencySearchRepository != null) {
            agencySearchRepository.deleteById(id.toString());
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
