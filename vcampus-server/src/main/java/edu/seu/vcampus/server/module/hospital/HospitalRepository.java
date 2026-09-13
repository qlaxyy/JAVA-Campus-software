package edu.seu.vcampus.server.module.hospital;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data boundary that can later be implemented with Access/JDBC. */
interface HospitalRepository {

    Optional<HospitalDoctor> findActiveDoctorByUserId(String userId);

    List<HospitalDoctor> findActiveDoctors();

    List<HospitalDoctor> findAllDoctors();

    default boolean isActiveDoctorUser(String userId) {
        return userId != null && findActiveDoctorByUserId(userId).isPresent();
    }

    List<DoctorApplication> findDoctorApplications();

    Optional<DoctorApplication> findDoctorApplication(String requestId);

    void saveDoctorApplication(DoctorApplication application);

    void saveDoctorProfile(DoctorProfile profile);

    List<HospitalDepartment> findActiveDepartments();

    List<HospitalDepartment> findAllDepartments();

    void insertDepartment(HospitalDepartment department);

    void updateDepartment(HospitalDepartment department);

    List<HospitalSlot> findSlots(LocalDate startDate, LocalDate endDate);

    List<HospitalSlot> findAllSlots();

    List<HospitalSlot> findSlotsByDoctorId(String doctorId);

    Optional<HospitalSlot> findSlotById(String scheduleId);

    void insertSlot(HospitalSlot slot);

    void updateSlot(HospitalSlot slot);

    List<HospitalAppointment> findAppointmentsByScheduleId(String scheduleId);

    List<HospitalBooking> findBookingsByPatientUserId(String patientUserId);

    Optional<HospitalBooking> findBookingById(String appointmentId);

    Optional<HospitalAppointment> findAppointmentById(String appointmentId);

    List<HospitalPatientBill> findBillsByPatientUserId(String patientUserId);

    Optional<HospitalPatientBill> findBillById(String billId);

    void updatePatientBill(HospitalPatientBill bill);

    Optional<HospitalPatientProfile> findPatientProfile(String patientUserId);

    void savePatientProfile(HospitalPatientProfile profile);

    /** Saves only when the stored version still equals the version seen by the client. */
    boolean savePatientProfileIfVersion(
            HospitalPatientProfile profile,
            long expectedVersion);

    List<HospitalConsultation> findConsultationsByPatientUserId(String patientUserId);

    List<HospitalConsultation> findConsultationsByDoctorId(String doctorId);

    Optional<HospitalConsultation> findConsultationById(String consultationId);

    Optional<HospitalConsultation> findConsultationByAppointmentId(String appointmentId);

    Optional<HospitalEpisode> findEpisodeById(String episodeId);

    List<HospitalExaminationOrder> findExaminationOrdersByPatientUserId(
            String patientUserId);

    List<HospitalExaminationOrder> findExaminationOrdersByDoctorId(String doctorId);

    List<HospitalExaminationOrder> findExaminationOrdersByEpisodeId(String episodeId);

    Optional<HospitalExaminationOrder> findExaminationOrderById(String orderId);

    Optional<HospitalExaminationReport> findExaminationReportByOrderId(String orderId);

    /** Saves the appointment and its registration charge as one indivisible aggregate. */
    void saveBooking(HospitalBooking booking);

    /** Creates a first-visit appointment and its clinical episode atomically. */
    void saveBookingAndEpisode(HospitalBooking booking, HospitalEpisode episode);

    /** Replaces an existing appointment and bill as one indivisible aggregate. */
    void updateBooking(HospitalBooking booking);

    /** Cancels an appointment and updates its clinical episode atomically. */
    void updateBookingAndEpisode(HospitalBooking booking, HospitalEpisode episode);

    /** Saves a signed consultation and completes its appointment atomically. */
    void saveConsultationAndUpdateBooking(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode completedEpisode,
            HospitalPatientBill treatmentBill);

    /** Saves a stage note, completes the appointment, and opens an examination. */
    void saveExaminationPlan(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode waitingEpisode,
            HospitalExaminationOrder examinationOrder,
            HospitalPatientBill examinationBill);

    /** Reviews the previous result and opens the next examination in one transaction. */
    void saveResultReviewExaminationPlan(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode waitingEpisode,
            HospitalExaminationOrder reviewedOrder,
            HospitalExaminationOrder nextExaminationOrder,
            HospitalPatientBill examinationBill);

    /** Publishes a demo report and advances the episode to result-ready. */
    void saveExaminationReport(
            HospitalExaminationOrder reportedOrder,
            HospitalExaminationReport report,
            HospitalEpisode resultReadyEpisode);

    /** Completes a result-review consultation and closes its clinical episode. */
    void saveResultReviewConsultation(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode completedEpisode,
            HospitalExaminationOrder reviewedOrder);
}
