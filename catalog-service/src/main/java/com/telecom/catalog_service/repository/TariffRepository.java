package com.telecom.catalog_service.repository;

import com.telecom.catalog_service.model.Tariff;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TariffRepository extends MongoRepository<Tariff, String> {
    // Spring Data MongoDB automatically implements basic CRUD operations
}