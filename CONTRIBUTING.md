# 贡献指南

提交代码前必须先阅读 [GitHub 协作规范](docs/05-GitHub协作规范.md) 和 [总体架构与接口约定](docs/03-总体架构与接口约定.md)。

最短工作流：

1. 开工前把本人模块 Epic 正文中的范围、验收条件和接口概要补充到足以指导开发；无需发表评论或等待确认。
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

## 如何帮助审查 Pull Request

审查前先提交并推送自己的改动，确保 `git status` 没有未保存内容。然后在项目根目录执行（把两处 `<目标分支>` 换成 PR 页面顶部 `from` 后面的分支名）：

```powershell
git fetch origin
git switch <目标分支>
git pull --ff-only origin <目标分支>

mvn clean verify
```

出现 `BUILD SUCCESS` 后，分别打开两个 PowerShell，在项目根目录依次运行：

服务器端：

```powershell
java -cp "vcampus-common\target\classes;vcampus-server\target\classes" edu.seu.vcampus.server.ServerMain
```

客户端：

```powershell
java -cp "vcampus-common\target\classes;vcampus-client\target\classes" edu.seu.vcampus.client.ClientMain
```

按照 PR 的验收条件检查正常流程、异常流程和界面。确认无误后，在 GitHub PR 页面进入 `Files changed`，点击 `Review changes`，选择 `Approve` 并提交评审；发现问题则选择 `Request changes` 并写明复现步骤。审查结束后切回自己的功能分支。

## Issue 与模块 Epic

只有一个独立 Issue 的全部验收条件都完成时才使用 `Closes #编号`。模块 Epic 在整个模块完成前保持 Open。涉及共享核心或其他模块的修改，编码前先在 Issue 说明并协调。

模块 Epic 正文是本模块唯一的功能设计入口，由模块负责人持续维护；评论区只汇报已经实现并验证的阶段结果。表与字段写 `database/schema/<module>.md`，测试证据与截图写在 PR。
