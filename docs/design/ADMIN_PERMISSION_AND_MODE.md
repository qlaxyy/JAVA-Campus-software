# 管理员权限与模块模式 UML / 流程图

本页固定身份、模块模式和数据库操作链路。业务模块的具体页面、Action、DTO 与表字段仍由对应 Epic 和数据字典维护。

## 1. 软件总体分层与六模块

```mermaid
flowchart LR
    subgraph Client[vcampus-client · Swing 客户端]
        Login[统一登录页]
        Hall[模块大厅]
        UserUI[用户管理]
        StudentUI[学籍]
        CourseUI[选课]
        LibraryUI[图书馆]
        ShopUI[商店]
        HospitalUI[医院]
        Login --> Hall
        Hall --> UserUI
        Hall --> StudentUI
        Hall --> CourseUI
        Hall --> LibraryUI
        Hall --> ShopUI
        Hall --> HospitalUI
    end

    subgraph Common[vcampus-common · 公共契约]
        Request[Request / Response]
        DTO[Action / DTO]
        Session[SessionInfo<br/>Role + AdminScope]
    end

    subgraph Server[vcampus-server · 服务器]
        Router[ActionRouter]
        Auth[会话与权限校验]
        Modules[六个 ServerModule]
        Service[业务 Service]
        DAO[模块 DAO]
        Router --> Auth --> Modules --> Service --> DAO
    end

    DB[(Access 数据库)]
    Login --> Request
    Hall --> Request
    Request --> Router
    DTO -. 双方共享 .- Modules
    Session -. 服务器签发 .-> Hall
    DAO --> DB
```

## 2. 身份与权限类图

```mermaid
classDiagram
    class UserAccount {
        +String userId
        +String username
        +Role role
        +AccountStatus status
    }
    class Role {
        <<enumeration>>
        STUDENT
        TEACHER
        MODULE_ADMIN
        SUPER_ADMIN
    }
    class AdminScope {
        <<enumeration>>
        STUDENT
        COURSE
        LIBRARY
        SHOP
        HOSPITAL
    }
    class SessionInfo {
        +String token
        +Role role
        +Set~AdminScope~ adminScopes
        +canManageUsers() boolean
        +canAdminister(moduleId) boolean
    }
    class ModuleAccessPolicy {
        +modeFor(session, moduleId) Optional~ModuleViewMode~
    }
    class ModuleViewMode {
        <<enumeration>>
        USER
        MANAGEMENT
    }
    class SessionLookup {
        +findSession(token) Optional~SessionInfo~
        +canAdminister(token, moduleId) boolean
    }
    class ServerModule {
        +registerHandlers(router, context)
    }
    class Service
    class DAO

    UserAccount --> Role
    UserAccount "1" --> "0..*" AdminScope : MODULE_ADMIN 授权
    UserAccount --> SessionInfo : 登录后签发
    SessionInfo --> Role
    SessionInfo --> AdminScope
    ModuleAccessPolicy --> SessionInfo
    ModuleAccessPolicy --> ModuleViewMode
    SessionLookup --> SessionInfo
    ServerModule --> SessionLookup : 服务器鉴权
    ServerModule --> Service
    Service --> DAO
```

## 3. 登录后如何决定模块模式

```mermaid
flowchart TD
    A[输入账号和密码] --> B[服务器验证凭据并加载 Role / AdminScope]
    B --> C{角色}
    C -->|STUDENT / TEACHER| D[显示业务模块 · 用户模式]
    C -->|MODULE_ADMIN| E[只显示 AdminScope 对应模块 · 管理模式]
    C -->|SUPER_ADMIN| F[显示用户管理和全部业务模块 · 管理模式]
    D --> G[发送普通业务 Action]
    E --> H[发送模块管理 Action]
    F --> H
    G --> I[服务器校验已登录及业务规则]
    H --> J[服务器再次校验角色与模块范围]
    J -->|无权限| K[返回 AUTH_FORBIDDEN]
    J -->|有权限| L[Service 校验并调用 DAO]
    L --> M[(Access 数据库)]
```

## 4. 管理员修改业务数据时序图

```mermaid
sequenceDiagram
    actor Admin as 子系统管理员
    participant UI as Swing 管理页面
    participant Client as ClientContext
    participant Server as 对应 ServerModule
    participant Auth as SessionLookup
    participant Service as 业务 Service
    participant DAO as 模块 DAO
    participant DB as Access

    Admin->>UI: 编辑并提交数据
    UI->>Client: send(MODULE.ADMIN_ACTION, DTO)
    Client->>Server: Request(token, action, data)
    Server->>Auth: canAdminister(token, moduleId)
    alt 未登录或无该模块范围
        Auth-->>Server: false
        Server-->>UI: AUTH_REQUIRED / AUTH_FORBIDDEN
    else 有管理权限
        Auth-->>Server: true
        Server->>Service: 校验并执行业务操作
        Service->>DAO: 新增 / 修改 / 状态变更
        DAO->>DB: 参数化 SQL / 事务
        DB-->>DAO: 结果
        DAO-->>Service: 持久化结果
        Service-->>UI: 成功响应与最新数据
    end
```

## 5. 各模块实现约定

- 普通模式与管理模式可以复用查询页面，但管理按钮只在 `MANAGEMENT` 下出现。
- 每个管理 Action 都在对应 Epic 写明所需 `AdminScope`、DTO、校验、错误码和测试。
- 客户端校验用于及时提示；服务器必须重复校验输入、身份和业务约束。
- 数据库只允许服务器 DAO 访问；客户端和 common 层不得出现 JDBC、SQL 或 Access 路径。
- “删除”优先实现为状态变更；库存、容量、号源等并发数据必须由 Service/事务原子更新。
