# 数据库说明

最终验收数据库为 Access `vCampus.accdb`。数据库只能由服务器的 Repository/DAO 访问，客户端页面不能直接执行 SQL。

## 1. 仓库中保存什么

- `schema/<module>.md`：各模块的表、字段、索引、约束和迁移说明。
- 虚构且可重复初始化的演示数据或脚本。
- 不提交个人电脑上的 `.accdb`、真实学号、联系方式、病历、明文密码或其他隐私数据。

最终二进制数据库由数据库集成人根据已评审的数据字典统一生成。Access/JDBC 最小连接实验通过前，各模块仍可完成页面、Action/DTO、Service、Repository 接口、测试和数据设计。

## 2. 数据归属

| 模块 | 数据字典 | 本模块拥有的数据 | 可引用的外部 ID |
|---|---|---|---|
| 用户管理 | [user.md](schema/user.md) | 账号、全局角色、管理范围 | 无 |
| 学生学籍 | [student.md](schema/student.md) | 学生、院系、专业、班级 | `userId` |
| 选课系统 | [course.md](schema/course.md) | 课程、开课、选课、成绩 | `userId`、`studentId` |
| 图书馆 | [library.md](schema/library.md) | 图书、馆藏、借阅记录 | `userId`、`studentId` |
| 商店 | [shop.md](schema/shop.md) | 商品、库存、购物车、订单 | `userId` |
| 医院 | [hospital.md](schema/hospital.md) | 科室、医生、排班、号源、预约 | `userId`、`studentId` |

规则：

- 表名使用 `tbl<PascalCase>`，字段使用 `lowerCamelCase`。
- 模块只能通过自己的 DAO 更新自己拥有的表。
- 跨模块保存稳定 ID，不复制密码、姓名等由其他模块维护的数据。
- 需要读取其他模块信息时，使用经过评审的只读服务或 Action。
- 唯一性、容量、库存和号源等约束必须由服务器校验；能落到数据库约束的同时建立约束。

## 3. 数据字典写法

每个 `schema/<module>.md` 至少包含：

1. 模块、对应 Epic 和状态（草稿/已评审/已实现）。
2. 表清单：业务含义、主键和重要约束。
3. 每张表的字段：Access 类型、必填、默认值和说明。
4. 外键含义、唯一索引、普通索引及删除/更新策略。
5. 一组正常演示数据和一组边界数据。
6. 尚未决定、需要共同评审的问题。

字段表示例：

| 字段 | Access 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `exampleId` | Long Integer | 是 | 自动编号 | 主键 |
| `status` | Short Text(20) | 是 | `ACTIVE` | 业务状态 |

## 4. 演示数据

- 只使用明显虚构的数据。
- 数据应能覆盖正常、空结果、边界和无权限演示。
- 初始化过程必须可重复，不依赖某位成员电脑上的手工操作。
- 测试账号统一使用项目文档中公开的开发期密码；正式交付前更换并保存慢哈希值。
