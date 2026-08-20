package com.telecom.customer_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers", schema = "customer_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    private LocalDateTime createdAt;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "First name must not be blank")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name must not be blank")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid format")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "keycloak_user_id", unique = true)
    private String keycloakUserId;

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Phone number must be between 10 and 15 digits"
    )
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
}