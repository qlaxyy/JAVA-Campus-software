# 最终演示数据库

服务器唯一运行数据库为 `database/vCampus.accdb`。它只由服务器端 JDBC/DAO 访问；其他组员只运行客户端，通过 Radmin VPN 连接服务器，不能复制、打开或直接修改该文件。

## 身份字段

- `username`：兼容既有 Java 接口的字段名，实际保存 8 位一卡通号，也是登录账号。
- `displayName`：当前姓名的唯一来源，只存纯姓名，不含“演示”“同学”“教师”等身份文字。
- `userId`：跨模块永久关联键；用户看不到也不能修改。教师、学生、医生、借阅、选课、订单等业务表都保存它。
- `Role`：只区分 `USER` 与 `SUPER_ADMIN`；子系统管理员由唯一 `AdminScope` 决定，教师和医生资格分别由教师表与医院医生档案决定。

最终种子库包含 39 个启用账号：1 个超级管理员、5 个子系统管理员、15 名学生、8 名教师、10 名医生。账号为 `20260000`—`20260038`，初始密码统一为 `123456`，只用于课程演示。服务器生成新账号时查询当年最大流水号，因此种子库的下一个号码是 `20260039`，不按表中人数推算。

| 一卡通号 | 姓名/范围 | 资格或权限 |
|---|---|---|
| `20260000` | 超级管理员 | 超级管理员 |
| `20260001` | 学籍管理员 | 学籍管理员 |
| `20260002` | 选课管理员 | 选课管理员 |
| `20260003` | 图书馆管理员 | 图书馆管理员 |
| `20260004` | 商店管理员 | 商店管理员 |
| `20260005` | 医院管理员 | 医院管理员 |
| `20260006`—`20260020` | 吴尚扬、施天琦、杨凯涵、吴昊哲、葛丰玮、廖俊杰、周一—周九 | 学生，对应 `U-STUDENT-001`—`U-STUDENT-015` |
| `20260021`—`20260028` | 王建国、李静、陈立、赵敏、周航、孙晓、郑文杰、许清 | 教师，对应 `U-TEACHER-001`—`U-TEACHER-008` |
| `20260029`—`20260038` | 陈安、刘宁、周岚、何远、王清、赵健、孙悦、林川、钱宁、吴凡 | 医生，对应 `U-DOCTOR-001`—`U-DOCTOR-010` |

教师院系和职称、医生科室和职称的完整种子清单以 [`FinalDemoRoster`](../vcampus-server/src/main/java/edu/seu/vcampus/server/demo/FinalDemoRoster.java) 为唯一代码来源；数据库重建器会逐条校验上述一卡通号、姓名、`userId` 与资格表引用。

## 安全重建

先停止服务器并完成构建，然后在仓库根目录执行：

```powershell
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar --rebuild-demo-database
```

命令会先把现有数据库复制到 `database/backups/vCampus-日期时间.accdb`，在临时文件中初始化全部模块，校验账号唯一性、39 个身份和跨表引用，最后才原子替换正式数据库。任何一步失败都会保留原数据库。正常启动命令不变，也不会清空数据：

```powershell
java -jar vcampus-server\target\vcampus-server-0.1.0-SNAPSHOT.jar
```

生成的 `.accdb`、备份和锁文件均不提交 Git；仓库只提交初始化代码、数据字典和重建说明。

## 数据归属

| 模块 | 主要表 | 规则 |
|---|---|---|
| 用户 | `tblUser`、`tblUserAdminScope`、`tblTeacherProfile`、`tblUserAuditLog` | 拥有账号、当前姓名、管理权和公共教师资格 |
| 学籍 | `tblStudentProfile`、`tblStudentStatusChange` | 15 份学生档案，以 `userId` 关联账号，姓名实时查用户目录 |
| 选课 | `tblCourse*`、`tblOfferingTeacher`、`tblEnrollment`、`tblGrade` | 保留 18 门课程目录和 8 个有教师的演示教学班；任课、选课与成绩持久化 |
| 医院 | `tblHospital*` | 科室保留；10 名医生、排班和医疗业务以 `userId` 关联 |
| 图书馆 | `tblBook*`、`tblBorrowRecord`、`tblReservation` | 5 种书、20 册馆藏以及借还预约演示数据 |
| 商店 | `tblShop*`、`tblCampusCard` | 商品、照片、库存、余额、购物车和订单全部在服务器 Access 中 |

模块只能写自己拥有的表；跨模块只能通过 `ServerContext.users()`、`ServerContext.teachers()` 等公共只读目录取当前姓名、一卡通号和资格，不能自行访问 `tblUser`。审计、订单、证明和医疗历史可以保留办理时姓名快照，但快照不能覆盖当前姓名。

## 单服务器、多客户端

所有电脑加入同一个 Radmin VPN 网络。服务器电脑启动服务端后，将控制台标记为 `Radmin VPN - recommended` 的 IPv4 和端口 8888 发给组员。客户端使用：

```powershell
java -jar vcampus-client\target\vcampus-client-0.1.0-SNAPSHOT.jar 26.x.x.x 8888
```

至少用两台真实电脑同时验证登录、共享购物车或订单以及一个并发业务。当前 Socket 协议没有 TLS，只能用于课程组可信成员的虚拟局域网，不能直接开放到公网。
