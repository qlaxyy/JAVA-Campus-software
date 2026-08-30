# 图书馆数据字典

## 1. 模块与负责人

- 模块：图书馆
- 对应 Epic：#6
- 状态：草稿，第一轮检索功能暂用内存演示数据

## 2. 表清单

| 表名 | 业务含义 | 主键 | 重要约束 |
|---|---|---|---|
| `tblLibraryBook` | 可检索书目及馆藏汇总 | `bookId` | ISBN 唯一，可借数量不得超过馆藏数量 |

## 3. 字段字典

### `tblLibraryBook`

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `bookId` | Short Text(20) | 是 | 无 | 业务主键，例如 `B001` |
| `isbn` | Short Text(20) | 是 | 无 | ISBN，唯一索引 |
| `title` | Short Text(200) | 是 | 无 | 书名 |
| `author` | Short Text(100) | 是 | 无 | 作者 |
| `category` | Short Text(50) | 是 | 无 | 图书分类 |
| `totalCount` | Long Integer | 是 | `0` | 馆藏总数，不得小于 0 |
| `availableCount` | Long Integer | 是 | `0` | 当前可借数，范围为 0 至 `totalCount` |

## 4. 关联与索引

- `bookId` 为主键，`isbn` 建唯一索引。
- `title`、`author`、`category` 建普通索引以支持检索。
- 后续借阅记录通过 `bookId` 逻辑关联书目；书目被引用后使用状态字段停用，不物理删除。

## 5. 演示数据

| bookId | isbn | title | author | category | totalCount | availableCount |
|---|---|---|---|---|---:|---:|
| `B001` | `9787111213826` | Java编程思想 | Bruce Eckel | 计算机 | 5 | 2 |
| `B004` | `9787020002207` | 红楼梦 | 曹雪芹 | 文学 | 6 | 0 |

第一组用于正常搜索和有库存展示；第二组用于无可借馆藏的边界展示。

## 6. 待评审问题

- 是否拆分书目与实体副本表，在借阅功能开发前评审决定。
- 馆藏数量应由副本状态实时汇总，还是由事务维护汇总字段，待数据库集成阶段决定。
