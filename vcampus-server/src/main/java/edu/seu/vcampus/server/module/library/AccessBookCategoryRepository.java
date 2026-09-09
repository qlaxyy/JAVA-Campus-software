package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookCategoryDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Read-only Access category dictionary. */
final class AccessBookCategoryRepository implements BookCategoryRepository {

    private final AccessLibraryStore store;

    AccessBookCategoryRepository(AccessLibraryStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public List<BookCategoryDTO> findAll() {
        return store.read("Cannot list library categories.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT categoryId, categoryName FROM tblBookCategory ORDER BY categoryId");
                 ResultSet result = statement.executeQuery()) {
                List<BookCategoryDTO> categories = new ArrayList<>();
                while (result.next()) {
                    categories.add(readCategory(result));
                }
                return categories;
            }
        });
    }

    @Override
    public Optional<BookCategoryDTO> findById(String categoryId) {
        return store.read("Cannot read library category.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT categoryId, categoryName FROM tblBookCategory "
                            + "WHERE categoryId = ?")) {
                statement.setString(1, categoryId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            ? Optional.of(readCategory(result)) : Optional.empty();
                }
            }
        });
    }

    private BookCategoryDTO readCategory(ResultSet result) throws java.sql.SQLException {
        return new BookCategoryDTO(
                result.getString("categoryId"), result.getString("categoryName"));
    }
}
