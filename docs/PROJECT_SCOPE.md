# 项目范围与分工

## 1. 项目目标

开发一个六人协作的 Java 虚拟校园 C/S 系统。最终系统具有统一登录、统一导航、统一 Socket 协议和统一数据库，而不是六个独立程序。

## 2. 技术与交付约束

- JDK 25、Maven 3.9.16、IntelliJ IDEA Community。
- Swing 客户端、Socket 通信、固定线程池服务器。
- `vcampus-common`、`vcampus-client`、`vcampus-server` 三个 Maven 模块。
- 验收数据库为 Access `vCampus.accdb`；Swing 页面不能直接执行 SQL。
- 网络传输对象实现 `Serializable`，客户端和服务器使用同一版本的 common。
- 最终生成客户端/服务器 JAR、数据库、JavaDoc 和课程要求的文档。

## 3. 六个业务模块

| 模块 | 最小范围 | Epic |
|---|---|---|
| 用户管理 | 登录、退出、账号状态、授权和会话 | [#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1) |
| 学生学籍 | 学生、院系、专业、班级和学籍维护 | [#2](https://github.com/qlaxyy/JAVA-Campus-software/issues/2) |
| 选课系统 | 课程、开课、选退课、课表和成绩 | [#3](https://github.com/qlaxyy/JAVA-Campus-software/issues/3) |
| 图书馆 | 图书、馆藏、借阅和归还 | [#6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6) |
| 商店 | 商品、库存、购物车和订单 | [#11](https://github.com/qlaxyy/JAVA-Campus-software/issues/11) |
| 医院 | 科室、医生、排班、号源和预约 | [#4](https://github.com/qlaxyy/JAVA-Campus-software/issues/4) |

不做真实支付、短信、邮件、第三方认证、Web/移动端、云部署和与核心验收无关的复杂功能。

## 4. 分工规则

- 六位成员每人主责一个业务模块，负责人以 Epic 的 Assignee 为准。
- 负责人完成本模块的页面、Action/DTO、服务器处理、Service、DAO、数据字典和测试。
- 总控维护公共框架、登录会话、模块注册、集成和合并规则，但不替子系统实现业务逻辑。
- 跨模块只传稳定 ID 或调用已评审接口，不直接修改其他模块的数据表。
- 姓名、学号、联系方式和真实业务数据不写入公开仓库。

每个负责人主要修改：

```text
vcampus-common/.../<module>/
vcampus-client/.../<module>/
vcampus-server/.../<module>/
database/schema/<module>.md
对应测试目录
```

修改公共核心或其他成员模块前必须先协调。

## 5. 功能完成标准

一项功能必须形成完整链路：

```text
Swing 页面 → ClientContext → Action/请求 DTO → Socket
→ ServerModule → Service → Repository/DAO → 响应 DTO → 页面展示
```

同时满足：

- 服务器端完成登录、权限、参数和业务规则校验。
- 正常、异常和越权流程有测试或可重复验证步骤。
- 数据库、公共协议或设计变化时同步唯一对应文档。
- `mvn clean verify` 通过，并由非作者评审 PR。

## 6. 四周阶段目标

| 阶段 | 目标 | 退出条件 |
|---|---|---|
| 第 1 周 | 范围、分工、公共骨架和首个接口 | 六个 Epic 明确；客户端—服务器最小链路通过 |
| 第 2 周 | 六模块纵向主流程 | 每个模块至少一条可演示链路，主要异常可验证 |
| 第 3 周 | 管理功能、跨模块、数据库和并发 | 权限边界明确；容量、库存、号源等无明显数据错误 |
| 第 4 周 | 功能冻结、部署、文档和答辩 | P0/P1 清零；新环境能运行；提交物和彩排完成 |

进度落后时先取消美化、统计和加分功能，不删除六个模块核心流程、服务器鉴权、异常处理和课程交付物。

## 7. 需求变更

新增或修改需求写入对应 Issue，说明动机、影响模块、协议/数据库变化和验收条件。跨模块或影响进度的变化由总控与相关负责人确认；功能冻结后只接受阻塞验收的问题修复。
