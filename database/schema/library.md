# 图书馆数据字典

## 1. 模块与实现状态

- 模块：图书馆
- 对应 Epic：#6
- 当前实现：阶段 4 已完成；正式启动使用 Access，单元测试和普通测试服务器仍可使用 InMemory
- 建库方式：`AccessLibraryStore` 首次连接时自动创建四张表和索引，并仅在空书目库中初始化演示馆藏

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblBook` | 书目元数据 | `bookId` | ISBN 唯一，状态为 `ACTIVE` 或 `INACTIVE` |
| `tblBookCopy` | 可流转的实体单册 | `copyId` | barcode 唯一，状态变化只能经过专用业务操作 |
| `tblBookCategory` | 分类字典 | `categoryId` | 名称必填，书目只引用有效分类 |
| `tblBorrowRecord` | 用户借阅和归还历史 | `recordId` | 一份单册同一时刻最多一条 `BORROWED` 记录 |

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
| `status` | Short Text(30) | 是 | `AVAILABLE` | `AVAILABLE`、`LOANED`、`WAITING_SHELVING`、`WITHDRAWN` |

普通资料编辑只能修改 `location` 和 `callNumber`。状态不得通过通用更新接口任意覆盖：

- 借书：`AVAILABLE -> LOANED`；
- 归还：`LOANED -> WAITING_SHELVING`；
- 管理员确认归架：`WAITING_SHELVING -> AVAILABLE`；
- 管理员注销：`AVAILABLE/WAITING_SHELVING -> WITHDRAWN`；`LOANED` 单册禁止注销。
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

## 4. 关联与索引

- `tblBook.isbn`、`tblBookCopy.barcode` 建唯一索引；
- `tblBook.categoryId -> tblBookCategory.categoryId`、`tblBookCopy.bookId -> tblBook.bookId`、
  `tblBorrowRecord.copyId -> tblBookCopy.copyId` 建外键；跨用户模块的 `userId` 只保存稳定标识，不建跨模块外键；
- `tblBook.title`、`author`、`categoryId` 建普通索引以支持检索；
- `tblBookCopy(bookId, status)` 建组合索引，用于书目库存与馆藏地汇总；
- `tblBorrowRecord(userId, status)` 用于查询个人当前借阅和历史；
- `tblBorrowRecord(copyId, status)` 用于定位一份单册的当前借阅；
- Access 不支持通用条件唯一索引时，“每份单册最多一条当前记录”仍须在同一事务中检查并写入；
- 逾期不持久化为第三种状态，由 `status == BORROWED && now > dueTime` 动态计算；
- 书目停用和单册下架均为软删除语义，借阅历史引用不会失效。

## 5. 搜索汇总规则

普通用户只检索 `ACTIVE` 书目；管理员查询可包含 `INACTIVE`。每个书目的库存按
`BookCopy.location` 分组返回：

- `totalCount`：该馆藏地未撤销的单册数；
- `availableCount`：仅当书目状态为 `ACTIVE` 时，统计其中状态为 `AVAILABLE` 的单册数；
  `INACTIVE` 书目即使仍有在架单册也返回 0；
- `LOANED` 和 `WAITING_SHELVING` 都计入馆藏，但不计入可借数；
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

## 7. 演示数据

演示书目会生成具有稳定 copyId 和 barcode 的实体单册，例如：

| copyId | barcode | bookId | 初始状态 |
|---|---|---|---|
| `CP-B001-001` | `SEU-B001-001` | `B001` | `AVAILABLE` |
| `CP-B001-003` | `SEU-B001-003` | `B001` | `WAITING_SHELVING` |

演示数据只在 Access 书目表为空时初始化，初始单册全部为 `AVAILABLE`；`WAITING_SHELVING`
必须由一次真实的“借书 -> 归还”产生。正常新增、编辑、借阅、归还和上架状态会跨服务器重启保留。
