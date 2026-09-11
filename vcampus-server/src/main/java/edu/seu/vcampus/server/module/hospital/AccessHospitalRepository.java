package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.security.UserDirectory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Access persistence for hospital data, including atomic clinical transitions. */
final class AccessHospitalRepository implements HospitalRepository {

    private static final String APPLICATION_TABLE = "tblHospitalDoctorApplication";
    private static final String PROFILE_TABLE = "tblHospitalDoctor";
    private static final String DEPARTMENT_TABLE = "tblHospitalDepartment";
    private static final String SCHEDULE_TABLE = "tblHospitalSchedule";
    private static final String PATIENT_PROFILE_TABLE = "tblHospitalPatientProfile";
    private static final String EPISODE_TABLE = "tblHospitalEpisode";
    private static final String APPOINTMENT_TABLE = "tblHospitalAppointment";
    private static final String BILL_TABLE = "tblHospitalBill";
    private static final String BILL_ITEM_TABLE = "tblHospitalBillItem";

    private final AccessDatabase database;
    private final Clock clock;
    private final UserDirectory users;

    AccessHospitalRepository(AccessDatabase database, Clock clock) {
        this(database, clock, null);
    }

    AccessHospitalRepository(AccessDatabase database, Clock clock, UserDirectory users) {
        this.database = database;
        this.clock = clock;
        this.users = users;
        initializeSchema();
        seedReferenceData();
    }

    @Override
    public boolean isActiveDoctorUser(String userId) {
        if (userId == null) {
            return false;
        }
        String sql = "SELECT * FROM tblHospitalDoctor WHERE userId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean("active");
            }
        } catch (SQLException exception) {
            throw failure("Cannot read doctor profile.", exception);
        }
    }

    @Override
    public Optional<HospitalDoctor> findActiveDoctorByUserId(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM tblHospitalDoctor WHERE userId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean("active")) {
                    return Optional.empty();
                }
                return Optional.of(new HospitalDoctor(
                        result.getString("doctorId"),
                        result.getString("userId"),
                        result.getString("departmentId"),
                        currentName(result.getString("userId"), result.getString("doctorName")),
                        result.getString("doctorTitle"),
                        true));
            }
        } catch (SQLException exception) {
            throw failure("Cannot read doctor profile.", exception);
        }
    }

    @Override
    public List<HospitalDoctor> findActiveDoctors() {
        String sql = "SELECT * FROM tblHospitalDoctor WHERE [active] = TRUE";
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<HospitalDoctor> doctors = new ArrayList<>();
            while (result.next()) {
                doctors.add(new HospitalDoctor(
                        result.getString("doctorId"),
                        nullableText(result.getString("userId")),
                        result.getString("departmentId"),
                        currentName(nullableText(result.getString("userId")), result.getString("doctorName")),
                        result.getString("doctorTitle"),
                        true));
            }
            return doctors;
        } catch (SQLException exception) {
            throw failure("Cannot list active doctors.", exception);
        }
    }

    @Override
    public List<HospitalDoctor> findAllDoctors() {
        String sql = "SELECT * FROM tblHospitalDoctor";
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<HospitalDoctor> doctors = new ArrayList<>();
            while (result.next()) {
                doctors.add(new HospitalDoctor(
                        result.getString("doctorId"),
                        nullableText(result.getString("userId")),
                        result.getString("departmentId"),
                        currentName(nullableText(result.getString("userId")), result.getString("doctorName")),
                        result.getString("doctorTitle"),
                        result.getBoolean("active")));
            }
            return doctors;
        } catch (SQLException exception) {
            throw failure("Cannot list doctors.", exception);
        }
    }

    @Override
    public List<DoctorApplication> findDoctorApplications() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM tblHospitalDoctorApplication ORDER BY createdAt DESC")) {
            List<DoctorApplication> applications = new ArrayList<>();
            while (result.next()) {
                applications.add(readApplication(result));
            }
            return applications;
        } catch (SQLException exception) {
            throw failure("Cannot list doctor applications.", exception);
        }
    }

    @Override
    public Optional<DoctorApplication> findDoctorApplication(String requestId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM tblHospitalDoctorApplication WHERE requestId = ?")) {
            statement.setString(1, requestId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readApplication(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Cannot read doctor application.", exception);
        }
    }

    @Override
    public synchronized void saveDoctorApplication(DoctorApplication application) {
        try (Connection connection = database.openConnection()) {
            if (applicationExists(connection, application.requestId())) {
                updateApplication(connection, application);
            } else {
                insertApplication(connection, application);
            }
        } catch (SQLException exception) {
            throw failure("Cannot save doctor application.", exception);
        }
    }

    @Override
    public synchronized void saveDoctorProfile(DoctorProfile profile) {
        try (Connection connection = database.openConnection()) {
            boolean existing = profileExists(connection, profile.userId());
            boolean legacyUserKey = columnExists(
                    connection, PROFILE_TABLE, "doctorUserId");
            boolean legacyNumberColumn = columnExists(
                    connection, PROFILE_TABLE, "doctorNumber");
            LocalDateTime now = LocalDateTime.now(clock);
            String sql = existing
                    ? "UPDATE tblHospitalDoctor SET departmentId = ?, doctorName = ?, "
                            + "doctorTitle = ?, [active] = ?, updatedAt = ? WHERE userId = ?"
                    : "INSERT INTO tblHospitalDoctor "
                            + (legacyUserKey ? "(doctorUserId, " : "(")
                            + (legacyNumberColumn ? "doctorNumber, " : "")
                            + "doctorId, userId, departmentId, doctorName, doctorTitle, "
                            + "[active], createdAt, updatedAt) VALUES ("
                            + (legacyUserKey ? "?, " : "")
                            + (legacyNumberColumn ? "?, " : "")
                            + "?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (existing) {
                    statement.setString(1, profile.departmentId());
                    statement.setString(2, profile.doctorName());
                    statement.setString(3, profile.doctorTitle());
                    statement.setBoolean(4, profile.active());
                    statement.setTimestamp(5, Timestamp.valueOf(now));
                    statement.setString(6, profile.userId());
                } else {
                    int index = 1;
                    if (legacyUserKey) {
                        statement.setString(index++, profile.userId());
                    }
                    if (legacyNumberColumn) {
                        statement.setString(index++,
                                legacyValue("LEGACY_", profile.userId()));
                    }
                    statement.setString(index++, UUID.randomUUID().toString());
                    statement.setString(index++, profile.userId());
                    statement.setString(index++, profile.departmentId());
                    statement.setString(index++, profile.doctorName());
                    statement.setString(index++, profile.doctorTitle());
                    statement.setBoolean(index++, profile.active());
                    statement.setTimestamp(index++, Timestamp.valueOf(now));
                    statement.setTimestamp(index, Timestamp.valueOf(now));
                }
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("Cannot save doctor profile.", exception);
        }
    }

    @Override
    public List<HospitalDepartment> findActiveDepartments() {
        String sql = "SELECT * FROM tblHospitalDepartment";
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<HospitalDepartment> departments = new ArrayList<>();
            while (result.next()) {
                if ("ACTIVE".equalsIgnoreCase(result.getString("status"))) {
                    departments.add(new HospitalDepartment(
                            result.getString("departmentId"),
                            result.getString("departmentName"),
                            nullableText(result.getString("parentDepartmentId")),
                            result.getBoolean("bookable"),
                            true));
                }
            }
            return departments;
        } catch (SQLException exception) {
            throw failure("Cannot list hospital departments.", exception);
        }
    }

    @Override
    public List<HospitalDepartment> findAllDepartments() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM tblHospitalDepartment")) {
            List<HospitalDepartment> departments = new ArrayList<>();
            while (result.next()) {
                departments.add(new HospitalDepartment(
                        result.getString("departmentId"),
                        result.getString("departmentName"),
                        nullableText(result.getString("parentDepartmentId")),
                        result.getBoolean("bookable"),
                        "ACTIVE".equalsIgnoreCase(result.getString("status"))));
            }
            return departments;
        } catch (SQLException exception) {
            throw failure("Cannot list hospital departments.", exception);
        }
    }

    @Override
    public synchronized void insertDepartment(HospitalDepartment department) {
        String sql = "INSERT INTO tblHospitalDepartment "
                + "(departmentId, departmentCode, departmentName, parentDepartmentId, "
                + "bookable, description, status, createdAt, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now(clock);
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, department.departmentId());
            statement.setString(2, departmentCode(department.departmentId()));
            statement.setString(3, department.departmentName());
            statement.setString(4, department.parentDepartmentId());
            statement.setBoolean(5, department.bookable());
            statement.setString(6, null);
            statement.setString(7, department.active() ? "ACTIVE" : "INACTIVE");
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Cannot insert hospital department.", exception);
        }
    }

    @Override
    public synchronized void updateDepartment(HospitalDepartment department) {
        String sql = "UPDATE tblHospitalDepartment SET departmentName = ?, "
                + "parentDepartmentId = ?, bookable = ?, status = ?, updatedAt = ? "
                + "WHERE departmentId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, department.departmentName());
            statement.setString(2, department.parentDepartmentId());
            statement.setBoolean(3, department.bookable());
            statement.setString(4, department.active() ? "ACTIVE" : "INACTIVE");
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now(clock)));
            statement.setString(6, department.departmentId());
            requireSingleUpdate(statement.executeUpdate(), "department update");
        } catch (SQLException exception) {
            throw failure("Cannot update hospital department.", exception);
        }
    }

    @Override
    public List<HospitalSlot> findSlots(LocalDate startDate, LocalDate endDate) {
        String sql = slotSelect() + " WHERE s.startTime >= ? AND s.startTime < ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
            return readSlots(statement);
        } catch (SQLException exception) {
            throw failure("Cannot search hospital schedules.", exception);
        }
    }

    @Override
    public List<HospitalSlot> findAllSlots() {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(slotSelect())) {
            return readSlots(statement);
        } catch (SQLException exception) {
            throw failure("Cannot list hospital schedules.", exception);
        }
    }

    @Override
    public List<HospitalSlot> findSlotsByDoctorId(String doctorId) {
        String sql = slotSelect() + " WHERE s.doctorId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, doctorId);
            return readSlots(statement);
        } catch (SQLException exception) {
            throw failure("Cannot list schedules for doctor.", exception);
        }
    }

    @Override
    public Optional<HospitalSlot> findSlotById(String scheduleId) {
        String sql = slotSelect() + " WHERE s.scheduleId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scheduleId);
            List<HospitalSlot> slots = readSlots(statement);
            return slots.isEmpty() ? Optional.empty() : Optional.of(slots.getFirst());
        } catch (SQLException exception) {
            throw failure("Cannot read hospital schedule.", exception);
        }
    }

    @Override
    public synchronized void insertSlot(HospitalSlot slot) {
        String sql = "INSERT INTO tblHospitalSchedule "
                + "(scheduleId, departmentId, doctorId, startTime, endTime, "
                + "registrationFeeCents, capacity, status, createdAt, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now(clock);
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSlot(statement, slot, now, false);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Cannot create hospital schedule.", exception);
        }
    }

    @Override
    public synchronized void updateSlot(HospitalSlot slot) {
        String sql = "UPDATE tblHospitalSchedule SET departmentId = ?, doctorId = ?, "
                + "startTime = ?, endTime = ?, registrationFeeCents = ?, capacity = ?, "
                + "status = ?, updatedAt = ? WHERE scheduleId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSlot(statement, slot, LocalDateTime.now(clock), true);
            requireSingleUpdate(statement.executeUpdate(), "schedule");
        } catch (SQLException exception) {
            throw failure("Cannot update hospital schedule.", exception);
        }
    }

    @Override
    public List<HospitalAppointment> findAppointmentsByScheduleId(String scheduleId) {
        String sql = appointmentSelect() + " WHERE scheduleId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scheduleId);
            return readAppointments(statement);
        } catch (SQLException exception) {
            throw failure("Cannot list appointments for schedule.", exception);
        }
    }

    @Override
    public List<HospitalBooking> findBookingsByPatientUserId(String patientUserId) {
        String sql = bookingSelect()
                + " WHERE b.billType = 'REGISTRATION' AND a.patientUserId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientUserId);
            return readBookings(statement);
        } catch (SQLException exception) {
            throw failure("Cannot list patient appointments.", exception);
        }
    }

    @Override
    public Optional<HospitalBooking> findBookingById(String appointmentId) {
        String sql = bookingSelect()
                + " WHERE b.billType = 'REGISTRATION' AND a.appointmentId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appointmentId);
            List<HospitalBooking> bookings = readBookings(statement);
            return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getFirst());
        } catch (SQLException exception) {
            throw failure("Cannot read hospital appointment booking.", exception);
        }
    }

    @Override
    public Optional<HospitalAppointment> findAppointmentById(String appointmentId) {
        String sql = appointmentSelect() + " WHERE appointmentId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appointmentId);
            List<HospitalAppointment> appointments = readAppointments(statement);
            return appointments.isEmpty()
                    ? Optional.empty() : Optional.of(appointments.getFirst());
        } catch (SQLException exception) {
            throw failure("Cannot read hospital appointment.", exception);
        }
    }

    @Override
    public List<HospitalPatientBill> findBillsByPatientUserId(String patientUserId) {
        String sql = patientBillSelect() + " WHERE b.patientUserId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientUserId);
            return readPatientBills(statement);
        } catch (SQLException exception) {
            throw failure("Cannot list patient hospital bills.", exception);
        }
    }

    @Override
    public Optional<HospitalPatientBill> findBillById(String billId) {
        String sql = patientBillSelect() + " WHERE b.billId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, billId);
            return readPatientBills(statement).stream().findFirst();
        } catch (SQLException exception) {
            throw failure("Cannot read patient hospital bill.", exception);
        }
    }

    @Override
    public synchronized void updatePatientBill(HospitalPatientBill bill) {
        String sql = "UPDATE tblHospitalBill SET paymentStatus = ?, paidAt = ?, "
                + "refundedAt = ? WHERE billId = ? AND patientUserId = ? "
                + "AND paymentStatus = 'UNPAID'";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bill.paymentStatus().name());
            setNullableTimestamp(statement, 2, bill.paidAt());
            setNullableTimestamp(statement, 3, bill.refundedAt());
            statement.setString(4, bill.billId());
            statement.setString(5, bill.patientUserId());
            requireSingleUpdate(statement.executeUpdate(), "hospital bill payment");
        } catch (SQLException exception) {
            throw failure("Cannot update patient hospital bill.", exception);
        }
    }

    @Override
    public Optional<HospitalPatientProfile> findPatientProfile(String patientUserId) {
        if (patientUserId == null) {
            return Optional.empty();
        }
        String sql = "SELECT patientUserId, bloodType, allergies, medicalHistory, "
                + "longTermMedication, emergencyContact, updatedAt "
                + "FROM tblHospitalPatientProfile WHERE patientUserId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientUserId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(readPatientProfile(result))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Cannot read patient health profile.", exception);
        }
    }

    @Override
    public synchronized void savePatientProfile(HospitalPatientProfile profile) {
        try (Connection connection = database.openConnection()) {
            savePatientProfile(connection, profile);
        } catch (SQLException exception) {
            throw failure("Cannot save patient health profile.", exception);
        }
    }

    @Override
    public List<HospitalConsultation> findConsultationsByPatientUserId(
            String patientUserId) {
        return queryClinical("SELECT * FROM tblHospitalConsultation WHERE patientUserId = ?",
                patientUserId, this::readConsultation);
    }

    @Override
    public List<HospitalConsultation> findConsultationsByDoctorId(String doctorId) {
        return queryClinical("SELECT * FROM tblHospitalConsultation WHERE doctorId = ?",
                doctorId, this::readConsultation);
    }

    @Override
    public Optional<HospitalConsultation> findConsultationById(String consultationId) {
        return queryClinical("SELECT * FROM tblHospitalConsultation WHERE consultationId = ?",
                consultationId, this::readConsultation).stream().findFirst();
    }

    @Override
    public Optional<HospitalConsultation> findConsultationByAppointmentId(
            String appointmentId) {
        return queryClinical("SELECT * FROM tblHospitalConsultation WHERE appointmentId = ?",
                appointmentId, this::readConsultation).stream().findFirst();
    }

    @Override
    public Optional<HospitalEpisode> findEpisodeById(String episodeId) {
        String sql = episodeSelect() + " WHERE episodeId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, episodeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readEpisode(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Cannot read clinical episode.", exception);
        }
    }

    @Override
    public List<HospitalExaminationOrder> findExaminationOrdersByPatientUserId(
            String patientUserId) {
        return queryClinical("SELECT * FROM tblHospitalExaminationOrder WHERE patientUserId = ?",
                patientUserId, this::readExaminationOrder);
    }

    @Override
    public List<HospitalExaminationOrder> findExaminationOrdersByDoctorId(
            String doctorId) {
        return queryClinical("SELECT * FROM tblHospitalExaminationOrder WHERE doctorId = ?",
                doctorId, this::readExaminationOrder);
    }

    @Override
    public List<HospitalExaminationOrder> findExaminationOrdersByEpisodeId(
            String episodeId) {
        return queryClinical("SELECT * FROM tblHospitalExaminationOrder WHERE episodeId = ?",
                episodeId, this::readExaminationOrder);
    }

    @Override
    public Optional<HospitalExaminationOrder> findExaminationOrderById(String orderId) {
        return queryClinical("SELECT * FROM tblHospitalExaminationOrder WHERE orderId = ?",
                orderId, this::readExaminationOrder).stream().findFirst();
    }

    @Override
    public Optional<HospitalExaminationReport> findExaminationReportByOrderId(
            String orderId) {
        return queryClinical("SELECT * FROM tblHospitalExaminationReport WHERE orderId = ?",
                orderId, result -> new HospitalExaminationReport(
                        result.getString("reportId"), result.getString("orderId"),
                        result.getString("summary"),
                        result.getTimestamp("reportedAt").toLocalDateTime()))
                .stream().findFirst();
    }

    @Override
    public synchronized void saveBooking(HospitalBooking booking) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                insertBooking(connection, booking);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot save hospital appointment.", exception);
        }
    }

    @Override
    public synchronized void saveBookingAndEpisode(
            HospitalBooking booking,
            HospitalEpisode episode) {
        validateBookingEpisode(booking, episode);
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                insertEpisode(connection, episode);
                insertBooking(connection, booking);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot save appointment and clinical episode.", exception);
        }
    }

    @Override
    public synchronized void updateBooking(HospitalBooking booking) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateBooking(connection, booking);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot update hospital appointment.", exception);
        }
    }

    @Override
    public synchronized void updateBookingAndEpisode(
            HospitalBooking booking,
            HospitalEpisode episode) {
        validateBookingEpisode(booking, episode);
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateBooking(connection, booking);
                updateEpisode(connection, episode);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot update appointment and clinical episode.", exception);
        }
    }

    @Override
    public synchronized void saveConsultationAndUpdateBooking(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode completedEpisode,
            HospitalPatientBill treatmentBill) {
        clinicalTransaction(connection -> {
            validateClinicalTransition(connection, consultation, completedBooking,
                    completedEpisode, EpisodeStatus.IN_PROGRESS, ConsultationOutcome.COMPLETED);
            if (completedBooking.appointment().visitType() == VisitType.RESULT_REVIEW) {
                throw new IllegalArgumentException("result review requires its examination order");
            }
            validateClinicalBill(treatmentBill, completedBooking, HospitalBillType.TREATMENT);
            insertConsultation(connection, consultation);
            completeAppointment(connection, completedBooking.appointment());
            insertPatientBill(connection, treatmentBill);
            transitionEpisode(connection, completedEpisode, EpisodeStatus.IN_PROGRESS);
        });
    }

    @Override
    public synchronized void saveExaminationPlan(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode waitingEpisode,
            HospitalExaminationOrder examinationOrder,
            HospitalPatientBill examinationBill) {
        clinicalTransaction(connection -> {
            validateClinicalTransition(connection, consultation, completedBooking,
                    waitingEpisode, EpisodeStatus.IN_PROGRESS, ConsultationOutcome.WAITING_FOR_RESULTS);
            if (completedBooking.appointment().visitType() == VisitType.RESULT_REVIEW
                    || !examinationOrder.orderedAppointmentId().equals(consultation.appointmentId())
                    || !examinationOrder.doctorId().equals(consultation.doctorId())
                    || examinationOrder.status() != ExaminationStatus.ORDERED) {
                throw new IllegalArgumentException("examination order does not match consultation");
            }
            validateOrderEpisode(examinationOrder, waitingEpisode);
            validateClinicalBill(
                    examinationBill, completedBooking, HospitalBillType.EXAMINATION);
            insertConsultation(connection, consultation);
            completeAppointment(connection, completedBooking.appointment());
            insertExaminationOrder(connection, examinationOrder);
            insertPatientBill(connection, examinationBill);
            transitionEpisode(connection, waitingEpisode, EpisodeStatus.IN_PROGRESS);
        });
    }

    @Override
    public synchronized void saveResultReviewExaminationPlan(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode waitingEpisode,
            HospitalExaminationOrder reviewedOrder,
            HospitalExaminationOrder nextExaminationOrder,
            HospitalPatientBill examinationBill) {
        clinicalTransaction(connection -> {
            validateClinicalTransition(connection, consultation, completedBooking,
                    waitingEpisode, EpisodeStatus.RESULT_READY,
                    ConsultationOutcome.WAITING_FOR_RESULTS);
            if (completedBooking.appointment().visitType() != VisitType.RESULT_REVIEW
                    || reviewedOrder.status() != ExaminationStatus.REVIEWED
                    || !reviewedOrder.orderedAppointmentId().equals(
                            completedBooking.appointment().sourceFirstVisitAppointmentId())
                    || !nextExaminationOrder.orderedAppointmentId()
                            .equals(consultation.appointmentId())
                    || !nextExaminationOrder.doctorId().equals(consultation.doctorId())
                    || nextExaminationOrder.status() != ExaminationStatus.ORDERED
                    || !exists(connection,
                            "SELECT reportId FROM tblHospitalExaminationReport WHERE orderId = ?",
                            reviewedOrder.orderId())) {
                throw new IllegalArgumentException(
                        "repeated examination does not match result-review visit");
            }
            validateOrderEpisode(reviewedOrder, waitingEpisode);
            validateOrderEpisode(nextExaminationOrder, waitingEpisode);
            validateClinicalBill(
                    examinationBill, completedBooking, HospitalBillType.EXAMINATION);
            insertConsultation(connection, consultation);
            completeAppointment(connection, completedBooking.appointment());
            transitionOrder(connection, reviewedOrder, ExaminationStatus.RESULT_READY);
            insertExaminationOrder(connection, nextExaminationOrder);
            insertPatientBill(connection, examinationBill);
            transitionEpisode(connection, waitingEpisode, EpisodeStatus.RESULT_READY);
        });
    }

    @Override
    public synchronized void saveExaminationReport(
            HospitalExaminationOrder reportedOrder,
            HospitalExaminationReport report,
            HospitalEpisode resultReadyEpisode) {
        clinicalTransaction(connection -> {
            validateOrderEpisode(reportedOrder, resultReadyEpisode);
            if (!report.orderId().equals(reportedOrder.orderId())
                    || reportedOrder.status() != ExaminationStatus.RESULT_READY
                    || resultReadyEpisode.status() != EpisodeStatus.RESULT_READY) {
                throw new IllegalArgumentException("report does not match clinical transition");
            }
            transitionOrder(connection, reportedOrder, ExaminationStatus.ORDERED);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tblHospitalExaminationReport "
                            + "(reportId, orderId, summary, reportedAt, sourceType) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, report.reportId());
                statement.setString(2, report.orderId());
                statement.setString(3, report.resultSummary());
                statement.setTimestamp(4, Timestamp.valueOf(report.reportedAt()));
                statement.setString(5, "DEMO");
                statement.executeUpdate();
            }
            transitionEpisode(connection, resultReadyEpisode, EpisodeStatus.WAITING_FOR_RESULTS);
        });
    }

    @Override
    public synchronized void saveResultReviewConsultation(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode completedEpisode,
            HospitalExaminationOrder reviewedOrder) {
        clinicalTransaction(connection -> {
            validateClinicalTransition(connection, consultation, completedBooking,
                    completedEpisode, EpisodeStatus.RESULT_READY, ConsultationOutcome.COMPLETED);
            validateOrderEpisode(reviewedOrder, completedEpisode);
            if (completedBooking.appointment().visitType() != VisitType.RESULT_REVIEW
                    || !reviewedOrder.orderedAppointmentId().equals(
                            completedBooking.appointment().sourceFirstVisitAppointmentId())
                    || reviewedOrder.status() != ExaminationStatus.REVIEWED
                    || !exists(connection, "SELECT reportId FROM tblHospitalExaminationReport "
                            + "WHERE orderId = ?", reviewedOrder.orderId())) {
                throw new IllegalArgumentException("result review does not match reported examination");
            }
            insertConsultation(connection, consultation);
            completeAppointment(connection, completedBooking.appointment());
            transitionOrder(connection, reviewedOrder, ExaminationStatus.RESULT_READY);
            transitionEpisode(connection, completedEpisode, EpisodeStatus.RESULT_READY);
        });
    }

    @FunctionalInterface
    private interface ClinicalWrite {
        void execute(Connection connection) throws SQLException;
    }

    private void clinicalTransaction(ClinicalWrite write) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                write.execute(connection);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot save hospital clinical transition.", exception);
        }
    }

    private void validateClinicalTransition(
            Connection connection, HospitalConsultation consultation, HospitalBooking booking,
            HospitalEpisode episode, EpisodeStatus expectedEpisode, ConsultationOutcome outcome)
            throws SQLException {
        validateBookingEpisode(booking, episode);
        HospitalAppointment appointment = booking.appointment();
        if (!consultation.appointmentId().equals(appointment.appointmentId())
                || !consultation.patientUserId().equals(appointment.patientUserId())
                || appointment.status() != AppointmentStatus.COMPLETED
                || consultation.outcome() != outcome
                || episode.status() != (outcome == ConsultationOutcome.COMPLETED
                        ? EpisodeStatus.COMPLETED : EpisodeStatus.WAITING_FOR_RESULTS)
                || (outcome == ConsultationOutcome.WAITING_FOR_RESULTS
                        && !consultation.medicationAdvice().isBlank())) {
            throw new IllegalArgumentException("clinical aggregate does not match transition");
        }
        // Recheck identity and state on the same connection that performs every write.
        try (PreparedStatement statement = connection.prepareStatement(
                appointmentSelect() + " WHERE appointmentId = ?")) {
            statement.setString(1, appointment.appointmentId());
            List<HospitalAppointment> found = readAppointments(statement);
            if (found.size() != 1) {
                throw new IllegalStateException("appointment does not exist");
            }
            HospitalAppointment current = found.getFirst();
            if (current.status() != AppointmentStatus.BOOKED
                    || !current.patientUserId().equals(appointment.patientUserId())
                    || !current.scheduleId().equals(appointment.scheduleId())
                    || !current.episodeId().equals(appointment.episodeId())
                    || current.visitType() != appointment.visitType()
                    || current.queueNumber() != appointment.queueNumber()
                    || !java.util.Objects.equals(current.sourceFirstVisitAppointmentId(),
                            appointment.sourceFirstVisitAppointmentId())) {
                throw new IllegalStateException("appointment changed or does not match consultation");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT s.doctorId FROM tblHospitalSchedule s INNER JOIN tblHospitalDoctor d "
                        + "ON s.doctorId = d.doctorId WHERE s.scheduleId = ? "
                        + "AND s.doctorId = ? AND s.departmentId = ? AND d.[active] = TRUE")) {
            statement.setString(1, appointment.scheduleId());
            statement.setString(2, consultation.doctorId());
            statement.setString(3, episode.departmentId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("consultation doctor is not assigned to appointment");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                episodeSelect() + " WHERE episodeId = ?")) {
            statement.setString(1, episode.episodeId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("clinical episode does not exist");
                }
                HospitalEpisode current = readEpisode(result);
                if (current.status() != expectedEpisode
                        || !current.patientUserId().equals(episode.patientUserId())
                        || !current.departmentId().equals(episode.departmentId())) {
                    throw new IllegalStateException("clinical episode changed or does not match");
                }
            }
        }
    }

    private void validateOrderEpisode(HospitalExaminationOrder order, HospitalEpisode episode) {
        if (!order.episodeId().equals(episode.episodeId())
                || !order.patientUserId().equals(episode.patientUserId())) {
            throw new IllegalArgumentException("examination and episode do not match");
        }
    }

    private void completeAppointment(Connection connection, HospitalAppointment appointment)
            throws SQLException {
        // Clinical completion leaves the already-paid registration bill untouched.
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tblHospitalAppointment SET status = 'COMPLETED', completedAt = ? "
                        + "WHERE appointmentId = ? AND patientUserId = ? AND scheduleId = ? "
                        + "AND episodeId = ? AND status = 'BOOKED'")) {
            statement.setTimestamp(1, Timestamp.valueOf(appointment.completedAt()));
            statement.setString(2, appointment.appointmentId());
            statement.setString(3, appointment.patientUserId());
            statement.setString(4, appointment.scheduleId());
            statement.setString(5, appointment.episodeId());
            requireSingleUpdate(statement.executeUpdate(), "appointment completion");
        }
    }

    private void transitionEpisode(Connection connection, HospitalEpisode episode,
                                   EpisodeStatus expected) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tblHospitalEpisode SET status = ?, completedAt = ? "
                        + "WHERE episodeId = ? AND patientUserId = ? AND departmentId = ? AND status = ?")) {
            statement.setString(1, episode.status().name());
            setNullableTimestamp(statement, 2, episode.completedAt());
            statement.setString(3, episode.episodeId());
            statement.setString(4, episode.patientUserId());
            statement.setString(5, episode.departmentId());
            statement.setString(6, expected.name());
            requireSingleUpdate(statement.executeUpdate(), "clinical episode transition");
        }
    }

    private void transitionOrder(Connection connection, HospitalExaminationOrder order,
                                 ExaminationStatus expected) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tblHospitalExaminationOrder SET status = ?, reviewedAt = ? "
                        + "WHERE orderId = ? AND episodeId = ? AND patientUserId = ? "
                        + "AND orderedAppointmentId = ? AND doctorId = ? AND status = ?")) {
            statement.setString(1, order.status().name());
            setNullableTimestamp(statement, 2, order.reviewedAt());
            statement.setString(3, order.orderId());
            statement.setString(4, order.episodeId());
            statement.setString(5, order.patientUserId());
            statement.setString(6, order.orderedAppointmentId());
            statement.setString(7, order.doctorId());
            statement.setString(8, expected.name());
            requireSingleUpdate(statement.executeUpdate(), "examination order transition");
        }
    }

    @FunctionalInterface
    private interface ClinicalReader<T> {
        T read(ResultSet result) throws SQLException;
    }

    private <T> List<T> queryClinical(String sql, String key, ClinicalReader<T> reader) {
        java.util.Objects.requireNonNull(key, "clinical query key must not be null");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                List<T> records = new ArrayList<>();
                while (result.next()) {
                    records.add(reader.read(result));
                }
                return records;
            }
        } catch (SQLException exception) {
            throw failure("Cannot read hospital clinical records.", exception);
        }
    }

    private HospitalConsultation readConsultation(ResultSet result) throws SQLException {
        ConsultationOutcome outcome = ConsultationOutcome.valueOf(result.getString("outcome"));
        return new HospitalConsultation(
                result.getString("consultationId"), result.getString("appointmentId"),
                result.getString("doctorId"), result.getString("patientUserId"), outcome,
                result.getString("diagnosisOpinion"), result.getString("examinationAdvice"),
                result.getString(outcome == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "interimCareAdvice" : "treatmentAdvice"),
                result.getString("medicationAdvice"), result.getString("followUpAdvice"),
                result.getTimestamp("createdAt").toLocalDateTime());
    }

    private HospitalExaminationOrder readExaminationOrder(ResultSet result) throws SQLException {
        return new HospitalExaminationOrder(
                result.getString("orderId"), result.getString("episodeId"),
                result.getString("orderedAppointmentId"), result.getString("doctorId"),
                result.getString("patientUserId"), result.getString("itemName"),
                result.getString("instructions"),
                ExaminationStatus.valueOf(result.getString("status")),
                result.getTimestamp("orderedAt").toLocalDateTime(),
                nullableDateTime(result, "reviewedAt"));
    }

    private void insertConsultation(Connection connection, HospitalConsultation consultation)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblHospitalConsultation (consultationId, appointmentId, doctorId, "
                        + "patientUserId, outcome, diagnosisOpinion, examinationAdvice, treatmentAdvice, "
                        + "interimCareAdvice, medicationAdvice, followUpAdvice, createdAt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            boolean stage = consultation.outcome() == ConsultationOutcome.WAITING_FOR_RESULTS;
            statement.setString(1, consultation.consultationId());
            statement.setString(2, consultation.appointmentId());
            statement.setString(3, consultation.doctorId());
            statement.setString(4, consultation.patientUserId());
            statement.setString(5, consultation.outcome().name());
            statement.setString(6, consultation.diagnosisOpinion());
            statement.setString(7, nullableText(consultation.examinationAdvice()));
            statement.setString(8, stage ? null : consultation.treatmentAdvice());
            statement.setString(9, stage ? nullableText(consultation.treatmentAdvice()) : null);
            statement.setString(10, nullableText(consultation.medicationAdvice()));
            statement.setString(11, nullableText(consultation.followUpAdvice()));
            statement.setTimestamp(12, Timestamp.valueOf(consultation.createdAt()));
            statement.executeUpdate();
        }
    }

    private void insertExaminationOrder(Connection connection, HospitalExaminationOrder order)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblHospitalExaminationOrder (orderId, episodeId, orderedAppointmentId, "
                        + "doctorId, patientUserId, itemName, instructions, status, orderedAt, reviewedAt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, order.orderId());
            statement.setString(2, order.episodeId());
            statement.setString(3, order.orderedAppointmentId());
            statement.setString(4, order.doctorId());
            statement.setString(5, order.patientUserId());
            statement.setString(6, order.itemName());
            statement.setString(7, nullableText(order.instructions()));
            statement.setString(8, order.status().name());
            statement.setTimestamp(9, Timestamp.valueOf(order.orderedAt()));
            setNullableTimestamp(statement, 10, order.reviewedAt());
            statement.executeUpdate();
        }
    }

    private void initializeClinicalSchema(Connection connection) throws SQLException {
        if (!tableExists(connection, "tblHospitalConsultation")) {
            execute(connection, "CREATE TABLE tblHospitalConsultation ("
                    + "consultationId TEXT(64) PRIMARY KEY, appointmentId TEXT(64) NOT NULL, "
                    + "doctorId TEXT(36) NOT NULL, patientUserId TEXT(36) NOT NULL, "
                    + "outcome TEXT(30) NOT NULL, diagnosisOpinion MEMO NOT NULL, "
                    + "examinationAdvice MEMO, treatmentAdvice MEMO, interimCareAdvice MEMO, "
                    + "medicationAdvice MEMO, followUpAdvice MEMO, createdAt DATETIME NOT NULL)");
        }
        ensureClinicalIndex(connection, "tblHospitalConsultation", "ux_hospitalConsultation_appointment",
                "appointmentId", true);
        ensureClinicalIndex(connection, "tblHospitalConsultation", "ix_hospitalConsultation_patient",
                "patientUserId, createdAt", false);
        if (!tableExists(connection, "tblHospitalExaminationOrder")) {
            execute(connection, "CREATE TABLE tblHospitalExaminationOrder ("
                    + "orderId TEXT(64) PRIMARY KEY, episodeId TEXT(64) NOT NULL, "
                    + "orderedAppointmentId TEXT(64) NOT NULL, doctorId TEXT(36) NOT NULL, "
                    + "patientUserId TEXT(36) NOT NULL, itemName TEXT(100) NOT NULL, "
                    + "instructions MEMO, status TEXT(20) NOT NULL, "
                    + "orderedAt DATETIME NOT NULL, reviewedAt DATETIME)");
        }
        ensureClinicalIndex(connection, "tblHospitalExaminationOrder", "ix_hospitalExamination_patient",
                "patientUserId, orderedAt", false);
        ensureClinicalIndex(connection, "tblHospitalExaminationOrder", "ix_hospitalExamination_doctor",
                "doctorId, status", false);
        // The current workflow permits exactly one examination per source appointment.
        ensureClinicalIndex(connection, "tblHospitalExaminationOrder", "ux_hospitalExamination_appointment",
                "orderedAppointmentId", true);
        ensureClinicalIndex(connection, "tblHospitalExaminationOrder", "ix_hospitalExamination_episode",
                "episodeId", false);
        if (!tableExists(connection, "tblHospitalExaminationReport")) {
            execute(connection, "CREATE TABLE tblHospitalExaminationReport ("
                    + "reportId TEXT(64) PRIMARY KEY, orderId TEXT(64) NOT NULL, "
                    + "summary MEMO NOT NULL, reportedAt DATETIME NOT NULL, sourceType TEXT(20) NOT NULL)");
        }
        ensureClinicalIndex(connection, "tblHospitalExaminationReport", "ux_hospitalReport_order",
                "orderId", true);
    }

    private void ensureClinicalIndex(Connection connection, String table, String name,
                                     String columns, boolean unique) throws SQLException {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (name.equalsIgnoreCase(indexName)
                        || (table + "_" + name).equalsIgnoreCase(indexName)) {
                    return;
                }
            }
        }
        execute(connection, "CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + name
                + " ON " + table + " (" + columns + ")");
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection()) {
            if (!tableExists(connection, DEPARTMENT_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalDepartment ("
                        + "departmentId TEXT(36) PRIMARY KEY, "
                        + "departmentCode TEXT(20) NOT NULL, "
                        + "departmentName TEXT(50) NOT NULL, "
                        + "parentDepartmentId TEXT(36), "
                        + "bookable YESNO NOT NULL, "
                        + "description MEMO, "
                        + "status TEXT(20) NOT NULL, "
                        + "createdAt DATETIME NOT NULL, "
                        + "updatedAt DATETIME NOT NULL)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblHospitalDepartment_code "
                        + "ON tblHospitalDepartment (departmentCode)");
                execute(connection, "CREATE INDEX ix_tblHospitalDepartment_parent "
                        + "ON tblHospitalDepartment (parentDepartmentId)");
                execute(connection, "CREATE INDEX ix_tblHospitalDepartment_status "
                        + "ON tblHospitalDepartment (status)");
            }
            if (!tableExists(connection, PROFILE_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalDoctor ("
                        + "doctorId TEXT(36) PRIMARY KEY, "
                        + "userId TEXT(36), "
                        + "departmentId TEXT(36) NOT NULL, "
                        + "doctorName TEXT(100) NOT NULL, "
                        + "doctorTitle TEXT(50) NOT NULL, "
                        + "active YESNO NOT NULL, "
                        + "createdAt DATETIME NOT NULL, "
                        + "updatedAt DATETIME NOT NULL)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblHospitalDoctor_userId "
                        + "ON tblHospitalDoctor (userId)");
                execute(connection, "CREATE INDEX ix_tblHospitalDoctor_department_active "
                        + "ON tblHospitalDoctor (departmentId, active)");
            } else {
                migrateDoctorColumns(connection);
            }
            if (!tableExists(connection, APPLICATION_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalDoctorApplication ("
                        + "requestId TEXT(40) PRIMARY KEY, "
                        + "applicationType TEXT(20) NOT NULL, "
                        + "username TEXT(50) NOT NULL, "
                        + "displayName TEXT(100) NOT NULL, "
                        + "departmentId TEXT(36) NOT NULL, "
                        + "doctorTitle TEXT(50) NOT NULL, "
                        + "requestedByUserId TEXT(36) NOT NULL, "
                        + "applicationStatus TEXT(20) NOT NULL, "
                        + "targetUserId TEXT(36), "
                        + "reviewedByUserId TEXT(36), "
                        + "createdAt DATETIME NOT NULL)");
                execute(connection, "CREATE INDEX ix_tblHospitalDoctorApplication_status "
                        + "ON tblHospitalDoctorApplication (applicationStatus)");
            }
            migrateDoctorApplicationColumns(connection);
            if (!tableExists(connection, SCHEDULE_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalSchedule ("
                        + "scheduleId TEXT(36) PRIMARY KEY, "
                        + "departmentId TEXT(36) NOT NULL, "
                        + "doctorId TEXT(36) NOT NULL, "
                        + "startTime DATETIME NOT NULL, "
                        + "endTime DATETIME NOT NULL, "
                        + "registrationFeeCents LONG NOT NULL, "
                        + "capacity LONG NOT NULL, "
                        + "status TEXT(20) NOT NULL, "
                        + "createdAt DATETIME NOT NULL, "
                        + "updatedAt DATETIME NOT NULL)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblHospitalSchedule_doctor_start "
                        + "ON tblHospitalSchedule (doctorId, startTime)");
                execute(connection, "CREATE INDEX ix_tblHospitalSchedule_department_start "
                        + "ON tblHospitalSchedule (departmentId, startTime, status)");
            }
            if (!tableExists(connection, PATIENT_PROFILE_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalPatientProfile ("
                        + "patientUserId TEXT(36) PRIMARY KEY, "
                        + "bloodType TEXT(10), "
                        + "allergies MEMO, "
                        + "medicalHistory MEMO, "
                        + "longTermMedication MEMO, "
                        + "emergencyContact TEXT(100), "
                        + "updatedAt DATETIME NOT NULL)");
            }
            if (!tableExists(connection, EPISODE_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalEpisode ("
                        + "episodeId TEXT(64) PRIMARY KEY, "
                        + "patientUserId TEXT(36) NOT NULL, "
                        + "departmentId TEXT(36) NOT NULL, "
                        + "status TEXT(30) NOT NULL, "
                        + "openedAt DATETIME NOT NULL, "
                        + "completedAt DATETIME)");
                execute(connection, "CREATE INDEX ix_tblHospitalEpisode_patient_status "
                        + "ON tblHospitalEpisode (patientUserId, status)");
            }
            if (!tableExists(connection, APPOINTMENT_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalAppointment ("
                        + "appointmentId TEXT(64) PRIMARY KEY, "
                        + "scheduleId TEXT(36) NOT NULL, "
                        + "patientUserId TEXT(36) NOT NULL, "
                        + "queueNumber LONG NOT NULL, "
                        + "visitType TEXT(20) NOT NULL, "
                        + "episodeId TEXT(64) NOT NULL, "
                        + "sourceFirstVisitAppointmentId TEXT(64), "
                        + "status TEXT(20) NOT NULL, "
                        + "createdAt DATETIME NOT NULL, "
                        + "cancelledAt DATETIME, "
                        + "completedAt DATETIME)");
                execute(connection, "CREATE INDEX ix_tblHospitalAppointment_patient_status "
                        + "ON tblHospitalAppointment (patientUserId, status)");
                execute(connection, "CREATE INDEX ix_tblHospitalAppointment_schedule_status "
                        + "ON tblHospitalAppointment (scheduleId, status)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblHospitalAppointment_queue "
                        + "ON tblHospitalAppointment (scheduleId, queueNumber)");
                execute(connection, "CREATE INDEX ix_tblHospitalAppointment_source "
                        + "ON tblHospitalAppointment (sourceFirstVisitAppointmentId)");
            }
            if (!tableExists(connection, BILL_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalBill ("
                        + "billId TEXT(64) PRIMARY KEY, "
                        + "appointmentId TEXT(64) NOT NULL, "
                        + "patientUserId TEXT(36) NOT NULL, "
                        + "billType TEXT(20) NOT NULL, "
                        + "paymentStatus TEXT(20) NOT NULL, "
                        + "createdAt DATETIME NOT NULL, "
                        + "paidAt DATETIME, "
                        + "refundedAt DATETIME)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblHospitalBill_appointment_type "
                        + "ON tblHospitalBill (appointmentId, billType)");
                execute(connection, "CREATE INDEX ix_tblHospitalBill_patient_status "
                        + "ON tblHospitalBill (patientUserId, paymentStatus)");
            }
            if (!tableExists(connection, BILL_ITEM_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalBillItem ("
                        + "billItemId TEXT(64) PRIMARY KEY, "
                        + "billId TEXT(64) NOT NULL, "
                        + "itemCode TEXT(30), "
                        + "itemName TEXT(100) NOT NULL, "
                        + "quantity LONG NOT NULL, "
                        + "unitPriceCents LONG NOT NULL)");
                execute(connection, "CREATE INDEX ix_tblHospitalBillItem_bill "
                        + "ON tblHospitalBillItem (billId)");
            }
            initializeClinicalSchema(connection);
        } catch (SQLException exception) {
            throw failure("Cannot initialize hospital schema.", exception);
        }
    }

    private void seedReferenceData() {
        InMemoryHospitalRepository seed = new InMemoryHospitalRepository(clock);
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                seedDepartments(connection, seed);
                seedDoctors(connection, seed);
                seedSchedules(connection, seed);
                seedPatientProfiles(connection, seed);
                seedBookingData(connection, seed);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot seed hospital reference data.", exception);
        }
    }

    private void seedDepartments(Connection connection, InMemoryHospitalRepository seed)
            throws SQLException {
        String sql = "INSERT INTO tblHospitalDepartment "
                + "(departmentId, departmentCode, departmentName, parentDepartmentId, "
                + "bookable, description, status, createdAt, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now(clock);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (HospitalDepartment department : seed.findActiveDepartments()) {
                if (exists(connection,
                        "SELECT departmentId FROM tblHospitalDepartment "
                                + "WHERE departmentId = ?",
                        department.departmentId())) {
                    continue;
                }
                statement.setString(1, department.departmentId());
                statement.setString(2, departmentCode(department.departmentId()));
                statement.setString(3, department.departmentName());
                statement.setString(4, department.parentDepartmentId());
                statement.setBoolean(5, department.bookable());
                statement.setString(6, null);
                statement.setString(7, "ACTIVE");
                statement.setTimestamp(8, Timestamp.valueOf(now));
                statement.setTimestamp(9, Timestamp.valueOf(now));
                statement.executeUpdate();
            }
        }
    }

    private void seedDoctors(Connection connection, InMemoryHospitalRepository seed)
            throws SQLException {
        boolean legacyUserKey = columnExists(connection, PROFILE_TABLE, "doctorUserId");
        boolean legacyNumberColumn = columnExists(
                connection, PROFILE_TABLE, "doctorNumber");
        String sql = "INSERT INTO tblHospitalDoctor "
                + (legacyUserKey
                ? "(doctorUserId, "
                : "(doctorId, userId, departmentId, doctorName, ")
                + (legacyUserKey && legacyNumberColumn ? "doctorNumber, " : "")
                + (legacyUserKey ? "doctorId, userId, departmentId, doctorName, " : "")
                + "doctorTitle, [active], createdAt, updatedAt) "
                + (legacyUserKey
                ? "VALUES (?, "
                        + (legacyNumberColumn ? "?, " : "")
                        + "?, ?, ?, ?, ?, ?, ?, ?)"
                : "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        LocalDateTime now = LocalDateTime.now(clock);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (HospitalDoctor doctor : seed.findAllDoctors()) {
                if (exists(connection,
                        "SELECT doctorId FROM tblHospitalDoctor WHERE doctorId = ?",
                        doctor.doctorId())) {
                    continue;
                }
                int index = 1;
                if (legacyUserKey) {
                    statement.setString(index++, doctor.userId() == null
                            ? legacyValue("UNBOUND_", doctor.doctorId())
                            : doctor.userId());
                    if (legacyNumberColumn) {
                        statement.setString(index++,
                                legacyValue("LEGACY_", doctor.doctorId()));
                    }
                }
                statement.setString(index++, doctor.doctorId());
                statement.setString(index++, doctor.userId());
                statement.setString(index++, doctor.departmentId());
                statement.setString(index++, doctor.doctorName());
                statement.setString(index++, doctor.doctorTitle());
                statement.setBoolean(index++, doctor.active());
                statement.setTimestamp(index++, Timestamp.valueOf(now));
                statement.setTimestamp(index, Timestamp.valueOf(now));
                statement.executeUpdate();
            }
        }
    }

    private void seedSchedules(Connection connection, InMemoryHospitalRepository seed)
            throws SQLException {
        String sql = "INSERT INTO tblHospitalSchedule "
                + "(scheduleId, departmentId, doctorId, startTime, endTime, "
                + "registrationFeeCents, capacity, status, createdAt, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now(clock);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (HospitalSlot slot : seed.findAllSlots()) {
                if (exists(connection,
                        "SELECT scheduleId FROM tblHospitalSchedule WHERE scheduleId = ?",
                        slot.scheduleId())) {
                    continue;
                }
                statement.setString(1, slot.scheduleId());
                statement.setString(2, slot.departmentId());
                statement.setString(3, slot.doctorId());
                statement.setTimestamp(4, Timestamp.valueOf(slot.startTime()));
                statement.setTimestamp(5, Timestamp.valueOf(slot.endTime()));
                statement.setLong(6, slot.priceCents());
                statement.setInt(7, slot.capacity());
                statement.setString(8, slot.published() ? "PUBLISHED" : "CLOSED");
                statement.setTimestamp(9, Timestamp.valueOf(now));
                statement.setTimestamp(10, Timestamp.valueOf(now));
                statement.executeUpdate();
            }
        }
    }

    private void seedPatientProfiles(Connection connection, InMemoryHospitalRepository seed)
            throws SQLException {
        for (HospitalPatientProfile profile : seed.findAllPatientProfiles()) {
            if (!patientProfileExists(connection, profile.patientUserId())) {
                insertPatientProfile(connection, profile);
            }
        }
    }

    private void seedBookingData(Connection connection, InMemoryHospitalRepository seed)
            throws SQLException {
        for (HospitalEpisode episode : seed.findAllEpisodes()) {
            if (!exists(connection,
                    "SELECT episodeId FROM tblHospitalEpisode WHERE episodeId = ?",
                    episode.episodeId())) {
                insertEpisode(connection, episode);
            }
        }
        for (HospitalBooking booking : seed.findAllBookings()) {
            if (!exists(connection,
                    "SELECT appointmentId FROM tblHospitalAppointment "
                            + "WHERE appointmentId = ?",
                    booking.appointment().appointmentId())) {
                insertBooking(connection, booking);
            }
        }
    }

    private void migrateDoctorColumns(Connection connection) throws SQLException {
        if (!columnExists(connection, PROFILE_TABLE, "doctorId")) {
            execute(connection,
                    "ALTER TABLE tblHospitalDoctor ADD COLUMN doctorId TEXT(36)");
        }
        if (!columnExists(connection, PROFILE_TABLE, "userId")) {
            execute(connection,
                    "ALTER TABLE tblHospitalDoctor ADD COLUMN userId TEXT(36)");
        }
        if (!columnExists(connection, PROFILE_TABLE, "doctorName")) {
            execute(connection,
                    "ALTER TABLE tblHospitalDoctor ADD COLUMN doctorName TEXT(100)");
        }
        if (!columnExists(connection, PROFILE_TABLE, "createdAt")) {
            execute(connection,
                    "ALTER TABLE tblHospitalDoctor ADD COLUMN createdAt DATETIME");
        }
        if (!columnExists(connection, PROFILE_TABLE, "updatedAt")) {
            execute(connection,
                    "ALTER TABLE tblHospitalDoctor ADD COLUMN updatedAt DATETIME");
        }
        if (columnExists(connection, PROFILE_TABLE, "doctorUserId")) {
            execute(connection, "UPDATE tblHospitalDoctor SET "
                    + "doctorId = doctorUserId WHERE doctorId IS NULL");
            execute(connection, "UPDATE tblHospitalDoctor SET "
                    + "userId = doctorUserId WHERE userId IS NULL");
            execute(connection, "UPDATE tblHospitalDoctor SET "
                    + "doctorName = doctorUserId WHERE doctorName IS NULL");
            execute(connection, "UPDATE tblHospitalDoctor SET "
                    + "doctorId = 'doctor-chen', doctorName = '陈医生' "
                    + "WHERE doctorUserId = 'U-TEACHER-001'");
            execute(connection, "UPDATE tblHospitalDoctor SET "
                    + "doctorId = 'doctor-liu', doctorName = '刘医生' "
                    + "WHERE doctorUserId = 'U-TEACHER-002'");
        }
        execute(connection, "UPDATE tblHospitalDoctor SET createdAt = NOW() "
                + "WHERE createdAt IS NULL");
        execute(connection, "UPDATE tblHospitalDoctor SET updatedAt = NOW() "
                + "WHERE updatedAt IS NULL");
    }

    private void migrateDoctorApplicationColumns(Connection connection) throws SQLException {
        if (!columnExists(connection, APPLICATION_TABLE, "applicationType")) {
            execute(connection,
                    "ALTER TABLE tblHospitalDoctorApplication ADD COLUMN applicationType TEXT(20)");
        }
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT requestId, applicationType "
                             + "FROM tblHospitalDoctorApplication");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE tblHospitalDoctorApplication SET applicationType = ? "
                             + "WHERE requestId = ?")) {
            boolean hasUpdates = false;
            while (result.next()) {
                String type = result.getString("applicationType");
                if (type == null || type.isBlank()) {
                    String requestId = result.getString("requestId");
                    update.setString(1, type == null || type.isBlank()
                            ? DoctorApplicationType.EXISTING_ACCOUNT.name() : type);
                    update.setString(2, requestId);
                    update.addBatch();
                    hasUpdates = true;
                }
            }
            if (hasUpdates) {
                update.executeBatch();
            }
        }
    }

    private String legacyValue(String prefix, String source) {
        String normalized = source == null ? "UNKNOWN"
                : source.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
        String value = prefix + normalized;
        return value.length() <= 32 ? value : value.substring(value.length() - 32);
    }

    private String departmentCode(String departmentId) {
        String code = departmentId.toUpperCase(Locale.ROOT)
                .replaceFirst("^DEPT-", "")
                .replace('-', '_');
        return code.length() <= 20 ? code : code.substring(0, 20);
    }

    private String nullableText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private DoctorApplication readApplication(ResultSet result) throws SQLException {
        DoctorApplicationType applicationType = DoctorApplicationType.valueOf(
                result.getString("applicationType").toUpperCase(Locale.ROOT));
        String storedUsername = result.getString("username");
        String username = applicationType == DoctorApplicationType.EXTERNAL_DOCTOR
                        && storedUsername != null && storedUsername.startsWith("__AUTO__")
                ? null : storedUsername;
        return new DoctorApplication(
                result.getString("requestId"),
                applicationType,
                username,
                result.getString("displayName"),
                result.getString("departmentId"),
                result.getString("doctorTitle"),
                result.getString("requestedByUserId"),
                DoctorApplicationStatus.valueOf(
                        result.getString("applicationStatus").toUpperCase(Locale.ROOT)),
                result.getString("targetUserId"),
                result.getString("reviewedByUserId"),
                result.getTimestamp("createdAt").toLocalDateTime());
    }

    private void insertApplication(Connection connection, DoctorApplication application)
            throws SQLException {
        boolean legacyNumberColumn = columnExists(
                connection, APPLICATION_TABLE, "doctorNumber");
        String sql = "INSERT INTO tblHospitalDoctorApplication "
                + (legacyNumberColumn
                ? "(requestId, applicationType, doctorNumber, username, "
                : "(requestId, applicationType, username, ")
                + "displayName, departmentId, doctorTitle, "
                + "requestedByUserId, applicationStatus, targetUserId, reviewedByUserId, createdAt) "
                + (legacyNumberColumn
                ? "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, application.requestId());
            statement.setString(index++, application.applicationType().name());
            if (legacyNumberColumn) {
                statement.setString(index++, legacyValue("APP_", application.requestId()));
            }
            bindApplication(statement, application, index);
            statement.executeUpdate();
        }
    }

    private void updateApplication(Connection connection, DoctorApplication application)
            throws SQLException {
        String sql = "UPDATE tblHospitalDoctorApplication SET applicationType = ?, "
                + "username = ?, displayName = ?, departmentId = ?, "
                + "doctorTitle = ?, requestedByUserId = ?, applicationStatus = ?, "
                + "targetUserId = ?, reviewedByUserId = ?, createdAt = ? "
                + "WHERE requestId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, application.applicationType().name());
            bindApplication(statement, application, 2);
            statement.setString(11, application.requestId());
            statement.executeUpdate();
        }
    }

    private void bindApplication(
            PreparedStatement statement,
            DoctorApplication application,
            int startIndex) throws SQLException {
        int index = startIndex;
        statement.setString(index++, storedUsername(application));
        statement.setString(index++, application.displayName());
        statement.setString(index++, application.departmentId());
        statement.setString(index++, application.doctorTitle());
        statement.setString(index++, application.requestedByUserId());
        statement.setString(index++, application.status().name());
        statement.setString(index++, application.targetUserId());
        statement.setString(index++, application.reviewedByUserId());
        statement.setTimestamp(index, Timestamp.valueOf(application.createdAt()));
    }

    private String storedUsername(DoctorApplication application) {
        return application.username() == null
                ? "__AUTO__" + application.requestId()
                : application.username();
    }

    private boolean applicationExists(Connection connection, String requestId)
            throws SQLException {
        return exists(connection,
                "SELECT requestId FROM tblHospitalDoctorApplication WHERE requestId = ?",
                requestId);
    }

    private boolean profileExists(Connection connection, String userId) throws SQLException {
        return exists(connection,
                "SELECT doctorId FROM tblHospitalDoctor WHERE userId = ?", userId);
    }

    private boolean patientProfileExists(Connection connection, String patientUserId)
            throws SQLException {
        return exists(connection,
                "SELECT patientUserId FROM tblHospitalPatientProfile "
                        + "WHERE patientUserId = ?",
                patientUserId);
    }

    private void savePatientProfile(
            Connection connection,
            HospitalPatientProfile profile) throws SQLException {
        if (patientProfileExists(connection, profile.patientUserId())) {
            updatePatientProfile(connection, profile);
        } else {
            insertPatientProfile(connection, profile);
        }
    }

    private void insertPatientProfile(
            Connection connection,
            HospitalPatientProfile profile) throws SQLException {
        String sql = "INSERT INTO tblHospitalPatientProfile "
                + "(patientUserId, bloodType, allergies, medicalHistory, "
                + "longTermMedication, emergencyContact, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPatientProfile(statement, profile, true);
            statement.executeUpdate();
        }
    }

    private void updatePatientProfile(
            Connection connection,
            HospitalPatientProfile profile) throws SQLException {
        String sql = "UPDATE tblHospitalPatientProfile SET bloodType = ?, allergies = ?, "
                + "medicalHistory = ?, longTermMedication = ?, emergencyContact = ?, "
                + "updatedAt = ? WHERE patientUserId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPatientProfile(statement, profile, false);
            statement.executeUpdate();
        }
    }

    private void bindPatientProfile(
            PreparedStatement statement,
            HospitalPatientProfile profile,
            boolean insert) throws SQLException {
        int index = 1;
        if (insert) {
            statement.setString(index++, profile.patientUserId());
        }
        statement.setString(index++, nullableText(profile.bloodType()));
        statement.setString(index++, nullableText(profile.allergies()));
        statement.setString(index++, nullableText(profile.medicalHistory()));
        statement.setString(index++, nullableText(profile.longTermMedication()));
        statement.setString(index++, nullableText(profile.emergencyContact()));
        statement.setTimestamp(index++, Timestamp.valueOf(profile.updatedAt()));
        if (!insert) {
            statement.setString(index, profile.patientUserId());
        }
    }

    private HospitalPatientProfile readPatientProfile(ResultSet result) throws SQLException {
        return new HospitalPatientProfile(
                result.getString("patientUserId"),
                result.getString("bloodType"),
                result.getString("allergies"),
                result.getString("medicalHistory"),
                result.getString("longTermMedication"),
                result.getString("emergencyContact"),
                result.getTimestamp("updatedAt").toLocalDateTime());
    }

    private String appointmentSelect() {
        return "SELECT appointmentId, patientUserId, scheduleId, queueNumber, "
                + "createdAt, cancelledAt, completedAt, status, visitType, episodeId, "
                + "sourceFirstVisitAppointmentId AS sourceAppointmentId "
                + "FROM tblHospitalAppointment";
    }

    private List<HospitalAppointment> readAppointments(PreparedStatement statement)
            throws SQLException {
        try (ResultSet result = statement.executeQuery()) {
            List<HospitalAppointment> appointments = new ArrayList<>();
            while (result.next()) {
                appointments.add(readAppointment(result, ""));
            }
            return appointments;
        }
    }

    private HospitalAppointment readAppointment(ResultSet result, String prefix)
            throws SQLException {
        return new HospitalAppointment(
                result.getString(prefix + "appointmentId"),
                result.getString(prefix + "patientUserId"),
                result.getString(prefix + "scheduleId"),
                result.getInt(prefix + "queueNumber"),
                result.getTimestamp(prefix + "createdAt").toLocalDateTime(),
                nullableDateTime(result, prefix + "cancelledAt"),
                nullableDateTime(result, prefix + "completedAt"),
                AppointmentStatus.valueOf(result.getString(prefix + "status")
                        .toUpperCase(Locale.ROOT)),
                VisitType.valueOf(result.getString(prefix + "visitType")
                        .toUpperCase(Locale.ROOT)),
                result.getString(prefix + "episodeId"),
                nullableText(result.getString(prefix + "sourceAppointmentId")));
    }

    private String episodeSelect() {
        return "SELECT episodeId, patientUserId, departmentId, status, openedAt, "
                + "completedAt FROM tblHospitalEpisode";
    }

    private HospitalEpisode readEpisode(ResultSet result) throws SQLException {
        return new HospitalEpisode(
                result.getString("episodeId"),
                result.getString("patientUserId"),
                result.getString("departmentId"),
                EpisodeStatus.valueOf(result.getString("status")
                        .toUpperCase(Locale.ROOT)),
                result.getTimestamp("openedAt").toLocalDateTime(),
                nullableDateTime(result, "completedAt"));
    }

    private String bookingSelect() {
        return "SELECT a.appointmentId AS appointmentId, "
                + "a.patientUserId AS patientUserId, a.scheduleId AS scheduleId, "
                + "a.queueNumber AS queueNumber, a.createdAt AS createdAt, "
                + "a.cancelledAt AS cancelledAt, a.completedAt AS completedAt, "
                + "a.status AS status, a.visitType AS visitType, "
                + "a.episodeId AS episodeId, "
                + "a.sourceFirstVisitAppointmentId AS sourceAppointmentId, "
                + "b.billId AS billId, b.appointmentId AS billAppointmentId, "
                + "b.createdAt AS billCreatedAt, b.paidAt AS billPaidAt, "
                + "b.refundedAt AS billRefundedAt, b.paymentStatus AS paymentStatus, "
                + "i.billItemId AS billItemId, i.billId AS itemBillId, "
                + "i.itemName AS itemName, i.quantity AS itemQuantity, "
                + "i.unitPriceCents AS unitPriceCents "
                + "FROM (tblHospitalAppointment AS a INNER JOIN tblHospitalBill AS b "
                + "ON a.appointmentId = b.appointmentId) "
                + "INNER JOIN tblHospitalBillItem AS i ON b.billId = i.billId";
    }

    private String patientBillSelect() {
        return "SELECT b.billId, b.appointmentId, b.patientUserId, b.billType, "
                + "b.paymentStatus, b.createdAt, b.paidAt, b.refundedAt, "
                + "i.billItemId, i.billId AS itemBillId, i.itemName, "
                + "i.quantity, i.unitPriceCents "
                + "FROM tblHospitalBill AS b INNER JOIN tblHospitalBillItem AS i "
                + "ON b.billId = i.billId";
    }

    private List<HospitalPatientBill> readPatientBills(PreparedStatement statement)
            throws SQLException {
        try (ResultSet result = statement.executeQuery()) {
            List<HospitalPatientBill> bills = new ArrayList<>();
            while (result.next()) {
                HospitalBillItem item = new HospitalBillItem(
                        result.getString("billItemId"),
                        result.getString("itemBillId"),
                        result.getString("itemName"),
                        result.getInt("quantity"),
                        result.getLong("unitPriceCents"));
                bills.add(new HospitalPatientBill(
                        result.getString("billId"),
                        result.getString("appointmentId"),
                        result.getString("patientUserId"),
                        HospitalBillType.valueOf(result.getString("billType")
                                .toUpperCase(Locale.ROOT)),
                        PaymentStatus.valueOf(result.getString("paymentStatus")
                                .toUpperCase(Locale.ROOT)),
                        result.getTimestamp("createdAt").toLocalDateTime(),
                        nullableDateTime(result, "paidAt"),
                        nullableDateTime(result, "refundedAt"),
                        item));
            }
            return bills;
        }
    }

    private List<HospitalBooking> readBookings(PreparedStatement statement)
            throws SQLException {
        try (ResultSet result = statement.executeQuery()) {
            List<HospitalBooking> bookings = new ArrayList<>();
            while (result.next()) {
                HospitalAppointment appointment = readAppointment(result, "");
                HospitalBill bill = new HospitalBill(
                        result.getString("billId"),
                        result.getString("billAppointmentId"),
                        result.getTimestamp("billCreatedAt").toLocalDateTime(),
                        nullableDateTime(result, "billPaidAt"),
                        nullableDateTime(result, "billRefundedAt"),
                        PaymentStatus.valueOf(result.getString("paymentStatus")
                                .toUpperCase(Locale.ROOT)));
                HospitalBillItem item = new HospitalBillItem(
                        result.getString("billItemId"),
                        result.getString("itemBillId"),
                        result.getString("itemName"),
                        result.getInt("itemQuantity"),
                        result.getLong("unitPriceCents"));
                bookings.add(new HospitalBooking(appointment, bill, item));
            }
            return bookings;
        }
    }

    private void insertEpisode(Connection connection, HospitalEpisode episode)
            throws SQLException {
        String sql = "INSERT INTO tblHospitalEpisode "
                + "(episodeId, patientUserId, departmentId, status, openedAt, completedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, episode.episodeId());
            statement.setString(2, episode.patientUserId());
            statement.setString(3, episode.departmentId());
            statement.setString(4, episode.status().name());
            statement.setTimestamp(5, Timestamp.valueOf(episode.openedAt()));
            setNullableTimestamp(statement, 6, episode.completedAt());
            statement.executeUpdate();
        }
    }

    private void updateEpisode(Connection connection, HospitalEpisode episode)
            throws SQLException {
        String sql = "UPDATE tblHospitalEpisode SET patientUserId = ?, departmentId = ?, "
                + "status = ?, openedAt = ?, completedAt = ? WHERE episodeId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, episode.patientUserId());
            statement.setString(2, episode.departmentId());
            statement.setString(3, episode.status().name());
            statement.setTimestamp(4, Timestamp.valueOf(episode.openedAt()));
            setNullableTimestamp(statement, 5, episode.completedAt());
            statement.setString(6, episode.episodeId());
            requireSingleUpdate(statement.executeUpdate(), "clinical episode");
        }
    }

    private void insertBooking(Connection connection, HospitalBooking booking)
            throws SQLException {
        insertAppointment(connection, booking.appointment());
        insertBill(connection, booking);
        insertBillItem(connection, booking.billItem());
    }

    private void insertAppointment(
            Connection connection,
            HospitalAppointment appointment) throws SQLException {
        String sql = "INSERT INTO tblHospitalAppointment "
                + "(appointmentId, scheduleId, patientUserId, queueNumber, visitType, "
                + "episodeId, sourceFirstVisitAppointmentId, status, createdAt, "
                + "cancelledAt, completedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAppointment(statement, appointment, false);
            statement.executeUpdate();
        }
    }

    private void updateBooking(Connection connection, HospitalBooking booking)
            throws SQLException {
        String appointmentSql = "UPDATE tblHospitalAppointment SET scheduleId = ?, "
                + "patientUserId = ?, queueNumber = ?, visitType = ?, episodeId = ?, "
                + "sourceFirstVisitAppointmentId = ?, status = ?, createdAt = ?, "
                + "cancelledAt = ?, completedAt = ? WHERE appointmentId = ?";
        try (PreparedStatement statement = connection.prepareStatement(appointmentSql)) {
            bindAppointment(statement, booking.appointment(), true);
            requireSingleUpdate(statement.executeUpdate(), "appointment");
        }
        String billSql = "UPDATE tblHospitalBill SET appointmentId = ?, patientUserId = ?, "
                + "billType = ?, paymentStatus = ?, createdAt = ?, paidAt = ?, "
                + "refundedAt = ? WHERE billId = ?";
        try (PreparedStatement statement = connection.prepareStatement(billSql)) {
            HospitalBill bill = booking.bill();
            statement.setString(1, bill.appointmentId());
            statement.setString(2, booking.appointment().patientUserId());
            statement.setString(3, "REGISTRATION");
            statement.setString(4, bill.paymentStatus().name());
            statement.setTimestamp(5, Timestamp.valueOf(bill.createdAt()));
            setNullableTimestamp(statement, 6, bill.paidAt());
            setNullableTimestamp(statement, 7, bill.refundedAt());
            statement.setString(8, bill.billId());
            requireSingleUpdate(statement.executeUpdate(), "registration bill");
        }
        String itemSql = "UPDATE tblHospitalBillItem SET billId = ?, itemCode = ?, "
                + "itemName = ?, quantity = ?, unitPriceCents = ? WHERE billItemId = ?";
        try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
            HospitalBillItem item = booking.billItem();
            statement.setString(1, item.billId());
            statement.setString(2, null);
            statement.setString(3, item.itemName());
            statement.setInt(4, item.quantity());
            statement.setLong(5, item.unitPriceCents());
            statement.setString(6, item.billItemId());
            requireSingleUpdate(statement.executeUpdate(), "registration bill item");
        }
    }

    private void bindAppointment(
            PreparedStatement statement,
            HospitalAppointment appointment,
            boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, appointment.appointmentId());
        }
        statement.setString(index++, appointment.scheduleId());
        statement.setString(index++, appointment.patientUserId());
        statement.setInt(index++, appointment.queueNumber());
        statement.setString(index++, appointment.visitType().name());
        statement.setString(index++, appointment.episodeId());
        statement.setString(index++, appointment.sourceFirstVisitAppointmentId());
        statement.setString(index++, appointment.status().name());
        statement.setTimestamp(index++, Timestamp.valueOf(appointment.createdAt()));
        setNullableTimestamp(statement, index++, appointment.cancelledAt());
        setNullableTimestamp(statement, index++, appointment.completedAt());
        if (update) {
            statement.setString(index, appointment.appointmentId());
        }
    }

    private void bindSlot(
            PreparedStatement statement,
            HospitalSlot slot,
            LocalDateTime now,
            boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, slot.scheduleId());
        }
        statement.setString(index++, slot.departmentId());
        statement.setString(index++, slot.doctorId());
        statement.setTimestamp(index++, Timestamp.valueOf(slot.startTime()));
        statement.setTimestamp(index++, Timestamp.valueOf(slot.endTime()));
        statement.setInt(index++, slot.priceCents());
        statement.setInt(index++, slot.capacity());
        statement.setString(index++, slot.published() ? "PUBLISHED" : "CLOSED");
        if (!update) {
            statement.setTimestamp(index++, Timestamp.valueOf(now));
        }
        statement.setTimestamp(index++, Timestamp.valueOf(now));
        if (update) {
            statement.setString(index, slot.scheduleId());
        }
    }

    private void insertBill(Connection connection, HospitalBooking booking)
            throws SQLException {
        HospitalBill bill = booking.bill();
        String sql = "INSERT INTO tblHospitalBill "
                + "(billId, appointmentId, patientUserId, billType, paymentStatus, "
                + "createdAt, paidAt, refundedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bill.billId());
            statement.setString(2, bill.appointmentId());
            statement.setString(3, booking.appointment().patientUserId());
            statement.setString(4, "REGISTRATION");
            statement.setString(5, bill.paymentStatus().name());
            statement.setTimestamp(6, Timestamp.valueOf(bill.createdAt()));
            setNullableTimestamp(statement, 7, bill.paidAt());
            setNullableTimestamp(statement, 8, bill.refundedAt());
            statement.executeUpdate();
        }
    }

    private void insertBillItem(Connection connection, HospitalBillItem item)
            throws SQLException {
        String sql = "INSERT INTO tblHospitalBillItem "
                + "(billItemId, billId, itemCode, itemName, quantity, unitPriceCents) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.billItemId());
            statement.setString(2, item.billId());
            statement.setString(3, null);
            statement.setString(4, item.itemName());
            statement.setInt(5, item.quantity());
            statement.setLong(6, item.unitPriceCents());
            statement.executeUpdate();
        }
    }

    private void insertPatientBill(
            Connection connection,
            HospitalPatientBill bill) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblHospitalBill (billId, appointmentId, patientUserId, "
                        + "billType, paymentStatus, createdAt, paidAt, refundedAt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, bill.billId());
            statement.setString(2, bill.appointmentId());
            statement.setString(3, bill.patientUserId());
            statement.setString(4, bill.billType().name());
            statement.setString(5, bill.paymentStatus().name());
            statement.setTimestamp(6, Timestamp.valueOf(bill.createdAt()));
            setNullableTimestamp(statement, 7, bill.paidAt());
            setNullableTimestamp(statement, 8, bill.refundedAt());
            statement.executeUpdate();
        }
        insertBillItem(connection, bill.item());
    }

    private static void validateClinicalBill(
            HospitalPatientBill bill,
            HospitalBooking booking,
            HospitalBillType expectedType) {
        if (bill == null
                || bill.billType() != expectedType
                || bill.paymentStatus() != PaymentStatus.UNPAID
                || !bill.appointmentId().equals(booking.appointment().appointmentId())
                || !bill.patientUserId().equals(booking.appointment().patientUserId())) {
            throw new IllegalArgumentException("clinical bill does not match appointment");
        }
    }

    private void validateBookingEpisode(
            HospitalBooking booking,
            HospitalEpisode episode) {
        if (!booking.appointment().episodeId().equals(episode.episodeId())
                || !booking.appointment().patientUserId().equals(episode.patientUserId())) {
            throw new IllegalArgumentException("booking and clinical episode do not match");
        }
    }

    private void setNullableTimestamp(
            PreparedStatement statement,
            int index,
            LocalDateTime value) throws SQLException {
        statement.setTimestamp(index, value == null ? null : Timestamp.valueOf(value));
    }

    private LocalDateTime nullableDateTime(ResultSet result, String column)
            throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private void requireSingleUpdate(int updatedRows, String entity) {
        if (updatedRows != 1) {
            throw new IllegalStateException(entity + " does not exist or is duplicated");
        }
    }

    private String slotSelect() {
        return "SELECT s.scheduleId, s.departmentId, d.departmentName, "
                + "s.doctorId, h.userId AS doctorUserId, h.doctorName, h.doctorTitle, s.startTime, s.endTime, "
                + "s.registrationFeeCents, s.capacity, s.status "
                + "FROM (tblHospitalSchedule AS s INNER JOIN tblHospitalDepartment AS d "
                + "ON s.departmentId = d.departmentId) "
                + "INNER JOIN tblHospitalDoctor AS h ON s.doctorId = h.doctorId";
    }

    private List<HospitalSlot> readSlots(PreparedStatement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery()) {
            List<HospitalSlot> slots = new ArrayList<>();
            while (result.next()) {
                slots.add(new HospitalSlot(
                        result.getString("scheduleId"),
                        result.getString("departmentId"),
                        result.getString("departmentName"),
                        result.getString("doctorId"),
                        currentName(nullableText(result.getString("doctorUserId")), result.getString("doctorName")),
                        result.getString("doctorTitle"),
                        result.getTimestamp("startTime").toLocalDateTime(),
                        result.getTimestamp("endTime").toLocalDateTime(),
                        result.getInt("registrationFeeCents"),
                        result.getInt("capacity"),
                        0,
                        "PUBLISHED".equalsIgnoreCase(result.getString("status"))));
            }
            return slots;
        }
    }

    private String currentName(String userId, String snapshot) {
        if (users == null || userId == null) {
            return snapshot;
        }
        return users.findByUserId(userId)
                .map(identity -> identity.displayName())
                .orElse(snapshot);
    }

    private boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean tableExists(Connection connection, String expected) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean columnExists(
            Connection connection,
            String tableName,
            String columnName) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(
                null, null, tableName, "%")) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message + " Database: " + database.path(), cause);
    }
}
