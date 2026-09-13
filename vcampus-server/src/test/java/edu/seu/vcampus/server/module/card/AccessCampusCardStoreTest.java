package edu.seu.vcampus.server.module.card;

import edu.seu.vcampus.common.card.CampusCardLedgerEntry;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessCampusCardStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsBalanceAcrossReopen() {
        Path databasePath = temporaryDirectory.resolve("cards.accdb");
        SessionInfo student = session("U-STUDENT-001", "20260001");
        AccessCampusCardStore first = new AccessCampusCardStore(new AccessDatabase(databasePath));
        first.recharge(student, 10_000);
        assertEquals(10_000, first.view(student).getBalanceFen());
        first.debit(student, 400, ModuleNames.SHOP, "order:SO-1");

        AccessCampusCardStore second = new AccessCampusCardStore(new AccessDatabase(databasePath));
        CampusCardView card = second.view(student);
        assertEquals(9_600, card.getBalanceFen());
        assertEquals(2, second.listLedger(student).size());
        assertEquals(CampusCardLedgerEntry.DEBIT, second.listLedger(student).getFirst().getEntryType());
    }

    @Test
    void rejectsInsufficientBalance() {
        Path databasePath = temporaryDirectory.resolve("empty-pay.accdb");
        SessionInfo student = session("U-STUDENT-001", "20260001");
        AccessCampusCardStore store = new AccessCampusCardStore(new AccessDatabase(databasePath));
        CardBusinessException exception = assertThrows(
                CardBusinessException.class,
                () -> store.debit(student, 20_000, ModuleNames.HOSPITAL, "bill:too-big"));
        assertEquals("余额不足，请充值！", exception.getMessage());
    }

    @Test
    void repeatsSameDebitReferenceWithoutDoubleCharge() {
        Path databasePath = temporaryDirectory.resolve("idempotent.accdb");
        SessionInfo student = session("U-STUDENT-001", "20260001");
        AccessCampusCardStore store = new AccessCampusCardStore(new AccessDatabase(databasePath));
        store.recharge(student, 10_000);
        store.debit(student, 500, ModuleNames.LIBRARY, "fine:1");
        store.debit(student, 500, ModuleNames.LIBRARY, "fine:1");
        assertEquals(9_500, store.view(student).getBalanceFen());
    }

    private static SessionInfo session(String userId, String username) {
        return new SessionInfo("token-" + userId, userId, username, username, Role.USER);
    }
}
