# 图书馆：我的借阅与归还交付说明

## 范围与依据

依据模块 [Epic #6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6) 的个人记录查询、归还规则和用户确认的“我的借阅＋归还＋清晰的成功提示”实现。

保留现有 Swing → ClientContext → Socket → CampusServer → ActionRouter → LibraryServerModule → LibraryService → Repository 分层。保留此前尚未提交的单选修复。不修改现有搜索/借阅 DTO，也不修改 Epic。

本次不实现分类契约调整、模块管理员维护图书、续借、预约或 Access 持久化。后续图书管理员使用现有 `SessionInfo.canAdminister(ModuleNames.LIBRARY)`，不新增全局角色。

## 如何演示

1. 在仓库根目录执行 `mvn clean verify`，按根 README 启动服务端和客户端；已运行的旧进程需要重新启动。
2. 使用公开测试账号 `student001 / 123456` 登录，进入图书馆。
3. “馆藏查询”中搜索 `Java`，选中一本有库存的书，点击“借阅选中图书”。表格仍只允许单选。
4. 借阅结果独立显示，不会被“找到几本书”的查询提示覆盖。点击顶部“我的借阅”标签；不再提供重复的跳转按钮。
5. “当前借阅”显示书名、借阅时间、到期时间、归还时间、状态和服务端计算的逾期标记。
6. 选中本人当前借阅，点击“归还选中图书”。归还成功后该行进入“历史借阅”，保存实际归还时间；历史页禁止再次归还。
7. 切回“馆藏查询”，自动重新查询上次关键词，可见库存恢复。

借还模拟柜台/自助终端登记，不表示远程获取实体图书。当前数据仍在内存中，服务器重启后清空借阅记录并恢复演示库存。

## 接口说明

仅新增 Epic 已列明的 Action、DTO 和错误码，不更改已存在的网络字段：

| Action | Request.data | 成功 Response.data |
|---|---|---|
| `LIBRARY.GET_BORROW_RECORDS` | `null` | `List<BorrowRecordDTO>`，服务端以可序列化 `ArrayList` 封装 |
| `LIBRARY.RETURN_BOOK` | `BookReturnRequest(recordId)` | `null` |

`BorrowRecordDTO` 字段为 `recordId`、`bookId`、`bookTitle`、`borrowTime`、`dueTime`、`returnTime`、`status`、`overdue`。时间使用服务端现有的 `LocalDateTime`；当前借阅的 `returnTime` 为 `null`。客户端仅格式化显示，不计算借期或逾期。

查询无筛选 DTO，当前/历史分类在客户端展示层完成。请求不接受客户端指定 `userId`；带非空数据的查询请求返回 `COMMON_INVALID_REQUEST`。后续若需要分页或筛选，需另行确认公共契约。

## 关键业务规则在哪里

- `LibraryServerModule.getBorrowRecords/returnBook`：从 `Request.token` 查服务器会话，验证 DTO，再用会话 `userId` 调用 Service。未登录/失效 token 拒绝。
- `LibraryService.getBorrowRecords`：查询本人所有记录，按借阅时间倒序返回，动态计算逾期；到期时刻恰好相等不算逾期，已归还记录不算逾期未还。
- `LibraryService.returnBook`：先验证归属，再拒绝重复归还；历史不删除。不存在和他人记录使用相同错误码，不泄露他人记录状态。
- `BorrowRecord.returnedAt`：生成包含 `returnTime` 的不可变已归还记录，不在原对象上提前修改状态。
- `LibraryService.circulationLock`：借阅、归还和两个查询使用同一锁；归还库存加一后更新记录，记录更新失败则扣回本次增加的库存。归还逾期书后可恢复借阅资格；归还释放五本上限中的名额。
- `InMemoryBorrowRecordRepository`：以唯一 `recordId` 保存不可变记录，支持本人查询、按 ID 查询和替换；插入/更新失败不得留下部分变更。
- `LibraryPanel/MyBorrowPanel`：后台线程发送请求，EDT 更新页面；单选、工作中禁用重复提交，排序后通过视图行转换取得正确记录 ID。三个表格均禁止拖动列头换位和手动拖拽列宽，保留滚动、点击排序及随窗口大小自动布局。
- 成功结果与刷新状态分开显示；成功后刷新失败不误报为借还失败。网络断开可能无法确认写操作结果，提示用户刷新本人记录核对，不自动重试借还请求。

这里是 InMemory V1 的锁与失败补偿，不是数据库事务。服务端组装仍只建立一套 LibraryService/Repository；不能用多个独立 Service 锁保护同一份共享库存。Access 阶段应在同一连接事务内同时修改库存与借阅记录，支持失败回滚和持久化。

## 改动文件

以下路径均相对仓库根目录；common/server/client Java 文件分别位于对应模块的 `src/main/java/edu/seu/vcampus/` 下。

| 位置 | 文件 | 作用 |
|---|---|---|
| common/library | `LibraryActions.java`、`BookReturnRequest.java`、`BorrowRecordDTO.java` | Epic 对应的 Action 和传输对象 |
| common/protocol | `ErrorCodes.java` | 新增记录不存在、已归还两个错误码 |
| server/module/library | `LibraryServerModule.java`、`LibraryService.java` | 鉴权入口、本人记录查询、归还及一致性规则 |
| server/module/library | `BorrowRecord.java`、`BorrowRecordRepository.java`、`InMemoryBorrowRecordRepository.java` | 归还时间、历史查询、不可变记录更新 |
| client/module/library | `LibraryClientModule.java`、`LibraryPanel.java` | 模块内导航、持久结果提示、保留单选修复 |
| client/module/library | `MyBorrowPanel.java`、`LibraryMessages.java` | 当前/历史借阅页、归还操作和中文错误提示 |
| server 的 test/module/library | `LibraryReturnServiceTest.java`、`LibraryServerModuleTest.java`、`LibraryServiceTest.java` | 归还、鉴权、并发、失败补偿与旧测试适配 |
| client 的 test/module/library | `LibrarySearchIntegrationTest.java`、`LibraryWorkflowUiTest.java` | 真实 Socket 与 Swing 操作回归 |
| 先前保留的 client 测试 | `LibraryPanelTest.java` | 单选、Ctrl/Shift 和全选限制；本轮未改动 |
| 文档 | `database/schema/library.md`、本文件 | 数据一致性规则、交付和验证说明 |

## 测试覆盖与复现

- Service：正常归还、保存历史与服务器归还时间、重复归还、他人/不存在记录、空 ID、本人当前和历史隔离、逾期边界、归还后恢复借阅资格与数量名额。
- 一致性：记录更新失败补偿库存、库存增加失败不改记录、书目丢失拒绝归还、并发归还只恢复一份库存、查询等待归还两步完成。
- Handler：匿名/失效 token、错误 DTO、禁止查询指定其他用户、记录归属、业务错误码和 requestId。
- Socket：真实登录后借书、查记录、拒绝他人归还、归还、查询历史、库存恢复、再次借阅；覆盖新 DTO 序列化。
- Swing：实际按钮与顶部标签导航完整闭环、没有重复跳转按钮、三个表格拖拽后列顺序/宽度不变且仍能点击排序、排序后按正确 recordId 归还、当前/历史单选、历史禁用归还、工作中重复点击、空/错误响应、成功后刷新失败、断网结果不确定提示。
- 先前借书上限、无库存、重复借、逾期停借、最后一本并发借阅和单选测试保留。

完整验证：`mvn clean verify`。

最新验证结果（2026-09-04）：在 `codex/library-6-return-book` 分支衔接最新 `main`（`c68de51`）后，`mvn clean verify` 为 `BUILD SUCCESS`；服务端 60 个、客户端 60 个，共 120 个测试，0 失败、0 错误、0 跳过。保留其他模块新增错误码，未覆盖其实现。此前界面调整阶段的 94 个测试亦全部通过，其中新增两个回归测试验证三个表格的鼠标拖拽限制与唯一标签入口；`git diff --check` 通过。

仅运行界面测试并生成真实 Swing 组件离屏预览（不打开桌面窗口）：

```powershell
mvn -pl vcampus-client -am "-Dtest=LibraryWorkflowUiTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dlibrary.captureUi=true" test
```

预览输出到 `vcampus-client/target/library-current-borrows.png` 和 `vcampus-client/target/library-return-history.png`，由真实 Socket 借还流程产生的数据渲染，不是静态设计稿。`target` 内文件不提交。
