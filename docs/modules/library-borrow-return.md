# 图书馆 V2：完整客户端流程与 Access 持久化

## 范围

本阶段严格依据 [Epic #6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6)，在阶段 1 的
`Book / BookCopy` 模型和阶段 2 的条码借还服务上完成 Swing 页面、管理员全馆借阅查询、
Socket 集成测试和 UI 测试。分层保持为：

`Swing -> ClientContext -> Socket -> CampusServer -> ActionRouter -> LibraryServerModule -> LibraryService -> Repository`

正式启动使用 Access 持久化；普通单元测试仍可注入 InMemory Repository。

## 用户入口

- **馆藏查询**：按关键词、分类查询书目，展示出版社、出版年、语种以及按馆藏地汇总的馆藏数和可借数；
  不展示内部 `copyId`，也不在检索结果中直接借实体书。
- **自助借还**：模拟自助终端，扫描或输入实体书馆藏条码后进行借书或归还登记；用户身份只取当前会话。
- **我的借阅**：自动分为当前借阅和历史借阅，展示书名、馆藏条码、借还时间、状态和动态逾期标记；
  页面只读，归还必须扫描实际单册条码。
- **图书管理**：仅图书馆模块管理员和超级管理员可见，详见
  [管理员维护说明](library-admin-maintenance.md)。

所有表格只读、单选，不允许拖动列或列宽；网络调用通过 `SwingWorker` 执行，不阻塞 EDT。

## 最终公共契约

| Action | Request.data | 成功 Response.data | 权限 |
|---|---|---|---|
| `LIBRARY.SEARCH_BOOKS` | `BookSearchRequest` | `BookSearchResult` | 已登录 |
| `LIBRARY.GET_BORROW_RECORDS` | `null` | `List<BorrowRecordDTO>` | 已登录 |
| `LIBRARY.BORROW_COPY` | `CopyBorrowRequest(barcode)` | `null` | 已登录 |
| `LIBRARY.RETURN_COPY` | `CopyReturnRequest(barcode)` | `null` | 已登录 |
| `LIBRARY.LIST_CATEGORIES` | `null` | `List<BookCategoryDTO>` | 已登录 |

V1 的 `BORROW_BOOK`、`RETURN_BOOK`、`UPDATE_STOCK`、`DELETE_BOOK` 以及对应请求 DTO 已删除，
客户端和服务器均不保留临时兼容入口。

## 借还规则与一致性

- 当前用户由 `Request.token -> SessionInfo.userId` 确认，请求不能指定 `userId`；
- 只允许借 `ACTIVE` 书目下状态为 `AVAILABLE` 的实体单册；
- `availableCount` 表示当前业务可借数，`INACTIVE` 书目的值固定为 0，即使仍有状态为 `AVAILABLE` 的单册；
- 最多同时借 5 本；同一书目有未归还记录时不能再借另一个副本；存在逾期未还时拒绝新借阅；
- 借期固定 30 天，时间由服务器生成；
- 借书必须同时完成 `BookCopy -> LOANED` 和创建 `BORROWED` 记录；
- 归还必须同时完成记录 `-> RETURNED`、写入 `returnTime` 和单册 `-> WAITING_SHELVING`；
- 归还后不会立即恢复可借数，管理员确认上架后才变为 `AVAILABLE`；
- 书目停用不妨碍已有借阅归还；逾期由 `BORROWED && now > dueTime` 动态计算；
- 同一单册同一时刻最多一条 `BORROWED` 记录，`LOANED` 与当前借阅记录保持对应。

`LibraryService.circulationLock` 负责同一服务器实例内的串行校验。正式 Access 版本由
`AccessLibraryStore` 在事务开始时绑定一个 JDBC Connection，`AccessBookCopyRepository` 与
`AccessBorrowRecordRepository` 在同一次借书或归还中复用该连接；任一步失败都会回滚整个事务。
InMemory 版本仍通过第二步失败时补偿第一步维持测试状态一致。

## 测试覆盖

- Service：关键词和分类校验、馆藏地汇总、停用书目过滤；
- 借还：条码定位、30 天借期、五本上限、同书目重复、逾期停借、他人归还拒绝；
- 状态机：`AVAILABLE -> LOANED -> WAITING_SHELVING -> AVAILABLE`；
- 一致性：同一条码并发只有一次借阅成功，任一写入失败不留下半完成状态；
- Socket：真实登录、检索、借书、个人记录、归还、管理员上架及库存变化；
- Access：四类 Repository 映射、唯一索引、借还失败回滚，以及两次服务器重启后的借阅和上架状态；
- Swing：三个普通用户入口、条码表单状态、当前/历史切换、只读单选表格和固定表头。

完整验证命令：`mvn clean verify`。
