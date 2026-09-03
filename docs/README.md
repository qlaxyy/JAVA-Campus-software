# 项目文档入口

项目级文档只保留下面 6 份。第一次了解项目时按顺序阅读前 3 份即可：

1. [项目范围与分工](PROJECT_SCOPE.md)：做什么、六个模块、谁负责、阶段目标。
2. [系统设计](design/SYSTEM_DESIGN.md)：系统怎样运行、登录会话、公共接口、权限和数据库边界。
3. [当前状态](PROJECT_STATUS.md)：已经完成什么、下一步和当前风险。
4. [质量与交付](QUALITY_AND_DELIVERY.md)：测试要求、完成标准和最终提交物。
5. [架构决定](ARCHITECTURE_DECISIONS.md)：为什么选择当前技术和协作方案。
6. [课程原始材料](课程原始材料/)：教师提供的原始文件，不修改。

最终提交材料（持续汇总）：

- [虚拟校园系统软件设计说明书](design/SOFTWARE_DESIGN_SPECIFICATION.md)：整个系统的详细设计。当前已完成公共架构和用户登录部分，其他子系统待负责人设计材料汇总。日常接入子系统仍优先阅读上面的“系统设计”。

其他位置：

- 安装、运行、Git、PR 和演示账号：仓库根 [README](../README.md)。
- 表和字段：[`database/schema/`](../database/schema/)，每个模块只维护自己的数据字典。
- 具体功能需求和验收条件：对应 GitHub Epic。
- 已发生的详细过程：Git 提交、PR 和 Issue，不再复制到多份日志。

维护规则：同一内容只写一处。范围变化改 `PROJECT_SCOPE.md`，架构变化改 `SYSTEM_DESIGN.md`，进度变化改 `PROJECT_STATUS.md`，表字段变化改对应模块数据字典。
