package edu.seu.vcampus.common.card;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Recent campus-card movements for the current user.
 */
public final class CampusCardLedgerResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<CampusCardLedgerEntry> entries;

    /**
     * Creates a ledger page.
     *
     * @param entries newest-first movements
     */
    public CampusCardLedgerResponse(List<CampusCardLedgerEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        this.entries = List.copyOf(new ArrayList<>(entries));
    }

    public List<CampusCardLedgerEntry> getEntries() {
        return entries;
    }
}
