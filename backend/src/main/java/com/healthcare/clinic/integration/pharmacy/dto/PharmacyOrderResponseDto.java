package com.healthcare.clinic.integration.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyOrderResponseDto {
    private String pharmacyReferenceId;
    private Long clinicPrescriptionId;
    private String status; // PENDING, ACCEPTED, PROCESSING, DISPENSED, REJECTED, OUT_OF_STOCK
    private LocalDateTime receivedAt;
    private String notes;
}
