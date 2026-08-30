# 当前架构决定

本文只记录仍然有效、会影响多人开发的决定。历史讨论可在 Git、Pull Request 和 Issue 中查阅。

## 1. 开发环境

- JDK 25、Maven 3.9.16、IntelliJ IDEA Community。
- 使用 Maven 多模块工程，提交前执行 `mvn clean verify`。
- 客户端和服务器必须使用同一版本的 `vcampus-common`。

## 2. 系统形态

- 系统由 `vcampus-common`、`vcampus-client`、`vcampus-server` 三个 Maven 模块组成。
- 一个客户端进程通过 Socket 连接一个服务器进程；六个子系统是同一程序中的业务模块，不是六套独立服务器。
- 客户端不直接访问数据库。认证、授权、业务校验和数据写入均由服务器完成。

## 3. 模块接入方式

- 客户端模块实现公共的 `ClientModule` 扩展点，用于提供模块名称和页面。
- 服务器模块实现公共的 `ServerModule` 扩展点，并通过 `ActionRouter` 接收本模块 Action。
- 业务请求统一使用 `Request`、`Response` 和可序列化 DTO；不得自行再造一套网络协议。

## 4. 登录与会话

- 当前认证和会话暂存于服务器内存，服务器重启后用户需要重新登录。
- 登录成功后服务器生成 token，并保存 `token → SessionInfo`；客户端只把会话保存在当前程序内存。
- 后续请求携带 token，服务器通过 `ServerContext.sessions()` 查询登录信息。
- 账号迁移到 Access 后，密码保存慢哈希值，不保存明文；会话是否持久化在实现到期策略时再决定。

## 5. 全局授权

- 全局 `Role` 只区分 `USER` 与 `SUPER_ADMIN`。
- 子系统管理权使用 `AdminScope`：`STUDENT`、`COURSE`、`LIBRARY`、`SHOP`、`HOSPITAL`。
- 模块内的业务资格由模块根据 `userId` 和自己的数据查询。例如医院查询医生名单，选课查询任课信息。
- 进入某个页面只是界面控制；每个需要保护的 Action 仍必须在服务器端重新校验。

## 6. 数据库

- 最终验收数据库为 Access `vCampus.accdb`，服务器通过 JDBC/DAO 访问。
- 每个模块维护自己的表和 `database/schema/<module>.md`，跨模块以稳定 ID 关联。
- 不提交个人 `.accdb`、真实身份信息或生产密码。
- Access 驱动、连接方式和迁移顺序须先在一台全新环境中验证，再替换当前内存 Repository。

## 7. 协作责任

- 模块负责人实现本模块的页面、Action/DTO、服务器处理、Service、DAO、数据字典和测试。
- 总控维护公共框架、会话、模块注册、集成规则和发布，不代替模块负责人定义业务细节。
- 负责人以对应 Epic 的 Assignee 为准；公共契约和跨模块数据库变化必须经过评审。
