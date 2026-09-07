package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookCategoryDTO;
import java.util.List;
import java.util.Optional;

/** Server-owned V1 category dictionary, later replaceable with the category table. */
final class InMemoryBookCategoryRepository implements BookCategoryRepository {
    private final List<BookCategoryDTO> categories = List.of(
            new BookCategoryDTO("C001", "计算机"),
            new BookCategoryDTO("C002", "文学"),
            new BookCategoryDTO("C003", "历史"));

    public List<BookCategoryDTO> findAll() { return categories; }
    public Optional<BookCategoryDTO> findById(String id) {
        return categories.stream().filter(category -> category.getCategoryId().equals(id)).findFirst();
    }
}
