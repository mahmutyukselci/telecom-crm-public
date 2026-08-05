package com.telecom.catalog_service;

import com.telecom.catalog_service.config.CacheConfig;
import com.telecom.catalog_service.model.Tariff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest
@Import(CacheConfig.class)
class TariffRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MongoTemplate mongoTemplate;

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
        Tariff saved = mongoTemplate.save(tariff);
        Tariff found = mongoTemplate.findById(saved.getId(), Tariff.class);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Ultra 100GB");
        assertThat(found.getPrice()).isEqualByComparingTo("499.99");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("2. Delete: Should delete tariff document from MongoDB completely")
    void deleteById_shouldRemoveDocument() {
        // Given
        Tariff saved = mongoTemplate.save(
                Tariff.builder().name("Temp Tariff").price(BigDecimal.TEN).build()
        );

        Tariff foundBeforeDelete = mongoTemplate.findById(saved.getId(), Tariff.class);
        assertThat(foundBeforeDelete).isNotNull();

        // When
        Query query = new Query(Criteria.where("id").is(saved.getId()));
        mongoTemplate.remove(query, Tariff.class);

        // Then
        Tariff foundAfterDelete = mongoTemplate.findById(saved.getId(), Tariff.class);
        assertThat(foundAfterDelete).isNull();
    }
}