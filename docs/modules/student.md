# 学生学籍管理模块设计说明

## 一、第一阶段目标
完成学籍数据字典（第一版）设计，打通首条最小端到端查询链路（`STUDENT.GET_PROFILE`）。

## 二、端到端用例：按学号查询学籍详情
- **操作 Action**：`STUDENT.GET_PROFILE`
- **请求实体**：`StudentProfileRequest`（携带 `studentId`）
- **响应实体**：`StudentProfileResponse`（封装包含 14 个标准字段的 `StudentProfileDto`）
- **正常流程**：
    1. 用户在“用户管理”完成登录（如演示账号 `student001`）。
    2. 切换至“学生学籍”页面，输入 `student001` 并点击“查询学籍”。
    3. 客户端发起后台异步请求，服务端校验当前 Session 是否有效。
    4. 服务端调用 `StudentService` 查询学籍内存数据并返回响应。
    5. 客户端表单完整呈现学号、姓名、院系、专业、班级、状态等详细档案。
- **异常流程**：
    1. 未登录拦截：服务端拦截并返回 `AUTH_REQUIRED` 错误，界面提示先登录。
    2. 学号未找到：服务端返回 `notFound` 提示，界面清空表单并提示无记录。
