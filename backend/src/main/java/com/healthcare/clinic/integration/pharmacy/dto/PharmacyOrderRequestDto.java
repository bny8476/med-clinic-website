package com.healthcare.clinic.integration.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyOrderRequestDto {
    private Long clinicPrescriptionId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorRegistrationNumber;
    private String diagnosis;
    private String notes;
    private List<PharmacyOrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PharmacyOrderItemDto {
        private String medicineName;
        private String externalMedicineId;
        private String dosage;
        private String frequency;
        private String duration;
        private Integer quantity;
        private String instructions;
    }
}
