package edu.seu.vcampus.server.module.library;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBookCopyRepositoryTest {

    @Test
    void demoCopiesHaveUniqueIdentityAndAreDistributedAcrossLocations() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryBookCopyRepository copies =
                InMemoryBookCopyRepository.seededFrom(books.searchAll(""));

        List<BookCopy> javaCopies = copies.findByBookId("B001");

        assertEquals(5, javaCopies.size());
        assertEquals(5, new HashSet<>(javaCopies.stream().map(BookCopy::copyId).toList()).size());
        assertEquals(5, new HashSet<>(javaCopies.stream().map(BookCopy::barcode).toList()).size());
        assertEquals(5, javaCopies.stream()
                .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE).count());
        assertEquals(3, javaCopies.stream()
                .filter(copy -> copy.location().startsWith("九龙湖校区")).count());
        assertEquals(2, javaCopies.stream()
                .filter(copy -> copy.location().startsWith("四牌楼校区")).count());
        assertThrows(UnsupportedOperationException.class, javaCopies::clear);
    }

    @Test
    void updatePreservesIdentityParentAndBarcode() {
        InMemoryBookCopyRepository copies = new InMemoryBookCopyRepository();
        BookCopy original = copy("CP-1", "BAR-1", "B001");
        copies.insert(original);

        copies.update(original.withLocation("四牌楼校区", "TP-001"));

        BookCopy moved = copies.findById("CP-1").orElseThrow();
        assertEquals("四牌楼校区", moved.location());
        assertEquals("TP-001", moved.callNumber());
        assertThrows(IllegalArgumentException.class, () -> copies.update(new BookCopy(
                "CP-1", "BAR-2", "B001", moved.location(), moved.callNumber(), moved.status())));
        assertThrows(IllegalArgumentException.class, () -> copies.update(new BookCopy(
                "CP-1", "BAR-1", "B002", moved.location(), moved.callNumber(), moved.status())));
        assertThrows(IllegalStateException.class,
                () -> copies.insert(copy("CP-2", "BAR-1", "B002")));
    }

    @Test
    void replacementRejectsDuplicatesWithoutPartialMutation() {
        InMemoryBookCopyRepository copies = new InMemoryBookCopyRepository();
        copies.insert(copy("CP-1", "BAR-1", "B001"));
        copies.insert(copy("CP-2", "BAR-2", "B002"));

        assertThrows(IllegalStateException.class, () -> copies.replaceForBook("B001", List.of(
                copy("CP-3", "BAR-2", "B001"))));

        assertEquals(List.of("CP-1"), copies.findByBookId("B001").stream()
                .map(BookCopy::copyId).toList());
        assertEquals(List.of("CP-2"), copies.findByBookId("B002").stream()
                .map(BookCopy::copyId).toList());
    }

    private static BookCopy copy(String id, String barcode, String bookId) {
        return new BookCopy(id, barcode, bookId, "九龙湖校区", "TP-001",
                BookCopyStatus.AVAILABLE);
    }
}
