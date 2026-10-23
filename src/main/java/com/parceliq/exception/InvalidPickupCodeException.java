package com.parceliq.exception;

/**
 * Custom Exception: InvalidPickupCodeException
 * Syllabus Concept: Exception Handling (try-catch, throw custom exceptions)
 * Thrown when a student attempts to verify or claim a parcel with a non-matching token.
 */
public class InvalidPickupCodeException extends Exception {
    private final String attemptedCode;
    private final String trackingNumber;

    public InvalidPickupCodeException(String message, String attemptedCode, String trackingNumber) {
        super(message);
        this.attemptedCode = attemptedCode;
        this.trackingNumber = trackingNumber;
    }

    public InvalidPickupCodeException(String message) {
        super(message);
        this.attemptedCode = "UNKNOWN";
        this.trackingNumber = "UNKNOWN";
    }

    public String getAttemptedCode() {
        return attemptedCode;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }
}
