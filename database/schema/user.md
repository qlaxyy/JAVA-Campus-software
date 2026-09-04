# 用户管理数据字典

## 1. 模块与状态

- 模块：用户管理、登录、会话与管理员授权
- 对应 Epic：[#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1)
- 状态：账号与管理范围已接入 Access DAO；慢哈希、审计和完整状态模型待实现

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblUser` | 一卡通登录账号和账号级权限 | `userId` | `username` 保存唯一一卡通号；密码只存校验值；状态控制登录 |
| `tblUserAdminScope` | 账号附加的业务管理范围 | `userAdminScopeId` | `(userId, moduleCode)` 唯一 |
| `tblUserAuditLog`（规划） | 用户与授权管理审计 | `auditLogId` | 尚未建表；只追加且不记录密码、盐或完整 token |

会话第一阶段仍保存在服务器内存，暂不落库。将来需要跨进程或重启保持会话时再单独评审会话表。

当前可运行版本为了与既有登录协议兼容，`tblUser` 实际保存 `passwordProof`
和 `enabled`；`tblUserAdminScope` 实际使用 `scopeId`、`userId`、`moduleCode`。
下面的慢哈希、时间字段和审计表是交付前的目标结构，不应误认为已经实现。

## 3. 字段字典

### `tblUser`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `userId` | Short Text(36) | 是 | 无 | 主键，使用稳定随机标识，不使用用户名作外键 |
| `username` | Short Text(50) | 是 | 无 | 技术字段名；实际保存 8 位一卡通号（4 位年份 + `0001`—`9999`），建立唯一索引 |
| `passwordHash` | Short Text(255) | 是 | 无 | 慢哈希结果；算法和参数随记录保存或统一版本化 |
| `passwordSalt` | Short Text(255) | 是 | 无 | 每个账号独立的密码盐 |
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
| `auditLogId` | AutoNumber | 是 | 自动编号 | 主键 |
| `actorUserId` | Short Text(36) | 是 | 无 | 操作者 |
| `actionCode` | Short Text(80) | 是 | 无 | 如 `USER.ADMIN_UPDATE_STATUS`、`USER.ADMIN_GRANT_SCOPE` |
| `targetUserId` | Short Text(36) | 否 | 空 | 被操作账号 |
| `resultCode` | Short Text(50) | 是 | 无 | 成功或安全错误码 |
| `requestId` | Short Text(36) | 是 | 无 | 对应网络请求，便于排查 |
| `occurredAt` | Date/Time | 是 | 当前时间 | 操作时间 |

## 4. 关系、索引与业务约束

- `tblUser.username` 建唯一索引。
- `tblUserAdminScope.userId + moduleCode` 建联合唯一索引。
- `USER` 可以存在零到多条范围记录；`SUPER_ADMIN` 不写范围记录时也由角色隐式覆盖所有业务模块。
- 只有 `SUPER_ADMIN` 可以新增管理员、分配/撤销范围、修改角色、启停账号和重置他人密码。
- 至少保留一个 `ACTIVE` 的 `SUPER_ADMIN`；禁止停用自己、删除自己或撤销最后一个启用超级管理员。
- 管理员只能发起密码重置，不能查看或恢复原密码。
- 修改角色、范围、账号状态或密码后，服务器必须清除该账号已有会话，使新权限立即生效。
- 业务模块只读取身份和范围，不跨模块更新上述表。

## 5. 开发期演示数据

| 一卡通号 | 角色 | 管理范围 | 用途 |
|---|---|---|---|
| `20260001` | `USER` | 无 | 普通账号；学籍资格由学籍子系统数据决定 |
| `20260002` | `USER` | 无 | 普通账号；当前医院有效医生档案绑定其 `userId` |
| `20260003` | `SUPER_ADMIN` | 隐式全部 | 用户管理与全局管理流程 |
| `20260004` | `USER` | `STUDENT` | 学籍管理授权 |
| `20260005` | `USER` | `COURSE` | 选课管理授权 |
| `20260006` | `USER` | `LIBRARY` | 图书馆管理授权 |
| `20260007` | `USER` | `SHOP` | 商店管理授权 |
| `20260008` | `USER` | `HOSPITAL` | 医院管理授权；可提交医生申请但不能审核 |

空的 `vCampus.accdb` 首次启动时会写入这些虚构账号；已有数据时不会重复初始化或覆盖修改。不能提交真实个人信息或密码。

## 6. 第一批用户管理 Action

| Action | 调用者 | 作用 |
|---|---|---|
| `USER.ADMIN_LIST_ACCOUNTS` | `SUPER_ADMIN` | 按用户名、角色、状态查询账号 |
| `USER.ADMIN_CREATE_ACCOUNT` | `SUPER_ADMIN` | 创建普通账号并设置初始管理范围 |
| `USER.ADMIN_BATCH_CREATE_ACCOUNTS` | `SUPER_ADMIN` | 原子批量创建不含管理权限的普通账号，最多 1000 个 |
| `USER.ADMIN_UPDATE_ACCOUNT` | `SUPER_ADMIN` | 修改显示名称和子系统管理范围 |
| `USER.ADMIN_UPDATE_STATUS` | `SUPER_ADMIN` | 启用或停用账号，不物理删除 |
| `USER.ADMIN_RESET_PASSWORD` | `SUPER_ADMIN` | 重置账号密码并清除该账号已有会话 |

上述 Action 已通过 `UserRepository` 使用 Access。第一版不提供创建其他超级管理员或修改全局角色；Action 和 DTO 保持不变。

医院申请分为两类。关联已有校园账号时，医院在提交阶段通过内部 `AccountProvisioning` 精确查询一卡通号并锁定其 `userId`；找不到或已禁用时拒绝提交。新建外来医生时不接收医院填写的一卡通号，超级管理员批准后由用户模块以“当前年份 + 当年最大流水号加一”生成唯一一卡通号，以初始密码 `123456` 创建 `Role.USER` 账号并返回新 `userId`。禁止根据姓名或碰巧重复的输入自动复用账户。

批量导入 CSV 使用 UTF-8 编码，第一行为 `campusCardNumber,displayName`。初始密码统一为
`123456`，文件中不保存密码和管理范围。服务器会再次检查已有账号和文件内重复账号；
任意一行失败时 Access 事务整体回滚。

旧开发数据库启动时会把 `student001`、`teacher001`、`admin` 等 8 个演示登录名迁移为
`20260001` 至 `20260008`，保留原 `userId`、显示名称、角色、管理范围和启停状态。由于开发期
`passwordProof` 包含登录名，发生迁移的账号密码统一重置为公开测试密码 `123456`。

## 7. 待评审问题

- UCanAccess 连接、建表、事务写入和重连读取已完成最小实验；多客户端并发写入限制仍需专项验证。
- 正式密码慢哈希优先评估 PBKDF2（JDK 自带）；具体迭代次数和兼容字段需做性能测试后确定。
- `LOCKED` 的失败次数阈值、自动解锁时间和管理员手工解锁流程待用户模块安全设计补充。
