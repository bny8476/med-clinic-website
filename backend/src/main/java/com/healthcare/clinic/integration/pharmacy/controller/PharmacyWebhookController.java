package com.healthcare.clinic.integration.pharmacy.controller;

import com.healthcare.clinic.doctor.entity.Prescription;
import com.healthcare.clinic.integration.pharmacy.dto.PharmacyStatusWebhookDto;
import com.healthcare.clinic.integration.pharmacy.service.PharmacyIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations/pharmacy/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PharmacyWebhookController {

    private final PharmacyIntegrationService pharmacyIntegrationService;

    @Value("${pharmacy.service.webhook-secret:clinic-webhook-secret-token}")
    private String configuredWebhookSecret;

    @PostMapping("/status-update")
    public ResponseEntity<?> handlePharmacyStatusUpdate(
            @RequestHeader(value = "X-Pharmacy-Webhook-Secret", required = false) String secretHeader,
            @RequestBody PharmacyStatusWebhookDto webhookPayload) {

        log.info("Received external Pharmacy webhook status update: {}", webhookPayload);

        // Security check: verify webhook signature header if configured
        if (configuredWebhookSecret != null && !configuredWebhookSecret.trim().isEmpty()) {
            if (secretHeader == null || !configuredWebhookSecret.equals(secretHeader.trim())) {
                log.warn("Unauthorized webhook attempt! Invalid X-Pharmacy-Webhook-Secret header.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook secret signature");
            }
        }

        if (webhookPayload == null) {
            return ResponseEntity.badRequest().body("Payload cannot be empty");
        }

        Prescription updated = pharmacyIntegrationService.processWebhookStatusUpdate(webhookPayload);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prescription reference not found");
        }

        return ResponseEntity.ok(updated);
    }
}
