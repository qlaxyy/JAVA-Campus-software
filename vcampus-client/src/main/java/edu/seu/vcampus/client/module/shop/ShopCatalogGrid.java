package edu.seu.vcampus.client.module.shop;

/**
 * Chooses a square tile grid that fits the catalog canvas.
 */
final class ShopCatalogGrid {

    static final int GAP = 14;
    static final int MIN_CELL = 176;
    static final int MAX_CELL = 260;

    record Plan(int columns, int rows, int cellSize, int pageSize) {

        int gridWidth() {
            return columns * cellSize + Math.max(0, columns - 1) * GAP;
        }

        int gridHeight() {
            return rows * cellSize + Math.max(0, rows - 1) * GAP;
        }
    }

    private ShopCatalogGrid() {
    }

    /**
     * Builds a square-cell plan. {@code fixedPageSize} null means fill the window.
     *
     * @param width canvas width
     * @param height canvas height
     * @param fixedPageSize optional explicit count, or {@code null} for auto
     * @return columns, rows and tile size
     */
    static Plan plan(int width, int height, Integer fixedPageSize) {
        int areaWidth = Math.max(1, width);
        int areaHeight = Math.max(1, height);
        if (fixedPageSize != null && fixedPageSize > 0) {
            return fixedPlan(areaWidth, areaHeight, fixedPageSize);
        }
        int columns = Math.max(1, (areaWidth + GAP) / (MIN_CELL + GAP));
        int rows = Math.max(1, (areaHeight + GAP) / (MIN_CELL + GAP));
        int cell = squareCell(areaWidth, areaHeight, columns, rows);
        while (cell < MIN_CELL && (columns > 1 || rows > 1)) {
            if (columns >= rows && columns > 1) {
                columns--;
            } else if (rows > 1) {
                rows--;
            } else {
                columns = Math.max(1, columns - 1);
            }
            cell = squareCell(areaWidth, areaHeight, columns, rows);
        }
        return new Plan(columns, rows, cell, columns * rows);
    }

    private static Plan fixedPlan(int width, int height, int pageSize) {
        int columns = preferredColumns(pageSize);
        int rows = Math.max(1, (pageSize + columns - 1) / columns);
        while (columns > 1 && squareCell(width, height, columns, rows) < MIN_CELL) {
            columns--;
            rows = Math.max(1, (pageSize + columns - 1) / columns);
        }
        int cell = squareCell(width, height, columns, rows);
        return new Plan(columns, rows, cell, pageSize);
    }

    private static int preferredColumns(int pageSize) {
        if (pageSize <= 4) {
            return 2;
        }
        if (pageSize <= 6) {
            return 3;
        }
        if (pageSize <= 9) {
            return 3;
        }
        return 4;
    }

    private static int squareCell(int width, int height, int columns, int rows) {
        int cellWidth = (width - Math.max(0, columns - 1) * GAP) / Math.max(1, columns);
        int cellHeight = (height - Math.max(0, rows - 1) * GAP) / Math.max(1, rows);
        return Math.max(120, Math.min(MAX_CELL, Math.min(cellWidth, cellHeight)));
    }
}
