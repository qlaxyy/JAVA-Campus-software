package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationIntegrationTest {

    @Test
    void demoUserCanLoginUseSessionAndLogout() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response login = context.login("student001", "Student@123".toCharArray());

            assertTrue(login.isSuccess());
            SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
            assertEquals(Role.STUDENT, session.getRole());
            assertFalse(session.getToken().isBlank());
            assertTrue(context.currentSession().isPresent());

            Response current = context.send(UserActions.CURRENT_SESSION, null);
            assertTrue(current.isSuccess());
            assertEquals(session.getUserId(),
                    assertInstanceOf(SessionInfo.class, current.getData()).getUserId());

            Response logout = context.logout();
            assertTrue(logout.isSuccess());
            assertTrue(context.currentSession().isEmpty());

            Response afterLogout = context.send(UserActions.CURRENT_SESSION, null);
            assertFalse(afterLogout.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED, afterLogout.getCode());
        }
    }

    @Test
    void wrongPasswordDoesNotCreateSession() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            char[] password = "wrong-password".toCharArray();

            Response response = context.login("student001", password);

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_INVALID_CREDENTIALS, response.getCode());
            assertTrue(context.currentSession().isEmpty());
            for (char value : password) {
                assertEquals('\0', value);
            }
        }
    }

    @Test
    void subsystemAdministratorReceivesServerAssignedScope() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response login = context.login(
                    "hospitaladmin", "HospitalAdmin@123".toCharArray());

            assertTrue(login.isSuccess());
            SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
            assertEquals(Role.MODULE_ADMIN, session.getRole());
            assertEquals(java.util.Set.of(AdminScope.HOSPITAL), session.getAdminScopes());
        }
    }

    @Test
    void everySubsystemAdministratorReceivesItsOwnScope() throws Exception {
        Map<String, AdminLogin> accounts = Map.of(
                "studentadmin", new AdminLogin("StudentAdmin@123", AdminScope.STUDENT),
                "courseadmin", new AdminLogin("CourseAdmin@123", AdminScope.COURSE),
                "libraryadmin", new AdminLogin("LibraryAdmin@123", AdminScope.LIBRARY),
                "shopadmin", new AdminLogin("ShopAdmin@123", AdminScope.SHOP),
                "hospitaladmin", new AdminLogin("HospitalAdmin@123", AdminScope.HOSPITAL));

        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            for (Map.Entry<String, AdminLogin> entry : accounts.entrySet()) {
                Response login = context.login(
                        entry.getKey(), entry.getValue().password().toCharArray());
                assertTrue(login.isSuccess(), entry.getKey());
                SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
                assertEquals(Role.MODULE_ADMIN, session.getRole());
                assertEquals(Set.of(entry.getValue().scope()), session.getAdminScopes());
            }
        }
    }

    private record AdminLogin(String password, AdminScope scope) {
    }
}
