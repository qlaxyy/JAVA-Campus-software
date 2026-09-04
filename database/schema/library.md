# 图书馆数据字典

## 1. 模块与负责人

- 模块：图书馆
- 对应 Epic：#6
- 状态：草稿，检索、借阅、个人记录查询和归还暂用内存演示数据

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblLibraryBook` | 可检索书目及馆藏汇总 | `bookId` | ISBN 唯一，可借数量不得超过馆藏数量 |
| `tblBorrowRecord` | 用户借阅与归还历史 | `recordId` | V1 状态仅为 `BORROWED` 或 `RETURNED` |

## 3. 字段字典

### `tblLibraryBook`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `bookId` | Short Text(20) | 是 | 无 | 业务主键，例如 `B001` |
| `isbn` | Short Text(20) | 是 | 无 | ISBN，唯一索引 |
| `title` | Short Text(200) | 是 | 无 | 书名 |
| `author` | Short Text(100) | 是 | 无 | 作者 |
| `category` | Short Text(50) | 是 | 无 | 图书分类 |
| `totalCount` | Long Integer | 是 | `0` | 馆藏总数，不得小于 0 |
| `availableCount` | Long Integer | 是 | `0` | 当前可借数，范围为 0 至 `totalCount` |

### `tblBorrowRecord`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `recordId` | Short Text(36) | 是 | 无 | 借阅记录主键 |
| `userId` | Short Text(36) | 是 | 无 | 当前会话对应的稳定用户 ID |
| `bookId` | Short Text(20) | 是 | 无 | 逻辑关联 `tblLibraryBook.bookId` |
| `borrowTime` | Date/Time | 是 | 无 | 借阅成功时间 |
| `dueTime` | Date/Time | 是 | 无 | 到期时间，等于借阅时间加 30 天 |
| `returnTime` | Date/Time | 否 | `NULL` | 实际归还时间 |
| `status` | Short Text(20) | 是 | `BORROWED` | V1 仅允许 `BORROWED`、`RETURNED` |

## 4. 关联与索引

- `bookId` 为主键，`isbn` 建唯一索引。
- `title`、`author`、`category` 建普通索引以支持检索。
- `tblBorrowRecord.bookId` 逻辑关联书目，`userId` 关联公共用户 ID。
- `tblBorrowRecord(userId, status)` 建组合索引，用于查询当前借阅和历史记录。
- 逾期不单独保存状态；由 `status == BORROWED && currentTime > dueTime` 动态计算。
- 书目被借阅记录引用后使用状态字段停用，不物理删除。

## 5. 演示数据

| bookId | isbn | title | author | category | totalCount | availableCount |
|---|---|---|---|---|---:|---:|
| `B001` | `9787111213826` | Java编程思想 | Bruce Eckel | 计算机 | 5 | 2 |
| `B004` | `9787020002207` | 红楼梦 | 曹雪芹 | 文学 | 6 | 0 |

第一组用于正常搜索和有库存展示；第二组用于无可借馆藏的边界展示。

## 6. 借阅一致性规则

- 借阅成功必须在同一服务器端业务操作中同时完成 `availableCount - 1` 和创建 `BORROWED` 记录。
- 归还成功必须同时完成 `availableCount + 1`、记录状态变为 `RETURNED` 和写入服务器生成的 `returnTime`；历史记录保留。
- 当前内存实现由单个 `LibraryService` 的 `circulationLock` 串行执行借阅、归还以及库存/个人记录查询；记录写入失败时执行库存补偿。Repository 写入抛异常时必须保持记录数据不变。
- 归还先验证记录属于当前会话用户，再检查是否已经归还；本人重复归还返回 `LIBRARY_ALREADY_RETURNED`，不再次增加库存。不存在或属于他人的记录统一返回 `LIBRARY_BORROW_RECORD_NOT_FOUND`。
- 记录查询只返回当前会话用户的数据，包含当前与历史记录；`overdue` 在服务端查询时动态计算，已归还记录不再视为逾期未还。
- 后续 Access/JDBC 实现应使用数据库事务，并保留服务器端临界区以避免最后一本被并发借出。
- 当前用户只能从 `Request.token → SessionInfo.userId` 获得，借阅请求不得提交 `userId`。
- 本次没有接入数据库。内存锁和失败补偿不具备进程崩溃恢复能力，也不等同于数据库事务；接入 Access 时需要同一连接上的事务边界，而不是两个 Repository 各自提交。

## 7. 待评审问题

- 是否拆分书目与实体副本表，在数据库集成前评审决定。
- 馆藏数量应由副本状态实时汇总，还是由事务维护汇总字段，待数据库集成阶段决定。
