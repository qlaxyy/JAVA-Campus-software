# 学生学籍管理系统数据字典

## 1. 模块与负责人
- **模块**：学生学籍管理系统
- **对应 Epic**：#1
- **状态**：已对齐现有实现（代码基准）

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
| :--- | :--- | :--- | :--- |
| tblStudent | 学生核心学籍与身份档案 | studentId | 唯一业务学号，全系统学生基础标识 |
| tblStudentProfile | 学生自维扩展与联络档案 | profileId | 一个学生仅一条自维记录，支持本人/管理员维护 |
| tblStatusChange | 学籍异动履历记录 | changeId | 记录休学、复学、转专业、退学等历史 |

## 3. 字段字典

### tblStudent（核心学籍与身份信息，服务端受控只读）

| 字段 | Access 类型 | 必填 | 默认值 | 对应代码/DTO字段 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| studentId | Long Integer | 是 | 自动编号 | id | 物理主键，对应选课模块 `tblEnrollment.studentId` |
| studentNo | Short Text(30) | 是 | 无 | studentId | 业务学号（如 `student001`），全校唯一 |
| studentName | Short Text(50) | 是 | 无 | name | 学生真实姓名 |
| gender | Short Text(10) | 是 | 无 | gender | 性别（`男` / `女` 或 `MALE` / `FEMALE`），供体育课校验 |
| ethnicity | Short Text(30) | 否 | 汉族 | ethnicity | 民族 |
| nativePlace | Short Text(100) | 否 | 无 | nativePlace | 籍贯 |
| idCardNumber | Short Text(30) | 是 | 无 | idCardNumber | 居民身份证号，唯一 |
| birthDate | Short Text(20) | 否 | 无 | birthDate | 出生日期（格式：`YYYY-MM-DD`） |
| enrollmentDate | Short Text(20) | 是 | 无 | enrollmentDate | 入学日期（格式：`YYYY-MM-DD`） |
| enrollmentYear | Integer | 是 | 2024 | enrollmentYear | 入学年份（如 `2024`） |
| departmentName | Short Text(100) | 是 | 无 | department | 所属院系（与选课模块开课院系对齐） |
| majorName | Short Text(100) | 是 | 无 | major | 专业名称（如“软件工程”） |
| className | Short Text(50) | 是 | 无 | className | 行政班级（如“软件工程2401班”） |
| schoolingYears | Integer | 是 | 4 | schoolingLength | 基本学制年限（默认 4 年） |
| planId | Long Integer | 是 | 1 | planId | 培养方案标识，对应选课模块 `tblTrainingPlanCourse.planId` |
| currentTerm | Integer | 是 | 1 | currentTerm | 当前修读建议学期（1-8），用于方案内选课筛选 |
| campusId | Long Integer | 是 | 1 | campusId | 就读校区标识，对应选课模块 `tblCampus.campusId` |
| academicStatus | Short Text(20) | 是 | 在读 | academicStatus | 学籍状态：`在读`、`休学`、`退学`、`毕业` |

### tblStudentProfile（联络与补充档案，支持自主维护）

| 字段 | Access 类型 | 必填 | 默认值 | 对应代码/DTO字段 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| profileId | Long Integer | 是 | 自动编号 | profileId | 主键，自增标识 |
| studentId | Long Integer | 是 | 无 | studentId | 外键，关联 `tblStudent.studentId`，唯一 |
| politicalStatus | Short Text(30) | 否 | 共青团员 | politicalStatus | 政治面貌（支持自维：群众、共青团员、中共党员等） |
| phone | Short Text(30) | 否 | 无 | phone | 移动联系电话（支持学生自主维护） |
| email | Short Text(100) | 否 | 无 | email | 电子邮箱（支持学生自主维护） |
| address | Short Text(255) | 否 | 无 | homeAddress | 家庭现居住通讯地址（支持学生自主维护） |
| emergencyContact | Short Text(50) | 否 | 无 | emergencyContact | 紧急联系人姓名（支持学生自主维护） |
| emergencyPhone | Short Text(30) | 否 | 无 | emergencyPhone | 紧急联系人电话（支持学生自主维护） |
| updatedAt | Date/Time | 是 | 当前时间 | updatedAt | 档案最近维护更新时间 |

### tblStatusChange（学籍异动履历记录）

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| changeId | Long Integer | 是 | 自动编号 | 主键，自增标识 |
| studentId | Long Integer | 是 | 无 | 外键，关联 `tblStudent.studentId` |
| changeType | Short Text(30) | 是 | 无 | 异动类别：`休学`、`复学`、`转专业`、`退学` |
| changeDate | Date/Time | 是 | 当前时间 | 异动生效时间 |
| reason | Short Text(255) | 否 | 无 | 异动原因说明 |
| operator | Short Text(50) | 是 | 无 | 审核操作人员账号（如管理员 `studentadmin`） |

## 4. 关联与索引

### 4.1 表关联
- `tblStudentProfile.studentId` → `tblStudent.studentId`
- `tblStatusChange.studentId` → `tblStudent.studentId`
- **跨模块关联支持**：
    - `tblEnrollment.studentId` → `tblStudent.studentId`（选课模块核心外键）
    - `tblStudent.campusId` → `tblCampus.campusId`（校区归属）
    - `tblStudent.planId` → `tblTrainingPlanCourse.planId`（选课培养方案课程筛选）

### 4.2 唯一索引
- `tblStudent.studentNo`
- `tblStudent.idCardNumber`
- `tblStudentProfile.studentId`

### 4.3 普通索引
- `tblStudent.planId`
- `tblStudent.academicStatus`
- `tblStudent.majorName`
- `tblStatusChange.studentId`

## 5. 跨模块选课联调确认项
1. **学生主键与标识对齐**：
    - 数据表物理主键为 `tblStudent.studentId`（Long Integer），对应选课表 `tblEnrollment.studentId`。
    - 业务和登录标识使用 `studentNo`（如 `student001`，登录会话规范化后的 `U-STUDENT-001`）。
2. **体育课限额判定**：
    - `tblStudent.gender` 提供性别标识，完全匹配选课教学班的容量与性别限制。
3. **培养方案建议学期**：
    - `tblStudent` 统一提供 `planId` 与 `currentTerm`（1-8），选课端可直接过滤方案内本学期开班的建议课程。
4. **统一学期命名**：
    - 全系统各模块统一使用 `2026-2027-1` 格式标识当前学期。

## 6. 演示数据（与内存库当前初始数据一致）

### tblStudent
| studentId | studentNo | studentName | gender | departmentName | majorName | className | schoolingYears | planId | currentTerm | campusId | academicStatus |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | student001 | 张三 | 男 | 计算机科学与工程学院 | 软件工程 | 软件工程2401班 | 4 | 1 | 1 | 1 | 在读 |
| 2 | student002 | 李四 | 女 | 计算机科学与工程学院 | 软件工程 | 软件工程2401班 | 4 | 1 | 1 | 1 | 在读 |

### tblStudentProfile
| profileId | studentId | politicalStatus | phone | email | address | emergencyContact | emergencyPhone |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 1 | 共青团员 | 13800138000 | student001@seu.edu.cn | 江苏省南京市江宁区东南大学九龙湖校区 | 张父 | 13900139000 |
| 2 | 2 | 群众 | 13800138001 | student002@seu.edu.cn | 江苏省南京市江宁区东南大学九龙湖校区 | 李母 | 13900139001 |
