package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBookRepositoryTest {

    private final InMemoryBookRepository repository = new InMemoryBookRepository();

    @Test
    void searchMatchesAuthorWithoutCaseSensitivity() {
        List<BookDTO> books = repository.search("bruce eckel");

        assertEquals(1, books.size());
        assertEquals("Java编程思想", books.getFirst().getTitle());
    }

    @Test
    void searchMatchesCategory() {
        List<BookDTO> books = repository.search("文学");

        assertEquals(1, books.size());
        assertEquals("红楼梦", books.getFirst().getTitle());
    }

    @Test
    void searchReturnsEmptyListWhenNothingMatches() {
        assertTrue(repository.search("不存在的图书").isEmpty());
    }
}
