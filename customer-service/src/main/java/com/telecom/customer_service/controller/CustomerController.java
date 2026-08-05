package com.telecom.customer_service.controller;

import com.telecom.customer_service.dto.CustomerIdentityResponse;
import com.telecom.customer_service.dto.CustomerRequest;
import com.telecom.customer_service.dto.CustomerResponse;
import com.telecom.customer_service.mapper.CustomerMapper;
import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = customerService.createCustomer(customer);
        CustomerResponse response = customerMapper.toResponse(savedCustomer);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();

        List<CustomerResponse> response = customers.stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and @customerSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<CustomerResponse> findCustomerById(@PathVariable String id) {
        Customer customer = customerService.findCustomerById(id);
        return ResponseEntity.ok(customerMapper.toResponse(customer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and @customerSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody CustomerRequest request) {

        Customer existingCustomer = customerService.findCustomerById(id);
        customerMapper.updateCustomerFromDto(request, existingCustomer);
        Customer updatedCustomer = customerService.updateCustomer(existingCustomer);

        return ResponseEntity.ok(customerMapper.toResponse(updatedCustomer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/identity")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and @customerSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<CustomerIdentityResponse> getCustomerIdentity(@PathVariable String id) {
        Customer customer = customerService.findCustomerById(id);
        return ResponseEntity.ok(new CustomerIdentityResponse(customer.getId(), customer.getKeycloakUserId()));
    }
}