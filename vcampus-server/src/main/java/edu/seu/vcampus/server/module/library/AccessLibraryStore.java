package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

/**
 * Owns the Access schema and binds all repositories to one connection during a transaction.
 */
final class AccessLibraryStore implements LibraryTransactionManager {

    private static final String BOOK_TABLE = "tblBook";
    private static final String COPY_TABLE = "tblBookCopy";
    private static final String CATEGORY_TABLE = "tblBookCategory";
    private static final String BORROW_TABLE = "tblBorrowRecord";
    private static final String RESERVATION_TABLE = "tblReservation";

    private final AccessDatabase database;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private final boolean newlyCreatedLibrarySchema;

    AccessLibraryStore(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        newlyCreatedLibrarySchema = initializeSchema();
        seedDemonstrationCatalog();
    }

    boolean isNewlyCreatedLibrarySchema() {
        return newlyCreatedLibrarySchema;
    }

    @Override
    public void execute(Runnable operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        if (transactionConnection.get() != null) {
            operation.run();
            return;
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                operation.run();
                connection.commit();
            } catch (RuntimeException | Error exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw failure("Cannot commit library transaction.", exception);
            } finally {
                transactionConnection.remove();
            }
        } catch (SQLException exception) {
            throw failure("Cannot complete library transaction.", exception);
        }
    }

    <T> T read(String operation, SqlFunction<T> work) {
        Connection current = transactionConnection.get();
        if (current != null) {
            return apply(operation, current, work);
        }
        try (Connection connection = database.openConnection()) {
            return apply(operation, connection, work);
        } catch (SQLException exception) {
            throw failure(operation, exception);
        }
    }

    void write(String operation, SqlConsumer work) {
        read(operation, connection -> {
            work.accept(connection);
            return null;
        });
    }

    private <T> T apply(String operation, Connection connection, SqlFunction<T> work) {
        try {
            return work.apply(connection);
        } catch (SQLException exception) {
            throw failure(operation, exception);
        }
    }

    private boolean initializeSchema() {
        try (Connection connection = database.openConnection()) {
            boolean categoryExists = tableExists(connection, CATEGORY_TABLE);
            boolean bookExists = tableExists(connection, BOOK_TABLE);
            boolean copyExists = tableExists(connection, COPY_TABLE);
            boolean borrowExists = tableExists(connection, BORROW_TABLE);
            boolean reservationExists = tableExists(connection, RESERVATION_TABLE);
            boolean newlyCreated = !(categoryExists || bookExists || copyExists
                    || borrowExists || reservationExists);
            if (!categoryExists) {
                executeSql(connection, "CREATE TABLE tblBookCategory ("
                        + "categoryId TEXT(20) PRIMARY KEY, "
                        + "categoryName TEXT(50) NOT NULL)");
            }
            if (!bookExists) {
                executeSql(connection, "CREATE TABLE tblBook ("
                        + "bookId TEXT(20) PRIMARY KEY, "
                        + "isbn TEXT(20) NOT NULL, "
                        + "title TEXT(200) NOT NULL, "
                        + "author TEXT(100) NOT NULL, "
                        + "categoryId TEXT(20) NOT NULL, "
                        + "publisher TEXT(100), "
                        + "publicationYear LONG, "
                        + "language TEXT(30), "
                        + "[status] TEXT(20) NOT NULL, "
                        + "CONSTRAINT fk_tblBook_category FOREIGN KEY (categoryId) "
                        + "REFERENCES tblBookCategory (categoryId))");
                executeSql(connection,
                        "CREATE UNIQUE INDEX ux_tblBook_isbn ON tblBook (isbn)");
                executeSql(connection,
                        "CREATE INDEX ix_tblBook_title ON tblBook (title)");
                executeSql(connection,
                        "CREATE INDEX ix_tblBook_author ON tblBook (author)");
                executeSql(connection,
                        "CREATE INDEX ix_tblBook_category ON tblBook (categoryId)");
            }
            if (!copyExists) {
                executeSql(connection, "CREATE TABLE tblBookCopy ("
                        + "copyId TEXT(36) PRIMARY KEY, "
                        + "barcode TEXT(50) NOT NULL, "
                        + "bookId TEXT(20) NOT NULL, "
                        + "location TEXT(100) NOT NULL, "
                        + "callNumber TEXT(100) NOT NULL, "
                        + "[status] TEXT(30) NOT NULL, "
                        + "CONSTRAINT fk_tblBookCopy_book FOREIGN KEY (bookId) "
                        + "REFERENCES tblBook (bookId))");
                executeSql(connection,
                        "CREATE UNIQUE INDEX ux_tblBookCopy_barcode ON tblBookCopy (barcode)");
                executeSql(connection, "CREATE INDEX ix_tblBookCopy_book_status "
                        + "ON tblBookCopy (bookId, [status])");
            }
            if (!borrowExists) {
                executeSql(connection, "CREATE TABLE tblBorrowRecord ("
                        + "recordId TEXT(36) PRIMARY KEY, "
                        + "userId TEXT(36) NOT NULL, "
                        + "copyId TEXT(36) NOT NULL, "
                        + "borrowTime DATETIME NOT NULL, "
                        + "dueTime DATETIME NOT NULL, "
                        + "returnTime DATETIME, "
                        + "[status] TEXT(20) NOT NULL, "
                        + "CONSTRAINT fk_tblBorrowRecord_copy FOREIGN KEY (copyId) "
                        + "REFERENCES tblBookCopy (copyId))");
                executeSql(connection, "CREATE INDEX ix_tblBorrowRecord_user_status "
                        + "ON tblBorrowRecord (userId, [status])");
                executeSql(connection, "CREATE INDEX ix_tblBorrowRecord_copy_status "
                        + "ON tblBorrowRecord (copyId, [status])");
            }
            if (!reservationExists) {
                executeSql(connection, "CREATE TABLE tblReservation ("
                        + "reservationId TEXT(36) PRIMARY KEY, "
                        + "userId TEXT(36) NOT NULL, "
                        + "bookId TEXT(20) NOT NULL, "
                        + "pickupLocation TEXT(100) NOT NULL, "
                        + "assignedCopyId TEXT(36), "
                        + "createdAt DATETIME NOT NULL, "
                        + "readyAt DATETIME, "
                        + "expiresAt DATETIME, "
                        + "closedAt DATETIME, "
                        + "[status] TEXT(30) NOT NULL, "
                        + "CONSTRAINT fk_tblReservation_book FOREIGN KEY (bookId) "
                        + "REFERENCES tblBook (bookId), "
                        + "CONSTRAINT fk_tblReservation_copy FOREIGN KEY (assignedCopyId) "
                        + "REFERENCES tblBookCopy (copyId))");
                executeSql(connection, "CREATE INDEX ix_tblReservation_user_status "
                        + "ON tblReservation (userId, [status])");
                executeSql(connection, "CREATE INDEX ix_tblReservation_queue "
                        + "ON tblReservation (bookId, pickupLocation, [status], "
                        + "createdAt, reservationId)");
                executeSql(connection, "CREATE INDEX ix_tblReservation_copy_status "
                        + "ON tblReservation (assignedCopyId, [status])");
            }
            return newlyCreated;
        } catch (SQLException exception) {
            throw failure("Cannot initialize Access library schema.", exception);
        }
    }

    private void seedDemonstrationCatalog() {
        execute(() -> {
            seedCategories();
            if (rowCount(BOOK_TABLE) != 0) {
                return;
            }
            List<SeedBook> books = List.of(
                    new SeedBook("B001", "9787111213826", "Java编程思想", "Bruce Eckel",
                            "C001", "机械工业出版社", 2007, 5),
                    new SeedBook("B002", "9787115428028", "深入理解Java虚拟机", "周志明",
                            "C001", "人民邮电出版社", 2019, 4),
                    new SeedBook("B003", "9787302511854", "数据结构（Java语言描述）", "徐孝凯",
                            "C001", "清华大学出版社", 2018, 3),
                    new SeedBook("B004", "9787020002207", "红楼梦", "曹雪芹",
                            "C002", "人民文学出版社", 2008, 6),
                    new SeedBook("B005", "9787101003048", "史记", "司马迁",
                            "C003", "中华书局", 2014, 2));
            for (SeedBook book : books) {
                insertSeedBook(book);
                insertSeedCopies(book);
            }
        });
    }

    private void seedCategories() {
        insertCategoryIfMissing("C001", "计算机");
        insertCategoryIfMissing("C002", "文学");
        insertCategoryIfMissing("C003", "历史");
    }

    private void insertCategoryIfMissing(String id, String name) {
        if (read("Cannot inspect library category.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT categoryId FROM tblBookCategory WHERE categoryId = ?")) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        })) {
            return;
        }
        write("Cannot seed library category.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tblBookCategory (categoryId, categoryName) VALUES (?, ?)")) {
                statement.setString(1, id);
                statement.setString(2, name);
                statement.executeUpdate();
            }
        });
    }

    private int rowCount(String table) {
        return read("Cannot count " + table + ".", connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                return result.getInt(1);
            }
        });
    }

    private void insertSeedBook(SeedBook book) {
        write("Cannot seed library book.", connection -> {
            String sql = "INSERT INTO tblBook (bookId, isbn, title, author, categoryId, "
                    + "publisher, publicationYear, language, [status]) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, book.id());
                statement.setString(2, book.isbn());
                statement.setString(3, book.title());
                statement.setString(4, book.author());
                statement.setString(5, book.categoryId());
                statement.setString(6, book.publisher());
                statement.setInt(7, book.publicationYear());
                statement.setString(8, "中文");
                statement.setString(9, "ACTIVE");
                statement.executeUpdate();
            }
        });
    }

    private void insertSeedCopies(SeedBook book) {
        String firstLocation = "九龙湖校区—中文图书阅览室3";
        String secondLocation = "四牌楼校区—中文书库二楼";
        for (int index = 1; index <= book.totalCount(); index++) {
            int copyNumber = index;
            write("Cannot seed physical book copy.", connection -> {
                String suffix = String.format("%03d", copyNumber);
                String location = copyNumber <= Math.max(1, (book.totalCount() + 1) / 2)
                        ? firstLocation : secondLocation;
                String sql = "INSERT INTO tblBookCopy "
                        + "(copyId, barcode, bookId, location, callNumber, [status]) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, "CP-" + book.id() + "-" + suffix);
                    statement.setString(2, "SEU-" + book.id() + "-" + suffix);
                    statement.setString(3, book.id());
                    statement.setString(4, location);
                    statement.setString(5, book.categoryId() + "/" + book.id());
                    statement.setString(6, BookCopyStatus.AVAILABLE.name());
                    statement.executeUpdate();
                }
            });
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

    private void executeSql(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private LibraryPersistenceException failure(String operation, SQLException exception) {
        return new LibraryPersistenceException(
                operation + " Database: " + database.path(), exception);
    }

    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    private record SeedBook(String id, String isbn, String title, String author,
                            String categoryId, String publisher, int publicationYear,
                            int totalCount) {
    }
}
