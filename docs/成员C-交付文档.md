# 成员C 交付文档（字段验证 + 联调）

这份文档面向成员C：你只需要做“把前端字段展示的结果跟后端真实返回结构对齐”，并完成一次联调验证即可。

## 0. 联调前置条件（必须确认）

1. 后端已启动并可访问：`http://localhost:8080/api`
2. AI 网关已接入完成（成员A已做）：后端接口返回中应出现真实检索/模型生成内容，而不是固定同一句 Mock
3. 你需要用三条接口各跑一次验证：
   - `POST /api/legal/consultation`
   - `POST /api/legal/case-analysis`
   - `POST /api/legal/contract-review`

## 1. 三个接口各自要验证哪些字段

### 1.1 法律咨询（Consultation）

后端接口：`POST /api/legal/consultation`

前端期望字段（来自 `frontend/src/shared/types/legal.ts`）：
- `data.category`: string
- `data.legalBasis`: string[]
- `data.recommendations`: string[]
- `data.riskAlerts`: string[]

你需要验证：
- 页面能正常渲染四个字段
- 数组不为空（至少有 1 项）

建议请求体（供你快速测试）：
```json
{
  "question": "我想申请工伤认定，申请材料需要准备哪些？关键期限有哪些？",
  "facts": ["上下班途中发生交通事故", "已取得事故认定书"]
}
```

### 1.2 案件分析（Case Analysis）

后端接口：`POST /api/legal/case-analysis`

前端期望字段：
- `data.keyFacts`: string[]
- `data.disputedIssues`: string[]
- `data.evidenceGaps`: string[]
- `data.suggestedActions`: string[]

你需要验证：
- 页面上“关键事实 / 争议焦点 / 证据缺口 / 后续建议”四块都能正常渲染
- 数组不为空（至少有 1 项）

建议请求体：
```json
{
  "caseSummary": "上下班途中交通事故导致工伤认定纠纷，事故责任比例与通勤路径存在争议",
  "evidencePoints": ["事故认定书", "通勤打卡记录", "就医记录"]
}
```

### 1.3 合同审查（Contract Review）

后端接口：`POST /api/legal/contract-review`

前端期望字段：
- `data.risks`: 结构化数组（每项包含）
  - `level`: `LOW|MEDIUM|HIGH`
  - `clause`: string  （注意：前端字段名是 `clause`）
  - `issue`: string
  - `suggestion`: string
- `data.missingClauses`: string[]（至少 1 项）
- `data.summary`: string

你需要验证：
- 页面“高/中/低风险条款计数”能正确统计（取决于 `risks[].level`）
- 缺失条款列表 `missingClauses` 不为空（成员A已做兜底）
- risks 列表能正常渲染：展示 `risks[].clause`（不要使用不存在的 `clauseTitle`）

建议请求体：
```json
{
  "contractTitle": "服务合同审查（验收+付款+保密）",
  "contractContent": "本合同约定交付后按对账结果结算，但验收标准不够明确；未设异议提出期限；付款仅写总价未约定付款节点与逾期责任；保密义务持续至信息公开之日但保密期限与例外不明确；解除与争议解决条款较简略。"
}
```

## 2. 你重点要注意的“字段名坑”

1. 案件分析：必须是 `disputedIssues`（不是 `disputeFocalPoints`）
2. 合同审查：风险条款原文展示字段名是 `risks[].clause`（不是 `clauseTitle`）

## 3. 验证完成的交付口径（你怎么判断算通过）

1. 前端三页面都能正常渲染四个/多个字段
2. 三接口的返回 JSON 与上面的字段要求一致
3. 没有出现 `undefined/null` 或空数组导致 UI 不显示的情况（尤其是 `missingClauses`）

## 4. 若出现问题，先检查这两类

1. 字段名对不上（最常见）：按本文件“字段名坑”逐项核对
2. 返回为空数组：建议先用成员A建议的测试请求体跑一遍定位原因（模型解析失败/检索无结果/兜底触发）

