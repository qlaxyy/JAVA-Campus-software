package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Edits catalog metadata without changing identity, status, copies or history. */
public final class UpdateBookRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    private final String bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String categoryId;
    private final String publisher;
    private final Integer publicationYear;
    private final String language;

    /**
     * Creates the transfer object.
     * @param bookId existing book identifier
     * @param isbn ISBN-10 or ISBN-13
     * @param title book title
     * @param author author name
     * @param categoryId existing category identifier
     */
    public UpdateBookRequest(String bookId, String isbn, String title, String author,
            String categoryId, String publisher, Integer publicationYear, String language) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.categoryId = categoryId;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.language = language;
    }

    /** @return existing book identifier */
    public String getBookId() { return bookId; }

    /** @return ISBN-10 or ISBN-13 */
    public String getIsbn() { return isbn; }

    /** @return book title */
    public String getTitle() { return title; }

    /** @return author name */
    public String getAuthor() { return author; }

    /** @return existing category identifier */
    public String getCategoryId() { return categoryId; }

    public String getPublisher() { return publisher; }

    public Integer getPublicationYear() { return publicationYear; }

    public String getLanguage() { return language; }
}
