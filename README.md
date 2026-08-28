# JAVA Virtual Campus（虚拟校园）

6 人 Java 实训项目：用户管理、学生学籍、选课、图书馆、商店，以及选做医院模块。

技术基线：**JDK 25 + Maven 3.9.16 + IntelliJ IDEA Community + Swing + Socket + Access/JDBC**。

```text
vcampus-client  →  vcampus-common  ←  vcampus-server
   Swing 界面       Action 与 DTO       业务、权限、DAO
```

> 组员日常只需要阅读本页和自己负责的模块 Epic。其他文档用于架构查阅、过程留痕和最终提交。

## 1. 第一次下载并运行

先安装 JDK 25、Maven、Git 和 IntelliJ IDEA Community。在自己选择的父目录打开 PowerShell：

```powershell
git clone https://github.com/qlaxyy/JAVA-Campus-software.git
cd JAVA-Campus-software
mvn clean verify
```

`git clone` 只执行一次。看到 `BUILD SUCCESS` 后，打开两个终端。

终端 1：

```powershell
java -cp "vcampus-common\target\classes;vcampus-server\target\classes" edu.seu.vcampus.server.ServerMain
```

看到 `Virtual Campus server started on port 8888.` 后保持终端运行。

终端 2：

```powershell
java -cp "vcampus-common\target\classes;vcampus-client\target\classes" edu.seu.vcampus.client.ClientMain
```

开发期测试账号：

| 身份 | 账户名 | 密码 |
|---|---|---|
| 学生 | `student001` | `Student@123` |
| 教师 | `teacher001` | `Teacher@123` |
| 超级管理员 | `admin` | `Admin@123` |
| 医院子系统管理员 | `hospitaladmin` | `HospitalAdmin@123` |

这些是公开的虚构测试账号，不得用于真实系统或复用个人密码。

登录页只输入账号和密码，不让用户自行选择身份。角色和子系统管理范围由服务器根据账号加载：超级管理员可管理账号、分配子系统管理员并进入所有管理模块；子系统管理员只能进入获授权模块。客户端隐藏无权入口，服务器仍会对每个管理请求独立鉴权。

当前可复现：登录门禁、模块大厅、登出、PING/PONG，以及用 `student001` 查询张三的学籍信息。停止服务器时在服务器终端按 `Ctrl + C`。

如果 8888 端口被占用，可临时改用 8890：

```powershell
# 服务端
java -cp "vcampus-common\target\classes;vcampus-server\target\classes" edu.seu.vcampus.server.ServerMain 8890

# 客户端
java -cp "vcampus-common\target\classes;vcampus-client\target\classes" edu.seu.vcampus.client.ClientMain 127.0.0.1 8890
```

## 2. 以后获取最新正式成果

进入已经克隆的项目根目录（能看到根 `pom.xml` 的目录）：

```powershell
git switch main
git status
git pull --ff-only origin main
mvn clean verify
```

如果 `git status` 显示未提交修改，先停止操作并保留终端输出；不要执行 `git reset --hard`，也不要重新克隆覆盖。

## 3. 开发自己的功能

每个具体功能使用一个独立分支。不要在 `main` 上开发或推送业务代码。

```powershell
git switch main
git pull --ff-only origin main
mvn clean verify
git switch -c feat/<module>-<summary>
```

示例：`git switch -c feat/library-book-search`

原则上只修改本人模块的以下位置：

```text
vcampus-common/src/main/java/edu/seu/vcampus/common/<module>/
vcampus-client/src/main/java/edu/seu/vcampus/client/module/<module>/
vcampus-server/src/main/java/edu/seu/vcampus/server/module/<module>/
database/schema/<module>.md
```

根 POM、公共协议、Socket、主界面、模块注册和会话框架属于共享核心。确需修改时，先在 Issue 说明并联系组长。

## 4. 保存、同步并提交 Pull Request

```powershell
git status
git add <本次相关文件>
git commit -m "feat(<module>): 完成某项功能"

git fetch origin
git merge origin/main
mvn clean verify
git push -u origin <自己的分支名>
```

若合并 `origin/main` 时出现冲突，不要删除别人的代码或强制覆盖，把冲突文件和终端输出发给组长。

第一次执行 `git push -u origin <自己的分支名>` 时，Git 会在 GitHub 自动创建同名的远程分支，不需要在网页上再次创建分支。

推送完成后，进入 GitHub 仓库的 **Pull requests** 页面，点击 **New pull request**，选择：

- `base: main`：准备合入的目标分支；
- `compare: 自己的分支名`：包含本次改动的来源分支。

确认方向是 **自己的功能分支 → `main`** 后创建 Pull Request，并填写：

- 写清实现内容、验证方法、测试结果和暂未完成部分；
- 使用 `Part of #Epic编号` 关联模块 Epic；
- 界面功能附截图；
- 邀请至少 1 名非作者评审；
- 评审通过后使用 Squash merge；
- 一个模块全部完成前不要关闭模块 Epic。

提交信息格式为 `<type>(<scope>): <中文简述>`，常用类型：`feat`、`fix`、`docs`、`test`、`refactor`、`build`、`chore`。

## 5. 如何审查别人的 Pull Request

先确保自己的修改已经提交和推送，然后把 `<目标分支>` 换成 PR 页面顶部 `from` 后的分支名：

```powershell
git fetch origin
git switch <目标分支>
git pull --ff-only origin <目标分支>
mvn clean verify
```

再按第 1 节启动服务端和客户端，检查 PR 描述中的正常、异常和界面流程。在 GitHub 的 `Files changed → Review changes` 中选择：

- 没有阻塞问题：`Approve`；
- 必须修改：`Request changes`，并写清复现步骤。

## 6. Issue 怎么用

- 每个负责人只维护自己模块的 Epic 正文：业务想法、范围、页面、Action、DTO、数据表、权限、依赖和验收清单都写在正文。
- 评论区只汇报**已经实现并验证**的阶段成果，格式为“已实现 / 验证结果 / PR”。
- 影响多个模块或共享核心的设计，单独创建 `[公共]` Issue。
- 表字段、主外键和约束写在 `database/schema/<module>.md`。

模块入口：

| 模块 | Epic | 数据字典 | 包名 |
|---|---|---|---|
| 用户管理 | [#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1) | [user.md](database/schema/user.md) | `user` |
| 学生学籍 | [#2](https://github.com/qlaxyy/JAVA-Campus-software/issues/2) | [student.md](database/schema/student.md) | `student` |
| 选课系统 | [#3](https://github.com/qlaxyy/JAVA-Campus-software/issues/3) | [course.md](database/schema/course.md) | `course` |
| 图书馆 | [#6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6) | [library.md](database/schema/library.md) | `library` |
| 商店 | [#11](https://github.com/qlaxyy/JAVA-Campus-software/issues/11) | [shop.md](database/schema/shop.md) | `shop` |
| 医院 | [#4](https://github.com/qlaxyy/JAVA-Campus-software/issues/4) | [hospital.md](database/schema/hospital.md) | `hospital` |

## 7. 全组只需遵守的规则

1. 不直接在 `main` 开发或推送业务代码。
2. 一项具体功能使用一个分支和一个 PR。
3. 客户端不直连数据库；业务和权限必须在服务器端校验。
4. 不提交 `target`、`.idea`、个人数据库、真实密码或密钥。
5. 不使用 `git reset --hard` 处理不理解的问题。
6. `mvn clean verify` 失败时不得合并 PR。
7. 修改公共核心或其他成员模块前先沟通。

## 8. 需要时再看的资料

- [需求基线](docs/01-需求基线.md)
- [总体架构与接口约定](docs/03-总体架构与接口约定.md)
- [四周交付计划](docs/04-四周交付计划.md)
- [验收与最终提交清单](docs/06-验收与提交清单.md)
- [过程记录、风险与追踪](docs/progress/README.md)
- [软件设计说明书草稿](docs/design/SOFTWARE_DESIGN_DRAFT.md)
- [测试计划草稿](docs/testing/TEST_PLAN_DRAFT.md)
- [教师原始材料](docs/课程原始材料/)

完整归档入口见 [docs/README.md](docs/README.md)，组员无需日常全部阅读。
