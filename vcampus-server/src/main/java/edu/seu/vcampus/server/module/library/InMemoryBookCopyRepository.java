package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory physical-copy repository used before the Access implementation. */
final class InMemoryBookCopyRepository implements BookCopyRepository {

    private static final String JIULONGHU = "九龙湖校区—中文图书阅览室3";
    private static final String SIPAILOU = "四牌楼校区—中文书库二楼";

    private final Map<String, BookCopy> copies = new LinkedHashMap<>();

    static InMemoryBookCopyRepository seededFrom(List<BookDTO> books) {
        InMemoryBookCopyRepository repository = new InMemoryBookCopyRepository();
        for (BookDTO book : books) {
            repository.replaceForBook(book.getBookId(), seedCopies(book));
        }
        return repository;
    }

    @Override
    public synchronized List<BookCopy> findByBookId(String bookId) {
        return copies.values().stream().filter(copy -> copy.bookId().equals(bookId)).toList();
    }

    @Override
    public synchronized Optional<BookCopy> findById(String copyId) {
        return Optional.ofNullable(copies.get(copyId));
    }

    @Override
    public synchronized Optional<BookCopy> findByBarcode(String barcode) {
        return copies.values().stream().filter(copy -> copy.barcode().equals(barcode)).findFirst();
    }

    @Override
    public synchronized void insert(BookCopy copy) {
        validateUnique(copy, null);
        copies.put(copy.copyId(), copy);
    }

    @Override
    public synchronized void update(BookCopy copy) {
        BookCopy original = findById(copy.copyId())
                .orElseThrow(() -> new IllegalStateException("Book copy does not exist."));
        if (!original.bookId().equals(copy.bookId()) || !original.barcode().equals(copy.barcode())) {
            throw new IllegalArgumentException("Book-copy identity and barcode are immutable.");
        }
        validateUnique(copy, copy.copyId());
        copies.put(copy.copyId(), copy);
    }

    @Override
    public synchronized void replaceForBook(String bookId, List<BookCopy> replacements) {
        List<BookCopy> snapshot = List.copyOf(replacements);
        Map<String, BookCopy> candidate = new LinkedHashMap<>();
        copies.values().stream().filter(copy -> !copy.bookId().equals(bookId))
                .forEach(copy -> candidate.put(copy.copyId(), copy));
        for (BookCopy copy : snapshot) {
            if (!copy.bookId().equals(bookId)) {
                throw new IllegalArgumentException("Replacement contains a copy for another book.");
            }
            if (candidate.containsKey(copy.copyId())
                    || candidate.values().stream().anyMatch(value -> value.barcode().equals(copy.barcode()))) {
                throw new IllegalStateException("Book-copy identifier or barcode already exists.");
            }
            candidate.put(copy.copyId(), copy);
        }
        copies.clear();
        copies.putAll(candidate);
    }

    synchronized Map<String, BookCopy> snapshot() {
        return new LinkedHashMap<>(copies);
    }

    synchronized void restore(Map<String, BookCopy> snapshot) {
        copies.clear();
        copies.putAll(snapshot);
    }

    private void validateUnique(BookCopy copy, String currentId) {
        if (copies.containsKey(copy.copyId()) && !copy.copyId().equals(currentId)) {
            throw new IllegalStateException("Book-copy identifier already exists.");
        }
        if (copies.values().stream().anyMatch(existing -> existing.barcode().equals(copy.barcode())
                && !existing.copyId().equals(currentId))) {
            throw new IllegalStateException("Book-copy barcode already exists.");
        }
    }

    /** Builds deterministic physical copies from the demonstration catalog seed counts. */
    static List<BookCopy> seedCopies(BookDTO book) {
        List<BookCopy> result = new ArrayList<>();
        for (int index = 1; index <= book.getTotalCount(); index++) {
            String suffix = String.format("%03d", index);
            String location = index <= Math.max(1, (book.getTotalCount() + 1) / 2)
                    ? JIULONGHU : SIPAILOU;
            result.add(new BookCopy(
                    "CP-" + book.getBookId() + "-" + suffix,
                    "SEU-" + book.getBookId() + "-" + suffix,
                    book.getBookId(),
                    location,
                    book.getCategoryId() + "/" + book.getBookId(),
                    BookCopyStatus.AVAILABLE));
        }
        return result;
    }
}
