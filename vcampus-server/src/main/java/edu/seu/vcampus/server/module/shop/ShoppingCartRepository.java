package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ShoppingCartView;

/** Persistence boundary for one user's server-side cart. */
interface ShoppingCartRepository {
    ShoppingCartView view(String userId);
    ShoppingCartView setQuantity(String userId, long productId, int quantity);
    ShoppingCartView clear(String userId);
}
