<template>
  <div class="page-container">
    <!-- Presets -->
    <div class="presets-row mb-6">
      <span class="text-sm text-muted mr-2">典型案由:</span>
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
            <h3 class="card-title">案情与证据梳理</h3>
            <p class="text-muted text-sm mt-1">输入案件事实与现有证据，进行深度剖析</p>
          </div>
          <span class="badge badge-success">分析引擎空闲</span>
        </div>

        <div class="form-container mt-6">
          <div class="form-group">
            <label for="summary" class="form-label">案情描述</label>
            <textarea
              id="summary"
              v-model="form.caseSummary"
              class="form-textarea lg-textarea"
              placeholder="请描述案件背景、双方主要争议点及履约经过..."
            ></textarea>
          </div>

          <div class="form-group">
            <label for="evidencePoints" class="form-label">已掌握证据 (逗号分隔)</label>
            <input
              id="evidencePoints"
              v-model="evidenceInput"
              class="form-input"
              placeholder="例如：合同文本、付款凭证、微信聊天记录"
            />
          </div>

          <div v-if="normalizedEvidence.length" class="tags-container mb-4 flex flex-wrap gap-2">
            <span v-for="item in normalizedEvidence" :key="item" class="badge text-xs" style="background:#f1f5f9;color:#475569;border:1px solid #cbd5e1;">
              <i class="icon-file mr-1"></i> {{ item }}
            </span>
          </div>

          <div class="form-actions mt-6 flex gap-4">
            <button class="btn btn-primary" :disabled="loading" @click="handleSubmit">
              {{ loading ? '深度解析中...' : '生成案件画像' }}
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
              <h3 class="card-title">案件结构化画像</h3>
              <p class="text-muted text-sm mt-1">证据链完整度估算：<strong class="text-primary">{{ evidenceCoverage }}%</strong></p>
            </div>
            <span class="badge badge-primary">画像完成</span>
          </div>

          <!-- Summary Metric Row -->
          <div class="metrics-row mb-6">
            <div class="metric-item">
              <div class="metric-label">关键事实</div>
              <div class="metric-value">{{ result.keyFacts.length }}<span class="metric-unit">项</span></div>
            </div>
            <div class="metric-item">
              <div class="metric-label">争议焦点</div>
              <div class="metric-value text-warning">{{ result.disputedIssues.length }}<span class="metric-unit">个</span></div>
            </div>
            <div class="metric-item">
              <div class="metric-label">证据缺口</div>
              <div class="metric-value text-danger">{{ result.evidenceGaps.length }}<span class="metric-unit">处</span></div>
            </div>
          </div>

          <!-- Detailed Columns -->
          <div class="result-grid">
            <div class="result-box">
              <h4 class="result-box-title text-primary">关键事实提取</h4>
              <ul class="clean-list">
                <li v-for="item in result.keyFacts" :key="item">{{ item }}</li>
              </ul>
            </div>

            <div class="result-box box-warning">
              <h4 class="result-box-title text-warning">核心争议焦点</h4>
              <ul class="clean-list">
                <li v-for="item in result.disputedIssues" :key="item">{{ item }}</li>
              </ul>
            </div>

            <div class="result-box box-danger">
              <h4 class="result-box-title text-danger">证据缺口与薄弱点</h4>
              <ul class="clean-list">
                <li v-for="item in result.evidenceGaps" :key="item">{{ item }}</li>
              </ul>
            </div>

            <div class="result-box box-success">
              <div class="flex items-center justify-between mb-2">
                <h4 class="result-box-title text-success">后续建议动作</h4>
                <button 
                  type="button"
                  class="btn-copy-small text-xs"
                  @click="copySuggestedActions"
                  title="复制全部建议"
                >
                  📋
                </button>
              </div>
              <ul class="clean-list">
                <li v-for="item in result.suggestedActions" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <h3 class="empty-title">等待案件输入</h3>
          <p class="empty-desc text-muted">输入案情描述与证据清单，系统将自动梳理争议焦点、评估证据链条并提供专业画像。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { submitCaseAnalysis } from '@/shared/api/legal';
import type { CaseAnalysisResponse } from '@/shared/types/legal';
import { toast } from '@/shared/ui/toast';

const form = reactive({
  caseSummary: '',
  evidencePoints: [] as string[]
});

const evidenceInput = ref('');
const result = ref<CaseAnalysisResponse | null>(null);
const loading = ref(false);

const presets = [
  {
    title: '服务合同违约',
    summary:
      '甲乙双方签订技术服务合同，约定乙方在两个月内完成系统交付。现乙方延期一个月仍未完成主要功能，甲方已多次催告并产生额外运营损失，双方就是否解除合同及赔偿范围发生争议。',
    evidence: ['技术服务合同', '催告邮件', '会议纪要', '付款记录']
  },
  {
    title: '劳动关系认定',
    summary:
      '申请人未签订书面劳动合同，但长期接受公司管理并按月领取报酬。双方因拖欠工资和未缴社保发生争议，公司否认劳动关系。',
    evidence: ['转账记录', '微信工作群聊天记录', '钉钉打卡截图', '工作安排群通知']
  }
];

function normalizeEvidence(raw: string) {
  return raw
    .split(/[，,、；;]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

const normalizedEvidence = computed(() => normalizeEvidence(evidenceInput.value));

const evidenceCoverage = computed(() => {
  if (!result.value) {
    // 结果未返回时，仅按用户已填证据做一个保守估算
    const present = normalizedEvidence.value.length;
    return Math.min(95, Math.round(30 + present * 15));
  }

  // 覆盖率 = 已掌握证据 /（已掌握证据 + AI识别缺口）
  const present = normalizedEvidence.value.length;
  const gaps = result.value.evidenceGaps?.length ?? 0;
  const denom = present + gaps;
  if (denom <= 0) return 0;
  return Math.min(100, Math.round((present / denom) * 100));
});

function applyPreset(preset: { summary: string; evidence: string[] }) {
  form.caseSummary = preset.summary;
  evidenceInput.value = preset.evidence.join('，');
  result.value = null;
}

async function copySuggestedActions() {
  if (!result.value?.suggestedActions) return;
  try {
    const text = result.value.suggestedActions.join('\n');
    await navigator.clipboard.writeText(text);
    toast('建议动作已复制到剪贴板', 'success');
  } catch (error) {
    console.error('复制失败:', error);
    toast('复制失败，请重试', 'error');
  }
}

async function handleSubmit() {
  loading.value = true;
  form.evidencePoints = normalizedEvidence.value;

  try {
    result.value = await submitCaseAnalysis({
      caseSummary: form.caseSummary,
      evidencePoints: form.evidencePoints
    });
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.caseSummary = '';
  form.evidencePoints = [];
  evidenceInput.value = '';
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
.mr-1 { margin-right: 0.25rem; }
.mr-2 { margin-right: 0.5rem; }
.pb-4 { padding-bottom: 1rem; }
.mb-4 { margin-bottom: 1rem; }
.mt-1 { margin-top: 0.25rem; }
.border-b { border-bottom: 1px solid var(--border-light); }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--primary); }
.text-warning { color: var(--warning); }
.text-danger { color: var(--danger); }
.text-success { color: var(--success); }
.gap-2 { gap: 0.5rem; }
.gap-4 { gap: 1rem; }
.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }

.lg-textarea {
  min-height: 160px;
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
}

.metric-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.25rem;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
}

.metric-unit {
  font-size: 0.75rem;
  font-weight: normal;
  color: var(--text-muted);
  margin-left: 0.25rem;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.result-box {
  background-color: var(--bg-app);
  padding: 1.25rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.box-danger {
  background-color: #fef2f2;
  border-color: #fecaca;
}

.box-warning {
  background-color: #fffbeb;
  border-color: #fde68a;
}

.box-success {
  background-color: #f0fdf4;
  border-color: #bbf7d0;
}

.result-box-title {
  font-size: 0.875rem;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-copy-small {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.btn-copy-small:hover {
  opacity: 1;
}

.items-center {
  align-items: center;
}

.justify-between {
  justify-content: space-between;
}

.mb-2 {
  margin-bottom: 0.5rem;
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
  .result-grid {
    grid-template-columns: 1fr;
  }
  .metrics-row {
    flex-direction: column;
  }
}
</style>
