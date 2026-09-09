package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * On-sale products matching a {@link ListProductsRequest}.
 */
public final class ListProductsResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<ProductSummaryDto> products;

    /**
     * Creates a list-products payload.
     *
     * @param products matching catalog rows
     */
    public ListProductsResponse(List<ProductSummaryDto> products) {
        Objects.requireNonNull(products, "products must not be null");
        this.products = new ArrayList<>(products);
    }

    /** @return a defensive copy of matching products */
    public List<ProductSummaryDto> getProducts() {
        return List.copyOf(products);
    }
}
