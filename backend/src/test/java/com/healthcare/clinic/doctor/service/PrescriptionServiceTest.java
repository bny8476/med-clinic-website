package com.healthcare.clinic.doctor.service;

import com.healthcare.clinic.clinicaldecision.service.CdsSafetyCheckService;
import com.healthcare.clinic.doctor.dto.PrescriptionItemRequest;
import com.healthcare.clinic.doctor.dto.PrescriptionRequest;
import com.healthcare.clinic.doctor.dto.PrescriptionResponse;
import com.healthcare.clinic.doctor.entity.Prescription;
import com.healthcare.clinic.doctor.repository.PrescriptionRepository;
import com.healthcare.clinic.identity.entity.User;
import com.healthcare.clinic.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.healthcare.clinic.laboratory.repository.LabTestCatalogRepository;
import com.healthcare.clinic.laboratory.repository.LabTestRequestRepository;
import com.healthcare.clinic.patient.repository.PatientProfileRepository;
import com.healthcare.clinic.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private com.healthcare.clinic.doctor.repository.PrescriptionRepository prescriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CdsSafetyCheckService cdsSafetyCheckService;

    @Mock
    private com.healthcare.clinic.clinicaldecision.service.DrugInteractionService drugInteractionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private LabTestCatalogRepository labTestCatalogRepository;

    @Mock
    private LabTestRequestRepository labTestRequestRepository;

    @Mock
    private com.healthcare.clinic.doctor.repository.DoctorProfileRepository doctorProfileRepository;

    @Mock
    private com.healthcare.clinic.branch.repository.BranchRepository branchRepository;

    @Mock
    private com.healthcare.clinic.doctor.repository.ClinicOutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PrescriptionService prescriptionService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(10L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testSafetyCheckBlockingLogic() {
        // Arrange
        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientId(1L);
        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setMedicationName("Amoxicillin");
        request.setItems(List.of(item));

        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> {
            Prescription p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        // Act
        prescriptionService.createPrescription(request);

        // Assert
        verify(cdsSafetyCheckService, times(1)).performSynchronousSafetyCheck(eq(1L), eq(List.of("Amoxicillin")), eq(10L));
    }

    @Test
    void testVoidWindowExpiry_FailsIfAfter15Minutes() {
        // Arrange
        Prescription prescription = new Prescription();
        prescription.setId(100L);
        prescription.setDoctorId(10L);
        // Created 16 minutes ago
        prescription.setCreatedAt(LocalDateTime.now().minusMinutes(16));

        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            prescriptionService.voidPrescription(100L, "Mistake");
        });

        assertEquals("Cannot void prescription after 15 minutes", exception.getMessage());
        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void testVoidWindowExpiry_SucceedsIfWithin15Minutes() {
        // Arrange
        Prescription prescription = new Prescription();
        prescription.setId(100L);
        prescription.setDoctorId(10L);
        // Created 5 minutes ago
        prescription.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PrescriptionResponse response = prescriptionService.voidPrescription(100L, "Mistake");

        // Assert
        assertNotNull(prescription.getVoidedAt());
        assertEquals("Mistake", prescription.getVoidReason());
        assertEquals("VOIDED", prescription.getPharmacyStatus());
        assertEquals("VOIDED", response.getPharmacyStatus());
        verify(prescriptionRepository, times(1)).save(prescription);
    }

    @Test
    void testPharmacyRecordCreationTransaction() {
        // Arrange
        Prescription prescription = new Prescription();
        prescription.setId(100L);
        prescription.setDoctorId(10L);
        prescription.setPatientId(1L);
        prescription.setPharmacyStatus("DRAFT");
        
        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArgument(0));
        
        User patient = new User();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));

        User doctor = new User();
        doctor.setFirstName("Dr.");
        doctor.setLastName("Smith");
        when(userRepository.findById(10L)).thenReturn(Optional.of(doctor));

        // Act
        prescriptionService.sendPrescription(100L, null);

        // Assert
        ArgumentCaptor<com.healthcare.clinic.doctor.entity.ClinicOutboxEvent> outboxCaptor = 
                ArgumentCaptor.forClass(com.healthcare.clinic.doctor.entity.ClinicOutboxEvent.class);
        
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        
        com.healthcare.clinic.doctor.entity.ClinicOutboxEvent savedEvent = outboxCaptor.getValue();
        assertEquals("PRESCRIPTION_SENT", savedEvent.getEventType());
        assertTrue(savedEvent.getPayload().contains("John Doe"));
        assertTrue(savedEvent.getPayload().contains("Dr. Smith"));
    }
}
