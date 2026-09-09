package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Read-only examination order together with its optional demo report. */
public final class ExaminationOrderView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final String episodeId;
    private final String orderedAppointmentId;
    private final String doctorName;
    private final String departmentName;
    private final String itemName;
    private final String instructions;
    private final ExaminationStatus status;
    private final String resultSummary;
    private final LocalDateTime orderedAt;
    private final LocalDateTime reportedAt;
    private final boolean resultReviewBooked;

    public ExaminationOrderView(
            String orderId,
            String episodeId,
            String orderedAppointmentId,
            String doctorName,
            String departmentName,
            String itemName,
            String instructions,
            ExaminationStatus status,
            String resultSummary,
            LocalDateTime orderedAt,
            LocalDateTime reportedAt,
            boolean resultReviewBooked) {
        this.orderId = requireText(orderId, "orderId");
        this.episodeId = requireText(episodeId, "episodeId");
        this.orderedAppointmentId = requireText(
                orderedAppointmentId, "orderedAppointmentId");
        this.doctorName = requireText(doctorName, "doctorName");
        this.departmentName = requireText(departmentName, "departmentName");
        this.itemName = requireText(itemName, "itemName");
        this.instructions = displayText(instructions, "无特别说明");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.resultSummary = displayText(resultSummary, "尚未出具");
        this.orderedAt = Objects.requireNonNull(orderedAt, "orderedAt must not be null");
        this.reportedAt = reportedAt;
        this.resultReviewBooked = resultReviewBooked;
        if (status == ExaminationStatus.ORDERED && reportedAt != null) {
            throw new IllegalArgumentException("an ordered examination cannot have a report time");
        }
        if ((status == ExaminationStatus.RESULT_READY
                || status == ExaminationStatus.REVIEWED) && reportedAt == null) {
            throw new IllegalArgumentException("a reported examination requires reportedAt");
        }
    }

    public String getOrderId() { return orderId; }

    public String getEpisodeId() { return episodeId; }

    public String getOrderedAppointmentId() { return orderedAppointmentId; }

    public String getDoctorName() { return doctorName; }

    public String getDepartmentName() { return departmentName; }

    public String getItemName() { return itemName; }

    public String getInstructions() { return instructions; }

    public ExaminationStatus getStatus() { return status; }

    public String getResultSummary() { return resultSummary; }

    public LocalDateTime getOrderedAt() { return orderedAt; }

    public LocalDateTime getReportedAt() { return reportedAt; }

    public boolean isResultReviewBooked() { return resultReviewBooked; }

    private static String displayText(String value, String emptyText) {
        return value == null || value.isBlank() ? emptyText : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
