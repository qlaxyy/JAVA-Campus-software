package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.*;
import edu.seu.vcampus.common.user.*;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class StudentViewTest {
    @Test void onlyActualStudentProfilePrefillsTheLoggedInCardNumber() throws Exception {
        verify("20260009", "U-STUDENT-004", true, "20260009");
        verify("20260001", "U-STUDENT-ADMIN-001", false, "");
        verify("20260021", "U-TEACHER-001", false, "");
        verify("20260029", "U-DOCTOR-001", false, "");
    }

    @Test void manualSearchIsNotOverwrittenByDelayedMembershipResponse() throws Exception {
        verify("20260009", "U-STUDENT-004", true, "20260020", "20260020");
    }

    private void verify(String number, String id, boolean found, String expected,
                        String... manual) throws Exception {
        ActionRouter router = new ActionRouter();
        router.register(UserActions.LOGIN, request -> Response.success(request, "成功",
                new SessionInfo("token", id, number, "测试姓名", Role.USER)));
        router.register(StudentActions.GET_PROFILE, request -> {
            assertEquals(number, ((StudentProfileRequest) request.getData()).getStudentId());
            StudentProfileResponse result = new StudentProfileResponse();
            result.setFound(found);
            if (found) {
                StudentProfileDto profile = new StudentProfileDto();
                profile.setStudentId(number);
                result.setProfile(profile);
            }
            return Response.success(request, "成功", result);
        });
        try (CampusServer server = new CampusServer(0, 2, router)) {
            server.start();
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(number, "123456".toCharArray()).isSuccess());
            AtomicReference<StudentView> view = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                view.set(new StudentView(context));
                assertEquals("", field(view.get()).getText());
                SwingWorker<Response, Void> worker = view.get().initializeOwnStudentSearch();
                worker.addPropertyChangeListener(event -> {
                    if ("state".equals(event.getPropertyName())
                            && event.getNewValue() == SwingWorker.StateValue.DONE) { done.countDown(); }
                });
                if (manual.length > 0) { field(view.get()).setText(manual[0]); }
            });
            assertTrue(done.await(10, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> assertEquals(expected, field(view.get()).getText()));
        }
    }

    private static JTextField field(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTextField field && "student.searchId".equals(field.getName())) {
                return field;
            }
            if (child instanceof Container container) {
                JTextField result = field(container);
                if (result != null) { return result; }
            }
        }
        return null;
    }
}
