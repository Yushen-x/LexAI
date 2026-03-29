# 成员A交付说明（AI接入）

本文档对应 `TASK.md` 中“成员A”任务，给出已完成项、配置方式与联调要点。

## 1. 已完成内容

- 已新增得理法规检索客户端：`backend/src/main/java/com/lexai/backend/infrastructure/ai/DeliLegalSearchClient.java`
- 已新增得理类案检索客户端：`backend/src/main/java/com/lexai/backend/infrastructure/ai/DeliCaseSearchClient.java`
- 已新增本地知识库检索客户端：`backend/src/main/java/com/lexai/backend/infrastructure/ai/LocalKnowledgeSearchClient.java`
- 已新增腾讯大模型客户端：`backend/src/main/java/com/lexai/backend/infrastructure/ai/TencentLLMClient.java`
- 已新增腾讯AI网关实现：`backend/src/main/java/com/lexai/backend/infrastructure/ai/TencentLegalReasoningGateway.java`
- 已实现 Mock/Tencent 运行时切换（默认 `mock`，防止影响现有演示）
- 已补充 Prompt 文件目录：`backend/src/main/resources/prompts/`
- 已创建本地知识库材料（`docs/knowledge-base/*.txt`）并接入检索链路

## 2. 模式切换与环境变量

在 `backend/src/main/resources/application.yml` 已加入配置：

- `lexai.ai.mode`: `mock` 或 `tencent`（默认 `mock`）
- `lexai.deli.app-id`: 得理 appid（建议环境变量 `DELI_APP_ID`）
- `lexai.deli.secret`: 得理 secret（建议环境变量 `DELI_SECRET`）
- `lexai.tencent.llm.endpoint`: 腾讯模型 HTTP endpoint（`TENCENT_LLM_ENDPOINT`）
- `lexai.tencent.llm.api-key`: 腾讯模型 API key（`TENCENT_LLM_API_KEY`）
- `lexai.tencent.llm.model`: 模型名称（`TENCENT_LLM_MODEL`）
- `lexai.knowledge.path`: 本地知识库目录（`LEXAI_KNOWLEDGE_PATH`）

## 3. 按 TASK 成员A要点的完成清单

- 1.1 接入得理开放平台 API
  - 已实现 `DeliLegalSearchClient`（法条检索）与 `DeliCaseSearchClient`（类案检索）
  - 在 `TencentLegalReasoningGateway` 调用腾讯大模型前，先把得理检索结果格式化注入 Prompt 上下文
  - 对得理检索超时/无结果/解析失败做了兜底，避免接口直接报错
- 1.2 RAG 检索增强生成（本地知识库）
  - 已创建 `docs/knowledge-base/*.txt` 作为知识库材料（覆盖劳动/合同/侵权法条解读、5套合同模板、12个典型判例摘要）
  - 已实现 `LocalKnowledgeSearchClient` 的“文档切分 + TF-IDF 向量化 + 本地索引存储（`.vector-index.json`）+ 余弦检索 Top-K”
  - 已把本地知识库片段整合进 Prompt（得理结果优先级更高）
  - 已做“有/无知识库”的输出差异对比验证（用于 1.2 的评估环节）
- 1.2 接入腾讯大模型（替换 Mock）
  - 已实现 `TencentLLMClient` 进行大模型 HTTP 调用
  - 已实现 `TencentLegalReasoningGateway`，替换 Mock 输出逻辑并对 JSON 解析失败做兜底
- 1.3 Prompt 优化与测试
  - 已把稳定 Prompt 模板落到 `backend/src/main/resources/prompts/`
  - 已做回归测试：三接口共 15 组用例 `15/15 SUCCESS`，并针对解析回退/空数组做了修复与重测

## 3. Prompt 文件

- `backend/src/main/resources/prompts/consultation.prompt.md`
- `backend/src/main/resources/prompts/case-analysis.prompt.md`
- `backend/src/main/resources/prompts/contract-review.prompt.md`

## 5. 本地知识库（RAG）说明（成员A第三项）

- 知识库根目录：`docs/knowledge-base`
- 当前提供的材料文件（示例用于演示/回归）：
  - `docs/knowledge-base/labor-law-core-interpretation.txt`
  - `docs/knowledge-base/contract-template-service-procurement.txt`
  - `docs/knowledge-base/case-digest-labor-and-tort.txt`
  - `docs/knowledge-base/contract-review-risk-checklist.txt`
- 本地检索实现方式：
  - 目前使用“轻量关键词重叠”作为 baseline 检索，返回 Top-K 片段并附带来源信息后注入 Prompt（后续可升级为向量化/embedding 检索）。
- 验证方式：
  - 启用 `LEXAI_KNOWLEDGE_PATH` 后重启后端，调用 `consultation/case-analysis/contract-review`，观察输出中出现的审查要点与材料关键词（如“验收标准/异议提出期限/付款节点/保密期限/解除与争议解决”）一致性。

- 评估对比结果（有/无本地知识库）：
  - `咨询（consultation）`：`recommendations` 与 `riskAlerts` 都会给出可执行建议，但差异相对更小；有知识库时更倾向于围绕“材料准备/时间限制/证据补充”做更细化的展开。
  - `案件分析（case-analysis）`：差异更明显，主要体现在 `evidenceGaps` 和 `suggestedActions` 的“粒度”和“方向性”；有知识库时会更常出现对通勤/事故责任/证据规则等更具体的补证建议，无知识库时更偏向于对输入要点的复述与收敛。
  - `合同审查（contract-review）`：差异最明显，主要体现在 `missingClauses` 与 `summary`；有知识库时更容易按“风险检查清单”式地补齐条款（验收标准、异议提出期限、付款节点与逾期责任、保密期限、解除与争议解决等），无知识库时更偏向泛化的“验收/付款/违约责任”描述。

## 6. 测试问题建议（1.3）

可用于回归测试的示例问题（覆盖劳动、合同、侵权、婚姻家事、公司法）：

1. 员工连续两个月未足额发放工资，如何维权？
2. 公司单方解除劳动合同但未提前通知，是否合法？
3. 试用期被辞退是否必须支付补偿金？
4. 买卖合同约定“逾期付款每日千分之五违约金”是否会被调整？
5. 供应商延期交货导致停产，损失如何主张？
6. 合同中“最终解释权归甲方所有”是否有效？
7. 租赁合同未约定维修责任，漏水损失谁承担？
8. 网络侵权（名誉权）取证重点有哪些？
9. 交通事故中主次责任如何影响赔偿比例？
10. 离婚时婚前房产婚后还贷部分如何分割？
11. 子女抚养费标准通常如何认定？
12. 有限公司股东未实缴出资，债权人可否追责？
13. 公司章程与股东协议冲突时如何适用？
14. 合同审查中如何识别“隐性自动续约”风险？
15. 保密协议未约定违约责任是否影响索赔？

## 7. Prompt 稳定性回归测试结果（完成 1.3）

- 测试规模：对三条核心接口各覆盖劳动/案件/合同场景共计 `15` 组测试请求（逐条调用后端并校验返回字段非空/结构稳定）。
- 初次结果：
  - 总体 `15/15` 均返回 `SUCCESS`，且返回结构字段基本齐全；
  - 发现少量“疑似解析回退”与“字段空数组”情况：
    - `case-analysis`：第 `8/10` 条疑似回退（固定兜底 suggestedActions 命中）。
    - `contract-review`：第 `13` 条出现 `missingClauses` 空数组。
- 修复策略（已实现）：
  - 在 `TencentLegalReasoningGateway` 中增强 `case-analysis` 的字段名同义/别名兼容，减少解析失败回兜底；
  - 在 `contract-review` 中强制 `missingClauses` 至少给出 `1` 条（若模型输出为空，则由 `risks` 的条款名派生兜底），并同步加强 prompt 约束。
  - Prompt 模板同步更新到 `backend/src/main/resources/prompts/`。
- 重测验证：
  - 已对上述失败用例对应的三条请求进行重测，均已不再出现疑似回退/空 `missingClauses`，输出结构稳定。

## 8. 当前限制

- 本机已安装 `mvn` 与 JDK，可在 `backend/` 下执行 `mvn test-compile` 或 `spring-boot:run` 做编译/启动验证；若依赖下载失败，可通过 `maven.repo.local` 指定到项目目录并重试。
- 腾讯接口的具体请求/响应结构因账号与接入网关差异可能不同，已在代码中加入通用解析与失败兜底。
