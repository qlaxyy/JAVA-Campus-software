# 用户管理数据字典

## 1. 模块与状态

- 模块：用户管理、登录、会话与管理员授权
- 对应 Epic：[#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1)
- 状态：账号、管理范围、公共教师档案、账号管理审计和密码慢哈希已接入 Access DAO；完整状态模型待实现

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblUser` | 一卡通登录账号和账号级权限 | `userId` | `username` 保存唯一一卡通号；密码只存校验值；状态控制登录 |
| `tblUserAdminScope` | 账号附加的业务管理范围 | `userAdminScopeId` | `(userId, moduleCode)` 唯一 |
| `tblUserAuditLog` | 超级管理员账号操作记录 | `auditId` | 只追加；不记录密码、密码 proof 或完整 token |
| `tblTeacherProfile` | 全校公共教师资格与教师基础档案 | `teacherUserId` | 每个账号最多一条；以 `userId` 关联账号；停用资格不删除账号 |

会话保存在服务器内存，暂不落库。服务器记录创建时间和最后访问时间：连续 30 分钟无操作或创建满 8 小时后 token 失效。将来需要跨进程或重启保持会话时再单独评审会话表。

当前可运行版本仍保留 `passwordProof` 列用于兼容旧数据库，但新建或已升级账号会在该列写入不可登录的固定占位值，实际校验使用 `passwordHash`、`passwordSalt` 和
`passwordIterations`。旧账号第一次成功登录后自动升级。账号状态当前使用 `enabled`；
下面的时间字段和完整状态枚举仍是交付前目标结构。

## 3. 字段字典

### `tblUser`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `userId` | Short Text(36) | 是 | 无 | 主键，使用稳定随机标识，不使用用户名作外键 |
| `username` | Short Text(50) | 是 | 无 | 技术字段名；实际保存 8 位一卡通号。普通账号使用 4 位年份 + `0001`—`9999`，`20260000` 保留给演示超级管理员；建立唯一索引 |
| `passwordHash` | Short Text(255) | 是 | 无 | 慢哈希结果；算法和参数随记录保存或统一版本化 |
| `passwordSalt` | Short Text(255) | 是 | 无 | 每个账号独立的密码盐 |
| `passwordIterations` | Long Integer | 是 | `120000` | 当前 PBKDF2-HMAC-SHA256 迭代次数；随账号保存以便以后升级参数 |
| `passwordProof` | Short Text(64) | 是 | 64 个 `0` | 仅为兼容旧 Access 表结构保留；新账号不在此保存可用凭据 |
| `displayName` | Short Text(100) | 是 | 无 | 界面显示名，不用于鉴权 |
| `roleCode` | Short Text(20) | 是 | `USER` | `USER`、`SUPER_ADMIN`；不保存学生、教师、医生等子系统业务身份 |
| `status` | Short Text(20) | 是 | `ACTIVE` | `ACTIVE`、`DISABLED`、`LOCKED` |
| `passwordChangedAt` | Date/Time | 是 | 当前时间 | 支持密码策略和会话失效判断 |
| `createdAt` | Date/Time | 是 | 当前时间 | 创建时间 |
| `updatedAt` | Date/Time | 是 | 当前时间 | 最后修改时间 |

### `tblUserAdminScope`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `userAdminScopeId` | AutoNumber | 是 | 自动编号 | 主键 |
| `userId` | Short Text(36) | 是 | 无 | 外键指向 `tblUser.userId` |
| `moduleCode` | Short Text(20) | 是 | 无 | `STUDENT`、`COURSE`、`LIBRARY`、`SHOP`、`HOSPITAL` |
| `grantedByUserId` | Short Text(36) | 是 | 无 | 授权操作的超级管理员 |
| `grantedAt` | Date/Time | 是 | 当前时间 | 授权时间 |

### `tblUserAuditLog`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `auditId` | Short Text(36) | 是 | UUID | 主键 |
| `occurredAt` | Date/Time | 是 | 当前时间 | 操作时间 |
| `actorUserId` | Short Text(36) | 是 | 无 | 操作者 |
| `actorUsername` | Short Text(50) | 是 | 无 | 操作者当时的一卡通号 |
| `actorDisplayName` | Short Text(100) | 是 | 无 | 操作者当时的显示名称 |
| `actionCode` | Short Text(80) | 是 | 无 | 如 `USER.ADMIN_UPDATE_STATUS` |
| `targetText` | Short Text(120) | 是 | 无 | 目标一卡通号、用户 ID 或批量操作摘要 |
| `successful` | Yes/No | 是 | 无 | 操作是否成功 |
| `detailText` | Short Text(255) | 是 | 无 | 响应码和简短结果，不含敏感凭据 |

### `tblTeacherProfile`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `teacherUserId` | Short Text(36) | 是 | 无 | 主键，关联 `tblUser.userId` |
| `department` | Short Text(100) | 是 | 无 | 教师所属院系 |
| `teacherTitle` | Short Text(50) | 是 | 无 | 教师职称 |
| `active` | Yes/No | 是 | `TRUE` | 教师资格是否有效；不控制账号能否登录 |
| `createdByUserId` | Short Text(36) | 是 | 无 | 首次建立档案的超级管理员 |
| `createdAt` | Date/Time | 是 | 当前时间 | 首次建立时间 |
| `updatedAt` | Date/Time | 是 | 当前时间 | 最近修改时间 |

## 4. 关系、索引与业务约束

- `tblUser.username` 建唯一索引。
- `tblUserAdminScope.userId + moduleCode` 建联合唯一索引。
- `tblTeacherProfile.teacherUserId` 与 `tblUser.userId` 一一对应；姓名和一卡通号始终从 `tblUser` 读取，不重复保存。
- `USER` 只能存在零或一条范围记录，即一个普通账号最多管理一个子系统；`SUPER_ADMIN` 不依赖范围记录，由角色隐式覆盖所有业务模块。
- 只有 `SUPER_ADMIN` 可以新增管理员、分配/撤销范围、修改角色、启停账号和重置他人密码。
- 至少保留一个 `ACTIVE` 的 `SUPER_ADMIN`；禁止停用自己、删除自己或撤销最后一个启用超级管理员。
- 管理员只能发起密码重置，不能查看或恢复原密码。
- 修改角色、范围、账号状态或密码后，服务器必须清除该账号已有会话，使新权限立即生效。
- 只有超级管理员能维护公共教师档案。选课和学籍服务器通过只读 `TeacherDirectory` 查询有效教师，不直接读写用户模块 DAO。
- 任课教师与教学班的绑定属于选课模块，不写入 `tblTeacherProfile`。

## 5. 开发期演示数据

| 一卡通号 | 角色 | 管理范围 | 用途 |
|---|---|---|---|
| `20260000` | `SUPER_ADMIN` | 隐式全部 | 保留的演示超级管理员号；用户管理与全局管理流程 |
| `20260001` | `USER` | 无 | 普通账号；学籍资格由学籍子系统数据决定 |
| `20260002` | `USER` | 无 | 普通账号；当前医院有效医生档案绑定其 `userId` |
| `20260003` | `USER` | `STUDENT` | 学籍管理授权 |
| `20260004` | `USER` | `COURSE` | 选课管理授权 |
| `20260005` | `USER` | `LIBRARY` | 图书馆管理授权 |
| `20260006` | `USER` | `SHOP` | 商店管理授权 |
| `20260007` | `USER` | `HOSPITAL` | 医院管理授权；可提交医生申请但不能审核 |
| `20260008` | `USER` | 无 | 教师演示账号；公共教师档案绑定 `U-COURSE-TEACHER-001`，任课关系由选课模块维护 |

空的 `vCampus.accdb` 首次启动时会写入这些虚构账号；旧开发数据库若未占用 `20260008`，服务器启动时也会补入教师演示账号，但不会覆盖同号账号或其他修改。不能提交真实个人信息或密码。

## 6. 第一批用户管理 Action

| Action | 调用者 | 作用 |
|---|---|---|
| `USER.CHANGE_PASSWORD` | 任意已登录账号 | 校验当前密码后修改自己的密码，并清除该账号全部已有会话 |
| `USER.ADMIN_LIST_ACCOUNTS` | `SUPER_ADMIN` | 按用户名、角色、状态查询账号 |
| `USER.ADMIN_PREVIEW_NEXT_ACCOUNT` | `SUPER_ADMIN` | 预览按当年最大流水号加一得到的下一张一卡通号 |
| `USER.ADMIN_CREATE_GENERATED_ACCOUNT` | `SUPER_ADMIN` | 由服务器重新计算并生成一卡通号，创建普通账号并设置 0–1 个管理范围 |
| `USER.ADMIN_CREATE_ACCOUNT` | `SUPER_ADMIN` | 兼容旧客户端的指定一卡通号创建接口；新版界面不再调用 |
| `USER.ADMIN_BATCH_CREATE_ACCOUNTS` | `SUPER_ADMIN` | 原子批量创建不含管理权限的普通账号，最多 1000 个 |
| `USER.ADMIN_UPDATE_ACCOUNT` | `SUPER_ADMIN` | 修改显示名称和子系统管理范围 |
| `USER.ADMIN_UPDATE_STATUS` | `SUPER_ADMIN` | 启用或停用账号，不物理删除 |
| `USER.ADMIN_RESET_PASSWORD` | `SUPER_ADMIN` | 重置账号密码并清除该账号已有会话 |
| `USER.ADMIN_LIST_AUDIT_LOGS` | `SUPER_ADMIN` | 查看全部账号管理操作记录 |
| `USER.CURRENT_TEACHER_PROFILE` | 任意已登录账号 | 查询本人是否具有有效教师资格及院系、职称 |
| `USER.ADMIN_LIST_TEACHERS` | `SUPER_ADMIN` | 查看全部教师档案，包括已停用资格 |
| `USER.ADMIN_SAVE_TEACHER_PROFILE` | `SUPER_ADMIN` | 为已有账号新增或更新教师档案，并启用或停用教师资格 |
| `USER.ADMIN_BATCH_SAVE_TEACHERS` | `SUPER_ADMIN` | 批量新增或更新已有账号的教师档案 |

上述账号 Action 已通过 `UserRepository` 使用 Access。新增、批量导入、编辑、启停和重置密码的成功结果及业务失败会追加到 `tblUserAuditLog`；查询审计记录只允许 `SUPER_ADMIN`，且没有修改或删除审计记录的 Action。创建和编辑普通账号时，客户端提示“选择 0–1 个权限”，服务器也会拒绝同时提交多个管理范围。第一版不提供创建其他超级管理员或修改全局角色。

医院申请分为两类。关联已有校园账号时，医院在提交阶段通过内部 `AccountProvisioning` 精确查询一卡通号并锁定其 `userId`；找不到或已禁用时拒绝提交。新建外来医生时不接收医院填写的一卡通号，超级管理员批准后由用户模块以“当前年份 + 当年最大流水号加一”生成唯一一卡通号，以初始密码 `123456` 创建 `Role.USER` 账号并返回新 `userId`。禁止根据姓名或碰巧重复的输入自动复用账户。

批量导入 CSV 使用 UTF-8 编码，第一行为 `campusCardNumber,displayName`。初始密码统一为
`123456`，文件中不保存密码和管理范围。服务器会再次检查已有账号和文件内重复账号；
任意一行失败时 Access 事务整体回滚。

教师管理页面使用独立的 UTF-8 CSV，首行固定为
`campusCardNumber,department,title`。一卡通号必须已经存在于账号名单；导入只新增或更新
`tblTeacherProfile` 并启用教师资格，不创建、禁用或删除登录账号，任意一行失败时整批回滚。

旧开发数据库启动时会把 `student001`、`teacher001`、`admin` 等 8 个演示登录名迁移为
`20260000` 至 `20260007`，保留原 `userId`、显示名称、角色、管理范围和启停状态；新增的教师演示账号固定使用 `20260008`，并在 `tblTeacherProfile` 中建立资格记录。由于开发期
旧 `passwordProof` 包含登录名，发生登录名迁移的账号密码统一重置为公开测试密码 `123456`，并立即转换为带独立盐值的 PBKDF2 结果。
已有开发数据库中的演示账号也会按其稳定的 `userId` 自动整理为上述连续编号；迁移过程不会改变人员身份和权限，但密码会因一卡通号变化重置为 `123456`。

## 7. 待评审问题

- UCanAccess 连接、建表、事务写入和重连读取已完成最小实验；多客户端并发写入限制仍需专项验证。
- 当前采用 JDK 自带 PBKDF2-HMAC-SHA256，每账号独立 16 字节随机盐、120000 次迭代；正式部署前仍应根据验收电脑性能复测参数。
- `LOCKED` 的失败次数阈值、自动解锁时间和管理员手工解锁流程待用户模块安全设计补充。
