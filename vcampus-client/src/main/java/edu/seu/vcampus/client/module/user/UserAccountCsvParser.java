package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.BatchCreateUserAccountsRequest;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.PasswordProof;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Reads the documented UTF-8 account-import CSV format. */
final class UserAccountCsvParser {

    private static final String USERNAME_HEADER = "campusCardNumber";
    private static final String DISPLAY_NAME_HEADER = "displayName";

    private UserAccountCsvParser() {
    }

    static List<CreateUserAccountRequest> parse(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件为空。");
        }
        validateHeader(lines.getFirst());
        List<CreateUserAccountRequest> accounts = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            if (accounts.size() >= BatchCreateUserAccountsRequest.MAX_ACCOUNTS) {
                throw new IllegalArgumentException("一次最多导入 "
                        + BatchCreateUserAccountsRequest.MAX_ACCOUNTS + " 个账号。");
            }
            List<String> fields = parseLine(line, index + 1);
            accounts.add(createAccount(fields, index + 1));
        }
        if (accounts.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件没有账号数据。");
        }
        return accounts;
    }

    private static void validateHeader(String line) {
        String normalized = line.startsWith("\uFEFF") ? line.substring(1) : line;
        List<String> fields = parseLine(normalized, 1);
        if (fields.size() != 2
                || !USERNAME_HEADER.equalsIgnoreCase(fields.get(0).trim())
                || !DISPLAY_NAME_HEADER.equalsIgnoreCase(fields.get(1).trim())) {
            throw new IllegalArgumentException(
                    "CSV 第一行必须是 campusCardNumber,displayName。");
        }
    }

    private static CreateUserAccountRequest createAccount(List<String> fields, int lineNumber) {
        if (fields.size() != 2) {
            throw new IllegalArgumentException("CSV 第 " + lineNumber + " 行必须有两列。");
        }
        String username = fields.get(0).trim();
        char[] password = "123456".toCharArray();
        try {
            return new CreateUserAccountRequest(
                    username,
                    fields.get(1).trim(),
                    PasswordProof.create(username, password),
                    Set.of());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "CSV 第 " + lineNumber + " 行无效：" + exception.getMessage(), exception);
        } finally {
            Arrays.fill(password, '\0');
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
