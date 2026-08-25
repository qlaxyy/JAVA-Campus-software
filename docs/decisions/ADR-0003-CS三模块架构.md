# ADR-0003：采用 Java C/S 与 Common-Client-Server 三模块架构

- 状态：通过
- 日期：2026-08-24
- 决策范围：系统总体结构、网络和代码组织

## 背景

课程要求应用系统采用客户端/服务器端结构，使用 Java，并要求多客户端、Socket、公共可序列化对象和分层设计。六个业务模块需要共享身份、消息、错误码和 DTO，同时保持客户端界面、服务器业务和数据访问职责分离。

## 决定

- 使用 Java C/S 架构，客户端采用 Swing。
- 客户端和服务器端通过 Socket 交换统一 Message；公共传输对象实现 `Serializable` 并声明序列化版本。
- Maven 采用三个顶层模块：
  - `vcampus-common`：Message、DTO/VO、枚举、错误码。
  - `vcampus-client`：Swing View、客户端 Service、网络连接。
  - `vcampus-server`：请求分发、业务 Service、DAO、线程池和数据库连接。
- 业务包按 user、student、course、library、shop、hospital 划分，并在模块内部按 view/service/dao/model 分层。
- 跨模块调用使用公共服务/契约，不直接依赖其他模块界面或随意更新其他模块数据表。
- 服务器使用受控线程池；容量、库存、号源等共享资源必须原子更新。

## 备选方案

### 单体桌面程序直接访问数据库

实现较快，但不满足课程 C/S、多客户端和 Socket 要求。未采用。

### Web/B/S 架构

生态成熟，但偏离课程指定 Java C/S 和桌面客户端方向。未采用。

### 六个独立工程分别实现模块

成员隔离明显，但会产生重复登录、协议和数据，最终难以集成。未采用。

## 影响与迁移

- 必须优先建立 Maven 骨架和最小 Socket 请求，再开始六模块并行开发。
- `vcampus-common` 的破坏性变更需要跨模块评审和同步迁移。
- GUI 不直接写 SQL，DAO 不包含界面/网络逻辑。
- 最小代码骨架已通过 PR #7 合并到 `main`，并通过 Maven 构建和 PING/PONG 集成测试；业务扩展继续按 ADR-0007 的固定六模块入口实现。

## 验证证据

- 课程原始材料中的 C/S、Socket、多线程和三项目建议。
- `docs/03-总体架构与接口约定.md`
- `docs/design/SOFTWARE_DESIGN_DRAFT.md` 第 3、10、11、12 章。
