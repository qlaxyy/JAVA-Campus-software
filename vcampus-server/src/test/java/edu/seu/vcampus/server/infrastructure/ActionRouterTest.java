package edu.seu.vcampus.server.infrastructure;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionRouterTest {

    @Test
    void registeredModuleHandlerReceivesRequest() {
        ActionRouter router = new ActionRouter();
        String action = ActionNames.of(ModuleNames.USER, "EXAMPLE");
        router.register(action, request -> Response.success(request, "ok", "result"));

        Response response = router.dispatch(Request.create(action, null, null));

        assertTrue(response.isSuccess());
        assertEquals("result", response.getData());
    }

    @Test
    void duplicateActionRegistrationIsRejected() {
        ActionRouter router = new ActionRouter();
        String action = ActionNames.of(ModuleNames.COURSE, "EXAMPLE");
        router.register(action, request -> Response.success(request, "first", null));

        assertThrows(IllegalStateException.class,
                () -> router.register(action, request -> Response.success(request, "second", null)));
    }

    @Test
    void handlerFailureReturnsSafeServerError() {
        ActionRouter router = new ActionRouter();
        String action = ActionNames.of(ModuleNames.SHOP, "EXAMPLE");
        router.register(action, request -> {
            throw new IllegalStateException("database details must stay on server");
        });

        Response response = router.dispatch(Request.create(action, null, null));

        assertFalse(response.isSuccess());
        assertEquals(ErrorCodes.COMMON_SERVER_ERROR, response.getCode());
    }
}
