# JAVA Virtual Campus（虚拟校园）

本仓库用于 6 人小组完成 Java 专业技能实训项目。范围为全部必做模块加医院选做模块：

1. 用户管理（注册/注销、登录/登出、授权）
2. 学生学籍管理
3. 选课系统
4. 图书馆
5. 商店
6. 医院（选做）

项目采用 Java C/S 架构，团队统一使用 JDK 25；客户端使用 Swing，客户端与服务器端通过 Socket 通信，服务器端支持多客户端并发，验收数据库以课程要求的 Access 为准。

## 新成员从这里开始

**每次开工先看：[团队开发通知（固定入口）](docs/团队开发通知.md)。** 今后的轮次目标、截止时间和统一通知都更新在该页面，群内只需转发这一链接。

请按顺序阅读：

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
