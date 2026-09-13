package edu.seu.vcampus.common.user;

import edu.seu.vcampus.common.protocol.ModuleNames;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminScopeTest {

    @Test
    void resolvesCanonicalAndLegacyModuleIdCasing() {
        assertEquals(Optional.of(AdminScope.STUDENT),
                AdminScope.fromModuleId(ModuleNames.STUDENT));
        assertEquals(Optional.of(AdminScope.STUDENT),
                AdminScope.fromModuleId("student"));
        assertEquals(Optional.of(AdminScope.STUDENT),
                AdminScope.fromModuleId("  Student  "));
    }

    @Test
    void rejectsMissingOrUnknownModuleIds() {
        assertTrue(AdminScope.fromModuleId(null).isEmpty());
        assertTrue(AdminScope.fromModuleId("  ").isEmpty());
        assertTrue(AdminScope.fromModuleId("UNKNOWN").isEmpty());
    }
}
