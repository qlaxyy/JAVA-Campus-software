package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Immutable snapshots prevent readers from seeing partially edited metadata or stock. */
final class InMemoryBookRepository implements BookRepository {
    private final Map<String, BookDTO> books = new LinkedHashMap<>();

    InMemoryBookRepository() {
        insert(book("B001", "9787111213826", "Java编程思想", "Bruce Eckel",
                "C001", "计算机", "机械工业出版社", 2007, 5));
        insert(book("B002", "9787115428028", "深入理解Java虚拟机", "周志明",
                "C001", "计算机", "人民邮电出版社", 2019, 4));
        insert(book("B003", "9787302511854", "数据结构（Java语言描述）", "徐孝凯",
                "C001", "计算机", "清华大学出版社", 2018, 3));
        insert(book("B004", "9787020002207", "红楼梦", "曹雪芹",
                "C002", "文学", "人民文学出版社", 2008, 6));
        insert(book("B005", "9787101003048", "史记", "司马迁",
                "C003", "历史", "中华书局", 2014, 2));
    }

    public synchronized List<BookDTO> search(String rawKeyword) {
        String keyword = rawKeyword.toLowerCase(Locale.ROOT);
        return books.values().stream().filter(this::isActive).filter(book ->
                contains(book.getTitle(), keyword) || contains(book.getAuthor(), keyword)
                        || contains(book.getIsbn(), keyword) || contains(book.getCategoryName(), keyword))
                .toList();
    }

    @Override
    public synchronized List<BookDTO> searchAll(String rawKeyword) {
        String keyword = rawKeyword.toLowerCase(Locale.ROOT);
        return books.values().stream().filter(book ->
                contains(book.getTitle(), keyword) || contains(book.getAuthor(), keyword)
                        || contains(book.getIsbn(), keyword) || contains(book.getCategoryName(), keyword))
                .toList();
    }

    public synchronized Optional<BookDTO> findById(String bookId) {
        return Optional.ofNullable(books.get(bookId)).filter(this::isActive);
    }

    public synchronized Optional<BookDTO> findIncludingInactive(String bookId) {
        return Optional.ofNullable(books.get(bookId));
    }

    public synchronized Optional<BookDTO> findByIsbn(String isbn) {
        return books.values().stream().filter(book -> book.getIsbn().equals(isbn)).findFirst();
    }

    public synchronized void insert(BookDTO book) {
        validate(book);
        if (books.containsKey(book.getBookId()) || findByIsbn(book.getIsbn()).isPresent()) {
            throw new IllegalStateException("Book identifier or ISBN already exists.");
        }
        books.put(book.getBookId(), book);
    }

    public synchronized void update(BookDTO book) {
        validate(book);
        if (findIncludingInactive(book.getBookId()).isEmpty() || findByIsbn(book.getIsbn())
                .filter(existing -> !existing.getBookId().equals(book.getBookId())).isPresent()) {
            throw new IllegalStateException("Book is missing or ISBN already exists.");
        }
        books.put(book.getBookId(), book);
    }

    private void validate(BookDTO book) {
        if (book.getBookId() == null || book.getBookId().isBlank()
                || book.getIsbn() == null || book.getIsbn().isBlank()
                || book.getTotalCount() < 0 || book.getAvailableCount() < 0
                || book.getAvailableCount() > book.getTotalCount()) {
            throw new IllegalArgumentException("Invalid book snapshot.");
        }
    }

    private boolean contains(String value, String keyword) {
        return value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isActive(BookDTO book) {
        return "ACTIVE".equals(book.getStatus());
    }

    private static BookDTO book(String id, String isbn, String title, String author,
            String categoryId, String categoryName, String publisher, int year,
            int total) {
        return new BookDTO(id, isbn, title, author, categoryId, categoryName,
                publisher, year, "中文", "ACTIVE",
                List.of(new edu.seu.vcampus.common.library.BookLocationDTO(
                        "未指定馆藏地", total, total)));
    }
}
