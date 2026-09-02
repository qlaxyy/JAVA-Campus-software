package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.server.module.student.StudentMemoryRepository;

import java.util.Objects;

/**
 * 从已有学籍模块读取学生性别。
 */
final class StudentProfileGenderRepository
    implements StudentGenderRepository {

    private final StudentMemoryRepository
        studentRepository;

    StudentProfileGenderRepository(
        StudentMemoryRepository studentRepository) {

        this.studentRepository =
            Objects.requireNonNull(
                studentRepository);
    }

    @Override
    public StudentGender findGender(
        String studentId) {

        StudentProfileDto profile =
            studentRepository
                .findById(
                    studentId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "未找到学生学籍信息。"));

        String gender =
            profile.getGender();

        if ("男".equals(gender)) {

            return StudentGender.MALE;
        }

        if ("女".equals(gender)) {

            return StudentGender.FEMALE;
        }

        throw new IllegalStateException(
            "学生性别数据无效。");
    }
}
