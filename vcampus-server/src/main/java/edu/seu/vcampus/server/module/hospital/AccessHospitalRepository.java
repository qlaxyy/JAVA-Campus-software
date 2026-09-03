package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Access persistence for doctor applications and profiles; slot data remains staged. */
final class AccessHospitalRepository implements HospitalRepository {

    private static final String APPLICATION_TABLE = "tblHospitalDoctorApplication";
    private static final String PROFILE_TABLE = "tblHospitalDoctor";

    private final AccessDatabase database;
    private final InMemoryHospitalRepository stagedCatalog;

    AccessHospitalRepository(AccessDatabase database, Clock clock) {
        this.database = database;
        this.stagedCatalog = new InMemoryHospitalRepository(clock);
        initializeSchema();
        seedDemonstrationDoctor();
    }

    @Override
    public boolean isActiveDoctorUser(String userId) {
        if (userId == null) {
            return false;
        }
        String sql = "SELECT active FROM tblHospitalDoctor WHERE doctorUserId = ?";
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
            boolean legacyNumberColumn = columnExists(
                    connection, PROFILE_TABLE, "doctorNumber");
            String sql = existing
                    ? "UPDATE tblHospitalDoctor SET departmentId = ?, "
                            + "doctorTitle = ?, active = ? WHERE doctorUserId = ?"
                    : legacyNumberColumn
                            ? "INSERT INTO tblHospitalDoctor "
                                    + "(departmentId, doctorTitle, active, doctorUserId, "
                                    + "doctorNumber) VALUES (?, ?, ?, ?, ?)"
                            : "INSERT INTO tblHospitalDoctor "
                                    + "(departmentId, doctorTitle, active, doctorUserId) "
                                    + "VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, profile.departmentId());
                statement.setString(2, profile.doctorTitle());
                statement.setBoolean(3, profile.active());
                statement.setString(4, profile.userId());
                if (!existing && legacyNumberColumn) {
                    statement.setString(5, legacyValue("LEGACY_", profile.userId()));
                }
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("Cannot save doctor profile.", exception);
        }
    }

    @Override
    public List<HospitalDepartment> findActiveDepartments() {
        return stagedCatalog.findActiveDepartments();
    }

    @Override
    public List<HospitalSlot> findSlots(LocalDate startDate, LocalDate endDate) {
        return stagedCatalog.findSlots(startDate, endDate);
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection()) {
            if (!tableExists(connection, PROFILE_TABLE)) {
                execute(connection, "CREATE TABLE tblHospitalDoctor ("
                        + "doctorUserId TEXT(36) PRIMARY KEY, "
                        + "departmentId TEXT(36) NOT NULL, "
                        + "doctorTitle TEXT(50) NOT NULL, "
                        + "active YESNO NOT NULL)");
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
        } catch (SQLException exception) {
            throw failure("Cannot initialize hospital onboarding schema.", exception);
        }
    }

    private void seedDemonstrationDoctor() {
        if (!doctorProfileExists("U-TEACHER-001")) {
            saveDoctorProfile(new DoctorProfile(
                    "U-TEACHER-001", "dept-general", "演示医生", true));
        }
    }

    private boolean doctorProfileExists(String userId) {
        try (Connection connection = database.openConnection()) {
            return profileExists(connection, userId);
        } catch (SQLException exception) {
            throw failure("Cannot check doctor profile.", exception);
        }
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
                "SELECT doctorUserId FROM tblHospitalDoctor WHERE doctorUserId = ?", userId);
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
