package edu.seu.vcampus.common.card;

import java.io.Serial;
import java.io.Serializable;

/**
 * Adds money to the caller's campus card through the card gateway.
 */
public final class CardRechargeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int MIN_FEN = 1;
    public static final int MAX_FEN = 1_000_000;

    private final int amountFen;

    /**
     * Creates a recharge request.
     *
     * @param amountFen amount in fen; must be 0.01–10000 yuan
     */
    public CardRechargeRequest(int amountFen) {
        if (amountFen < MIN_FEN || amountFen > MAX_FEN) {
            throw new IllegalArgumentException("amountFen must be between 0.01 and 10000 yuan");
        }
        this.amountFen = amountFen;
    }

    public int getAmountFen() {
        return amountFen;
    }
}
