package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.SessionInfo;

/**
 * Shop-mode checks based on SYSTEM_DESIGN.md. Does not own login.
 */
final class ShopAccess {

    private ShopAccess() {
    }

    static boolean canManageShop(SessionInfo session) {
        return session.canAdminister(ModuleNames.SHOP);
    }
}
