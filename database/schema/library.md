# 图书馆数据字典

## 1. 模块与实现状态

- 模块：图书馆
- 对应 Epic：#6
- 当前实现：预约与入口调整阶段 4 已完成；正式启动使用 Access，单元测试和普通测试服务器仍可使用 InMemory
- 建库方式：`AccessLibraryStore` 首次连接时自动创建五张表和索引；仅在整套图书馆表首次创建且业务表为空时初始化一致的演示数据

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblBook` | 书目元数据 | `bookId` | ISBN 唯一，状态为 `ACTIVE` 或 `INACTIVE` |
| `tblBookCopy` | 可流转的实体单册 | `copyId` | barcode 唯一，状态变化只能经过专用业务操作 |
| `tblBookCategory` | 分类字典 | `categoryId` | 名称必填，书目只引用有效分类 |
| `tblBorrowRecord` | 用户借阅和归还历史 | `recordId` | 一份单册同一时刻最多一条 `BORROWED` 记录 |
| `tblReservation` | 用户书目预约及单册分配 | `reservationId` | 一个保留单册同一时刻只对应一条待取预约 |

## 3. 字段字典

### `tblBook`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `bookId` | Short Text(20) | 是 | 无 | 业务主键，例如 `B001` |
| `isbn` | Short Text(20) | 是 | 无 | ISBN，唯一索引；停用书目仍占用原 ISBN |
| `title` | Short Text(200) | 是 | 无 | 书名 |
| `author` | Short Text(100) | 是 | 无 | 作者 |
| `categoryId` | Short Text(20) | 是 | 无 | 关联 `tblBookCategory.categoryId` |
| `publisher` | Short Text(100) | 否 | `NULL` | 出版社 |
| `publicationYear` | Long Integer | 否 | `NULL` | 出版年 |
| `language` | Short Text(30) | 否 | `NULL` | 语种 |
| `status` | Short Text(20) | 是 | `ACTIVE` | `ACTIVE`、`INACTIVE` |

`tblBook` 不保存 `totalCount` 或 `availableCount`。馆藏数和可借数由对应
`tblBookCopy` 汇总，避免书目计数和实体单册状态形成两套事实来源。

### `tblBookCopy`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `copyId` | Short Text(36) | 是 | 无 | 实体单册主键 |
| `barcode` | Short Text(50) | 是 | 无 | 馆藏条码，登记后不可修改，唯一索引 |
| `bookId` | Short Text(20) | 是 | 无 | 关联 `tblBook.bookId`，登记后不可修改 |
| `location` | Short Text(100) | 是 | 无 | 馆藏地 |
| `callNumber` | Short Text(100) | 是 | 无 | 索书号 |
| `status` | Short Text(30) | 是 | `AVAILABLE` | `AVAILABLE`、`RESERVED`、`LOANED`、`WAITING_SHELVING`、`WITHDRAWN` |

普通资料编辑只能修改 `location` 和 `callNumber`。状态不得通过通用更新接口任意覆盖：

- 预约分配：`AVAILABLE -> RESERVED`；预约取消或过期后释放为 `AVAILABLE`，并优先顺延给队首；
- 借书：普通单册 `AVAILABLE -> LOANED`，预约用户取书时 `RESERVED -> LOANED`；
- 归还：`LOANED -> WAITING_SHELVING`；
- 管理员确认归架：`WAITING_SHELVING -> AVAILABLE`；
- 管理员注销：`AVAILABLE/WAITING_SHELVING -> WITHDRAWN`；`RESERVED/LOANED` 单册禁止注销。
- 管理员恢复：`WITHDRAWN -> AVAILABLE`；其他状态不能执行恢复。

### `tblBookCategory`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `categoryId` | Short Text(20) | 是 | 无 | 分类主键 |
| `categoryName` | Short Text(50) | 是 | 无 | 分类显示名称 |

当前演示字典为 `C001=计算机`、`C002=文学`、`C003=历史`；暂不提供分类增删改。

### `tblBorrowRecord`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `recordId` | Short Text(36) | 是 | 无 | 借阅记录主键 |
| `userId` | Short Text(36) | 是 | 无 | 当前会话对应的稳定用户 ID |
| `copyId` | Short Text(36) | 是 | 无 | 关联 `tblBookCopy.copyId`，不直接保存 `bookId` |
| `borrowTime` | Date/Time | 是 | 无 | 借阅成功时间 |
| `dueTime` | Date/Time | 是 | 无 | 到期时间，等于借阅时间加 30 天 |
| `returnTime` | Date/Time | 否 | `NULL` | 实际归还时间 |
| `status` | Short Text(20) | 是 | `BORROWED` | `BORROWED`、`RETURNED` |

记录不变量：

- `BORROWED => returnTime == NULL`；
- `RETURNED => returnTime != NULL`；
- `dueTime >= borrowTime`，非空 `returnTime >= borrowTime`；
- 一份 `BookCopy` 同一时刻最多存在一条 `BORROWED` 记录；
- `BookCopy.status == LOANED` 当且仅当存在该单册的当前 `BORROWED` 记录。

### `tblReservation`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `reservationId` | Short Text(36) | 是 | 无 | 预约业务主键 |
| `userId` | Short Text(36) | 是 | 无 | 当前会话对应的稳定用户 ID |
| `bookId` | Short Text(20) | 是 | 无 | 关联 `tblBook.bookId`，预约面向书目而非客户端指定的单册 |
| `pickupLocation` | Short Text(100) | 是 | 无 | 用户选择的有效取书馆藏地 |
| `assignedCopyId` | Short Text(36) | 否 | `NULL` | 待取时分配的实体单册，关联 `tblBookCopy.copyId` |
| `createdAt` | Date/Time | 是 | 无 | 预约创建时间，参与稳定 FIFO 排序 |
| `readyAt` | Date/Time | 否 | `NULL` | 单册完成保留的时间 |
| `expiresAt` | Date/Time | 否 | `NULL` | 待取截止时间，等于 `readyAt + 24 小时` |
| `closedAt` | Date/Time | 否 | `NULL` | 完成、取消或过期时间 |
| `status` | Short Text(30) | 是 | `WAITING` | `WAITING`、`READY_FOR_PICKUP`、`FULFILLED`、`CANCELED`、`EXPIRED` |

预约不变量：

- `WAITING` 不分配单册，`assignedCopyId/readyAt/expiresAt/closedAt` 均为 `NULL`；
- `READY_FOR_PICKUP` 必须同时具有单册、待取开始和截止时间，`closedAt == NULL`；
- `FULFILLED/CANCELED/EXPIRED` 必须具有 `closedAt`；
- `BookCopy.status == RESERVED` 当且仅当它被一条 `READY_FOR_PICKUP` 预约占用；
- 同一书目、馆藏地的排队按 `createdAt + reservationId` 稳定 FIFO。

## 4. 关联与索引

- `tblBook.isbn`、`tblBookCopy.barcode` 建唯一索引；
- `tblBook.categoryId -> tblBookCategory.categoryId`、`tblBookCopy.bookId -> tblBook.bookId`、
  `tblBorrowRecord.copyId -> tblBookCopy.copyId`、`tblReservation.bookId -> tblBook.bookId`、
  `tblReservation.assignedCopyId -> tblBookCopy.copyId` 建外键；跨用户模块的 `userId` 只保存稳定标识，不建跨模块外键；
- `tblBook.title`、`author`、`categoryId` 建普通索引以支持检索；
- `tblBookCopy(bookId, status)` 建组合索引，用于书目库存与馆藏地汇总；
- `tblBorrowRecord(userId, status)` 用于查询个人当前借阅和历史；
- `tblBorrowRecord(copyId, status)` 用于定位一份单册的当前借阅；
- `tblReservation(userId, status)` 用于查询个人有效预约和历史；
- `tblReservation(bookId, pickupLocation, status, createdAt, reservationId)` 用于稳定排队；
- `tblReservation(assignedCopyId, status)` 用于定位单册的待取归属；
- Access 不支持通用条件唯一索引时，“每份单册最多一条当前记录”仍须在同一事务中检查并写入；
- 逾期不持久化为第三种状态，由 `status == BORROWED && now > dueTime` 动态计算；
- 书目停用和单册下架均为软删除语义，借阅历史引用不会失效。

## 5. 搜索汇总规则

普通用户只检索 `ACTIVE` 书目；管理员查询可包含 `INACTIVE`。每个书目的库存按
`BookCopy.location` 分组返回：

- `totalCount`：该馆藏地未撤销的单册数；
- `availableCount`：仅当书目状态为 `ACTIVE` 时，统计其中状态为 `AVAILABLE` 的单册数；
  `INACTIVE` 书目即使仍有在架单册也返回 0；
- `RESERVED`、`LOANED` 和 `WAITING_SHELVING` 都计入馆藏，但不计入可借数；
- `WITHDRAWN` 不计入馆藏数或可借数。

## 6. 借还一致性规则

- 用户身份只允许从 `Request.token -> SessionInfo.userId` 获取，借还 DTO 不接受 `userId`；
- 借书按 barcode 定位一份 `AVAILABLE` 单册；同一书目的另一单册仍算重复借阅；
- 最多同时借 5 本；存在逾期未还记录时拒绝新借阅；借期 30 天；
- 借书成功必须同时创建 `BORROWED` 记录并将单册改为 `LOANED`；
- 归还允许书目已经 `INACTIVE`，成功时同时把记录改为 `RETURNED` 并将单册改为
  `WAITING_SHELVING`；归还不会立即恢复可借数；
- 只有图书馆模块管理员或超级管理员可以确认上架；确认后单册才变为 `AVAILABLE`；
- `LibraryService.circulationLock` 串行执行同一个服务器实例内的管理与借还操作；
- InMemory 测试实现的第二步失败时恢复第一步；
- Access 实现由 `AccessLibraryStore` 通过当前线程事务上下文向多个 Repository 提供同一个 JDBC
  `Connection`，Repository 在借还业务中不独立提交；两步中任何一步失败都会统一回滚。

## 7. 预约规则与一致性

- 用户身份只从 `Request.token -> SessionInfo.userId` 获取；创建预约只接受 `bookId` 和取书馆藏地；
- 最多同时存在 3 条 `WAITING/READY_FOR_PICKUP` 预约；逾期未还、已借同书目或同书目已有有效预约时拒绝；
- 有可借单册时立即保留 24 小时，否则进入队列；待取过期后，同一用户 7 天内不能再次预约同一书目，主动取消不触发冷却；
- 归架、新增或恢复单册时，优先分配给相同书目和馆藏地的队首；取消或过期释放单册并自动顺延；
- 他人不能借走 `RESERVED` 单册；预约用户扫描已保留条码时，预约、单册和借阅记录在同一事务内完成；
- 用户借到同书目的其他可借单册时，原有效预约结束并释放已保留单册；停止书目借阅时原子取消全部有效预约；
- 不启动后台定时器；检索、预约、个人预约、借还和管理状态操作前使用服务器 `Clock` 在事务内清理到期预约。

## 8. 演示数据

演示书目会生成具有稳定 copyId 和 barcode 的实体单册。首次创建整套图书馆表时，还会为普通学生账号
`20260001`（`U-STUDENT-001`）建立以下一致场景：

| 场景 | copyId | barcode | bookId | 单册状态 | 关联状态 |
|---|---|---|---|---|---|
| 当前借阅 | `CP-B001-001` | `SEU-B001-001` | `B001` | `LOANED` | `BORROWED`，借期 30 天 |
| 历史借阅 | `CP-B002-001` | `SEU-B002-001` | `B002` | `AVAILABLE` | `RETURNED` |
| 预约待取 | `CP-B003-001` | `SEU-B003-001` | `B003` | `RESERVED` | `READY_FOR_PICKUP`，保留 24 小时 |

初始化要求“本次首次创建全部图书馆表”且借阅、预约业务表为空，三组状态在一个 Access 事务中写入；
任一步失败全部回滚。已有数据库即使借阅或预约表为空也不会补种，取消或修改演示记录后重启不会恢复。
正常新增、编辑、预约、借阅、归还和上架状态均跨服务器重启保留。
