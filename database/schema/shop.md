# 校园商店数据字典

- 状态：商品、库存、照片、校园卡、购物车、订单和订单明细均已接入 Access。
- 身份：所有用户业务按 `userId` 关联；商店管理权限来自 `AdminScope.SHOP`。

| 表 | 用途 | 关键约束 |
|---|---|---|
| `tblShopCategory` | 商品分类 | 分类名唯一 |
| `tblShopProduct` | 商品与实时库存 | `productId` 主键；库存不得扣成负数 |
| `tblShopProductPhoto` | 商品图片 | `(productId, photoIndex)` 唯一 |
| `tblShopListingRecord` | 上架和调整历史 | 保存操作时名称及操作者姓名快照 |
| `tblCampusCard` | 校园卡余额 | `userId`、一卡通号分别唯一；金额单位为分 |
| `tblShopCartItem` | 服务器购物车 | `(userId, productId)` 唯一；换客户端仍可查看 |
| `tblShopOrder` | 订单头 | `orderId`、`orderSequence` 唯一；状态为 `PAID`/`CANCELLED` |
| `tblShopOrderItem` | 订单行 | `(orderId, lineNumber)` 唯一；保存商品名和成交价快照 |

主要 Action：

- 商品：`SHOP.LIST_PRODUCTS`、`SHOP.LIST_CATEGORIES`、`SHOP.ADD_CATEGORY`、`SHOP.PUBLISH_PRODUCT`、`SHOP.UPDATE_PRODUCT`。
- 购物车：`SHOP.GET_CART`、`SHOP.SET_CART_ITEM`、`SHOP.CLEAR_CART`。
- 交易：`SHOP.GET_CAMPUS_CARD`、`SHOP.RECHARGE_CAMPUS_CARD`、`SHOP.CREATE_ORDER`、`SHOP.LIST_ORDERS`、`SHOP.CANCEL_ORDER`、`SHOP.LIST_SALES`。

Access 模式下，余额扣减、带 `stockQty >= quantity` 条件的库存扣减、订单头和订单明细写入在同一 JDBC 事务中完成；任一步失败都会整体回滚。同一服务器进程还会串行执行结算，避免重复付款。最终数据库为全部 39 个演示账号各初始化 100 元余额。商品与分类沿用商店负责人现有种子数据。
