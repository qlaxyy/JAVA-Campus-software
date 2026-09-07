package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Registers one physical copy under an existing catalog record. */
public final class AddBookCopyRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;
    private final String barcode;
    private final String location;
    private final String callNumber;

    public AddBookCopyRequest(String bookId, String barcode, String location,
            String callNumber) {
        this.bookId = bookId;
        this.barcode = barcode;
        this.location = location;
        this.callNumber = callNumber;
    }

    public String getBookId() { return bookId; }

    public String getBarcode() { return barcode; }

    public String getLocation() { return location; }

    public String getCallNumber() { return callNumber; }
}
