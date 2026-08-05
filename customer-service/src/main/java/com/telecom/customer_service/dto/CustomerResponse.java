package com.telecom.customer_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}