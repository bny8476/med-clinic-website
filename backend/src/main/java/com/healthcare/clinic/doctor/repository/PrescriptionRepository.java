package com.healthcare.clinic.doctor.repository;

import com.healthcare.clinic.doctor.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("doctorPrescriptionRepository")
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Prescription> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
    List<Prescription> findBySignedAtAfterAndPharmacyStatusNot(LocalDateTime date, String status);
    List<Prescription> findByEncounterId(Long encounterId);
    List<Prescription> findByPharmacyStatusInOrderByCreatedAtDesc(List<String> pharmacyStatuses);
    List<Prescription> findByPharmacyStatusOrderByCreatedAtDesc(String pharmacyStatus);
    List<Prescription> findByPharmacyStatus(String pharmacyStatus);
    java.util.Optional<Prescription> findByPharmacyReferenceId(String pharmacyReferenceId);
    long countByStatus(String status);

    @Modifying
    @Query("UPDATE Prescription p SET p.pharmacyStatus = 'ACCEPTED', p.assignedPharmacyUserId = :pharmacistId, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.pharmacyStatus = 'PENDING' AND (p.assignedPharmacyUserId IS NULL OR p.assignedPharmacyUserId = :pharmacistId)")
    int claimPrescription(@Param("id") Long id, @Param("pharmacistId") Long pharmacistId);

    @Modifying
    @Query("UPDATE Prescription p SET p.pharmacyStatus = 'PROCESSING', p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.pharmacyStatus = 'ACCEPTED' AND p.assignedPharmacyUserId = :pharmacistId")
    int startProcessingPrescription(@Param("id") Long id, @Param("pharmacistId") Long pharmacistId);

    @Modifying
    @Query("UPDATE Prescription p SET p.pharmacyStatus = 'DISPENSED', p.dispensedAt = CURRENT_TIMESTAMP, p.dispensedBy = :dispensedBy, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.pharmacyStatus IN ('ACCEPTED', 'PROCESSING') AND p.assignedPharmacyUserId = :pharmacistId")
    int dispensePrescription(@Param("id") Long id, @Param("pharmacistId") Long pharmacistId, @Param("dispensedBy") String dispensedBy);

    @Modifying
    @Query("UPDATE Prescription p SET p.pharmacyStatus = 'REJECTED', p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.pharmacyStatus IN ('PENDING', 'ACCEPTED', 'PROCESSING') AND (p.assignedPharmacyUserId IS NULL OR p.assignedPharmacyUserId = :pharmacistId)")
    int rejectPrescription(@Param("id") Long id, @Param("pharmacistId") Long pharmacistId);
}
