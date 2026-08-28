# 虚拟校园系统软件设计说明书（持续草稿）

> 状态：持续维护，不作为当前最终提交版。带“待确认/待设计”的内容必须由后续 Issue、PR、测试或教师要求补充，禁止凭空填写。

## 文档修订记录

| 日期 | 版本 | 变更 | 证据 |
|---|---|---|---|
| 2026-08-24 | V0.1 | 根据课程材料、需求基线和架构规划建立持续草稿 | `713d91c` 至 `8b8ba9d` |
| 2026-08-25 | V0.2 | 同步三模块骨架、六模块扩展点、开发期会话、主导航、路由与测试实况 | PR #7、ADR-0007/0008、9 个自动测试 |
| 2026-08-28 | V0.3 | 同步附加管理授权、医院模块内三模式、选课批次链路和现行设计入口 | PR #24、#25、#26，27 个自动测试 |

# 1 引言

## 1.1 编写目的

本文档描述虚拟校园系统的需求、总体结构、业务模块、公共模块、网络、多线程和数据库设计，为六人并行开发、代码评审、测试、部署和答辩提供统一依据。

预期读者：项目组成员、课程教师/助教、测试和验收人员。

## 1.2 背景

- 软件名称：虚拟校园系统（JAVA Virtual Campus）。
- 任务来源：Java 专业技能实训课程项目。
- 开发团队：6 人小组；姓名、学号和联系方式不写入公开仓库，按课程分工表另行提交。
- 使用者：学生、教师、具有附加模块管理授权的人员、超级管理员，以及由各子系统维护的专业身份（如医生）。
- 系统不设置全局 `USER / MANAGEMENT` 模式；同一账号可按服务器授权进入子系统内部的普通、专业或管理工作台。现行准则见 `SYSTEM_DESIGN.md`，权限细节见 `ADMIN_PERMISSION_AND_MODE.md`。
- 运行形式：Java C/S 客户端—服务器端系统。

## 1.3 定义

| 术语 | 含义 |
|---|---|
| C/S | Client/Server，客户端/服务器架构 |
| DTO | Data Transfer Object，跨层或网络传输的数据对象 |
| DAO | Data Access Object，封装数据库访问的对象 |
| Message | 客户端与服务器端之间统一请求/响应消息 |
| action | 标识业务操作的字符串，如 `COURSE.ENROLL` |
| Assignee | GitHub Issue 当前负责人 |
| ADR | Architecture Decision Record，架构决策记录 |
| Epic | 覆盖一个模块完整目标的上层 Issue |

## 1.4 参考资料

- `docs/课程原始材料/软件实践安排(202509).docx`
- `docs/课程原始材料/软件设计说明书DEMO(20250825).docx`
- `docs/01-需求基线.md`
- `docs/03-总体架构与接口约定.md`
- 根目录 `README.md` 中的统一环境与运行教程
- 后续 ADR、模块 Issue、PR 和测试记录。
- `docs/decisions/ADR-0001-JDK25与Maven版本.md`
- `docs/decisions/ADR-0002-GitHub-Issue自主认领.md`
- `docs/decisions/ADR-0005-组长统一分配模块.md`（现行分配规则）
- `docs/decisions/ADR-0006-统一IntelliJ-IDEA-Community.md`
- `docs/decisions/ADR-0003-CS三模块架构.md`
- `docs/decisions/ADR-0004-Access与JDBC方案.md`
- `docs/decisions/ADR-0007-六模块独立扩展点.md`
- `docs/decisions/ADR-0008-开发期内存认证基线.md`

# 2 程序系统的分析

## 2.1 可行性分析

### 技术可行性

Java 25 提供 Swing、Socket、多线程、序列化和 JDBC 等项目所需能力。Maven 用于统一项目结构、依赖与打包，GitHub 用于任务、评审和版本管理。课程要求的 Access/JDBC 具体方案尚待最小连接验证和 ADR 确认。

已通过环境与总体结构决策见 ADR-0001、ADR-0003；数据库方案仍处于 ADR-0004 提议状态。

### 操作可行性

客户端采用统一登录、主导航和模块页面。通过演示账号与初始化测试数据降低验收操作成本。最终操作流程需由界面原型和用户测试验证。

### 进度可行性

六位成员分别主责六个业务模块，公共协议、数据库、UI、测试和文档工作通过公共 Issue 协同。项目按四周八阶段推进，并在第三周末冻结功能。

### 主要风险

当前主要风险为 Access/JDBC 未确认、业务 action/DTO 尚待各模块评审以及业务功能仍处于第一轮开发。六个模块 Epic、公共骨架和独立扩展位置已经建立，降低了并行集成风险。详见 `docs/progress/RISK_REGISTER.md`。

## 2.2 需求分析

系统必须包含：

1. 用户管理：注册、注销、登录、登出、授权。
2. 学生学籍：学生、院系、专业、班级和学籍维护。
3. 选课系统：课程、开课、选退课、课表和成绩。
4. 图书馆：图书、馆藏、借阅和归还。
5. 商店：商品、库存、购物车和订单。
6. 医院：科室、医生、排班/号源和预约。

全局需求：统一身份与权限、服务器端校验、多客户端并发、统一错误码、异常友好提示、关键操作审计、可初始化演示数据。完整基线见 `docs/01-需求基线.md`。

## 2.3 开发设计环境

### 2.3.1 集成开发环境

团队统一使用 IntelliJ IDEA Community Edition，通过仓库根目录 `pom.xml` 导入项目。Project SDK、语言级别、Maven Importer 和 Maven Runner 均设置为 JDK 25；最终提交版在全员安装完成后补充实际 IDEA 版本号。

### 2.3.2 JDK 25

教师已说明 Java 版本不限，团队统一使用 JDK 25；Maven 编译 release/source/target 固定为 25。

### 2.3.3 数据库

验收数据库文件名为 `vCampus.accdb`。Access 版本、JDBC 驱动与连接方式待机房环境确认及原型验证后写入 ADR。

### 2.3.4 Maven 与第三方组件

- Apache Maven 3.9.16。
- Maven Compiler Plugin 3.15.0。
- Maven Enforcer Plugin 3.6.3。
- JDBC 驱动、日志、测试库等依赖待技术选型后记录准确版本和用途。

### 2.3.5 协作环境

- Git for Windows 2.x。
- GitHub Issue → 功能分支 → Pull Request → 评审 → 合并。
- UTF-8 编码，Asia/Shanghai 时区。

# 3 程序系统的结构

## 3.1 逻辑结构

```text
Swing View
    ↓
Client Service
    ↓ Message/Socket
Server Request Dispatcher
    ↓
Server Business Service
    ↓
DAO/JDBC
    ↓
Access Database
```

## 3.2 Maven 模块

已建立：

- `vcampus-common`：Message、DTO/VO、枚举、错误码。
- `vcampus-client`：Swing 页面、客户端服务、网络连接。
- `vcampus-server`：请求分发、业务服务、DAO、线程池和数据库连接。

状态：三模块骨架和固定六模块扩展位置均已合并；学籍、选课和医院已经分别形成第一条可运行链路。

## 3.3 业务模块关系

- 所有模块依赖用户模块提供统一身份、会话和权限。
- 选课依赖学籍有效状态。
- 图书馆依赖用户/学籍身份，但拥有自己的借阅数据。
- 商店只依赖用户身份，不依赖未选择的银行模块。
- 医院依赖用户/学籍身份，并限制敏感信息访问。

模块之间通过公共接口或经评审的只读查询协作，不直接更新其他模块的数据表。

客户端由固定 `ClientModules` 目录生成六个主导航入口，服务器由固定 `ServerModules` 目录把六个模块注册到 `ActionRouter`。每位负责人只修改本模块目录，避免共同修改主窗口和 Socket 分发核心；共享清单变更按 ADR-0007 评审。

# 4 用户管理模块设计

## 4.1 已确认需求

注册、注销、登录、登出、账户启停、角色/权限、会话校验和登录审计。密码不得明文存储，所有受保护请求在服务器端授权。当前仅实现可替换的开发期虚构账号、随机 token、会话查询和登出，以解除其他模块的开发阻塞。

## 4.2 待形成设计证据

- 登录/注册/账户管理界面原型及截图。
- 登录、退出、权限校验流程图。
- User/Session/Role/Permission 实体与表结构。
- `USER.*` action、请求/响应 DTO、错误码。
- Client/Server Service 接口及测试。

状态：Epic [#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1) 已分配；开发期基础登录已实现并通过 2 个 Socket 测试，但 Access 用户 DAO、正式密码策略、账户/权限管理、过期和审计均未实现，不能判定用户模块完成。

# 5 学生学籍模块设计

## 5.1 已确认需求

学生、院系、专业、班级和学籍状态；学生查看允许信息；管理员增查改停用；向选课、图书馆和医院提供稳定只读身份查询。

## 5.2 待形成设计证据

- 个人学籍、学生管理和基础数据界面。
- 学籍新增/修改/停用流程。
- Student/Department/Major/Class 实体、约束和表结构。
- `STUDENT.*` action、DTO、错误码和跨模块查询接口。

状态：Epic [#2](https://github.com/qlaxyy/JAVA-Campus-software/issues/2) 已分配，个人学籍查询链路已实现，维护功能待开发。

# 6 选课系统模块设计

## 6.1 已确认需求

课程、开课班、教师、容量、时间地点、选退课、课表和成绩；校验重复、容量、冲突、开放状态和成绩范围。

## 6.2 待形成设计证据

- 课程查询、选课、课表、成绩管理界面。
- 选课/退课和成绩录入流程图。
- Course/Offering/Enrollment/Score 实体和表结构。
- `COURSE.*` action、DTO、错误码。
- 最后名额的并发一致性测试。

状态：Epic [#3](https://github.com/qlaxyy/JAVA-Campus-software/issues/3) 已分配，选课批次列表已实现；课程列表、选退课、课表和成绩仍待开发。

# 7 图书馆模块设计

## 7.1 已确认需求

图书、分类、馆藏、检索、借阅、归还和借阅记录；校验库存、重复借阅、借阅上限和记录状态。

## 7.2 待形成设计证据

- 图书检索、我的借阅、借还和图书维护界面。
- 借阅/归还状态流转图。
- Book/Inventory/Loan 实体和表结构。
- `LIBRARY.*` action、DTO、错误码。
- 最后一本书的并发测试。

状态：Epic [#6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6) 已分配，业务功能未实现。

# 8 商店模块设计

## 8.1 已确认需求

商品分类、商品、价格、库存、上下架、购物车、下单、订单查询、取消和模拟支付状态；不接入真实支付。

## 8.2 待形成设计证据

- 商品、购物车、订单和商品管理界面。
- 创建/取消订单状态图。
- Product/Cart/Order/OrderItem 实体和表结构。
- `SHOP.*` action、DTO、错误码。
- 库存原子扣减和失败回滚测试。

状态：商店模块 Epic #11 已建立；具体业务完成情况以后续 PR 和测试证据为准。

# 9 医院模块设计

## 9.1 已确认需求

科室、医生、排班/号源、预约、取消和预约记录；诊疗信息只保留最小必要范围并限制访问。

## 9.2 待形成设计证据

- 科室医生查询、预约、我的预约和排班管理界面。
- 预约/取消状态及流程图。
- Department/Doctor/Schedule/Appointment 实体和表结构。
- `HOSPITAL.*` action、DTO、错误码。
- 最后一个号源的并发测试和隐私检查。

状态：Epic [#4](https://github.com/qlaxyy/JAVA-Campus-software/issues/4) 已分配，患者号源查询和患者/医生/管理员三模式入口已实现；预约、诊疗与管理功能待开发。

# 10 公共模块设计

## 10.1 Message

最小骨架将消息分为 `Request` 与 `Response`：请求包含 `requestId`、`action`、`token`、`data`，响应包含 `requestId`、`success`、`code`、`message`、`data`。两者显式声明 `serialVersionUID`。当前已实现 `COMMON.PING`、`USER.LOGIN/CURRENT_SESSION/LOGOUT`、模块名校验和线程安全 `ActionRouter`；重复 action 被拒绝，handler 异常转换为安全错误码。对象过滤、协议版本和兼容策略仍待公共契约 Issue/ADR 确认。

## 10.2 公共 DTO、枚举和错误码

公共层只包含客户端和服务器端共同需要的稳定可序列化对象，不依赖 Swing、DAO、数据库连接或 Socket 实现。错误码按模块前缀划分。

## 10.3 数据库与工具类

连接配置、连接管理、异常转换、日志、日期和校验工具待实现。不得把密码、个人绝对路径或真实数据写入仓库。

# 11 网络模块设计

客户端通过 Socket 向服务器发送统一 Request；服务器完成读取、分发并返回 Response。最小骨架已固定双方先创建并刷新 `ObjectOutputStream`、再创建 `ObjectInputStream`，网络超时为 5 秒，并采用一次连接处理一次请求的模型。

状态：`COMMON.PING → PONG` 已通过自动化集成测试；长连接、认证会话、对象过滤、重连策略和正式日志仍待设计。

# 12 多线程模块设计

服务器骨架已使用固定大小线程池处理客户端连接。共享容量、库存和号源的检查与更新仍须在服务层临界区或数据库事务中原子完成。

需要形成：线程模型 ADR、线程池配置依据、多客户端并发测试和异常连接恢复测试。

# 13 数据库设计

## 13.1 元数据与命名

数据库文件名 `vCampus.accdb`，表名采用 `tbl<PascalCase>`。主键、外键、时间、状态和审计字段的准确规范待数据库 ADR 确认。

## 13.2 表设计

当前已建立 `database/schema/TABLE_OWNERSHIP.md` 和六个模块数据字典入口；具体表和字段仍是待负责人补充、待评审的候选设计。用户/权限、学生/院系/专业/班级、课程/开课/选课/成绩、图书/馆藏/借阅、商品/购物车/订单、科室/医生/排班/预约分别归对应模块维护。

每张表最终必须记录：字段、类型、主键/外键、默认值、是否为空、唯一性、检查约束、索引和业务说明。

## 13.3 E-R 图与初始化数据

待六个模块提交表设计后统一绘制 E-R 图。测试数据必须使用虚构身份，并支持重复初始化。

# 14 测试、部署与其他

- 测试依据：`docs/testing/TEST_PLAN_DRAFT.md`。
- 需求追踪：`docs/progress/TRACEABILITY.md`。
- 风险：`docs/progress/RISK_REGISTER.md`。
- 最终产物：`vCampusClient.jar`、`vCampusServer.jar`、`vCampus.accdb`、JavaDoc、使用说明、项目报告和答辩材料。

## 最终化规则

本草稿只有在以下条件满足后才能转换为最终 Word：

- 所有“待确认/待设计”均有 Issue、ADR、PR、测试或教师要求作为证据。
- 接口、类、表和界面与最终代码一致。
- 图表和截图来自实际运行版本。
- 文档版本、修改记录、目录和引用已更新。
- 按课程 Word 模板排版并完成逐页检查。
