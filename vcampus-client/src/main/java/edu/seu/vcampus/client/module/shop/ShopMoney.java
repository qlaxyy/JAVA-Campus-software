package edu.seu.vcampus.client.module.shop;

final class ShopMoney {

    private ShopMoney() {
    }

    static String yuan(int fen) {
        return "¥" + String.format("%.2f", fen / 100.0);
    }
}
