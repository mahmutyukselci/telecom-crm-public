package com.telecom.customer_service;

import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.repository.CustomerRepository;
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

import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ========================================================================
    // 1. CUSTOM QUERY & NEGATIVE CASES
    // ========================================================================

    @Test
    @DisplayName("findByKeycloakUserId: Should return customer when Keycloak user ID exists")
    void findByKeycloakUserId_whenExists_shouldReturnCustomer() {
        // Given
        Customer customer = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@telecom.com")
                .phone("5551112233")
                .keycloakUserId("kc-existing-id-100")
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(customer);

        // When
        Optional<Customer> found = customerRepository.findByKeycloakUserId("kc-existing-id-100");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@telecom.com");
    }

    @Test
    @DisplayName("findByKeycloakUserId: Should return empty Optional when Keycloak user ID does not exist")
    void findByKeycloakUserId_whenNotFound_shouldReturnEmpty() {
        // When
        Optional<Customer> found = customerRepository.findByKeycloakUserId("non-existent-kc-id");

        // Then
        assertThat(found).isEmpty();
    }

    // ========================================================================
    // 2. UNIQUE CONSTRAINTS (KEYCLOAK ID & EMAIL)
    // ========================================================================

    @Test
    @DisplayName("save: Should throw exception when inserting duplicate Keycloak user ID")
    void save_whenDuplicateKeycloakId_shouldThrowException() {
        // Given
        Customer firstCustomer = Customer.builder()
                .firstName("User1").lastName("Test").email("user1@telecom.com")
                .phone("5551110001").keycloakUserId("kc-unique-id").build();

        entityManager.persistAndFlush(firstCustomer);
        entityManager.detach(firstCustomer);

        Customer duplicateCustomer = Customer.builder()
                .firstName("User2").lastName("Test").email("user2@telecom.com")
                .phone("5551110002").keycloakUserId("kc-unique-id").build();

        // When & Then
        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicateCustomer))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("save: Should throw exception when inserting duplicate email address")
    void save_whenDuplicateEmail_shouldThrowException() {
        // Given
        Customer firstCustomer = Customer.builder()
                .firstName("Alice").lastName("Smith").email("unique.email@telecom.com")
                .phone("5552220001").keycloakUserId("kc-alice-id").build();

        entityManager.persistAndFlush(firstCustomer);
        entityManager.detach(firstCustomer);

        Customer duplicateEmailCustomer = Customer.builder()
                .firstName("Bob").lastName("Smith").email("unique.email@telecom.com")
                .phone("5552220002").keycloakUserId("kc-bob-id").build();

        // When & Then
        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicateEmailCustomer))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    // ========================================================================
    // 3. NULL CONSTRAINTS (MANDATORY FIELDS)
    // ========================================================================

    @Test
    @DisplayName("save: Should throw exception when mandatory field email is null")
    void save_whenEmailIsNull_shouldThrowException() {
        // Given
        Customer customerWithNullEmail = Customer.builder()
                .firstName("Invalid").lastName("User").email(null)
                .phone("5553330000").keycloakUserId("kc-null-email-id").build();


        assertThatThrownBy(() -> entityManager.persistAndFlush(customerWithNullEmail))
                .isInstanceOfAny(
                        DataIntegrityViolationException.class,
                        PersistenceException.class,
                        jakarta.validation.ConstraintViolationException.class
                );
    }

    // ========================================================================
    // 4. UPDATE & DIRTY CHECKING LIFECYCLE
    // ========================================================================

    @Test
    @DisplayName("update: Should successfully persist modified customer fields to database")
    void update_shouldPersistModifiedFields() {
        // Given
        Customer customer = Customer.builder()
                .firstName("OldName").lastName("Doe").email("old.name@telecom.com")
                .phone("5554440000").keycloakUserId("kc-update-id").build();

        Customer saved = entityManager.persistAndFlush(customer);

        // When
        saved.setFirstName("NewName");
        saved.setEmail("updated.name@telecom.com");
        Customer updated = entityManager.persistAndFlush(saved);

        // Then
        entityManager.clear(); // Clear first-level cache to force a fresh SELECT from DB
        Customer foundInDb = customerRepository.findById(updated.getId()).orElseThrow();

        assertThat(foundInDb.getFirstName()).isEqualTo("NewName");
        assertThat(foundInDb.getEmail()).isEqualTo("updated.name@telecom.com");
    }
}