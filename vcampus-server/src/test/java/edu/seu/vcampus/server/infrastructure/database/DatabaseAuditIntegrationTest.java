package edu.seu.vcampus.server.infrastructure.database;

import edu.seu.vcampus.common.protocol.*;
import edu.seu.vcampus.common.shop.AddCategoryRequest;
import edu.seu.vcampus.common.user.*;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseAuditIntegrationTest {
    @TempDir Path directory;

    @Test void subsystemWritesAppearInSuperAdminAuditWithAuthoritativeActorAndSurviveRestart() {
        Path path = directory.resolve("integration.accdb");
        ActionRouter router = ServerModules.createPersistentRouter(path);
        SessionInfo shop = login(router, "20260004");
        SessionInfo administrator = login(router, "20260000");
        Request creation = Request.create("SHOP.ADD_CATEGORY", shop.getToken(),
                new AddCategoryRequest("审计验收分类"));
        assertTrue(router.dispatch(creation).isSuccess());
        UserAuditLogResponse response = assertInstanceOf(UserAuditLogResponse.class,
                router.dispatch(Request.create(UserActions.ADMIN_LIST_AUDIT_LOGS,
                        administrator.getToken(), null)).getData());
        var entry = response.getEntries().stream()
                .filter(item -> item.getDetail().contains(creation.getRequestId()))
                .filter(item -> item.getTarget().equals("tblShopCategory"))
                .findFirst().orElseThrow();
        assertEquals("DATABASE.INSERT", entry.getActionCode());
        assertEquals(shop.getUserId(), entry.getActorUserId());
        assertEquals(shop.getUsername(), entry.getActorUsername());
        assertEquals(shop.getDisplayName(), entry.getActorDisplayName());
        assertFalse(entry.getDetail().contains("审计验收分类"));
        Response denied = router.dispatch(Request.create(UserActions.ADMIN_LIST_AUDIT_LOGS,
                shop.getToken(), null));
        assertEquals(ErrorCodes.AUTH_FORBIDDEN, denied.getCode());
        ActionRouter restarted = ServerModules.createPersistentRouter(path);
        SessionInfo afterRestart = login(restarted, "20260000");
        UserAuditLogResponse persisted = assertInstanceOf(UserAuditLogResponse.class,
                restarted.dispatch(Request.create(UserActions.ADMIN_LIST_AUDIT_LOGS,
                        afterRestart.getToken(), null)).getData());
        assertTrue(persisted.getEntries().stream().anyMatch(item -> item.getAuditId().equals(entry.getAuditId())));
    }

    private static SessionInfo login(ActionRouter router, String number) {
        Response response = router.dispatch(Request.create(UserActions.LOGIN, null,
                new LoginRequest(number, PasswordProof.create(number, "123456".toCharArray()))));
        assertTrue(response.isSuccess());
        return assertInstanceOf(SessionInfo.class, response.getData());
    }
}
