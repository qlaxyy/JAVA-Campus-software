package edu.seu.vcampus.client.module.shop;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ShopMoney {

    private ShopMoney() {
    }

    static String yuan(int fen) {
        return "¥" + String.format("%.2f", fen / 100.0);
    }

    /**
     * Parses a yuan amount typed by the user.
     *
     * @return fen, or {@code null} when the text is empty, not numeric, or out of range
     */
    static Integer yuanToFen(String text, int minFen, int maxFen) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim().replace("¥", "").replace("元", "").replace(",", "");
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            BigDecimal fen = new BigDecimal(trimmed).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
            int amountFen = fen.intValueExact();
            if (amountFen < minFen || amountFen > maxFen) {
                return null;
            }
            return amountFen;
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }
}
