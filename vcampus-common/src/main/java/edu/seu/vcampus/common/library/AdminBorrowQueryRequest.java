package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Selects one administrator borrow-record view. */
public final class AdminBorrowQueryRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String CURRENT = "CURRENT";
    public static final String HISTORY = "HISTORY";
    public static final String OVERDUE = "OVERDUE";

    private final String scope;

    public AdminBorrowQueryRequest(String scope) {
        this.scope = scope;
    }

    public String getScope() { return scope; }
}
