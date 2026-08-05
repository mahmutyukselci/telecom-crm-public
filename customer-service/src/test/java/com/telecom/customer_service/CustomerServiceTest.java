package com.telecom.customer_service;

import com.telecom.customer_service.exception.CustomerNotFoundException;
import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.outbox.OutboxService;
import com.telecom.customer_service.repository.CustomerRepository;
import com.telecom.customer_service.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("createCustomer: Should successfully save customer and record outbox event")
    void createCustomer_shouldSaveCustomerAndRecordOutboxEvent() {
        // Given
        Customer customer = Customer.builder()
                .id("cust-100")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@telecom.com")
                .phone("5551234567")
                .build();

        when(customerRepository.save(customer)).thenReturn(customer);

        // When
        Customer saved = customerService.createCustomer(customer);

        // Then
        assertThat(saved.getEmail()).isEqualTo("john.doe@telecom.com");
        verify(customerRepository, times(1)).save(customer);
        verify(outboxService, times(1)).saveEvent(
                eq("CUSTOMER"),
                eq("cust-100"),
                eq("CUSTOMER_CREATED"),
                eq("customer-created-topic"),
                any(Customer.class)
        );
    }

    @Test
    @DisplayName("findCustomerById: Should return customer when ID exists")
    void findCustomerById_whenExists_shouldReturnCustomer() {
        // Given
        String customerId = "cust-100";
        Customer customer = Customer.builder().id(customerId).firstName("Alice").build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // When
        Customer found = customerService.findCustomerById(customerId);

        // Then
        assertThat(found.getId()).isEqualTo(customerId);
        assertThat(found.getFirstName()).isEqualTo("Alice");
        verify(customerRepository, times(1)).findById(customerId);
    }

    @Test
    @DisplayName("findCustomerById: Should throw CustomerNotFoundException when ID does not exist")
    void findCustomerById_whenNotFound_shouldThrowException() {
        // Given
        String customerId = "missing-id";
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customerService.findCustomerById(customerId))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, times(1)).findById(customerId);
    }

    @Test
    @DisplayName("getAllCustomers: Should return list of all registered customers")
    void getAllCustomers_shouldReturnCustomerList() {
        // Given
        Customer c1 = Customer.builder().id("1").firstName("John").build();
        Customer c2 = Customer.builder().id("2").firstName("Jane").build();

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        // When
        List<Customer> result = customerService.getAllCustomers();

        // Then
        assertThat(result).hasSize(2);
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("deleteCustomer: Should find customer, remove it from repository, and record outbox event")
    void deleteCustomer_whenExists_shouldDeleteCustomerAndRecordOutboxEvent() {
        // Given
        String customerId = "cust-to-delete";
        Customer customer = Customer.builder().id(customerId).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doNothing().when(customerRepository).delete(customer);

        // When
        customerService.deleteCustomer(customerId);

        // Then
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).delete(customer);
        verify(outboxService, times(1)).saveEvent(
                eq("CUSTOMER"),
                eq(customerId),
                eq("CUSTOMER_DELETED"),
                eq("customer-deleted-topic"),
                eq(customer)
        );
    }

    @Test
    @DisplayName("updateCustomer: Should save updated customer and record outbox event")
    void updateCustomer_shouldSaveCustomerAndRecordOutboxEvent() {
        // Given
        Customer customer = Customer.builder()
                .id("cust-100")
                .firstName("John-Updated")
                .email("john@telecom.com")
                .build();

        when(customerRepository.save(customer)).thenReturn(customer);

        // When
        Customer updated = customerService.updateCustomer(customer);

        // Then
        assertThat(updated.getFirstName()).isEqualTo("John-Updated");
        verify(customerRepository, times(1)).save(customer);
        verify(outboxService, times(1)).saveEvent(
                eq("CUSTOMER"),
                eq("cust-100"),
                eq("CUSTOMER_UPDATED"),
                eq("customer-updated-topic"),
                any(Customer.class)
        );
    }

    @Test
    @DisplayName("deleteCustomer: Should throw CustomerNotFoundException when ID does not exist")
    void deleteCustomer_whenNotFound_shouldThrowException() {
        // Given
        String missingId = "missing-del-id";
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customerService.deleteCustomer(missingId))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, times(1)).findById(missingId);
        verify(customerRepository, never()).delete(any());
        verifyNoInteractions(outboxService);
    }
}