# ADR-0001：统一使用 JDK 25 与 Maven 3.9.16

- 状态：通过
- 日期：2026-08-24
- 决策范围：全体开发、构建、测试和部署环境

## 背景

课程材料同时包含“不低于 JDK 1.8”和旧版兼容示例。团队已经下载 JDK 25，教师随后明确 Java 版本不限。如果成员继续使用不同大版本，可能出现语法、字节码、插件和运行环境不一致。

## 决定

- 团队统一使用 JDK 25；补丁版本可以不同，大版本必须为 25。
- Maven 统一为 3.9.16。
- Maven 编译 `release/source/target` 固定为 25，并通过 Enforcer 拒绝其他 Java/Maven 大版本组合。
- 文本编码统一 UTF-8，Maven JVM 时区使用 Asia/Shanghai。
- 本 ADR 最初允许 IntelliJ IDEA 或 Eclipse；该 IDE 选择条款已由 ADR-0006 取代。Project SDK、语言级别和 Maven Runner 必须指向 JDK 25 的约束继续有效。

## 备选方案

### 使用 JDK 8

优点是与旧课程示例一致；缺点是全员需要重复安装，且教师已明确不限制版本。未采用。

### 允许成员自由使用任意 Java 版本

减少个人配置约束，但会增加构建、API 和运行时差异。未采用。

### 使用 JDK 25 编译但目标 Java 8

可以保留部分旧环境兼容，但不能使用 Java 25 语言/API，且最终仍需在 Java 8 环境验证。当前没有此验收要求，未采用。

## 影响与迁移

- 最终演示电脑必须安装 JDK 25，或发布时提供对应运行环境说明。
- 每位成员首次开发前运行环境检查脚本和 `mvn validate`。
- 若教师再次指定低版本环境，需要新 ADR 取代本决定并完成代码/API 兼容审计。

## 验证证据

- `.java-version`
- `pom.xml`
- `.mvn/jvm.config`
- `scripts/check-environment.ps1`
- 根目录 `README.md`
