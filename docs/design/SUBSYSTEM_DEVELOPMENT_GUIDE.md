# 子系统开发接口手册

> 面向学籍、选课、图书馆、商店和医院模块开发者。本文回答：一个功能需要建哪些类、调用哪些公共接口，以及服务器怎样识别和鉴权当前用户。

## 1. “接口”具体指什么

| 类型 | 含义 | 示例 |
|---|---|---|
| Java 扩展接口 | 模块接入公共框架必须实现的方法 | `ClientModule`、`ServerModule`、`SessionLookup` |
| 网络业务接口 | 一次客户端—服务器业务调用的完整约定 | Action + 请求 DTO + 响应 DTO + 权限 + 错误码 |

例如“查询选课批次”接口：

```text
Action：COURSE.LIST_BATCHES
请求：data = null
权限：token 必须对应有效 Session
成功数据：List<SelectionBatchInfo>
失败：AUTH_REQUIRED / COMMON_INVALID_REQUEST
```

每个新业务功能都必须在 Epic 或 PR 中写清这五项。

## 2. 公共调用链

```text
Swing 页面
  → ClientContext.send(action, requestDto)
  → Request(action, token, data)
  → ActionRouter
  → 本模块 ServerModule handler
  → SessionLookup.findSession(token)
  → 本模块 Service
  → 本模块 Repository / DAO
  → Response(responseDto)
```

子系统不自行登录、不保存第二套用户、不直接创建 Socket，也不直接访问数据库。

## 3. 六个公共接口

| 接口 | 位置 | 用法 |
|---|---|---|
| [`ClientModule`](../../vcampus-client/src/main/java/edu/seu/vcampus/client/module/ClientModule.java) | client | 实现模块名称和根 Swing 页面 |
| [`ClientContext`](../../vcampus-client/src/main/java/edu/seu/vcampus/client/application/ClientContext.java) | client | `send(action, dto)`；自动携带当前 token |
| [`Request`](../../vcampus-common/src/main/java/edu/seu/vcampus/common/protocol/Request.java) / [`Response`](../../vcampus-common/src/main/java/edu/seu/vcampus/common/protocol/Response.java) | common | 统一网络消息 |
| [`ServerModule`](../../vcampus-server/src/main/java/edu/seu/vcampus/server/module/ServerModule.java) | server | 注册本模块 Action handler |
| [`ServerContext`](../../vcampus-server/src/main/java/edu/seu/vcampus/server/module/ServerContext.java) | server | 取得公共 Session 查询服务 |
| [`SessionLookup`](../../vcampus-server/src/main/java/edu/seu/vcampus/server/security/SessionLookup.java) | server | 查询当前用户及检查管理权限 |

关键签名：

```java
public interface ClientModule {
    String id();
    String displayName();
    JComponent createView(ClientContext context);
}

Response response = context.send(action, requestDto);

public interface ServerModule {
    String id();
    void registerHandlers(ActionRouter router, ServerContext context);
}

Optional<SessionInfo> session =
        context.sessions().findSession(request.getToken());
```

## 4. 一个功能需要哪些文件

以医院“查询我的预约”为例：

```text
vcampus-common/.../hospital/
  HospitalActions.java             Action 常量
  AppointmentListResponse.java     响应 DTO
  AppointmentView.java             单条展示 DTO

vcampus-client/.../hospital/
  MyAppointmentsPanel.java         Swing 页面

vcampus-server/.../hospital/
  HospitalServerModule.java        注册 Action、校验 Session/DTO
  HospitalService.java             业务规则
  HospitalRepository.java          数据访问接口
  AccessHospitalRepository.java    JDBC 实现

vcampus-server/src/test/.../hospital/
  HospitalServiceTest.java         业务测试

vcampus-client/src/test/.../
  HospitalIntegrationTest.java     Socket 集成测试
```

模块负责人原则上只修改自己模块的 common、client、server、测试和 `database/schema/<module>.md`。公共接口不够用时先开公共 Issue。

## 5. common：Action 和 DTO

```java
public static final String LIST_MY_APPOINTMENTS =
        ActionNames.of(ModuleNames.HOSPITAL, "LIST_MY_APPOINTMENTS");
```

规则：

- Action 固定为 `<MODULE>.<VERB>`，使用 `ActionNames.of` 和 `ModuleNames`。
- DTO 必须实现 `Serializable` 并声明 `serialVersionUID`。
- DTO 只传业务数据，不放 Swing、Socket、DAO、连接或密码。
- “查询我的数据”通常不传 `userId`；服务器从 Session 读取。
- 集合字段使用 `List.copyOf` 等不可变副本。

## 6. client：页面发送请求

```java
Response response = context.send(
        HospitalActions.LIST_MY_APPOINTMENTS,
        null);

if (!response.isSuccess()) {
    showError(response.getMessage());
    return;
}

AppointmentListResponse result =
        (AppointmentListResponse) response.getData();
```

- 只使用 `ClientContext.send`，它会自动附带 token。
- 不直接创建 Socket，不自己保存 token。
- 网络调用放进 `SwingWorker`，不要阻塞 Swing EDT。
- `currentSession()` 可用于界面提示，但不能代替服务器鉴权。

## 7. server：识别当前用户

```java
private Response listMyAppointments(Request request, ServerContext context) {
    Optional<SessionInfo> session =
            context.sessions().findSession(request.getToken());
    if (session.isEmpty()) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in first.");
    }

    if (request.getData() != null) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Request data must be empty.");
    }

    String userId = session.get().getUserId();
    AppointmentListResponse result = service.listForUser(userId);
    return Response.success(request, "Appointments loaded.", result);
}
```

注册 handler：

```java
router.register(
        HospitalActions.LIST_MY_APPOINTMENTS,
        request -> listMyAppointments(request, context));
```

标准顺序：

1. 用 token 查询 Session；
2. 无 Session 返回 `AUTH_REQUIRED`；
3. 校验 DTO 类型和字段；
4. 从 Session 读取可信 `userId` 和权限；
5. 调用 Service；
6. 返回明确 DTO 或安全错误码。

## 8. 不同权限怎样检查

| 功能 | 服务器判断 |
|---|---|
| 普通登录功能 | `findSession(token)` 存在 |
| 查询“我的数据” | 使用 `session.getUserId()`，不相信客户端 userId |
| 医生、任课教师等专业功能 | 用 `userId` 查询本模块专业绑定 |
| 子系统管理功能 | `sessions().canAdminister(token, ModuleNames.X)` |
| 账号和授权管理 | `sessions().canManageUsers(token)` |

管理 Action 先区分未登录，再检查范围：

```java
Optional<SessionInfo> session =
        context.sessions().findSession(request.getToken());
if (session.isEmpty()) {
    return Response.failure(
            request.getRequestId(), ErrorCodes.AUTH_REQUIRED, "Please log in first.");
}
if (!context.sessions().canAdminister(
        request.getToken(), ModuleNames.HOSPITAL)) {
    return Response.failure(
            request.getRequestId(), ErrorCodes.AUTH_FORBIDDEN,
            "Hospital administrator permission is required.");
}
```

不能用 `Role.TEACHER` 推断医生，也不能用管理权限代替医生绑定。

## 9. 多模式模块

参考医院增加一个由服务器计算的模式权限接口：

```text
HOSPITAL.GET_MODE_ACCESS
请求：token，data = null
响应：HospitalModeAccessView
  patientAllowed
  doctorAllowed
  adminAllowed
```

客户端据此启用模式按钮。进入模式后的每个 Action 仍要再次鉴权。

## 10. 分层边界

| 层 | 负责 | 不负责 |
|---|---|---|
| ServerModule handler | Action、Session、DTO 类型、Response | SQL、复杂业务规则 |
| Service | 业务规则、状态流转、并发约束 | Swing、Socket |
| Repository / DAO | 数据查询和写入 | 权限界面、Response |

数据链路必须是：

```text
Swing → ClientContext → Action/DTO → ServerModule → Service → Repository/DAO → Access
```

## 11. 合并前最低测试要求

- 未登录返回 `AUTH_REQUIRED`；
- 错误 DTO 返回 `COMMON_INVALID_REQUEST`；
- 越权返回 `AUTH_FORBIDDEN`；
- Service 正常、边界和异常规则通过；
- 至少一条客户端—Socket—服务器集成测试；
- `mvn clean verify` 全量成功；
- UI 功能附实际运行截图。

整体身份和权限设计见 [虚拟校园系统现行设计总览](SYSTEM_DESIGN.md)。
