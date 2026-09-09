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
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

            Response login = context.login("20260001", "123456".toCharArray());

            assertTrue(login.isSuccess());
            SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
            assertEquals(Role.USER, session.getRole());
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
    void demoTeacherCanLoginButHasNoGlobalAdministrativeAuthority() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response login = context.login("20260008", "123456".toCharArray());

            assertTrue(login.isSuccess());
            SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
            assertEquals("U-COURSE-TEACHER-001", session.getUserId());
            assertEquals("20260008", session.getUsername());
            assertEquals("演示教师", session.getDisplayName());
            assertEquals(Role.USER, session.getRole());
            assertTrue(session.getAdminScopes().isEmpty());
        }
    }

    @Test
    void wrongPasswordDoesNotCreateSession() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            char[] password = "wrong-password".toCharArray();

            Response response = context.login("20260001", password);

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_INVALID_CREDENTIALS, response.getCode());
            assertTrue(context.currentSession().isEmpty());
            for (char value : password) {
                assertEquals('\0', value);
            }
        }
    }

    @Test
    void userCanChangeOwnPasswordAndMustLoginAgain() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());

            Response changed = context.changePassword(
                    "123456".toCharArray(), "654321".toCharArray());

            assertTrue(changed.isSuccess());
            assertTrue(context.currentSession().isEmpty());
            assertFalse(context.login("20260001", "123456".toCharArray()).isSuccess());
            assertTrue(context.login("20260001", "654321".toCharArray()).isSuccess());
        }
    }

    @Test
    void wrongCurrentPasswordDoesNotChangePasswordOrSession() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());

            Response changed = context.changePassword(
                    "wrong".toCharArray(), "654321".toCharArray());

            assertFalse(changed.isSuccess());
            assertEquals(ErrorCodes.AUTH_INVALID_CREDENTIALS, changed.getCode());
            assertTrue(context.currentSession().isPresent());
            assertTrue(context.send(UserActions.CURRENT_SESSION, null).isSuccess());
        }
    }

    @Test
    void subsystemAdministratorReceivesServerAssignedScope() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response login = context.login(
                    "20260007", "123456".toCharArray());

            assertTrue(login.isSuccess());
            SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
            assertEquals(Role.USER, session.getRole());
            assertEquals(java.util.Set.of(AdminScope.HOSPITAL), session.getAdminScopes());
        }
    }

    @Test
    void everySubsystemAdministratorReceivesItsOwnScope() throws Exception {
        Map<String, AdminLogin> accounts = Map.of(
                "20260003", new AdminLogin(AdminScope.STUDENT),
                "20260004", new AdminLogin(AdminScope.COURSE),
                "20260005", new AdminLogin(AdminScope.LIBRARY),
                "20260006", new AdminLogin(AdminScope.SHOP),
                "20260007", new AdminLogin(AdminScope.HOSPITAL));

        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            for (Map.Entry<String, AdminLogin> entry : accounts.entrySet()) {
                Response login = context.login(
                        entry.getKey(), "123456".toCharArray());
                assertTrue(login.isSuccess(), entry.getKey());
                SessionInfo session = assertInstanceOf(SessionInfo.class, login.getData());
                assertEquals(Role.USER, session.getRole());
                assertEquals(Set.of(entry.getValue().scope()), session.getAdminScopes());
            }
        }
    }

    @Test
    void manyClientsCanLoginAndUseIndependentSessionsConcurrently() throws Exception {
        int clientCount = 12;
        try (CampusServer server = new CampusServer(0, 4)) {
            server.start();
            ExecutorService clients = Executors.newFixedThreadPool(clientCount);
            CountDownLatch ready = new CountDownLatch(clientCount);
            CountDownLatch start = new CountDownLatch(1);
            try {
                List<Future<SessionInfo>> results = new ArrayList<>();
                for (int index = 0; index < clientCount; index++) {
                    results.add(clients.submit(() -> {
                        ClientContext context = new ClientContext(
                                new CampusClient("127.0.0.1", server.getPort()));
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("clients did not start together");
                        }
                        Response login = context.login(
                                "20260001", "123456".toCharArray());
                        assertTrue(login.isSuccess());
                        SessionInfo session = assertInstanceOf(
                                SessionInfo.class, login.getData());
                        Response current = context.send(
                                UserActions.CURRENT_SESSION, null);
                        assertTrue(current.isSuccess());
                        assertEquals(session.getToken(), assertInstanceOf(
                                SessionInfo.class, current.getData()).getToken());
                        return session;
                    }));
                }

                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                Set<String> tokens = new HashSet<>();
                for (Future<SessionInfo> result : results) {
                    SessionInfo session = result.get(10, TimeUnit.SECONDS);
                    assertEquals("20260001", session.getUsername());
                    tokens.add(session.getToken());
                }
                assertEquals(clientCount, tokens.size());
            } finally {
                clients.shutdownNow();
            }
        }
    }

    @Test
    void loggingOutOneClientDoesNotEndAnotherClientSession() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext first = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            ClientContext second = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            assertTrue(first.login("20260001", "123456".toCharArray()).isSuccess());
            assertTrue(second.login("20260001", "123456".toCharArray()).isSuccess());
            assertFalse(first.currentSession().orElseThrow().getToken().equals(
                    second.currentSession().orElseThrow().getToken()));

            assertTrue(first.logout().isSuccess());
            assertTrue(second.send(UserActions.CURRENT_SESSION, null).isSuccess());
        }
    }

    private record AdminLogin(AdminScope scope) {
    }
}
