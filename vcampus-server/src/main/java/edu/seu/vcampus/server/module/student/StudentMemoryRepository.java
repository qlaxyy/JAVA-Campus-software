package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.StudentProfileDto;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临时学籍内存数据源
 * TODO: 后续接入 Access/JDBC 后由 StudentDao 替换
 */
public class StudentMemoryRepository {
    private final Map<String, StudentProfileDto> database = new ConcurrentHashMap<>();

    public StudentMemoryRepository() {
        database.put("student001", new StudentProfileDto(
            "student001", "张三", "男", "32010220040101001X",
            "2004-01-01", "汉族", "江苏省南京市", "共青团员",
            "计算机科学与工程学院", "软件工程", "软工01班",
            "2023", "本科生", "在读"
        ));
        database.put("213000001", new StudentProfileDto(
            "213000001", "李四", "女", "320102200105120023",
            "2001-05-12", "汉族", "江苏省无锡市", "中共预备党员",
            "电子科学与工程学院", "微电子科学与工程", "微电子02班",
            "2022", "硕士研究生", "休学"
        ));
    }

    public Optional<StudentProfileDto> findById(String studentId) {
        if (studentId == null) return Optional.empty();
        return Optional.ofNullable(database.get(studentId.trim()));
    }
}
