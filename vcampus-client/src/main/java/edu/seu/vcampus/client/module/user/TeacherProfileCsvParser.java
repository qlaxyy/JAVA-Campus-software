package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.SaveTeacherProfileRequest;
import edu.seu.vcampus.common.user.UserAccountView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the UTF-8 teacher-import CSV format. */
final class TeacherProfileCsvParser {

    private TeacherProfileCsvParser() {
    }

    static List<SaveTeacherProfileRequest> parse(
            Path path,
            List<UserAccountView> accounts) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件为空。");
        }
        validateHeader(lines.getFirst());
        Map<String, UserAccountView> accountsByCard = new HashMap<>();
        for (UserAccountView account : accounts) {
            accountsByCard.put(account.getUsername(), account);
        }
        List<SaveTeacherProfileRequest> teachers = new ArrayList<>();
        Set<String> importedCards = new HashSet<>();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isBlank()) {
                continue;
            }
            int lineNumber = index + 1;
            List<String> fields = parseLine(lines.get(index), lineNumber);
            if (fields.size() != 3) {
                throw new IllegalArgumentException("CSV 第 " + lineNumber + " 行必须有三列。");
            }
            String card = fields.get(0).trim();
            UserAccountView account = accountsByCard.get(card);
            if (account == null) {
                throw new IllegalArgumentException(
                        "CSV 第 " + lineNumber + " 行的一卡通号 " + card + " 不存在于账号名单。");
            }
            if (!importedCards.add(card)) {
                throw new IllegalArgumentException("CSV 中的一卡通号 " + card + " 重复。");
            }
            try {
                teachers.add(new SaveTeacherProfileRequest(
                        account.getUserId(), fields.get(1), fields.get(2), true));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "CSV 第 " + lineNumber + " 行无效：院系和职称不能为空。", exception);
            }
        }
        if (teachers.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件没有教师数据。");
        }
        return teachers;
    }

    private static void validateHeader(String line) {
        String normalized = line.startsWith("\uFEFF") ? line.substring(1) : line;
        List<String> fields = parseLine(normalized, 1);
        if (fields.size() != 3
                || !"campusCardNumber".equalsIgnoreCase(fields.get(0).trim())
                || !"department".equalsIgnoreCase(fields.get(1).trim())
                || !"title".equalsIgnoreCase(fields.get(2).trim())) {
            throw new IllegalArgumentException(
                    "CSV 第一行必须是 campusCardNumber,department,title。");
        }
    }

    private static List<String> parseLine(String line, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("CSV 第 " + lineNumber + " 行引号未闭合。");
        }
        fields.add(field.toString());
        return fields;
    }
}
