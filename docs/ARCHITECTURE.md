# LexAI 工程架构说明

## 一、项目形态

LexAI 采用前后端分离架构：

- 前端：`Vue 3 + Vite + TypeScript`
- 后端：`Spring Boot + Java`
- 交互方式：`REST API`
- 部署形态：前端静态站点独立部署，后端 API 服务独立部署

## 二、目录结构

```text
LexAI
├─ backend
│  ├─ pom.xml
│  └─ src
│     ├─ main
│     │  ├─ java/com/lexai/backend
│     │  │  ├─ application
│     │  │  ├─ common
│     │  │  ├─ config
│     │  │  ├─ domain
│     │  │  ├─ infrastructure
│     │  │  └─ interfaces
│     │  └─ resources
│     └─ test
├─ docs
│  └─ ARCHITECTURE.md
├─ frontend
│  ├─ src
│  │  ├─ modules
│  │  ├─ router
│  │  └─ shared
│  ├─ index.html
│  ├─ package.json
│  ├─ tsconfig.json
│  └─ vite.config.ts
├─ README.md
└─ TASKS.md
```

## 三、前端架构

前端采用模块化组织方式，避免后续页面和业务增多后互相耦合。

- `src/modules`：按业务场景划分页面与组件，例如法律咨询、案件分析、合同审查
- `src/router`：统一维护路由配置
- `src/shared/api`：统一维护接口请求层
- `src/shared/layout`：统一维护页面框架布局
- `src/shared/types`：统一管理前端类型定义
- `src/shared/styles`：全局样式与设计变量

推荐后续继续扩展：

- 新增 `stores/` 管理跨页面状态
- 新增 `composables/` 复用业务逻辑
- 新增 `components/` 沉淀通用业务组件

## 四、后端架构

后端采用接近分层/六边形架构的组织方式，兼顾比赛展示与后续扩展：

- `interfaces`：对外接口层，负责 REST API 暴露
- `application`：应用服务层，负责流程编排与用例实现
- `domain`：领域模型层，沉淀法律场景中的核心对象与枚举
- `infrastructure`：基础设施层，负责外部 AI 平台、知识库、存储系统接入
- `common`：通用响应、异常处理等横切能力
- `config`：跨域、配置类、系统初始化配置

这种组织方式的优势：

- 控制器不直接耦合底层实现
- 后续接入腾讯系 AI 平台时，只需要替换 `infrastructure` 层实现
- 后续从 Mock 服务升级为真实检索、向量库、知识库服务时改动范围可控

## 五、建议的能力演进路径

### 第一阶段

- 先完成前端页面骨架
- 先完成后端 REST API 骨架
- 使用 Mock 结果打通主流程演示

### 第二阶段

- 接入腾讯系 AI 工具平台
- 增加 Prompt 编排、法律知识检索与结果约束
- 建立结构化输出规范

### 第三阶段

- 引入真实知识库与评测集
- 引入风险识别、引用追溯、人工复核机制
- 增加日志审计、用户管理与历史记录

## 六、接口约定

当前骨架中预留了以下接口：

- `GET /api/system/health`
- `GET /api/system/overview`
- `POST /api/legal/consultation`
- `POST /api/legal/case-analysis`
- `POST /api/legal/contract-review`

## 七、后续建议

如果你们准备往比赛答辩方向继续抬升，可以在当前结构上继续增加：

- `knowledge` 模块：法律知识库管理
- `rag` 模块：检索增强流程
- `workflow` 模块：多智能体任务编排
- `audit` 模块：日志审计与风险控制
- `auth` 模块：多角色权限体系

