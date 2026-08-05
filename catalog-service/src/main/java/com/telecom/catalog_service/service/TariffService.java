package com.telecom.catalog_service.service;

import com.telecom.catalog_service.mapper.TariffMapper;
import com.telecom.catalog_service.model.Tariff;
import com.telecom.catalog_service.repository.TariffRepository;
import com.telecom.catalog_service.exception.TariffNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.telecom.catalog_service.dto.TariffRequest;
import com.telecom.catalog_service.dto.TariffResponse;

import java.util.List;

@Service
public class TariffService {

    private final TariffRepository tariffRepository;
    private final TariffMapper tariffMapper;

    public TariffService(TariffRepository tariffRepository, TariffMapper tariffMapper) {
        this.tariffRepository = tariffRepository;
        this.tariffMapper = tariffMapper;
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public TariffResponse createTariff(TariffRequest request) {
        Tariff tariff = tariffMapper.toEntity(request);
        Tariff savedTariff = tariffRepository.save(tariff);

        return tariffMapper.toResponse(savedTariff);
    }
    @Cacheable(value = "tariffs", key = "'all'")
    public List<TariffResponse> getAllTariffs() {
        return tariffRepository.findAll()
                .stream()
                .map(tariffMapper::toResponse)
                .toList();
    }
    @Cacheable(value = "tariffs", key = "#id")
    public TariffResponse getTariffById(String id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException(id));

        return tariffMapper.toResponse(tariff);
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public void deleteTariff(String id) {
        if (!tariffRepository.existsById(id)) {
            throw new TariffNotFoundException(id);
        }
        tariffRepository.deleteById(id);
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public TariffResponse updateTariff(String id, TariffRequest request, boolean activate) {

        Tariff existingTariff = tariffRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException(id));

        tariffMapper.updateTariffFromRequest(request, existingTariff);

        existingTariff.setActive(activate);

        Tariff updatedTariff = tariffRepository.save(existingTariff);

        return tariffMapper.toResponse(updatedTariff);
    }
}