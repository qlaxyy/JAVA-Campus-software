package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Immutable user-facing view of one library reservation. */
public final class ReservationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String reservationId;
    private final String bookId;
    private final String isbn;
    private final String bookTitle;
    private final String bookAuthor;
    private final String pickupLocation;
    private final String assignedBarcode;
    private final LocalDateTime createdAt;
    private final LocalDateTime readyAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime closedAt;
    private final String status;
    private final Integer queuePosition;

    /** Creates a reservation view; queuePosition is present only while waiting. */
    public ReservationDTO(String reservationId, String bookId, String isbn,
            String bookTitle, String bookAuthor, String pickupLocation,
            String assignedBarcode, LocalDateTime createdAt, LocalDateTime readyAt,
            LocalDateTime expiresAt, LocalDateTime closedAt, String status,
            Integer queuePosition) {
        this.reservationId = reservationId;
        this.bookId = bookId;
        this.isbn = isbn;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
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

    public String getBookId() { return bookId; }

    public String getIsbn() { return isbn; }

    public String getBookTitle() { return bookTitle; }

    public String getBookAuthor() { return bookAuthor; }

    public String getPickupLocation() { return pickupLocation; }

    public String getAssignedBarcode() { return assignedBarcode; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getReadyAt() { return readyAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public LocalDateTime getClosedAt() { return closedAt; }

    public String getStatus() { return status; }

    public Integer getQueuePosition() { return queuePosition; }
}
