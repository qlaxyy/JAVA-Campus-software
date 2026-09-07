package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;

/**
 * Adds money to the caller's virtual campus card.
 */
public final class RechargeCampusCardRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int MIN_FEN = 1000;
    public static final int MAX_FEN = 10_000;

    private final int amountFen;

    /**
     * Creates a recharge request.
     *
     * @param amountFen amount in fen; must be 10–100 yuan
     */
    public RechargeCampusCardRequest(int amountFen) {
        if (amountFen < MIN_FEN || amountFen > MAX_FEN) {
            throw new IllegalArgumentException("amountFen must be between 10 and 100 yuan");
        }
        this.amountFen = amountFen;
    }

    public int getAmountFen() {
        return amountFen;
    }
}
