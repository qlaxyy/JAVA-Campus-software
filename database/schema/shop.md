# 校园商店数据字典

- 状态：商品、库存、照片、购物车、订单和订单明细均已接入 Access。
- 身份：所有用户业务按 `userId` 关联；商店管理权限来自 `AdminScope.SHOP`。
- 支付：校园卡余额和流水归校园卡模块维护，见 [card.md](card.md)。

## 表清单

| 表 | 用途 | 关键约束 |
|---|---|---|
| `tblShopCategory` | 商品分类 | 分类名唯一 |
| `tblShopProduct` | 商品、价格和库存 | `stockQty >= 0`；状态为 `ON_SALE` / `OFF_SALE` |
| `tblShopProductPhoto` | 商品照片 | 按商品和顺序号关联 |
| `tblShopCartItem` | 服务器购物车 | `(userId, productId)` 唯一；换客户端仍可查看 |
| `tblShopOrder` | 订单头 | `orderId`、`orderSequence` 唯一；状态为 `PAID` / `CANCELLED` |
| `tblShopOrderItem` | 订单行 | `(orderId, lineNumber)` 唯一；保存商品名和成交价快照 |

校园卡表不归商店所有：`tblCampusCard` 保存余额，`tblCampusCardLedger` 保存充值、扣款和退款流水。

## 主要 Action

- 商品：`SHOP.LIST_PRODUCTS`、`SHOP.LIST_CATEGORIES`、`SHOP.ADD_CATEGORY`、`SHOP.PUBLISH_PRODUCT`、`SHOP.UPDATE_PRODUCT`。
- 购物车：`SHOP.GET_CART`、`SHOP.SET_CART_ITEM`、`SHOP.CLEAR_CART`。
- 交易：`SHOP.GET_CAMPUS_CARD`、`SHOP.RECHARGE_CAMPUS_CARD`、`SHOP.CREATE_ORDER`、`SHOP.LIST_ORDERS`、`SHOP.CANCEL_ORDER`、`SHOP.LIST_SALES`。
- 独立校园卡入口：`CARD.GET`、`CARD.RECHARGE`、`CARD.LIST_LEDGER`。

## 一致性规则

Access 模式下，商店下单的余额扣减、带 `stockQty >= quantity` 条件的库存扣减、订单头和订单明细写入在同一 JDBC 事务中完成；任一步失败都会整体回滚。同一服务器进程还会串行执行结算，避免重复付款。医院和图书馆通过校园卡网关完成独立费用结算。

最终数据库为全部 39 个演示账号各初始化 100 元余额。商品与分类沿用商店负责人现有种子数据，购物车和订单在服务器重启及更换客户端后仍保留。
