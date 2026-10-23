package com.parceliq.model;

/**
 * Interface Verifiable
 * Syllabus Concept: Java Interfaces & Decoupled Validation
 * Decouples security verification so parcels, locker units, or student identity cards can all implement it.
 */
public interface Verifiable {
    /**
     * Verifies the provided security code against the entity's recorded token/credential.
     * @param inputCode The token or PIN provided by the student or guard.
     * @return true if valid and matching; false otherwise.
     */
    boolean verifyCode(String inputCode);
}
