package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Examination orders owned by the current patient. */
public final class ExaminationListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<ExaminationOrderView> examinations;

    public ExaminationListResponse(List<ExaminationOrderView> examinations) {
        this.examinations = List.copyOf(Objects.requireNonNull(
                examinations, "examinations must not be null"));
    }

    public List<ExaminationOrderView> getExaminations() {
        return examinations;
    }
}
