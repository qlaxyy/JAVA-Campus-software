# 校园卡数据字典

- 模块：校园卡（独立子系统，默认 TCP **8889**）
- 对应 Epic：[#11](https://github.com/qlaxyy/JAVA-Campus-software/issues/11)
- 状态：已实现首版网关。余额与流水写入 Access；商店、医院、图书馆通过 Socket 连接本端口扣款/退款/查询。
- 身份：始终使用登录会话 `userId` 归属钱包；一卡通号等于用户模块账号。

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblCampusCard` | 虚拟校园卡余额 | `userId` | 一卡通号 `cardNo` 与登录账号相同 |
| `tblCampusCardLedger` | 充值/扣款/退款流水 | `txnId` | `(merchant, reference, entryType)` 唯一，用于幂等 |

## 3. 字段字典

### `tblCampusCard`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `userId` | Short Text(36) | 是 | | 账号稳定主键 |
| `username` | Short Text(50) | 是 | | 一卡通号 |
| `cardNo` | Short Text(50) | 是 | | 展示卡号，当前等于 `username` |
| `balanceFen` | Long Integer | 是 | 0 | 余额，单位分 |
| `updatedAt` | Date/Time | 否 | | 最近变动时间 |

### `tblCampusCardLedger`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `txnId` | Short Text(36) | 是 | | 主键 |
| `userId` | Short Text(36) | 是 | | 钱包所属账号 |
| `merchant` | Short Text(20) | 是 | | `SHOP` / `HOSPITAL` / `LIBRARY` / `CARD` |
| `reference` | Short Text(80) | 是 | | 商户侧业务单号 |
| `entryType` | Short Text(20) | 是 | | `RECHARGE` / `DEBIT` / `CREDIT` |
| `amountFen` | Long Integer | 是 | | 变动金额，单位分，正数 |
| `createdAt` | Date/Time | 否 | | 记账时间 |

## 4. Socket 动作（校园卡端口，默认 8889）

其他子系统不要直连 Access 校园卡表。登录仍在校园服务端口完成，再把同一 `token` 发到本端口。

- `CARD.GET`：查询当前登录人余额。
- `CARD.RECHARGE`：自助充值 10–100 元。
- `CARD.DEBIT`：商户扣款。请求 `CardTransferRequest(amountFen, merchant, reference)`。
- `CARD.CREDIT`：商户退款。`merchant+reference+CREDIT` 与扣款相同规则幂等。
- `CARD.LIST_LEDGER`：当前人流水。
- `COMMON.PING`：探活。

余额不足返回 `CARD_INSUFFICIENT_BALANCE`，文案「余额不足，请充值！」。

## 5. 演示数据

空表首次创建时写入：`20260001`（`U-STUDENT-001`）、`20260006`（`U-SHOP-ADMIN-001`）各 100.00 元。已有行不会重置。

## 6. 调用约定

- 商店下单：`merchant=SHOP`，`reference=order:{orderId}`；退款 `order-refund:{orderId}`。
- 医院缴费：`merchant=HOSPITAL`，`reference=bill:{billId}`。
- 图书馆费用：调用 `LibraryServerModule.settleFee`，`merchant=LIBRARY`。
