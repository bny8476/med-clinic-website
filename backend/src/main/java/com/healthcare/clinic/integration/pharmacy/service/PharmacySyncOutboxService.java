package com.healthcare.clinic.integration.pharmacy.service;

import com.healthcare.clinic.doctor.entity.Prescription;
import com.healthcare.clinic.doctor.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PharmacySyncOutboxService {

    private final PrescriptionRepository prescriptionRepository;
    private final PharmacyIntegrationService pharmacyIntegrationService;

    /**
     * Outbox background retry task.
     * Polls prescriptions marked RETRY_PENDING every 2 minutes and attempts to sync them to the external Pharmacy service.
     */
    @Scheduled(cron = "0 */2 * * * *")
    public void retryPendingPharmacySync() {
        List<Prescription> pendingSync = prescriptionRepository.findAll().stream()
                .filter(p -> "RETRY_PENDING".equalsIgnoreCase(p.getPharmacyStatus()))
                .collect(Collectors.toList());

        if (pendingSync.isEmpty()) {
            return;
        }

        log.info("PharmacySyncOutboxWorker: Found {} prescriptions requiring external pharmacy sync retry.", pendingSync.size());
        for (Prescription rx : pendingSync) {
            try {
                log.info("Retrying external pharmacy sync for Prescription #{}", rx.getId());
                pharmacyIntegrationService.sendPrescriptionToPharmacy(rx.getId());
            } catch (Exception e) {
                log.warn("Retry failed for Prescription #{}: {}", rx.getId(), e.getMessage());
            }
        }
    }
}
