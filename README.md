# LexAI

LexAI 是一个面向法律咨询、案件分析与合同审查场景的智能法律工作台。仓库已完成前后端分离、合同全生命周期、待办闭环、AI 网关（腾讯混元 + 得理 + 本地 RAG）及工作台数据可视化；支持 Mock 演示与真实 API 两种运行模式。

## 当前状态

- 前后端分离工程已就绪（Vue 3 + Spring Boot 3 + Java 21）
- 法律咨询、案件分析、合同审查、合同起草四条 AI 链路已联调
- 合同台账 CRUD、统计缓存、待办任务闭环已实现
- **AI 会话历史**：咨询 / 案件分析自动持久化，侧栏可恢复、可搜索
- **工作台**：合同统计图表（ECharts）、最近 AI 活动、动态系统状态
- AI 模式：`lexai.ai.mode=tencent`（真实 API，需配置密钥）或 `mock`（无密钥演示）

## 技术栈

- Frontend: Vue 3 + Vite + TypeScript + Vue Router + Axios + ECharts
- Backend: Spring Boot 3 + Java 21 + JPA + MySQL（本地可无 MySQL，自动 H2）

## 启动方式（推荐）

项目根目录提供一键脚本：

```bash
chmod +x start.sh stop.sh
./start.sh              # 后台启动（自动检测 Java 21、MySQL/H2、Mock 模式）
./stop.sh                 # 停止全部服务
./start.sh --foreground   # 前台运行，Ctrl+C 停止
```

- 前端：http://localhost:5173  
- 后端：http://localhost:8081/api  
- 日志：`backend/backend.out.log`、`frontend/frontend.out.log`

### 手动启动

**后端**（需 Java 21）：

```bash
cd backend
cp .env.example .env   # 按需填写密钥
./mvnw spring-boot:run
```

**前端**：

```bash
cd frontend
npm install
npm run dev
```

**真实 AI 接口**：在 `backend/.env` 配置 `TENCENT_LLM_API_KEY`、`DELI_APP_ID` 等，详见  
[docs/AI接口密钥配置与调用操作指南（成员通用）.md](docs/AI接口密钥配置与调用操作指南（成员通用）.md)。

## 当前页面

| 路径 | 说明 |
|------|------|
| `/dashboard` | 工作台（统计图表、最近 AI 活动、系统状态） |
| `/consultation` | 法律咨询（含历史侧栏） |
| `/case-analysis` | 案件分析（含历史侧栏） |
| `/contract-review` | 合同审查 |
| `/contract-draft` | 合同智能起草 |
| `/contract-list` | 合同台账 |
| `/workflow-pending` | 待办任务 |

## 接口清单

### 系统

- `GET /api/system/health` — 扩展健康信息（AI 模式、数据库、知识库、会话数）
- `GET /api/system/overview` — 平台概览

### 法律 AI

- `POST /api/legal/consultation`
- `POST /api/legal/case-analysis`
- `POST /api/legal/contract-review`
- `POST /api/legal/contract-draft`
- `GET /api/legal/sessions?type=&keyword=&page=&size=` — 会话历史（支持搜索）
- `GET /api/legal/sessions/recent?limit=5` — 最近 AI 活动
- `GET /api/legal/sessions/{id}` — 会话详情

### 合同与待办

- `GET/POST /api/contracts` — 列表与新建
- `GET/PUT/DELETE /api/contracts/{id}` — 详情、更新、软删除
- `GET /api/contracts/statistics` — 统计概览（总数 / 状态 / 类型 / 月度趋势）
- `GET /api/tasks`、`PUT /api/tasks/{id}/status`

> 所有响应均带 `X-Request-Id` 关联 ID，便于日志追踪。

## 性能优化与可观测性

- **合同统计缓存**：Spring Cache + Caffeine（60s TTL），写操作主动失效
- **知识库检索**：TF-IDF + 倒排预筛 + LRU 查询缓存
- **业务流水号**：`SequenceGenerator` 统一合同号 / 任务号 / 会话号
- **请求追踪**：`RequestCorrelationFilter` + MDC `[traceId]`
- **前端**：路由懒加载、vendor 分包、GET 在途去重

详见 [docs/性能优化与测试.md](docs/性能优化与测试.md)。

## 测试

```bash
cd backend && ./mvnw test
cd frontend && npm run build
```

JaCoCo 报告：`backend/target/site/jacoco/index.html`

## 相关文档

- **[docs/近期功能改动说明.md](docs/近期功能改动说明.md)** — 本次会话历史、Dashboard 可视化等改动详情
- [docs/AI接口密钥配置与调用操作指南（成员通用）.md](docs/AI接口密钥配置与调用操作指南（成员通用）.md)
- [docs/性能优化与测试.md](docs/性能优化与测试.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/TASK.md](docs/TASK.md)
- [docs/ProjectCharter.zh.md](docs/ProjectCharter.zh.md)
