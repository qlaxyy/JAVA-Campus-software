package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Virtual campus-card wallet used by shop checkout.
 */
public final class CampusCardView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String username;
    private final String cardNo;
    private final int balanceFen;

    /**
     * Creates a wallet snapshot.
     *
     * @param userId owning account
     * @param username login campus-card number
     * @param cardNo campus-card number, currently equal to {@code username}
     * @param balanceFen remaining balance in fen
     */
    public CampusCardView(String userId, String username, String cardNo, int balanceFen) {
        this.userId = requireText(userId, "userId");
        this.username = requireText(username, "username");
        this.cardNo = requireText(cardNo, "cardNo");
        if (balanceFen < 0) {
            throw new IllegalArgumentException("balanceFen must not be negative");
        }
        this.balanceFen = balanceFen;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getCardNo() {
        return cardNo;
    }

    public int getBalanceFen() {
        return balanceFen;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
