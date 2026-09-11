package edu.seu.vcampus.server.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDatabaseRebuilderTest {
    @TempDir Path directory;

    @Test
    void replacesOnlyAfterValidationAndBacksUpExistingDatabase() throws Exception {
        Path database = directory.resolve("vCampus.accdb");
        byte[] previousDatabase = "previous demo database".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(database, previousDatabase);

        DemoDatabaseRebuilder.Result result = DemoDatabaseRebuilder.rebuild(database);

        assertEquals(39, result.accountCount());
        assertNotNull(result.backupPath());
        assertTrue(Files.exists(result.backupPath()));
        assertEquals(previousDatabase.length, Files.size(result.backupPath()));
        DemoDatabaseRebuilder.validate(database);
    }
}
