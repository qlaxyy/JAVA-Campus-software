package edu.seu.vcampus.server.infrastructure.database;

import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseAuditTrailTest {
    @TempDir Path directory;

    private AccessDatabase database() throws Exception {
        AccessDatabase database = new AccessDatabase(directory.resolve("audit.accdb"));
        try (var connection = database.openConnection(); var sql = connection.createStatement()) {
            sql.executeUpdate("CREATE TABLE tblExample (id LONG PRIMARY KEY, note TEXT(100))");
        }
        return database;
    }

    @Test void journalsAutoCommitWritesWithIdentityButWithoutPayload() throws Exception {
        var database = database();
        var actor = actor("20260006");
        var request = Request.create("STUDENT.UPDATE_PROFILE", actor.getToken(), null);
        try (var context = DatabaseAuditContext.open(request, actor);
             var connection = database.openConnection();
             var insert = connection.prepareStatement("INSERT INTO tblExample VALUES (?, ?)")) {
            insert.setInt(1, 1);
            insert.setString(2, "PASSWORD-AND-HEALTH-SECRET");
            assertSame(connection, insert.getConnection());
            assertEquals(1, insert.executeUpdate());
            assertTrue(connection.getAutoCommit());
        }
        var entries = DatabaseAuditTrail.findAll(new AccessDatabase(database.path()));
        var write = entries.stream().filter(e -> e.getActionCode().equals("DATABASE.INSERT")).findFirst().orElseThrow();
        assertEquals(actor.getUserId(), write.getActorUserId());
        assertEquals(actor.getUsername(), write.getActorUsername());
        assertTrue(write.getDetail().contains(request.getAction()));
        assertTrue(write.getDetail().contains(request.getRequestId()));
        assertFalse(write.getDetail().contains("SECRET"));
        assertEquals(2, entries.size()); // CREATE + INSERT; SELECT never journals itself.
        assertEquals(entries.size(), DatabaseAuditTrail.findAll(database).size());
    }

    @Test void failedJournalRollsBackBusinessWriteEvenIfCallerCatchesAndCommits() throws Exception {
        var database = database();
        for (boolean autoCommit : new boolean[]{true, false}) {
            try (Connection raw = java.sql.DriverManager.getConnection("jdbc:ucanaccess://" + database.path())) {
                raw.setAutoCommit(autoCommit);
                Connection failing = (Connection) java.lang.reflect.Proxy.newProxyInstance(
                        Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("prepareStatement")
                                    && ((String) args[0]).startsWith("INSERT INTO tblDatabaseAuditLog")) {
                                throw new SQLException("Journal write failed for test");
                            }
                            try { return method.invoke(raw, args); }
                            catch (java.lang.reflect.InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                        });
                try (Connection audited = DatabaseAuditTrail.wrap(failing);
                     Statement statement = audited.createStatement()) {
                    assertThrows(SQLException.class,
                            () -> statement.executeUpdate("INSERT INTO tblExample VALUES (1, 'must roll back')"));
                    if (!autoCommit) { audited.commit(); }
                }
            }
            try (Connection connection = database.openConnection(); Statement statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT COUNT(*) FROM tblExample")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        }
        assertEquals(1, DatabaseAuditTrail.findAll(database).size()); // Only initial CREATE remains.
    }

    @Test void commitRollbackAndSavepointKeepJournalInSameTransaction() throws Exception {
        var database = database();
        try (var connection = database.openConnection(); var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.executeUpdate("INSERT INTO tblExample VALUES (1, 'kept')");
            var point = connection.setSavepoint();
            statement.executeUpdate("INSERT INTO tblExample VALUES (2, 'rolled back to savepoint')");
            connection.rollback(point);
            connection.commit();
            statement.executeUpdate("UPDATE tblExample SET note = 'rolled back' WHERE id = 1");
            connection.rollback();
        }
        var entries = DatabaseAuditTrail.findAll(database);
        assertEquals(1, entries.stream().filter(e -> e.getActionCode().equals("DATABASE.INSERT")).count());
        assertTrue(entries.stream().noneMatch(e -> e.getActionCode().equals("DATABASE.UPDATE")));
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM tblExample")) {
            assertTrue(rows.next());
            assertEquals("kept", rows.getString("note"));
            assertFalse(rows.next());
        }
    }

    @Test void batchAndNoOpUpdatesHaveAccurateJournalCounts() throws Exception {
        var database = database();
        try (var connection = database.openConnection();
             var insert = connection.prepareStatement("INSERT INTO tblExample VALUES (?, ?)");
             var statement = connection.createStatement()) {
            for (int number = 1; number <= 3; number++) {
                insert.setInt(1, number); insert.setString(2, "demo"); insert.addBatch();
            }
            assertEquals(3, insert.executeBatch().length);
            statement.addBatch("UPDATE tblExample SET note = 'updated' WHERE id = 1");
            statement.addBatch("DELETE FROM tblExample WHERE id = 3");
            assertEquals(2, statement.executeBatch().length);
            assertEquals(0, statement.executeUpdate("UPDATE tblExample SET note = 'none' WHERE id = 999"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO tblExample VALUES (1, 'duplicate')"));
        }
        var entries = DatabaseAuditTrail.findAll(database);
        assertEquals(3, entries.stream().filter(e -> e.getActionCode().equals("DATABASE.INSERT")).count());
        assertEquals(1, entries.stream().filter(e -> e.getActionCode().equals("DATABASE.UPDATE")).count());
        assertEquals(1, entries.stream().filter(e -> e.getActionCode().equals("DATABASE.DELETE")).count());
    }

    @Test void requestIdentityIsIsolatedAcrossThreadsAndRemovedAfterScope() throws Exception {
        var database = database();
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(() -> write(database, 1, actor("20260006")));
            var second = threads.submit(() -> write(database, 2, actor("20260007")));
            first.get(); second.get();
        }
        write(database, 3, null);
        var entries = DatabaseAuditTrail.findAll(database).stream()
                .filter(e -> e.getActionCode().equals("DATABASE.INSERT")).toList();
        assertEquals(3, entries.size());
        assertEquals(java.util.Set.of("20260006", "20260007", "ANONYMOUS"),
                entries.stream().map(e -> e.getActorUsername()).collect(java.util.stream.Collectors.toSet()));
        assertEquals("SYSTEM", DatabaseAuditContext.current().number());
    }

    private void write(AccessDatabase database, int id, SessionInfo actor) {
        try (var context = DatabaseAuditContext.open(Request.create("SHOP.SET_CART_QUANTITY", null, null), actor);
             var connection = database.openConnection();
             var insert = connection.prepareStatement("INSERT INTO tblExample VALUES (?, 'demo')")) {
            insert.setInt(1, id); insert.executeUpdate();
        } catch (Exception exception) { throw new RuntimeException(exception); }
    }

    private SessionInfo actor(String number) {
        return new SessionInfo("token-" + number, "U-" + number, number, "测试用户", Role.USER);
    }
}
