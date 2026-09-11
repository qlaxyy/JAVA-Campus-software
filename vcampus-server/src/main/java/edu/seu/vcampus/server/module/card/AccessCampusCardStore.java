package edu.seu.vcampus.server.module.card;

import edu.seu.vcampus.common.card.CampusCardLedgerEntry;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Access-backed campus-card balances and ledger.
 */
public final class AccessCampusCardStore implements CampusCardWallet {

    static final int DEMO_BALANCE_FEN = 10_000;
    private static final int MAX_BALANCE_FEN = 1_000_000;
    private static final String CARD_TABLE = "tblCampusCard";
    private static final String LEDGER_TABLE = "tblCampusCardLedger";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccessDatabase database;

    /** Opens or creates campus-card tables in the shared Access file. */
    public AccessCampusCardStore(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        initializeSchema();
        seedDemoCardsIfEmpty();
    }

    @Override
    public CampusCardView view(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        synchronized (this) {
            try (Connection connection = database.openConnection()) {
                connection.setAutoCommit(false);
                try {
                    CampusCardView card = ensureCard(connection, session);
                    connection.commit();
                    return card;
                } catch (SQLException exception) {
                    rollback(connection);
                    throw exception;
                }
            } catch (SQLException exception) {
                throw failure("Cannot read campus card.", exception);
            }
        }
    }

    @Override
    public CampusCardView recharge(SessionInfo session, int amountFen) {
        Objects.requireNonNull(session, "session must not be null");
        if (amountFen < 1) {
            throw new CardBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "金额无效。");
        }
        synchronized (this) {
            return mutate(session, amountFen, ModuleNames.CARD,
                    "recharge:" + UUID.randomUUID(), CampusCardLedgerEntry.RECHARGE, false);
        }
    }

    @Override
    public CampusCardView debit(
            SessionInfo session, int amountFen, String merchant, String reference) {
        synchronized (this) {
            return mutate(session, amountFen, merchant, reference, CampusCardLedgerEntry.DEBIT, true);
        }
    }

    @Override
    public CampusCardView credit(
            SessionInfo session, int amountFen, String merchant, String reference) {
        synchronized (this) {
            return mutate(session, amountFen, merchant, reference, CampusCardLedgerEntry.CREDIT, false);
        }
    }

    @Override
    public List<CampusCardLedgerEntry> listLedger(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        String sql = "SELECT * FROM tblCampusCardLedger WHERE userId = ? ORDER BY createdAt DESC";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, session.getUserId());
            try (ResultSet result = statement.executeQuery()) {
                List<CampusCardLedgerEntry> entries = new ArrayList<>();
                while (result.next()) {
                    entries.add(readLedger(result));
                }
                return List.copyOf(entries);
            }
        } catch (SQLException exception) {
            throw failure("Cannot read campus-card ledger.", exception);
        }
    }

    private CampusCardView mutate(
            SessionInfo session,
            int amountFen,
            String merchant,
            String reference,
            String entryType,
            boolean debit) {
        Objects.requireNonNull(session, "session must not be null");
        if (amountFen < 1 || merchant == null || merchant.isBlank()
                || reference == null || reference.isBlank()) {
            throw new CardBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "金额或业务单号无效。");
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (findLedger(connection, session.getUserId(), merchant, reference, entryType) != null) {
                    CampusCardView current = ensureCard(connection, session);
                    connection.commit();
                    return current;
                }
                CampusCardView current = ensureCard(connection, session);
                int next = debit
                        ? current.getBalanceFen() - amountFen
                        : Math.addExact(current.getBalanceFen(), amountFen);
                if (debit && next < 0) {
                    throw new CardBusinessException(
                            ErrorCodes.CARD_INSUFFICIENT_BALANCE, "余额不足，请充值！");
                }
                if (next > MAX_BALANCE_FEN) {
                    throw new CardBusinessException(
                            ErrorCodes.COMMON_INVALID_REQUEST, "校园卡余额已达上限。");
                }
                updateBalance(connection, current.getUserId(), next);
                insertLedger(connection, session, merchant, reference, entryType, amountFen);
                connection.commit();
                return new CampusCardView(
                        current.getUserId(), current.getUsername(), current.getCardNo(), next);
            } catch (CardBusinessException exception) {
                rollback(connection);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot update campus card.", exception);
        }
    }

    private CampusCardView ensureCard(Connection connection, SessionInfo session) throws SQLException {
        CampusCardView existing = findCard(connection, session.getUserId());
        if (existing != null) {
            return existing;
        }
        CampusCardView created = new CampusCardView(
                session.getUserId(), session.getUsername(), session.getUsername(), 0);
        insertCard(connection, created);
        return created;
    }

    private CampusCardView findCard(Connection connection, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tblCampusCard WHERE userId = ?")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new CampusCardView(
                        result.getString("userId"),
                        result.getString("username"),
                        result.getString("cardNo"),
                        result.getInt("balanceFen"));
            }
        }
    }

    private CampusCardLedgerEntry findLedger(
            Connection connection,
            String userId,
            String merchant,
            String reference,
            String entryType) throws SQLException {
        String sql = "SELECT * FROM tblCampusCardLedger WHERE userId = ? AND merchant = ? "
                + "AND reference = ? AND entryType = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, merchant);
            statement.setString(3, reference);
            statement.setString(4, entryType);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return readLedger(result);
            }
        }
    }

    private void insertCard(Connection connection, CampusCardView card) throws SQLException {
        String sql = "INSERT INTO tblCampusCard (userId, username, cardNo, balanceFen, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, card.getUserId());
            statement.setString(2, card.getUsername());
            statement.setString(3, card.getCardNo());
            statement.setInt(4, card.getBalanceFen());
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private void updateBalance(Connection connection, String userId, int balanceFen) throws SQLException {
        String sql = "UPDATE tblCampusCard SET balanceFen = ?, updatedAt = ? WHERE userId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, balanceFen);
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(3, userId);
            statement.executeUpdate();
        }
    }

    private void insertLedger(
            Connection connection,
            SessionInfo session,
            String merchant,
            String reference,
            String entryType,
            int amountFen) throws SQLException {
        String sql = "INSERT INTO tblCampusCardLedger "
                + "(txnId, userId, merchant, reference, entryType, amountFen, createdAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, session.getUserId());
            statement.setString(3, merchant);
            statement.setString(4, reference);
            statement.setString(5, entryType);
            statement.setInt(6, amountFen);
            statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private CampusCardLedgerEntry readLedger(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("createdAt");
        String display = createdAt == null
                ? LocalDateTime.now().format(TIME)
                : createdAt.toLocalDateTime().format(TIME);
        return new CampusCardLedgerEntry(
                result.getString("txnId"),
                result.getString("userId"),
                result.getString("merchant"),
                result.getString("reference"),
                result.getString("entryType"),
                result.getInt("amountFen"),
                display);
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection()) {
            if (!tableExists(connection, CARD_TABLE)) {
                execute(connection, "CREATE TABLE tblCampusCard ("
                        + "userId TEXT(36) PRIMARY KEY, "
                        + "username TEXT(50) NOT NULL, "
                        + "cardNo TEXT(50) NOT NULL, "
                        + "balanceFen LONG NOT NULL, "
                        + "updatedAt DATETIME)");
            }
            if (!tableExists(connection, LEDGER_TABLE)) {
                execute(connection, "CREATE TABLE tblCampusCardLedger ("
                        + "txnId TEXT(36) PRIMARY KEY, "
                        + "userId TEXT(36) NOT NULL, "
                        + "merchant TEXT(20) NOT NULL, "
                        + "reference TEXT(80) NOT NULL, "
                        + "entryType TEXT(20) NOT NULL, "
                        + "amountFen LONG NOT NULL, "
                        + "createdAt DATETIME)");
                execute(connection, "CREATE UNIQUE INDEX ux_tblCampusCardLedger_merchant_ref_type "
                        + "ON tblCampusCardLedger (merchant, reference, entryType)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize campus-card schema.", exception);
        }
    }

    private void seedDemoCardsIfEmpty() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM tblCampusCard")) {
            if (result.next() && result.getInt(1) > 0) {
                return;
            }
        } catch (SQLException exception) {
            throw failure("Cannot count campus cards.", exception);
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                insertCard(connection, new CampusCardView(
                        "U-STUDENT-001", "20260001", "20260001", DEMO_BALANCE_FEN));
                insertCard(connection, new CampusCardView(
                        "U-SHOP-ADMIN-001", "20260006", "20260006", DEMO_BALANCE_FEN));
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot seed campus cards.", exception);
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

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original exception is rethrown by the caller.
        }
    }

    private static IllegalStateException failure(String message, SQLException exception) {
        return new IllegalStateException(message, exception);
    }
}
