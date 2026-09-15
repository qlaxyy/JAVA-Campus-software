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
    @Test void cardActionButtonsUseTheSameNeutralBackground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudentView view = new StudentView(new ClientContext(new CampusClient("127.0.0.1", 1, 100)));
                for (String name : new String[]{"btnApplyModify", "btnOpenChange", "btnDownloadCert", "btnOpenAudit"}) {
                    var field = StudentView.class.getDeclaredField(name); field.setAccessible(true);
                    assertEquals(new Color(241, 245, 249), ((JButton) field.get(view)).getBackground());
                }
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
    }

    @Test void changeApplicationButtonsStayVisibleButDisabledBeforeStudentQuery() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1, 100));
                var sessionField = ClientContext.class.getDeclaredField("session");
                sessionField.setAccessible(true);
                ((edu.seu.vcampus.client.application.ClientSession) sessionField.get(context)).set(
                        new SessionInfo("test", "U-TEST", "20260006", "吴尚扬", Role.USER));
                StudentView view = new StudentView(context);
                var visibility = StudentView.class.getDeclaredMethod("updateButtonVisibility", StudentProfileDto.class);
                visibility.setAccessible(true);
                for (String name : new String[]{"btnApplyModify", "btnOpenChange"}) {
                    var field = StudentView.class.getDeclaredField(name); field.setAccessible(true);
                    JButton button = (JButton) field.get(view);
                    visibility.invoke(view, (Object) null);
                    assertTrue(button.isVisible()); assertFalse(button.isEnabled());
                    StudentProfileDto own = new StudentProfileDto(); own.setStudentId("20260006");
                    visibility.invoke(view, own);
                    assertTrue(button.isVisible()); assertTrue(button.isEnabled());
                    own.setStudentId("20260007"); visibility.invoke(view, own);
                    assertFalse(button.isVisible()); assertFalse(button.isEnabled());
                }
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
    }

    @Test void certificateAndGraduationButtonsRemainVisibleButDisabledBeforeQuery() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                for (Role role : new Role[]{Role.USER, Role.SUPER_ADMIN}) {
                    ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1, 100));
                    var sessionField = ClientContext.class.getDeclaredField("session");
                    sessionField.setAccessible(true);
                    ((edu.seu.vcampus.client.application.ClientSession) sessionField.get(context)).set(
                            new SessionInfo("test", "U-TEST", "20260006", "测试姓名", role));
                    StudentView view = new StudentView(context);
                    var visibility = StudentView.class.getDeclaredMethod(
                            "updateButtonVisibility", StudentProfileDto.class);
                    visibility.setAccessible(true);
                    for (String name : new String[]{"btnDownloadCert", "btnOpenAudit"}) {
                        var buttonField = StudentView.class.getDeclaredField(name);
                        buttonField.setAccessible(true);
                        JButton button = (JButton) buttonField.get(view);
                        assertTrue(button.isVisible());
                        assertFalse(button.isEnabled());
                        StudentProfileDto profile = new StudentProfileDto();
                        profile.setStudentId("20260006");
                        visibility.invoke(view, profile);
                        assertTrue(button.isVisible()); assertTrue(button.isEnabled());
                        profile.setStudentId("20260007");
                        visibility.invoke(view, profile);
                        assertEquals(role == Role.SUPER_ADMIN, button.isVisible());
                        assertEquals(role == Role.SUPER_ADMIN, button.isEnabled());
                        visibility.invoke(view, (Object) null);
                        assertTrue(button.isVisible()); assertFalse(button.isEnabled());
                    }
                }
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }

    @Test void onlyActualStudentProfilePrefillsTheLoggedInCardNumber() throws Exception {
        verify("20260009", "U-STUDENT-004", true, "20260009");
        verify("20260001", "U-STUDENT-ADMIN-001", false, "");
        verify("20260021", "U-TEACHER-001", false, "");
        verify("20260029", "U-DOCTOR-001", false, "");
    }

    @Test void manualSearchIsNotOverwrittenByDelayedMembershipResponse() throws Exception {
        verify("20260009", "U-STUDENT-004", true, "20260020", "20260020");
    }

    @Test void studentIdentityDoesNotDependOnSeedIdsOrCardNumberArithmetic() throws Exception {
        verify("20260039", "U-89ea546759124ad8a52462a140644335", true, "20260039");
        verify("20270001", "U-STUDENT-100", true, "20270001");
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
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(expected, field(view.get()).getText());
                if (found) {
                    try {
                        var identity = StudentView.class.getDeclaredMethod("isCurrentSelfStudent", String.class);
                        identity.setAccessible(true);
                        assertEquals(true, identity.invoke(view.get(), number));
                        assertEquals(false, identity.invoke(view.get(), number.substring(1)));
                        assertEquals(false, identity.invoke(view.get(), "20269999"));
                        var visibility = StudentView.class.getDeclaredMethod(
                                "updateButtonVisibility", StudentProfileDto.class);
                        visibility.setAccessible(true);
                        StudentProfileDto own = new StudentProfileDto();
                        own.setStudentId(number);
                        visibility.invoke(view.get(), own);
                        var edit = StudentView.class.getDeclaredField("btnEdit");
                        edit.setAccessible(true);
                        assertTrue(((JButton) edit.get(view.get())).isVisible());
                        own.setStudentId("20269999");
                        visibility.invoke(view.get(), own);
                        assertFalse(((JButton) edit.get(view.get())).isVisible());
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError(exception);
                    }
                }
            });
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
