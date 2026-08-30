package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.List;
import java.util.Locale;

/** Deterministic fake book data used before the Access repository is available. */
final class InMemoryBookRepository implements BookRepository {

    private static final List<BookDTO> BOOKS = List.of(
            new BookDTO("B001", "9787111213826", "Java编程思想", "Bruce Eckel", "计算机", 5, 2),
            new BookDTO("B002", "9787115428028", "深入理解Java虚拟机", "周志明", "计算机", 4, 1),
            new BookDTO("B003", "9787302511854", "数据结构（Java语言描述）", "徐孝凯", "计算机", 3, 3),
            new BookDTO("B004", "9787020002207", "红楼梦", "曹雪芹", "文学", 6, 0),
            new BookDTO("B005", "9787101003048", "史记", "司马迁", "历史", 2, 1));

    @Override
    public List<BookDTO> search(String rawKeyword) {
        String keyword = rawKeyword.toLowerCase(Locale.ROOT);
        return BOOKS.stream()
                .filter(book -> matches(book, keyword))
                .toList();
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
}
