package com.healthcare.clinic.emergency.entity;

import com.healthcare.clinic.doctor.entity.DoctorProfile;
import com.healthcare.clinic.inpatient.entity.Admission;
import com.healthcare.clinic.patient.entity.PatientProfile;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Entity
@Table(name = "emergency_encounters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EmergencyEncounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientProfile patient; // Can be null if Jane Doe before registration

    @Column(name = "arrival_mode", nullable = false, length = 50)
    private String arrivalMode; // WALK_IN, AMBULANCE, POLICE, OTHER

    @Column(name = "ambulance_request_id")
    private Long ambulanceRequestId;

    @CreatedDate
    @Column(name = "arrived_at", nullable = false, updatable = false)
    private ZonedDateTime arrivedAt;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "REGISTERED"; // REGISTERED, IN_TRIAGE, IN_TREATMENT, DISPOSITIONED

    @Column(length = 50)
    private String disposition; // ADMITTED, DISCHARGED, TRANSFERRED, DECEASED, LAMA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id")
    private Admission admission; // Set if disposition == ADMITTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private DoctorProfile assignedDoctor;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;
}
