package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable administrator view of one reservation.
 *
 * <p>Differs from {@link ReservationDTO} by carrying the owning reader: administrators need to see
 * whose queue they are looking at, while the reader-facing view never exposes another user.
 */
public final class AdminReservationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String reservationId;
    private final String userId;
    private final String bookId;
    private final String bookTitle;
    private final String pickupLocation;
    private final String assignedBarcode;
    private final LocalDateTime createdAt;
    private final LocalDateTime readyAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime closedAt;
    private final String status;
    private final Integer queuePosition;

    public AdminReservationDTO(String reservationId, String userId, String bookId,
            String bookTitle, String pickupLocation, String assignedBarcode,
            LocalDateTime createdAt, LocalDateTime readyAt, LocalDateTime expiresAt,
            LocalDateTime closedAt, String status, Integer queuePosition) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.pickupLocation = pickupLocation;
        this.assignedBarcode = assignedBarcode;
        this.createdAt = createdAt;
        this.readyAt = readyAt;
        this.expiresAt = expiresAt;
        this.closedAt = closedAt;
        this.status = status;
        this.queuePosition = queuePosition;
    }

    public String getReservationId() { return reservationId; }

    /** @return stable identifier of the reader who owns this reservation */
    public String getUserId() { return userId; }

    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getPickupLocation() { return pickupLocation; }
    public String getAssignedBarcode() { return assignedBarcode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadyAt() { return readyAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getStatus() { return status; }

    /** @return 1-based queue position while waiting, otherwise null */
    public Integer getQueuePosition() { return queuePosition; }
}
