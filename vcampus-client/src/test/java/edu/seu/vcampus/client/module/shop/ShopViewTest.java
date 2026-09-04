package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopViewTest {

    @Test
    void studentSeesMallTabsOnly() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            ShopView view = new ShopView(context);
            assertEquals(ShopView.CARD_SHOP, view.visibleCard());
            assertEquals(3, view.tabCount());
        }
    }

    @Test
    void adminStartsOnModeChooserThenCanOpenShoppingOrManage() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260007", "123456".toCharArray()).isSuccess());
            ShopView view = new ShopView(context);
            assertEquals(ShopView.CARD_SELECT, view.visibleCard());
            view.openShopping();
            assertEquals(ShopView.CARD_SHOP, view.visibleCard());
            assertEquals(3, view.tabCount());
            view.openManage();
            assertEquals(ShopView.CARD_MANAGE, view.visibleCard());
        }
    }
}
