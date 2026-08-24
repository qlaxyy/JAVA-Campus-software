# GitHub 协作规范

## 1. 仓库设置建议（组长执行）

在 GitHub 为 `main` 设置分支保护：

- 合并前必须通过 Pull Request。
- 至少 1 个批准；有新提交后旧批准失效。
- 所有评审对话必须解决。
- 必须通过构建/测试检查后才可合并。
- 禁止 force push 和删除 `main`。

5 位组员均应接受邀请并成为 Collaborator。首次开发前在仓库设置中确认没有 `Pending Invite`；只有组长维护 Release 和仓库设置，组长同样通过 PR 提交业务代码。

## 2. Issue 规则

一个 Issue 应能在 0.5-2 天内完成，至少包含：

- 用户故事/目标。
- 范围内与范围外。
- 验收条件（正常、异常、权限）。
- 涉及的页面、action、DTO、表。
- 依赖和风险。
- 负责人、评审人、里程碑、标签。

六个模块 Epic Issue 由组长按小组已经确认的分工直接设置 Assignee。六人（包括组长）每人主责一个业务模块且不重复。模块负责人和公共职责均以 Issue Assignee 为准，不在公开仓库文档维护固定人员名单；后续公共任务可另行分配或自主认领。

推荐标签：`module:user`、`module:student`、`module:course`、`module:library`、`module:shop`、`module:hospital`、`area:common`、`area:database`、`area:docs`、`priority:P0-P3`、`status:blocked`。

## 3. 分支命名

- 功能：`feat/<module>-<issue>-<summary>`
- 修复：`fix/<module>-<issue>-<summary>`
- 文档：`docs/<issue>-<summary>`
- 发布：`release/<version>`

示例：`feat/library-42-return-book`。分支从最新 `main` 创建，只处理一个 Issue，禁止长期个人总分支。

## 4. Pull Request 规则

- 尽量小于 400 行有效改动；超过时说明为何不能拆分。
- 必须关联 Issue，例如 `Closes #42`。
- 必须填写测试证据和界面截图（涉及 UI 时）。
- 公共契约、表结构或跨模块行为变化须明确列出影响方。
- 作者先自查，再请求评审；作者不能批准自己的 PR。
- 评审关注正确性、权限、并发、异常处理、契约兼容和可测试性，不只看格式。
- 使用 Squash merge，PR 标题作为最终提交信息；合并后删除分支。

## 5. 冲突与同步

- 开始工作和提交 PR 前同步最新 `main`。
- 冲突由分支作者解决，涉及他人模块时与对方结对，不猜测删除他人代码。
- 不提交 IDE 配置、构建产物、个人数据库副本、密码、密钥或绝对路径。
- 大规模重命名/格式化单独 PR，避免与功能改动混在一起。

## 6. 公共文件的变更门槛

以下内容不是某个成员的“私人文件”：根 `pom.xml`、`vcampus-common`、数据库公共结构、主导航、连接/线程框架、配置、错误码和发布目录。修改时必须在 PR 描述中 @相关负责人；如会破坏其他模块，先完成迁移方案再合并。

## 7. 推荐的 Git 命令流程

```bash
git switch main
git pull --ff-only
git switch -c feat/course-23-enroll
# 开发、测试
git add <本次相关文件>
git commit -m "feat(course): 完成选课容量校验"
git push -u origin feat/course-23-enroll
```

随后在 GitHub 创建 PR。禁止使用 `git push --force` 覆盖共享分支历史。
