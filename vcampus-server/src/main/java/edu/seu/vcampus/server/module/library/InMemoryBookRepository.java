package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Deterministic fake book data used before the Access repository is available. */
final class InMemoryBookRepository implements BookRepository {

    private final Map<String, BookStock> books = createBooks();

    @Override
    public synchronized List<BookDTO> search(String rawKeyword) {
        String keyword = rawKeyword.toLowerCase(Locale.ROOT);
        return books.values().stream()
                .map(BookStock::toDto)
                .filter(book -> matches(book, keyword))
                .toList();
    }

    @Override
    public synchronized Optional<BookDTO> findById(String bookId) {
        BookStock stock = books.get(bookId);
        return stock == null ? Optional.empty() : Optional.of(stock.toDto());
    }

    @Override
    public synchronized boolean decrementAvailableCount(String bookId) {
        BookStock stock = books.get(bookId);
        if (stock == null || stock.availableCount == 0) {
            return false;
        }
        stock.availableCount--;
        return true;
    }

    @Override
    public synchronized void incrementAvailableCount(String bookId) {
        BookStock stock = books.get(bookId);
        if (stock == null || stock.availableCount >= stock.totalCount) {
            throw new IllegalStateException("Cannot restore library stock.");
        }
        stock.availableCount++;
    }

    private boolean matches(BookDTO book, String keyword) {
        return contains(book.getTitle(), keyword)
                || contains(book.getAuthor(), keyword)
                || contains(book.getIsbn(), keyword)
                || contains(book.getCategory(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static Map<String, BookStock> createBooks() {
        Map<String, BookStock> result = new LinkedHashMap<>();
        add(result, "B001", "9787111213826", "Java编程思想", "Bruce Eckel", "计算机", 5, 2);
        add(result, "B002", "9787115428028", "深入理解Java虚拟机", "周志明", "计算机", 4, 1);
        add(result, "B003", "9787302511854", "数据结构（Java语言描述）", "徐孝凯", "计算机", 3, 3);
        add(result, "B004", "9787020002207", "红楼梦", "曹雪芹", "文学", 6, 0);
        add(result, "B005", "9787101003048", "史记", "司马迁", "历史", 2, 1);
        return result;
    }

    private static void add(
            Map<String, BookStock> books,
            String bookId,
            String isbn,
            String title,
            String author,
            String category,
            int totalCount,
            int availableCount) {
        books.put(bookId, new BookStock(
                bookId, isbn, title, author, category, totalCount, availableCount));
    }

    private static final class BookStock {

        private final String bookId;
        private final String isbn;
        private final String title;
        private final String author;
        private final String category;
        private final int totalCount;
        private int availableCount;

        private BookStock(
                String bookId,
                String isbn,
                String title,
                String author,
                String category,
                int totalCount,
                int availableCount) {
            this.bookId = bookId;
            this.isbn = isbn;
            this.title = title;
            this.author = author;
            this.category = category;
            this.totalCount = totalCount;
            this.availableCount = availableCount;
        }

        private BookDTO toDto() {
            return new BookDTO(
                    bookId, isbn, title, author, category, totalCount, availableCount);
        }
    }
}
