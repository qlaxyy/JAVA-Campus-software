# JAVA Virtual Campus（虚拟校园）

6 人 Java 实训项目：完成用户管理、学生学籍、选课、图书馆、商店，以及选做医院模块。

技术基线：**JDK 25 + Maven 3.9.16 + Swing + Socket + Access/JDBC**。项目采用 C/S 三模块结构：

```text
vcampus-client  →  vcampus-common  ←  vcampus-server
   Swing 界面       请求/响应 DTO       业务、权限、DAO
```

客户端禁止直接访问数据库；权限和业务规则必须在服务器端校验。

## 现在从哪里开始

组员日常只需看 3 个入口：

1. **[当前开发通知](docs/团队开发通知.md)**：这一轮要做什么、完成标准和截止安排。
2. **[贡献指南](CONTRIBUTING.md)**：分支、提交、Pull Request 的最短流程。
3. **自己负责的模块 Epic**：维护模块设计、验收清单和已完成进度。

| 模块 | 模块 Epic | 数据字典 | 包名 |
|---|---|---|---|
| 用户管理 | [#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1) | [数据](database/schema/user.md) | `user` |
| 学生学籍 | [#2](https://github.com/qlaxyy/JAVA-Campus-software/issues/2) | [数据](database/schema/student.md) | `student` |
| 选课系统 | [#3](https://github.com/qlaxyy/JAVA-Campus-software/issues/3) | [数据](database/schema/course.md) | `course` |
| 图书馆 | [#6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6) | [数据](database/schema/library.md) | `library` |
| 商店 | 待补建 | [数据](database/schema/shop.md) | `shop` |
| 医院 | [#4](https://github.com/qlaxyy/JAVA-Campus-software/issues/4) | [数据](database/schema/hospital.md) | `hospital` |

其余文件主要供组长统筹、架构查阅和最终报告留痕，统一从 **[文档导航](docs/README.md)** 进入，不要求组员每天全部阅读。

## 第一次运行

用 IntelliJ IDEA Community 打开包含本文件和根 `pom.xml` 的仓库目录，将 Project SDK 和 Maven Runner 设为 JDK 25，然后运行：

```powershell
mvn clean verify
```

看到 `BUILD SUCCESS` 后，先运行服务器 `ServerMain`，再运行客户端 `ClientMain`。完整运行说明见 [公共工程骨架与运行](docs/11-公共工程骨架与运行.md)。

## 开发一个功能

```powershell
git switch main
git pull --ff-only origin main
git switch -c feat/<module>-<issue>-<summary>

# 编写并测试代码
mvn clean verify
git add <本次相关文件>
git commit -m "feat(<module>): 完成某项功能"
git push -u origin <分支名>
```

推送后到 GitHub 创建 `自己的分支 → main` 的 Pull Request。禁止直接在 `main` 开发或推送业务代码。

## 成员可以修改哪里

把 `<module>` 替换为上表包名：

```text
vcampus-common/src/main/java/edu/seu/vcampus/common/<module>/
vcampus-client/src/main/java/edu/seu/vcampus/client/module/<module>/
vcampus-server/src/main/java/edu/seu/vcampus/server/module/<module>/
database/schema/<module>.md
```

根 `pom.xml`、公共 Request/Response、Socket 框架、主导航、模块注册表和会话框架属于共享核心。确需修改时，先在 Issue 说明并由组长确认。

## 当前状态

- 阶段：第一轮六模块并行开发。
- 目标：每个模块先完成 1 条可演示的端到端链路。
- 基线：公共 Socket、PING/PONG、开发期登录与会话已经建立。
- 验收：Pull Request 前必须执行 `mvn clean verify`，并提供测试说明；涉及界面时附截图。

最新要求以 [当前开发通知](docs/团队开发通知.md) 为准，历史进展以 [项目推进记录](docs/progress/README.md) 为准。
