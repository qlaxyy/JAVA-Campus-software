package edu.seu.vcampus.client.module.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopCatalogPagesTest {

    @Test
    void fourItemsFitOnOnePage() {
        assertEquals(1, ShopCatalogPages.pageCount(4, 4));
        assertEquals(List.of("a", "b", "c", "d"), ShopCatalogPages.slice(List.of("a", "b", "c", "d"), 1, 4));
    }

    @Test
    void eightItemsSplitAcrossTwoPagesOfFour() {
        List<String> items = List.of("1", "2", "3", "4", "5", "6", "7", "8");

        assertEquals(2, ShopCatalogPages.pageCount(8, 4));
        assertEquals(List.of("1", "2", "3", "4"), ShopCatalogPages.slice(items, 1, 4));
        assertEquals(List.of("5", "6", "7", "8"), ShopCatalogPages.slice(items, 2, 4));
    }

    @Test
    void sixPerPageKeepsEightItemsOnTwoPages() {
        assertEquals(2, ShopCatalogPages.pageCount(8, 6));
        assertEquals(6, ShopCatalogPages.slice(List.of("1", "2", "3", "4", "5", "6", "7", "8"), 1, 6).size());
    }

    @Test
    void clampsPagePastTheEnd() {
        List<String> items = List.of("1", "2", "3", "4", "5");

        assertEquals(2, ShopCatalogPages.pageCount(5, 4));
        assertEquals(List.of("5"), ShopCatalogPages.slice(items, 9, 4));
        assertEquals(1, ShopCatalogPages.clampPage(0, 5, 4));
    }

    @Test
    void compactWindowKeepsFirstAndLast() {
        List<Integer> window = ShopCatalogPages.pageWindow(5, 12);

        assertEquals(1, window.getFirst());
        assertEquals(12, window.getLast());
        assertTrue(window.contains(ShopCatalogPages.ELLIPSIS));
        assertTrue(window.contains(5));
    }

    @Test
    void autoGridUsesSquareCells() {
        ShopCatalogGrid.Plan plan = ShopCatalogGrid.plan(900, 520, null);

        assertTrue(plan.columns() >= 2);
        assertTrue(plan.rows() >= 1);
        assertEquals(plan.columns() * plan.rows(), plan.pageSize());
        assertTrue(plan.cellSize() >= ShopCatalogGrid.MIN_CELL);
    }

    @Test
    void fourPerPageUsesTwoByTwo() {
        ShopCatalogGrid.Plan plan = ShopCatalogGrid.plan(800, 600, 4);

        assertEquals(2, plan.columns());
        assertEquals(2, plan.rows());
        assertEquals(4, plan.pageSize());
    }
}
