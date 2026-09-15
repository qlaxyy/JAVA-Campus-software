package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Selects one administrator borrow-record view, optionally narrowed to a single reader. */
public final class AdminBorrowQueryRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    public static final String CURRENT = "CURRENT";
    public static final String HISTORY = "HISTORY";
    public static final String OVERDUE = "OVERDUE";

    private final String scope;
    private final String userId;

    /** Queries every reader; equivalent to {@code new AdminBorrowQueryRequest(scope, null)}. */
    public AdminBorrowQueryRequest(String scope) {
        this(scope, null);
    }

    /**
     * @param scope one of {@link #CURRENT}, {@link #HISTORY} or {@link #OVERDUE}
     * @param userId stable reader id to narrow the result to, or {@code null} for every reader
     */
    public AdminBorrowQueryRequest(String scope, String userId) {
        this.scope = scope;
        this.userId = userId;
    }

    public String getScope() { return scope; }

    /** @return reader id filter, or {@code null} when the query covers every reader */
    public String getUserId() { return userId; }
}
