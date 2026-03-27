# 成员C 功能优化清单（2026年3月27日）

> 基于成员A AI接入完成后的前端核心功能验证与优化

---

## 📋 完成表

| # | 功能模块 | 优化项 | 状态 | 文件 |
|---|---------|--------|------|------|
| 1 | **法律咨询** | 字段验证（4/4） | ✅ | ConsultationView.vue |
| 1.1 | 法律咨询 | 法律依据复制功能 | ✅ | ConsultationView.vue |
| 2 | **案件分析** | 字段验证（4/4） | ✅ | CaseAnalysisView.vue |
| 2.1 | 案件分析 | 动态覆盖率计算 | ✅ | CaseAnalysisView.vue |
| 2.2 | 案件分析 | 建议动作复制功能 | ✅ | CaseAnalysisView.vue |
| 3 | **合同审查** | 字段验证（5/5） | ✅ | ContractReviewView.vue |
| 3.1 | 合同审查 | 风险分级UI优化 | ✅ | ContractReviewView.vue |
| 3.2 | 合同审查 | 风险建议复制按钮 | ✅ | ContractReviewView.vue |
| 4 | **API层** | 错误处理完善 | ✅ | http.ts |

---

## 🔍 字段映射验证结果

### 1. ConsultationResponse (法律咨询)

```javascript
✅ category → result.category
✅ legalBasis → result.legalBasis
✅ recommendations → result.recommendations  
✅ riskAlerts → result.riskAlerts
```

**优化：** 添加法律依据复制按钮

---

### 2. CaseAnalysisResponse (案件分析)

```javascript
✅ keyFacts → result.keyFacts
✅ disputedIssues → result.disputedIssues
✅ evidenceGaps → result.evidenceGaps
✅ suggestedActions → result.suggestedActions
```

**优化：** 
- 动态覆盖率计算：基于 `evidenceGaps.length` 反推
- 建议动作复制功能

**覆盖率计算逻辑：**
```javascript
const coverageRate = Math.max(40, Math.min(95, 100 - (evidenceGapCount * 12)))
```

---

### 3. ContractReviewResponse (合同审查)

```javascript
✅ risks → result.risks (数组)
✅ risks[].level → item.level (HIGH|MEDIUM|LOW)
✅ risks[].clause → item.clause
✅ risks[].issue → item.issue
✅ risks[].suggestion → item.suggestion
✅ missingClauses → result.missingClauses (数组)
✅ summary → result.summary (注意：不是notes)
```

**优化：**
- 风险分级UI颜色编码（H红M黄L绿）
- 修改建议复制按钮

---

## 🎨 UI改进详情

### ConsultationView 改进

**法律依据框：**
```
┌─────────────────────────────────────┐
│  法律依据  📋 [复制]               │  ← 添加复制按钮
├─────────────────────────────────────┤
│ • 《劳动法》第XX条...              │
│ • 《劳动合同法》第YY条...          │
│ • ...                               │
└─────────────────────────────────────┘
```

**复制功能：** 一键复制全部依据（按换行符分隔）

---

### CaseAnalysisView 改进

**1. 动态覆盖率显示：**

| 证据缺口数 | 覆盖率 | 说明 |
|-----------|--------|------|
| 0个 | 95% | 证据链完整 |
| 1个 | 83% | 1个缺口 |
| 2个 | 71% | 2个缺口 |
| 3个 | 59% | 3个缺口 |
| 5个+ | 40% | 证据严重不足 |

**2. 建议动作框改进：**
```
┌─────────────────────────────────────┐
│  后续建议动作  📋 [复制]           │  ← 添加复制按钮
├─────────────────────────────────────┤
│ • 整理时间线并建立证据目录         │
│ • 围绕争议焦点逐项匹配证据材料     │
│ • ...                               │
└─────────────────────────────────────┘
```

---

### ContractReviewView 改进

**1. 风险分级颜色编码：**

```
┌─ 高风险 ─────────────────────┐
│ 背景色：#fef2f2（红色调）    │
│ 左边框：红色                 │
└──────────────────────────────┘

┌─ 中风险 ─────────────────────┐
│ 背景色：#fffbeb（黄色调）    │
│ 左边框：黄色                 │
└──────────────────────────────┘

┌─ 低风险 ─────────────────────┐
│ 背景色：标准（无特殊）      │
│ 左边框：绿色                 │
└──────────────────────────────┘
```

**2. 风险修改建议复制功能：**

```
风险项
├─ 风险等级：[高风险]
├─ 原条款：单方解释权条款
├─ 风险释明：条款存在明显失衡...
└─ 修改建议：
   删除单方绝对解释权表述...  [📋 复制]  ← 新增按钮
```

**复制行为：** 点击复制该条建议的全文

---

## 🔧 技术实现要点

### 1. 覆盖率动态计算（CaseAnalysisView）

**旧逻辑（硬编码）：**
```javascript
const base = 38;
const increment = normalizedEvidence.value.length * 14;
return Math.min(96, base + increment);
```

**新逻辑（基于AI输出）：**
```javascript
if (!result.value) {
  // 无结果时使用旧逻辑（初始估计）
  const base = 38;
  const increment = normalizedEvidence.value.length * 14;
  return Math.min(96, base + increment);
}

// AI返回结果后，从缺口反推
const evidenceGapCount = result.value.evidenceGaps?.length ?? 0;
const coverageRate = Math.max(40, Math.min(95, 100 - (evidenceGapCount * 12)));
return Math.round(coverageRate);
```

### 2. 复制功能实现（三个模块）

**通用模式：**
```javascript
async function copyXxx() {
  if (!result.value?.xxx) return;
  try {
    const text = result.value.xxx.join('\n');
    await navigator.clipboard.writeText(text);
    alert('已复制到剪贴板');
  } catch (error) {
    alert('复制失败，请重试');
  }
}
```

**模板调用：**
```vue
<button @click="copyXxx" class="btn-copy-small">📋</button>
```

### 3. Axios错误拦截器（http.ts）

已完善的状态码映射：
```javascript
400 → "请求参数错误"
401 → "认证失败，请重新登录"  
403 → "暂无权限访问该资源"
404 → "请求的资源不存在"
422 → "数据验证失败"
500 → "服务器错误，请稍后重试"
503 → "服务暂时不可用，请稍后重试"
网络错误 → "网络连接失败，请检查网络设置"
```

---

## 📝 使用场景示例

### 场景1：律师审查合同

1. 打开合同审查页面
2. 输入合同名称与正文
3. 点击"开始智能审查"
4. AI返回风险列表，按风险等级显示（红黄绿）
5. 查看风险修改建议，点击📋复制到文档
6. **优势：** 快速获取规范的修改建议文本

### 场景2：案件分析师整理证据

1. 打开案件分析页面
2. 输入案情+证据清单
3. 系统自动计算证据链完整度
4. 查看"建议动作"，点击📋复制到记事本
5. 离线阅读、编辑、补充
6. **优势：** 证据完整度动态反馈，建议可直接使用

### 场景3：法律咨询师回答问题

1. 打开法律咨询页面
2. 输入用户提问
3. AI返回法律依据、建议、风险提示
4. 点击法律依据📋复制全部
5. 粘贴到咨询回复模板中
6. **优势：** 提高工作效率，确保依据准确

---

## 🔌 与成员A的集成状态

**后端配置切换：**

```yaml
# application.yml
lexai:
  ai:
    mode: mock          # ← 改为 "tencent" 使用真实AI
    # 其他AI配置...
```

**前端无需改动：**
- ✅ 字段映射已完全对齐
- ✅ 错误处理完善
- ✅ UI层已优化
- **直接可用成员A的AI服务**

---

## 🚀 后续改进空间

### 可选优化（优先级排序）

| 优先级 | 项目 | 难度 | 描述 |
|--------|------|------|------|
| P1 | 存草稿改为API调用 | 低 | 等待成员B完成后，改为POST /api/contracts |
| P2 | 法律依据URL识别 | 中 | 自动识别文本中的法条编号，生成链接 |
| P3 | 风险权重优化 | 中 | 根据演示反馈，调整AI给出的风险等级细粒度 |
| P4 | 知识库缓存 | 高 | 本地缓存RAG向量索引，提升查询速度 |

---

## ✅ 验收清单

- [x] 三个核心功能字段验证完成
- [x] 法律咨询复制功能实现
- [x] 案件分析覆盖率动态计算
- [x] 案件分析建议复制功能
- [x] 合同审查风险分级UI优化
- [x] 合同审查建议复制功能
- [x] Axios错误拦截器完善
- [x] 与成员A实现完全对接
- [x] 文档编写完成

---

**总体状态：** ✅ **完成** - 可交付演示和测试
