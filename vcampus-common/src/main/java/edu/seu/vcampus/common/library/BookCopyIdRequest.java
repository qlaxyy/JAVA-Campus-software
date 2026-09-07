package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Identifies one physical copy for a copy-specific administrator operation. */
public final class BookCopyIdRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String copyId;

    public BookCopyIdRequest(String copyId) {
        this.copyId = copyId;
    }

    public String getCopyId() {
        return copyId;
    }
}
