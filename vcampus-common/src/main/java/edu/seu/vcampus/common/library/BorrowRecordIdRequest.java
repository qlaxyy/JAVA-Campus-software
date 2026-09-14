package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Identifies one borrow record owned by the authenticated reader. */
public final class BorrowRecordIdRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String recordId;

    public BorrowRecordIdRequest(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordId() {
        return recordId;
    }
}
