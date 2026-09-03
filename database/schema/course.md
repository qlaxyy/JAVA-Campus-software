# 选课系统数据字典

## 1. 模块与负责人

* 模块：选课系统
* 对应 Epic：#3
* 状态：草稿

## 2. 表清单

| 表名                      | 业务含义        | 主键                  | 重要约束              |
| ----------------------- | ----------- | ------------------- | ----------------- |
| `tblCourse`             | 课程基本信息      | `courseId`          | 课程编号唯一            |
| `tblCourseOffering`     | 每学期开设的具体教学班 | `offeringId`        | 同一课程可开多个教学班       |
| `tblCourseSchedule`     | 教学班上课时间     | `scheduleId`        | 支持周次、单双周和时间冲突检测   |
| `tblOfferingTeacher`    | 教学班任课教师     | `offeringTeacherId` | 一个教学班可有多个教师       |
| `tblSelectionBatch`     | 选课批次        | `batchId`           | 记录预选、重修、退改补及开放时间  |
| `tblTrainingPlanCourse` | 培养方案课程      | `planCourseId`      | 记录课程建议修读学期        |
| `tblCourseSubstitution` | 方案外课程替代关系   | `substitutionId`    | 一门方案外课程只替代一门方案内课程 |
| `tblEnrollment`         | 学生选课和退课记录   | `enrollmentId`      | 同一课程只能同时选择一个教学班   |
| `tblGrade`              | 学生成绩        | `gradeId`           | 每条选课记录最多对应一条成绩    |
| `tblCampus`             | 校区信息        | `campusId`          | 校区编号唯一            |
| `tblTeachingLocation`   | 上课地点        | `locationId`        | 地点属于某个校区          |

## 3. 字段字典

### `tblCourse`

| 字段                | Access 类型       | 必填 | 默认值       | 说明       |
| ----------------- | --------------- | -- | --------- | -------- |
| `courseId`        | Long Integer    | 是  | 自动编号      | 主键       |
| `courseCode`      | Short Text(30)  | 是  | 无         | 课程编号，唯一  |
| `courseName`      | Short Text(100) | 是  | 无         | 课程名称     |
| `credits`         | Double          | 是  | 无         | 学分       |
| `totalHours`      | Long Integer    | 是  | 无         | 总学时      |
| `courseType`      | Short Text(20)  | 是  | 无         | 必修、限选、任选 |
| `courseGroup`     | Short Text(20)  | 是  | `REGULAR` | 普通、体育、通选 |
| `generalCategory` | Short Text(40)  | 否  | 无         | 通选课类别    |
| `sportProject`    | Short Text(50)  | 否  | 无         | 体育项目     |
| `departmentName`  | Short Text(100) | 是  | 无         | 开课院系     |
| `status`          | Short Text(20)  | 是  | `ACTIVE`  | 课程状态     |

`courseGroup`：

* `REGULAR` 普通课程
* `PE` 体育课
* `GENERAL` 通选课

---

### `tblCourseOffering`

| 字段                  | Access 类型      | 必填 | 默认值       | 说明         |
| ------------------- | -------------- | -- | --------- | ---------- |
| `offeringId`        | Long Integer   | 是  | 自动编号      | 主键         |
| `courseId`          | Long Integer   | 是  | 无         | 对应课程       |
| `semester`          | Short Text(30) | 是  | 无         | 开课学期       |
| `classNo`           | Short Text(20) | 是  | 无         | 教学班编号      |
| `locationId`        | Long Integer   | 否  | 无         | 上课地点       |
| `teachingLanguage`  | Short Text(20) | 是  | `CHINESE` | 中文、双语、全英文  |
| `genderRestriction` | Short Text(20) | 是  | `ALL`     | 男女均可、仅男、仅女 |
| `capacity`          | Long Integer   | 是  | 无         | 总人数上限      |
| `maleCapacity`      | Long Integer   | 否  | 无         | 男生人数上限     |
| `femaleCapacity`    | Long Integer   | 否  | 无         | 女生人数上限     |
| `status`            | Short Text(20) | 是  | `OPEN`    | 教学班状态      |

普通教学班只使用 `capacity`。

体育教学班可以同时使用：

* `capacity`
* `maleCapacity`
* `femaleCapacity`

以支持男女分别限额。

当前已选人数不保存为字段，由 `tblEnrollment` 动态统计。

---

### `tblOfferingTeacher`

| 字段                  | Access 类型      | 必填 | 默认值  | 说明     |
| ------------------- | -------------- | -- | ---- | ------ |
| `offeringTeacherId` | Long Integer   | 是  | 自动编号 | 主键     |
| `offeringId`        | Long Integer   | 是  | 无    | 对应教学班  |
| `teacherName`       | Short Text(50) | 是  | 无    | 任课教师姓名 |

---

### `tblCourseSchedule`

| 字段            | Access 类型      | 必填 | 默认值     | 说明       |
| ------------- | -------------- | -- | ------- | -------- |
| `scheduleId`  | Long Integer   | 是  | 自动编号    | 主键       |
| `offeringId`  | Long Integer   | 是  | 无       | 对应教学班    |
| `dayOfWeek`   | Integer        | 是  | 无       | 星期几，1-7  |
| `startPeriod` | Integer        | 是  | 无       | 开始节次     |
| `endPeriod`   | Integer        | 是  | 无       | 结束节次     |
| `startWeek`   | Integer        | 是  | 无       | 开始教学周    |
| `endWeek`     | Integer        | 是  | 无       | 结束教学周    |
| `weekPattern` | Short Text(20) | 是  | `EVERY` | 每周、单周、双周 |

`weekPattern`：

* `EVERY`
* `ODD`
* `EVEN`

---

### `tblSelectionBatch`

| 字段            | Access 类型       | 必填 | 默认值  | 说明     |
| ------------- | --------------- | -- | ---- | ------ |
| `batchId`     | Long Integer    | 是  | 自动编号 | 主键     |
| `semester`    | Short Text(30)  | 是  | 无    | 所属学期   |
| `batchName`   | Short Text(100) | 是  | 无    | 批次名称   |
| `batchType`   | Short Text(30)  | 是  | 无    | 批次类型   |
| `startTime`   | Date/Time       | 是  | 无    | 开始时间   |
| `endTime`     | Date/Time       | 是  | 无    | 结束时间   |
| `allowSelect` | Yes/No          | 是  | Yes  | 是否允许选课 |
| `allowDrop`   | Yes/No          | 是  | Yes  | 是否允许退课 |

`batchType`：

* `PRE_SELECTION` 预选课
* `RETAKE` 重修选课
* `ADD_DROP` 退改补

批次状态根据当前时间动态判断，不单独存储。

---

### `tblTrainingPlanCourse`

| 字段                | Access 类型    | 必填 | 默认值  | 说明     |
| ----------------- | ------------ | -- | ---- | ------ |
| `planCourseId`    | Long Integer | 是  | 自动编号 | 主键     |
| `planId`          | Long Integer | 是  | 无    | 培养方案标识 |
| `courseId`        | Long Integer | 是  | 无    | 对应课程   |
| `recommendedTerm` | Integer      | 是  | 无    | 建议修读学期 |

用于方案内课程筛选。

---

### `tblCourseSubstitution`

| 字段                   | Access 类型    | 必填 | 默认值  | 说明        |
| -------------------- | ------------ | -- | ---- | --------- |
| `substitutionId`     | Long Integer | 是  | 自动编号 | 主键        |
| `planCourseId`       | Long Integer | 是  | 无    | 被替代的方案内课程 |
| `substituteCourseId` | Long Integer | 是  | 无    | 方案外替代课程   |

学生界面不直接显示替代关系。

---

### `tblEnrollment`

| 字段                | Access 类型      | 必填 | 默认值        | 说明     |
| ----------------- | -------------- | -- | ---------- | ------ |
| `enrollmentId`    | Long Integer   | 是  | 自动编号       | 主键     |
| `studentId`       | Long Integer   | 是  | 无          | 学生标识   |
| `offeringId`      | Long Integer   | 是  | 无          | 教学班    |
| `selectedBatchId` | Long Integer   | 是  | 无          | 选课所在批次 |
| `status`          | Short Text(20) | 是  | `SELECTED` | 已选或已退  |
| `selectedAt`      | Date/Time      | 是  | 当前时间       | 选课时间   |
| `droppedAt`       | Date/Time      | 否  | 无          | 退课时间   |

`status`：

* `SELECTED`
* `DROPPED`

退课不删除记录。

---

### `tblGrade`

| 字段             | Access 类型    | 必填 | 默认值  | 说明     |
| -------------- | ------------ | -- | ---- | ------ |
| `gradeId`      | Long Integer | 是  | 自动编号 | 主键     |
| `enrollmentId` | Long Integer | 是  | 无    | 对应选课记录 |
| `score`        | Double       | 否  | 无    | 百分制成绩  |
| `recordedAt`   | Date/Time    | 否  | 无    | 成绩录入时间 |

是否及格不单独保存，需要时按 `score >= 60` 判断。

重修资格不要求成绩不及格，只要以前正式修读过该课程即可。

---

### `tblCampus`

| 字段           | Access 类型       | 必填 | 默认值  | 说明   |
| ------------ | --------------- | -- | ---- | ---- |
| `campusId`   | Long Integer    | 是  | 自动编号 | 主键   |
| `campusCode` | Short Text(30)  | 是  | 无    | 校区编号 |
| `campusName` | Short Text(100) | 是  | 无    | 校区名称 |

---

### `tblTeachingLocation`

| 字段             | Access 类型       | 必填 | 默认值  | 说明   |
| -------------- | --------------- | -- | ---- | ---- |
| `locationId`   | Long Integer    | 是  | 自动编号 | 主键   |
| `campusId`     | Long Integer    | 是  | 无    | 所属校区 |
| `locationName` | Short Text(100) | 是  | 无    | 上课地点 |

地点直接保存完整名称，例如“纪忠楼YF101”“九龙湖体育馆”。

## 4. 关联与索引

### 4.1 表关联

* `tblCourseOffering.courseId` → `tblCourse.courseId`
* `tblOfferingTeacher.offeringId` → `tblCourseOffering.offeringId`
* `tblCourseSchedule.offeringId` → `tblCourseOffering.offeringId`
* `tblCourseOffering.locationId` → `tblTeachingLocation.locationId`
* `tblTeachingLocation.campusId` → `tblCampus.campusId`
* `tblTrainingPlanCourse.courseId` → `tblCourse.courseId`
* `tblCourseSubstitution.planCourseId` → `tblTrainingPlanCourse.planCourseId`
* `tblCourseSubstitution.substituteCourseId` → `tblCourse.courseId`
* `tblEnrollment.offeringId` → `tblCourseOffering.offeringId`
* `tblEnrollment.studentId` → 学生模块学生主键
* `tblGrade.enrollmentId` → `tblEnrollment.enrollmentId`

### 4.2 唯一索引

* `tblCourse.courseCode`
* `tblCourseOffering(courseId, semester, classNo)`
* `tblEnrollment(studentId, offeringId)`
* `tblGrade.enrollmentId`
* `tblCampus.campusCode`
* `tblTeachingLocation(campusId, locationName)`
* `tblTrainingPlanCourse(planId, courseId)`

### 4.3 普通索引

* `tblCourseOffering.courseId`
* `tblCourseOffering.semester`
* `tblCourseSchedule.offeringId`
* `tblOfferingTeacher.offeringId`
* `tblEnrollment.studentId`
* `tblEnrollment.offeringId`
* `tblSelectionBatch.semester`

## 5. 服务器主要校验规则

学生选课时服务器需要检查：

1. 当前批次是否处于开放时间；
2. 当前批次是否允许选课；
3. 教学班是否开放；
4. 学生是否具有当前课程的选课资格；
5. 是否已经选择同一课程的其他教学班；
6. 是否达到人数上限；
7. 体育课是否满足性别和人数限制；
8. 是否与当前已选课程时间冲突。

时间冲突需要同时检查：

* 星期
* 节次
* 开始周和结束周
* 单周、双周

学生退课时需要重新检查批次时间和是否允许退课。

## 6. 主要选课规则

* 方案内课程只显示当前培养方案建议学期内且本学期开班的课程。
* 已经历史修读过的普通课程不能通过预选课再次选择。
* 普通课程再次修读必须进入重修批次。
* 重修不要求以前成绩不及格，只要以前修过即可。
* 体育课和通选课不参加重修。
* 同一学生同一时间只能选择同一课程的一个教学班。
* 换教学班必须先退课再重新选择。
* 体育课允许同一学期选择多门，只要不冲突且满足容量限制。
* 通选课允许同一学期选择多门。
* 全校课程查询只提供查询，不允许直接选课。
* 全校课程查询默认每页 20 条，只查询当前学期。

## 7. 演示数据

### `tblSelectionBatch`

| batchId | semester    | batchName         | batchType     | startTime        | endTime          |
| ------- | ----------- | ----------------- | ------------- | ---------------- | ---------------- |
| 1       | 2026-2027-1 | 2026-2027秋季学期预选课  | PRE_SELECTION | 2026-09-01 08:00 | 2026-09-05 23:59 |
| 2       | 2026-2027-1 | 2026-2027秋季学期重修选课 | RETAKE        | 2026-09-06 08:00 | 2026-09-08 23:59 |
| 3       | 2026-2027-1 | 2026-2027秋季学期退改补  | ADD_DROP      | 2026-09-10 08:00 | 2026-09-15 23:59 |

### `tblCourse`

| courseId | courseCode | courseName | credits | courseType | courseGroup |
| -------- | ---------- | ---------- | ------- | ---------- | ----------- |
| 1        | MA101      | 高等数学       | 5.0     | 必修         | REGULAR     |
| 2        | MA201      | 工科数学分析     | 5.0     | 限选         | REGULAR     |
| 3        | PE101      | 羽毛球        | 1.0     | 任选         | PE          |
| 4        | GE101      | 天文学入门      | 2.0     | 任选         | GENERAL     |

## 8. 待联调问题

* 学生模块最终学生主键字段名需要确认。
* 学生性别由学生模块提供，用于体育课资格判断。
* 学生当前培养方案和建议修读学期的获取方式需要与学生模块确认。
* 当前学期的统一字符串格式需要项目组统一，例如 `2026-2027-1`。
