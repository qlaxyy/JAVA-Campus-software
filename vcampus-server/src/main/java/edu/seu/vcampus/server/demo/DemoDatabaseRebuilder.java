package edu.seu.vcampus.server.demo;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.infrastructure.database.DatabaseProcessLock;
import edu.seu.vcampus.server.module.ServerModules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Safely backs up and atomically rebuilds the one final demonstration database. */
public final class DemoDatabaseRebuilder {
    private static final DateTimeFormatter BACKUP_CLOCK =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private DemoDatabaseRebuilder() { }

    public static Result rebuild(Path databasePath) throws IOException {
        Path target = databasePath.toAbsolutePath().normalize();
        try (DatabaseProcessLock ignored = DatabaseProcessLock.acquire(target)) {
            Path parent = target.getParent();
            Files.createDirectories(parent);
            Path backup = null;
            if (Files.exists(target)) {
                Path backupDirectory = parent.resolve("backups");
                Files.createDirectories(backupDirectory);
                backup = backupDirectory.resolve("vCampus-" + LocalDateTime.now().format(BACKUP_CLOCK) + ".accdb");
                Files.copy(target, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }

            Path temporary = parent.resolve("vCampus-rebuild-" + UUID.randomUUID() + ".accdb");
            try {
                ServerModules.createPersistentRouter(temporary);
                normalizeDoctors(temporary);
                normalizeCourseData(temporary);
                appendInitializationAudit(temporary);
                validate(temporary);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return new Result(target, backup, FinalDemoRoster.accounts().size());
            } catch (RuntimeException | IOException exception) {
                Files.deleteIfExists(temporary);
                throw exception;
            }
        }
    }

    private static void normalizeDoctors(Path databasePath) {
        AccessDatabase database = new AccessDatabase(databasePath);
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (FinalDemoRoster.DoctorSeed doctor : FinalDemoRoster.doctors()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE tblHospitalDoctor SET userId = ?, departmentId = ?, doctorName = ?, "
                                    + "doctorTitle = ?, [active] = TRUE, updatedAt = ? WHERE doctorId = ?")) {
                        update.setString(1, doctor.userId());
                        update.setString(2, doctor.departmentId());
                        update.setString(3, FinalDemoRoster.displayName(doctor.userId()));
                        update.setString(4, doctor.title());
                        update.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                        update.setString(6, doctor.doctorId());
                        if (update.executeUpdate() != 1) throw new IllegalStateException("Missing seeded doctor: " + doctor.doctorId());
                    }
                    try (PreparedStatement schedules = connection.prepareStatement(
                            "UPDATE tblHospitalSchedule SET departmentId = ? WHERE doctorId = ?")) {
                        schedules.setString(1, doctor.departmentId());
                        schedules.setString(2, doctor.doctorId());
                        schedules.executeUpdate();
                    }
                }
                try (PreparedStatement deleteSchedules = connection.prepareStatement(
                        "DELETE FROM tblHospitalSchedule WHERE doctorId = ?");
                     PreparedStatement deleteDoctor = connection.prepareStatement(
                        "DELETE FROM tblHospitalDoctor WHERE doctorId = ?")) {
                    deleteSchedules.setString(1, "doctor-sun"); deleteSchedules.executeUpdate();
                    deleteDoctor.setString(1, "doctor-sun"); deleteDoctor.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot normalize final hospital data.", exception);
        }
    }

    private static void appendInitializationAudit(Path databasePath) {
        AccessDatabase database = new AccessDatabase(databasePath);
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tblUserAuditLog (auditId, occurredAt, actorUserId, actorUsername, "
                             + "actorDisplayName, actionCode, targetText, successful, detailText) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(3, FinalDemoRoster.SUPER_ADMIN_USER_ID);
            statement.setString(4, "20260000");
            statement.setString(5, "吴尚扬");
            statement.setString(6, "DEMO_DATABASE_INITIALIZED");
            statement.setString(7, "database/vCampus.accdb");
            statement.setBoolean(8, true);
            statement.setString(9, "已生成39个最终演示账号并完成跨表校验。");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot append initialization audit.", exception);
        }
    }

    private static void normalizeCourseData(Path databasePath) {
        long[] offerings = {1001L, 2001L, 3001L, 9001L, 14001L, 15002L, 14003L, 15001L};
        AccessDatabase database = new AccessDatabase(databasePath);
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM tblGrade");
                    statement.executeUpdate("DELETE FROM tblEnrollment");
                    statement.executeUpdate("UPDATE tblCourseOffering SET selectedCount = 0");
                    statement.executeUpdate("UPDATE tblCatalogOffering SET selectedCount = 0");
                }
                try (PreparedStatement planName = connection.prepareStatement(
                        "UPDATE tblOfferingTeacher SET teacherName = ? WHERE offeringId = ?");
                     PreparedStatement catalogName = connection.prepareStatement(
                        "UPDATE tblCatalogTeacher SET teacherName = ? WHERE offeringId = ?")) {
                    for (int index = 0; index < offerings.length; index++) {
                        String name = FinalDemoRoster.displayName("U-TEACHER-" + String.format("%03d", index + 1));
                        planName.setString(1, name); planName.setLong(2, offerings[index]); planName.addBatch();
                        catalogName.setString(1, name); catalogName.setLong(2, offerings[index]); catalogName.addBatch();
                    }
                    planName.executeBatch(); catalogName.executeBatch();
                }
                try (PreparedStatement enrollment = connection.prepareStatement(
                        "INSERT INTO tblEnrollment (userId, studentId, selectedBatchId, offeringId, "
                                + "enrollmentStatus, selectedAt, droppedAt) VALUES (?, ?, 2, ?, 'SELECTED', ?, NULL)",
                        Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement grade = connection.prepareStatement(
                        "INSERT INTO tblGrade (enrollmentId, score, usualScore, finalExamScore, recordedAt) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    for (int index = 0; index < FinalDemoRoster.students().size(); index++) {
                        FinalDemoRoster.AccountSeed student = FinalDemoRoster.students().get(index);
                        long offeringId = offerings[index % offerings.length];
                        enrollment.setString(1, student.userId());
                        enrollment.setString(2, student.campusCardNumber());
                        enrollment.setLong(3, offeringId);
                        enrollment.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusDays(3)));
                        enrollment.executeUpdate();
                        long enrollmentId;
                        try (ResultSet keys = enrollment.getGeneratedKeys()) {
                            if (!keys.next()) throw new IllegalStateException("Cannot read generated enrollment id.");
                            enrollmentId = keys.getLong(1);
                        }
                        if (index < 8) {
                            double usual = 78 + index;
                            double exam = 82 + index;
                            grade.setLong(1, enrollmentId);
                            grade.setDouble(2, usual * 0.4 + exam * 0.6);
                            grade.setDouble(3, usual);
                            grade.setDouble(4, exam);
                            grade.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
                            grade.addBatch();
                        }
                    }
                    grade.executeBatch();
                }
                try (PreparedStatement updatePlan = connection.prepareStatement(
                        "UPDATE tblCourseOffering SET selectedCount = "
                                + "(SELECT COUNT(*) FROM tblEnrollment e WHERE e.offeringId = tblCourseOffering.offeringId "
                                + "AND e.enrollmentStatus = 'SELECTED')");
                     PreparedStatement updateCatalog = connection.prepareStatement(
                        "UPDATE tblCatalogOffering SET selectedCount = "
                                + "(SELECT COUNT(*) FROM tblEnrollment e WHERE e.offeringId = tblCatalogOffering.offeringId "
                                + "AND e.enrollmentStatus = 'SELECTED')")) {
                    updatePlan.executeUpdate(); updateCatalog.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot normalize final course data.", exception);
        }
    }

    public static void validate(Path databasePath) {
        AccessDatabase database = new AccessDatabase(databasePath);
        try (Connection connection = database.openConnection()) {
            requireCount(connection, "SELECT COUNT(*) FROM tblUser WHERE enabled = TRUE", 39, "enabled accounts");
            requireCount(connection, "SELECT COUNT(*) FROM tblUser", 39, "all accounts");
            requireCount(connection, "SELECT COUNT(*) FROM tblUser WHERE roleCode = 'SUPER_ADMIN'", 1, "super administrators");
            requireCount(connection, "SELECT COUNT(*) FROM tblUser u INNER JOIN tblUserAdminScope s "
                    + "ON u.userId = s.userId WHERE u.roleCode = 'USER'", 5, "subsystem administrator scopes");
            requireCount(connection, "SELECT COUNT(*) FROM tblTeacherProfile WHERE active = TRUE", 8, "teachers");
            requireCount(connection, "SELECT COUNT(*) FROM tblStudentProfile", 15, "student profiles");
            requireCount(connection, "SELECT COUNT(*) FROM tblHospitalDoctor WHERE active = TRUE", 10, "doctors");
            requireCount(connection, "SELECT COUNT(*) FROM tblCampusCard", 39, "campus cards");
            requireCount(connection, "SELECT COUNT(*) FROM tblCourseTeacherAssignment", 8, "teaching assignments");
            requireCount(connection, "SELECT COUNT(*) FROM tblEnrollment WHERE enrollmentStatus = 'SELECTED'", 15, "demo enrollments");
            requireCount(connection, "SELECT COUNT(*) FROM tblBook", 5, "book titles");
            requireCount(connection, "SELECT COUNT(*) FROM tblBookCopy", 20, "book copies");
            requireCount(connection, "SELECT COUNT(*) FROM tblBorrowRecord", 2, "borrow demonstrations");
            requireCount(connection, "SELECT COUNT(*) FROM tblReservation", 1, "reservation demonstrations");
            requireCount(connection, "SELECT COUNT(*) FROM (SELECT username FROM tblUser GROUP BY username HAVING COUNT(*) > 1)", 0, "duplicate card numbers");
            requireCount(connection, "SELECT COUNT(*) FROM (SELECT userId FROM tblUser GROUP BY userId HAVING COUNT(*) > 1)", 0, "duplicate user ids");
            requireCount(connection, "SELECT COUNT(*) FROM tblTeacherProfile t LEFT JOIN tblUser u "
                    + "ON t.teacherUserId = u.userId WHERE u.userId IS NULL", 0, "orphan teachers");
            requireCount(connection, "SELECT COUNT(*) FROM tblStudentProfile s LEFT JOIN tblUser u "
                    + "ON s.userId = u.userId WHERE u.userId IS NULL", 0, "orphan students");
            requireCount(connection, "SELECT COUNT(*) FROM tblHospitalDoctor d LEFT JOIN tblUser u "
                    + "ON d.userId = u.userId WHERE u.userId IS NULL", 0, "orphan doctors");
            requireCount(connection, "SELECT COUNT(*) FROM tblCourseTeacherAssignment a LEFT JOIN tblTeacherProfile t "
                    + "ON a.teacherUserId = t.teacherUserId WHERE t.teacherUserId IS NULL OR t.active = FALSE", 0, "invalid teaching assignments");
            requireCount(connection, "SELECT COUNT(*) FROM tblEnrollment e LEFT JOIN tblUser u "
                    + "ON e.userId = u.userId WHERE u.userId IS NULL", 0, "orphan enrollments");
            requireCount(connection, "SELECT COUNT(*) FROM tblBorrowRecord b LEFT JOIN tblUser u "
                    + "ON b.userId = u.userId WHERE u.userId IS NULL", 0, "orphan borrow records");
            requireCount(connection, "SELECT COUNT(*) FROM tblReservation r LEFT JOIN tblUser u "
                    + "ON r.userId = u.userId WHERE u.userId IS NULL", 0, "orphan reservations");
            requireCount(connection, "SELECT COUNT(*) FROM tblShopCartItem c LEFT JOIN tblUser u "
                    + "ON c.userId = u.userId WHERE u.userId IS NULL", 0, "orphan cart rows");
            requireCount(connection, "SELECT COUNT(*) FROM tblShopOrder o LEFT JOIN tblUser u "
                    + "ON o.userId = u.userId WHERE u.userId IS NULL", 0, "orphan shop orders");
            requireAtLeast(connection, "SELECT COUNT(*) FROM tblUserAuditLog WHERE actionCode = 'DEMO_DATABASE_INITIALIZED'", 1, "initialization audit events");
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT MAX(username) FROM tblUser WHERE username LIKE '2026%'") ) {
                result.next();
                if (!"20260038".equals(result.getString(1))) {
                    throw new IllegalStateException("Expected maximum demo card number 20260038.");
                }
            }
            for (FinalDemoRoster.AccountSeed account : FinalDemoRoster.accounts()) requireAccount(connection, account);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot validate rebuilt database.", exception);
        }
    }

    private static void requireCount(Connection connection, String sql, int expected, String label) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            int actual = result.getInt(1);
            if (actual != expected) throw new IllegalStateException(label + ": expected " + expected + ", actual " + actual);
        }
    }

    private static void requireAtLeast(
            Connection connection, String sql, int minimum, String label) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            int actual = result.getInt(1);
            if (actual < minimum) throw new IllegalStateException(
                    label + ": expected at least " + minimum + ", actual " + actual);
        }
    }

    private static void requireAccount(
            Connection connection, FinalDemoRoster.AccountSeed expected) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT username, displayName, roleCode, enabled FROM tblUser WHERE userId = ?")) {
            statement.setString(1, expected.userId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException(
                        "Missing final demo user: " + expected.userId());
                if (!expected.campusCardNumber().equals(result.getString("username"))
                        || !expected.displayName().equals(result.getString("displayName"))
                        || !expected.role().name().equals(result.getString("roleCode"))
                        || !result.getBoolean("enabled")) {
                    throw new IllegalStateException(
                            "Final demo user does not match roster: " + expected.userId());
                }
            }
        }
    }

    public record Result(Path databasePath, Path backupPath, int accountCount) { }
}
