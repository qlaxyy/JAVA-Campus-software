package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.ApplyStatusChangeRequest;
import edu.seu.vcampus.common.student.StatusChangeDto;
import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;
import edu.seu.vcampus.server.demo.FinalDemoRoster;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.security.UserDirectory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Access-backed student profiles used by the single demonstration server. */
public final class AccessStudentRepository implements StudentRepository {

    private static final String PROFILE_TABLE = "tblStudentProfile";
    private static final String CHANGE_TABLE = "tblStudentStatusChange";
    private static final DateTimeFormatter CHANGE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AccessDatabase database;
    private final UserDirectory users;

    public AccessStudentRepository(AccessDatabase database, UserDirectory users) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.users = Objects.requireNonNull(users, "users must not be null");
        initializeSchema();
        seedIfEmpty();
    }

    @Override
    public Optional<StudentProfileDto> findByStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM tblStudentProfile "
                + "WHERE studentNumber = ? OR userId = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String normalized = studentId.trim();
            statement.setString(1, normalized);
            statement.setString(2, normalized);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readProfile(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Cannot read student profile.", exception);
        }
    }

    @Override
    public synchronized boolean updateProfile(StudentUpdateProfileRequest request) {
        if (request == null || request.getStudentId() == null) {
            return false;
        }
        String sql = "UPDATE tblStudentProfile SET politicalStatus = ?, phone = ?, "
                + "email = ?, homeAddress = ?, emergencyContact = ?, emergencyPhone = ? "
                + "WHERE studentNumber = ? OR userId = ?";
        StudentProfileDto current = findByStudentId(request.getStudentId()).orElse(null);
        if (current == null) {
            return false;
        }
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, valueOrCurrent(request.getPoliticalStatus(), current.getPoliticalStatus()));
            statement.setString(2, valueOrCurrent(request.getPhone(), current.getPhone()));
            statement.setString(3, valueOrCurrent(request.getEmail(), current.getEmail()));
            statement.setString(4, valueOrCurrent(request.getHomeAddress(), current.getHomeAddress()));
            statement.setString(5, valueOrCurrent(request.getEmergencyContact(), current.getEmergencyContact()));
            statement.setString(6, valueOrCurrent(request.getEmergencyPhone(), current.getEmergencyPhone()));
            String key = request.getStudentId().trim();
            statement.setString(7, key);
            statement.setString(8, key);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw failure("Cannot update student profile.", exception);
        }
    }

    @Override
    public synchronized StatusChangeDto createStatusChange(ApplyStatusChangeRequest request) {
        if (request == null) {
            return null;
        }
        StudentProfileDto profile = findByStudentId(request.getStudentId()).orElse(null);
        if (profile == null) {
            return null;
        }
        String createdAt = LocalDateTime.now().format(CHANGE_TIME);
        String sql = "INSERT INTO tblStudentStatusChange "
                + "(studentNumber, studentNameSnapshot, changeType, reason, changeDate, "
                + "auditStatus, operatorNameSnapshot) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, profile.getStudentId());
            statement.setString(2, profile.getName());
            statement.setString(3, request.getChangeType());
            statement.setString(4, request.getReason());
            statement.setString(5, createdAt);
            statement.setString(6, "待审核");
            statement.setString(7, "-");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated status-change id was returned.");
                }
                StatusChangeDto dto = new StatusChangeDto();
                dto.setChangeId(keys.getLong(1));
                dto.setStudentId(profile.getStudentId());
                dto.setStudentName(profile.getName());
                dto.setChangeType(request.getChangeType());
                dto.setReason(request.getReason());
                dto.setChangeDate(createdAt);
                dto.setAuditStatus("待审核");
                dto.setOperator("-");
                return dto;
            }
        } catch (SQLException exception) {
            throw failure("Cannot create student status change.", exception);
        }
    }

    @Override
    public List<StatusChangeDto> listStatusChanges(String studentId) {
        String sql = "SELECT * FROM tblStudentStatusChange";
        boolean filtered = studentId != null && !studentId.isBlank();
        if (filtered) {
            sql += " WHERE studentNumber = ?";
        }
        sql += " ORDER BY changeId DESC";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filtered) {
                String number = resolveStudentNumber(studentId).orElse(studentId.trim());
                statement.setString(1, number);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<StatusChangeDto> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(readStatusChange(result));
                }
                return List.copyOf(rows);
            }
        } catch (SQLException exception) {
            throw failure("Cannot list student status changes.", exception);
        }
    }

    @Override
    public synchronized boolean auditStatusChange(
            Long changeId, boolean approved, String operator) {
        if (changeId == null) {
            return false;
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String studentNumber;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT studentNumber, auditStatus FROM tblStudentStatusChange "
                                + "WHERE changeId = ?")) {
                    select.setLong(1, changeId);
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next() || !"待审核".equals(result.getString("auditStatus"))) {
                            connection.rollback();
                            return false;
                        }
                        studentNumber = result.getString("studentNumber");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE tblStudentStatusChange SET auditStatus = ?, "
                                + "operatorNameSnapshot = ? WHERE changeId = ?")) {
                    update.setString(1, approved ? "已通过" : "已驳回");
                    update.setString(2, operator == null ? "-" : operator);
                    update.setLong(3, changeId);
                    update.executeUpdate();
                }
                if (approved) {
                    String changeType;
                    try (PreparedStatement select = connection.prepareStatement(
                            "SELECT changeType FROM tblStudentStatusChange WHERE changeId = ?")) {
                        select.setLong(1, changeId);
                        try (ResultSet result = select.executeQuery()) {
                            result.next();
                            changeType = result.getString(1);
                        }
                    }
                    String status = switch (changeType) {
                        case "休学" -> "休学";
                        case "复学" -> "在读";
                        case "退学" -> "退学";
                        default -> null;
                    };
                    if (status != null) {
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE tblStudentProfile SET academicStatus = ? "
                                        + "WHERE studentNumber = ?")) {
                            update.setString(1, status);
                            update.setString(2, studentNumber);
                            update.executeUpdate();
                        }
                    }
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot audit student status change.", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, PROFILE_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblStudentProfile ("
                        + "profileId LONG PRIMARY KEY, userId TEXT(36) NOT NULL, "
                        + "studentNumber TEXT(20) NOT NULL, gender TEXT(10), ethnicity TEXT(30), "
                        + "nativePlace TEXT(100), idCardNumber TEXT(30), birthDate TEXT(20), "
                        + "enrollmentDate TEXT(20), enrollmentYear LONG, department TEXT(100), "
                        + "major TEXT(100), className TEXT(100), schoolingLength LONG, "
                        + "academicStatus TEXT(30), planId LONG, currentTerm LONG, campusId LONG, "
                        + "politicalStatus TEXT(30), phone TEXT(30), email TEXT(100), "
                        + "homeAddress TEXT(255), emergencyContact TEXT(100), emergencyPhone TEXT(30))");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblStudentProfile_userId "
                        + "ON tblStudentProfile (userId)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblStudentProfile_number "
                        + "ON tblStudentProfile (studentNumber)");
            }
            if (!tableExists(connection, CHANGE_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblStudentStatusChange ("
                        + "changeId AUTOINCREMENT PRIMARY KEY, studentNumber TEXT(20) NOT NULL, "
                        + "studentNameSnapshot TEXT(100) NOT NULL, changeType TEXT(30) NOT NULL, "
                        + "reason TEXT(255), changeDate TEXT(30) NOT NULL, "
                        + "auditStatus TEXT(30) NOT NULL, operatorNameSnapshot TEXT(100) NOT NULL)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize student schema.", exception);
        }
    }

    private void seedIfEmpty() {
        try (Connection connection = database.openConnection();
             Statement count = connection.createStatement();
             ResultSet result = count.executeQuery("SELECT COUNT(*) FROM tblStudentProfile")) {
            result.next();
            if (result.getInt(1) != 0) {
                return;
            }
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tblStudentProfile (profileId, userId, studentNumber, gender, "
                            + "ethnicity, nativePlace, idCardNumber, birthDate, enrollmentDate, "
                            + "enrollmentYear, department, major, className, schoolingLength, "
                            + "academicStatus, planId, currentTerm, campusId, politicalStatus, "
                            + "phone, email, homeAddress, emergencyContact, emergencyPhone) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                List<FinalDemoRoster.AccountSeed> students = FinalDemoRoster.students();
                for (int index = 0; index < students.size(); index++) {
                    FinalDemoRoster.AccountSeed student = students.get(index);
                    int number = index + 1;
                    String major = number <= 5 ? "计算机科学与技术"
                            : number <= 10 ? "软件工程" : "网络空间安全";
                    String className = number <= 5 ? "计科2601班"
                            : number <= 10 ? "软件2601班" : "网安2601班";
                    bindSeed(insert, number, student, major, className);
                    insert.addBatch();
                }
                insert.executeBatch();
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
            throw failure("Cannot seed student profiles.", exception);
        }
    }

    private static void bindSeed(
            PreparedStatement statement,
            int index,
            FinalDemoRoster.AccountSeed student,
            String major,
            String className) throws SQLException {
        int column = 1;
        statement.setLong(column++, index);
        statement.setString(column++, student.userId());
        statement.setString(column++, student.campusCardNumber());
        statement.setString(column++, index % 2 == 0 ? "女" : "男");
        statement.setString(column++, "汉族");
        statement.setString(column++, index % 3 == 0 ? "江苏省苏州市" : "江苏省南京市");
        statement.setString(column++, String.format("320102200601%02d%04d", index, index));
        statement.setString(column++, String.format("2006-01-%02d", index));
        statement.setString(column++, "2026-09-01");
        statement.setInt(column++, 2026);
        statement.setString(column++, "计算机科学与工程学院");
        statement.setString(column++, major);
        statement.setString(column++, className);
        statement.setInt(column++, 4);
        statement.setString(column++, "在读");
        statement.setLong(column++, index <= 5 ? 1L : index <= 10 ? 2L : 3L);
        statement.setInt(column++, 1);
        statement.setLong(column++, 1L);
        statement.setString(column++, index % 3 == 0 ? "群众" : "共青团员");
        statement.setString(column++, String.format("1380000%04d", index));
        statement.setString(column++, student.campusCardNumber() + "@seu.edu.cn");
        statement.setString(column++, "江苏省南京市江宁区东南大学九龙湖校区");
        statement.setString(column++, student.displayName() + "家长");
        statement.setString(column, String.format("1390000%04d", index));
    }

    private StudentProfileDto readProfile(ResultSet result) throws SQLException {
        StudentProfileDto profile = new StudentProfileDto();
        profile.setId(result.getLong("profileId"));
        profile.setStudentId(result.getString("studentNumber"));
        String userId = result.getString("userId");
        profile.setName(users.findByUserId(userId)
                .map(identity -> identity.displayName())
                .orElse(result.getString("studentNumber")));
        profile.setGender(result.getString("gender"));
        profile.setEthnicity(result.getString("ethnicity"));
        profile.setNativePlace(result.getString("nativePlace"));
        profile.setIdCardNumber(result.getString("idCardNumber"));
        profile.setBirthDate(result.getString("birthDate"));
        profile.setEnrollmentDate(result.getString("enrollmentDate"));
        profile.setEnrollmentYear(integer(result, "enrollmentYear"));
        profile.setDepartment(result.getString("department"));
        profile.setMajor(result.getString("major"));
        profile.setClassName(result.getString("className"));
        profile.setSchoolingLength(integer(result, "schoolingLength"));
        profile.setAcademicStatus(result.getString("academicStatus"));
        profile.setPlanId(longValue(result, "planId"));
        profile.setCurrentTerm(integer(result, "currentTerm"));
        profile.setCampusId(longValue(result, "campusId"));
        profile.setPoliticalStatus(result.getString("politicalStatus"));
        profile.setPhone(result.getString("phone"));
        profile.setEmail(result.getString("email"));
        profile.setHomeAddress(result.getString("homeAddress"));
        profile.setEmergencyContact(result.getString("emergencyContact"));
        profile.setEmergencyPhone(result.getString("emergencyPhone"));
        return profile;
    }

    private static StatusChangeDto readStatusChange(ResultSet result) throws SQLException {
        StatusChangeDto dto = new StatusChangeDto();
        dto.setChangeId(result.getLong("changeId"));
        dto.setStudentId(result.getString("studentNumber"));
        dto.setStudentName(result.getString("studentNameSnapshot"));
        dto.setChangeType(result.getString("changeType"));
        dto.setReason(result.getString("reason"));
        dto.setChangeDate(result.getString("changeDate"));
        dto.setAuditStatus(result.getString("auditStatus"));
        dto.setOperator(result.getString("operatorNameSnapshot"));
        return dto;
    }

    private Optional<String> resolveStudentNumber(String userIdOrNumber) {
        return findByStudentId(userIdOrNumber).map(StudentProfileDto::getStudentId);
    }

    private static Integer integer(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static Long longValue(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static String valueOrCurrent(String value, String current) {
        return value == null ? current : value;
    }

    private static boolean tableExists(Connection connection, String expected) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message + " Database: " + database.path(), cause);
    }
}
