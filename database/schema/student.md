# 学生学籍模块数据字典

### 1. 模块与负责人
- **模块**：学生学籍管理 (student)
- **对应 Epic**：#1
- **状态**：已评审

### 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
| :--- | :--- | :--- | :--- |
| `tblStudentProfile` | 学生学籍基本档案表 | `studentId` | `userId` 唯一约束（1:1对应用户账号）、`status` 状态枚举 |
| `tblDepartment` | 院系信息字典表 | `deptId` | `deptName` 唯一 |
| `tblMajor` | 专业信息字典表 | `majorId` | 关联 `deptId` |
| `tblClass` | 行政班级信息表 | `classId` | 关联 `majorId` |

### 3. 字段字典

#### (1) tblStudentProfile（学生基本档案表）

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `studentId` | Short Text(20) | 是 | 无 | 主键，一卡通号/学号（如 student001） |
| `userId` | Short Text(20) | 是 | 无 | 逻辑外键，关联用户管理模块的主账号标识（唯一索引） |
| `studentName` | Short Text(50) | 是 | 无 | 学生真实姓名 |
| `gender` | Short Text(4) | 是 | '男' | 性别：男 / 女 |
| `idCard` | Short Text(18) | 是 | 无 | 身份证号（脱敏展示） |
| `birthDate` | Short Text(20) | 否 | 无 | 出生日期（格式：YYYY-MM-DD） |
| `ethnicity` | Short Text(20) | 否 | '汉族' | 民族 |
| `nativePlace` | Short Text(50) | 否 | 无 | 生源地 / 籍贯 |
| `politicalStatus`| Short Text(20) | 否 | '共青团员' | 政治面貌（中共党员/中共预备党员/共青团员/群众） |
| `deptId` | Short Text(20) | 是 | 无 | 逻辑外键，关联 tblDepartment.deptId |
| `majorId` | Short Text(20) | 是 | 无 | 逻辑外键，关联 tblMajor.majorId |
| `classId` | Short Text(20) | 是 | 无 | 逻辑外键，关联 tblClass.classId |
| `enrollmentYear` | Short Text(4) | 是 | '2023' | 入学年份（如：2023） |
| `educationLevel` | Short Text(20) | 是 | '本科生' | 培养层次（本科生 / 硕士研究生 / 博士研究生） |
| `status` | Short Text(20) | 是 | 'ACTIVE' | 学籍状态：ACTIVE(在读)、SUSPENDED(休学)、DROPPED(退学)、GRADUATED(毕业) |

#### (2) tblDepartment（院系表）

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `deptId` | Short Text(20) | 是 | 无 | 主键，院系编号（如 D01） |
| `deptName` | Short Text(50) | 是 | 无 | 院系名称（如 计算机科学与工程学院） |

#### (3) tblMajor（专业表）

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `majorId` | Short Text(20) | 是 | 无 | 主键，专业编号（如 M0101） |
| `deptId` | Short Text(20) | 是 | 无 | 所属院系编号，关联 tblDepartment.deptId |
| `majorName` | Short Text(50) | 是 | 无 | 专业名称（如 软件工程） |

#### (4) tblClass（行政班级表）

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `classId` | Short Text(20) | 是 | 无 | 主键，班级编号（如 C2301） |
| `majorId` | Short Text(20) | 是 | 无 | 所属专业编号，关联 tblMajor.majorId |
| `className` | Short Text(50) | 是 | 无 | 班级全称（如 软工01班） |

### 4. 关联与索引
- **主键索引**：`tblStudentProfile(studentId)`, `tblDepartment(deptId)`, `tblMajor(majorId)`, `tblClass(classId)`。
- **唯一索引**：`tblStudentProfile(userId)`（一账号一档案）, `tblDepartment(deptName)`。
- **检索索引**：`tblStudentProfile(deptId, majorId, classId)` 联合检索。
- **约束策略**：`userId` 逻辑关联用户模块，禁止跨模块物理级联删除；状态变更修改 `status` 字段，禁止随意物理删除。

### 5. 演示数据

**tblDepartment**
- `D01` -> `计算机科学与工程学院`
- `D02` -> `电子科学与工程学院`

**tblMajor**
- `M0101` -> `D01` -> `软件工程`
- `M0201` -> `D02` -> `微电子科学与工程`

**tblClass**
- `C2301` -> `M0101` -> `软工01班`
- `C2202` -> `M0201` -> `微电子02班`

**tblStudentProfile**
- 记录 1（在读本科生）：
    - `studentId`: `student001`, `userId`: `student001`, `studentName`: `张三`, `gender`: `男`, `idCard`: `32010220040101001X`, `birthDate`: `2004-01-01`, `ethnicity`: `汉族`, `nativePlace`: `江苏省南京市`, `politicalStatus`: `共青团员`, `deptId`: `D01`, `majorId`: `M0101`, `classId`: `C2301`, `enrollmentYear`: `2023`, `educationLevel`: `本科生`, `status`: `ACTIVE`
- 记录 2（休学研究生）：
    - `studentId`: `213000001`, `userId`: `213000001`, `studentName`: `李四`, `gender`: `女`, `idCard`: `320102200105120023`, `birthDate`: `2001-05-12`, `ethnicity`: `汉族`, `nativePlace`: `江苏省无锡市`, `politicalStatus`: `中共预备党员`, `deptId`: `D02`, `majorId`: `M0201`, `classId`: `C2202`, `enrollmentYear`: `2022`, `educationLevel`: `硕士研究生`, `status`: `SUSPENDED`

### 6. 待评审问题
1. 第一轮查询响应中院系、专业和班级由服务端直接扁平化为名称输出，待后续评估是否提供独立的字典维护接口。
2. 日期字段暂用 `Short Text(20)` 格式化存储，后续根据 Access/JDBC 方案确定是否调整为原生 Date 类型。
