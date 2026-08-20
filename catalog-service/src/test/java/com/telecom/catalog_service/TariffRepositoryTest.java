package com.telecom.catalog_service;

import com.telecom.catalog_service.config.CacheConfig;
import com.telecom.catalog_service.model.Tariff;
import com.telecom.catalog_service.repository.TariffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Testcontainers
@DataMongoTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Import(CacheConfig.class)
class TariffRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private TariffRepository tariffRepository;

    @Test
    @DisplayName("1. CRUD: Should save tariff document to MongoDB and retrieve it by ID")
    void saveAndFindById_shouldPersistTariffDocument() {
        // Given
        Tariff tariff = Tariff.builder()
                .name("Ultra 100GB")
                .description("Maximum speed package")
                .price(new BigDecimal("499.99"))
                .dataLimitGb(100)
                .voiceLimitMinutes(2000)
                .smsLimit(2000)
                .isActive(true)
                .validityDays(30)
                .build();

        // When
        Tariff saved = tariffRepository.save(tariff);
        Optional<Tariff> found = tariffRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Ultra 100GB");
        assertThat(found.get().getPrice()).isEqualByComparingTo("499.99");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("2. Delete: Should delete tariff document from MongoDB completely")
    void deleteById_shouldRemoveDocument() {
        // Given
        Tariff saved = tariffRepository.save(
                Tariff.builder().name("Temp Tariff").price(BigDecimal.TEN).build()
        );
        assertThat(tariffRepository.existsById(saved.getId())).isTrue();

        // When
        tariffRepository.deleteById(saved.getId());

        // Then
        assertThat(tariffRepository.existsById(saved.getId())).isFalse();
    }
}