<template>
  <div class="page-container">
    <!-- Mode banner: 一眼区分「合同流程审查」与「匿名 AI 试用」 -->
    <div class="mode-banner mb-6" :class="linkedContract ? 'mode-banner--linked' : 'mode-banner--trial'">
      <div class="mode-banner__main">
        <div class="mode-banner__head">
          <span class="mode-banner__tag">
            {{ linkedContract ? '合同流程审查' : 'AI 试用模式' }}
          </span>
          <span class="mode-banner__desc">
            {{
              linkedContract
                ? '当前正在为台账内的一份合同执行 AI + 人工双重审查，结果将自动回写合同档案与待办任务。'
                : '当前为匿名试用，可粘贴任意条款体验 AI 风险识别，但不会保存到合同台账，也不会生成待办。'
            }}
          </span>
        </div>
        <div v-if="linkedContract" class="mode-banner__contract">
          <div class="mode-banner__contract-line">
            <strong>{{ linkedContract.contractNo }}</strong> · {{ linkedContract.name }}
          </div>
          <div class="mode-banner__contract-meta">
            <span>{{ linkedContract.contractType }}</span>
            <span>·</span>
            <span>{{ linkedContract.partyA || '甲方未填' }} ↔ {{ linkedContract.partyB || '乙方未填' }}</span>
            <span>·</span>
            <span>状态：{{ contractStatusLabel(linkedContract.status) }}</span>
          </div>
        </div>
      </div>
      <div class="mode-banner__actions">
        <button v-if="linkedContract" type="button" class="btn btn-secondary text-sm" @click="openLedgerDetail">
          返回台账详情
        </button>
        <button v-else type="button" class="btn btn-secondary text-sm" @click="goLedger">
          去合同台账新建合同
        </button>
      </div>
    </div>

    <!-- Presets -->
    <div class="presets-row mb-6">
      <span class="text-sm text-muted mr-2">示例合同:</span>
      <button
        v-for="preset in presets"
        :key="preset.title"
        class="badge preset-chip"
        type="button"
        @click="applyPreset(preset)"
      >
        {{ preset.title }}
      </button>
    </div>

    <div class="grid-layout">
      <!-- Input Section -->
      <div class="card input-card">
        <div class="card-header pb-4 border-b">
          <div>
            <h3 class="card-title">智能合同审查</h3>
            <p class="text-muted text-sm mt-1">
              {{
                linkedContract
                  ? '为该合同执行 AI 风险识别 + 人工复核结论，并把结论同步到台账与待办。'
                  : '粘贴合同正文体验 AI 风险识别，结果仅在当前页面展示。'
              }}
            </p>
          </div>
          <span class="badge badge-success">引擎空闲</span>
        </div>

        <div class="form-container mt-6">
          <!-- 合同档案模式（从台账跳转过来）：直接显示已起草的正文，不要再让用户重输 -->
          <template v-if="linkedContract">
            <div class="form-group">
              <label class="form-label">合同名称</label>
              <input :value="form.contractTitle" class="form-input" readonly />
            </div>

            <div class="form-group">
              <div class="flex items-center justify-between mb-1">
                <label class="form-label">合同正文（来自合同档案）</label>
                <div class="flex gap-2">
                  <button v-if="!editLinkedContent" type="button" class="btn-link text-xs" @click="expandLinkedContent = !expandLinkedContent">
                    {{ expandLinkedContent ? '收起' : '展开完整正文' }}
                  </button>
                  <button type="button" class="btn-link text-xs" @click="toggleEditLinkedContent">
                    {{ editLinkedContent ? '✕ 取消修改' : '✎ 修改正文' }}
                  </button>
                </div>
              </div>
              <textarea
                v-if="editLinkedContent"
                v-model="form.contractContent"
                class="form-textarea lg-textarea"
              ></textarea>
              <div v-else class="contract-preview" :class="{ 'is-expanded': expandLinkedContent }">
                <pre class="contract-preview__body">{{ form.contractContent || '（合同档案中正文为空，请先到合同台账中补全）' }}</pre>
                <div v-if="!expandLinkedContent && form.contractContent && form.contractContent.length > 480" class="contract-preview__fade"></div>
              </div>
              <p class="text-xs text-muted mt-1">
                正文已从合同档案 <strong>{{ linkedContract.contractNo }}</strong> 自动加载，AI 将基于此正文执行风险审查。
              </p>
            </div>

            <div class="form-actions mt-6 flex gap-4">
              <button class="btn btn-primary" :disabled="loading" @click="handleSubmit">
                <span v-if="loading" class="mr-2">⟳</span>
                {{ loading ? '深度审查中...' : '对该合同执行 AI 审查' }}
              </button>
              <button class="btn btn-secondary" type="button" @click="openLedgerDetail">
                返回合同台账
              </button>
            </div>
          </template>

          <!-- AI 试用模式：手动粘贴 -->
          <template v-else>
            <div class="form-group">
              <label for="title" class="form-label">合同名称</label>
              <input
                id="title"
                v-model="form.contractTitle"
                class="form-input"
                placeholder="例如：技术开发服务合同"
              />
            </div>

            <div class="form-group">
              <label for="contractContent" class="form-label">合同正文</label>
              <textarea
                id="contractContent"
                v-model="form.contractContent"
                class="form-textarea lg-textarea"
                placeholder="请在此粘贴完整的合同原文内容..."
              ></textarea>
            </div>

            <div class="form-actions mt-6 flex gap-4">
              <button class="btn btn-primary" :disabled="loading" @click="handleSubmit">
                <span v-if="loading" class="mr-2">⟳</span>
                {{ loading ? '深度审查中...' : '开始智能审查' }}
              </button>
              <button class="btn btn-secondary" type="button" @click="resetForm">清空内容</button>
            </div>
          </template>
        </div>
      </div>

      <!-- Result Section -->
      <div class="card result-card" :class="{ 'bg-soft': !result && !loading }">
        <AiThinkingPanel
          v-if="loading"
          subtitle="腾讯混元 hunyuan-lite + 得理法律法规检索 + 本地 RAG 知识库 + 二轮自检"
          :steps="[
            '解析合同正文与关键条款',
            '调用得理法规检索（queryListLaw）',
            '本地知识库 TF-IDF 召回相似条款',
            '混元大模型识别风险 / 缺失条款',
            '二轮自检（ReAct）：剔除幻觉 / 校准 confidence',
            '结构化输出风险等级与修改建议'
          ]"
          :step-interval-ms="2000"
        />
        <div v-if="result" class="result-content fade-in">
          <div class="card-header border-b pb-4 mb-6">
            <div>
              <h3 class="card-title">合同审查风控报告</h3>
              <p class="text-muted text-sm mt-1">
                本次审查文档：<strong class="text-primary">{{ form.contractTitle || '未命名合同' }}</strong>
                <span class="ml-2 self-check-tag" title="启用了二轮自检（ReAct 模式）">· 二轮自检 ✓</span>
              </p>
            </div>
            <div class="flex gap-2 items-center">
              <ConfidenceBadge :value="result.confidence" />
              <span class="badge badge-warning">审查完成</span>
            </div>
          </div>

          <!-- Summary Metric Row -->
          <div class="metrics-row mb-6">
            <div class="metric-item score-card">
              <div class="metric-label">整体健康度</div>
              <div class="metric-value flex items-baseline" :class="getScoreColor(reviewScore)">
                {{ reviewScore }}
                <span class="text-sm font-normal text-muted ml-1">分</span>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-label">高风险条款</div>
              <div class="metric-value text-danger">{{ riskCount.high }}<span class="metric-unit">项</span></div>
            </div>
            <div class="metric-item">
              <div class="metric-label">中风险条款</div>
              <div class="metric-value text-warning">{{ riskCount.medium }}<span class="metric-unit">项</span></div>
            </div>
            <div class="metric-item">
              <div class="metric-label">严重缺失项</div>
              <div class="metric-value" style="color:var(--text-strong);">{{ result.missingClauses.length }}<span class="metric-unit">项</span></div>
            </div>
          </div>

          <!-- Detailed Sections -->
          <div class="report-sections">
            <section class="report-section">
              <h4 class="section-heading text-primary">审查摘要</h4>
              <p class="summary-text">
                <CitedText :text="result.summary" :context="result.retrievalContext" @cite-click="onCiteClick" />
              </p>
            </section>

            <section v-if="result.missingClauses.length" class="report-section mt-6">
              <h4 class="section-heading" style="color:var(--text-strong);">缺失核心条款</h4>
              <div class="flex flex-wrap gap-2 mt-3">
                <span v-for="item in result.missingClauses" :key="item" class="badge" style="background:#f1f5f9;color:#334155;border:1px dashed #cbd5e1;">
                  {{ item }}
                </span>
              </div>
            </section>

            <section v-if="result.risks.length" class="report-section mt-6">
              <h4 class="section-heading mb-4 text-primary">检出风险清单</h4>
              <div class="risk-list flex flex-col gap-4">
                <div 
                  v-for="(item, idx) in result.risks" 
                  :key="`${item.level}-${idx}`" 
                  class="risk-item"
                  :class="`risk-item-${item.level.toLowerCase()}`"
                >
                  <div class="risk-item-header">
                    <span class="badge risk-badge" :class="getRiskBadgeClass(item.level)">{{ getRiskLevelLabel(item.level) }}</span>
                    <h5 class="risk-clause">原条款：{{ item.clause }}</h5>
                  </div>
                  <div class="risk-content mt-3">
                    <div class="risk-block">
                      <strong class="text-muted text-xs uppercase tracking-wider block mb-1">风险释明</strong>
                      <p class="text-sm">
                        <CitedText :text="item.issue" :context="result.retrievalContext" @cite-click="onCiteClick" />
                      </p>
                    </div>
                  <div class="risk-block mt-3">
                    <strong class="text-success text-xs uppercase tracking-wider block mb-1">修改建议</strong>
                    <div class="flex items-start justify-between gap-2">
                      <p class="text-sm flex-1">
                        <CitedText :text="item.suggestion" :context="result.retrievalContext" @cite-click="onCiteClick" />
                      </p>
                      <button 
                        type="button"
                        class="btn-copy text-xs whitespace-nowrap ml-2"
                        @click="copyRiskSuggestion(item.suggestion)"
                        title="复制建议"
                      >
                        📋 复制
                      </button>
                    </div>
                  </div>
                  </div>
                </div>
              </div>
            </section>

            <AiTracePanel
              :laws="result.retrievalContext?.laws ?? []"
              :cases="result.retrievalContext?.cases ?? []"
              :knowledge="result.retrievalContext?.knowledge ?? []"
              :input-desc="form.contractTitle || '合同正文'"
            />

            <section v-if="hasAnyRetrieval" class="report-section mt-6">
              <h4 class="section-heading text-primary">检索原文（用于复核 · 点击 [L#][K#] 标签可定位）</h4>
              <div class="retrieval-grid mt-3">
                <RagSourceList
                  kind="law"
                  chip-prefix="L"
                  title-prefix="得理 · 法律法规"
                  :items="result.retrievalContext?.laws ?? []"
                  :highlighted-index="highlightLawIdx"
                />
                <RagSourceList
                  kind="kb"
                  chip-prefix="K"
                  title-prefix="本地 RAG 知识库"
                  :items="result.retrievalContext?.knowledge ?? []"
                  :highlighted-index="highlightKbIdx"
                />
              </div>
            </section>

            <section v-if="currentContractId !== null" class="report-section mt-6">
              <h4 class="section-heading text-primary">人工复核与决策</h4>
              <p class="text-sm text-muted mt-1">
                决策保存后，台账中的最近一次审查结果与待办任务都会自动同步：
                <span class="text-success">通过 → 待办标记完成</span>、
                <span class="text-warning">退回修改 → 待办标记驳回</span>。
              </p>
              <div class="manual-review-grid mt-3">
                <div class="form-group">
                  <label for="reviewDecision" class="form-label">处理结论</label>
                  <select id="reviewDecision" v-model="reviewDecision" class="form-input">
                    <option v-for="option in reviewDecisionOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </div>
                <div class="form-group manual-review-full">
                  <label for="reviewerOpinion" class="form-label">人工意见</label>
                  <textarea
                    id="reviewerOpinion"
                    v-model="reviewerOpinion"
                    class="form-textarea"
                    placeholder="例如：AI 已识别主要风险，但违约责任还需要结合项目背景再细化。"
                  ></textarea>
                </div>
              </div>
              <div class="manual-review-actions mt-3">
                <button
                  type="button"
                  class="btn btn-primary"
                  :disabled="savingReviewFeedback"
                  @click="saveManualReview"
                >
                  {{ savingReviewFeedback ? '保存中...' : '保存决策并同步待办' }}
                </button>
              </div>
            </section>

            <section v-else class="report-section mt-6 trial-hint">
              <h4 class="section-heading">想保存人工意见 / 同步待办？</h4>
              <p class="text-sm text-muted mt-1">
                试用模式不会保存任何数据。请到 <a href="javascript:void(0)" class="text-primary" @click="goLedger">合同台账</a>
                新建合同后，再从台账详情或合同起草页跳过来执行完整审查。
              </p>
            </section>
          </div>
        </div>

        <div v-else-if="!loading" class="empty-state">
          <div class="empty-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <h3 class="empty-title">等待合同输入</h3>
          <p class="empty-desc text-muted">在左侧输入合同名称与正文内容，LexAI 将运用深度学习模型为您逐条审查法律风险并提供修改建议。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getContract, updateContract, updateContractReview } from '@/shared/api/contracts';
import { submitContractReview } from '@/shared/api/legal';
import type { ContractItem, ContractLatestReview, ContractReviewDecision, ContractStatus } from '@/shared/types/contracts';
import type { ContractReviewResponse } from '@/shared/types/legal';
import { toast } from '@/shared/ui/toast';
import AiThinkingPanel from '@/shared/ui/AiThinkingPanel.vue';
import AiTracePanel from '@/shared/ui/AiTracePanel.vue';
import ConfidenceBadge from '@/shared/ui/ConfidenceBadge.vue';
import CitedText from '@/shared/ui/CitedText.vue';
import RagSourceList from '@/shared/ui/RagSourceList.vue';

const route = useRoute();
const router = useRouter();

const CONTRACT_STATUS_LABEL: Record<ContractStatus, string> = {
  DRAFT: '草稿',
  UNDER_REVIEW: '审查中',
  SIGNED: '已签署',
  IN_PROGRESS: '执行中',
  COMPLETED: '已完成',
  TERMINATED: '已终止'
};

function contractStatusLabel(status: ContractStatus): string {
  return CONTRACT_STATUS_LABEL[status] ?? status;
}

function goLedger() {
  router.push({ name: 'contractList' });
}
const form = reactive({
  contractTitle: '',
  contractContent: ''
});

const result = ref<ContractReviewResponse | null>(null);
const loading = ref(false);
const savingReviewFeedback = ref(false);
const currentContractId = ref<number | null>(null);
const linkedContract = ref<ContractItem | null>(null);
const reviewerOpinion = ref('');
const reviewDecision = ref<ContractReviewDecision>('PENDING_CONFIRMATION');
const expandLinkedContent = ref(false);
const editLinkedContent = ref(false);

function toggleEditLinkedContent() {
  editLinkedContent.value = !editLinkedContent.value;
  if (editLinkedContent.value) expandLinkedContent.value = true;
}

const presets = [
  {
    title: '技术服务合同',
    contractTitle: '技术服务合同',
    contractContent:
      '甲方委托乙方提供系统建设服务。合同未约定明确验收标准。甲方拥有单方解释权归甲方所有。合同对保密、争议解决、解除与终止未作明确约定。'
  },
  {
    title: '设备采购合同',
    contractTitle: '设备采购合同',
    contractContent:
      '乙方按甲方要求提供设备，合同约定交付时间但未明确验收方式及付款细则。若发生争议，双方另行协商处理。'
  }
];

const riskCount = computed(() => {
  const risks = result.value?.risks ?? [];
  return {
    high: risks.filter((item) => item.level === 'HIGH').length,
    medium: risks.filter((item) => item.level === 'MEDIUM').length,
    low: risks.filter((item) => item.level === 'LOW').length
  };
});

const reviewScore = computed(() => {
  const high = riskCount.value.high * 22;
  const medium = riskCount.value.medium * 12;
  const low = riskCount.value.low * 4;
  const missing = (result.value?.missingClauses.length ?? 0) * 7;
  return Math.max(42, 100 - high - medium - low - missing);
});

const hasAnyRetrieval = computed(() => {
  const r = result.value?.retrievalContext;
  if (!r) return false;
  return (r.laws?.length ?? 0) + (r.knowledge?.length ?? 0) > 0;
});

const highlightLawIdx = ref<number | null>(null);
const highlightKbIdx = ref<number | null>(null);

function onCiteClick(seg: { kind: string; index: number }) {
  if (seg.kind === 'law') highlightLawIdx.value = seg.index - 1;
  else if (seg.kind === 'kb') highlightKbIdx.value = seg.index - 1;
}

const reviewDecisionOptions: { value: ContractReviewDecision; label: string }[] = [
  { value: 'PENDING_CONFIRMATION', label: '待人工确认' },
  { value: 'NEEDS_REVISION', label: '退回修改' },
  { value: 'APPROVED', label: '审查通过' }
];

function getScoreColor(score: number) {
  if (score >= 90) return 'text-success';
  if (score >= 70) return 'text-warning';
  return 'text-danger';
}

function getRiskLevelLabel(level: string) {
  if (level === 'HIGH') return '高风险';
  if (level === 'MEDIUM') return '中风险';
  return '低风险';
}

function getRiskBadgeClass(level: string) {
  if (level === 'HIGH') return 'badge-danger';
  if (level === 'MEDIUM') return 'badge-warning';
  return 'badge-low';
}

async function copyRiskSuggestion(suggestion: string) {
  try {
    await navigator.clipboard.writeText(suggestion);
    toast('修改建议已复制到剪贴板', 'success');
  } catch (error) {
    console.error('复制失败:', error);
    toast('复制失败，请重试', 'error');
  }
}

async function loadLinkedContract(contractId: number) {
  const contract = await getContract(contractId);
  linkedContract.value = contract;
  currentContractId.value = contract.id;
  form.contractTitle = contract.name;
  form.contractContent = contract.content;
  hydrateLatestReview(contract.latestReview);
}

function hydrateLatestReview(latestReview: ContractLatestReview | null) {
  syncManualReview(latestReview);

  if (!latestReview) {
    result.value = null;
    return;
  }

  const hasStructuredReview = latestReview.summary || latestReview.risks.length || latestReview.missingClauses.length;
  if (!hasStructuredReview) {
    result.value = null;
    return;
  }

  result.value = {
    risks: latestReview.risks ?? [],
    missingClauses: latestReview.missingClauses ?? [],
    summary: latestReview.summary ?? '',
    confidence: null,
    retrievalContext: {
      laws: [],
      cases: [],
      knowledge: []
    }
  };
}

function syncManualReview(latestReview: ContractLatestReview | null) {
  reviewerOpinion.value = latestReview?.reviewerOpinion ?? '';
  reviewDecision.value = latestReview?.reviewDecision ?? 'PENDING_CONFIRMATION';
}

onMounted(async () => {
  const rawContractId = route.query.contractId;
  const contractId = typeof rawContractId === 'string' ? Number(rawContractId) : Number.NaN;
  if (Number.isInteger(contractId) && contractId > 0) {
    try {
      await loadLinkedContract(contractId);
      return;
    } catch (error) {
      console.error('加载合同详情失败:', error);
      toast('加载待审合同失败，请稍后重试', 'error');
    }
  }

  const pendingContent = sessionStorage.getItem('pendingContractContent');
  const pendingName = sessionStorage.getItem('pendingContractName');
  if (pendingContent && !form.contractContent.trim()) {
    form.contractContent = pendingContent;
  }
  if (pendingName && !form.contractTitle.trim()) {
    form.contractTitle = pendingName;
  }
  sessionStorage.removeItem('pendingContractContent');
  sessionStorage.removeItem('pendingContractName');
});

function applyPreset(preset: { contractTitle: string; contractContent: string }) {
  form.contractTitle = preset.contractTitle;
  form.contractContent = preset.contractContent;
  result.value = null;
  reviewerOpinion.value = '';
  reviewDecision.value = 'PENDING_CONFIRMATION';
}

async function handleSubmit() {
  if (!form.contractContent.trim()) {
    toast('请先输入合同正文内容', 'warning');
    return;
  }
  result.value = null;
  loading.value = true;

  try {
    if (currentContractId.value !== null && linkedContract.value) {
      linkedContract.value = await updateContract(currentContractId.value, {
        name: form.contractTitle.trim() || linkedContract.value.name,
        contractType: linkedContract.value.contractType,
        partyA: linkedContract.value.partyA,
        partyB: linkedContract.value.partyB,
        amount: linkedContract.value.amount,
        content: form.contractContent,
        source: linkedContract.value.source || undefined,
        status: 'UNDER_REVIEW'
      });
      form.contractTitle = linkedContract.value.name;
      form.contractContent = linkedContract.value.content;
    }

    result.value = await submitContractReview({
      contractTitle: form.contractTitle,
      contractContent: form.contractContent,
      contractId: currentContractId.value ?? undefined
    });

    if (currentContractId.value !== null) {
      linkedContract.value = await getContract(currentContractId.value);
      syncManualReview(linkedContract.value.latestReview);
    }
  } catch (error) {
    console.error('合同审查失败:', error);
    result.value = null;
    toast('合同审查失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function saveManualReview() {
  if (currentContractId.value === null) {
    toast('请先关联一份已落库合同', 'warning');
    return;
  }

  savingReviewFeedback.value = true;
  try {
    linkedContract.value = await updateContractReview(currentContractId.value, {
      reviewerOpinion: reviewerOpinion.value,
      reviewDecision: reviewDecision.value
    });
    syncManualReview(linkedContract.value.latestReview);
    toast('人工复核意见已保存', 'success');
  } catch (error) {
    console.error('保存人工复核意见失败:', error);
    toast('保存人工复核意见失败，请稍后重试', 'error');
  } finally {
    savingReviewFeedback.value = false;
  }
}

function openLedgerDetail() {
  if (currentContractId.value === null) {
    return;
  }
  router.push({
    name: 'contractList',
    query: { contractId: String(currentContractId.value) }
  });
}

function resetForm() {
  form.contractTitle = '';
  form.contractContent = '';
  result.value = null;
  currentContractId.value = null;
  linkedContract.value = null;
  reviewerOpinion.value = '';
  reviewDecision.value = 'PENDING_CONFIRMATION';
}
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.mb-6 { margin-bottom: 1.5rem; }
.mt-6 { margin-top: 1.5rem; }
.mr-2 { margin-right: 0.5rem; }
.pb-4 { padding-bottom: 1rem; }
.mb-4 { margin-bottom: 1rem; }
.mt-1 { margin-top: 0.25rem; }
.mt-3 { margin-top: 0.75rem; }
.mb-1 { margin-bottom: 0.25rem; }
.ml-1 { margin-left: 0.25rem; }
.border-b { border-bottom: 1px solid var(--border-light); }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--primary); }
.text-warning { color: var(--warning); }
.text-danger { color: var(--danger); }
.text-success { color: var(--success); }
.font-normal { font-weight: 400; }
.block { display: block; }
.uppercase { text-transform: uppercase; }
.tracking-wider { letter-spacing: 0.05em; }
.gap-2 { gap: 0.5rem; }
.gap-4 { gap: 1rem; }
.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-wrap { flex-wrap: wrap; }
.items-center { align-items: center; }
.items-baseline { align-items: baseline; }

.lg-textarea {
  min-height: 240px;
}

.presets-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.preset-chip {
  background-color: var(--bg-surface);
  color: var(--text-main);
  border: 1px solid var(--border-strong);
  cursor: pointer;
  transition: all 0.2s;
  font-weight: normal;
}

.preset-chip:hover {
  background-color: var(--primary-soft);
  color: var(--primary);
  border-color: var(--primary);
}

.grid-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: 1.5rem;
  align-items: start;
}
.grid-layout > * { min-width: 0; }

/* 桌面端：输入卡固定在视口顶部，结果卡可继续往下长，避免结果撑大整页后输入卡滚出视野 */
@media (min-width: 1024px) {
  .input-card {
    position: sticky;
    top: 1rem;
    align-self: start;
    max-height: calc(100vh - 2rem);
    overflow-y: auto;
  }
}

.result-card {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}
.result-card :deep(pre),
.result-card :deep(code) {
  white-space: pre-wrap;
  word-break: break-word;
}

.contract-preview {
  position: relative;
  background: var(--bg-app);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 0.75rem 1rem;
  max-height: 220px;
  overflow: hidden;
}
.contract-preview.is-expanded {
  max-height: 1000px;
  overflow-y: auto;
}
.contract-preview__body {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--text-main);
}
.contract-preview__fade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 60px;
  background: linear-gradient(to bottom, rgba(255,255,255,0), var(--bg-app));
  pointer-events: none;
}

.btn-link {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  padding: 0.15rem 0.35rem;
  border-radius: var(--radius-sm);
  font-weight: 500;
}
.btn-link:hover {
  background: var(--primary-soft);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-title {
  margin: 0;
  font-size: 1.125rem;
}

.bg-soft {
  background-color: var(--bg-surface);
}

.metrics-row {
  display: flex;
  gap: 1rem;
}

.metric-item {
  flex: 1;
  background-color: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 1rem;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
}

.metric-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
}

.metric-value {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
  margin-top: auto;
}

.metric-unit {
  font-size: 0.75rem;
  font-weight: normal;
  color: var(--text-muted);
  margin-left: 0.25rem;
}

.section-heading {
  font-size: 1rem;
  margin: 0;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-light);
}

.summary-text {
  font-size: 0.875rem;
  line-height: 1.6;
  margin-top: 0.75rem;
  color: var(--text-main);
}

.self-check-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 600;
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  color: #047857;
  border: 1px solid #6ee7b7;
}

.mode-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  background: var(--bg-surface);
}

.mode-banner--linked {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(37, 99, 235, 0.02));
  border-color: rgba(37, 99, 235, 0.35);
}

.mode-banner--trial {
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.10), rgba(234, 179, 8, 0.02));
  border-color: rgba(234, 179, 8, 0.45);
}

.mode-banner__main {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.mode-banner__head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.mode-banner__tag {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  color: var(--text-strong);
}

.mode-banner--linked .mode-banner__tag {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.35);
  color: var(--primary);
}

.mode-banner--trial .mode-banner__tag {
  background: rgba(234, 179, 8, 0.18);
  border-color: rgba(234, 179, 8, 0.5);
  color: #92400e;
}

.mode-banner__desc {
  font-size: 0.875rem;
  color: var(--text-main);
}

.mode-banner__contract {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-top: 0.25rem;
}

.mode-banner__contract-line {
  font-size: 0.95rem;
  color: var(--text-strong);
}

.mode-banner__contract-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.mode-banner__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.trial-hint {
  border: 1px dashed var(--border-light);
  background: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
}

.manual-review-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.manual-review-full {
  grid-column: 1 / -1;
}

.manual-review-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.retrieval-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.retrieval-card {
  background-color: var(--bg-app);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 1rem;
}

.retrieval-title {
  font-size: 0.875rem;
  margin: 0 0 0.75rem;
  color: var(--text-strong);
}

.clean-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.clean-list li {
  position: relative;
  padding-left: 1rem;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--text-main);
}

.clean-list li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: var(--text-muted);
}

.risk-item {
  background-color: var(--bg-app);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  border-left: 4px solid var(--border-strong);
}

.risk-item-high {
  border-left-color: var(--danger);
  background-color: #fef2f2;
}

.risk-item-medium {
  border-left-color: var(--warning);
  background-color: #fffbeb;
}

.risk-item-low {
  border-left-color: var(--success);
}

.risk-item-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.risk-clause {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-strong);
}

.badge-high {
  background-color: var(--danger);
  color: white;
}

.badge-medium {
  background-color: var(--warning);
  color: white;
}

.badge-low {
  background-color: var(--success);
  color: white;
}

.risk-block p {
  margin: 0;
  color: var(--text-main);
  line-height: 1.5;
}

.btn-copy {
  background-color: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--primary);
  padding: 0.375rem 0.75rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.75rem;
  transition: all 0.2s;
  font-weight: 500;
}

.btn-copy:hover {
  background-color: var(--primary);
  color: white;
}

.btn-copy:active {
  transform: scale(0.95);
}

.items-start {
  align-items: flex-start;
}

.whitespace-nowrap {
  white-space: nowrap;
}

.ml-2 {
  margin-left: 0.5rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  height: 100%;
  min-height: 400px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: var(--primary-soft);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.25rem;
}

.empty-title {
  font-size: 1.125rem;
  margin-bottom: 0.5rem;
}

.empty-desc {
  font-size: 0.875rem;
  max-width: 320px;
  line-height: 1.6;
}

.fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1024px) {
  .grid-layout {
    grid-template-columns: 1fr;
  }

  .retrieval-grid {
    grid-template-columns: 1fr;
  }

  .manual-review-grid {
    grid-template-columns: 1fr;
  }

  .mode-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .metrics-row {
    flex-direction: column;
  }
}
</style>
