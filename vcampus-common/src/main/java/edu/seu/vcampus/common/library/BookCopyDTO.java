package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Immutable network view of one physical library copy. */
public final class BookCopyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String copyId;
    private final String barcode;
    private final String bookId;
    private final String location;
    private final String callNumber;
    private final String status;

    public BookCopyDTO(String copyId, String barcode, String bookId, String location,
            String callNumber, String status) {
        this.copyId = copyId;
        this.barcode = barcode;
        this.bookId = bookId;
        this.location = location;
        this.callNumber = callNumber;
        this.status = status;
    }

    public String getCopyId() { return copyId; }

    public String getBarcode() { return barcode; }

    public String getBookId() { return bookId; }

    public String getLocation() { return location; }

    public String getCallNumber() { return callNumber; }

    public String getStatus() { return status; }
}
