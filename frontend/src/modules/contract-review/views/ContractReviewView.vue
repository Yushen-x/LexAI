<template>
  <div class="page-container">
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
            <p class="text-muted text-sm mt-1">上传或输入合同正文，一键识别法律风险</p>
          </div>
          <span class="badge badge-success">引擎空闲</span>
        </div>

        <div class="form-container mt-6">
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
        </div>
      </div>

      <!-- Result Section -->
      <div class="card result-card" :class="{ 'bg-soft': !result }">
        <div v-if="result" class="result-content fade-in">
          <div class="card-header border-b pb-4 mb-6">
            <div>
              <h3 class="card-title">合同审查风控报告</h3>
              <p class="text-muted text-sm mt-1">本次审查文档：<strong class="text-primary">{{ form.contractTitle || '未命名合同' }}</strong></p>
            </div>
            <span class="badge badge-warning">审查完成</span>
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
              <p class="summary-text">{{ result.summary }}</p>
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
                      <p class="text-sm">{{ item.issue }}</p>
                    </div>
                  <div class="risk-block mt-3">
                    <strong class="text-success text-xs uppercase tracking-wider block mb-1">修改建议</strong>
                    <div class="flex items-start justify-between gap-2">
                      <p class="text-sm flex-1">{{ item.suggestion }}</p>
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
          </div>
        </div>

        <div v-else class="empty-state">
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
import { submitContractReview } from '@/shared/api/legal';
import type { ContractReviewResponse } from '@/shared/types/legal';
import { toast } from '@/shared/ui/toast';

const form = reactive({
  contractTitle: '',
  contractContent: ''
});

const result = ref<ContractReviewResponse | null>(null);
const loading = ref(false);

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
  return 'badge-warning';
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

onMounted(() => {
  const pendingContent = sessionStorage.getItem('pendingContractContent');
  const pendingName = sessionStorage.getItem('pendingContractName');
  if (pendingContent && !form.contractContent.trim()) {
    form.contractContent = pendingContent;
  }
  if (pendingName && !form.contractTitle.trim()) {
    form.contractTitle = pendingName;
  }
});

function applyPreset(preset: { contractTitle: string; contractContent: string }) {
  form.contractTitle = preset.contractTitle;
  form.contractContent = preset.contractContent;
  result.value = null;
}

async function handleSubmit() {
  if (!form.contractContent.trim()) return;
  loading.value = true;

  try {
    result.value = await submitContractReview({
      contractTitle: form.contractTitle,
      contractContent: form.contractContent
    });
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.contractTitle = '';
  form.contractContent = '';
  result.value = null;
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
  grid-template-columns: 1fr 1.2fr;
  gap: 1.5rem;
  align-items: start;
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
}

@media (max-width: 640px) {
  .metrics-row {
    flex-direction: column;
  }
}
</style>
