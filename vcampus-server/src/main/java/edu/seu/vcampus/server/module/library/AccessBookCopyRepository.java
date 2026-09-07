package edu.seu.vcampus.server.module.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Access-backed repository for physical library copies. */
final class AccessBookCopyRepository implements BookCopyRepository {

    private static final String SELECT_COPY = "SELECT copyId, barcode, bookId, location, "
            + "callNumber, [status] FROM tblBookCopy";

    private final AccessLibraryStore store;

    AccessBookCopyRepository(AccessLibraryStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public List<BookCopy> findByBookId(String bookId) {
        return store.read("Cannot list physical book copies.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_COPY + " WHERE bookId = ? ORDER BY barcode")) {
                statement.setString(1, bookId);
                try (ResultSet result = statement.executeQuery()) {
                    List<BookCopy> copies = new ArrayList<>();
                    while (result.next()) {
                        copies.add(readCopy(result));
                    }
                    return copies;
                }
            }
        });
    }

    @Override
    public Optional<BookCopy> findById(String copyId) {
        return findOne("copyId", copyId);
    }

    @Override
    public Optional<BookCopy> findByBarcode(String barcode) {
        return findOne("barcode", barcode);
    }

    @Override
    public void insert(BookCopy copy) {
        Objects.requireNonNull(copy, "copy must not be null");
        store.write("Cannot insert physical book copy.", connection -> {
            String sql = "INSERT INTO tblBookCopy "
                    + "(copyId, barcode, bookId, location, callNumber, [status]) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindCopy(statement, copy, true);
                requireOne(statement.executeUpdate(), "Physical copy was not inserted.");
            }
        });
    }

    @Override
    public void update(BookCopy copy) {
        Objects.requireNonNull(copy, "copy must not be null");
        store.execute(() -> {
            BookCopy original = findById(copy.copyId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Physical book copy does not exist."));
            if (!original.bookId().equals(copy.bookId())
                    || !original.barcode().equals(copy.barcode())) {
                throw new IllegalArgumentException(
                        "Book-copy identity, barcode and parent book are immutable.");
            }
            store.write("Cannot update physical book copy.", connection -> {
                String sql = "UPDATE tblBookCopy SET location = ?, callNumber = ?, "
                        + "[status] = ? WHERE copyId = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, copy.location());
                    statement.setString(2, copy.callNumber());
                    statement.setString(3, copy.status().name());
                    statement.setString(4, copy.copyId());
                    requireOne(statement.executeUpdate(), "Physical copy does not exist.");
                }
            });
        });
    }

    @Override
    public void replaceForBook(String bookId, List<BookCopy> copies) {
        List<BookCopy> snapshot = List.copyOf(copies);
        if (snapshot.stream().anyMatch(copy -> !copy.bookId().equals(bookId))) {
            throw new IllegalArgumentException("Replacement contains a copy for another book.");
        }
        store.execute(() -> {
            store.write("Cannot replace physical book copies.", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM tblBookCopy WHERE bookId = ?")) {
                    statement.setString(1, bookId);
                    statement.executeUpdate();
                }
            });
            snapshot.forEach(this::insert);
        });
    }

    private Optional<BookCopy> findOne(String column, String value) {
        String condition;
        if ("copyId".equals(column)) {
            condition = " WHERE copyId = ?";
        } else if ("barcode".equals(column)) {
            condition = " WHERE barcode = ?";
        } else {
            throw new IllegalArgumentException("Unsupported copy lookup column.");
        }
        return store.read("Cannot read physical book copy.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_COPY + condition)) {
                statement.setString(1, value);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(readCopy(result)) : Optional.empty();
                }
            }
        });
    }

    private BookCopy readCopy(ResultSet result) throws java.sql.SQLException {
        return new BookCopy(
                result.getString("copyId"),
                result.getString("barcode"),
                result.getString("bookId"),
                result.getString("location"),
                result.getString("callNumber"),
                BookCopyStatus.valueOf(result.getString("status")));
    }

    private void bindCopy(PreparedStatement statement, BookCopy copy, boolean includeIdentity)
            throws java.sql.SQLException {
        int index = 1;
        if (includeIdentity) {
            statement.setString(index++, copy.copyId());
            statement.setString(index++, copy.barcode());
            statement.setString(index++, copy.bookId());
        }
        statement.setString(index++, copy.location());
        statement.setString(index++, copy.callNumber());
        statement.setString(index, copy.status().name());
    }

    private void requireOne(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
