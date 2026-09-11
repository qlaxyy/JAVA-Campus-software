# 教师功能接入说明

面向选课和学籍模块负责人。本文只说明教师资格与跨模块协作，具体页面由各模块自行设计。

## 1. 统一规则

- 超级管理员在用户模块维护公共教师名单。
- 选课和学籍模块不再各自保存教师名单。
- 教师身份不能根据姓名、账号或 `userId` 中是否包含 `teacher` 判断。
- 取消教师资格只停用教师档案，不禁用其登录账号。

## 2. 公共教师接口

服务器子系统通过 `ServerContext` 查询：

```java
// 查询一个 userId 是否为有效教师
context.teachers().findByUserId(userId);

// 取得全部有效教师，供选课管理员选择
context.teachers().findActiveTeachers();
```

返回的 `TeacherIdentity` 包含：

```text
userId、campusCardNumber、displayName、department、title
```

客户端查询当前登录账号的教师档案：

```text
Action：USER.CURRENT_TEACHER_PROFILE
请求数据：null
返回数据：TeacherProfileView
```

客户端不提交当前教师的 `userId`。服务器必须通过请求 token 查询 `SessionInfo.userId`。

## 3. 选课模块负责的内容

选课模块维护“教师教哪个教学班”。任课关系建议保存为：

```text
tblOfferingTeacher
- offeringTeacherId
- offeringId
- teacherUserId
```

`(offeringId, teacherUserId)` 应唯一。教师姓名只用于显示，不作为关联字段。

选课管理员添加任课关系时：

1. 调用 `context.teachers().findActiveTeachers()` 取得可选教师；
2. 选择教师和教学班；
3. 将教师 `userId` 写入任课关系表。

教师端目前已有以下网络 Action：

| Action | 请求 DTO | 作用 |
|---|---|---|
| `COURSE.TEACHER_LIST_OFFERINGS` | `BatchRequest(batchId)` | 查看本人负责的教学班 |
| `COURSE.TEACHER_LIST_STUDENTS` | `TeacherListStudentsRequest(batchId, offeringId)` | 查看教学班学生 |
| `COURSE.TEACHER_LIST_GRADES` | `TeacherListStudentsRequest(batchId, offeringId)` | 查看教学班成绩 |
| `COURSE.TEACHER_UPDATE_GRADE` | `AdminUpdateGradeRequest(...)` | 修改本人教学班学生成绩 |

任课关系已经由 `tblOfferingTeacher` 持久化，使用 `teacherUserId` 关联公共教师目录。最终演示数据为 8 名教师各预置至少一个教学班；选课模块后续维护任课关系时继续使用同一张表，`teacherName` 仅作为兼容旧 DTO 的显示快照。

## 4. 学籍模块负责的内容

学籍模块已经实现档案查询、档案修改、异动申请、异动查询和异动审批。

教师查看学生学籍时必须同时满足：

1. `context.teachers().findByUserId(teacherUserId)` 确认教师资格有效；
2. 根据选课模块的任课关系和选课记录，确认目标学生属于该教师的教学班。

教师只能查看规定范围内的学生资料，不能修改学生学籍，也不能查看或审批学籍异动。

“教师只能查看自己教学班学生”的联合鉴权已经接通：学籍服务器会同时校验教师资格、任课关系与学生有效选课记录。

## 5. 分工总结

```text
用户模块：维护谁是教师
选课模块：维护教师、教学班和选课记录之间的关系
学籍模块：根据任课关系限制教师可以查看哪些学生
```
