package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Return request; ownership is determined from the server session, not the payload. */
public final class BookReturnRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String recordId;

    /** @param recordId stable identifier of the borrow record to return */
    public BookReturnRequest(String recordId) {
        this.recordId = recordId;
    }

    /** @return borrow record identifier */
    public String getRecordId() {
        return recordId;
    }
}
