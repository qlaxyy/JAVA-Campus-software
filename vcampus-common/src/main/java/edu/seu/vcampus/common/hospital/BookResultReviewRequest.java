package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Patient request for one zero-fee result-review continuation visit. */
public final class BookResultReviewRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String orderId;

    public BookResultReviewRequest(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        this.orderId = orderId.trim();
    }

    public String getOrderId() {
        return orderId;
    }
}
