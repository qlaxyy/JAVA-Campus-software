package edu.seu.vcampus.server.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinalDemoRosterTest {

    @Test
    void usesRoleNamesForAdministratorsAndTeamMembersForStudents() {
        assertEquals(List.of(
                        "超级管理员", "学籍管理员", "选课管理员",
                        "图书馆管理员", "商店管理员", "医院管理员"),
                FinalDemoRoster.accounts().subList(0, 6).stream()
                        .map(FinalDemoRoster.AccountSeed::displayName)
                        .toList());

        assertEquals(List.of(
                        "吴尚扬", "施天琦", "杨凯涵", "吴昊哲", "葛丰玮", "廖俊杰",
                        "周一", "周二", "周三", "周四", "周五", "周六", "周七", "周八", "周九"),
                FinalDemoRoster.students().stream()
                        .map(FinalDemoRoster.AccountSeed::displayName)
                        .toList());
    }
}
