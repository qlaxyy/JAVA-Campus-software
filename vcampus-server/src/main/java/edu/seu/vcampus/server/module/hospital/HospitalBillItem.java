package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** One registration-fee line belonging to a hospital bill. */
record HospitalBillItem(
        String billItemId,
        String billId,
        String itemName,
        int quantity,
        long unitPriceCents) {

    HospitalBillItem {
        Objects.requireNonNull(billItemId, "billItemId must not be null");
        Objects.requireNonNull(billId, "billId must not be null");
        Objects.requireNonNull(itemName, "itemName must not be null");
        if (quantity <= 0 || unitPriceCents < 0) {
            throw new IllegalArgumentException("bill item amount fields are invalid");
        }
    }

    long amountCents() {
        return Math.multiplyExact(quantity, unitPriceCents);
    }
}
