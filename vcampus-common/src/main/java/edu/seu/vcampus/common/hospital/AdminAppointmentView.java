package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Appointment ledger row visible to hospital administrators, without clinical content. */
public final class AdminAppointmentView implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;
    private final String patientUserId;
    private final String scheduleId;
    private final int queueNumber;
    private final AppointmentStatus appointmentStatus;
    private final VisitType visitType;
    private final String departmentName;
    private final String doctorName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long amountCents;
    private final PaymentStatus paymentStatus;
    private final boolean cancellable;

    public AdminAppointmentView(
            String appointmentId,
            String patientUserId,
            String scheduleId,
            int queueNumber,
            AppointmentStatus appointmentStatus,
            VisitType visitType,
            String departmentName,
            String doctorName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long amountCents,
            PaymentStatus paymentStatus,
            boolean cancellable) {
        this.appointmentId = Objects.requireNonNull(appointmentId);
        this.patientUserId = Objects.requireNonNull(patientUserId);
        this.scheduleId = Objects.requireNonNull(scheduleId);
        this.queueNumber = queueNumber;
        this.appointmentStatus = Objects.requireNonNull(appointmentStatus);
        this.visitType = Objects.requireNonNull(visitType);
        this.departmentName = Objects.requireNonNull(departmentName);
        this.doctorName = Objects.requireNonNull(doctorName);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.amountCents = amountCents;
        this.paymentStatus = Objects.requireNonNull(paymentStatus);
        this.cancellable = cancellable;
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientUserId() { return patientUserId; }
    public String getScheduleId() { return scheduleId; }
    public int getQueueNumber() { return queueNumber; }
    public AppointmentStatus getAppointmentStatus() { return appointmentStatus; }
    public VisitType getVisitType() { return visitType; }
    public String getDepartmentName() { return departmentName; }
    public String getDoctorName() { return doctorName; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getAmountCents() { return amountCents; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public boolean isCancellable() { return cancellable; }
}
