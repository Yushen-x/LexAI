<template>
  <div class="page-container">
    <!-- Presets -->
    <div class="presets-row mb-6">
      <span class="text-sm text-muted mr-2">快捷选填:</span>
      <button
        v-for="preset in consultationPresets"
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
            <h3 class="card-title">法律咨询分析</h3>
            <p class="text-muted text-sm mt-1">描述您遇到的法律问题及相关事实</p>
          </div>
          <span class="badge badge-success">系统就绪</span>
        </div>

        <div class="form-container mt-6">
          <div class="form-group">
            <label for="question" class="form-label">咨询问题</label>
            <textarea
              id="question"
              v-model="form.question"
              class="form-textarea"
              placeholder="例如：公司未签劳动合同且拖欠工资，我应该如何维权？"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="facts" class="form-label">补充事实 (逗号分隔)</label>
            <input
              id="facts"
              v-model="factsInput"
              class="form-input"
              placeholder="例如：工作满 6 个月，未缴社保，有聊天记录"
            />
          </div>

          <div v-if="normalizedFacts.length" class="tags-container mb-4 flex flex-wrap gap-2">
            <span v-for="item in normalizedFacts" :key="item" class="badge text-xs" style="background:#f1f5f9;color:#475569;border:1px solid #cbd5e1;">
              {{ item }}
            </span>
          </div>

          <div class="form-actions mt-6 flex gap-4">
            <button class="btn btn-primary" :disabled="loading" @click="handleSubmit">
              {{ loading ? '智能推演中...' : '开始分析' }}
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
              <h3 class="card-title">专业分析报告</h3>
              <p class="text-muted text-sm mt-1">识别领域：<strong style="color:var(--primary);">{{ result.category }}</strong></p>
            </div>
            <span class="badge badge-primary">已生成</span>
          </div>

          <!-- Summary Metric Row -->
          <div class="metrics-row mb-6">
            <div class="metric-item">
              <div class="metric-label">相关法条</div>
              <div class="metric-value">{{ result.legalBasis.length }}<span class="metric-unit">项</span></div>
            </div>
            <div class="metric-item">
              <div class="metric-label">应对建议</div>
              <div class="metric-value text-success">{{ result.recommendations.length }}<span class="metric-unit">条</span></div>
            </div>
            <div class="metric-item">
              <div class="metric-label">风险点提示</div>
              <div class="metric-value text-danger">{{ result.riskAlerts.length }}<span class="metric-unit">个</span></div>
            </div>
          </div>

          <!-- Detailed Columns -->
          <div class="result-grid">
            <div class="result-box">
              <div class="flex items-center justify-between mb-2">
                <h4 class="result-box-title text-primary">法律依据</h4>
                <button 
                  type="button"
                  class="btn-copy-small text-xs"
                  @click="copyLegalBasis"
                  title="复制全部依据"
                >
                  📋
                </button>
              </div>
              <div class="basis-list">
                <div v-for="item in structuredLegalBasis" :key="item.raw" class="basis-item">
                  <div class="basis-head">
                    <span class="basis-law">{{ item.law }}</span>
                    <span v-if="item.article" class="basis-article">{{ item.article }}</span>
                  </div>
                  <div class="basis-content text-muted text-sm">{{ item.content }}</div>
                </div>
              </div>
            </div>

            <div class="result-box">
              <h4 class="result-box-title text-success">建议路径</h4>
              <ul class="clean-list">
                <li v-for="item in result.recommendations" :key="item">{{ item }}</li>
              </ul>
            </div>

            <div class="result-box box-danger">
              <h4 class="result-box-title text-danger">风险提示</h4>
              <ul class="clean-list">
                <li v-for="item in result.riskAlerts" :key="item">{{ item }}</li>
              </ul>
            </div>

            <div class="result-box box-warning">
              <h4 class="result-box-title text-warning">建议补充材料</h4>
              <ul class="clean-list">
                <li v-for="item in suggestedMaterials" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">AI</div>
          <h3 class="empty-title">等待咨询输入</h3>
          <p class="empty-desc text-muted">在左侧输入您遇到的法律问题与相关事实，LexAI 智能体将为您生成专业的法律推演方案与操作建议。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { submitConsultation } from '@/shared/api/legal';
import type { ConsultationResponse } from '@/shared/types/legal';
import { toast } from '@/shared/ui/toast';

const form = reactive({
  question: '',
  facts: [] as string[]
});

const factsInput = ref('');
const result = ref<ConsultationResponse | null>(null);
const loading = ref(false);

const consultationPresets = [
  {
    title: '劳动报酬争议',
    question: '公司未与我签订劳动合同，并拖欠两个月工资，我应当如何主张权利？',
    facts: ['已入职6个月', '存在微信工作安排记录', '工资通过个人转账发放']
  },
  {
    title: '合同违约处理',
    question: '合作方未按约定时间交付项目成果，我方是否可以解除合同并要求赔偿？',
    facts: ['合同约定明确交付时间', '已有催告记录', '项目延期导致额外损失']
  },
  {
    title: '婚姻家事咨询',
    question: '离婚后对孩子抚养权和抚养费有争议，应该优先准备哪些材料？',
    facts: ['孩子长期由一方照顾', '双方经济状况差异较大', '已有沟通记录']
  }
];

function normalizeFacts(raw: string) {
  return raw
    .split(/[，,、；;]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

const normalizedFacts = computed(() => normalizeFacts(factsInput.value));

const suggestedMaterials = computed(() => {
  const category = result.value?.category ?? '';

  if (category.includes('劳动')) {
    return ['劳动关系证明材料', '工资发放记录', '沟通记录与考勤记录'];
  }

  if (category.includes('合同')) {
    return ['合同文本及补充协议', '履约催告记录', '损失证明材料'];
  }

  if (category.includes('婚姻')) {
    return ['日常照料证明', '收入与居住情况材料', '既往沟通记录'];
  }

  return ['完整主体身份材料', '关键事实客观证明', '与争议直接相关的往来记录'];
});

function parseLegalBasis(raw: string) {
  const normalized = raw.trim();
  // Common patterns: 《xxx法》第N条：内容 / xxx法 第N条 内容 / 纯文本
  const m = normalized.match(
    /^(?<law>《[^》]+》|[^第:：]{2,30}?(?:法|条例|规定))?\s*(?<article>第[^:：\s]{1,12}条)?\s*[:：]?\s*(?<content>.*)$/
  );
  const law = (m?.groups?.law || '相关依据').trim();
  const article = (m?.groups?.article || '').trim();
  const content = (m?.groups?.content || normalized).trim();
  return { raw: normalized, law, article, content };
}

const structuredLegalBasis = computed(() => {
  const items = result.value?.legalBasis ?? [];
  return items.map(parseLegalBasis);
});

function applyPreset(preset: { question: string; facts: string[] }) {
  form.question = preset.question;
  factsInput.value = preset.facts.join('，');
  result.value = null;
}

async function copyLegalBasis() {
  if (!result.value?.legalBasis) return;
  try {
    const text = result.value.legalBasis.join('\n');
    await navigator.clipboard.writeText(text);
    toast('法律依据已复制到剪贴板', 'success');
  } catch (error) {
    console.error('复制失败:', error);
    toast('复制失败，请重试', 'error');
  }
}

async function handleSubmit() {
  loading.value = true;
  form.facts = normalizedFacts.value;

  try {
    result.value = await submitConsultation({
      question: form.question,
      facts: form.facts
    });
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.question = '';
  form.facts = [];
  factsInput.value = '';
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
.border-b { border-bottom: 1px solid var(--border-light); }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--primary); }
.text-success { color: var(--success); }
.text-danger { color: var(--danger); }
.text-warning { color: var(--warning); }
.gap-2 { gap: 0.5rem; }
.gap-4 { gap: 1rem; }
.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }

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

.basis-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.basis-item {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  background: var(--bg-surface);
}

.basis-head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.35rem;
}

.basis-law {
  color: var(--text-strong);
  font-weight: 600;
}

.basis-article {
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  background: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--border-light);
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

.result-box-title {
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
  font-size: 1.5rem;
  font-weight: 700;
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
