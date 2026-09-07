package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookCategoryDTO;
import java.util.List;
import java.util.Optional;

/** Read-only category data boundary; category administration is outside this increment. */
interface BookCategoryRepository {
    List<BookCategoryDTO> findAll();
    Optional<BookCategoryDTO> findById(String categoryId);
}
