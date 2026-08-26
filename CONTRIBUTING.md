# 贡献指南

提交代码前必须先阅读 [GitHub 协作规范](docs/05-GitHub协作规范.md) 和 [总体架构与接口约定](docs/03-总体架构与接口约定.md)。

最短工作流：

1. 在本人负责的模块 Epic 评论区写清本轮功能，不修改 Issue 正文；留言后即可开工。
2. 从最新 `main` 创建分支：`feat/<module>-<issue>-<summary>`。
3. 完成代码、测试、JavaDoc 和必要文档。
4. 本地执行构建与测试，并进行一次基本手工验证。
5. 创建 Pull Request，使用 `Part of #Epic编号` 关联模块 Epic，并邀请至少 1 名组员评审。
6. 处理所有阻塞意见后使用 Squash merge；合并后删除分支。

提交信息格式：`<type>(<scope>): <中文简述>`。

- `feat`：新功能
- `fix`：修复
- `docs`：文档
- `test`：测试
- `refactor`：不改变行为的重构
- `build`：构建、依赖或打包
- `chore`：其他维护

示例：`feat(course): 完成学生退选和容量回滚`

只有一个独立 Issue 的全部验收条件都完成时才使用 `Closes #编号`。模块 Epic 在整个模块完成前保持 Open。涉及共享核心或其他模块的修改，编码前先在 Issue 说明并协调。
