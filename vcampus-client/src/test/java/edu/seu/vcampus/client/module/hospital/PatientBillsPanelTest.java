package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.PatientBillView;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientBillsPanelTest {

    @Test
    void ignoresAnOldAccountResponseThatArrivesAfterReactivation() throws Exception {
        CountDownLatch firstRequestStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstRequest = new CountDownLatch(1);
        CountDownLatch firstRequestReturned = new CountDownLatch(1);
        ActionRouter router = new ActionRouter();
        router.register(UserActions.LOGIN, request -> {
            LoginRequest login = (LoginRequest) request.getData();
            SessionInfo session = new SessionInfo(
                    "token-" + login.getUsername(), login.getUsername(),
                    login.getUsername(), login.getUsername(), Role.USER);
            return Response.success(request, "ok", session);
        });
        router.register(HospitalActions.LIST_MY_BILLS, request -> {
            if ("token-20260011".equals(request.getToken())) {
                firstRequestStarted.countDown();
                try {
                    releaseFirstRequest.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                firstRequestReturned.countDown();
                return Response.success(request, "ok",
                        new PatientBillListResponse(List.of(bill("first", "旧账号费用"))));
            }
            return Response.success(request, "ok",
                    new PatientBillListResponse(List.of(bill("second", "新账号费用"))));
        });

        try (CampusServer server = new CampusServer(0, 3, router)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260011", "123456".toCharArray()).isSuccess());
            PatientBillsPanel[] panel = new PatientBillsPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PatientBillsPanel(context, () -> { });
                panel[0].activate();
            });
            assertTrue(firstRequestStarted.await(1, TimeUnit.SECONDS));

            assertTrue(context.login("20260012", "123456".toCharArray()).isSuccess());
            SwingUtilities.invokeAndWait(panel[0]::activate);
            assertTrue(awaitCondition(() -> labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("新账号费用"))));

            releaseFirstRequest.countDown();
            assertTrue(firstRequestReturned.await(1, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> { });
            assertFalse(labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("旧账号费用")));
        } finally {
            releaseFirstRequest.countDown();
        }
    }

    @Test
    void displaysDoctorGeneratedFeeAndReloadsPaidState() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260006", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260029", "123456".toCharArray()).isSuccess());
            assertTrue(context.send(
                    HospitalActions.SUBMIT_CONSULTATION,
                    new SubmitConsultationRequest(
                            booking.getAppointmentId(), "课程演示诊断", "",
                            "课程演示处置", "无", "必要时复诊"))
                    .isSuccess());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260006", "123456".toCharArray()).isSuccess());

            PatientBillsPanel[] panel = new PatientBillsPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PatientBillsPanel(context, () -> { });
                panel[0].activate();
            });

            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "payHospitalBillButton").size() == 1));
            List<String> labels = labelTexts(panel[0]);
            assertTrue(labels.stream().anyMatch(text -> text.contains("诊疗处置服务")));
            assertTrue(labels.contains("¥18.00"));
            String billId = namedButtons(panel[0], "payHospitalBillButton")
                    .getFirst().getActionCommand();

            assertTrue(context.send(
                    HospitalActions.PAY_BILL,
                    new PayHospitalBillRequest(billId)).isSuccess());
            SwingUtilities.invokeAndWait(panel[0]::activate);
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "payHospitalBillButton").isEmpty()));
            PatientBillListResponse paid = assertInstanceOf(
                    PatientBillListResponse.class,
                    context.send(HospitalActions.LIST_MY_BILLS, null).getData());
            assertTrue(paid.getBills().stream()
                    .anyMatch(bill -> bill.getBillId().equals(billId)
                            && bill.getPaymentStatus() == PaymentStatus.PAID));
        }
    }

    private static PatientBillView bill(String id, String itemName) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 10, 0);
        return new PatientBillView(
                "bill-" + id, "appointment-" + id, HospitalBillType.TREATMENT,
                itemName, 1_800, PaymentStatus.UNPAID, "全科门诊", "李医生",
                now, now, null, null);
    }

    private static List<String> labelTexts(Container root) {
        return components(root, JLabel.class).stream()
                .map(JLabel::getText)
                .filter(text -> text != null)
                .toList();
    }

    private static List<AbstractButton> namedButtons(Container root, String name) {
        return components(root, AbstractButton.class).stream()
                .filter(button -> name.equals(button.getName()))
                .toList();
    }

    private static <T extends Component> List<T> components(
            Container root,
            Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(components(container, type));
            }
        }
        return found;
    }

    private static boolean awaitCondition(java.util.function.BooleanSupplier condition)
            throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            AtomicBoolean satisfied = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
