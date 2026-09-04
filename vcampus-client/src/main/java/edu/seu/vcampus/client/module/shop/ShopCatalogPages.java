package edu.seu.vcampus.client.module.shop;

import java.util.ArrayList;
import java.util.List;

/**
 * Web-style catalog paging with a configurable page size.
 */
final class ShopCatalogPages {

    static final int ELLIPSIS = -1;

    private ShopCatalogPages() {
    }

    static int pageCount(int totalItems, int pageSize) {
        int size = Math.max(1, pageSize);
        if (totalItems <= 0) {
            return 1;
        }
        return (totalItems + size - 1) / size;
    }

    static int clampPage(int page, int totalItems, int pageSize) {
        int count = pageCount(totalItems, pageSize);
        if (page < 1) {
            return 1;
        }
        return Math.min(page, count);
    }

    static <T> List<T> slice(List<T> items, int page, int pageSize) {
        int size = Math.max(1, pageSize);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        int safePage = clampPage(page, items.size(), size);
        int from = (safePage - 1) * size;
        int to = Math.min(from + size, items.size());
        return List.copyOf(items.subList(from, to));
    }

    /**
     * Returns 1-based page numbers to render. {@link #ELLIPSIS} marks a gap.
     *
     * @param current current page, 1-based
     * @param pageCount total pages
     * @return labels for a compact pager
     */
    static List<Integer> pageWindow(int current, int pageCount) {
        int count = Math.max(1, pageCount);
        int page = Math.min(count, Math.max(1, current));
        if (count <= 7) {
            List<Integer> all = new ArrayList<>();
            for (int index = 1; index <= count; index++) {
                all.add(index);
            }
            return all;
        }
        List<Integer> window = new ArrayList<>();
        window.add(1);
        int start = Math.max(2, page - 1);
        int end = Math.min(count - 1, page + 1);
        if (start > 2) {
            window.add(ELLIPSIS);
        }
        for (int index = start; index <= end; index++) {
            window.add(index);
        }
        if (end < count - 1) {
            window.add(ELLIPSIS);
        }
        window.add(count);
        return window;
    }
}
