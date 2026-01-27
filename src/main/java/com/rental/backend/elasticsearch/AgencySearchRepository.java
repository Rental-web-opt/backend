package com.rental.backend.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgencySearchRepository extends ElasticsearchRepository<AgencyDocument, String> {
    
    List<AgencyDocument> findByNameContaining(String name);
    
    List<AgencyDocument> findByCity(String city);
    
    List<AgencyDocument> findByIsOpenTrue();
}
