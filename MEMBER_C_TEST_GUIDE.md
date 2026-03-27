# 成员C功能测试指南与项目运行说明

**文档版本：v1.0**  
**最后更新：2026年3月25日**  
**准备者：成员C（合同起草全栈）**

---

## 📋 目录

1. [功能概述](#功能概述)
2. [系统要求](#系统要求)
3. [项目启动](#项目启动)
4. [功能测试清单](#功能测试清单)
5. [测试用例](#测试用例)
6. [故障排查](#故障排查)
7. [演示场景](#演示场景)

---

## 功能概述

### 成员C负责的功能范围

**合同起草全栈 + 核心功能字段验证**

#### 3.1 合同起草后端API ✅
- **端点**：`POST /api/legal/contract-draft`
- **功能**：接收用户填写的合同基本信息，调用AI生成完整合同文本
- **请求字段**：
  - contractName（合同名称）*必填
  - contractType（合同类型）*必填
  - partyA（甲方名称）*必填
  - partyB（乙方名称）*必填
  - amount（合同金额）*必填
  - duration（合同期限）*可选
  - requirements（核心需求）*可选

#### 3.2 合同起草前端接线 ✅
- **页面**：`/contract-draft`
- **功能列表**：
  1. ✅ 生成合同 - 根据表单数据生成完整合同
  2. ✅ AI侧边栏 ASK模式 - 问答咨询
  3. ✅ AI侧边栏 AGENT模式 - 修改指令
  4. ✅ 存草稿 - 保存到localStorage
  5. ✅ 提交审查 - 跳转到合同审查页面
  6. ✅ 复制全文 - 一键复制
  7. ✅ 下载TXT - 导出为文本文件

#### 3.3 核心功能字段验证 ✅
- ✅ **法律咨询** - 字段对齐验证（category, legalBasis, recommendations, riskAlerts）
- ✅ **案件分析** - 字段对齐验证（keyFacts, disputeFocalPoints, evidenceGaps, actionRecommendations）
- ✅ **合同审查** - UI优化（风险等级颜色编码）
- ✅ **前端错误处理** - Axios拦截器统一错误提示

---

## 系统要求

### 本地开发环境
- **JDK**：11+ (建议 17)
- **Maven**：3.6+
- **Node.js**：16+
- **npm**：8+
- **Git**：2.0+

### 硬件要求
- CPU: 2核+
- 内存: 4GB+
- 磁盘: 5GB+（包含依赖下载）

### 浏览器支持
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

---

## 项目启动

### 整体架构
```
LexAI 项目
├── Backend (Spring Boot)
│   └── 运行端口: 8080
└── Frontend (Vue 3 + Vite)
    └── 运行端口: 5173
```

### 方式一：IDE集成开发（推荐）

#### 1. 后端启动 (IntelliJ IDEA)

```bash
# 进入项目目录
cd /Users/linqi/Desktop/LexAI

# 1. 打开 IDE
# File → Open → 选择 LexAI/backend 目录

# 2. 配置 JDK
# Project Structure → Project → JDK 选择 11+

# 3. Maven 导入依赖
# 右键 pom.xml → Maven → Reload Project

# 4. 运行主类
# 在 LexAiApplication.java 上右键 → Run
# 或快捷键 Ctrl+Shift+F10 (Win) / Ctrl+Shift+R (Mac)
```

**预期输出**：
```
2026-03-25 12:00:00.000  INFO ... LexAiApplication : Started LexAiApplication in 5.234 seconds
```

#### 2. 前端启动

```bash
# 进入前端目录
cd /Users/linqi/Desktop/LexAI/frontend

# 安装依赖（首次）
npm install

# 启动开发服务器
npm run dev

# 预期输出:
#   VITE v4.x.x  ready in XXX ms
#   ➜  Local:   http://localhost:5173/
#   ➜  press h to show help
```

### 方式二：命令行启动（快速验证）

#### 1. 后端启动

```bash
cd /Users/linqi/Desktop/LexAI/backend

# 清理并构建
mvn clean compile

# 运行
mvn spring-boot:run

# 服务器将在 http://localhost:8080 启动
```

#### 2. 前端启动

```bash
cd /Users/linqi/Desktop/LexAI/frontend

# 启动开发服务器
npm run dev
```

### 方式三：Docker启动（生产环境模拟）

```bash
# 构建后端镜像
cd /Users/linqi/Desktop/LexAI/backend
mvn clean package -DskipTests
docker build -t lexai-backend:latest .
docker run -p 8080:8080 lexai-backend:latest

# 构建前端镜像
cd /Users/linqi/Desktop/LexAI/frontend
npm run build
docker build -t lexai-frontend:latest .
docker run -p 5173:5173 lexai-frontend:latest
```

---

## 功能测试清单

### 预检清单（启动前）

- [ ] 后端编译无误 (`mvn clean compile` 成功)
- [ ] 前端依赖已安装 (`npm install` 成功)
- [ ] 端口 8080 和 5173 未被占用
- [ ] 网络连接正常

### 启动检查

#### 后端检查

| 检查项 | 预期结果 | 状态 |
|--------|--------|------|
| 应用启动成功 | 看到 "Started LexAiApplication" | ✅ |
| 健康检查端点 | GET http://localhost:8080/system/health → 200 OK | ✅ |
| Swagger文档 | http://localhost:8080/swagger-ui.html 可访问 | ⚠️ (可选) |

#### 前端检查

| 检查项 | 预期结果 | 状态 |
|--------|--------|------|
| 开发服务器启动 | 看到 "Local: http://localhost:5173" | ✅ |
| 页面加载 | http://localhost:5173 显示LexAI工作台 | ✅ |
| 控制台无错误 | F12 Console 无红色错误信息 | ✅ |

---

## 测试用例

### 测试点 1：合同起草页面基本加载

**步骤**：
1. 打开浏览器，访问 `http://localhost:5173`
2. 在左侧导航点击 "合同起草" (或直接访问 `/contract-draft`)

**预期结果**：
- ✅ 页面正常加载，无加载错误
- ✅ 看到表单表格：合同名称、合同类型、甲方名称、乙方名称、标的额、合同期限
- ✅ 看到"生成合同"、"存草稿"、"生成并提交审查"三个按钮
- ✅ 右侧AI侧边栏显示正常

---

### 测试点 2：生成合同功能

**前置条件**：合同起草页面已正常加载

**步骤**：
1. 在表单中填入测试数据：
   ```
   合同名称：云服务采购合同
   合同类型：采购合同
   甲方名称：ABC有限公司
   乙方名称：XYZ科技公司
   标的额：50000
   合同期限：12个月
   核心需求：云服务、技术支持
   ```
2. 点击 "生成合同" 或 "生成并提交审查"
3. 观察加载过程

**预期结果**：
- ✅ 右侧编辑区显示加载动画（3秒左右）
- ✅ 生成的合同包含以下章节：
  - 合同标题和日期
  - 甲乙方信息
  - 服务内容与范围
  - 服务期限
  - 费用与支付条款
  - 双方权利与义务
  - 保密条款
  - 违约责任条款
  - 争议解决方式
- ✅ 正文中包含用户填入的公司名称和标的额数值

**关键验证点**：
```
应检查以下关键内容是否在生成文本中：
- "ABC有限公司"（甲方）
- "XYZ科技公司"（乙方）  
- "¥50000元"（金额）
- "12个月"（期限）
- 违约金计算：50000 * 0.1% = 50元/天
- 三阶段付款：30% → 15000元，70% → 35000元
```

---

### 测试点 3：AI侧边栏 ASK模式

**前置条件**：已生成一份合同

**步骤**：
1. 确保AI侧边栏已展开（右上角 "呼出助手" 按钮）
2. 点击 "问答咨询" tab
3. 在输入框输入问题：`这份合同的保密条款有啥问题？`
4. 点击 "发送指令" 或 Ctrl+Enter

**预期结果**：
- ✅ 用户消息显示在聊天框（靠右，蓝色背景）
- ✅ AI回复显示在下方（靠左，白色背景，包含 "Lex" 头像）
- ✅ AI回复内容涉及保密条款建议
- ✅ 支持多轮对话

**示例对话**：
```
用户：这份合同有什么风险？
AI：我已经检查了您提供的信息。主要风险包括：
    1. 付款条件不够明确
    2. 违约责任需要细化
    3. 争议解决机制需要补充

用户：帮我改一下违约金条款
AI：已接受指令。我为您生成了符合标的额量级的违约责任条款，请核对。
   [显示修改建议预览框]
```

---

### 测试点 4：AI侧边栏 AGENT模式

**前置条件**：已生成合同，ASK对话进行过

**步骤**：
1. 点击 "Agent 修改指令" tab
2. 在输入框输入指令：`把违约金提升到千分之二`
3. 点击 "发送指令"

**预期结果**：
- ✅ 聊天框显示用户指令
- ✅ AI回复显示修改建议
- ✅ 显示 "INSERT 操作" 的预览框，包含修改内容
- ✅ 预览框下方有两个按钮："应用此修改" 和 "撤销"

**验证修改生效**：
1. 点击 "应用此修改"
2. 查看左侧编辑区，应该在合约末尾追加新的违约条款

---

### 测试点 5：存草稿功能

**前置条件**：已生成合同或编辑过合同内容

**步骤**：
1. 在合同编辑区修改或添加内容
2. 点击 "存草稿" 按钮
3. 等待提示信息

**预期结果**：
- ✅ 按钮显示 "保存中..." 的临时状态
- ✅ 弹出提示："草稿已保存！"
- ✅ 刷新页面后，内容仍然保留（localStorage持久化）

**验证持久化**：
1. F12 打开开发者工具 → Application → Local Storage
2. 查看 `contract_draft_云服务采购合同` 的键值
3. 应该包含 JSON 格式的合同数据

---

### 测试点 6：提交审查功能

**前置条件**：已生成合同

**步骤**：
1. 点击 "生成并提交审查" 按钮
2. 观察页面跳转

**预期结果**：
- ✅ 按钮显示 "提交中..."
- ✅ 页面跳转到合同审查页面 (`/contract-review`)
- ✅ 合同审查页面的编辑框自动填入之前生成的合同内容
- ✅ 点击"开始智能审查"会分析该合同的风险

---

### 测试点 7：复制和下载功能

**前置条件**：已生成合同

**步骤**：

#### 7.1 复制全文
1. 点击编辑区右上角的 "📋 复制全文"
2. 打开文本编辑器（如记事本），粘贴（Ctrl+V）

**预期结果**：
- ✅ 弹出提示："已复制到剪贴板"
- ✅ 粘贴的内容是完整的合同正文

#### 7.2 下载TXT
1. 点击编辑区右上角的 "⬇️ 下载TXT"
2. 检查浏览器下载文件夹

**预期结果**：
- ✅ 下载一个 `.txt` 文件，名称为 `合同名称.txt`
- ✅ 文件大小合理（> 2KB）
- ✅ 用文本编辑器打开，内容完整

---

### 测试点 8：表单验证

**步骤**：
1. 不填任何字段，点击 "生成合同"

**预期结果**：
- ✅ 显示错误提示："请填写甲方名称、乙方名称和标的额"
- ✅ 合同不会生成

---

### 测试点 9：法律咨询页面字段验证

**步骤**：
1. 访问 `/consultation`（法律咨询）
2. 输入问题，点击 "开始分析"
3. 查看返回结果的字段

**预期结果**：
- ✅ 返回的结果包含字段：
  - `category`（问题分类）
  - `legalBasis`（法律依据列表）
  - `recommendations`（建议列表）
  - `riskAlerts`（风险提示列表）
- ✅ 这些字段在页面上正确展示（三列布局）

---

### 测试点 10：合同审查风险UI

**步骤**：
1. 访问 `/contract-review`（合同审查）
2. 输入一份标准合同，点击 "开始智能审查"
3. 查看风险显示

**预期结果**：
- ✅ 高风险项显示为 **红色**
- ✅ 中风险项显示为 **橙色**
- ✅ 低风险项显示为 **黄色**
- ✅ 每条风险显示：风险等级、原条款、风险释明、修改建议

---

## 故障排查

### 问题 1：后端启动失败 - "端口 8080 已被占用"

**症状**：
```
Caused by: java.net.BindException: Address already in use
```

**解决方案**：

```bash
# macOS/Linux - 查找占用 8080 的进程
lsof -i :8080

# 杀死进程
kill -9 <PID>

# 或者改用其他端口
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

---

### 问题 2：前端 API 请求失败 - 404 或 502

**症状**：
```
POST http://localhost:8080/api/legal/contract-draft 404 (Not Found)
```

**排查步骤**：
1. 检查后端是否启动：`curl http://localhost:8080/system/health`
2. 检查网络：确保前端和后端在同一局域网
3. 检查 API 路径是否正确（应为 `/api/legal/contract-draft`）
4. 检查后端代码是否编译：`mvn clean compile`

**解决方案**：
```bash
# 重启后端
mvn clean compile
mvn spring-boot:run
```

---

### 问题 3：前端加载页面blank（白屏）

**症状**：浏览器显示空白页面

**排查步骤**：
1. 打开 F12 控制台，查看错误信息
2. 查看 Network 标签，检查 JS 文件是否加载成功

**解决方案**：
```bash
# 清理缓存并重新启动
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

---

### 问题 4：合同生成为空或格式错误

**症状**：点击"生成合同"后，编辑区提示内容为空

**排查步骤**：
1. 打开浏览器 F12 → Network → 查看 POST 请求的响应
2. 检查是否返回 500 错误

**解决方案**：
```bash
# 检查后端日志
# 在 IDE 中看 Run 窗口的输出
# 或者通过命令行重启后端：
mvn spring-boot:run

# 检查 Request 数据是否完整：
# amount 必须是数字类型，不能是字符串
```

---

### 问题 5：localStorage 草稿未保存

**症状**：刷新页面后，之前的草稿消失

**排查步骤**：
1. F12 → Application → Local Storage → http://localhost:5173
2. 查看是否有 `contract_draft_*` 的键

**解决方案**：
```bash
# 浏览器隐私模式下 localStorage 不可用
# 使用正常的浏览窗口（非隐私模式）

# 或者清理浏览器缓存后重试
```

---

### 问题 6：AI侧边栏不显示或按钮无反应

**症状**：右侧AI侧边栏不出现，或"发送指令"按钮点击无反应

**排查步骤**：
1. F12 Console 查看是否有错误
2. 检查 `aiSidebarVisible` 状态
3. 检查是否安装了 Vue DevTools

**解决方案**：
```bash
# 清理浏览器缓存
# Ctrl+R 刷新页面
# 或在 URL 栏输入：
http://localhost:5173/?nocache=1

# 如果仍未解决，重启开发服务器
npm run dev
```

---

## 演示场景

### 场景 1：完整的合同生成 + 审查流程（5分钟）

**目标**：从零到生成并审查一份合同

**步骤**：
1. 打开 http://localhost:5173/contract-draft
2. 填入表单数据（如上文测试点2）
3. 点击"生成合同"，等待生成
4. 询问 AI："这份合同有什么风险？"
5. 点击"生成并提交审查"
6. 在审查页面查看合同的风险分析

**演示效果**：展示 LexAI 从起草到审查的全流程

---

### 场景 2：AI Agent 智能修改（3分钟）

**目标**：展示 Agent 模式的修改建议

**步骤**：
1. 在合同页面已生成合同
2. 切换到 "Agent 修改指令" tab
3. 输入指令："加上知识产权条款"
4. AI 返回修改建议，点击"应用此修改"
5. 查看左侧编辑区已追加新条款

**演示效果**：展示 AI 对合同的智能增强

---

### 场景 3：多人协作场景模拟（用于成员间对接）

**依赖关系验证**：
1. ✅ 成员C 合同起草 → 存草稿（调用localStorage）
2. ✅ 成员B 合同台账取数 ← 成员C 的合同记录
3. ✅ 成员A AI接入 → 成员C 的生成逻辑升级
4. ✅ 成员D 演示 ← 使用成员C的完整功能

**验证清单**：
- [ ] 后端API可被任何页面调用
- [ ] 前端API函数可复用
- [ ] 类型定义导出正确
- [ ] 错误处理统一

---

## 快速命令参考

### 后端相关
```bash
# 清理并编译
mvn clean compile

# 运行
mvn spring-boot:run

# 单元测试
mvn test

# 打包
mvn clean package -DskipTests
```

### 前端相关
```bash
# 安装依赖
npm install

# 开发服务器
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

### 常用链接
- 工作台首页：http://localhost:5173
- 合同起草：http://localhost:5173/contract-draft
- 法律咨询：http://localhost:5173/consultation
- 合同审查：http://localhost:5173/contract-review
- 案件分析：http://localhost:5173/case-analysis
- 后端健康检查：http://localhost:8080/system/health

---

## 性能测试基准

| 操作 | 预期时间 | 环境 |
|------|--------|------|
| 页面加载 | < 2s | 首次访问，无缓存 |
| 合同生成 | 2-3s | 本地 Mock 实现 |
| 存草稿 | < 200ms | 本地 localStorage |
| AI 对话响应 | 1s | 模拟延迟 |

---

## 成功标志 ✅

当你看到以下现象时，说明功能完整可用：

- [x] 后端启动成功，日志显示 "Started LexAiApplication"
- [x] 前端访问无白屏，页面正常加载
- [x] 合同生成后包含完整的8个章节
- [x] AI侧边栏支持 ASK 和 AGENT 两种模式
- [x] localStorage 正确保存草稿数据
- [x] 页面间导航流畅（起草→审查）
- [x] 浏览器控制台无红色错误信息
- [x] 所有表单字段都能正确验证

---

## 联系反馈

如在测试中遇到问题，请记录：
1. **问题描述** - 发生了什么
2. **复现步骤** - 如何重现
3. **预期结果** - 应该发生什么
4. **实际结果** - 实际发生了什么
5. **截图/日志** - 附加错误信息

这些信息有助于快速定位和解决问题。

---

**文档完成。祝测试顺利！** 🚀
