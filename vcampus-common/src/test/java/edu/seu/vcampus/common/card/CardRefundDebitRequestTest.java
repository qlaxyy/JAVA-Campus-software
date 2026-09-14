package edu.seu.vcampus.common.card;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardRefundDebitRequestTest {

    @Test
    void refundReferenceMustBeDerivedFromOriginalDebit() {
        CardRefundDebitRequest request = new CardRefundDebitRequest(
                "U-PATIENT-001",
                1_200,
                "HOSPITAL",
                "registration:bill-1",
                "registration:bill-1:refund");

        assertEquals("registration:bill-1:refund", request.getRefundReference());
        assertThrows(IllegalArgumentException.class, () -> new CardRefundDebitRequest(
                "U-PATIENT-001",
                1_200,
                "HOSPITAL",
                "registration:bill-1",
                "another-refund"));
    }
}
