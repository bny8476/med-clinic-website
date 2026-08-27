package com.healthcare.clinic.doctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Set by pharmacy when prescription is dispensed/verified */
    @Builder.Default
    @Column(name = "pharmacy_status")
    private String pharmacyStatus = "NOT_SENT";

    @Column(name = "pharmacy_reference_id")
    private String pharmacyReferenceId;

    @Column(name = "sent_to_pharmacy_at")
    private LocalDateTime sentToPharmacyAt;

    @Column(name = "last_pharmacy_status_updated_at")
    private LocalDateTime lastPharmacyStatusUpdatedAt;

    @Column(name = "pharmacy_sync_error", columnDefinition = "TEXT")
    private String pharmacySyncError;

    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    @Column(name = "dispensed_by")
    private String dispensedBy;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrescriptionItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "assigned_pharmacy_user_id")
    private Long assignedPharmacyUserId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Builder.Default
    @Column(nullable = false)
    private String status = "Draft"; // Draft, Signed, Void, Cancelled

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_hash")
    private String signatureHash;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "diagnosis_id")
    private Long diagnosisId;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Builder.Default
    @Column(name = "refills_allowed")
    private Integer refillsAllowed = 0;

    @Builder.Default
    @Column(name = "refills_remaining")
    private Integer refillsRemaining = 0;

    @Builder.Default
    @Column(name = "refill_interval_days")
    private Integer refillIntervalDays = 0;

    @Column(name = "doctor_registration_number")
    private String doctorRegistrationNumber;

    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }
}
