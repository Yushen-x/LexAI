# LexAI

LexAI 是一个面向法律咨询、案件分析与合同审查场景的智能法律工作台原型。当前仓库已经完成前后端基础工程、核心页面骨架和部分接口联调，后端推理能力目前由 Mock 引擎提供，便于后续继续接入真实 AI 能力。

## 当前状态

- 已完成前后端分离的基础工程搭建
- 已具备法律咨询、案件分析、合同审查 3 条核心接口链路
- 已提供工作台、咨询、案件分析、合同审查、合同起草、合同台账、待办任务等页面骨架
- 当前后端返回以 Mock 数据为主，适合联调、演示和后续功能迭代

## 技术栈

- Frontend: Vue 3 + Vite + TypeScript + Vue Router + Axios
- Backend: Spring Boot 3 + Java 21

## 启动方式

### 1. 启动后端

要求：本地已安装 `Java 21` 和 `Maven`

```bash
cd backend
mvn spring-boot:run
```

启动后默认地址：`http://localhost:8080/api`

**要调用得理 / 腾讯真实接口（避免只走兜底）**：请按 **[docs/后端本地启动与API环境配置.md](docs/后端本地启动与API环境配置.md)** 从零步到九步操作；密钥只在本地 `backend/.env` 填写，由你自己或负责人提供，勿提交仓库。

### 2. 启动前端

要求：本地已安装 `Node.js`

```bash
cd frontend
npm install
npm run dev
```

启动后默认地址：`http://localhost:5173`

前端默认通过 Vite 代理访问后端 `/api`。

## 当前页面与接口

### 页面

- `/dashboard`：工作台
- `/consultation`：法律咨询
- `/case-analysis`：案件分析
- `/contract-review`：合同审查
- `/contract-draft`：合同智能起草
- `/contract-list`：合同台账
- `/workflow-pending`：待办任务

### 接口

- `GET /api/system/health`
- `GET /api/system/overview`
- `POST /api/legal/consultation`
- `POST /api/legal/case-analysis`
- `POST /api/legal/contract-review`

## 相关文档

- `docs/后端本地启动与API环境配置.md`：后端环境变量与分步启动（傻瓜模式）
- `docs/ARCHITECTURE.md`：工程架构说明
- `docs/ProjectCharter.md`：英文项目章程
- `docs/ProjectCharter.zh.md`：中文项目章程
- `TASK.md`：当前任务完成情况与后续待办总结
