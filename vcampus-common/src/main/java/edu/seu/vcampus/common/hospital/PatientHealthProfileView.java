package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Patient-authored health summary shown as consultation background information. */
public final class PatientHealthProfileView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bloodType;
    private final String allergies;
    private final String medicalHistory;
    private final String longTermMedication;
    private final String emergencyContact;
    private final LocalDateTime updatedAt;
    private final long version;

    public PatientHealthProfileView(
            String bloodType,
            String allergies,
            String medicalHistory,
            String longTermMedication,
            String emergencyContact,
            LocalDateTime updatedAt) {
        this(bloodType, allergies, medicalHistory, longTermMedication,
                emergencyContact, updatedAt, 0L);
    }

    public PatientHealthProfileView(
            String bloodType,
            String allergies,
            String medicalHistory,
            String longTermMedication,
            String emergencyContact,
            LocalDateTime updatedAt,
            long version) {
        this.bloodType = displayText(bloodType);
        this.allergies = displayText(allergies);
        this.medicalHistory = displayText(medicalHistory);
        this.longTermMedication = displayText(longTermMedication);
        this.emergencyContact = displayText(emergencyContact);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (version < 0L) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public String getLongTermMedication() {
        return longTermMedication;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    private static String displayText(String value) {
        return value == null || value.isBlank() ? "未填写" : value.trim();
    }
}
