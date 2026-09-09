package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Merchant listing log returned by {@code SHOP.LIST_LISTINGS}.
 */
public final class ListListingsResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<ShopListingRecordDto> records;

    /**
     * Creates a listing-log payload.
     *
     * @param records newest first
     */
    public ListListingsResponse(List<ShopListingRecordDto> records) {
        Objects.requireNonNull(records, "records must not be null");
        this.records = new ArrayList<>(records);
    }

    public List<ShopListingRecordDto> getRecords() {
        return List.copyOf(records);
    }
}
