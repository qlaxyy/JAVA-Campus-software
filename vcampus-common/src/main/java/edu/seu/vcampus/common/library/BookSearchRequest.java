package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Keyword criteria sent by the library search page. */
public final class BookSearchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String keyword;

    public BookSearchRequest(String keyword) {
        this.keyword = keyword;
    }

    /** @return user-entered title, author, ISBN or category keyword */
    public String getKeyword() {
        return keyword;
    }
}
