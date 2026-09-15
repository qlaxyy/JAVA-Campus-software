package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Outcome of an idempotent weekly generation request. */
public final class WeeklyScheduleGenerationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int created;
    private final int existing;
    private final int elapsed;

    public WeeklyScheduleGenerationView(int created, int existing, int elapsed) {
        if (created < 0 || existing < 0 || elapsed < 0) {
            throw new IllegalArgumentException("schedule generation counts must not be negative");
        }
        this.created = created;
        this.existing = existing;
        this.elapsed = elapsed;
    }

    public int getCreated() { return created; }

    public int getExisting() { return existing; }

    public int getElapsed() { return elapsed; }
}
