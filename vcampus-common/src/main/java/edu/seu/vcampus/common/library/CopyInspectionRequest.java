package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Requests a read-only self-service terminal inspection by physical-copy barcode. */
public final class CopyInspectionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String barcode;

    public CopyInspectionRequest(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
