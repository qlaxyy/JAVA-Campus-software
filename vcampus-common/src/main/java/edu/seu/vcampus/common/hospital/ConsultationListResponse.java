package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Consultation history owned by the currently authenticated patient. */
public final class ConsultationListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<ConsultationRecordView> consultations;

    public ConsultationListResponse(List<ConsultationRecordView> consultations) {
        this.consultations = List.copyOf(Objects.requireNonNull(
                consultations, "consultations must not be null"));
    }

    public List<ConsultationRecordView> getConsultations() {
        return consultations;
    }
}
