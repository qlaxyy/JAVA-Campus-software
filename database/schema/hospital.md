# 校医院模块数据字典

> 对应 Epic：[#4 医院模块：医生排班与预约](https://github.com/qlaxyy/JAVA-Campus-software/issues/4)
>
> 状态：第一版设计草案，待数据库集成人评审
>
> 设计日期：2026-08-26

## 1. 文档用途和边界

本文档定义医院模块拥有的数据表、字段、关系、状态、索引、约束和虚构演示数据。第一版只设计 Epic #4 核心需要的四张表：科室、医生、排班/号源和预约。

当前只提交文本数据字典，不创建或提交个人电脑上的二进制 `.accdb` 文件。最终 `vCampus.accdb` 由数据库集成人根据评审后的数据字典统一生成。

医院模块允许引用公共用户的稳定 `userId`，但不复制或更新用户模块的账号、密码、姓名和角色数据。本数据字典不保存真实病历、处方、支付或个人隐私。

## 2. 零基础概念说明

### 2.1 表、行和字段

数据库中的表类似有严格规则的表格：

```text
tblHospitalDepartment
┌──────────────┬────────────────┬────────┐
│ departmentId │ departmentName │ status │  ← 字段/列
├──────────────┼────────────────┼────────┤
│ DEP-001      │ 内科           │ ACTIVE │  ← 一行记录
│ DEP-002      │ 外科           │ ACTIVE │
└──────────────┴────────────────┴────────┘
```

- 表：保存同一类数据，例如全部科室。
- 字段/列：描述这一类数据有哪些属性，例如科室名称。
- 行/记录：一个具体对象，例如“内科”这一条科室记录。
- 数据类型：限制字段能保存什么，例如文本、整数或日期时间。

### 2.2 主键

主键是每一行独一无二的标识。例如两个医生可能同名，但 `doctorId` 必须不同：

```text
DOC-001  张医生
DOC-002  张医生
```

程序使用 ID 找记录，不依赖可能重复或变化的显示名称。

### 2.3 外键

外键用一个表中的 ID 引用另一个表。例如医生记录中的 `departmentId = DEP-001` 表示该医生属于内科：

```text
tblHospitalDepartment.departmentId
               ↑
               └── tblHospitalDoctor.departmentId
```

外键防止出现“医生属于一个根本不存在的科室”。跨模块的 `userId` 先记录为逻辑外键，医院模块只能引用，不能更新用户表。

### 2.4 约束和索引

- 约束：数据库或服务器必须遵守的规则，例如容量必须大于0。
- 唯一索引：保证某个值或字段组合不能重复。
- 普通索引：帮助数据库更快找到数据，不要求唯一。
- 状态字段：使用 `ACTIVE/CLOSED/CANCELLED` 等值保留历史记录，避免随意物理删除。

## 3. 表清单和关系

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblHospitalDepartment` | 校医院科室 | `departmentId` | 科室代码唯一；使用状态停用 |
| `tblHospitalDoctor` | 医生资料和可选用户绑定 | `doctorId` | 医生代码唯一；所属科室必须存在 |
| `tblHospitalSchedule` | 某医生在某科室的一段排班及其容量 | `scheduleId` | 时间合法；容量不低于已预约数；同一医生排班不能重叠 |
| `tblHospitalAppointment` | 患者对某条排班的预约 | `appointmentId` | 患者和排班必须存在；状态合法；重复预约由服务器事务拒绝 |

关系图：

```text
tblHospitalDepartment
        1
        │
        ├──────── N tblHospitalDoctor
        │                    1
        │                    │
        └────────────── N tblHospitalSchedule
                                  1
                                  │
                                  N
                       tblHospitalAppointment N ─── 1 公共用户 userId
```

通俗理解：

```text
一个科室可以有多名医生
一名医生可以有多条排班
一条排班可以有多条预约
一个用户可以有多条预约
```

## 4. 字段字典

### 4.1 `tblHospitalDepartment`：科室

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `departmentId` | Short Text(36) | 是 | 无 | 主键；程序稳定 ID，例如 `DEP-001` 或 UUID |
| `departmentCode` | Short Text(20) | 是 | 无 | 业务代码，例如 `INTERNAL`；唯一 |
| `departmentName` | Short Text(50) | 是 | 无 | 页面显示名称，例如“内科”；第一版要求唯一 |
| `description` | Long Text | 否 | `null` | 科室简介，不保存医疗诊断规则 |
| `status` | Short Text(20) | 是 | `ACTIVE` | `ACTIVE` 或 `INACTIVE` |
| `createdAt` | Date/Time | 是 | 新建时间 | 创建时间 |
| `updatedAt` | Date/Time | 是 | 新建时间 | 最近更新时间 |

业务规则：

- `departmentCode`、`departmentName` 去除首尾空格后不能为空。
- 已被医生或排班引用的科室不物理删除；停用时改为 `INACTIVE`。
- 停用科室不能创建新排班；已有历史记录保留。

### 4.2 `tblHospitalDoctor`：医生

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `doctorId` | Short Text(36) | 是 | 无 | 主键；稳定医生 ID |
| `doctorCode` | Short Text(20) | 是 | 无 | 医院内部医生代码；唯一 |
| `userId` | Short Text(64) | 否 | `null` | 逻辑外键，引用公共用户；用于医生登录绑定 |
| `departmentId` | Short Text(36) | 是 | 无 | 外键，引用 `tblHospitalDepartment.departmentId` |
| `doctorName` | Short Text(50) | 是 | 无 | 页面显示姓名；允许不同医生同名 |
| `doctorTitle` | Short Text(50) | 否 | 空字符串 | 页面显示职称，例如“主治医师” |
| `introduction` | Long Text | 否 | `null` | 简短介绍；不得包含真实敏感信息 |
| `status` | Short Text(20) | 是 | `ACTIVE` | `ACTIVE` 或 `INACTIVE` |
| `createdAt` | Date/Time | 是 | 新建时间 | 创建时间 |
| `updatedAt` | Date/Time | 是 | 新建时间 | 最近更新时间 |

业务规则：

- `userId` 可以暂时为空，表示医生尚未绑定登录账号。
- 非空 `userId` 在医院医生表中只能绑定一个医生；具体 Access 唯一空值行为待数据库实验确认，服务器始终再次校验。
- 不能仅凭公共角色为 `TEACHER` 就认定是医生；医生模式还必须找到状态为 `ACTIVE` 的绑定记录。
- 停用医生不能创建新排班，但历史排班和预约保留。
- 医生姓名不是主键，也不要求唯一。

### 4.3 `tblHospitalSchedule`：排班和号源

第一版把“一名医生在一段时间内可接诊若干人”保存为一行，而不是为每个名额建立一行。例如容量为5表示这一段排班最多接受5条非取消预约。

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `scheduleId` | Short Text(36) | 是 | 无 | 主键；对应 `SlotView.scheduleId` |
| `departmentId` | Short Text(36) | 是 | 无 | 外键，引用排班发生的科室 |
| `doctorId` | Short Text(36) | 是 | 无 | 外键，引用 `tblHospitalDoctor.doctorId` |
| `startTime` | Date/Time | 是 | 无 | 开始时间 |
| `endTime` | Date/Time | 是 | 无 | 结束时间，必须晚于开始时间 |
| `capacity` | Long Integer | 是 | 无 | 总容量，必须大于0 |
| `bookedCount` | Long Integer | 是 | `0` | 已占用数量，范围为0到capacity |
| `status` | Short Text(20) | 是 | `DRAFT` | `DRAFT`、`PUBLISHED` 或 `CLOSED` |
| `createdAt` | Date/Time | 是 | 新建时间 | 创建时间 |
| `updatedAt` | Date/Time | 是 | 新建时间 | 最近更新时间 |

状态含义：

| 数据库状态 | 是否出现在普通查询 | 对应 `SlotAvailability` |
|---|---:|---|
| `DRAFT` | 否 | 不返回客户端 |
| `PUBLISHED` 且 `bookedCount < capacity` | 是 | `AVAILABLE` |
| `PUBLISHED` 且 `bookedCount = capacity` | 是 | `FULL` |
| `CLOSED` | 是，可用于说明已关闭 | `CLOSED` |

业务规则：

- `endTime` 必须晚于 `startTime`。
- `capacity > 0`。
- `0 <= bookedCount <= capacity`。
- 管理员不能把容量调整到小于 `bookedCount`。
- 同一医生的排班不能发生时间重叠；Access 普通唯一索引无法判断时间区间重叠，服务器保存前必须检查。
- 同一医生、同一开始时间不允许出现两条排班。
- 排班的 `departmentId` 必须等于该医生记录的 `departmentId`，不能把医生排入其他科室而不先更新并评审医生归属。
- 有预约的排班不能物理删除；可以关闭，但不能静默改变医生和时间。
- `bookedCount` 统计 `BOOKED` 和 `COMPLETED` 预约；取消 `BOOKED` 预约时减1，完成预约时不改变。
- 创建预约时，“检查剩余量、写预约、bookedCount加1”必须在同一个服务器临界区或数据库事务中完成。

### 4.4 `tblHospitalAppointment`：预约

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `appointmentId` | Short Text(36) | 是 | 无 | 主键；稳定预约 ID |
| `scheduleId` | Short Text(36) | 是 | 无 | 外键，引用 `tblHospitalSchedule.scheduleId` |
| `patientUserId` | Short Text(64) | 是 | 无 | 逻辑外键，引用当前登录用户 `userId` |
| `status` | Short Text(20) | 是 | `BOOKED` | `BOOKED`、`COMPLETED` 或 `CANCELLED` |
| `createdAt` | Date/Time | 是 | 新建时间 | 预约创建时间 |
| `cancelledAt` | Date/Time | 否 | `null` | 合法取消时间 |
| `completedAt` | Date/Time | 否 | `null` | 医生完成接诊的时间；P1 启用 |
| `cancellationReason` | Short Text(200) | 否 | `null` | 可选取消原因，不保存敏感病情描述 |

状态流转：

```text
BOOKED ─────→ COMPLETED
   │
   └────────→ CANCELLED
```

业务规则：

- 创建预约时 `patientUserId` 必须由服务器从 token 会话读取，不能信任客户端上传的用户 ID。
- 一个患者不能同时拥有同一排班的另一条 `BOOKED` 或 `COMPLETED` 预约。
- 第一版允许服务器在合法取消后决定是否允许重新预约；Access 不支持所有数据库的条件唯一索引，最终方案待并发实验确认。
- 只能从 `BOOKED` 取消或完成；`COMPLETED` 和 `CANCELLED` 不允许再次直接变更。
- 取消预约与 `bookedCount` 减1必须在同一个事务或临界区完成，失败时两者都不修改。
- 预约记录不物理删除，用状态保存过程证据。
- `cancelledAt` 只在 `CANCELLED` 时非空；`completedAt` 只在 `COMPLETED` 时非空。

## 5. 关联、索引与删除策略

### 5.1 外键关系

| 本表字段 | 引用目标 | 删除/更新策略 |
|---|---|---|
| `tblHospitalDoctor.departmentId` | `tblHospitalDepartment.departmentId` | 禁止级联删除；使用科室状态停用 |
| `tblHospitalSchedule.departmentId` | `tblHospitalDepartment.departmentId` | 禁止级联删除；历史排班保留 |
| `tblHospitalSchedule.doctorId` | `tblHospitalDoctor.doctorId` | 禁止级联删除；使用医生状态停用 |
| `tblHospitalAppointment.scheduleId` | `tblHospitalSchedule.scheduleId` | 禁止级联删除；预约历史保留 |
| `tblHospitalDoctor.userId` | 公共用户 `userId` | 逻辑引用；医院模块不得更新用户表 |
| `tblHospitalAppointment.patientUserId` | 公共用户 `userId` | 逻辑引用；医院模块不得更新用户表 |

### 5.2 建议索引

| 表 | 索引字段 | 类型 | 用途 |
|---|---|---|---|
| `tblHospitalDepartment` | `departmentCode` | 唯一 | 防止科室代码重复 |
| `tblHospitalDepartment` | `departmentName` | 唯一 | 第一版防止显示名称重复 |
| `tblHospitalDepartment` | `status` | 普通 | 查询有效科室 |
| `tblHospitalDoctor` | `doctorCode` | 唯一 | 防止医生代码重复 |
| `tblHospitalDoctor` | `userId` | 条件唯一/服务器校验 | 防止一个用户绑定多个医生身份 |
| `tblHospitalDoctor` | `(departmentId, status)` | 组合普通 | 按科室查询有效医生 |
| `tblHospitalSchedule` | `(doctorId, startTime)` | 组合唯一 | 防止同一医生同一开始时间重复排班 |
| `tblHospitalSchedule` | `(departmentId, startTime, status)` | 组合普通 | 按科室和日期查询号源 |
| `tblHospitalAppointment` | `(patientUserId, status)` | 组合普通 | 查询“我的预约” |
| `tblHospitalAppointment` | `(scheduleId, status)` | 组合普通 | 查询排班预约并检查容量 |

物理删除只允许用于尚未被引用且确认是误建的草稿数据；已产生排班、预约或历史关系的数据一律通过状态停用/关闭/取消。

## 6. 查询号源怎样由四张表得到

第一条 `HOSPITAL.SEARCH_SLOTS` 查询主要组合三张表：

```text
tblHospitalSchedule
    │ doctorId
    ├────────→ tblHospitalDoctor
    │
    │ departmentId
    └────────→ tblHospitalDepartment
```

对应 DTO 字段来源：

| `SlotView` 字段 | 数据来源/计算 |
|---|---|
| `scheduleId` | `tblHospitalSchedule.scheduleId` |
| `departmentId/name` | Schedule 的 departmentId + Department 的 departmentName |
| `doctorId/name/title` | Schedule 的 doctorId + Doctor 的 name/title |
| `startTime/endTime` | `tblHospitalSchedule` |
| `capacity` | `tblHospitalSchedule.capacity` |
| `remaining` | `capacity - bookedCount`，由服务器计算 |
| `availability` | 根据 Schedule status、capacity、bookedCount 转换 |

`tblHospitalAppointment` 在第一条纯查询中不必逐行返回，但预约/取消功能会写入它，并与 `bookedCount` 一致更新。

## 7. 并发与一致性策略

### 7.1 为什么不能只“先查后写”

假设只剩一个号源，两个客户端几乎同时查询，都可能看到 `remaining = 1`。如果两边随后都直接创建预约，就会超过容量。

错误流程：

```text
客户端A查到剩1 ─┐
                 ├─→ A、B都预约成功，产生超卖
客户端B查到剩1 ─┘
```

正确策略：服务器处理预约时重新检查，并把以下操作作为一个不可分割的整体：

```text
检查会话和重复预约
→ 检查 bookedCount < capacity
→ 创建预约
→ bookedCount 加1
→ 整体提交
```

第一轮内存实现按 `scheduleId` 使用服务器临界区；Access 实现必须使用数据库事务，并在 ADR-0004 实验中验证两个客户端抢最后一个号源时只能一个成功。

### 7.2 冗余 bookedCount 的一致性

`bookedCount` 可以加快查询并帮助原子占号，但它与预约表存在重复信息。任何预约、取消操作都必须同时更新预约表和 `bookedCount`。演示数据初始化后应校验：

```text
bookedCount
= 同一 scheduleId 下状态为 BOOKED 或 COMPLETED 的预约数量
```

若数据库实验无法可靠保证两者一致，应评审是否改为事务内实时统计预约数量，而不是继续维护冗余字段。

## 8. 隐私和数据归属

- 普通用户只能通过会话查看自己的预约；“我的预约”请求不接受可替换的 `patientUserId`。
- 医生查看患者信息属于 P1，必须由当前登录医生和有效 appointmentId 双重授权。
- 管理员可以维护基础数据和排班，但第一版数据字典不提供真实病历内容。
- 查询号源只返回科室、医生和排班公开信息，不返回预约患者列表。
- 日志可以记录 requestId、userId、action、结果和耗时，但不记录完整医疗描述。
- 表中只保存公共用户稳定 ID；姓名等展示信息通过已评审的公共接口读取或使用虚构演示数据。
- 不在公开仓库提交真实手机号、学号、病史、处方或支付数据。

## 9. Java 与 Access 类型对应草案

| 业务含义 | Java 类型 | Access 类型 | 说明 |
|---|---|---|---|
| 稳定 ID/状态/名称 | `String`/枚举名 | Short Text | 枚举以固定英文名称保存 |
| 日期和时刻 | `LocalDateTime` | Date/Time | JDBC 层负责与时间类型转换 |
| 容量和数量 | `int` | Long Integer | 服务器检查非负和上限 |
| 可选长说明 | `String` | Long Text | `null` 与空字符串语义要统一 |

查询请求中的 `LocalDate` 不一定直接保存到表中；服务器用它筛选 `startTime` 所在的校园当地日期。

## 10. 虚构演示数据计划

所有数据必须明确为课程演示数据。日期使用相对时间，避免固定日期过期后查询不到。

| 类型 | 最小演示数据 |
|---|---|
| 科室 | 内科、外科 |
| 医生 | 内科2名、外科1名；使用虚构姓名和代码 |
| 可用排班 | `今天+1天`，容量5，已预约2，显示 `AVAILABLE` |
| 已满排班 | `今天+1天`，容量2，已预约2，显示 `FULL` |
| 已关闭排班 | `今天+2天`，状态 `CLOSED` |
| 预约 | 使用公开演示账号对应的虚构 userId，不使用成员真实信息 |

第一轮内存实现使用 `LocalDate.now().plusDays(...)` 生成未来排班；后续 Access 初始化材料提供可重复生成或刷新演示日期的说明。

## 11. P1/P2 扩展表占位

以下表不属于本次核心数据字典完成条件，只有 Epic 扩展范围通过评审后才细化：

- `tblHospitalPatientProfile`：患者自述健康档案。
- `tblHospitalConsultation`：医生诊断与处置记录。
- `tblHospitalCharge`：模拟费用清单。
- `tblHospitalPrescriptionItem`：可选结构化简化处方。
- `tblHospitalTriageSummary`：患者确认的预问诊摘要。

未评审前不得把这些表写入最终 `.accdb` 并声称已经属于课程验收范围。

## 12. 待数据库集成人评审

- [ ] JDK 25 下的 Access JDBC 驱动和日期时间转换方式。
- [ ] Short Text ID 长度与公共 `userId` 的最终长度。
- [ ] Access 对“唯一索引忽略多个 null”的实际行为，决定医生 `userId` 索引实现。
- [ ] Access 事务隔离和并发写入能否保证最后一个号源只预约成功一次。
- [ ] `bookedCount` 与预约记录的事务一致性；是否保留冗余计数字段。
- [ ] 取消后是否允许同一患者重新预约同一排班，以及相应唯一约束。
- [ ] Access CHECK 约束能力不足时，哪些规则由 DAO/Service 和自动测试保证。
- [ ] 最终建表、初始化、重置演示日期和恢复数据的脚本/说明格式。
