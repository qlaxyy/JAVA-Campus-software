package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Access-backed catalog metadata repository. Inventory remains derived from copies. */
final class AccessBookRepository implements BookRepository {

    private static final String SELECT_BOOKS = "SELECT b.*, c.categoryName "
            + "FROM tblBook b INNER JOIN tblBookCategory c "
            + "ON b.categoryId = c.categoryId";

    private final AccessLibraryStore store;

    AccessBookRepository(AccessLibraryStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public List<BookDTO> search(String keyword) {
        return filter(keyword, true);
    }

    @Override
    public List<BookDTO> searchAll(String keyword) {
        return filter(keyword, false);
    }

    @Override
    public Optional<BookDTO> findById(String bookId) {
        return findOne(" WHERE b.bookId = ? AND b.[status] = 'ACTIVE'", bookId);
    }

    @Override
    public Optional<BookDTO> findIncludingInactive(String bookId) {
        return findOne(" WHERE b.bookId = ?", bookId);
    }

    @Override
    public Optional<BookDTO> findByIsbn(String isbn) {
        return findOne(" WHERE b.isbn = ?", isbn);
    }

    @Override
    public void insert(BookDTO book) {
        validate(book);
        store.write("Cannot insert library book.", connection -> {
            String sql = "INSERT INTO tblBook (bookId, isbn, title, author, categoryId, "
                    + "publisher, publicationYear, language, [status]) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindBook(statement, book, false);
                requireOne(statement.executeUpdate(), "Book was not inserted.");
            }
        });
    }

    @Override
    public void update(BookDTO book) {
        validate(book);
        store.write("Cannot update library book.", connection -> {
            String sql = "UPDATE tblBook SET isbn = ?, title = ?, author = ?, "
                    + "categoryId = ?, publisher = ?, publicationYear = ?, language = ?, "
                    + "[status] = ? WHERE bookId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindBook(statement, book, true);
                requireOne(statement.executeUpdate(), "Book does not exist.");
            }
        });
    }

    private List<BookDTO> filter(String rawKeyword, boolean activeOnly) {
        String keyword = Objects.requireNonNull(rawKeyword, "keyword must not be null")
                .toLowerCase(Locale.ROOT);
        return loadAll().stream()
                .filter(book -> !activeOnly || "ACTIVE".equals(book.getStatus()))
                .filter(book -> contains(book.getTitle(), keyword)
                        || contains(book.getAuthor(), keyword)
                        || contains(book.getIsbn(), keyword)
                        || contains(book.getCategoryName(), keyword))
                .toList();
    }

    private List<BookDTO> loadAll() {
        return store.read("Cannot list library books.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_BOOKS + " ORDER BY b.bookId");
                 ResultSet result = statement.executeQuery()) {
                List<BookDTO> books = new ArrayList<>();
                while (result.next()) {
                    books.add(readBook(result));
                }
                return books;
            }
        });
    }

    private Optional<BookDTO> findOne(String condition, String value) {
        return store.read("Cannot read library book.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_BOOKS + condition)) {
                statement.setString(1, value);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(readBook(result)) : Optional.empty();
                }
            }
        });
    }

    private BookDTO readBook(ResultSet result) throws SQLException {
        int publicationYear = result.getInt("publicationYear");
        Integer year = result.wasNull() ? null : publicationYear;
        return new BookDTO(
                result.getString("bookId"),
                result.getString("isbn"),
                result.getString("title"),
                result.getString("author"),
                result.getString("categoryId"),
                result.getString("categoryName"),
                emptyIfNull(result.getString("publisher")),
                year,
                emptyIfNull(result.getString("language")),
                result.getString("status"),
                List.of());
    }

    private void bindBook(PreparedStatement statement, BookDTO book, boolean update)
            throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, book.getBookId());
        }
        statement.setString(index++, book.getIsbn());
        statement.setString(index++, book.getTitle());
        statement.setString(index++, book.getAuthor());
        statement.setString(index++, book.getCategoryId());
        setOptionalText(statement, index++, book.getPublisher());
        if (book.getPublicationYear() == null) {
            statement.setNull(index++, Types.INTEGER);
        } else {
            statement.setInt(index++, book.getPublicationYear());
        }
        setOptionalText(statement, index++, book.getLanguage());
        statement.setString(index++, book.getStatus());
        if (update) {
            statement.setString(index, book.getBookId());
        }
    }

    private void setOptionalText(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private void validate(BookDTO book) {
        Objects.requireNonNull(book, "book must not be null");
        if (book.getBookId() == null || book.getBookId().isBlank()
                || book.getIsbn() == null || book.getIsbn().isBlank()
                || book.getTitle() == null || book.getTitle().isBlank()
                || book.getAuthor() == null || book.getAuthor().isBlank()
                || book.getCategoryId() == null || book.getCategoryId().isBlank()
                || !("ACTIVE".equals(book.getStatus())
                || "INACTIVE".equals(book.getStatus()))) {
            throw new IllegalArgumentException("Invalid book snapshot.");
        }
    }

    private boolean contains(String value, String keyword) {
        return value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private void requireOne(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
