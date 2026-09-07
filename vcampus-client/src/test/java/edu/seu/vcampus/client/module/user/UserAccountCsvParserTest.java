package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountCsvParserTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsUtf8RowsAndQuotedComma() throws IOException {
        Path csv = write("\uFEFFcampusCardNumber,displayName\n"
                + "20261001,张同学\n"
                + "20261002,\"李同学,二班\"\n");

        List<CreateUserAccountRequest> accounts = UserAccountCsvParser.parse(csv);

        assertEquals(2, accounts.size());
        assertEquals("20261001", accounts.get(0).getUsername());
        assertEquals("李同学,二班", accounts.get(1).getDisplayName());
    }

    @Test
    void reportsInvalidRowNumber() throws IOException {
        Path csv = write("campusCardNumber,displayName\ninvalid-name,测试\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UserAccountCsvParser.parse(csv));

        assertTrue(exception.getMessage().contains("第 2 行"));
    }

    private Path write(String content) throws IOException {
        Path path = temporaryDirectory.resolve("accounts.csv");
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
