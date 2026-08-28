# 需求—设计—实现—测试追踪表

本表用于确保最终软件设计说明书描述的内容与实际 Issue、代码和测试一致。链接在相关工作产生后补充。

## 六个业务模块

| 模块 | 需求基线 | 设计说明书章节 | 模块 Epic | 主要 PR | 测试证据 | 界面/演示证据 | 状态 |
|---|---|---|---|---|---|---|---|
| 用户管理 | `01-需求基线.md` 4.1 | 用户管理模块设计、ADR-0009 | [#1](https://github.com/qlaxyy/JAVA-Campus-software/issues/1) | `feat/admin-role-model` 待 PR | `AuthorizationModelTest`、`ModuleAccessPolicyTest`、认证集成测试 | 待补充 | 管理员权限基础已实现待合并，账号管理链路待开发 |
| 学生学籍 | `01-需求基线.md` 4.2 | 学生学籍模块设计 | [#2](https://github.com/qlaxyy/JAVA-Campus-software/issues/2) | PR #19 | 学籍查询集成测试 | 待补充 | 第一条查询链路已合并 |
| 选课系统 | `01-需求基线.md` 4.3 | 选课模块设计 | [#3](https://github.com/qlaxyy/JAVA-Campus-software/issues/3) | 待补充 | 待补充 | 待补充 | 已分配，待设计 |
| 图书馆 | `01-需求基线.md` 4.4 | 图书馆模块设计 | [#6](https://github.com/qlaxyy/JAVA-Campus-software/issues/6) | 待补充 | 待补充 | 待补充 | 已分配，待设计 |
| 商店 | `01-需求基线.md` 4.5 | 商店模块设计 | [#11](https://github.com/qlaxyy/JAVA-Campus-software/issues/11) | 待补充 | 待补充 | 待补充 | 已建立，待实现 |
| 医院 | `01-需求基线.md` 4.6 | 医院模块设计、`database/schema/hospital.md` | [#4](https://github.com/qlaxyy/JAVA-Campus-software/issues/4) | PR #23 | `HospitalServiceTest`、`HospitalSearchIntegrationTest` | 待补充 | 患者端号源查询链路已合并 |

## 公共设计与交付

| 内容 | 当前依据 | 需要形成的证据 | 状态 |
|---|---|---|---|
| 开发环境 | 根目录 `README.md`、ADR-0001、ADR-0006 | 环境检查结果、实际 IDE/版本、构建日志 | 进行中 |
| 系统结构 | `03-总体架构与接口约定.md`、根目录 `README.md` | 三模块 POM、六模块扩展位置、架构 ADR | PR #7、#8 已合并；六模块进入第一轮并行开发 |
| 公共 Message | `03-总体架构与接口约定.md` 3 | Request/Response、ActionRouter、PING、登录/会话、单元/集成测试 | 路由、连接和基础会话契约已实现，业务契约由各模块设计 |
| 网络与多线程 | `03-总体架构与接口约定.md` 6 | Socket、固定线程池、超时、PING/PONG 测试 | 最小链路已验证，完整策略待 ADR |
| 数据库 | `03-总体架构与接口约定.md` 5 | E-R 图、数据字典、建库脚本、连接验证 | 未开始 |
| GUI 规范 | `03-总体架构与接口约定.md` 7 | 主导航原型、六模块入口、界面截图 | 统一导航和占位页已实现，业务页面待开发 |
| 测试 | `06-验收与提交清单.md` | 测试计划、用例、结果、缺陷修复链接 | 公共层 9 个自动测试通过，业务测试待开发 |
| 发布与答辩 | `06-验收与提交清单.md` | Release、部署记录、演示脚本、彩排记录 | 未开始 |

## 架构决策追踪

| 决策 | 状态 | 实现证据 | 验证缺口 |
|---|---|---|---|
| ADR-0001 JDK 25/Maven 3.9.16 | 通过，部分验证 | `pom.xml`、环境检查脚本、`mvn clean verify` 成功 | 其余成员环境与构建结果 |
| ADR-0002 Issue 自主认领 | 被 ADR-0005 取代 | 保留历史决定 | 不再实施 |
| ADR-0005 组长统一分配模块 | 通过，已验证 | 分工规范、Epic 创建清单、Issue 模板、#1/#2/#3/#4/#6/#11 | 六个业务模块 Epic 已全部建立 |
| ADR-0003 C/S 三模块架构 | 通过，最小实现已验证 | PR #7、三模块 POM、Socket PING/PONG、2 个集成测试 | 业务扩展和完整并发验证 |
| ADR-0007 六模块独立扩展点 | 通过，已实现待合并 | ActionRouter、六个 ClientModule/ServerModule、5 个路由/目录测试 | 六模块首个 handler 和页面 |
| ADR-0008 开发期内存认证 | 通过（临时），已合并 | USER 三个 action、ClientContext/ServerContext、认证集成测试 | Access DAO、慢哈希、会话过期与审计 |
| ADR-0009 管理员权限模型 | 通过，基础实现待合并 | 四角色、AdminScope、SessionInfo、导航策略和 6 项相关测试 | 超级管理员账号管理页面、管理 Action、DAO 与业务模块管理端鉴权 |
| ADR-0004 Access/JDBC | 提议 | 决策问题和实验门禁 | 教师确认、驱动选型、最小原型和部署验证 |

## 最终材料草稿

| 材料 | 持续草稿 | 当前覆盖 | 主要缺口 |
|---|---|---|---|
| 软件设计说明书 | `docs/design/SOFTWARE_DESIGN_DRAFT.md` | 已有需求、环境、总体结构和各章节待办 | 完整模块分配、接口/表、图、实际代码和截图 |
| 测试计划/分析 | `docs/testing/TEST_PLAN_DRAFT.md` | 已有策略、环境、编号、严重度和证据格式 | 实际用例、执行结果、缺陷和修复 |
| 最终提交资料 | `docs/delivery/DOCUMENT_REGISTER.md` | 已登记课程要求的主要交付物 | 负责人、实际日期、文件和验证结果 |

## 更新规则

- 模块 Issue 建立/认领后，补充 Epic 链接和状态。
- PR 合并后，补充 PR 链接、对应设计变化和测试证据。
- 界面可用后保存截图或演示材料路径。
- 每周会逐行检查“设计已写但代码不存在”或“代码已合并但设计/测试未更新”的不一致。
