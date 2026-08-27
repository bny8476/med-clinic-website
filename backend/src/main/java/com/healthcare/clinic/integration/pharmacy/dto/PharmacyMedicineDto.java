package com.healthcare.clinic.integration.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyMedicineDto {
    private String id;
    private String name;
    private String genericName;
    private String category;
    private String dosageForm;
    private String strength;
    private BigDecimal price;
    private Integer availableQuantity;
    private Boolean inStock;
    private String manufacturer;
}
