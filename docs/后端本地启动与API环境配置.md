# 后端本地启动

按顺序做即可。密码、密钥、接口地址由**你自己或负责人提供**，填进本地 `backend/.env`，**不要**把填好的文件提交到 Git。

---

## 零、先确认本机已安装

- **Java 21**（终端执行 `java -version` 能看到 21）
- **Maven**（执行 `mvn -v` 有输出）
- 若要连前端页面：再装 **Node.js**（`node -v`）

---

## 一、进入项目目录

把整个仓库克隆到本地后，记住**项目根目录**路径，下面用「项目根」指这里。

示例（按你的实际路径改）：

```bash
cd /你的路径/LexAI
```

---

## 二、复制环境变量模板

在项目根目录执行：

```bash
cp backend/.env.example backend/.env
```

只会生成一个**本地专用**的 `backend/.env`，该文件已被 Git 忽略，不会上传。

---

## 三、编辑 `backend/.env`，填入你自己的值

用任意编辑器打开 **`backend/.env`**，把下面几项**等号右边**填完整（不要加引号，除非值里本身有空格等特殊字符时再按需加）：

| 要填的项 | 含义 |
|----------|------|
| `DELI_APP_ID` | 得理开放平台给你的 app id |
| `DELI_SECRET` | 得理开放平台给你的 secret |
| `TENCENT_LLM_ENDPOINT` | 腾讯（或兼容 OpenAI）的对话接口完整 URL |
| `TENCENT_LLM_API_KEY` | 该接口的 API Key（Bearer） |
| `TENCENT_LLM_MODEL` | 模型名称（按你们控制台或文档填写） |

# LexAI / Deli credentials
export DELI_APP_ID='QthdBErlyaYvyXul'
export DELI_SECRET='EC5D455E6BD348CE8E18BE05926D2EBE'

# LexAI / Tencent LLM credentials
export TENCENT_LLM_API_KEY='sk-uPQesAvXHd6kEaSNhlM56NwWjQzOgDewpesMv80Axa8zgsT5'
export TENCENT_LLM_ENDPOINT='https://api.hunyuan.cloud.tencent.com/v1/chat/completions'
export TENCENT_LLM_MODEL='hunyuan-lite'


**可选**（不填也能启动，只是本地知识库检索为空）：取消注释并填写 `LEXAI_KNOWLEDGE_PATH`，例如从 `backend` 目录相对到仓库知识库：

```text
LEXAI_KNOWLEDGE_PATH=../docs/knowledge-base
```

保存文件后关闭编辑器。

---

## 四、用终端启动后端（macOS / Linux）

**必须**先让当前终端「认识」`.env` 里的变量，再启动 Spring Boot。

在项目根目录执行：

```bash
cd backend
set -a
source .env
set +a
mvn spring-boot:run
```

看到日志里 Spring 启动完成、没有立刻报错退出，即表示进程在跑。**不要关这个终端窗口**。

- 后端根路径：`http://localhost:8080/api`
- 健康检查：浏览器或使用下面命令：

```bash
curl http://localhost:8080/api/actuator/health
```

若返回含 `"status":"UP"` 即正常。

---

## 五、Windows 用户怎么启动后端

若用 **PowerShell**，在 `backend` 目录下可先逐行设置（把值换成你自己的），再运行 Maven：

```powershell
$env:DELI_APP_ID="你的得理APP_ID"
$env:DELI_SECRET="你的得理SECRET"
$env:TENCENT_LLM_ENDPOINT="你的接口URL"
$env:TENCENT_LLM_API_KEY="你的API_KEY"
$env:TENCENT_LLM_MODEL="你的模型名"
mvn spring-boot:run
```

若用 **Git Bash**，可与第四节一样使用 `set -a`、`source .env`。

---

## 六、（可选）启动前端页面

**新开一个终端窗口**（后端那个继续保持运行），在项目根执行：

```bash
cd frontend
npm install
npm run dev
```

浏览器打开终端里提示的地址（一般为 `http://localhost:5173`）。

---

## 七、怎样算「API 真正调到了」（不是纯兜底）

随便测一条法律咨询，若返回里 **法律依据** 出现真实法条片段、**建议** 内容随问题变化，一般说明得理 + 大模型链路已生效。若全是固定套话、且提示「未检索到法条」之类，多半是环境变量未加载或未填对，回到第三、四步检查。

---

## 八、不想配密钥、只跑界面

若暂时没有任何密钥，可把后端改成 Mock 模式再启动（无需得理/腾讯）。在 `backend/src/main/resources/application.yml` 里把 `lexai.ai.mode` 改为 `mock`，或启动时加参数：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--lexai.ai.mode=mock"
```

---

## 九、常见问题

| 现象 | 处理 |
|------|------|
| 提示端口 8080 被占用 | 关掉占用 8080 的程序，或启动时加 `--server.port=8081` 并相应改前端代理 |
| `source: .env: No such file` | 确认当前目录是 `backend`，且已执行过第二节复制出 `.env` |
| 仍然全是兜底输出 | 核对 `.env` 是否保存、第四节是否在**同一终端**先 `source` 再 `mvn`；检查网络能否访问得理与腾讯接口地址 |
