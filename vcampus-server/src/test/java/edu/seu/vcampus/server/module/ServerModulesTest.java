package edu.seu.vcampus.server.module;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerModulesTest {

    @Test
    void catalogContainsSixUniqueModules() {
        Set<String> ids = ServerModules.modules().stream()
                .map(ServerModule::id)
                .collect(Collectors.toSet());

        assertEquals(6, ServerModules.modules().size());
        assertEquals(6, ids.size());
    }
}
