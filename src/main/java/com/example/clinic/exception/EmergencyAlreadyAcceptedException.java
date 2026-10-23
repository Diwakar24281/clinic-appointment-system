package com.example.clinic.exception;

/**
 * Thrown when a hospital attempts to accept an emergency that was already accepted by another hospital.
 */
public class EmergencyAlreadyAcceptedException extends RuntimeException {
    public EmergencyAlreadyAcceptedException(String message) {
        super(message);
    }
}
