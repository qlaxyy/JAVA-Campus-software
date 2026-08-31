package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Access-backed account repository. Sessions deliberately remain in memory. */
final class AccessUserRepository implements UserRepository {

    private static final String USER_TABLE = "tblUser";
    private static final String SCOPE_TABLE = "tblUserAdminScope";

    private final AccessDatabase database;

    AccessUserRepository(AccessDatabase database) {
        this.database = database;
        initializeSchema();
    }

    @Override
    public Optional<UserAccount> findById(String userId) {
        return findOne("SELECT * FROM tblUser WHERE userId = ?", userId);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return findOne("SELECT * FROM tblUser WHERE username = ?", username);
    }

    @Override
    public List<UserAccount> findAll() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM tblUser ORDER BY username")) {
            Map<String, Set<AdminScope>> scopes = loadAllScopes(connection);
            List<UserAccount> accounts = new ArrayList<>();
            while (result.next()) {
                accounts.add(readAccount(result, scopes.getOrDefault(
                        result.getString("userId"), Set.of())));
            }
            return accounts;
        } catch (SQLException exception) {
            throw failure("Cannot list user accounts.", exception);
        }
    }

    @Override
    public synchronized void save(UserAccount account) {
        saveTransaction(List.of(account));
    }

    @Override
    public synchronized void saveAll(List<UserAccount> accounts) {
        saveTransaction(List.copyOf(accounts));
    }

    private void saveTransaction(List<UserAccount> accounts) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (UserAccount account : accounts) {
                    if (exists(connection, account.userId())) {
                        updateAccount(connection, account);
                    } else {
                        insertAccount(connection, account);
                    }
                    replaceScopes(connection, account);
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot save user accounts.", exception);
        }
    }

    private Optional<UserAccount> findOne(String sql, String value) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String userId = result.getString("userId");
                return Optional.of(readAccount(result, loadScopes(connection, userId)));
            }
        } catch (SQLException exception) {
            throw failure("Cannot read user account.", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection()) {
            if (!tableExists(connection, USER_TABLE)) {
                execute(connection, "CREATE TABLE tblUser ("
                        + "userId TEXT(36) PRIMARY KEY, "
                        + "username TEXT(50) NOT NULL, "
                        + "displayName TEXT(100) NOT NULL, "
                        + "roleCode TEXT(20) NOT NULL, "
                        + "passwordProof TEXT(64) NOT NULL, "
                        + "enabled YESNO NOT NULL)");
                execute(connection,
                        "CREATE UNIQUE INDEX ux_tblUser_username ON tblUser (username)");
            }
            if (!tableExists(connection, SCOPE_TABLE)) {
                execute(connection, "CREATE TABLE tblUserAdminScope ("
                        + "scopeId AUTOINCREMENT PRIMARY KEY, "
                        + "userId TEXT(36) NOT NULL, "
                        + "moduleCode TEXT(20) NOT NULL)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblUserAdminScope_user_module "
                        + "ON tblUserAdminScope (userId, moduleCode)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize Access user schema.", exception);
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

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private boolean exists(Connection connection, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT userId FROM tblUser WHERE userId = ?")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void insertAccount(Connection connection, UserAccount account) throws SQLException {
        String sql = "INSERT INTO tblUser "
                + "(userId, username, displayName, roleCode, passwordProof, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.userId());
            statement.setString(2, account.username());
            statement.setString(3, account.displayName());
            statement.setString(4, account.role().name());
            statement.setString(5, account.passwordProof());
            statement.setBoolean(6, account.enabled());
            statement.executeUpdate();
        }
    }

    private void updateAccount(Connection connection, UserAccount account) throws SQLException {
        String sql = "UPDATE tblUser SET username = ?, displayName = ?, roleCode = ?, "
                + "passwordProof = ?, enabled = ? WHERE userId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.username());
            statement.setString(2, account.displayName());
            statement.setString(3, account.role().name());
            statement.setString(4, account.passwordProof());
            statement.setBoolean(5, account.enabled());
            statement.setString(6, account.userId());
            statement.executeUpdate();
        }
    }

    private void replaceScopes(Connection connection, UserAccount account) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tblUserAdminScope WHERE userId = ?")) {
            delete.setString(1, account.userId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO tblUserAdminScope (userId, moduleCode) VALUES (?, ?)")) {
            boolean hasScopes = false;
            for (AdminScope scope : account.adminScopes()) {
                insert.setString(1, account.userId());
                insert.setString(2, scope.name());
                insert.addBatch();
                hasScopes = true;
            }
            if (hasScopes) {
                insert.executeBatch();
            }
        }
    }

    private Set<AdminScope> loadScopes(Connection connection, String userId) throws SQLException {
        EnumSet<AdminScope> scopes = EnumSet.noneOf(AdminScope.class);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT moduleCode FROM tblUserAdminScope WHERE userId = ?")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    scopes.add(parseScope(result.getString("moduleCode")));
                }
            }
        }
        return scopes;
    }

    private Map<String, Set<AdminScope>> loadAllScopes(Connection connection) throws SQLException {
        Map<String, Set<AdminScope>> scopes = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT userId, moduleCode FROM tblUserAdminScope")) {
            while (result.next()) {
                scopes.computeIfAbsent(
                                result.getString("userId"),
                                ignored -> EnumSet.noneOf(AdminScope.class))
                        .add(parseScope(result.getString("moduleCode")));
            }
        }
        return scopes;
    }

    private UserAccount readAccount(ResultSet result, Set<AdminScope> scopes)
            throws SQLException {
        try {
            return new UserAccount(
                    result.getString("userId"),
                    result.getString("username"),
                    result.getString("displayName"),
                    Role.valueOf(result.getString("roleCode").toUpperCase(Locale.ROOT)),
                    scopes,
                    result.getString("passwordProof"),
                    result.getBoolean("enabled"));
        } catch (IllegalArgumentException exception) {
            throw new SQLException("Access contains an unknown role or admin scope.", exception);
        }
    }

    private AdminScope parseScope(String value) throws SQLException {
        try {
            return AdminScope.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new SQLException("Unknown admin scope in Access: " + value, exception);
        }
    }

    private void rollback(Connection connection, SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    private UserPersistenceException failure(String message, SQLException cause) {
        return new UserPersistenceException(
                message + " Database: " + database.path(), cause);
    }
}
