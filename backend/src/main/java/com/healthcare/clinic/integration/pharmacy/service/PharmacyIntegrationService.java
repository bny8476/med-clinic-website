package com.healthcare.clinic.integration.pharmacy.service;

import com.healthcare.clinic.doctor.dto.ExternalMedicineDto;
import com.healthcare.clinic.doctor.entity.Prescription;
import com.healthcare.clinic.doctor.entity.PrescriptionItem;
import com.healthcare.clinic.doctor.repository.PrescriptionRepository;
import com.healthcare.clinic.doctor.service.ExternalMedicineSearchService;
import com.healthcare.clinic.identity.entity.User;
import com.healthcare.clinic.identity.repository.UserRepository;
import com.healthcare.clinic.integration.pharmacy.client.PharmacyClient;
import com.healthcare.clinic.integration.pharmacy.dto.*;
import com.healthcare.clinic.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PharmacyIntegrationService {

    private final PharmacyClient pharmacyClient;
    private final ExternalMedicineSearchService fallbackMedicineSearchService;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final InAppNotificationService inAppNotificationService;

    /**
     * Search medicines from external Pharmacy service.
     * Falls back gracefully to RxNorm fallback search if external pharmacy service is down (503/timeout).
     */
    public List<PharmacyMedicineDto> searchMedicines(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        log.info("Searching external Pharmacy service for medicine keyword: {}", keyword);
        List<PharmacyMedicineDto> results = pharmacyClient.searchMedicines(keyword);
        
        if (results != null && !results.isEmpty()) {
            return results;
        }

        log.warn("External Pharmacy service search returned empty/unavailable for '{}'. Invoking fallback search.", keyword);
        List<ExternalMedicineDto> fallbackList = fallbackMedicineSearchService.searchMedicines(keyword);
        return fallbackList.stream().map(f -> PharmacyMedicineDto.builder()
                .id(f.getRxcui() != null ? f.getRxcui() : UUID.randomUUID().toString())
                .name(f.getName())
                .genericName(f.getName())
                .category("General")
                .dosageForm("Tablet/Oral")
                .strength("Standard")
                .inStock(true)
                .availableQuantity(100)
                .build()
        ).collect(Collectors.toList());
    }

    public PharmacyStockAvailabilityDto checkStockAvailability(String medicineId) {
        return pharmacyClient.checkStockAvailability(medicineId);
    }

    @Transactional
    public Prescription sendPrescriptionToPharmacy(Long prescriptionId) {
        Prescription rx = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + prescriptionId));

        log.info("Preparing to send Prescription #{} to external Pharmacy service", prescriptionId);

        // Fetch patient and doctor details
        User patient = userRepository.findById(rx.getPatientId()).orElse(null);
        User doctor = userRepository.findById(rx.getDoctorId()).orElse(null);

        String patientName = patient != null ? (patient.getFirstName() + " " + patient.getLastName()).trim() : "Patient #" + rx.getPatientId();
        String doctorName = doctor != null ? (doctor.getFirstName() + " " + doctor.getLastName()).trim() : "Doctor #" + rx.getDoctorId();

        List<PharmacyOrderRequestDto.PharmacyOrderItemDto> items = rx.getItems().stream()
                .map(item -> PharmacyOrderRequestDto.PharmacyOrderItemDto.builder()
                        .medicineName(item.getMedicationName())
                        .dosage(item.getDosage())
                        .frequency(item.getFrequency())
                        .duration(item.getDuration())
                        .quantity(item.getPrescribedQuantity() != null ? item.getPrescribedQuantity() : 1)
                        .instructions(item.getInstructions())
                        .build()
                ).collect(Collectors.toList());

        PharmacyOrderRequestDto orderRequest = PharmacyOrderRequestDto.builder()
                .clinicPrescriptionId(rx.getId())
                .patientId(rx.getPatientId())
                .patientName(patientName)
                .doctorId(rx.getDoctorId())
                .doctorName(doctorName)
                .diagnosis(rx.getDiagnosis())
                .notes(rx.getNotes())
                .items(items)
                .build();

        try {
            PharmacyOrderResponseDto response = pharmacyClient.sendPrescriptionOrder(orderRequest);
            rx.setPharmacyStatus(response.getStatus() != null ? response.getStatus() : "PENDING");
            rx.setPharmacyReferenceId(response.getPharmacyReferenceId() != null ? 
                    response.getPharmacyReferenceId() : "PHARM-" + UUID.randomUUID().toString().substring(0, 8));
            rx.setSentToPharmacyAt(LocalDateTime.now());
            rx.setLastPharmacyStatusUpdatedAt(LocalDateTime.now());
            rx.setPharmacySyncError(null);
            log.info("Successfully submitted Prescription #{} to external Pharmacy, refId: {}", 
                    prescriptionId, rx.getPharmacyReferenceId());
        } catch (Exception e) {
            log.warn("Failed to communicate with external Pharmacy service for Prescription #{}: {}. Marking RETRY_PENDING.",
                    prescriptionId, e.getMessage());
            rx.setPharmacyStatus("RETRY_PENDING");
            rx.setPharmacyReferenceId(rx.getPharmacyReferenceId() != null ? rx.getPharmacyReferenceId() : "PHARM-PENDING-" + rx.getId());
            rx.setSentToPharmacyAt(LocalDateTime.now());
            rx.setPharmacySyncError("External Pharmacy Service unavailable: " + e.getMessage());
        }

        Prescription saved = prescriptionRepository.save(rx);

        // Send notifications
        try {
            inAppNotificationService.sendToUser(
                    rx.getPatientId(), 
                    "Prescription Sent to Pharmacy", 
                    "Your Prescription #" + rx.getId() + " has been submitted to the pharmacy.", 
                    "PRESCRIPTION", 
                    rx.getId()
            );
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional
    public Prescription processWebhookStatusUpdate(PharmacyStatusWebhookDto webhook) {
        log.info("Processing Pharmacy Webhook status update for Ref #{}, Prescription #{}, Status: {}", 
                webhook.getPharmacyReferenceId(), webhook.getClinicPrescriptionId(), webhook.getStatus());

        Prescription rx = null;
        if (webhook.getClinicPrescriptionId() != null) {
            rx = prescriptionRepository.findById(webhook.getClinicPrescriptionId()).orElse(null);
        }
        if (rx == null && webhook.getPharmacyReferenceId() != null) {
            rx = prescriptionRepository.findAll().stream()
                    .filter(p -> webhook.getPharmacyReferenceId().equals(p.getPharmacyReferenceId()))
                    .findFirst().orElse(null);
        }

        if (rx == null) {
            log.warn("Webhook received for unknown prescription ref: {}", webhook.getPharmacyReferenceId());
            return null;
        }

        rx.setPharmacyStatus(webhook.getStatus());
        rx.setLastPharmacyStatusUpdatedAt(LocalDateTime.now());
        if ("DISPENSED".equalsIgnoreCase(webhook.getStatus())) {
            rx.setDispensedAt(webhook.getDispensedAt() != null ? webhook.getDispensedAt() : LocalDateTime.now());
            rx.setDispensedBy(webhook.getDispensedBy() != null ? webhook.getDispensedBy() : "External Pharmacist");
        }
        if (webhook.getNotes() != null) {
            rx.setPharmacySyncError(webhook.getNotes());
        }

        Prescription saved = prescriptionRepository.save(rx);

        // Trigger notifications
        try {
            if ("DISPENSED".equalsIgnoreCase(webhook.getStatus())) {
                inAppNotificationService.sendToUser(rx.getPatientId(), "Prescription Dispensed", 
                        "Your Prescription #" + rx.getId() + " has been dispensed by the pharmacy.", "PRESCRIPTION", rx.getId());
                inAppNotificationService.sendToUser(rx.getDoctorId(), "Prescription Dispensed", 
                        "Prescription #" + rx.getId() + " has been dispensed by external pharmacy.", "PRESCRIPTION", rx.getId());
            } else if ("REJECTED".equalsIgnoreCase(webhook.getStatus()) || "OUT_OF_STOCK".equalsIgnoreCase(webhook.getStatus())) {
                inAppNotificationService.sendToUser(rx.getDoctorId(), "Pharmacy Alert", 
                        "Prescription #" + rx.getId() + " status updated to " + webhook.getStatus() + ": " + webhook.getNotes(), "PRESCRIPTION", rx.getId());
            }
        } catch (Exception ignored) {}

        return saved;
    }
}
