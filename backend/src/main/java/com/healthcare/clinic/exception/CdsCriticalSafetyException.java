package com.healthcare.clinic.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when a synchronous prescription safety check detects a CRITICAL
 * drug allergy, drug-disease contraindication, or severe drug-drug interaction.
 * Triggers a rollback of the prescription transaction and returns HTTP 422.
 */
@Getter
public class CdsCriticalSafetyException extends RuntimeException {

    private final List<String> safetyAlerts;

    public CdsCriticalSafetyException(String message, List<String> safetyAlerts) {
        super(message);
        this.safetyAlerts = safetyAlerts;
    }
}
