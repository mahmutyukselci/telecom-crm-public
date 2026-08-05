package com.telecom.catalog_service.service;

import com.telecom.catalog_service.mapper.TariffMapper;
import com.telecom.catalog_service.model.Tariff;
import com.telecom.catalog_service.exception.TariffNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import com.telecom.catalog_service.dto.TariffRequest;
import com.telecom.catalog_service.dto.TariffResponse;

import java.util.List;

@Service
public class TariffService {
    private final TariffMapper tariffMapper;
    private final MongoTemplate mongoTemplate;

    // Updated constructor
    public TariffService(
                         TariffMapper tariffMapper,
                         MongoTemplate mongoTemplate) {
        this.tariffMapper = tariffMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public TariffResponse createTariff(TariffRequest request) {
        Tariff tariff = tariffMapper.toEntity(request);

        // Example: using mongoTemplate instead of repository.save()
        Tariff savedTariff = mongoTemplate.save(tariff);

        return tariffMapper.toResponse(savedTariff);
    }

    @Cacheable(value = "tariffs", key = "'all'")
    public List<TariffResponse> getAllTariffs() {
        // Example: using mongoTemplate instead of repository.findAll()
        return mongoTemplate.findAll(Tariff.class)
                .stream()
                .map(tariffMapper::toResponse)
                .toList();
    }

    @Cacheable(value = "tariffs", key = "#id")
    public TariffResponse getTariffById(String id) {
        // Example: finding by ID using mongoTemplate
        Tariff tariff = mongoTemplate.findById(id, Tariff.class);

        if (tariff == null) {
            throw new TariffNotFoundException(id);
        }

        return tariffMapper.toResponse(tariff);
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public void deleteTariff(String id) {
        Tariff tariff = mongoTemplate.findById(id, Tariff.class);

        if (tariff == null) {
            throw new TariffNotFoundException(id);
        }

        mongoTemplate.remove(tariff);
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public TariffResponse updateTariff(String id, TariffRequest request, boolean activate) {
        Tariff existingTariff = mongoTemplate.findById(id, Tariff.class);

        if (existingTariff == null) {
            throw new TariffNotFoundException(id);
        }

        tariffMapper.updateTariffFromRequest(request, existingTariff);
        existingTariff.setActive(activate);

        Tariff updatedTariff = mongoTemplate.save(existingTariff);
        return tariffMapper.toResponse(updatedTariff);
    }
}