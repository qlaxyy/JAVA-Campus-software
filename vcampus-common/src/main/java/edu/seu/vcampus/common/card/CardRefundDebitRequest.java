package edu.seu.vcampus.common.card;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Refunds a previously posted campus-card debit to its original owner.
 *
 * <p>The original debit reference and the refund reference make the operation
 * safe to retry. A refund is ignored when the original debit does not exist,
 * which keeps legacy simulated hospital payments from creating card balance.
 */
public final class CardRefundDebitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String targetUserId;
    private final int amountFen;
    private final String merchant;
    private final String debitReference;
    private final String refundReference;

    public CardRefundDebitRequest(
            String targetUserId,
            int amountFen,
            String merchant,
            String debitReference,
            String refundReference) {
        this.targetUserId = requireText(targetUserId, "targetUserId");
        if (amountFen < 1) {
            throw new IllegalArgumentException("amountFen must be positive");
        }
        this.amountFen = amountFen;
        this.merchant = requireText(merchant, "merchant");
        this.debitReference = requireText(debitReference, "debitReference");
        this.refundReference = requireText(refundReference, "refundReference");
        if (!this.refundReference.equals(this.debitReference + ":refund")) {
            throw new IllegalArgumentException(
                    "refundReference must be derived from debitReference");
        }
    }

    public String getTargetUserId() { return targetUserId; }
    public int getAmountFen() { return amountFen; }
    public String getMerchant() { return merchant; }
    public String getDebitReference() { return debitReference; }
    public String getRefundReference() { return refundReference; }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException(fieldName + " format is invalid");
        }
        return normalized;
    }
}
