# ADR-0006：统一使用 IntelliJ IDEA Community Edition

- 状态：通过
- 日期：2026-08-24
- 决策范围：六位成员的集成开发环境
- 部分取代：ADR-0001 中“IDEA 或 Eclipse 二选一”的 IDE 选择条款

## 背景

项目初期允许成员在 IntelliJ IDEA 与 Eclipse 之间自行选择。团队进入统一环境准备阶段后，为减少 Maven 导入、JDK 配置、运行配置和远程排错方式的差异，组长决定使用同一 IDE。

## 决定

- 六位成员统一使用 IntelliJ IDEA Community Edition。
- 不依赖 Ultimate 专有功能，避免试用期或授权差异影响协作。
- 通过仓库根目录 `pom.xml` 打开项目，不为各业务模块分别创建独立工程。
- Project SDK、Language level、Maven Importer 和 Maven Runner 均使用 JDK 25。
- Maven 实际命令行版本统一为 3.9.16；不能只依赖 IDEA 内置 Maven 而跳过命令行环境检查。
- `.idea` 和个人运行配置不提交到仓库，公共配置通过 Maven、`.editorconfig` 和文档维护。

## 备选方案

### IDEA 与 Eclipse 并存

成员选择自由，但组内初次协作和远程排错成本更高，不再采用。

### IntelliJ IDEA Ultimate

功能更多，但课程项目不需要其专有功能，且可能存在授权和试用期差异，不采用。

## 影响与迁移

- 已安装 Eclipse 的成员需要安装 IDEA Community，并从仓库根 `pom.xml` 重新导入。
- 软件设计说明书开发环境章节统一写 IntelliJ IDEA Community Edition，最终版本号在全员安装后按实际情况补充。
- JDK 25、Maven 3.9.16、Git 和 UTF-8 等既有环境约束不变。

## 验证证据

- 根目录 `README.md`
- 六位成员的环境检查与 Maven 导入结果
- 最终软件设计说明书开发环境章节
