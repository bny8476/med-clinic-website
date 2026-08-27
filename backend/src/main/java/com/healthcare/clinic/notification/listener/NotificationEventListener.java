package com.healthcare.clinic.notification.listener;

import com.healthcare.clinic.appointment.event.AppointmentBookedEvent;
import com.healthcare.clinic.notification.event.AppointmentCancelledEvent;
import com.healthcare.clinic.notification.event.InvoiceCreatedEvent;
import com.healthcare.clinic.notification.event.LabResultReleasedEvent;
import com.healthcare.clinic.notification.event.QueueTokenCalledEvent;
import com.healthcare.clinic.clinicaldecision.event.PrescriptionCreatedEvent;
import com.healthcare.clinic.notification.service.EmailNotificationService;
import com.healthcare.clinic.notification.service.InAppNotificationService;
import com.healthcare.clinic.notification.service.TwilioNotificationService;
import com.healthcare.clinic.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final InAppNotificationService inApp;
    private final EmailNotificationService email;
    private final TwilioNotificationService twilio;
    private final UserRepository userRepository;

    // ─── Appointment Booked ───────────────────────────────────────────────────

    @Async
    @EventListener
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        log.info("Handling AppointmentBookedEvent for appointment {}", event.getAppointmentId());
        inApp.sendToUser(event.getPatientUserId(),
                "Appointment Confirmed",
                "Your appointment with Dr. " + event.getDoctorName() + " is confirmed for " + event.getStartTime(),
                "APPOINTMENT", event.getAppointmentId());
        email.sendAppointmentConfirmation(event);
        
        userRepository.findById(event.getPatientUserId()).ifPresent(user -> {
            twilio.sendAppointmentConfirmationSms(user.getPhoneNumber(), event.getDoctorName(), event.getStartTime().toString());
        });
        
        inApp.sendToUser(event.getDoctorUserId(),
                "New Appointment",
                "New appointment booked for " + event.getStartTime(),
                "APPOINTMENT", event.getAppointmentId());
        
        inApp.sendToRole("ROLE_ADMIN",
                "New Appointment",
                "New appointment booked with Dr. " + event.getDoctorName(),
                "APPOINTMENT", event.getAppointmentId());
    }

    // ─── Appointment Cancelled ────────────────────────────────────────────────

    @Async
    @EventListener
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Handling AppointmentCancelledEvent for appointment {}", event.getAppointmentId());
        inApp.sendToUser(event.getPatientUserId(),
                "Appointment Cancelled",
                "Your appointment with Dr. " + event.getDoctorName() + " on " + event.getStartTime() + " has been cancelled.",
                "APPOINTMENT", event.getAppointmentId());
        email.sendAppointmentCancellation(event);
        
        userRepository.findById(event.getPatientUserId()).ifPresent(user -> {
            twilio.sendAppointmentCancellationSms(user.getPhoneNumber(), event.getDoctorName(), event.getStartTime().toString());
        });
        
        inApp.sendToUser(event.getDoctorUserId(),
                "Appointment Cancelled",
                "Appointment on " + event.getStartTime() + " has been cancelled.",
                "APPOINTMENT", event.getAppointmentId());
                
        inApp.sendToRole("ROLE_ADMIN",
                "Appointment Cancelled",
                "Appointment with Dr. " + event.getDoctorName() + " on " + event.getStartTime() + " cancelled.",
                "APPOINTMENT", event.getAppointmentId());
    }

    // ─── Invoice Created ──────────────────────────────────────────────────────

    @Async
    @EventListener
    public void onInvoiceCreated(InvoiceCreatedEvent event) {
        log.info("Handling InvoiceCreatedEvent for invoice {}", event.getInvoiceId());
        inApp.sendToUser(event.getPatientId(),
                "New Invoice: " + event.getInvoiceNumber(),
                "You have a new invoice for ₹" + event.getTotalAmount() + ". Please log in to view and pay.",
                "INVOICE", event.getInvoiceId());
        email.sendInvoiceCreated(event);
        
        userRepository.findById(event.getPatientId()).ifPresent(user -> {
            twilio.sendInvoiceCreatedSms(user.getPhoneNumber(), event.getInvoiceNumber(), event.getTotalAmount().toString(), event.getDueDate().toString());
        });
    }

    // ─── Lab Result Released ──────────────────────────────────────────────────

    @Async
    @EventListener
    public void onLabResultReleased(LabResultReleasedEvent event) {
        log.info("Handling LabResultReleasedEvent for request {}", event.getRequestId());
        inApp.sendToUser(event.getPatientId(),
                "Lab Results Available",
                "Your results for " + event.getTestName() + " are ready. Please contact your doctor.",
                "LAB_RESULT", event.getRequestId());
        email.sendLabResultReleased(event);
        
        userRepository.findById(event.getPatientId()).ifPresent(user -> {
            twilio.sendLabResultReleasedSms(user.getPhoneNumber(), event.getTestName());
        });
        
        if (event.getDoctorId() != null) {
            inApp.sendToUser(event.getDoctorId(),
                    "Lab Results Ready",
                    "Results for " + event.getTestName() + " (Patient: " + event.getPatientName() + ") are now available.",
                    "LAB_RESULT", event.getRequestId());
        }
    }

    // ─── Queue Token Called ───────────────────────────────────────────────────

    @Async
    @EventListener
    public void onQueueTokenCalled(QueueTokenCalledEvent event) {
        log.info("Handling QueueTokenCalledEvent for token #{}", event.getTokenNumber());
        email.sendQueueTokenCalled(event);
        // In-app for queued patient is optional (they may not be logged in)
        
        if (event.getPatientUserId() != null) {
            userRepository.findById(event.getPatientUserId()).ifPresent(user -> {
                twilio.sendQueueTokenCalledSms(user.getPhoneNumber(), event.getTokenNumber(), event.getBranchName());
            });
        }
        
        inApp.sendToRole("ROLE_NURSE",
                "Queue Token Called",
                "Token #" + event.getTokenNumber() + " called at " + event.getBranchName(),
                "QUEUE", null);
                
        inApp.sendToRole("ROLE_ADMIN",
                "Queue Token Called",
                "Token #" + event.getTokenNumber() + " called at " + event.getBranchName(),
                "QUEUE", null);
    }

    // ─── Prescription Created ─────────────────────────────────────────────────

    @Async
    @EventListener
    public void onPrescriptionCreated(PrescriptionCreatedEvent event) {
        log.info("Handling PrescriptionCreatedEvent for prescription {}", event.getPrescriptionId());
    }
}
