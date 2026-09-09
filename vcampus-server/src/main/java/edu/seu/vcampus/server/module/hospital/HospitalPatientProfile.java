package edu.seu.vcampus.server.module.hospital;

import java.time.LocalDateTime;
import java.util.Objects;

/** Internal patient-authored health profile. */
record HospitalPatientProfile(
        String patientUserId,
        String bloodType,
        String allergies,
        String medicalHistory,
        String longTermMedication,
        String emergencyContact,
        LocalDateTime updatedAt) {

    HospitalPatientProfile {
        patientUserId = requireText(patientUserId, "patientUserId");
        bloodType = normalize(bloodType);
        allergies = normalize(allergies);
        medicalHistory = normalize(medicalHistory);
        longTermMedication = normalize(longTermMedication);
        emergencyContact = normalize(emergencyContact);
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
