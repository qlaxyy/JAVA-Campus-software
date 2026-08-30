# 用户管理数据字典

## 1. 模块与状态

- 模块：用户管理、登录、会话与管理员授权
- 对应 Epic：[#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1)
- 状态：权限模型和内存版账号管理已实现；Access 类型和 DAO 尚待最小原型验证

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblUser` | 登录账号和账号级权限 | `userId` | `username` 唯一；密码只存带盐慢哈希；状态控制登录 |
| `tblUserAdminScope` | 账号附加的业务管理范围 | `userAdminScopeId` | `(userId, moduleCode)` 唯一 |
| `tblUserAuditLog` | 用户与授权管理审计 | `auditLogId` | 只追加；不记录密码、盐或完整 token |

会话第一阶段仍保存在服务器内存，暂不落库。将来需要跨进程或重启保持会话时再单独评审会话表。

## 3. 字段字典

### `tblUser`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `userId` | Short Text(36) | 是 | 无 | 主键，使用稳定随机标识，不使用用户名作外键 |
| `username` | Short Text(50) | 是 | 无 | 登录名，建立唯一索引；保存前统一去除首尾空白并按确定规则规范化 |
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

| 用户名 | 角色 | 管理范围 | 用途 |
|---|---|---|---|
| `student001` | `USER` | 无 | 普通账号；学籍资格由学籍子系统数据决定 |
| `teacher001` | `USER` | 无 | 普通账号；当前医院医生名单包含其 `userId` |
| `admin` | `SUPER_ADMIN` | 隐式全部 | 用户管理与全局管理流程 |
| `studentadmin` | `USER` | `STUDENT` | 学籍管理授权 |
| `courseadmin` | `USER` | `COURSE` | 选课管理授权 |
| `libraryadmin` | `USER` | `LIBRARY` | 图书馆管理授权 |
| `shopadmin` | `USER` | `SHOP` | 商店管理授权 |
| `hospitaladmin` | `USER` | `HOSPITAL` | 医院管理授权；当前不在医院医生名单中 |

这些账号当前由 `InMemoryAuthenticationService` 提供，接入 Access 时用虚构数据替换，不能提交真实个人信息或密码。

## 6. 第一批用户管理 Action

| Action | 调用者 | 作用 |
|---|---|---|
| `USER.ADMIN_LIST_ACCOUNTS` | `SUPER_ADMIN` | 按用户名、角色、状态查询账号 |
| `USER.ADMIN_CREATE_ACCOUNT` | `SUPER_ADMIN` | 创建普通账号并设置初始管理范围 |
| `USER.ADMIN_UPDATE_ACCOUNT` | `SUPER_ADMIN` | 修改显示名称和子系统管理范围 |
| `USER.ADMIN_UPDATE_STATUS` | `SUPER_ADMIN` | 启用或停用账号，不物理删除 |
| `USER.ADMIN_RESET_PASSWORD` | `SUPER_ADMIN` | 重置账号密码并清除该账号已有会话 |

上述 Action 已有内存 Repository 版本。第一版不提供创建其他超级管理员或修改全局角色；接入 Access 时保持 Action 和 DTO 不变，只替换 Repository/DAO。

## 7. 待评审问题

- Access JDBC 驱动、连接串、事务能力和并发写入限制需通过公共数据库实验确认。
- 正式密码慢哈希优先评估 PBKDF2（JDK 自带）；具体迭代次数和兼容字段需做性能测试后确定。
- `LOCKED` 的失败次数阈值、自动解锁时间和管理员手工解锁流程待用户模块安全设计补充。
