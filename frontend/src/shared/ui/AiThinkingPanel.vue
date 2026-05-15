<template>
  <div class="ai-thinking" role="status" aria-live="polite">
    <div class="ai-thinking-header">
      <div class="brain-icon">
        <span class="pulse-dot"></span>
        <span class="pulse-dot"></span>
        <span class="pulse-dot"></span>
      </div>
      <div class="header-text">
        <div class="title">{{ title }}</div>
        <div class="subtitle">{{ subtitle }}</div>
      </div>
    </div>

    <div class="step-list">
      <div
        v-for="(step, idx) in steps"
        :key="step"
        class="step-item"
        :class="stepClass(idx)"
      >
        <span class="step-icon">
          <template v-if="idx < activeStep">✓</template>
          <template v-else-if="idx === activeStep"><span class="spinner" /></template>
          <template v-else>{{ idx + 1 }}</template>
        </span>
        <span class="step-text">{{ step }}</span>
        <span v-if="idx === activeStep" class="step-shimmer" />
      </div>
    </div>

    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
    </div>
    <div class="progress-meta">
      <span>{{ Math.min(activeStep + 1, steps.length) }} / {{ steps.length }} 阶段</span>
      <span>已用 {{ elapsedSeconds }}s</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';

interface Props {
  title?: string;
  subtitle?: string;
  steps?: string[];
  /** 步骤平均耗时（毫秒），驱动伪进度推进 */
  stepIntervalMs?: number;
}

const props = withDefaults(defineProps<Props>(), {
  title: 'AI 智能体推理中',
  subtitle: '腾讯混元大模型 + 得理法律检索 + 本地知识库 RAG',
  steps: () => [
    '解析问题与事实要素',
    '调用得理法规 / 类案接口',
    '本地知识库 TF-IDF 召回',
    '混元大模型综合推理',
    '结构化输出与风险标注',
  ],
  stepIntervalMs: 1400,
});

function pinAtLastStep() {
  if (props.steps.length === 0) return;
  activeStep.value = props.steps.length - 1;
}

defineExpose({ pinAtLastStep });

const activeStep = ref(0);
const elapsedSeconds = ref(0);
let stepTimer: number | null = null;
let elapsedTimer: number | null = null;

const progressPct = computed(() => {
  const ratio = (activeStep.value + 0.5) / props.steps.length;
  return Math.min(95, Math.round(ratio * 100));
});

function stepClass(idx: number) {
  if (idx < activeStep.value) return 'done';
  if (idx === activeStep.value) return 'active';
  return 'pending';
}

function start() {
  stop();
  activeStep.value = 0;
  elapsedSeconds.value = 0;
  stepTimer = window.setInterval(() => {
    if (activeStep.value < props.steps.length - 1) {
      activeStep.value += 1;
    }
  }, props.stepIntervalMs);
  elapsedTimer = window.setInterval(() => {
    elapsedSeconds.value += 1;
  }, 1000);
}

function stop() {
  if (stepTimer !== null) {
    clearInterval(stepTimer);
    stepTimer = null;
  }
  if (elapsedTimer !== null) {
    clearInterval(elapsedTimer);
    elapsedTimer = null;
  }
}

onMounted(start);
onUnmounted(stop);
watch(() => props.steps, start);
</script>

<style scoped>
.ai-thinking {
  border-radius: 14px;
  padding: 1.25rem 1.4rem;
  background: linear-gradient(135deg, #eef4ff 0%, #f5ecff 100%);
  border: 1px solid rgba(99, 102, 241, 0.2);
  box-shadow: 0 4px 18px rgba(79, 70, 229, 0.08);
  position: relative;
  overflow: hidden;
}

.ai-thinking-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.brain-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.35);
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  display: inline-block;
  animation: bounce 1.2s infinite ease-in-out;
}
.pulse-dot:nth-child(2) { animation-delay: 0.15s; }
.pulse-dot:nth-child(3) { animation-delay: 0.3s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.7; }
  40% { transform: scale(1.1); opacity: 1; }
}

.header-text .title {
  font-weight: 700;
  font-size: 1rem;
  color: #312e81;
}
.header-text .subtitle {
  margin-top: 2px;
  font-size: 0.78rem;
  color: #6b7280;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.8rem;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.45rem 0.6rem;
  border-radius: 8px;
  font-size: 0.85rem;
  position: relative;
  transition: all 0.25s;
}

.step-item.pending { color: #9ca3af; background: transparent; }
.step-item.active {
  color: #312e81;
  background: rgba(255, 255, 255, 0.85);
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.12);
}
.step-item.done { color: #059669; }

.step-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  background: #fff;
  color: inherit;
  border: 1.5px solid currentColor;
  flex-shrink: 0;
}

.step-item.done .step-icon {
  background: #10b981;
  color: #fff;
  border-color: #10b981;
}

.step-item.active .step-icon {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}

.spinner {
  width: 10px;
  height: 10px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.step-shimmer {
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background: linear-gradient(90deg, transparent, rgba(99,102,241,0.12), transparent);
  background-size: 200% 100%;
  animation: shimmer 1.6s infinite linear;
  pointer-events: none;
}
@keyframes shimmer {
  from { background-position: 200% 0; }
  to { background-position: -200% 0; }
}

.progress-bar {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.15);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  border-radius: 999px;
  transition: width 0.6s ease-out;
}
.progress-meta {
  margin-top: 0.4rem;
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  color: #6b7280;
}
</style>
