package com.telecom.notification_service;

import com.telecom.notification_service.model.ProcessedEvent;
import com.telecom.notification_service.repository.ProcessedEventRepository;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=notification_schema",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
})
class ProcessedEventRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("1. CRUD: Should save and read ProcessedEvent successfully")
    void save_shouldPersistProcessedEvent() {
        // Given
        ProcessedEvent event = new ProcessedEvent("event-uuid-001", LocalDateTime.now());

        // When
        processedEventRepository.saveAndFlush(event);

        // Then
        assertThat(processedEventRepository.existsById("event-uuid-001")).isTrue();
    }

    @Test
    @DisplayName("2. Unique Constraint: Should throw exception when inserting duplicate eventId")
    void save_whenDuplicateEventId_shouldThrowException() {
        // Given: First insert succeeds
        ProcessedEvent firstEvent = new ProcessedEvent("event-dup-999", LocalDateTime.now());
        entityManager.persistAndFlush(firstEvent);

        // Detach so Hibernate doesn't optimize it in the first-level cache
        entityManager.detach(firstEvent);

        ProcessedEvent duplicateEvent = new ProcessedEvent("event-dup-999", LocalDateTime.now());

        // When & Then: Force a direct INSERT using TestEntityManager to trigger PK constraint
        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicateEvent))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }
}