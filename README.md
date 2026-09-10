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
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar
```

首次启动会自动创建 `database/vCampus.accdb`、用户表、图书馆四张业务表、医生申请/档案表、
9 个开发期账号和图书馆演示馆藏。看到 `Virtual Campus server started on port 8888.` 后保持终端运行。
账号、书目、实体单册和借阅记录会保存在 Access 中；超级管理员的账号维护操作也会写入 Access 审计表。
token 会话仍保存在服务器内存，连续 30 分钟无操作或单次登录达到 8 小时后自动失效，服务器重启后也需要重新登录。

图书馆 V2 是首次正式接入 Access，不迁移早期开发数据库中的旧版图书表。若已有旧结构的 `.accdb`，
请先备份，并通过服务器第二个参数改用新的数据库路径，或确认无需旧数据后重新创建开发库。

终端 2：

```powershell
java -jar vcampus-client\target\vcampus-client-0.1.0-SNAPSHOT.jar
```

开发期测试账号：

| 用这个账号测试什么 | 服务器中额外登记的资料或权限 | 一卡通号 | 密码 |
|---|---|---|---|
| 全系统管理 | 可以管理账号和所有业务模块 | `20260000` | `123456` |
| 普通学生功能 | 无 | `20260001` | `123456` |
| 医生工作台 | 医院有效医生档案绑定了这个账号 | `20260002` | `123456` |
| 学籍管理 | 可以维护学籍数据 | `20260003` | `123456` |
| 选课管理 | 可以维护选课数据 | `20260004` | `123456` |
| 图书馆管理 | 可以维护图书馆数据 | `20260005` | `123456` |
| 商店管理 | 可以维护商店数据 | `20260006` | `123456` |
| 医院管理 | 可以维护医院数据 | `20260007` | `123456` |
| 教师功能 | 普通账号；已绑定公共教师档案 | `20260008` | `123456` |

这些是公开的虚构测试账号，统一简单密码仅用于联调，不得用于真实系统或复用个人密码。一卡通号固定为“4 位年份 + 4 位流水号”，流水号范围为 `0001`—`9999`，每个人只有一个账号。所有已登录账号都可以进入患者模式；`20260002` 能进入医生模式，是因为医院有效医生档案绑定了它的 `userId`；`20260008` 的公共教师档案绑定 `U-COURSE-TEACHER-001`，选课和学籍服务器可据此确认教师资格，具体任课关系仍由选课模块保存。

登录页只输入账号和密码，不让用户自行选择身份。全局 `Role` 只区分普通账号 `USER` 与超级管理员 `SUPER_ADMIN`；普通账号可以不具有子系统管理权，也可以具有一个 `AdminScope`，但不能同时管理多个子系统。超级管理员由 `Role` 直接获得全系统管理能力。客户端隐藏无权操作只用于改善体验，服务器仍会对每个请求独立鉴权。登录后可在右上角修改自己的密码，修改成功后旧会话全部失效，需要使用新密码重新登录。

子系统管理员忘记密码时，由超级管理员在“用户管理”中重置。如果唯一的超级管理员忘记密码，先停止服务器，再在保存数据库的服务器电脑上执行下面的本地恢复命令。命令会隐藏密码输入、只允许重置 `SUPER_ADMIN`，并写入账号审计记录；不要把新密码直接写在命令参数里。

```powershell
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar --reset-super-admin-password
```

默认重置 `20260000`，默认数据库为 `database\vCampus.accdb`。指定其他超级管理员和数据库时使用：

```powershell
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar --reset-super-admin-password 20260000 database\vCampus.accdb
```

系统没有全局“用户/管理模式”。每个子系统在模块内部提供自己的模式入口，例如医院管理员可以进入患者和管理员模式；只有绑定有效医生档案的账号才能进入医生模式。医生申请必须明确选择“关联已有校园账号”或“新建外来医生账号”：前者校验并锁定已有一卡通号，后者不填写一卡通号，由用户模块在超级管理员批准后按当年已有最大流水号加一生成。客户端只能发送 Action，实际校验和数据库读写必须经过服务器 Service 与 DAO，禁止 Swing 客户端直接连接 Access。完整规则见 [现行系统设计总览](docs/design/SYSTEM_DESIGN.md)。

当前可复现：登录门禁、模块大厅、登出、PING/PONG、超级管理员账号维护与 CSV 批量导入、学生学籍查询与异动、选课与退课、图书检索借还、商店购物车及校园卡支付、医院患者号源查询、医院三模式入口，以及“医院管理员分类申请—超级管理员审核—账号关联/生成—申请记录交付账号—医生档案激活”链路。停止服务器时在服务器终端按 `Ctrl + C`。

超级管理员单独新增账号时，一卡通号由服务器按照“当前年份 + 当年最大流水号加一”自动生成；界面中的一卡通号框只用于预览，不能手工修改，最终号码以服务器创建结果为准。批量导入文件使用 UTF-8 CSV，第一行固定为 `campusCardNumber,displayName`。每次最多
1000 个普通账号，初始密码统一为 `123456`；任何一行错误都会取消整批写入。

如果 8888 端口被占用，可临时改用 8890：

```powershell
# 服务端
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar 8890

# 客户端
java -jar vcampus-client\target\vcampus-client-0.1.0-SNAPSHOT.jar 127.0.0.1 8890
```

服务器还可接收第二个参数作为数据库路径，例如：

```powershell
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar 8888 database\test.accdb
```

### 使用 Radmin VPN 在多台电脑上联调

服务器只在一台电脑上运行，Access 数据库也只放在服务器电脑上；其他电脑只运行客户端，不复制或打开 `vCampus.accdb`。Java 程序仍使用 TCP Socket，Radmin VPN 只负责让不同电脑组成可互通的虚拟局域网。

1. 所有电脑安装 Radmin VPN。由服务器电脑创建一个网络，组员使用相同的网络名称和密码加入；Radmin VPN 中各电脑应显示“在线”。
2. 记录服务器电脑在 Radmin VPN 中显示的虚拟 IPv4 地址。服务器启动后也会列出可用地址，并将 Radmin 网卡标为推荐。
3. 在服务器电脑运行下方原有服务器命令，并在 Windows 防火墙提示时允许 Java 通信。无需修改服务器启动命令，也不要把 8888 端口映射到公网。
4. 在客户端电脑切到与服务器一致的 Git 版本并完成构建，然后把下方示例 IP 换成服务器的 Radmin VPN 地址：

```powershell
java -jar vcampus-client\target\vcampus-client-0.1.0-SNAPSHOT.jar 26.12.34.56 8888
```

连接失败时，先在客户端电脑检查端口：

```powershell
Test-NetConnection 26.12.34.56 -Port 8888
```

看到 `TcpTestSucceeded : True` 才说明 Radmin VPN 链路和防火墙已经放行。若为 `False`，依次检查 Radmin 中双方是否在线、所填地址是否为服务器的 Radmin IPv4、服务器是否仍在运行，以及服务器防火墙是否允许 Java/8888 端口。不要填写客户端自己的 IP，也不要把数据库文件发给客户端。

验收时至少同时打开两个客户端，分别完成登录、会话查询和一项业务操作；其中一个客户端退出后，另一个客户端应仍能正常操作。Radmin VPN 仅用于课程组可信成员之间的联调，本项目当前 Socket 协议没有 TLS，不应开放给不可信网络。

并发验收不能只验证“多个窗口能打开”。还要让多个客户端同时竞争同一资源，并确认选课最后一个名额、商品最后一件库存、同一本图书和医院最后一个号源都不会被重复分配。用户模块已有并发会话隔离和自动生成一卡通号不重复测试；数据库唯一索引作为第二道约束。

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

- [系统设计与接口说明](docs/design/SYSTEM_DESIGN.md)
- [项目范围与分工](docs/PROJECT_SCOPE.md)
- [项目当前状态](docs/PROJECT_STATUS.md)
- [质量与交付清单](docs/QUALITY_AND_DELIVERY.md)
- [当前架构决定](docs/ARCHITECTURE_DECISIONS.md)
- [教师原始材料](docs/课程原始材料/)

完整阅读顺序见 [文档入口](docs/README.md)。
