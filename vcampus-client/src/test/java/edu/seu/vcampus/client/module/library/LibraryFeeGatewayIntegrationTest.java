package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.card.CardActions;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.BorrowRecordIdRequest;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.server.VirtualCampusRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 逾期滞纳金经校园卡扣款的端到端链路：真实 Socket、真实 Access 库、真实卡网关。
 *
 * <p>用演示库里预置的逾期记录（`U-LIBRARY-ADMIN-001` 借的《史记》，超期 15 天）走完整流程：
 * 归还 → 产生滞纳金 → 未结清时不能借书 → 缴费扣款 → 恢复借阅 → 重复缴费不再扣款。
 */
class LibraryFeeGatewayIntegrationTest {

    /** 演示数据：超期 15 天；费率 0.5 元/天，故滞纳金 750 分。 */
    private static final String OVERDUE_BARCODE = "SEU-B005-002";
    private static final int EXPECTED_FEE_FEN = 750;

    @TempDir
    Path temporaryDirectory;

    @Test
    void overdueFineIsSettledThroughTheCampusCardPort() throws Exception {
        Path databasePath = temporaryDirectory.resolve("library-fee.accdb");
        try (VirtualCampusRuntime runtime = VirtualCampusRuntime.start(0, 0, databasePath)) {
            ClientContext reader = login("20260003", runtime.campusPort(), runtime.cardPort());
            int before = balance(reader);

            Response returned = reader.send(
                    LibraryActions.RETURN_COPY, new CopyReturnRequest(OVERDUE_BARCODE));
            assertTrue(returned.isSuccess(), returned.getMessage());

            BorrowRecordDTO overdue = recordWithBarcode(reader, OVERDUE_BARCODE);
            assertEquals("RETURNED", overdue.getStatus());
            assertEquals(EXPECTED_FEE_FEN, overdue.getFeeFen(), "逾期 15 天 × 0.5 元");
            assertFalse(overdue.isFeeSettled());

            failure(ErrorCodes.LIBRARY_OUTSTANDING_FEE, reader.send(
                    LibraryActions.BORROW_COPY, new CopyBorrowRequest("SEU-B004-001")));

            Response paid = reader.send(LibraryActions.PAY_FEE,
                    new BorrowRecordIdRequest(overdue.getRecordId()));
            assertTrue(paid.isSuccess(), paid.getMessage());
            assertEquals(before - EXPECTED_FEE_FEN, balance(reader));

            assertTrue(reader.send(
                    LibraryActions.BORROW_COPY, new CopyBorrowRequest("SEU-B004-001")).isSuccess(),
                    "结清费用后应恢复借阅");
            assertTrue(recordWithBarcode(reader, OVERDUE_BARCODE).isFeeSettled());

            failure(ErrorCodes.LIBRARY_FEE_NOT_PAYABLE, reader.send(LibraryActions.PAY_FEE,
                    new BorrowRecordIdRequest(overdue.getRecordId())));
            assertEquals(before - EXPECTED_FEE_FEN, balance(reader), "重复缴费不能再次扣款");
        }
    }

    @Test
    void settledFeeSurvivesAServerRestart() throws Exception {
        Path databasePath = temporaryDirectory.resolve("library-fee-restart.accdb");
        String recordId;

        try (VirtualCampusRuntime runtime = VirtualCampusRuntime.start(0, 0, databasePath)) {
            ClientContext reader = login("20260003", runtime.campusPort(), runtime.cardPort());
            reader.send(LibraryActions.RETURN_COPY, new CopyReturnRequest(OVERDUE_BARCODE));
            BorrowRecordDTO overdue = recordWithBarcode(reader, OVERDUE_BARCODE);
            recordId = overdue.getRecordId();
            assertTrue(reader.send(LibraryActions.PAY_FEE,
                    new BorrowRecordIdRequest(recordId)).isSuccess());
        }

        try (VirtualCampusRuntime second = VirtualCampusRuntime.start(0, 0, databasePath)) {
            ClientContext reader = login("20260003", second.campusPort(), second.cardPort());
            BorrowRecordDTO settled = records(reader).stream()
                    .filter(record -> record.getRecordId().equals(recordId))
                    .findFirst().orElseThrow();
            assertTrue(settled.isFeeSettled(), "结清状态应跨服务器重启保留");
            assertEquals(EXPECTED_FEE_FEN, settled.getFeeFen());
            // 余额也应保留：演示初始 100 元减去 7.50 元
            assertEquals(10_000 - EXPECTED_FEE_FEN, balance(reader));
        }
    }

    private static int balance(ClientContext context) throws Exception {
        CampusCardView card = assertInstanceOf(
                CampusCardView.class, context.sendCard(CardActions.GET, null).getData());
        return card.getBalanceFen();
    }

    private static List<BorrowRecordDTO> records(ClientContext context) throws Exception {
        Response response = context.send(LibraryActions.GET_BORROW_RECORDS, null);
        assertTrue(response.isSuccess(), response.getMessage());
        @SuppressWarnings("unchecked")
        List<BorrowRecordDTO> data = (List<BorrowRecordDTO>) response.getData();
        return data;
    }

    private static BorrowRecordDTO recordWithBarcode(ClientContext context, String barcode)
            throws Exception {
        return records(context).stream()
                .filter(record -> barcode.equals(record.getBarcode()))
                .findFirst().orElseThrow();
    }

    private static void failure(String expectedCode, Response response) {
        assertFalse(response.isSuccess(), "应当被拒绝，实际成功：" + response.getMessage());
        assertEquals(expectedCode, response.getCode());
    }

    private static ClientContext login(String username, int campusPort, int cardPort)
            throws Exception {
        ClientContext context = new ClientContext(
                new CampusClient("127.0.0.1", campusPort),
                new CampusClient("127.0.0.1", cardPort));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }
}
