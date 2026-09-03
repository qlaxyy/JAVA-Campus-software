package edu.seu.vcampus.server.module.hospital;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data boundary that can later be implemented with Access/JDBC. */
interface HospitalRepository {

    boolean isActiveDoctorUser(String userId);

    List<DoctorApplication> findDoctorApplications();

    Optional<DoctorApplication> findDoctorApplication(String requestId);

    void saveDoctorApplication(DoctorApplication application);

    void saveDoctorProfile(DoctorProfile profile);

    List<HospitalDepartment> findActiveDepartments();

    List<HospitalSlot> findSlots(LocalDate startDate, LocalDate endDate);
}
