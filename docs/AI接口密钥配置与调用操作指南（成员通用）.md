# AI 接口密钥配置与调用操作指南（成员通用）

适用范围：本项目 **所有成员** 在本地启用 **得理（DELI）** 与 **腾讯混元（Tencent LLM）** 的真实调用（避免后端走兜底/Mock）。

> 重要原则：**任何真实密钥都不要写进代码、不要提交到 Git、不要发到群里。**
> 仓库里只允许提交“模板文件”如 `.env.example` / `backend/.env.example`（值必须为空）。

---

## 1. 你需要哪些变量

后端从系统环境变量读取（见 `backend/src/main/resources/application.yml`）：

- `DELI_APP_ID`：得理开放平台 AppId
- `DELI_SECRET`：得理开放平台 Secret
- `TENCENT_LLM_API_KEY`：腾讯混元 API Key
- `TENCENT_LLM_ENDPOINT`：腾讯混元 Chat Completions Endpoint（示例：`https://api.hunyuan.cloud.tencent.com/v1/chat/completions`）
- `TENCENT_LLM_MODEL`：模型名（示例：`hunyuan-lite`）
- `LEXAI_KNOWLEDGE_PATH`：（可选）本地知识库目录

> 注意：当前 Spring Boot 配置是 `${ENV_VAR:}` 这种形式，**只会读“环境变量”**；项目里并未配置 `spring.config.import` 来自动加载 `.env` 文件。
> 因此 `.env` / `backend/.env` 只是“存放变量的本地文件”，你仍需要用终端/IDE 把它加载成环境变量后再启动后端。

---

## 2. 获取密钥的协作方式（推荐）

- **每位成员自己向负责人/管理员索要** `DELI_*` 与 `TENCENT_*` 的真实值
- 本仓库只提交模板：
  - `backend/.env.example`（给后端启动用的模板）

---

## 3. 本地配置步骤（macOS / Linux，zsh/bash）

### 3.1 创建你的本地文件（不要提交）

在项目根目录执行：

```bash
cp backend/.env.example backend/.env
```

然后编辑 `backend/.env`，把下面这些值填上（不要加引号）：

```dotenv
DELI_APP_ID=xxxx
DELI_SECRET=xxxx
TENCENT_LLM_API_KEY=xxxx
TENCENT_LLM_ENDPOINT=https://api.hunyuan.cloud.tencent.com/v1/chat/completions
TENCENT_LLM_MODEL=hunyuan-lite
LEXAI_KNOWLEDGE_PATH=/absolute/path/to/knowledge_base
```

### 3.2 在启动后端前加载为环境变量

在项目根目录执行：

```bash
set -a
source backend/.env
set +a
```

说明：
- `set -a` 会让 `source` 进来的变量自动 `export` 成环境变量
- 只对当前终端会话生效；开新终端需要重新执行一次

### 3.3 启动后端

```bash
cd backend
mvn spring-boot:run
```

---

## 4. Windows 配置（PowerShell）

### 4.1 准备 `backend\.env`

复制模板并填写真实值（同上）。

### 4.2 将 `.env` 加载到当前 PowerShell 会话

在项目根目录运行：

```powershell
Get-Content .\backend\.env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $kv = $_ -split '=', 2
  [System.Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), "Process")
}
```

然后启动后端：

```powershell
cd backend
mvn spring-boot:run
```

---

## 5. IDE 启动（推荐，最省事）

### 5.1 IntelliJ IDEA

- Run/Debug Configuration → 你的 Spring Boot 配置
- 在 **Environment variables** 里填入：
  - `DELI_APP_ID=...;DELI_SECRET=...;TENCENT_LLM_API_KEY=...;TENCENT_LLM_ENDPOINT=...;TENCENT_LLM_MODEL=...;LEXAI_KNOWLEDGE_PATH=...`

### 5.2 VS Code

- 如果用 Java 插件启动，在启动配置里设置环境变量（或在终端按 **第 3.2** 方式 `source backend/.env` 后再运行）

---

## 6. 如何“调用你这个 API”（其他成员该怎么做）

结论：其他成员**不需要也不应该**直接调用你个人的 Key。

- **项目调用链**：
  - 前端只调用后端：`/api/legal/*`
  - 后端根据环境变量决定是否使用真实的 **得理** 和 **腾讯混元**（否则走兜底/Mock）
- **其他成员要启用真实 AI**：
  - 在自己电脑上配置同样的环境变量（用自己的密钥）
  - 然后按本指南启动后端即可

---

## 7. 快速自检：确认真实调用已生效

1) 先检查环境变量是否加载成功（macOS/Linux）：

```bash
echo "$DELI_APP_ID"
echo "$TENCENT_LLM_ENDPOINT"
echo "$TENCENT_LLM_MODEL"
```

2) 启动后端后，用 README 里接口任意调用一个（例如咨询/案件分析/合同审查）。

3) 现象判断：
- **如果没配置/没加载成功**：很容易出现“同质化兜底输出”（每次结果非常类似）
- **如果配置成功**：不同输入会有更明显差异，且更贴近法条/案例/合同条款语义

---

## 8. 可以提交到 `main` 的文件清单

允许提交（模板、无密钥）：
- `backend/.env.example`

禁止提交（任何真实值/本地文件）：
- `.env`
- `backend/.env`

