package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Server-calculated hospital workspace permissions for the current session. */
public final class HospitalModeAccessView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean patientMode;
    private final boolean doctorMode;
    private final boolean adminMode;

    public HospitalModeAccessView(
            boolean patientMode,
            boolean doctorMode,
            boolean adminMode) {
        this.patientMode = patientMode;
        this.doctorMode = doctorMode;
        this.adminMode = adminMode;
    }

    public boolean canAccess(HospitalMode mode) {
        Objects.requireNonNull(mode, "mode must not be null");
        return switch (mode) {
            case PATIENT -> patientMode;
            case DOCTOR -> doctorMode;
            case ADMIN -> adminMode;
        };
    }

    public boolean canUsePatientMode() {
        return patientMode;
    }

    public boolean canUseDoctorMode() {
        return doctorMode;
    }

    public boolean canUseAdminMode() {
        return adminMode;
    }
}
