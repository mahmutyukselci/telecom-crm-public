package com.telecom.catalog_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tariffs")
public class Tariff {

    @Id
    private String id;

    // Core details of the telecom tariff
    private String name;
    private String description;
    private BigDecimal price;

    // Package limits
    private Integer dataLimitGb;
    private Integer voiceLimitMinutes;
    private Integer smsLimit;

    // Status flag to control market visibility
    private boolean isActive;

    private Integer validityDays;
}