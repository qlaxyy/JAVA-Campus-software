package edu.seu.vcampus.server.infrastructure.database;

import edu.seu.vcampus.common.user.UserAuditLogEntry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Audits writes through the shared JDBC boundary, in the SAME transaction as the write.
 * Only table names, operation types and counts are persisted; SQL and bound values are not.
 */
public final class DatabaseAuditTrail {
    private static final String TABLE = "tblDatabaseAuditLog";
    private static final Pattern WRITE = Pattern.compile(
            "(?is)^\\s*(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM|CREATE\\s+TABLE|ALTER\\s+TABLE|DROP\\s+TABLE)"
                    + "\\s+[\\[\"`]?([a-zA-Z_][a-zA-Z_0-9]*)");

    private DatabaseAuditTrail() { }

    /** Raw connection is used for journal writes to avoid recursive interception. */
    public static Connection wrap(Connection raw) throws SQLException {
        ensureSchema(raw);
        Connection[] proxy = new Connection[1];
        proxy[0] = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (ignored, method, arguments) -> {
                    Object value = invoke(raw, method, arguments);
                    if (value instanceof Statement statement) {
                        String template = arguments != null && arguments.length > 0
                                && arguments[0] instanceof String sql ? sql : null;
                        return wrapStatement(raw, proxy[0], statement, template);
                    }
                    return value;
                });
        return proxy[0];
    }

    private static synchronized void ensureSchema(Connection raw) throws SQLException {
        try (ResultSet tables = raw.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (TABLE.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return;
                }
            }
        }
        try (Statement statement = raw.createStatement()) {
            statement.executeUpdate("CREATE TABLE " + TABLE + " (auditId TEXT(36) PRIMARY KEY, "
                    + "occurredAt DATETIME NOT NULL, actorUserId TEXT(64) NOT NULL, "
                    + "actorUsername TEXT(50) NOT NULL, actorDisplayName TEXT(100) NOT NULL, "
                    + "actionCode TEXT(80) NOT NULL, targetText TEXT(120) NOT NULL, "
                    + "successful YESNO NOT NULL, detailText MEMO NOT NULL)");
            statement.executeUpdate("CREATE INDEX ix_databaseAudit_time ON " + TABLE + " (occurredAt)");
        }
    }

    private static Statement wrapStatement(
            Connection raw, Connection proxy, Statement statement, String template) {
        List<String> batch = new ArrayList<>();
        Class<?> type = statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(), new Class<?>[]{type},
                (ignored, method, arguments) -> {
                    String name = method.getName();
                    if ("getConnection".equals(name)) {
                        return proxy;
                    }
                    if ("addBatch".equals(name)) {
                        Object result = invoke(statement, method, arguments);
                        batch.add(template == null ? (String) arguments[0] : template);
                        return result;
                    }
                    if ("clearBatch".equals(name)) {
                        batch.clear();
                    }
                    boolean batched = name.equals("executeBatch") || name.equals("executeLargeBatch");
                    String sql = template != null ? template
                            : arguments != null && arguments.length > 0 && arguments[0] instanceof String text
                                    ? text : null;
                    boolean executing = name.equals("execute") || name.equals("executeUpdate")
                            || name.equals("executeLargeUpdate") || batched;
                    List<String> writes = batched ? new ArrayList<>(batch) : List.of(sql == null ? "" : sql);
                    if (!executing || writes.stream().noneMatch(text -> change(text) != null)) {
                        try {
                            return invoke(statement, method, arguments);
                        } finally {
                            if (batched) { batch.clear(); }
                        }
                    }
                    // DDL is driver-managed. DML in auto-commit mode is wrapped so a failed audit
                    // cannot commit an unrecorded business mutation.
                    boolean localTransaction = raw.getAutoCommit()
                            && writes.stream().filter(text -> change(text) != null)
                                    .allMatch(text -> change(text).isDml());
                    if (localTransaction) { raw.setAutoCommit(false); }
                    try {
                        Object result;
                        try {
                            result = invoke(statement, method, arguments);
                        } catch (BatchUpdateException exception) {
                            if (!localTransaction) {
                                try {
                                    recordBatch(raw, writes, exception.getLargeUpdateCounts());
                                } catch (SQLException auditFailure) {
                                    try { raw.rollback(); }
                                    catch (SQLException rollback) { auditFailure.addSuppressed(rollback); }
                                    auditFailure.addSuppressed(exception);
                                    throw auditFailure;
                                }
                            }
                            throw exception;
                        }
                        try {
                            if (batched) {
                                long[] counts = result instanceof long[] longs ? longs : toLongs((int[]) result);
                                recordBatch(raw, writes, counts);
                            } else {
                                long count = result instanceof Number number ? number.longValue()
                                        : statement.getUpdateCount();
                                record(raw, change(sql), count);
                            }
                        } catch (SQLException auditFailure) {
                            // Also fail closed for caller-owned transactions. A caller catching the
                            // exception must never be able to commit an unjournaled business write.
                            if (change(sql) == null || change(sql).isDml() || batched) {
                                try { raw.rollback(); } catch (SQLException rollback) { auditFailure.addSuppressed(rollback); }
                            }
                            throw auditFailure;
                        }
                        if (localTransaction) { raw.commit(); }
                        return result;
                    } catch (Throwable exception) {
                        if (localTransaction) {
                            try { raw.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
                        }
                        throw exception;
                    } finally {
                        if (batched) { batch.clear(); }
                        if (localTransaction) { raw.setAutoCommit(true); }
                    }
                });
    }

    private static void recordBatch(Connection raw, List<String> sql, long[] counts) throws SQLException {
        for (int index = 0; index < Math.min(sql.size(), counts.length); index++) {
            if (counts[index] != Statement.EXECUTE_FAILED) {
                record(raw, change(sql.get(index)), counts[index]);
            }
        }
    }

    private static long[] toLongs(int[] values) {
        long[] result = new long[values.length];
        for (int index = 0; index < values.length; index++) { result[index] = values[index]; }
        return result;
    }

    private static Change change(String sql) {
        if (sql == null) { return null; }
        var matcher = WRITE.matcher(sql);
        if (!matcher.find()) { return null; }
        String table = matcher.group(2);
        // Journal tables are append-only histories, not business data. Do not audit the audit.
        if (table.toLowerCase(Locale.ROOT).contains("audit")) { return null; }
        String operation = matcher.group(1).trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
        return new Change(operation, table);
    }

    private static void record(Connection raw, Change change, long count) throws SQLException {
        if (change == null || change.isDml() && count == 0) { return; }
        var actor = DatabaseAuditContext.current();
        String countText = change.isDml() ? (count < 0 ? "条数由驱动未提供" : "影响 " + count + " 条记录")
                : "数据库结构变更";
        try (PreparedStatement entry = raw.prepareStatement("INSERT INTO " + TABLE
                + " (auditId, occurredAt, actorUserId, actorUsername, actorDisplayName, actionCode, "
                + "targetText, successful, detailText) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            entry.setString(1, UUID.randomUUID().toString());
            entry.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            entry.setString(3, actor.userId());
            entry.setString(4, actor.number());
            entry.setString(5, actor.name());
            entry.setString(6, "DATABASE." + change.operation());
            entry.setString(7, change.table());
            entry.setBoolean(8, true);
            entry.setString(9, "业务动作：" + actor.action() + "；" + countText
                    + "；请求编号：" + actor.requestId());
            entry.executeUpdate();
        }
    }

    public static List<UserAuditLogEntry> findAll(AccessDatabase database) {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM " + TABLE
                     + " ORDER BY occurredAt DESC, auditId DESC")) {
            List<UserAuditLogEntry> entries = new ArrayList<>();
            while (rows.next()) {
                entries.add(new UserAuditLogEntry(rows.getString("auditId"),
                        rows.getTimestamp("occurredAt").getTime(), rows.getString("actorUserId"),
                        rows.getString("actorUsername"), rows.getString("actorDisplayName"),
                        rows.getString("actionCode"), rows.getString("targetText"),
                        rows.getBoolean("successful"), rows.getString("detailText")));
            }
            return List.copyOf(entries);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot read database audit trail.", exception);
        }
    }

    private static Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
        try {
            return method.invoke(object, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private record Change(String operation, String table) {
        boolean isDml() { return operation.equals("INSERT") || operation.equals("UPDATE") || operation.equals("DELETE"); }
    }
}
