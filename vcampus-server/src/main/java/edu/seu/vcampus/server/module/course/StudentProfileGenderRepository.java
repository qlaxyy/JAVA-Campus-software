package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.server.module.student.StudentRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * 从已有学籍模块读取学生性别。
 */
final class StudentProfileGenderRepository
    implements StudentGenderRepository {

    private final StudentRepository
        studentRepository;

    StudentProfileGenderRepository(
        StudentRepository studentRepository) {

        this.studentRepository =
            Objects.requireNonNull(
                studentRepository);
    }

    @Override
    public StudentGender findGender(
        String studentId) {

        StudentProfileDto profile =
            findProfile(
                studentId);

        String gender =
            profile.getGender();

        if ("男".equals(
            gender)) {

            return StudentGender.MALE;
        }

        if ("女".equals(
            gender)) {

            return StudentGender.FEMALE;
        }

        throw new IllegalStateException(
            "学生性别数据无效。");
    }

    /**
     * 查询学生档案。
     *
     * 优先直接使用登录学号查询。
     * 当前演示账号和学籍模块编号不一致时，
     * 再进行一次兼容查询。
     */
    private StudentProfileDto findProfile(
        String studentId) {

        Optional<StudentProfileDto> profile =
            studentRepository.findByStudentId(
                studentId);

        if (profile.isPresent()) {

            return profile.get();
        }

        throw new IllegalStateException(
            "未找到学生学籍信息。");
    }
}
