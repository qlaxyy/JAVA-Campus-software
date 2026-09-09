package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Requests a self-service return using the scanned physical-copy barcode. */
public final class CopyReturnRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String barcode;

    public CopyReturnRequest(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
