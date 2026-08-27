package com.healthcare.clinic.integration.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyStockAvailabilityDto {
    private String medicineId;
    private String medicineName;
    private Boolean inStock;
    private Integer availableQuantity;
    private String batchNumber;
    private LocalDate expiryDate;
    private String warehouseLocation;
}
