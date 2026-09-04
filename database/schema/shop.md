# 校园商店数据字典

- 模块：商店
- 对应 Epic：[#11](https://github.com/qlaxyy/JAVA-Campus-software/issues/11)
- 状态：草稿。当前查询链路使用内存数据，不提交个人 `.accdb`。
- 身份：订单与购物车只保存 `userId`。管理权为 `SessionInfo.canAdminister(SHOP)`，即 `AdminScope.SHOP` 或超级管理员。

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblProductCategory` | 商品分类 | `categoryId` | `name` 唯一；`status` 停用代替物理删除 |
| `tblProduct` | 商品 | `productId` | `priceFen>0`；`saleStatus` 为 `ON_SALE`/`OFF_SALE`；描述必填 |
| `tblProductPhoto` | 商品照片 | `photoId` | 同一商品 1–9 张；本轮存在内存 DTO |
| `tblCampusCard` | 虚拟校园卡 | `userId` | 演示：`student001` / `shopadmin` 各 100.00 元；本轮内存 |
| `tblCartItem` | 购物车行 | `cartItemId` | 客户端本地；同一 `userId+productId` 唯一 |
| `tblOrder` | 订单头 | `orderId` | `PAID` / `CANCELLED`；校园卡扣款；本轮内存 |
| `tblOrderItem` | 订单行 | `orderItemId` | 保存下单时名称与单价快照；本轮内存 |

## 3. 字段字典

### `tblProductCategory`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `categoryId` | Long Integer | 是 | 自动编号 | 主键 |
| `name` | Short Text(40) | 是 | | 分类名 |
| `status` | Short Text(20) | 是 | `ACTIVE` | `ACTIVE` / `DISABLED` |

### `tblProduct`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `productId` | Long Integer | 是 | 自动编号 | 主键 |
| `categoryId` | Long Integer | 是 | | 逻辑外键到分类 |
| `name` | Short Text(80) | 是 | | 商品标题 |
| `description` | Long Text | 是 | | 卖家描述 |
| `sellerName` | Short Text(40) | 是 | | 发布时的展示名 |
| `priceFen` | Long Integer | 是 | | 单价，单位分 |
| `stockQty` | Long Integer | 是 | 0 | 可售数量，不得为负 |
| `saleStatus` | Short Text(20) | 是 | `ON_SALE` | `ON_SALE` / `OFF_SALE` |

### `tblProductPhoto`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `photoId` | Long Integer | 是 | 自动编号 | 主键 |
| `productId` | Long Integer | 是 | | 逻辑外键到商品 |
| `sortNo` | Long Integer | 是 | 0 | 展示顺序，从 0 开始 |
| `content` | OLE Object | 是 | | 商品照片，上架至少 1 张、最多 9 张 |

当前查询与上架链路仍把照片放在内存 DTO 中，不写入个人 `.accdb`。

## 4. 关联与索引

- 商品逻辑关联分类，不跨模块改其他表。
- 唯一：分类名；查询索引：`saleStatus`、`categoryId`、`name`。

## 5. 演示数据（内存，后续导入 Access）

正常：文具中性笔 3.50 元数量 120，含照片与描述；日常用品抽纸；食品矿泉水。  
边界：下架「停售纪念本」「过期试吃饼干」，列表接口不得返回。  
分类编号：1 文具、2 日常用品、3 食品。  
上架：`SHOP.PUBLISH_PRODUCT` 需商店管理权，照片 1–9 张。  
校园卡演示：`student001`、`shopadmin` 各 100.00 元。

## 6. Socket 动作（校园卡支付端口）

- `SHOP.GET_CAMPUS_CARD`：查询当前登录人虚拟校园卡。
- `SHOP.RECHARGE_CAMPUS_CARD`：充值 10–100 元。
- `SHOP.CREATE_ORDER`：仅 `CAMPUS_CARD`；余额不足返回 `SHOP_INSUFFICIENT_BALANCE`，文案「余额不足，请充值！」。
- `SHOP.LIST_ORDERS` / `SHOP.CANCEL_ORDER`：我的订单与退款。
- `SHOP.LIST_SALES`：商店管理员成交列表。

## 7. 待评审问题

- Access 驱动与事务能力待 ADR-0004。
- 校园卡与订单仍为内存实现，尚未写入个人 `.accdb`。
