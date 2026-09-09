package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** One signed clinical record with examinations from the same clinical episode. */
public final class DoctorClinicalRecordView implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ConsultationRecordView consultation;
    private final List<ExaminationOrderView> episodeExaminations;

    public DoctorClinicalRecordView(
            ConsultationRecordView consultation,
            List<ExaminationOrderView> episodeExaminations) {
        this.consultation = Objects.requireNonNull(
                consultation, "consultation must not be null");
        this.episodeExaminations = List.copyOf(Objects.requireNonNull(
                episodeExaminations, "episodeExaminations must not be null"));
    }

    public ConsultationRecordView getConsultation() {
        return consultation;
    }

    public List<ExaminationOrderView> getEpisodeExaminations() {
        return episodeExaminations;
    }
}
