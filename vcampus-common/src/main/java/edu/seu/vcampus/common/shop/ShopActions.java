package edu.seu.vcampus.common.shop;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/**
 * Public actions owned by the campus-shop module.
 *
 * <p>Registered on the server: catalog, campus-card wallet, and order actions.
 */
public final class ShopActions {

    public static final String LIST_PRODUCTS = ActionNames.of(ModuleNames.SHOP, "LIST_PRODUCTS");
    public static final String PUBLISH_PRODUCT = ActionNames.of(ModuleNames.SHOP, "PUBLISH_PRODUCT");
    public static final String GET_CAMPUS_CARD = ActionNames.of(ModuleNames.SHOP, "GET_CAMPUS_CARD");
    public static final String RECHARGE_CAMPUS_CARD = ActionNames.of(ModuleNames.SHOP, "RECHARGE_CAMPUS_CARD");
    public static final String CREATE_ORDER = ActionNames.of(ModuleNames.SHOP, "CREATE_ORDER");
    public static final String LIST_ORDERS = ActionNames.of(ModuleNames.SHOP, "LIST_ORDERS");
    public static final String CANCEL_ORDER = ActionNames.of(ModuleNames.SHOP, "CANCEL_ORDER");
    public static final String LIST_SALES = ActionNames.of(ModuleNames.SHOP, "LIST_SALES");

    private ShopActions() {
    }
}
