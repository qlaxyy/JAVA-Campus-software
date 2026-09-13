package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookCategoryDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Server-owned V1 category dictionary, later replaceable with the category table. */
final class InMemoryBookCategoryRepository implements BookCategoryRepository {
    private final List<BookCategoryDTO> categories = new ArrayList<>(List.of(
            new BookCategoryDTO("C001", "计算机"),
            new BookCategoryDTO("C002", "文学"),
            new BookCategoryDTO("C003", "历史")));

    public synchronized List<BookCategoryDTO> findAll() { return List.copyOf(categories); }
    public synchronized Optional<BookCategoryDTO> findById(String id) {
        return categories.stream().filter(category -> category.getCategoryId().equals(id)).findFirst();
    }

    public synchronized void save(BookCategoryDTO category) {
        if (findById(category.getCategoryId()).isPresent()) {
            throw new IllegalStateException("Category identifier already exists.");
        }
        categories.add(category);
    }
}
