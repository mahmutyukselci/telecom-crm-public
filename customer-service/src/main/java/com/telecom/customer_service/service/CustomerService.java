package com.telecom.customer_service.service;

import com.telecom.customer_service.exception.CustomerNotFoundException;
import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.outbox.OutboxService;
import com.telecom.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String AGGREGATE_TYPE_CUSTOMER = "CUSTOMER";
    private static final String TOPIC_CUSTOMER_CREATED = "customer-created-topic";
    private static final String TOPIC_CUSTOMER_UPDATED = "customer-updated-topic";
    private static final String TOPIC_CUSTOMER_DELETED = "customer-deleted-topic";

    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;

    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Creating new customer with ID: {}", customer.getId());
        Customer savedCustomer = customerRepository.save(customer);

        // Persist outbox event within the same DB transaction
        outboxService.saveEvent(
                AGGREGATE_TYPE_CUSTOMER,
                savedCustomer.getId(),
                "CUSTOMER_CREATED",
                TOPIC_CUSTOMER_CREATED,
                savedCustomer
        );

        log.info("Customer created and outbox event recorded successfully for ID: {}", savedCustomer.getId());
        return savedCustomer;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer findCustomerById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional
    public Customer updateCustomer(Customer customer) {
        log.info("Updating customer with ID: {}", customer.getId());
        Customer updatedCustomer = customerRepository.save(customer);

        // Persist update event to outbox table
        outboxService.saveEvent(
                AGGREGATE_TYPE_CUSTOMER,
                updatedCustomer.getId(),
                "CUSTOMER_UPDATED",
                TOPIC_CUSTOMER_UPDATED,
                updatedCustomer
        );

        log.info("Customer updated and outbox event recorded successfully for ID: {}", updatedCustomer.getId());
        return updatedCustomer;
    }

    @Transactional
    public void deleteCustomer(String id) {
        log.info("Deleting customer with ID: {}", id);
        Customer customer = findCustomerById(id);
        customerRepository.delete(customer);

        // Persist deletion event to outbox table
        outboxService.saveEvent(
                AGGREGATE_TYPE_CUSTOMER,
                id,
                "CUSTOMER_DELETED",
                TOPIC_CUSTOMER_DELETED,
                customer
        );

        log.info("Customer deleted and outbox event recorded successfully for ID: {}", id);
    }
}