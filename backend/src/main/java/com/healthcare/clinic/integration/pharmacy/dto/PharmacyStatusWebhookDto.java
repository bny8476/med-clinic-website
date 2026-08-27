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
public class PharmacyStatusWebhookDto {
    private String pharmacyReferenceId;
    private Long clinicPrescriptionId;
    private String status; // ACCEPTED, PROCESSING, DISPENSED, REJECTED, OUT_OF_STOCK, CANCELLED
    private LocalDateTime dispensedAt;
    private String dispensedBy;
    private String notes;
}
