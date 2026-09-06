package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * One merchant listing or catalog-change row.
 */
public final class ShopListingRecordDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long recordId;
    private final long productId;
    private final String productName;
    private final String action;
    private final String detail;
    private final String operatorName;
    private final String createdAt;

    /**
     * Creates a listing-log snapshot.
     *
     * @param recordId log key
     * @param productId catalog key
     * @param productName title at the time of the event
     * @param action 上架 or 调整
     * @param detail human-readable change
     * @param operatorName merchant display name
     * @param createdAt local date-time
     */
    public ShopListingRecordDto(
            long recordId,
            long productId,
            String productName,
            String action,
            String detail,
            String operatorName,
            String createdAt) {
        this.recordId = recordId;
        this.productId = productId;
        this.productName = requireText(productName, "productName");
        this.action = requireText(action, "action");
        this.detail = requireText(detail, "detail");
        this.operatorName = requireText(operatorName, "operatorName");
        this.createdAt = requireText(createdAt, "createdAt");
    }

    public long getRecordId() {
        return recordId;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
