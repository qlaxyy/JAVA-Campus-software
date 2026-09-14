package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookCategoryDTO;
import java.util.List;
import java.util.Optional;

/** Data boundary for the reusable library classification dictionary. */
interface BookCategoryRepository {
    List<BookCategoryDTO> findAll();
    Optional<BookCategoryDTO> findById(String categoryId);
    void save(BookCategoryDTO category);
}
