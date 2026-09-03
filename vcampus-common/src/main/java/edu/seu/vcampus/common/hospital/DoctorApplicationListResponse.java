package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Immutable list returned by the doctor-registration workflow. */
public final class DoctorApplicationListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<DoctorApplicationView> applications;

    public DoctorApplicationListResponse(List<DoctorApplicationView> applications) {
        this.applications = List.copyOf(applications);
    }

    public List<DoctorApplicationView> getApplications() { return applications; }
}
