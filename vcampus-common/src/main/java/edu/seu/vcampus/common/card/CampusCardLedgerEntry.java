package edu.seu.vcampus.common.card;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * One persisted campus-card movement.
 */
public final class CampusCardLedgerEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String RECHARGE = "RECHARGE";
    public static final String DEBIT = "DEBIT";
    public static final String CREDIT = "CREDIT";

    private final String txnId;
    private final String userId;
    private final String merchant;
    private final String reference;
    private final String entryType;
    private final int amountFen;
    private final String createdAt;

    /**
     * Creates a ledger row.
     *
     * @param txnId unique movement id
     * @param userId owning account
     * @param merchant subsystem that requested the movement
     * @param reference merchant-unique key
     * @param entryType {@link #RECHARGE}, {@link #DEBIT} or {@link #CREDIT}
     * @param amountFen positive amount in fen
     * @param createdAt display timestamp
     */
    public CampusCardLedgerEntry(
            String txnId,
            String userId,
            String merchant,
            String reference,
            String entryType,
            int amountFen,
            String createdAt) {
        this.txnId = requireText(txnId, "txnId");
        this.userId = requireText(userId, "userId");
        this.merchant = requireText(merchant, "merchant");
        this.reference = requireText(reference, "reference");
        this.entryType = requireText(entryType, "entryType");
        if (amountFen < 1) {
            throw new IllegalArgumentException("amountFen must be positive");
        }
        this.amountFen = amountFen;
        this.createdAt = requireText(createdAt, "createdAt");
    }

    public String getTxnId() {
        return txnId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getReference() {
        return reference;
    }

    public String getEntryType() {
        return entryType;
    }

    public int getAmountFen() {
        return amountFen;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
