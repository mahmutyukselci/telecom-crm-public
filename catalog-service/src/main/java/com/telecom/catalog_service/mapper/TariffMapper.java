package com.telecom.catalog_service.mapper;

import com.telecom.catalog_service.dto.TariffRequest;
import com.telecom.catalog_service.dto.TariffResponse;
import com.telecom.catalog_service.model.Tariff;
import org.mapstruct.*;

// componentModel = "spring" allows us to inject this mapper using @RequiredArgsConstructor
@Mapper(componentModel = "spring")
public interface TariffMapper {

    // Ignores ID mapping and sets default active status to false for new tariffs
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "false")
    Tariff toEntity(TariffRequest request);

    @Mapping(source = "active", target = "isActive")
    TariffResponse toResponse(Tariff tariff);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateTariffFromRequest(TariffRequest request, @MappingTarget Tariff tariff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateTariffFromRequestFull(TariffRequest request, @MappingTarget Tariff tariff);
}