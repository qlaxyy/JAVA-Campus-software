package edu.seu.vcampus.client.module;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientModulesTest {

    @Test
    void catalogContainsSixUniqueModules() {
        Set<String> ids = ClientModules.all().stream()
                .map(ClientModule::id)
                .collect(Collectors.toSet());

        assertEquals(6, ClientModules.all().size());
        assertEquals(6, ids.size());
    }
}
