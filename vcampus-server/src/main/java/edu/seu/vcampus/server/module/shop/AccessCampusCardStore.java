package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.demo.FinalDemoRoster;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

/** Access-backed campus-card balances shared by every client. */
final class AccessCampusCardStore implements CampusCardRepository {

    private static final String TABLE = "tblCampusCard";
    private static final int MAX_BALANCE_FEN = 1_000_000;
    private final AccessDatabase database;

    AccessCampusCardStore(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        initializeSchema();
        synchronizeCampusCards();
    }

    @Override
    public synchronized CampusCardView view(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        try (Connection connection = database.openConnection()) {
            CampusCardView existing = find(connection, session.getUserId());
            if (existing != null) {
                return existing;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tblCampusCard (userId, campusCardNumber, balanceFen) VALUES (?, ?, 0)")) {
                statement.setString(1, session.getUserId());
                statement.setString(2, session.getUsername());
                statement.executeUpdate();
            }
            return new CampusCardView(session.getUserId(), session.getUsername(), session.getUsername(), 0);
        } catch (SQLException exception) {
            throw failure("Cannot read campus card.", exception);
        }
    }

    @Override
    public synchronized CampusCardView recharge(SessionInfo session, int amountFen) {
        if (amountFen <= 0) {
            throw new IllegalArgumentException("amountFen must be positive");
        }
        view(session);
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tblCampusCard SET balanceFen = balanceFen + ? "
                             + "WHERE userId = ? AND balanceFen + ? <= ?")) {
            statement.setInt(1, amountFen);
            statement.setString(2, session.getUserId());
            statement.setInt(3, amountFen);
            statement.setInt(4, MAX_BALANCE_FEN);
            if (statement.executeUpdate() != 1) {
                throw new ShopBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "校园卡余额已达上限。");
            }
            return view(session);
        } catch (SQLException exception) {
            throw failure("Cannot recharge campus card.", exception);
        }
    }

    @Override
    public synchronized CampusCardView deduct(SessionInfo session, int amountFen) {
        view(session);
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tblCampusCard SET balanceFen = balanceFen - ? "
                             + "WHERE userId = ? AND balanceFen >= ?")) {
            statement.setInt(1, amountFen);
            statement.setString(2, session.getUserId());
            statement.setInt(3, amountFen);
            if (statement.executeUpdate() != 1) {
                throw new ShopBusinessException(ErrorCodes.SHOP_INSUFFICIENT_BALANCE, "余额不足，请充值！");
            }
            return view(session);
        } catch (SQLException exception) {
            throw failure("Cannot deduct campus card.", exception);
        }
    }

    @Override
    public synchronized void refund(String userId, int amountFen) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tblCampusCard SET balanceFen = balanceFen + ? WHERE userId = ?")) {
            statement.setInt(1, amountFen);
            statement.setString(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Cannot refund campus card.", exception);
        }
    }

    private CampusCardView find(Connection connection, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tblCampusCard WHERE userId = ?")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new CampusCardView(
                        result.getString("userId"), result.getString("campusCardNumber"),
                        result.getString("campusCardNumber"), result.getInt("balanceFen")) : null;
            }
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, TABLE)) {
                statement.executeUpdate("CREATE TABLE tblCampusCard ("
                        + "userId TEXT(64) PRIMARY KEY, campusCardNumber TEXT(8) NOT NULL, "
                        + "balanceFen LONG NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblCampusCard_number "
                        + "ON tblCampusCard (campusCardNumber)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize campus-card schema.", exception);
        }
    }

    /**
     * Adds missing cards without changing an existing card owner or balance.
     *
     * <p>The complete campus server owns its account list in {@code tblUser}; an older
     * database may use card numbers that have since been reassigned in the final-demo
     * fixture.  Reading the actual accounts prevents a fixture from taking over those
     * numbers during startup.  Standalone shop repositories do not have a user table,
     * so they retain the original demo-fixture behavior.</p>
     */
    private void synchronizeCampusCards() {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement userExists = connection.prepareStatement(
                    "SELECT userId FROM tblCampusCard WHERE userId = ?");
                 PreparedStatement numberExists = connection.prepareStatement(
                    "SELECT userId FROM tblCampusCard WHERE campusCardNumber = ?");
                 PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tblCampusCard (userId, campusCardNumber, balanceFen) VALUES (?, ?, ?)")) {
                int pendingInsertCount = 0;
                for (CampusCardSeed account : cardSeeds(connection)) {
                    userExists.setString(1, account.userId());
                    try (ResultSet result = userExists.executeQuery()) {
                        if (result.next()) {
                            continue;
                        }
                    }
                    numberExists.setString(1, account.campusCardNumber());
                    try (ResultSet result = numberExists.executeQuery()) {
                        if (result.next()) {
                            throw new SQLException(
                                    "Campus-card number " + account.campusCardNumber()
                                            + " already belongs to user "
                                            + result.getString("userId")
                                            + "; cannot assign it to " + account.userId());
                        }
                    }
                    insert.setString(1, account.userId());
                    insert.setString(2, account.campusCardNumber());
                    insert.setInt(3, InMemoryCampusCardStore.DEMO_BALANCE_FEN);
                    insert.addBatch();
                    pendingInsertCount++;
                }
                if (pendingInsertCount > 0) {
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot synchronize campus cards with user accounts.", exception);
        }
    }

    private List<CampusCardSeed> cardSeeds(Connection connection) throws SQLException {
        if (!tableExists(connection, "tblUser")) {
            return FinalDemoRoster.accounts().stream()
                    .map(account -> new CampusCardSeed(
                            account.userId(), account.campusCardNumber()))
                    .toList();
        }
        List<CampusCardSeed> accounts = new java.util.ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT userId, username FROM tblUser ORDER BY username")) {
            while (result.next()) {
                accounts.add(new CampusCardSeed(
                        result.getString("userId"), result.getString("username")));
            }
        }
        return accounts;
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

    private record CampusCardSeed(String userId, String campusCardNumber) {
    }
}
