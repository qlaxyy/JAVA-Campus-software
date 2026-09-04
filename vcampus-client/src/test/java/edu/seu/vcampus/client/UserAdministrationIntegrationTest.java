package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.BatchCreateUserAccountsRequest;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.UserAccountListResponse;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
    void superAdministratorCanListCreateAndLoginNewAccount() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());

            Response initial = administrator.send(UserActions.ADMIN_LIST_ACCOUNTS, null);
            UserAccountListResponse accounts = assertInstanceOf(
                    UserAccountListResponse.class, initial.getData());
            assertEquals(8, accounts.getAccounts().size());

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
    void superAdministratorCanBatchCreateAccounts() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());

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
            assertTrue(administrator.login("20260003", password()).isSuccess());

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
            assertTrue(administrator.login("20260003", password()).isSuccess());
            assertTrue(student.login("20260001", password()).isSuccess());
            String studentId = student.currentSession().orElseThrow().getUserId();

            Response disabled = administrator.send(
                    UserActions.ADMIN_UPDATE_STATUS,
                    new UpdateUserStatusRequest(studentId, false));
            assertTrue(disabled.isSuccess());

            Response oldSession = student.send(UserActions.CURRENT_SESSION, null);
            assertEquals(ErrorCodes.AUTH_REQUIRED, oldSession.getCode());
            assertFalse(client(server).login("20260001", password()).isSuccess());
        }
    }

    @Test
    void superAdministratorCannotDisableCurrentAccount() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());
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
    void changingScopesInvalidatesOldSessionAndAppliesAtNextLogin() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext administrator = client(server);
            ClientContext target = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());
            assertTrue(target.login("20260005", password()).isSuccess());
            String targetId = target.currentSession().orElseThrow().getUserId();

            Response updated = administrator.send(
                    UserActions.ADMIN_UPDATE_ACCOUNT,
                    new UpdateUserAccountRequest(
                            targetId, "权限已更新", Set.of(AdminScope.HOSPITAL)));

            assertTrue(updated.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED,
                    target.send(UserActions.CURRENT_SESSION, null).getCode());
            assertTrue(target.login("20260005", password()).isSuccess());
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
            assertTrue(administrator.login("20260003", password()).isSuccess());
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
