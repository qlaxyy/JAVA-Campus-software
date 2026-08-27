package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.hospital.VisitType;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

/** Hospital business rules independent from sockets and Swing. */
final class HospitalService {

    private static final int SEARCH_DAYS = 7;

    private final HospitalRepository repository;
    private final Clock clock;

    HospitalService(HospitalRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    DepartmentListResponse listDepartments() {
        return new DepartmentListResponse(repository.findActiveDepartments().stream()
                .sorted(Comparator.comparing(HospitalDepartment::departmentName))
                .map(department -> new DepartmentView(
                        department.departmentId(), department.departmentName()))
                .toList());
    }

    SlotListResponse searchSlots(SearchSlotsRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.getVisitType() != VisitType.FIRST_VISIT) {
            throw new IllegalArgumentException(
                    "Follow-up slot search is not available in the first submission.");
        }
        if (request.getDepartmentId() == null || request.getDepartmentId().isBlank()) {
            throw new IllegalArgumentException("departmentId is required");
        }
        boolean departmentExists = repository.findActiveDepartments().stream()
                .anyMatch(department -> department.departmentId()
                        .equals(request.getDepartmentId()));
        if (!departmentExists) {
            throw new IllegalArgumentException("departmentId does not exist");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = today.plusDays(SEARCH_DAYS - 1L);
        LocalDateTime now = LocalDateTime.now(clock);
        return new SlotListResponse(repository.findSlots(today, endDate).stream()
                .filter(HospitalSlot::published)
                .filter(slot -> !slot.startTime().isBefore(now))
                .filter(slot -> slot.departmentId().equals(request.getDepartmentId()))
                .filter(slot -> request.getDoctorId() == null
                        || slot.doctorId().equals(request.getDoctorId()))
                .sorted(Comparator.comparing(HospitalSlot::startTime)
                        .thenComparing(HospitalSlot::doctorName))
                .map(HospitalService::toView)
                .toList());
    }

    private static SlotView toView(HospitalSlot slot) {
        int remaining = slot.capacity() - slot.bookedCount();
        SlotAvailability availability = remaining == 0
                ? SlotAvailability.FULL
                : SlotAvailability.AVAILABLE;
        return new SlotView(
                slot.scheduleId(),
                slot.departmentId(),
                slot.departmentName(),
                slot.doctorId(),
                slot.doctorName(),
                slot.doctorTitle(),
                slot.startTime(),
                slot.endTime(),
                slot.priceCents(),
                slot.capacity(),
                remaining,
                availability);
    }
}
