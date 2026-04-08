# LexAI 启动脚本 — 完成总结

## 项目分析

LexAI 是一个**前后端分离的智能法律工作台**，技术栈：
- **前端**: Vue 3 + Vite + TypeScript（端口 5173）
- **后端**: Spring Boot 3 + Java 21 + Maven + H2 内存数据库（端口 8080）
- 前端通过 Vite 代理转发 `/api` 请求到后端

## 创建的文件

### 启动脚本（3 个）

| 文件 | 适用系统 | 使用方式 |
|------|---------|---------|
| [start.bat](file:///d:/Desktop/LexAI/start.bat) | Windows（CMD） | 双击运行，或在项目根目录 `.\start.bat` |
| [start.ps1](file:///d:/Desktop/LexAI/start.ps1) | Windows（PowerShell） | `.\start.ps1`（更推荐，有彩色输出） |
| [start.sh](file:///d:/Desktop/LexAI/start.sh) | macOS / Linux | `chmod +x start.sh && ./start.sh` |

### 停止脚本（2 个）

| 文件 | 适用系统 | 使用方式 |
|------|---------|---------|
| [stop.bat](file:///d:/Desktop/LexAI/stop.bat) | Windows | 双击运行 |
| [stop.sh](file:///d:/Desktop/LexAI/stop.sh) | macOS / Linux | `./stop.sh` |

## 脚本功能

所有启动脚本均包含以下 **5 个步骤**：

1. **环境检查** — 自动检测 Java、Maven、Node.js、npm 是否安装，并显示版本号。缺失时给出安装指引链接
2. **配置检查** — 自动检测 `backend/.env` 是否存在，不存在时从 `.env.example` 复制模板
3. **环境变量加载** — 解析 `.env` 文件，支持 `KEY=VALUE` 和 `export KEY='VALUE'` 两种格式
4. **启动后端** — 在独立窗口/后台启动 Spring Boot，自动轮询健康检查端点等待就绪；若未配置 API 密钥则自动切换为 Mock 模式
5. **启动前端** — 首次运行时自动执行 `npm install`，然后启动 Vite 开发服务器；完成后自动打开浏览器

### 额外特性
- `start.sh`：Ctrl+C 时自动清理后端和前端进程
- `start.ps1` / `start.bat`：后端和前端各在独立 CMD 窗口运行，主窗口可安全关闭
- 所有脚本自动检测路径，**不依赖绝对路径**，clone 到任意目录均可运行
