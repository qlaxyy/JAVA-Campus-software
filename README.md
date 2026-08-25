# JAVA Virtual Campus（虚拟校园）

本仓库用于 6 人小组完成 Java 专业技能实训项目。范围为全部必做模块加医院选做模块：

1. 用户管理（注册/注销、登录/登出、授权）
2. 学生学籍管理
3. 选课系统
4. 图书馆
5. 商店
6. 医院（选做）

项目采用 Java C/S 架构，团队统一使用 JDK 25；客户端使用 Swing，客户端与服务器端通过 Socket 通信，服务器端支持多客户端并发，验收数据库以课程要求的 Access 为准。

## 开发入口：先看这里

无论手写还是使用 AI 生成代码，开始开发前都必须先确认以下内容：

1. **[团队开发通知（固定入口）](docs/团队开发通知.md)**：当前轮次、时间、完成标准和最新通知。
2. **[需求基线](docs/01-需求基线.md)**：必须实现什么、哪些内容不做。
3. **[总体架构与接口约定](docs/03-总体架构与接口约定.md)**：分层、Action、DTO、错误码、数据库和并发规则。
4. **[GitHub 协作规范](docs/05-GitHub协作规范.md)**：Issue、分支、Pull Request 和评审规则。
5. **[成员并行开发开工指南](docs/12-成员并行开发开工指南.md)**：每个人能改哪些目录、第一轮怎样交付。
6. **[开发期基础登录与会话](docs/13-开发期基础登录与会话.md)**：其他模块怎样复用登录状态和服务器权限校验。

今后的轮次通知统一更新在“团队开发通知”页面，群内只需转发该固定链接。

## 项目结构（已确定，未经评审不要改变）

```text
JAVA-Campus-software/
├─ pom.xml                 Maven 父工程和统一版本约束
├─ vcampus-common/         客户端/服务器共享的协议、Action、DTO、角色和错误码
├─ vcampus-client/         Swing 页面、ClientContext、网络客户端
├─ vcampus-server/         Socket、线程池、业务服务、权限校验和未来 DAO
├─ database/
│  ├─ schema/              六模块数据字典和表归属
│  └─ seed/                虚构演示数据与初始化说明
├─ docs/
│  ├─ modules/             六模块设计文档
│  ├─ decisions/           架构决策 ADR
│  ├─ progress/            当前状态、推进日志、风险和追踪记录
│  ├─ design/              软件设计说明书持续草稿
│  ├─ testing/             测试计划持续草稿
│  └─ 课程原始材料/        教师原始要求
├─ scripts/                环境检查和公共脚本
└─ .github/                Issue 与 Pull Request 模板
```

依赖方向固定为：`vcampus-client → vcampus-common ← vcampus-server`。`vcampus-common` 不能依赖 Swing、Socket 实现、DAO 或数据库连接；客户端不能直接访问数据库。

## 六个模块的固定开发区域

每位负责人原则上只修改自己模块对应的五个位置。点击下表可直接查看当前设计和数据字典：

| 模块 | 模块设计 | 数据字典 | 包名 `<module>` |
|---|---|---|---|
| 用户管理 | [user.md](docs/modules/user.md) | [user.md](database/schema/user.md) | `user` |
| 学生学籍 | [student.md](docs/modules/student.md) | [student.md](database/schema/student.md) | `student` |
| 选课系统 | [course.md](docs/modules/course.md) | [course.md](database/schema/course.md) | `course` |
| 图书馆 | [library.md](docs/modules/library.md) | [library.md](database/schema/library.md) | `library` |
| 商店 | [shop.md](docs/modules/shop.md) | [shop.md](database/schema/shop.md) | `shop` |
| 医院 | [hospital.md](docs/modules/hospital.md) | [hospital.md](database/schema/hospital.md) | `hospital` |

把 `<module>` 替换为上表包名后，五个可开发区域为：

```text
vcampus-common/src/main/java/edu/seu/vcampus/common/<module>/
vcampus-client/src/main/java/edu/seu/vcampus/client/module/<module>/
vcampus-server/src/main/java/edu/seu/vcampus/server/module/<module>/
docs/modules/<module>.md
database/schema/<module>.md
```

以下属于共享核心，不能由成员或 AI 擅自改动：根 `pom.xml`、公共 `Request/Response`、`CampusClient`、`CampusServer`、`MainFrame`、模块注册表、会话框架、公共错误码和数据库公共结构。确需修改时，先在 Epic 说明原因并单独建立公共契约 Issue/PR。

## 最短开发流程

```powershell
git switch main
git pull origin main
mvn clean verify
git switch -c feat/<module>-<issue编号>-<功能简述>
```

开发完成后必须同步模块设计、数据字典和自动测试，执行 `mvn clean verify`，再创建关联 Issue 的 Pull Request。禁止直接向 `main` 推送业务代码。完整流程见 [贡献指南](CONTRIBUTING.md)。

## 完整文档索引

除上面的开工必读文档外，项目规划和交付资料按以下顺序维护：

1. [共同阅读与首次行动](docs/00-共同阅读与首次行动.md)
2. [需求基线](docs/01-需求基线.md)
3. [六人分工](docs/02-六人分工.md)
4. [总体架构与接口约定](docs/03-总体架构与接口约定.md)
5. [四周交付计划](docs/04-四周交付计划.md)
6. [GitHub 协作规范](docs/05-GitHub协作规范.md)
7. [验收与提交清单](docs/06-验收与提交清单.md)
8. [会议与进度记录](docs/07-会议与进度记录.md)
9. [开发环境统一规范](docs/08-开发环境统一规范.md)
10. [组长统筹与过程留痕](docs/09-组长统筹与过程留痕.md)
11. [六个模块 Epic 创建清单](docs/10-模块Epic创建清单.md)
12. [公共工程骨架与运行](docs/11-公共工程骨架与运行.md)
13. [成员并行开发开工指南](docs/12-成员并行开发开工指南.md)
14. [开发期基础登录与会话](docs/13-开发期基础登录与会话.md)

项目最新推进情况见 [当前状态](docs/progress/CURRENT_STATUS.md) 和 [项目推进日志](docs/progress/PROJECT_LOG.md)。

持续草稿：[软件设计说明书](docs/design/SOFTWARE_DESIGN_DRAFT.md) · [测试计划](docs/testing/TEST_PLAN_DRAFT.md) · [最终提交资料登记表](docs/delivery/DOCUMENT_REGISTER.md)

决策与阶段记录：[ADR 索引](docs/decisions/README.md) · [第 0 周启动报告](docs/progress/2026-08-24-week-0.md)

课程原始材料保存在 [`docs/课程原始材料`](docs/课程原始材料)，所有成员必须阅读，规划文档不能替代教师原文。

## 当前状态

- 阶段：第一轮六模块并行开发
- 默认分支：`main`（应始终可编译、可演示）
- 开发方式：Issue → 功能分支 → Pull Request → 评审 → 合并
- 当前团队动作：公共开发基线已通过 PR #8 合并；各负责人按 [团队开发通知](docs/团队开发通知.md) 在 3—4 天内完成一条自选、低依赖、可演示的端到端功能

## 公共工程快速验证

在仓库根目录运行：

```powershell
mvn clean verify
```

构建成功后，在 IDEA 中先运行 `vcampus-server` 的 `ServerMain`，再运行 `vcampus-client` 的 `ClientMain`。客户端窗口点击“测试服务器连接”，显示 `连接成功：PONG` 即说明公共链路正常。完整步骤见 [公共工程骨架与运行](docs/11-公共工程骨架与运行.md)。

## 重要约束

- 禁止直接向 `main` 推送业务代码。
- 一人主责一个业务模块，但公共规范由全员共同遵守。
- 跨模块调用只通过已评审的公共接口/消息契约，不能直接依赖其他模块的界面或 DAO。
- 每个接口、类和公开方法应有 JavaDoc；单个 Java 文件原则上不超过 200 行。
- 最终产物名称：`vCampusClient.jar`、`vCampusServer.jar`、`vCampus.accdb`。
- 团队统一使用 JDK 25 和 Maven 3.9.16；首次开发前运行 `scripts/check-environment.ps1`。
