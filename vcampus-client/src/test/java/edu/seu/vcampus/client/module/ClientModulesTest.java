package edu.seu.vcampus.client.module;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientModulesTest {

    @Test
    void catalogContainsUniqueModules() {
        Set<String> ids = ClientModules.all().stream()
                .map(ClientModule::id)
                .collect(Collectors.toSet());

        assertEquals(7, ClientModules.all().size());
        assertEquals(7, ids.size());
    }
}
