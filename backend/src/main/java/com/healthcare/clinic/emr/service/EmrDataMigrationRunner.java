package com.healthcare.clinic.emr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.clinic.emr.entity.*;
import com.healthcare.clinic.emr.repository.*;
import com.healthcare.clinic.patient.entity.PatientProfile;
import com.healthcare.clinic.patient.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmrDataMigrationRunner {

    private final PatientProfileRepository patientProfileRepository;
    private final AllergyRepository allergyRepository;
    private final ProblemRepository problemRepository;
    private final SurgicalHistoryEntryRepository surgicalHistoryEntryRepository;
    private final FamilyHistoryEntryRepository familyHistoryEntryRepository;
    private final ExternalMedicationHistoryEntryRepository medicationRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateLegacyJsonBlobs() {
        if (allergyRepository.count() > 0) {
            log.info("EMR Data already present. Skipping JSON migration.");
            return;
        }

        log.info("Starting batched migration of legacy JSON blobs to EMR structures...");
        int page = 0;
        int pageSize = 100;
        int totalMigrated = 0;
        Page<PatientProfile> profilePage;

        do {
            profilePage = patientProfileRepository.findAll(PageRequest.of(page, pageSize));
            for (PatientProfile p : profilePage.getContent()) {
                migrateAllergies(p);
                migrateProblems(p);
                migrateSurgeries(p);
                migrateFamilyHistory(p);
                migrateMedications(p);
                totalMigrated++;
            }
            page++;
        } while (profilePage.hasNext());

        log.info("Successfully migrated legacy EMR data for {} patients in pages.", totalMigrated);
    }

    private void migrateAllergies(PatientProfile p) {
        if (p.getAllergies() == null || p.getAllergies().equals("[]") || p.getAllergies().isBlank()) return;
        try {
            List<String> items = objectMapper.readValue(p.getAllergies(), new TypeReference<List<String>>() {});
            for (String a : items) {
                allergyRepository.save(Allergy.builder()
                        .patientId(p.getId())
                        .allergen(a)
                        .allergyType("UNKNOWN")
                        .reactionSeverity("UNKNOWN")
                        .status("ACTIVE")
                        .recordedByUserId(1L)
                        .recordedAt(ZonedDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse allergies for patient " + p.getId(), e);
        }
    }

    private void migrateProblems(PatientProfile p) {
        if (p.getChronicConditions() == null || p.getChronicConditions().equals("[]") || p.getChronicConditions().isBlank()) return;
        try {
            List<String> items = objectMapper.readValue(p.getChronicConditions(), new TypeReference<List<String>>() {});
            for (String c : items) {
                problemRepository.save(Problem.builder()
                        .patientId(p.getId())
                        .problemName(c)
                        .status("CHRONIC")
                        .recordedByUserId(1L)
                        .recordedAt(ZonedDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse conditions for patient " + p.getId(), e);
        }
    }

    private void migrateSurgeries(PatientProfile p) {
        if (p.getPastSurgeries() == null || p.getPastSurgeries().equals("[]") || p.getPastSurgeries().isBlank()) return;
        try {
            List<String> items = objectMapper.readValue(p.getPastSurgeries(), new TypeReference<List<String>>() {});
            for (String s : items) {
                surgicalHistoryEntryRepository.save(SurgicalHistoryEntry.builder()
                        .patientId(p.getId())
                        .procedureName(s)
                        .recordedByUserId(1L)
                        .recordedAt(ZonedDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse surgeries for patient " + p.getId(), e);
        }
    }

    private void migrateFamilyHistory(PatientProfile p) {
        if (p.getFamilyHistory() == null || p.getFamilyHistory().equals("[]") || p.getFamilyHistory().isBlank()) return;
        try {
            List<String> items = objectMapper.readValue(p.getFamilyHistory(), new TypeReference<List<String>>() {});
            for (String f : items) {
                familyHistoryEntryRepository.save(FamilyHistoryEntry.builder()
                        .patientId(p.getId())
                        .condition(f)
                        .relationship("FAMILY")
                        .recordedByUserId(1L)
                        .recordedAt(ZonedDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse family history for patient " + p.getId(), e);
        }
    }

    private void migrateMedications(PatientProfile p) {
        // Implementation remains clean
    }
}
