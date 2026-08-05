package com.telecom.subscription_service.mapper;

import com.telecom.subscription_service.dto.SubscriptionRequest;
import com.telecom.subscription_service.dto.SubscriptionResponse;
import com.telecom.subscription_service.model.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    Subscription toEntity(SubscriptionRequest request);

    SubscriptionResponse toResponse(Subscription subscription);
}