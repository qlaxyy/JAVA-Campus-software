package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Serializable summary of one searchable library book. */
public final class BookDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3L;

    private final String bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String categoryId;
    private final String categoryName;
    private final String publisher;
    private final Integer publicationYear;
    private final String language;
    private final String status;
    private final List<BookLocationDTO> locations;

    /** Creates a V2 catalog summary whose inventory is grouped by location. */
    public BookDTO(
            String bookId,
            String isbn,
            String title,
            String author,
            String categoryId,
            String categoryName,
            String publisher,
            Integer publicationYear,
            String language,
            String status,
            List<BookLocationDTO> locations) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.language = language;
        this.status = status;
        this.locations = List.copyOf(locations);
    }

    /** @return stable library book identifier */
    public String getBookId() {
        return bookId;
    }

    /** @return international standard book number */
    public String getIsbn() {
        return isbn;
    }

    /** @return display title */
    public String getTitle() {
        return title;
    }

    /** @return display author */
    public String getAuthor() {
        return author;
    }

    /** @return stable library category identifier */
    public String getCategoryId() { return categoryId; }

    /** @return category display name */
    public String getCategoryName() { return categoryName; }

    public String getPublisher() { return publisher; }

    public Integer getPublicationYear() { return publicationYear; }

    public String getLanguage() { return language; }

    public String getStatus() { return status; }

    /** @return immutable inventory summaries grouped by location */
    public List<BookLocationDTO> getLocations() { return locations; }

    /** @return total number of copies */
    public int getTotalCount() {
        return locations.stream().mapToInt(BookLocationDTO::getTotalCount).sum();
    }

    /** @return copies currently available to borrow */
    public int getAvailableCount() {
        return locations.stream().mapToInt(BookLocationDTO::getAvailableCount).sum();
    }
}
