# [图书馆] 模块 Epic：书目、实体馆藏、借阅与归还

> 本文是 Epic #6 的 V2 替换草稿，供人工复制和修改。  
> 设计依据：`docs/modules/library-v2-design-proposal.md`。

## 模块

图书馆

## 用户故事 / 目标

实现校园图书馆实体图书的信息管理与借还登记。

- 普通用户可以检索书目、查看各馆藏地的馆藏与可借数量、通过模拟自助终端登记本人借还，
  并查看个人当前及历史借阅记录；
- 图书管理员可以维护书目和实体单册、处理归还后的重新上架，并查看全馆借阅及逾期记录；
- 系统以实体单册 `BookCopy` 作为流通对象，不再直接修改书目的总库存和可借库存；
- 借还用于模拟柜台或自助借还终端对实体书的登记，不表示用户能在检索页面远程借走实体书。

## 范围

### 范围内

- 书目与分类；
- 实体单册、唯一条码、馆藏位置和索书号；
- 关键词、分类检索；
- 按馆藏地统计馆藏数和可借数；
- 模拟自助终端的条码借书、条码归还；
- 归还后待上架、管理员确认上架；
- 用户当前借阅和历史记录；
- 30 天借期、借阅上限、重复借阅和逾期停借；
- 管理员书目上下架、单册维护；
- 管理员查看全馆借阅及逾期记录；
- InMemory 实现与测试；
- 最终 Access 持久化及借还事务。

### 范围外

- 预约排队、委托借阅、续借；
- 书评、书架、荐购、热门排行；
- 学期末或消息通知；
- 真实罚款、欠款支付和遗失赔偿；
- 采购批次、盘点、馆际互借；
- 真实条码/RFID 硬件；
- 管理员代替任意读者办理借书；
- 复杂借阅等级。

## 领域模型

### `Book`

表示一份书目，回答“这是什么书”。建议字段：

- `bookId`
- `categoryId`
- `isbn`
- `title`
- `author`
- `publisher`
- `publicationYear`
- `language`
- `status: ACTIVE | INACTIVE`

`Book` 不保存可手工修改的 `totalCount` 或 `availableCount`。

### `BookCopy`

表示一本实体馆藏，回答“这是馆里的哪一本”。建议字段：

- `copyId`
- `barcode`：唯一，登记后不可修改；
- `bookId`
- `location`
- `callNumber`
- `status: AVAILABLE | LOANED | WAITING_SHELVING | WITHDRAWN`

### `BorrowRecord`

表示用户对具体实体单册的一次借阅。建议字段：

- `recordId`
- `userId`
- `copyId`
- `borrowTime`
- `dueTime`
- `returnTime`
- `status: BORROWED | RETURNED`

逾期由服务器动态计算：

```text
status == BORROWED && currentTime > dueTime
```

不单独持久化 `OVERDUE` 状态。

## 验收条件

### 图书检索

- [ ] 用户可以按书名、作者、ISBN 关键词检索；
- [ ] 用户可以按 `categoryId` 筛选；
- [ ] 普通用户只能检索 `ACTIVE` 书目；
- [ ] 每条结果展示书目基本信息；
- [ ] 同一书目按 `location` 分组展示馆藏数和可借数；
- [ ] 馆藏数由非 `WITHDRAWN` 单册数量计算；
- [ ] 可借数由 `ACTIVE` 书目下 `AVAILABLE` 单册数量计算；
- [ ] 不向普通用户展示内部 `copyId` 或完整单册管理列表；
- [ ] 搜索页不提供按 `bookId` 直接借书的按钮。

### 借书

- [ ] 用户在“自助借还”页输入实体书条码发起借书；
- [ ] 当前用户只能由 `Request.token -> SessionInfo.userId` 确定；
- [ ] 借书请求不包含 `userId`；
- [ ] 条码不存在、书目 `INACTIVE` 或单册不是 `AVAILABLE` 时拒绝借阅；
- [ ] 用户最多同时借 5 本；
- [ ] 用户存在逾期未还记录时拒绝新借阅；
- [ ] 用户已有同一 `bookId` 的未归还记录时拒绝重复借阅，即使条码不同；
- [ ] 借期为 30 天；
- [ ] 借书成功创建 `BORROWED` 记录，并将对应单册改为 `LOANED`；
- [ ] 创建记录和改变单册状态必须同时成功或同时失败；
- [ ] 并发时同一单册只能被一个用户借到。

### 归还与上架

- [ ] 用户在“自助借还”页输入实体书条码发起归还；
- [ ] 只能归还当前用户本人的有效 `BORROWED` 记录；
- [ ] 归还成功写入 `returnTime`，记录变为 `RETURNED`；
- [ ] 归还成功时单册从 `LOANED` 变为 `WAITING_SHELVING`；
- [ ] 更新记录和改变单册状态必须同时成功或同时失败；
- [ ] `WAITING_SHELVING` 单册不能被借阅；
- [ ] 管理员确认上架后，单册才变回 `AVAILABLE`；
- [ ] 书目即使已经 `INACTIVE`，其既有借阅仍能归还；
- [ ] 历史借阅记录始终保留。

### 用户借阅记录

- [ ] 用户可以查看自己的当前借阅和历史记录；
- [ ] 记录展示书名、条码、借阅时间、到期时间、归还时间、状态和是否逾期；
- [ ] 普通用户不能查询其他用户的借阅记录；
- [ ] 逾期判断由服务器按当前时间动态计算。

### 管理员维护

- [ ] 具有图书管理范围的模块管理员和超级管理员可以进入管理页；
- [ ] 管理员可以新增、修改书目信息；
- [ ] 管理员可以将书目切换为 `ACTIVE` 或 `INACTIVE`；
- [ ] 书目停止借阅时不删除单册和历史记录，也不影响既有借阅归还；
- [ ] 管理员可以登记具有唯一条码的实体单册；
- [ ] 管理员可以查看全部书目，包括 `INACTIVE` 书目；
- [ ] 管理员可以查看某书目下的全部单册及状态；
- [ ] 通用单册编辑只能修改 `location` 和 `callNumber`；
- [ ] `copyId`、`bookId`、`barcode` 和 `status` 不能通过通用编辑修改；
- [ ] `LOANED` 单册不能注销；
- [ ] `WITHDRAWN` 采用逻辑注销并保留历史；
- [ ] 管理员可以查看全馆当前、历史和逾期借阅；
- [ ] 管理员查询先只展示稳定 `userId`，不自行跨用户模块查询姓名和学号；
- [ ] 普通用户直接伪造管理 Action 时，服务器返回无权限。

### 界面与质量

- [ ] 表格内容只读、单行选择，不允许拖动重排；
- [ ] 客户端在已知非法状态下禁用操作，并给出明确原因；
- [ ] 客户端校验只改善体验，服务器仍执行完整校验；
- [ ] 网络操作不阻塞 Swing EDT；
- [ ] `mvn clean verify` 通过；
- [ ] 最终 Access 版本重启后数据不丢失。

## 页面

### 普通用户

#### `LibraryPanel`：图书检索

- 关键词、分类筛选；
- 展示书目、分类和出版信息；
- 按馆藏地展示馆藏数、可借数；
- 不直接办理借书。

#### `SelfServicePanel`：自助借还

- 输入条码模拟扫描实体书；
- 显示当前登录用户；
- 发起本人借书或归还；
- 显示明确的成功或失败结果。

#### `MyBorrowPanel`：我的借阅

- 当前借阅；
- 历史借阅；
- 应还时间和逾期提示。

### 图书管理员

#### `LibraryAdminPanel`：图书管理

- 书目：新增、编辑、开放借阅、停止借阅；
- 单册：登记、修改位置、待上架确认、逻辑注销；
- 借阅：查看当前、历史和逾期记录。

客户端入口依据本地 `SessionInfo` 显示；所有管理操作仍必须由服务器鉴权。

## Action 与 DTO

以下是 V2 目标契约。修改 `vcampus-common` 时客户端和服务器必须同步。

### 普通用户 Action

| Action | 请求 | 响应 |
|---|---|---|
| `LIBRARY.SEARCH_BOOKS` | `BookSearchRequest` | `BookSearchResult` |
| `LIBRARY.GET_BORROW_RECORDS` | 无 data | `List<BorrowRecordDTO>` |
| `LIBRARY.BORROW_COPY` | `CopyBorrowRequest` | 成功无业务数据 |
| `LIBRARY.RETURN_COPY` | `CopyReturnRequest` | 成功无业务数据 |

### 管理 Action

| Action | 请求 | 响应 |
|---|---|---|
| `LIBRARY.ADMIN_SEARCH_BOOKS` | `BookSearchRequest` | `BookSearchResult`，包含 INACTIVE |
| `LIBRARY.ADD_BOOK` | `AddBookRequest` | `BookDTO` |
| `LIBRARY.UPDATE_BOOK` | `UpdateBookRequest` | `BookDTO` |
| `LIBRARY.SET_BOOK_STATUS` | `SetBookStatusRequest` | `BookDTO` |
| `LIBRARY.ADD_BOOK_COPY` | `AddBookCopyRequest` | `BookCopyDTO` |
| `LIBRARY.LIST_BOOK_COPIES` | `ListBookCopiesRequest` | `List<BookCopyDTO>` |
| `LIBRARY.UPDATE_BOOK_COPY` | `UpdateBookCopyRequest` | `BookCopyDTO` |
| `LIBRARY.SHELVE_BOOK_COPY` | `BookCopyIdRequest` | `BookCopyDTO` |
| `LIBRARY.WITHDRAW_BOOK_COPY` | `BookCopyIdRequest` | `BookCopyDTO` |
| `LIBRARY.ADMIN_QUERY_BORROWS` | `AdminBorrowQueryRequest` | `List<AdminBorrowRecordDTO>` |

不再保留 V1 的：

- `LIBRARY.BORROW_BOOK`：只按 `bookId` 借书；
- `LIBRARY.RETURN_BOOK`：只按借阅记录模拟用户点击归还；
- `LIBRARY.UPDATE_STOCK`：直接维护总库存；
- `LIBRARY.DELETE_BOOK`：将书目库存清零并隐藏。

### 关键 DTO

#### `BookSearchRequest`

- `keyword: String`
- `categoryId: String`，可选

#### `BookDTO`

- `bookId`
- `categoryId`、`categoryName`
- `isbn`、`title`、`author`
- `publisher`、`publicationYear`、`language`
- `status`
- `locations: List<BookLocationDTO>`

#### `BookLocationDTO`

- `location`
- `totalCount`：由单册统计
- `availableCount`：由单册统计

不得把单个 `location` 或可手工修改的库存字段放回 `Book` 领域对象。

#### `BookCopyDTO`

- `copyId`
- `barcode`
- `bookId`
- `location`
- `callNumber`
- `status`

#### `CopyBorrowRequest` / `CopyReturnRequest`

- `barcode`

不包含 `userId`。

#### `BorrowRecordDTO`

- `recordId`
- `bookId`、`bookTitle`
- `copyId`、`barcode`
- `borrowTime`、`dueTime`、`returnTime`
- `status`
- `overdue`：服务器动态计算

#### `UpdateBookCopyRequest`

- `copyId`
- `location`
- `callNumber`

不得包含 `bookId`、`barcode` 或 `status`。

所有网络 DTO 必须实现 `Serializable`、显式声明 `serialVersionUID`，并且只保存业务数据。

## 业务规则与不变量

### 借阅规则

1. 普通用户最多同时借 5 本；
2. 默认借期 30 天；
3. 存在逾期未还记录时禁止新借阅；
4. 同一用户不能同时借同一书目的多个单册；
5. 只有 ACTIVE 书目的 AVAILABLE 单册可以借；
6. 用户身份由 session 确定；
7. 借书时创建记录与 `BookCopy -> LOANED` 必须保持一致。

### 归还规则

1. 只能归还当前用户本人处于 BORROWED 的记录；
2. 归还时结束记录与 `BookCopy -> WAITING_SHELVING` 必须保持一致；
3. 待上架单册由管理员确认后变为 AVAILABLE；
4. 历史记录不删除。

### 状态不变量

```text
BORROWED => returnTime == null
RETURNED => returnTime != null

一个 BookCopy 同一时刻最多存在一条 BORROWED 记录
BookCopy.status == LOANED
<=> 存在该 copyId 对应的 BORROWED 记录
```

### 事务

- InMemory 阶段所有关联读写使用同一个服务器临界区；
- Access 阶段由同一事务边界向相关 Repository 提供同一个 JDBC `Connection`；
- Repository 不得在同一次借还业务中各自打开连接或独立提交；
- 借书、归还中任何一步失败，所有相关修改必须回滚。

## 权限

### 普通用户

允许检索、查看馆藏、自助办理本人借还、查看本人记录。

禁止维护书目/单册、改变单册状态、查看其他用户记录。

### 图书管理员

`SessionInfo.canAdminister(ModuleNames.LIBRARY) == true` 时，可以执行图书馆管理 Action。
模块管理员仍是普通 `Role.USER` 加 `AdminScope.LIBRARY`；超级管理员也可以管理。

权限必须在服务器端检查，客户端隐藏页面不能替代鉴权。

## 分层职责

- View：显示、输入、异步请求、反馈，不写 SQL、不判断最终权限；
- CampusClient / ClientContext：封装 Request、携带 token、通过 Socket 通信；
- LibraryServerModule：注册路由，校验会话和 DTO，转换统一 Response；
- LibraryService：实现借还、状态流转、权限和业务规则；
- Repository：只负责数据访问，不决定用户能否借书；
- 数据库表及字段以 `database/schema/library.md` 为准。

V2 暂不拆分多个 Service，继续遵循现有模块架构。

## 实施顺序

### 阶段 1：单册模型和搜索汇总

- 增加 BookCopy、状态枚举和 InMemory Repository；
- 为现有演示书目生成实体单册；
- 搜索结果改为按 location 统计；
- 完成模型、Repository、汇总和权限测试；
- 本阶段暂不切换借还流程。

### 阶段 2：条码借还

- BorrowRecord 从 bookId 改为 copyId；
- 实现 BORROW_COPY、RETURN_COPY；
- 实现 WAITING_SHELVING 和管理员确认上架；
- 完成规则、不变量、并发和失败回滚测试。

### 阶段 3：页面和管理员查询

- 改造检索、自助借还、我的借阅和管理员页面；
- 增加全馆借阅、历史和逾期查询；
- 完成 Socket 集成和 Swing UI 测试。

### 阶段 4：Access

- 实现 Book、BookCopy、BorrowRecord、分类 Repository；
- 使用同一 Connection 事务完成借还；
- 验证重启恢复和数据库集成测试。

每个阶段单独提交并确保 `mvn clean verify` 通过，不一次性实现整个 V2。

## 依赖与风险

### 依赖

- 用户模块提供登录、token 和 SessionInfo；
- 公共模块提供 Request、Response、模块权限和序列化协议；
- V2 公共 Action/DTO 需要客户端与服务器同步修改；
- Access 最终表结构同步维护在 `database/schema/library.md`。

### 风险

- BookCopy 状态和 BorrowRecord 不一致；
- 多 Repository 未共享同一事务；
- 同一条码或同一单册发生并发借阅；
- 通用编辑绕过单册状态机；
- 普通用户检索到 INACTIVE 书目；
- 管理权限只在客户端控制；
- 一次实现全部 V2 导致模型问题扩散到页面和借还流程。

