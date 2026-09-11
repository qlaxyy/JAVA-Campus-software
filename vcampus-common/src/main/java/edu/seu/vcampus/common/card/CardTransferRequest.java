package edu.seu.vcampus.common.card;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Debit or credit a campus card for another campus subsystem.
 */
public final class CardTransferRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int amountFen;
    private final String merchant;
    private final String reference;

    /**
     * Creates a transfer request.
     *
     * @param amountFen amount in fen, must be positive
     * @param merchant subsystem id such as {@code SHOP}, {@code HOSPITAL}, {@code LIBRARY}
     * @param reference idempotency key unique per merchant, for example {@code bill:abc}
     */
    public CardTransferRequest(int amountFen, String merchant, String reference) {
        if (amountFen < 1) {
            throw new IllegalArgumentException("amountFen must be positive");
        }
        this.amountFen = amountFen;
        this.merchant = requireText(merchant, "merchant");
        this.reference = requireText(reference, "reference");
    }

    public int getAmountFen() {
        return amountFen;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getReference() {
        return reference;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
