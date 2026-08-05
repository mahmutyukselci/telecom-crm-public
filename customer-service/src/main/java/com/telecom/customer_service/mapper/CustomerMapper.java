package com.telecom.customer_service.mapper;

import com.telecom.customer_service.dto.CustomerRequest;
import com.telecom.customer_service.dto.CustomerResponse;
import com.telecom.customer_service.model.Customer;
import org.mapstruct.*;
;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Customer toEntity(CustomerRequest dto);

    CustomerResponse toResponse(Customer customer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateCustomerFromDto(CustomerRequest dto, @MappingTarget Customer customer);
}