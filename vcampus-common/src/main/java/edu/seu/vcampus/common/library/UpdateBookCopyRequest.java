package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Edits mutable physical-copy metadata without exposing status or identity fields. */
public final class UpdateBookCopyRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String copyId;
    private final String location;
    private final String callNumber;

    public UpdateBookCopyRequest(String copyId, String location, String callNumber) {
        this.copyId = copyId;
        this.location = location;
        this.callNumber = callNumber;
    }

    public String getCopyId() { return copyId; }

    public String getLocation() { return location; }

    public String getCallNumber() { return callNumber; }
}
