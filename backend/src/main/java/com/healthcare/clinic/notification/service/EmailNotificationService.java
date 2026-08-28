package com.healthcare.clinic.notification.service;

import com.healthcare.clinic.appointment.event.AppointmentBookedEvent;
import com.healthcare.clinic.notification.event.AppointmentCancelledEvent;
import com.healthcare.clinic.notification.event.InvoiceCreatedEvent;
import com.healthcare.clinic.notification.event.LabResultReleasedEvent;
import com.healthcare.clinic.notification.event.QueueTokenCalledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender emailSender;

    @Value("${spring.mail.username:noreply@healthcareclinic.com}")
    private String defaultFromEmail;

    public EmailNotificationService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    // ─── Appointment ──────────────────────────────────────────────────────────

    public void sendAppointmentConfirmation(AppointmentBookedEvent event) {
        if (!StringUtils.hasText(event.getPatientEmail())) return;
        String subject = "✅ Appointment Confirmed — HealthCare Clinic";
        String body = buildHtml(
                "Appointment Confirmed",
                String.format("Dear Patient,<br><br>Your appointment with <strong>Dr. %s</strong> has been confirmed.<br>" +
                        "<br><b>Date & Time:</b> %s<br><br>" +
                        "Please arrive 10 minutes early. If you need to cancel or reschedule, contact us at least 24 hours in advance.",
                        event.getDoctorName(), event.getStartTime())
        );
        sendHtmlEmail(event.getPatientEmail(), subject, body);
    }

    public void sendAppointmentCancellation(AppointmentCancelledEvent event) {
        if (!StringUtils.hasText(event.getPatientEmail())) return;
        String subject = "❌ Appointment Cancelled — HealthCare Clinic";
        String body = buildHtml(
                "Appointment Cancelled",
                String.format("Dear Patient,<br><br>Your appointment scheduled for <strong>%s</strong> " +
                        "with <strong>Dr. %s</strong> has been cancelled.<br><br>Please contact us to reschedule.",
                        event.getStartTime(), event.getDoctorName())
        );
        sendHtmlEmail(event.getPatientEmail(), subject, body);
    }

    // ─── Invoice ──────────────────────────────────────────────────────────────

    public void sendInvoiceCreated(InvoiceCreatedEvent event) {
        if (!StringUtils.hasText(event.getPatientEmail())) return;
        String subject = "🧾 Invoice Created — " + event.getInvoiceNumber();
        String body = buildHtml(
                "New Invoice",
                String.format("Dear %s,<br><br>A new invoice has been created for your recent visit.<br>" +
                        "<br><b>Invoice No.:</b> %s<br><b>Amount Due:</b> ₹%s<br><b>Due Date:</b> %s<br><br>" +
                        "Please log in to your patient portal to view and pay your invoice.",
                        event.getPatientName(), event.getInvoiceNumber(),
                        event.getTotalAmount(), event.getDueDate())
        );
        sendHtmlEmail(event.getPatientEmail(), subject, body);
    }

    // ─── Lab Result ───────────────────────────────────────────────────────────

    public void sendLabResultReleased(LabResultReleasedEvent event) {
        if (!StringUtils.hasText(event.getPatientEmail())) return;
        String subject = "🔬 Lab Results Ready — HealthCare Clinic";
        String body = buildHtml(
                "Lab Results Available",
                String.format("Dear %s,<br><br>Your lab test results for <strong>%s</strong> are now available.<br>" +
                        "<br>Please log in to your patient portal or contact your doctor to review the results.",
                        event.getPatientName(), event.getTestName())
        );
        sendHtmlEmail(event.getPatientEmail(), subject, body);
    }

    // ─── Queue ────────────────────────────────────────────────────────────────

    public void sendQueueTokenCalled(QueueTokenCalledEvent event) {
        if (!StringUtils.hasText(event.getPatientEmail())) return;
        String subject = "🔔 Your Turn — Token #" + event.getTokenNumber();
        String body = buildHtml(
                "Your Number is Being Called",
                String.format("Dear Patient,<br><br>Token number <strong>#%d</strong> is now being called at <strong>%s</strong>.<br>" +
                        "<br>Please proceed to the consultation room.",
                        event.getTokenNumber(), event.getBranchName())
        );
        sendHtmlEmail(event.getPatientEmail(), subject, body);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (emailSender == null) {
            log.warn("JavaMailSender is not configured. Skipping HTML email dispatch to {}: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            emailSender.send(message);
            log.info("HTML email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildHtml(String heading, String content) {
        return "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;background:#f8fafc;padding:20px;\">" +
               "<div style=\"max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;" +
               "border:1px solid #e2e8f0;\">" +
               "<div style=\"background:#1e3a5f;padding:24px 32px;\">" +
               "<h2 style=\"color:#fff;margin:0;font-size:1.2rem;\">HealthCare Clinic</h2></div>" +
               "<div style=\"padding:32px;\">" +
               "<h3 style=\"color:#1e3a5f;margin-top:0;\">" + heading + "</h3>" +
               "<p style=\"color:#334155;line-height:1.7;\">" + content + "</p>" +
               "</div>" +
               "<div style=\"background:#f1f5f9;padding:16px 32px;text-align:center;\">" +
               "<p style=\"color:#94a3b8;font-size:0.8rem;margin:0;\">© 2025 HealthCare Clinic. All rights reserved.</p>" +
               "</div></div></body></html>";
    }
}
