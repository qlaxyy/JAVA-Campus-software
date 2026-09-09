package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Access-backed repository for the shared teacher-profile table. */
final class AccessTeacherRepository implements TeacherRepository {

    private static final String TABLE = "tblTeacherProfile";
    private final AccessDatabase database;

    AccessTeacherRepository(AccessDatabase database) {
        this.database = database;
        initializeSchema();
    }

    @Override
    public Optional<TeacherProfile> findByUserId(String userId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM tblTeacherProfile WHERE teacherUserId = ?")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Cannot read teacher profile.", exception);
        }
    }

    @Override
    public List<TeacherProfile> findAll() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM tblTeacherProfile ORDER BY teacherUserId")) {
            List<TeacherProfile> profiles = new ArrayList<>();
            while (result.next()) {
                profiles.add(read(result));
            }
            return List.copyOf(profiles);
        } catch (SQLException exception) {
            throw failure("Cannot list teacher profiles.", exception);
        }
    }

    @Override
    public synchronized void save(TeacherProfile profile) {
        try (Connection connection = database.openConnection()) {
            if (exists(connection, profile.userId())) {
                update(connection, profile);
            } else {
                insert(connection, profile);
            }
        } catch (SQLException exception) {
            throw failure("Cannot save teacher profile.", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection()) {
            if (tableExists(connection, TABLE)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE tblTeacherProfile ("
                        + "teacherUserId TEXT(36) PRIMARY KEY, "
                        + "department TEXT(100) NOT NULL, "
                        + "teacherTitle TEXT(50) NOT NULL, "
                        + "active YESNO NOT NULL, "
                        + "createdByUserId TEXT(36) NOT NULL, "
                        + "createdAt DATETIME NOT NULL, "
                        + "updatedAt DATETIME NOT NULL)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize teacher schema.", exception);
        }
    }

    private static TeacherProfile read(ResultSet result) throws SQLException {
        return new TeacherProfile(
                result.getString("teacherUserId"),
                result.getString("department"),
                result.getString("teacherTitle"),
                result.getBoolean("active"),
                result.getString("createdByUserId"),
                result.getTimestamp("createdAt").toInstant(),
                result.getTimestamp("updatedAt").toInstant());
    }

    private static boolean tableExists(Connection connection, String expected)
            throws SQLException {
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

    private static boolean exists(Connection connection, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT teacherUserId FROM tblTeacherProfile WHERE teacherUserId = ?")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void insert(Connection connection, TeacherProfile profile)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblTeacherProfile "
                        + "(teacherUserId, department, teacherTitle, active, "
                        + "createdByUserId, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, profile, true);
            statement.executeUpdate();
        }
    }

    private static void update(Connection connection, TeacherProfile profile)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tblTeacherProfile SET department = ?, teacherTitle = ?, active = ?, "
                        + "updatedAt = ? WHERE teacherUserId = ?")) {
            statement.setString(1, profile.department());
            statement.setString(2, profile.title());
            statement.setBoolean(3, profile.active());
            statement.setTimestamp(4, Timestamp.from(profile.updatedAt()));
            statement.setString(5, profile.userId());
            statement.executeUpdate();
        }
    }

    private static void bind(
            PreparedStatement statement,
            TeacherProfile profile,
            boolean includeCreation) throws SQLException {
        statement.setString(1, profile.userId());
        statement.setString(2, profile.department());
        statement.setString(3, profile.title());
        statement.setBoolean(4, profile.active());
        if (includeCreation) {
            statement.setString(5, profile.createdByUserId());
            statement.setTimestamp(6, Timestamp.from(profile.createdAt()));
            statement.setTimestamp(7, Timestamp.from(profile.updatedAt()));
        }
    }

    private static UserPersistenceException failure(String message, SQLException cause) {
        return new UserPersistenceException(message, cause);
    }
}
