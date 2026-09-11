package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Requests simulated payment of one patient-owned hospital bill. */
public final class PayHospitalBillRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String billId;

    public PayHospitalBillRequest(String billId) {
        Objects.requireNonNull(billId, "billId must not be null");
        if (billId.isBlank()) {
            throw new IllegalArgumentException("billId must not be blank");
        }
        this.billId = billId.trim();
    }

    public String getBillId() { return billId; }
}
