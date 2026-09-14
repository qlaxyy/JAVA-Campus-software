# 图书馆 V2：线上预约、模拟借还与 Access 持久化

## 范围

本轮严格依据 [Epic #6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6)，保留
`Book / BookCopy`、条码借还和 Access 事务，增加书目预约、模式分离、个人预约以及一致演示数据。
分层保持为：

`Swing -> ClientContext -> Socket -> CampusServer -> ActionRouter -> LibraryServerModule -> LibraryService -> Repository`

正式启动使用 Access 持久化；普通单元测试仍可注入 InMemory Repository。

## 模式与用户入口

- 进入图书馆后先选择 **线上图书馆** 或 **模拟自助终端**；具有图书馆管理权的账号还会看到独立的
  **图书管理员工作台**。管理员入口不与读者选项卡混排；返回校园服务后
  再次进入图书馆，也始终从模式选择页开始；不修改六模块首页。
- **线上图书馆 / 馆藏查询**：按关键词、分类查询书目，展示按馆藏地汇总的馆藏数和可借数；
  选择书目和取书馆藏地后预约，不在网页检索结果中直接借走实体书。
  关键词同时匹配书目元数据（书名、作者、ISBN、分类名、出版社、语种、出版年）与实体单册
  （索书号、馆藏条码），所以拿着书脊上的索书号或条码就能直接查到它在哪个馆藏地。
  结果分页展示（每页 20 条），底部给出"第几页 / 共几页"与上一页、下一页按钮。
- **线上图书馆 / 我的图书馆**：分为当前借阅、历史借阅和我的预约。当前借阅支持合规续借；预约页显示状态、排队位次、
  分配条码和取书截止时间，排队中或待取预约可以取消。
- **模拟自助终端**：复用当前登录会话，扫描或输入实体书馆藏条码后先由服务器预检；界面展示
  书名、单册状态和当前用户可执行的操作，只启用合法的借书或归还按钮。正式提交时服务器仍会
  在事务内重新校验，成功后刷新预约与借阅状态。
- **图书管理员工作台**：仅图书馆模块管理员和超级管理员可见，详见
  [管理员维护说明](library-admin-maintenance.md)。

所有表格只读、单选，不允许拖动列或列宽；网络调用通过 `SwingWorker` 执行，不阻塞 EDT。

## 最终公共契约

| Action | Request.data | 成功 Response.data | 权限 |
|---|---|---|---|
| `LIBRARY.SEARCH_BOOKS` | `BookSearchRequest` | `BookSearchResult` | 已登录 |
| `LIBRARY.GET_BORROW_RECORDS` | `null` | `List<BorrowRecordDTO>` | 已登录 |
| `LIBRARY.RENEW_BORROW` | `BorrowRecordIdRequest(recordId)` | `BorrowRecordDTO` | 已登录且为借阅本人 |
| `LIBRARY.PAY_FEE` | `BorrowRecordIdRequest(recordId)` | `BorrowRecordDTO` | 已登录且为借阅本人；经校园卡扣款 |
| `LIBRARY.REPORT_LOST` | `BorrowRecordIdRequest(recordId)` | `BorrowRecordDTO` | 已登录且为借阅本人；注销单册并产生赔偿 |
| `LIBRARY.BORROW_COPY` | `CopyBorrowRequest(barcode)` | `null` | 已登录 |
| `LIBRARY.RETURN_COPY` | `CopyReturnRequest(barcode)` | `null` | 已登录 |
| `LIBRARY.INSPECT_COPY` | `CopyInspectionRequest(barcode)` | `CopyInspectionDTO` | 已登录 |
| `LIBRARY.CREATE_RESERVATION` | `CreateReservationRequest(bookId, pickupLocation)` | `ReservationDTO` | 已登录 |
| `LIBRARY.GET_MY_RESERVATIONS` | `null` | `List<ReservationDTO>` | 已登录 |
| `LIBRARY.CANCEL_RESERVATION` | `ReservationIdRequest(reservationId)` | `ReservationDTO` | 已登录且为预约本人 |
| `LIBRARY.LIST_CATEGORIES` | `null` | `List<BookCategoryDTO>` | 已登录 |
| `LIBRARY.ADD_BOOK_CATEGORY` | `AddBookCategoryRequest(categoryName)` | `BookCategoryDTO` | 图书管理员 |

V1 的 `BORROW_BOOK`、`RETURN_BOOK`、`UPDATE_STOCK`、`DELETE_BOOK` 以及对应请求 DTO 已删除，
客户端和服务器均不保留临时兼容入口。

## 借还规则与一致性

- 当前用户由 `Request.token -> SessionInfo.userId` 确认，请求不能指定 `userId`；
- 只允许借 `ACTIVE` 书目下状态为 `AVAILABLE` 的实体单册，或当前用户本人预约的 `RESERVED` 单册；
- `availableCount` 表示当前业务可借数，`INACTIVE` 书目的值固定为 0，即使仍有状态为 `AVAILABLE` 的单册；
- 最多同时借 5 本；同一书目有未归还记录时不能再借另一个副本；存在逾期未还时拒绝新借阅；
- 借期固定 30 天，时间由服务器生成；
- 本人当前、未逾期的记录最多续借 1 次，从原到期日顺延 30 天；同书目存在有效预约时不能续借；
- 借书必须同时完成 `BookCopy -> LOANED` 和创建 `BORROWED` 记录；
- 归还必须同时完成记录 `-> RETURNED`、写入 `returnTime` 和单册 `-> WAITING_SHELVING`；
- 归还后不会立即恢复可借数，管理员确认归架后才变为 `AVAILABLE`；
- 书目停用不妨碍已有借阅归还；逾期由 `BORROWED && now > dueTime` 动态计算；
- 同一单册同一时刻最多一条 `BORROWED` 记录，`LOANED` 与当前借阅记录保持对应。

预约规则：

- 预约面向“书目 + 取书馆藏地”，请求不接受 `userId`、`copyId`；有可借单册时立即保留 24 小时，
  这里把工作人员找书并送到取书点简化为系统自动配书；无可借单册时按
  `createdAt + reservationId` 稳定 FIFO 排队；
- 最多同时存在 3 条有效预约；逾期未还、已借同书目、同书目已有有效预约或 7 天爽约冷却期内拒绝；
- 归架、新增或恢复单册会优先分配给同书目同馆藏地的队首；取消或过期释放单册并顺延下一人；
- 借预约单册时，预约 `-> FULFILLED`、单册 `-> LOANED`、创建 `BORROWED` 记录必须同一事务成功；
- `RESERVED` 不计可借数但计入馆藏数，且不能被他人借阅、编辑或注销；停用书目会取消其有效预约；
- 系统不使用后台定时器，而是在检索、预约、借还和管理状态操作前原子清理到期预约。

费用规则：

两类费用互斥，一条记录最多产生一笔：

- **逾期滞纳金**（已归还且未申报丢失）：`min(逾期天数 × 0.5 元, 50 元)`；
  恰好到期时刻归还不算逾期、不计费。未归还的逾期记录不产生金额，
  此时仍由"存在逾期未还"拦截借阅与预约；
- **丢书赔偿**（读者申报丢失）：`书价 + 5 元手续费`。申报成功后单册立即转为 `WITHDRAWN`，
  记录同时关闭并写入 `lostReportedAt`，三件事在同一事务内完成。既逾期又丢失时**只算赔偿**，
  不叠加滞纳金。书若找回，管理员用既有的「恢复单册」把它变回可借；
- 书价来自 `tblBook.priceFen`（管理员在书目维护中填写，0.01–9999.99 元，必填）；
- 金额**不落库**：只保存结清时间 `feeSettledAt`，其余由 `dueTime`、`returnTime`、
  `lostReportedAt` 与书价推导。这些输入一旦写入就不再变化，因此金额稳定且不会漂移；
- 存在未结清费用时，借书与预约分别返回 `LIBRARY_OUTSTANDING_FEE`；结清后立即恢复；
- 缴费经校园卡 `debit` 完成，商户号为 `LIBRARY`、reference 为 `fee:<recordId>`。
  按 `recordId` 在 `circulationLock` 内串行，重复提交因记录已结清而被拒绝
  （`LIBRARY_FEE_NOT_PAYABLE`）；钱包侧的 `商户号 + reference` 幂等是第二道防线；
- 扣款成功但本地写入失败时，用 `fee-refund:<recordId>` 补偿退款，读者不会被多扣；
- 余额不足由卡模块返回 `CARD_INSUFFICIENT_BALANCE`，图书馆原样透传。

丢书赔偿**尚未实现**：`tblBorrowRecord.lostReportedAt` 已预留，单册可复用 `WITHDRAWN` 状态，
接入时按书价加手续费计算赔偿。

`LibraryService.circulationLock` 负责同一服务器实例内的串行校验。正式 Access 版本由
`AccessLibraryStore` 在事务开始时绑定一个 JDBC Connection，Book、BookCopy、BorrowRecord 与
Reservation 的 Access Repository 在一次业务中复用该连接；任一步失败都会回滚整个事务。
InMemory 版本仍通过第二步失败时补偿第一步维持测试状态一致。

## 首次启动演示数据

仅当本次首次创建整套图书馆表且借阅、预约业务表为空时，服务器为 `20260006`（`U-STUDENT-001`）和
`20260003`（`U-LIBRARY-ADMIN-001`）原子初始化：

- `Java编程思想 / SEU-B001-001`：当前借阅，对应 `LOANED + BORROWED`；
- `深入理解Java虚拟机 / SEU-B002-001`：历史借阅，对应 `AVAILABLE + RETURNED`；
- `数据结构（Java语言描述） / SEU-B003-001`：预约待取，对应 `RESERVED + READY_FOR_PICKUP`。
- 图书管理员 `20260003` 的 `史记 / SEU-B005-002`：逾期未还，用于演示禁止借书、预约和续借。

已有数据库不会补种，演示状态在重启后保持；用户取消或完成演示预约后，后续启动也不会还原。

## 测试覆盖

- Service：关键词和分类校验、馆藏地汇总、停用书目过滤；
- 检索：索书号与条码命中所属书目、大小写不敏感、逐页取完与全量一致且不重不漏、
  末页余数、越界与极大页码返回空页、非法页码与页大小被拒、分类过滤先于分页；
- 借还：条码定位、30 天借期、一次续借、预约阻止续借、五本上限、同书目重复、逾期停借、他人归还拒绝；
- 费用：逾期 0/1/15/封顶天数的费率、未归还不计费、缴费幂等、余额不足、
  扣款后本地写入失败补偿退款、未缴费用阻止借书与预约且结清后恢复；
- 丢书：赔偿为书价加手续费、单册转已注销、记录关闭并标记、既逾期又丢失时只算赔偿、
  重复申报与申报已归还记录被拒、他人不能代为申报；
- 状态机：`AVAILABLE -> LOANED -> WAITING_SHELVING -> AVAILABLE`，最后一步由管理员确认归架；
- 一致性：同一条码并发只有一次借阅成功，任一写入失败不留下半完成状态；
- 预约：立即保留、无库存排队、稳定 FIFO、三条上限、逾期停约、同书防重复、24 小时过期、
  7 天冷却、取消顺延、停用书目取消预约；
- Socket：真实登录、线上预约、我的图书馆、预约条码借书、归还、管理员上架及状态刷新；
- 终端预检：可借、本人借出、本人预约、他人预约、待上架和已注销状态只启用合法操作；
- Access：五类 Repository 映射、唯一索引、预约/单册/借阅三方失败回滚，以及多次服务器重启后的状态；
- 演示种子：仅新建图书馆表时写入、四组关联不变量、写入失败整体回滚、已有空业务表不补种；
- Swing：模式选择、预约按钮状态、取消状态、条码归属反馈、只读单选表格和清晰中文错误信息。

完整验证命令：`mvn clean verify`。
