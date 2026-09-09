package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Patient-owned hospital bills ordered for the fee statement. */
public final class PatientBillListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<PatientBillView> bills;

    public PatientBillListResponse(List<PatientBillView> bills) {
        this.bills = List.copyOf(Objects.requireNonNull(bills, "bills must not be null"));
    }

    public List<PatientBillView> getBills() { return bills; }
}
