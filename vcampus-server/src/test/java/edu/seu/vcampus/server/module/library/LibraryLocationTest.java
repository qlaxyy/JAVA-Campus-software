package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.AddLocationRequest;
import edu.seu.vcampus.common.library.BookCopyDTO;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.LibraryLocationDTO;
import edu.seu.vcampus.common.library.ListBookCopiesRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 馆藏地字典。
 *
 * <p>单册以"房间名"落库，因此房间名就是主键。字典把这串自由文本变成一份可枚举的清单：
 * 登记或迁移单册只能落到字典里已有的名字上，新房间必须显式新增。这样一来，一个错别字
 * 不会再悄悄把一本书分到一间并不存在的阅览室里，管理员也能看到每个馆藏地实际有多少册。
 *
 * <p>代价是馆藏地与分类一样**只能新增**：改名会让所有引用旧名字的单册失联。
 */
class LibraryLocationTest {

    private static final String JIULONGHU = InMemoryBookCopyRepository.JIULONGHU;
    private static final String SIPAILOU = InMemoryBookCopyRepository.SIPAILOU;
    private static final String ANNEX = "Jiulonghu Annex";

    private static final SessionInfo ADMIN = new SessionInfo("admin", "A-1", "libraryadmin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));
    private static final SessionInfo READER = new SessionInfo("reader", "U-1", "reader", "读者",
            Role.USER);

    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final LibraryService service =
            new LibraryService(books, new InMemoryBorrowRecordRepository(), copies);

    @Test
    void dictionaryListsEverySeededLocationWithItsCopyCount() {
        List<LibraryLocationDTO> locations = service.listLocations(ADMIN);
        assertEquals(List.of(JIULONGHU, SIPAILOU),
                locations.stream().map(LibraryLocationDTO::getLocationName).toList(),
                "种子馆藏地按固定顺序返回，界面下拉因此不会每次换个次序");

        for (LibraryLocationDTO location : locations) {
            long expected = copies.findAll().stream()
                    .filter(copy -> copy.location().equals(location.getLocationName()))
                    .filter(copy -> copy.status() != BookCopyStatus.WITHDRAWN)
                    .count();
            assertEquals(expected, location.getCopyCount(), location.getLocationName());
            assertTrue(expected > 0, "演示数据里每个馆藏地都有单册");
        }
    }

    @Test
    void aLocationMustBeAddedBeforeAnyCopyCanBeFiledThere() {
        failure(ErrorCodes.LIBRARY_LOCATION_NOT_FOUND, () -> service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "BC-BEFORE", ANNEX, "TP312/NEW")));
        assertThrows(IllegalArgumentException.class, () -> service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "BC-TOO-LONG", "馆".repeat(101), "TP312/NEW")),
                "长度上限先于字典查找");

        LibraryLocationDTO added = service.addLocation(ADMIN, new AddLocationRequest(ANNEX));
        assertEquals(ANNEX, added.getLocationName());
        assertEquals(0, added.getCopyCount(), "刚新增的馆藏地还没有单册");

        BookCopyDTO registered = service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "BC-BEFORE", ANNEX, "TP312/NEW"));
        assertEquals(ANNEX, registered.getLocation());
        assertEquals(1, copyCountOf(ANNEX));
    }

    @Test
    void duplicateLocationNamesAreRejectedIgnoringCaseAndSurroundingSpace() {
        service.addLocation(ADMIN, new AddLocationRequest(ANNEX));
        failure(ErrorCodes.LIBRARY_DUPLICATE_LOCATION,
                () -> service.addLocation(ADMIN, new AddLocationRequest(ANNEX)));
        failure(ErrorCodes.LIBRARY_DUPLICATE_LOCATION, () -> service.addLocation(ADMIN,
                new AddLocationRequest("  " + ANNEX.toUpperCase(Locale.ROOT) + "  ")));
        failure(ErrorCodes.LIBRARY_DUPLICATE_LOCATION,
                () -> service.addLocation(ADMIN, new AddLocationRequest(JIULONGHU)));
    }

    @Test
    void movingACopyRequiresAKnownTargetAndKeepsBothCountsHonest() {
        BookCopyDTO source = firstCopyAt(JIULONGHU);
        int sourceBefore = copyCountOf(JIULONGHU);
        int targetBefore = copyCountOf(SIPAILOU);

        failure(ErrorCodes.LIBRARY_LOCATION_NOT_FOUND, () -> service.updateBookCopy(ADMIN,
                new UpdateBookCopyRequest(source.getCopyId(), "四牌楼", "TP312/MOVE")));
        assertEquals(sourceBefore, copyCountOf(JIULONGHU), "被拒的迁移不改动任何计数");

        BookCopyDTO moved = service.updateBookCopy(ADMIN,
                new UpdateBookCopyRequest(source.getCopyId(), SIPAILOU, "TP312/MOVE"));
        assertEquals(SIPAILOU, moved.getLocation());
        assertEquals(sourceBefore - 1, copyCountOf(JIULONGHU));
        assertEquals(targetBefore + 1, copyCountOf(SIPAILOU));
    }

    @Test
    void withdrawnCopiesStopCountingButTheirLocationRemains() {
        int before = copyCountOf(JIULONGHU);
        service.withdrawBookCopy(ADMIN, new BookCopyIdRequest(firstCopyAt(JIULONGHU).getCopyId()));

        assertEquals(before - 1, copyCountOf(JIULONGHU), "已注销单册不再计入馆藏地");
        assertTrue(service.listLocations(ADMIN).stream()
                        .anyMatch(location -> JIULONGHU.equals(location.getLocationName())),
                "馆藏地是物理房间，不会因为一本书注销而消失");
    }

    @Test
    void onlyLibraryAdministratorsCanReadOrExtendTheDictionary() {
        failure(ErrorCodes.AUTH_FORBIDDEN, () -> service.listLocations(READER));
        failure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.addLocation(READER, new AddLocationRequest(ANNEX)));
    }

    private int copyCountOf(String locationName) {
        return service.listLocations(ADMIN).stream()
                .filter(location -> location.getLocationName().equals(locationName))
                .findFirst().orElseThrow().getCopyCount();
    }

    private BookCopyDTO firstCopyAt(String locationName) {
        return service.listBookCopies(ADMIN, new ListBookCopiesRequest("B001")).stream()
                .filter(copy -> locationName.equals(copy.getLocation()))
                .findFirst().orElseThrow();
    }

    private static void failure(String expectedCode, org.junit.jupiter.api.function.Executable action) {
        LibraryBusinessException exception = assertThrows(
                LibraryBusinessException.class, action);
        assertEquals(expectedCode, exception.code());
    }
}
