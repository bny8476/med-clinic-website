package com.healthcare.clinic.integration.pharmacy.controller;

import com.healthcare.clinic.doctor.entity.Prescription;
import com.healthcare.clinic.doctor.repository.PrescriptionRepository;
import com.healthcare.clinic.integration.pharmacy.dto.PharmacyMedicineDto;
import com.healthcare.clinic.integration.pharmacy.dto.PharmacyStockAvailabilityDto;
import com.healthcare.clinic.integration.pharmacy.service.PharmacyIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/integrations/pharmacy")
@RequiredArgsConstructor
public class PharmacyIntegrationController {

    private final PharmacyIntegrationService pharmacyIntegrationService;
    private final PrescriptionRepository prescriptionRepository;

    @GetMapping("/medicines/search")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_PHARMACIST') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PharmacyMedicineDto>> searchMedicines(@RequestParam String keyword) {
        return ResponseEntity.ok(pharmacyIntegrationService.searchMedicines(keyword));
    }

    @GetMapping("/medicines/{medicineId}/availability")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_PHARMACIST') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PharmacyStockAvailabilityDto> checkStockAvailability(@PathVariable String medicineId) {
        return ResponseEntity.ok(pharmacyIntegrationService.checkStockAvailability(medicineId));
    }

    @PostMapping("/prescriptions/{id}/send")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Prescription> sendPrescriptionToPharmacy(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyIntegrationService.sendPrescriptionToPharmacy(id));
    }

    @PostMapping("/prescriptions/{id}/retry-sync")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Prescription> retryPrescriptionSync(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyIntegrationService.sendPrescriptionToPharmacy(id));
    }

    @GetMapping("/prescriptions/failed-sync")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<Prescription>> getFailedSyncPrescriptions() {
        List<Prescription> failed = prescriptionRepository.findAll().stream()
                .filter(p -> "RETRY_PENDING".equalsIgnoreCase(p.getPharmacyStatus()) || p.getPharmacySyncError() != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(failed);
    }
}
