# 贡献指南

提交代码前必须先阅读 [GitHub 协作规范](docs/05-GitHub协作规范.md) 和 [总体架构与接口约定](docs/03-总体架构与接口约定.md)。

最短工作流：

1. 领取或创建一个 Issue，写清验收条件。
2. 从最新 `main` 创建分支：`feat/<module>-<issue>-<summary>`。
3. 完成代码、测试、JavaDoc 和必要文档。
4. 本地执行构建与测试，并进行一次基本手工验证。
5. 创建 Pull Request，关联 Issue，邀请至少 1 名组员评审。
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
