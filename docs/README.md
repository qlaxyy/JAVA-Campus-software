# 归档与交付资料

组员的运行、Git、Issue 和 Pull Request 教程已经统一到仓库首页 [README](../README.md)，不在 `docs` 中维护重复版本。

本目录只保留需要长期留痕或用于最终报告的资料：

- [需求基线](01-需求基线.md)
- [六人分工](02-六人分工.md)
- [总体架构与接口约定](03-总体架构与接口约定.md)
- [四周交付计划](04-四周交付计划.md)
- [验收与最终提交清单](06-验收与提交清单.md)
- [现行系统设计总览（开发唯一准则）](design/SYSTEM_DESIGN.md)
- [过程记录、风险与需求追踪](progress/README.md)
- [架构决策记录](decisions/README.md)
- [软件设计说明书草稿](design/SOFTWARE_DESIGN_DRAFT.md)
- [管理员权限与模块模式 UML/流程图](design/ADMIN_PERMISSION_AND_MODE.md)
- [测试计划草稿](testing/TEST_PLAN_DRAFT.md)
- [最终提交资料登记表](delivery/DOCUMENT_REGISTER.md)
- [教师原始材料](课程原始材料/)

维护原则：

- 当前整体架构、身份、权限和模块边界只在 `design/SYSTEM_DESIGN.md` 维护；
- 日常教程只更新根目录 `README.md`；
- 模块内部需求、页面、Action 和验收项更新对应 GitHub Epic 正文；
- 数据字段只更新 `database/schema/<module>.md`；
- 已发生进展只追加到 `progress/PROJECT_LOG.md`；
- 重大架构决定记录到 `decisions/`；
- 教师原始材料不得改写或删除。
