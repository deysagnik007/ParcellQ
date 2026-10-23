package com.parceliq.exception;

/**
 * Custom Exception: ParcelAlreadyClaimedException
 * Syllabus Concept: Exception Handling (try-catch, throw custom exceptions)
 * Thrown when an intake desk or student attempts to re-claim an already collected parcel,
 * preventing double-claiming and security breaches at the university shelter.
 */
public class ParcelAlreadyClaimedException extends Exception {
    private final int parcelId;
    private final String trackingNumber;

    public ParcelAlreadyClaimedException(String message, int parcelId, String trackingNumber) {
        super(message);
        this.parcelId = parcelId;
        this.trackingNumber = trackingNumber;
    }

    public ParcelAlreadyClaimedException(String message) {
        super(message);
        this.parcelId = -1;
        this.trackingNumber = "UNKNOWN";
    }

    public int getParcelId() {
        return parcelId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }
}
