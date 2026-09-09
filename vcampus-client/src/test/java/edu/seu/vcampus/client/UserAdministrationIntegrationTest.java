package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.BatchCreateUserAccountsRequest;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.CreateGeneratedUserAccountRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.UserAccountListResponse;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.common.user.UserAuditLogResponse;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class UserAdministrationIntegrationTest {

    @Test
    void regularAccountCannotListAccounts() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = client(server);
            assertTrue(student.login("20260001", password()).isSuccess());
            Response response = student.send(UserActions.ADMIN_LIST_ACCOUNTS, null);
            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, response.getCode());
        }
    }

    @Test
    void regularAccountCannotReadAccountAuditLogs() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = client(server);
            assertTrue(student.login("20260001", password()).isSuccess());

            Response response = student.send(UserActions.ADMIN_LIST_AUDIT_LOGS, null);

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, response.getCode());
        }
    }

    @Test
    void superAdministratorCanListCreateAndLoginNewAccount() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());

            Response initial = administrator.send(UserActions.ADMIN_LIST_ACCOUNTS, null);
            UserAccountListResponse accounts = assertInstanceOf(
                    UserAccountListResponse.class, initial.getData());
            assertEquals(9, accounts.getAccounts().size());

            CreateUserAccountRequest request = new CreateUserAccountRequest(
                    "20261001", "新建用户", proof("20261001"), Set.of(AdminScope.COURSE));
            Response created = administrator.send(UserActions.ADMIN_CREATE_ACCOUNT, request);
            assertTrue(created.isSuccess());

            ClientContext newUser = client(server);
            assertTrue(newUser.login("20261001", password()).isSuccess());
            assertEquals(Set.of(AdminScope.COURSE),
                    newUser.currentSession().orElseThrow().getAdminScopes());
        }
    }

    @Test
    void regularAccountCannotReceiveMoreThanOneSubsystemManagementScope() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());

            CreateUserAccountRequest request = new CreateUserAccountRequest(
                    "20261009", "权限过多账号", proof("20261009"),
                    Set.of(AdminScope.COURSE, AdminScope.LIBRARY));
            Response response = administrator.send(
                    UserActions.ADMIN_CREATE_ACCOUNT, request);

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, response.getCode());
            assertFalse(client(server).login("20261009", password()).isSuccess());
        }
    }

    @Test
    void superAdministratorPreviewsAndCreatesNextGeneratedAccountNumber()
            throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());

            Response preview = administrator.send(
                    UserActions.ADMIN_PREVIEW_NEXT_ACCOUNT, null);
            assertTrue(preview.isSuccess());
            String suggested = assertInstanceOf(String.class, preview.getData());

            Response created = administrator.send(
                    UserActions.ADMIN_CREATE_GENERATED_ACCOUNT,
                    new CreateGeneratedUserAccountRequest(
                            "自动编号用户", Set.of(AdminScope.LIBRARY)));

            assertTrue(created.isSuccess());
            UserAccountView account = assertInstanceOf(
                    UserAccountView.class, created.getData());
            assertEquals(suggested, account.getUsername());
            assertEquals(Set.of(AdminScope.LIBRARY), account.getAdminScopes());
            assertTrue(client(server).login(account.getUsername(), password()).isSuccess());
        }
    }

    @Test
    void superAdministratorCanBatchCreateAccounts() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());

            BatchCreateUserAccountsRequest request = new BatchCreateUserAccountsRequest(List.of(
                    createRequest("20261002", "新生一"),
                    createRequest("20261003", "新生二")));
            Response imported = administrator.send(
                    UserActions.ADMIN_BATCH_CREATE_ACCOUNTS, request);

            assertTrue(imported.isSuccess());
            UserAccountListResponse created = assertInstanceOf(
                    UserAccountListResponse.class, imported.getData());
            assertEquals(2, created.getAccounts().size());
            assertTrue(client(server).login("20261002", password()).isSuccess());
            assertTrue(client(server).login("20261003", password()).isSuccess());
        }
    }

    @Test
    void duplicateInBatchRejectsTheWholeImport() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());

            Response imported = administrator.send(
                    UserActions.ADMIN_BATCH_CREATE_ACCOUNTS,
                    new BatchCreateUserAccountsRequest(List.of(
                            createRequest("20261004", "新生三"),
                            createRequest("20260001", "重复账号"))));

            assertFalse(imported.isSuccess());
            assertEquals(ErrorCodes.USER_USERNAME_EXISTS, imported.getCode());
            assertFalse(client(server).login("20261004", password()).isSuccess());
        }
    }

    @Test
    void disablingAccountInvalidatesItsSessionAndBlocksLogin() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            ClientContext student = client(server);
            AtomicInteger authenticationLost = new AtomicInteger();
            student.setAuthenticationLostHandler(authenticationLost::incrementAndGet);
            assertTrue(administrator.login("20260000", password()).isSuccess());
            assertTrue(student.login("20260001", password()).isSuccess());
            String studentId = student.currentSession().orElseThrow().getUserId();

            Response disabled = administrator.send(
                    UserActions.ADMIN_UPDATE_STATUS,
                    new UpdateUserStatusRequest(studentId, false));
            assertTrue(disabled.isSuccess());

            Response oldSession = student.send(UserActions.CURRENT_SESSION, null);
            assertEquals(ErrorCodes.AUTH_REQUIRED, oldSession.getCode());
            assertTrue(student.currentSession().isEmpty());
            assertEquals(1, authenticationLost.get());
            assertFalse(client(server).login("20260001", password()).isSuccess());
        }
    }

    @Test
    void superAdministratorCannotDisableCurrentAccount() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());
            String administratorId = administrator.currentSession().orElseThrow().getUserId();

            Response response = administrator.send(
                    UserActions.ADMIN_UPDATE_STATUS,
                    new UpdateUserStatusRequest(administratorId, false));

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.USER_SELF_DISABLE_FORBIDDEN, response.getCode());
            assertTrue(administrator.send(UserActions.CURRENT_SESSION, null).isSuccess());
        }
    }

    @Test
    void accountChangesAndBusinessFailuresAppearInAuditLog() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());

            Response created = administrator.send(
                    UserActions.ADMIN_CREATE_GENERATED_ACCOUNT,
                    new CreateGeneratedUserAccountRequest("审计测试账号", Set.of()));
            assertTrue(created.isSuccess());
            String administratorId = administrator.currentSession().orElseThrow().getUserId();
            Response rejected = administrator.send(
                    UserActions.ADMIN_UPDATE_STATUS,
                    new UpdateUserStatusRequest(administratorId, false));
            assertFalse(rejected.isSuccess());

            Response response = administrator.send(UserActions.ADMIN_LIST_AUDIT_LOGS, null);
            UserAuditLogResponse logs = assertInstanceOf(
                    UserAuditLogResponse.class, response.getData());

            assertEquals(2, logs.getEntries().size());
            assertEquals(UserActions.ADMIN_UPDATE_STATUS,
                    logs.getEntries().get(0).getActionCode());
            assertFalse(logs.getEntries().get(0).isSuccessful());
            assertEquals("20260000", logs.getEntries().get(0).getTarget());
            assertEquals(UserActions.ADMIN_CREATE_GENERATED_ACCOUNT,
                    logs.getEntries().get(1).getActionCode());
            assertTrue(logs.getEntries().get(1).isSuccessful());
            assertEquals("20260000", logs.getEntries().get(1).getActorUsername());
        }
    }

    @Test
    void changingScopesInvalidatesOldSessionAndAppliesAtNextLogin() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            ClientContext target = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());
            assertTrue(target.login("20260004", password()).isSuccess());
            String targetId = target.currentSession().orElseThrow().getUserId();

            Response updated = administrator.send(
                    UserActions.ADMIN_UPDATE_ACCOUNT,
                    new UpdateUserAccountRequest(
                            targetId, "权限已更新", Set.of(AdminScope.HOSPITAL)));

            assertTrue(updated.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED,
                    target.send(UserActions.CURRENT_SESSION, null).getCode());
            assertTrue(target.login("20260004", password()).isSuccess());
            assertEquals(Set.of(AdminScope.HOSPITAL),
                    target.currentSession().orElseThrow().getAdminScopes());
        }
    }

    @Test
    void resettingPasswordInvalidatesSessionAndReplacesCredentials() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            ClientContext student = client(server);
            assertTrue(administrator.login("20260000", password()).isSuccess());
            assertTrue(student.login("20260001", password()).isSuccess());
            String studentId = student.currentSession().orElseThrow().getUserId();

            Response reset = administrator.send(
                    UserActions.ADMIN_RESET_PASSWORD,
                    new ResetUserPasswordRequest(studentId, proof("20260001", "654321")));

            assertTrue(reset.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED,
                    student.send(UserActions.CURRENT_SESSION, null).getCode());
            assertFalse(client(server).login("20260001", password()).isSuccess());
            assertTrue(client(server).login(
                    "20260001", "654321".toCharArray()).isSuccess());
        }
    }

    private static ClientContext client(CampusServer server) {
        return new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
    }

    private static char[] password() {
        return "123456".toCharArray();
    }

    private static String proof(String username) {
        return proof(username, "123456");
    }

    private static CreateUserAccountRequest createRequest(
            String username,
            String displayName) {
        return new CreateUserAccountRequest(
                username, displayName, proof(username), Set.of());
    }

    private static String proof(String username, String passwordText) {
        char[] password = passwordText.toCharArray();
        try {
            return PasswordProof.create(username, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
