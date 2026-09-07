package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Requests a self-service loan using the scanned physical-copy barcode. */
public final class CopyBorrowRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String barcode;

    public CopyBorrowRequest(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
