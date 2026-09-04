package edu.seu.vcampus.common.shop;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/**
 * Public actions owned by the campus-shop module.
 *
 * <p>Registered on the server: {@link #LIST_PRODUCTS}, {@link #PUBLISH_PRODUCT}.
 * Cart and order actions are not registered yet.
 */
public final class ShopActions {

    public static final String LIST_PRODUCTS = ActionNames.of(ModuleNames.SHOP, "LIST_PRODUCTS");
    public static final String PUBLISH_PRODUCT = ActionNames.of(ModuleNames.SHOP, "PUBLISH_PRODUCT");

    private ShopActions() {
    }
}
