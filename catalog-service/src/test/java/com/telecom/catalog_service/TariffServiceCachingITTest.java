package com.telecom.catalog_service;

import com.telecom.catalog_service.dto.TariffRequest;
import com.telecom.catalog_service.dto.TariffResponse;
import com.telecom.catalog_service.model.Tariff;
import com.telecom.catalog_service.repository.TariffRepository;
import com.telecom.catalog_service.service.TariffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.TestPropertySource;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class TariffServiceCachingITTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;


    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private TariffService tariffService;

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        tariffRepository.deleteAll();
        Objects.requireNonNull(cacheManager.getCache("tariffs")).clear();
    }

    @Test
    @DisplayName("E2E-1: @Cacheable should cache tariff retrieval and avoid duplicate database hits")
    void shouldCacheTariffRetrieval() {
        // 1. GIVEN: Create a tariff in MongoDB directly
        Tariff tariff = tariffRepository.save(Tariff.builder()
                .name("Cache Test Tariff")
                .price(new BigDecimal("150.00"))
                .isActive(true)
                .build());

        String tariffId = tariff.getId();

        // 2. WHEN: First invocation (Should hit DB and store in cache)
        TariffResponse firstCall = tariffService.getTariffById(tariffId);

        // Modify DB record directly bypassing the service to prove cache is used on second call
        tariff.setName("Modified In DB Only");
        tariffRepository.save(tariff);

        // Second invocation (Should retrieve from "tariffs" cache, not DB)
        TariffResponse secondCall = tariffService.getTariffById(tariffId);

        // 3. THEN: The name should still be "Cache Test Tariff" because it was served from cache
        assertThat(firstCall.name()).isEqualTo("Cache Test Tariff");
        assertThat(secondCall.name()).isEqualTo("Cache Test Tariff");
    }

    @Test
    @DisplayName("E2E-2: @CacheEvict should clear cache when tariff is updated")
    void shouldEvictCacheOnTariffUpdate() {
        // 1. GIVEN: Create a tariff and warm up the cache
        Tariff tariff = tariffRepository.save(Tariff.builder()
                .name("Old Tariff Name")
                .price(new BigDecimal("100.00"))
                .isActive(false)
                .build());

        String tariffId = tariff.getId();
        tariffService.getTariffById(tariffId); // Populates cache

        // 2. WHEN: Update tariff via service (Should trigger @CacheEvict)
        TariffRequest updateRequest = new TariffRequest(
                "New Tariff Name", "Updated desc", new BigDecimal("120.00"),
                20, 500, 500, 30
        );
        tariffService.updateTariff(tariffId, updateRequest, true);

        // 3. THEN: Fetching by ID again should return newly evicted and updated data
        TariffResponse latestResponse = tariffService.getTariffById(tariffId);
        assertThat(latestResponse.name()).isEqualTo("New Tariff Name");
        assertThat(latestResponse.isActive()).isTrue();
    }
}