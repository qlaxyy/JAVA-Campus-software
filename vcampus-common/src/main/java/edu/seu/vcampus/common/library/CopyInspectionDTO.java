package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Immutable terminal decision for one physical copy and the authenticated user. */
public final class CopyInspectionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookTitle;
    private final String barcode;
    private final String copyStatus;
    private final boolean borrowAllowed;
    private final boolean returnAllowed;
    private final boolean reservedForCurrentUser;
    private final String statusMessage;

    public CopyInspectionDTO(String bookTitle, String barcode, String copyStatus,
            boolean borrowAllowed, boolean returnAllowed,
            boolean reservedForCurrentUser, String statusMessage) {
        this.bookTitle = bookTitle;
        this.barcode = barcode;
        this.copyStatus = copyStatus;
        this.borrowAllowed = borrowAllowed;
        this.returnAllowed = returnAllowed;
        this.reservedForCurrentUser = reservedForCurrentUser;
        this.statusMessage = statusMessage;
    }

    public String getBookTitle() { return bookTitle; }

    public String getBarcode() { return barcode; }

    public String getCopyStatus() { return copyStatus; }

    public boolean isBorrowAllowed() { return borrowAllowed; }

    public boolean isReturnAllowed() { return returnAllowed; }

    public boolean isReservedForCurrentUser() { return reservedForCurrentUser; }

    public String getStatusMessage() { return statusMessage; }
}
