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
import java.util.Objects;

/** Access-backed campus-card balances shared by every client. */
final class AccessCampusCardStore implements CampusCardRepository {

    private static final String TABLE = "tblCampusCard";
    private static final int MAX_BALANCE_FEN = 1_000_000;
    private final AccessDatabase database;

    AccessCampusCardStore(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        initializeSchema();
        seedDemoBalances();
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

    private void seedDemoBalances() {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement exists = connection.prepareStatement(
                    "SELECT userId FROM tblCampusCard WHERE userId = ?");
                 PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tblCampusCard (userId, campusCardNumber, balanceFen) VALUES (?, ?, ?)")) {
                int pendingInsertCount = 0;
                for (FinalDemoRoster.AccountSeed account : FinalDemoRoster.accounts()) {
                    exists.setString(1, account.userId());
                    try (ResultSet result = exists.executeQuery()) {
                        if (result.next()) {
                            continue;
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
            throw failure("Cannot seed campus cards.", exception);
        }
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
