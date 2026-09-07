package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.UserAuditLogEntry;
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

/** Access-backed append-only account-management audit log. */
final class AccessUserAuditRepository implements UserAuditRepository {

    private static final String TABLE = "tblUserAuditLog";

    private final AccessDatabase database;

    AccessUserAuditRepository(AccessDatabase database) {
        this.database = database;
        initializeSchema();
    }

    @Override
    public synchronized void append(UserAuditLogEntry entry) {
        String sql = "INSERT INTO tblUserAuditLog "
                + "(auditId, occurredAt, actorUserId, actorUsername, actorDisplayName, "
                + "actionCode, targetText, successful, detailText) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.getAuditId());
            statement.setTimestamp(2, new Timestamp(entry.getOccurredAtEpochMillis()));
            statement.setString(3, entry.getActorUserId());
            statement.setString(4, entry.getActorUsername());
            statement.setString(5, entry.getActorDisplayName());
            statement.setString(6, entry.getActionCode());
            statement.setString(7, entry.getTarget());
            statement.setBoolean(8, entry.isSuccessful());
            statement.setString(9, entry.getDetail());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Cannot append user audit record.", exception);
        }
    }

    @Override
    public synchronized List<UserAuditLogEntry> findAll() {
        String sql = "SELECT * FROM tblUserAuditLog "
                + "ORDER BY occurredAt DESC, auditId DESC";
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<UserAuditLogEntry> entries = new ArrayList<>();
            while (result.next()) {
                entries.add(new UserAuditLogEntry(
                        result.getString("auditId"),
                        result.getTimestamp("occurredAt").getTime(),
                        result.getString("actorUserId"),
                        result.getString("actorUsername"),
                        result.getString("actorDisplayName"),
                        result.getString("actionCode"),
                        result.getString("targetText"),
                        result.getBoolean("successful"),
                        result.getString("detailText")));
            }
            return List.copyOf(entries);
        } catch (SQLException exception) {
            throw failure("Cannot read user audit records.", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection()) {
            if (tableExists(connection, TABLE)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE tblUserAuditLog ("
                        + "auditId TEXT(36) PRIMARY KEY, "
                        + "occurredAt DATETIME NOT NULL, "
                        + "actorUserId TEXT(36) NOT NULL, "
                        + "actorUsername TEXT(50) NOT NULL, "
                        + "actorDisplayName TEXT(100) NOT NULL, "
                        + "actionCode TEXT(80) NOT NULL, "
                        + "targetText TEXT(120) NOT NULL, "
                        + "successful YESNO NOT NULL, "
                        + "detailText TEXT(255) NOT NULL)");
                statement.executeUpdate("CREATE INDEX ix_tblUserAuditLog_occurredAt "
                        + "ON tblUserAuditLog (occurredAt)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize Access user audit schema.", exception);
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
        }
        return false;
    }

    private UserPersistenceException failure(String message, SQLException cause) {
        return new UserPersistenceException(
                message + " Database: " + database.path(), cause);
    }
}
