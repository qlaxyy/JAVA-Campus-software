package edu.seu.vcampus.server.module.hospital;

import java.time.LocalDate;
import java.util.List;

/** Data boundary that can later be implemented with Access/JDBC. */
interface HospitalRepository {

    List<HospitalDepartment> findActiveDepartments();

    List<HospitalSlot> findSlots(LocalDate startDate, LocalDate endDate);
}
