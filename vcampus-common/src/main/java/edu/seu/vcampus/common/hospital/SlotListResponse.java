package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Immutable seven-day slot-search payload returned by the server. */
public final class SlotListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<SlotView> slots;

    public SlotListResponse(List<SlotView> slots) {
        this.slots = List.copyOf(Objects.requireNonNull(slots, "slots must not be null"));
    }

    public List<SlotView> getSlots() {
        return slots;
    }
}
