# 图书馆 V2：管理员维护

## 权限边界

图书管理员不是新的全局角色，而是 `Role.USER + AdminScope.LIBRARY`；`Role.SUPER_ADMIN` 也可管理。
客户端根据 `SessionInfo.canAdminister(ModuleNames.LIBRARY)` 决定是否显示“图书管理”标签，但每个管理 Action
仍由 `LibraryServerModule` 从 token 查询服务器会话并再次鉴权，客户端不能在 DTO 中自报身份或权限。

## 管理页面

“图书管理”包含三个区域：

1. **书目维护**：查询全部书目（含 `INACTIVE`），新增/编辑 ISBN、书名、作者、分类、出版社、出版年和语种，
   通过专用操作切换“开放借阅 / 停止借阅”。新增书目初始馆藏为 0。
2. **实体单册**：为选中的书目登记唯一馆藏条码，维护馆藏地和索书号，确认归还单册上架，或软注销单册。
   登记后的 barcode、bookId 和 status 不能通过通用编辑修改。
3. **借阅查询**：查询全馆当前借阅、借阅历史或逾期未还，展示稳定 `userId`、书名、馆藏条码和时间。

已知不合法操作直接禁用：`AVAILABLE` 不能再次上架，`LOANED` 和 `WITHDRAWN` 不能注销，
`WITHDRAWN` 不能再编辑；条码登记后不可编辑。服务器仍对所有状态进行权威校验。

## 管理契约

| Action | Request.data | 成功 Response.data |
|---|---|---|
| `ADMIN_SEARCH_BOOKS` | `BookSearchRequest` | `BookSearchResult` |
| `ADD_BOOK` | `AddBookRequest` | `BookDTO` |
| `UPDATE_BOOK` | `UpdateBookRequest` | `BookDTO` |
| `SET_BOOK_STATUS` | `SetBookStatusRequest` | `BookDTO` |
| `ADD_BOOK_COPY` | `AddBookCopyRequest` | `BookCopyDTO` |
| `LIST_BOOK_COPIES` | `ListBookCopiesRequest` | `List<BookCopyDTO>` |
| `UPDATE_BOOK_COPY` | `UpdateBookCopyRequest` | `BookCopyDTO` |
| `SHELVE_BOOK_COPY` | `BookCopyIdRequest` | `BookCopyDTO` |
| `WITHDRAW_BOOK_COPY` | `BookCopyIdRequest` | `BookCopyDTO` |
| `ADMIN_QUERY_BORROWS` | `AdminBorrowQueryRequest` | `List<AdminBorrowRecordDTO>` |

## 关键业务规则

- `Book.status` 只能是 `ACTIVE / INACTIVE`；普通搜索只返回 ACTIVE，管理员搜索包含两者；
- `Book` 不保存 `totalCount / availableCount`，两个值由未注销 `BookCopy` 的状态按馆藏地汇总；
  `INACTIVE` 仍保留馆藏数，但业务可借数固定为 0；
- 新增书目与登记实体单册分离，同 ISBN 不能重复新增；同一 barcode 全馆唯一；
- `UPDATE_BOOK_COPY` 只接收 `copyId / location / callNumber`，不能绕过状态机；
- 注销是 `BookCopy -> WITHDRAWN` 的软删除，不物理删除书目或借阅历史；借出中的单册禁止注销；
- `WITHDRAWN` 不计馆藏和可借数，但仍可用于解释历史借阅；
- 当前、历史、逾期查询由 BorrowRecord 推导，不另存借阅数量或逾期布尔状态；
- 所有写操作和借还共用同一个 Service 临界区，避免管理操作与流通操作交错破坏状态。

## 测试覆盖

- 模块管理员、超级管理员放行；普通用户、其他模块管理员、匿名请求拒绝；
- 新增书目初始零馆藏、ISBN 规范化和唯一性、元数据编辑保持状态和实体单册；
- 单册登记、barcode 唯一、位置编辑不改变 barcode/status、软注销不破坏历史；
- 停用书目普通用户不可见且不可借，管理员仍能查询和恢复；
- `LOANED` 禁止注销，归还后必须先确认上架；
- 全馆当前/历史/逾期筛选以及动态逾期判断；
- 实际 Socket 管理生命周期和 Swing 管理页面的权限可见性、表格限制、状态按钮禁用逻辑。

完整验证命令：`mvn clean verify`。正式服务器使用 Access，书目、单册状态和借阅记录会跨重启保留；
`ServerModules.createRouter()` 创建的普通测试服务器仍使用 InMemory 数据。
