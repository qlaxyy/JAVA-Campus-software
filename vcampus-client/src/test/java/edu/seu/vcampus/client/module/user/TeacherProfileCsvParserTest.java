package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SaveTeacherProfileRequest;
import edu.seu.vcampus.common.user.UserAccountView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherProfileCsvParserTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void mapsCampusCardNumbersToExistingUserIds() throws IOException {
        Path csv = write("\uFEFFcampusCardNumber,department,title\n"
                + "20260001,计算机学院,讲师\n"
                + "20260002,医学院,副教授\n");

        List<SaveTeacherProfileRequest> profiles = TeacherProfileCsvParser.parse(
                csv,
                List.of(account("U-1", "20260001"), account("U-2", "20260002")));

        assertEquals(2, profiles.size());
        assertEquals("U-1", profiles.getFirst().getUserId());
        assertEquals("计算机学院", profiles.getFirst().getDepartment());
        assertTrue(profiles.getFirst().isActive());
    }

    @Test
    void rejectsCampusCardNumberWithoutAnAccount() throws IOException {
        Path csv = write("campusCardNumber,department,title\n"
                + "20269999,计算机学院,讲师\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TeacherProfileCsvParser.parse(csv, List.of(account("U-1", "20260001"))));

        assertTrue(exception.getMessage().contains("不存在于账号名单"));
    }

    private UserAccountView account(String userId, String card) {
        return new UserAccountView(userId, card, "测试账号", Role.USER, Set.of(), true);
    }

    private Path write(String content) throws IOException {
        Path path = temporaryDirectory.resolve("teachers.csv");
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
