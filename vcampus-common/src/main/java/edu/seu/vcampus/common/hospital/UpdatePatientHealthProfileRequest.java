package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Patient-authored health information. The patient identity comes from the session. */
public final class UpdatePatientHealthProfileRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bloodType;
    private final String allergies;
    private final String medicalHistory;
    private final String longTermMedication;
    private final String emergencyContact;
    private final long expectedVersion;

    public UpdatePatientHealthProfileRequest(
            String bloodType,
            String allergies,
            String medicalHistory,
            String longTermMedication,
            String emergencyContact) {
        this(bloodType, allergies, medicalHistory, longTermMedication,
                emergencyContact, 0L);
    }

    public UpdatePatientHealthProfileRequest(
            String bloodType,
            String allergies,
            String medicalHistory,
            String longTermMedication,
            String emergencyContact,
            long expectedVersion) {
        this.bloodType = normalize(bloodType);
        this.allergies = normalize(allergies);
        this.medicalHistory = normalize(medicalHistory);
        this.longTermMedication = normalize(longTermMedication);
        this.emergencyContact = normalize(emergencyContact);
        if (expectedVersion < 0L) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        this.expectedVersion = expectedVersion;
    }

    public String getBloodType() { return bloodType; }

    public String getAllergies() { return allergies; }

    public String getMedicalHistory() { return medicalHistory; }

    public String getLongTermMedication() { return longTermMedication; }

    public String getEmergencyContact() { return emergencyContact; }

    public long getExpectedVersion() { return expectedVersion; }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
