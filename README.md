# 综合工作流审批系统

基于 **Spring Boot 3 + Vue 3** 的工作流审批系统基础骨架，当前提供：

- 账号密码登录、JWT 令牌签发与登录校验
- **组织架构部门树管理**：树形表格查看多级部门，任意节点新增子部门、编辑名称/负责人、删除空部门（同级重名与子部门/用户关联校验，仅管理员可维护）
- **人员管理**：人员分页查询与增删改（用户名/密码/手机/邮箱/角色/所属部门），用户名唯一、手机邮箱格式校验；负责人与所属部门自动联动
- **流程设计器（管理员）**：递归树形画布渲染主干链路与条件分支网关，开始/结束之间通过「+」插入审批节点或分支网关，支持并行/互斥模式、分支拖拽排序与增删、分支内独立追加并嵌套子节点、末尾自动合流；右侧抽屉配置审批节点名称与审批人类别，草稿保存与回显
- MySQL 用户表/部门表/流程定义表/流程节点表 + BCrypt 密码哈希（MyBatis-Plus）
- 统一接口返回结构与全局异常处理
- 简洁规范的 Element Plus 登录页（前端表单校验）
- Pinia 认证状态存储、路由守卫、Axios 拦截器
- Docker Compose 一键拉起 MySQL / 后端 / 前端

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.3、Java 17、MyBatis-Plus 3.5、MySQL 8、JJWT 0.12、BCrypt |
| 前端 | Vue 3、Vite 5、Vue Router 4、Pinia 2、Element Plus、Axios |
| 部署 | Docker Compose、Ninx（前端静态资源 + API 反代） |

## 内置账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 系统管理员（不归属部门） |
| `it_admin` / `biz_admin` / `hr_admin` / `design_admin` | `12345678` | 各部门管理员（部门负责人） |
| `it_user1`、`it_user2` 等各部门 user1/user2 | `12345678` | 普通用户 |

内置一级部门：**信息科技部、平台业务部、人力资源部、设计部**，每个部门 1 名管理员 + 2 名普通用户。

> 部门与账号在后端首次启动时幂等创建（重启不会重置数据、不覆盖人工调整），密码均以 BCrypt 哈希存储。生产环境请登录后尽快修改密码，并通过 `JWT_SECRET` 环境变量更换 JWT 密钥。
>
> **升级提示**：应用启动时由 `SchemaMigrationRunner` 幂等地为旧版 `wf_process_node` 补齐分支拓扑所需列（`node_type`、`gateway_mode`、`join_node_id`、`parent_id`、`branch_group`、`condition_expr`），旧的线性审批节点会自动作为主链 `APPROVAL` 节点保留，无需手工处理；也可用 `docker compose down -v` 重置后重新初始化。全新部署无需处理。

## 快速开始（一键启动）

前置要求：本机已安装 Docker 与 Docker Compose 插件。

```bash
# 在项目根目录执行
docker compose up -d --build
```

启动完成后：

| 服务 | 地址 |
| --- | --- |
| 前端页面 | http://localhost |
| 后端接口 | http://localhost:8080 |
| MySQL | localhost:3306（root / root123） |

使用 `admin / admin123` 登录即可。

常用命令：

```bash
docker compose logs -f backend   # 查看后端日志
docker compose down              # 停止并移除容器
docker compose down -v           # 同时删除数据库数据卷（重置数据）
```

## 接口说明

统一返回结构：

```json
{ "code": 0, "message": "操作成功", "data": {} }
```

- `code = 0` 表示成功；非 0 为业务/系统错误码，HTTP 状态码与语义保持一致（401 未认证、400 参数错误、500 系统错误）。

| 方法 | 路径 | 是否需要令牌 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 否 | 账号密码登录，返回 token 与用户信息 |
| GET | `/api/auth/info` | 是 | 校验令牌并获取当前用户信息 |
| POST | `/api/auth/logout` | 是 | 登出（前端清除令牌） |
| GET | `/api/depts/tree` | 是（所有登录用户） | 获取部门树（供人员寻址） |
| POST | `/api/depts` | 是（**ADMIN**） | 新增部门，同级重名返回 2003 |
| PUT | `/api/depts` | 是（**ADMIN**） | 编辑部门（名称/负责人/排序/状态/备注） |
| DELETE | `/api/depts/{id}` | 是（**ADMIN**） | 删除空部门；有子部门(2004)/有用户(2005)时拒绝 |
| GET | `/api/users/page` | 是（**ADMIN**） | 人员分页，支持 keyword/role/deptId 过滤 |
| POST | `/api/users` | 是（**ADMIN**） | 新增人员（用户名唯一，密码 BCrypt 存储） |
| PUT | `/api/users` | 是（**ADMIN**） | 编辑人员（密码留空表示不修改） |
| DELETE | `/api/users/{id}` | 是（**ADMIN**） | 删除人员；负责人(1005)/删除自己(1006)时拒绝 |
| GET | `/api/users/options` | 是（**ADMIN**） | 用户下拉选项，可选 `?deptId=` 过滤 |
| GET | `/api/process-designer/draft` | 是（**ADMIN**） | 回显流程画布草稿；首次访问自动创建仅含开始/结束的默认草稿 |
| PUT | `/api/process-designer/draft` | 是（**ADMIN**） | 全量保存画布草稿（流程名称 + 审批节点链）；非管理员调用返回 403 |

### 部门负责人归属联动规则

在部门管理中为部门设置负责人时，后端按以下规则处理：

1. 用户**尚未分配部门** → 自动将其「所属部门」设置为该部门；
2. 用户**已归属其他部门** → 拒绝设置，返回 `2007 该用户已归属其他部门，不能设置为本部门负责人`；
3. 用户本就属于该部门 → 正常设置。

反向约束：在人员管理中**调整部门**或**删除**一个现任部门负责人时，会被拒绝（`1005`），需先在部门管理中为该部门更换负责人，避免部门悬挂失效负责人。

### 流程设计器草稿

- 入口：管理员登录后从左侧菜单「流程设计器」进入（路由 `/designer`，前端路由守卫 + 后端 `@RequireRole("ADMIN")` 切面双重限制，非管理员无法打开页面或调用接口，越权调用返回 `403`）。
- 画布以**递归树形组件**自上而下渲染：**开始 → 主链（审批节点 / 条件分支网关可任意嵌套）→ 结束**。任意「+」处可插入审批人节点或条件分支网关；审批节点点击后在右侧抽屉配置名称与审批人类别（`USER` 指定人员 / `DEPT_LEADER` 部门负责人 / `ROLE_ADMIN` 管理员），悬停可删除。
- **条件分支网关**：分叉头可在「并行（同时执行）/ 互斥（满足其一）」间切换；互斥模式每条分支需填写条件名称、可附条件表达式；分支可拖拽排序、增删（至少保留 2 条），分支内独立追加子节点并支持继续嵌套网关，多条分支在末尾自动汇聚到「合流」节点。
- 开始/结束为内置节点不落库；拓扑持久化于 `wf_process_definition`（当前固定一条 `process_key=approval_main` 的草稿，首次进入自动创建）与 `wf_process_node`。
- `wf_process_node` 为**树形拓扑的扁平邻接表**：`node_type` 区分审批/分叉/分支锚点/汇聚；分叉行用 `join_node_id` 配对汇聚行，用 `(parent_id, branch_group)` 表达父子嵌套（分支锚点与分支内子节点共享组标识）与兄弟排序（`sort`）。
- 保存为**事务内全量重建**（先清空该流程全部节点行再递归落库），服务端递归校验节点类型、网关分支数（≥2）、互斥条件名、嵌套深度（≤10）、节点总量（≤200）与审批人存在性。

保存请求示例（主链含一个互斥网关，分支内可继续嵌套 `branches`）：

```bash
curl -X PUT http://localhost:8080/api/process-designer/draft \
  -H "Authorization: Bearer <admin-token>" \
  -H 'Content-Type: application/json' \
  -d '{
    "processName": "主干审批流程",
    "remark": null,
    "nodes": [
      { "type": "APPROVAL", "nodeName": "部门负责人审批", "approverType": "DEPT_LEADER" },
      {
        "type": "GATEWAY", "gatewayMode": "EXCLUSIVE",
        "branches": [
          { "conditionName": "金额>1000", "conditionExpr": "amount > 1000",
            "nodes": [ { "type": "APPROVAL", "nodeName": "总经理审批", "approverType": "ROLE_ADMIN" } ] },
          { "conditionName": "默认", "conditionExpr": null, "nodes": [] }
        ]
      }
    ]
  }'
```

登录请求示例：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

受保护接口需携带请求头：

```
Authorization: Bearer <token>
```

## 本地开发

### 后端

```bash
cd backend
# 仅启动 MySQL
docker compose up -d mysql
mvn spring-boot:run
```

后端运行在 http://localhost:8080，可通过环境变量覆盖数据库与 JWT 配置（见 `backend/src/main/resources/application.yml`）。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 http://localhost:5173，开发服务器已将 `/api` 代理到 `http://localhost:8080`。

## 目录结构

```
.
├── docker-compose.yml          # 三服务一键编排
├── backend/                    # Spring Boot 3 后端
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/workflow/approval/
│       │   ├── common/         # 统一返回 Result / ResultCode
│       │   ├── config/         # MP、密码器、Web、数据初始化、流程表结构迁移
│       │   ├── controller/     # 认证、部门、用户、流程设计器接口
│       │   ├── dto/            # 请求/响应对象（含递归树状流程草稿 VO）
│       │   ├── entity/         # 用户、部门、流程定义、流程节点实体
│       │   ├── exception/      # 业务异常 + 全局异常处理
│       │   ├── mapper/         # MyBatis-Plus Mapper
│       │   ├── security/       # JWT、拦截器、@RequireRole 角色切面
│       │   └── service/        # 登录、部门树/CRUD、流程拓扑解析与持久化
│       ├── resources/
│       │   ├── application.yml
│       │   └── db/init.sql     # 建库建表脚本
│       └── test/               # H2 内存库集成测试（流程拓扑树↔扁平行往返）
└── frontend/                   # Vue 3 前端
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── api/                # Axios 封装与接口
        ├── router/             # 路由与登录守卫
        ├── stores/             # Pinia 认证状态
        ├── layout/             # 登录后主框架
        └── views/              # 登录页、工作台、部门/人员管理、流程设计器
```

## 后续业务扩展建议

- 在 `sys_user` 基础上扩展角色权限表（RBAC）与接口级权限注解
- JWT 刷新令牌机制 / Redis 黑名单登出
- 工作流定义、流程实例、审批任务等业务表与审批流引擎接入
