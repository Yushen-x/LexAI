# 完成内容说明（本地环境 + 成员C交付）

更新时间：2026-03-29

本文档用于汇总当前仓库中已完成的主要工作内容，重点覆盖：本地环境跑通 AI 样例接口、以及 `docs/TASK.md` 中成员C范围的前端接线与字段对齐。

---

## 1. 本地环境与样例接口验证

- 按 `docs/后端本地启动与API环境配置.md` 配置本地环境（加载 `backend/.env` 环境变量、使用 Java 21 启动后端）。
- 按 `docs/AI能力接口测试样例汇总.md` 对本地接口进行逐条调用验证，确认咨询 / 案件分析 / 合同审查 / 合同起草链路返回为**非兜底**的真实内容（在 AI 端点/密钥缺失时会出现同质化兜底输出，此问题通过正确配置环境变量规避）。

---

## 2. 前端统一提示（Toast + Axios 拦截器）

### 2.1 全局 Toast

- 新增轻量 DOM Toast：支持 `success / error / warning / info`，自动消失、可点击关闭。
- Toast 样式使用现有的 CSS 变量与 token（不引入第三方 UI 组件库）。

### 2.2 Axios 统一错误处理

- 在 Axios 响应拦截器中统一处理：
  - HTTP 网络/状态码异常 → 统一 toast 错误提示 + reject
  - 业务返回体包含 `code` 且 `code !== 'SUCCESS'` → 统一 toast 提示 + reject

目标效果：页面内尽量不再使用 `alert()`；错误提示口径一致，减少每个页面重复写错误处理。

---

## 3. 合同起草页（成员C核心接线）

页面：合同起草（`frontend/src/modules/contract/views/ContractDraftView.vue`）

### 3.1 合同生成

- “生成合同”按钮接入后端合同起草接口：`POST /api/legal/contract-draft`
- 生成成功后将 `generatedContent` 填充到左侧正文编辑区。

### 3.2 存草稿（写入合同台账）

- “存草稿”按钮调用合同台账创建接口：`POST /api/contracts`
- 默认以草稿状态保存（`status: 'DRAFT'`），用于在合同列表/台账中沉淀记录。

### 3.3 提交审查（预填并跳转）

- “提交审查”会将当前合同内容写入 `sessionStorage`（`pendingContractContent` / `pendingContractName`），并跳转至合同审查页。
- 合同审查页支持从 `sessionStorage` 自动预填，减少手动复制粘贴。

### 3.4 AI 侧边栏两种模式

- ASK 问答咨询：调用咨询接口 `POST /api/legal/consultation`，基于当前合同元信息与正文节选组织 facts，返回建议/风险提示/法律依据。
- AGENT 修改指令：将“修改需求 + 当前全文”合并进 requirements，再次调用 `POST /api/legal/contract-draft` 生成新正文并替换编辑区内容。

### 3.5 复制 / 下载

- 支持“复制全文”“下载 TXT”，并统一使用 Toast 反馈（成功/失败）。

---

## 4. 三核心功能字段对齐与展示优化（成员C范围）

### 4.1 法律咨询（Consultation）

- 将 `legalBasis: string[]` 做了结构化展示（从常见文本格式中解析出：法规名 / 条款号 / 内容摘要），提升可读性。
- 复制提示改为 Toast。

### 4.2 案件分析（Case Analysis）

- 覆盖率改为动态计算：
  - 无结果时：根据用户已填证据做保守估算
  - 有结果时：覆盖率 = 已提供证据数 /（已提供证据数 + AI 识别缺口数）
- 复制提示改为 Toast。

### 4.3 合同审查（Contract Review）

- 风险等级 badge 做了样式映射（HIGH / MEDIUM / LOW）。
- 每条风险的“修改建议”支持一键复制并用 Toast 提示。
- 支持从合同起草页跳转时的 sessionStorage 预填。

---

## 5. 其他 UI 微调

- 顶部栏移除“Demo Environment”徽标（避免固定文案影响演示一致性）。

---

## 6. 变更文件清单（按功能归类）

### Toast / 基础设施
- `frontend/src/shared/ui/toast.ts`
- `frontend/src/shared/styles/main.css`
- `frontend/src/shared/api/http.ts`

### 合同台账 API
- `frontend/src/shared/api/contracts.ts`

### 业务页面
- `frontend/src/modules/contract/views/ContractDraftView.vue`
- `frontend/src/modules/consultation/views/ConsultationView.vue`
- `frontend/src/modules/case-analysis/views/CaseAnalysisView.vue`
- `frontend/src/modules/contract-review/views/ContractReviewView.vue`
- `frontend/src/shared/layout/AppShell.vue`

---

## 7. 验证方式（建议）

- 前端：`npm run build` 通过（用于确认类型检查与打包无误）。
- 后端：按 `docs/AI能力接口测试样例汇总.md` 调用接口验证输出与字段结构。
