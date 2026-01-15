package com.rental.backend.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarSearchRepository extends ElasticsearchRepository<CarDocument, String> {
    
    List<CarDocument> findByNameContainingOrBrandContaining(String name, String brand);
    
    List<CarDocument> findByType(String type);
    
    List<CarDocument> findByAvailableTrue();
    
    List<CarDocument> findByPricePerDayBetween(Double min, Double max);
}
