<template>
  <div class="ai-trace card" :class="{ collapsed: !expanded }">
    <header class="trace-header" @click="expanded = !expanded">
      <div class="left">
        <span class="trace-badge">AI 推理链路</span>
        <span class="trace-summary">
          {{ summaryText }}
        </span>
      </div>
      <div class="right">
        <span class="hint">{{ expanded ? '收起' : '展开' }}</span>
        <span class="caret" :class="{ open: expanded }">▾</span>
      </div>
    </header>

    <div v-show="expanded" class="trace-body">
      <div class="pipeline">
        <div class="node">
          <div class="node-icon">①</div>
          <div class="node-text">
            <div class="node-title">用户输入</div>
            <div class="node-desc">{{ inputDesc || '问题 / 事实 / 合同正文' }}</div>
          </div>
        </div>
        <div class="connector"></div>

        <div class="node" :class="{ filled: lawCount > 0 }">
          <div class="node-icon">②</div>
          <div class="node-text">
            <div class="node-title">得理 · 法律法规</div>
            <div class="node-desc">命中 {{ lawCount }} 条</div>
          </div>
        </div>
        <div class="connector"></div>

        <div class="node" :class="{ filled: caseCount > 0 }">
          <div class="node-icon">③</div>
          <div class="node-text">
            <div class="node-title">得理 · 类案检索</div>
            <div class="node-desc">命中 {{ caseCount }} 条</div>
          </div>
        </div>
        <div class="connector"></div>

        <div class="node" :class="{ filled: kbCount > 0 }">
          <div class="node-icon">④</div>
          <div class="node-text">
            <div class="node-title">本地 RAG 知识库</div>
            <div class="node-desc">召回 {{ kbCount }} 段</div>
          </div>
        </div>
        <div class="connector"></div>

        <div class="node llm">
          <div class="node-icon">⑤</div>
          <div class="node-text">
            <div class="node-title">腾讯混元大模型</div>
            <div class="node-desc">{{ modelTag }} · 结构化输出</div>
          </div>
        </div>
      </div>

      <div class="legend">
        <span class="dot dot-filled"></span><span>已采集</span>
        <span class="dot dot-empty"></span><span>未命中（兜底/跳过）</span>
        <span class="model-name">模型：{{ modelTag }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

interface Props {
  laws?: string[];
  cases?: string[];
  knowledge?: string[];
  inputDesc?: string;
  modelTag?: string;
  defaultExpanded?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  laws: () => [],
  cases: () => [],
  knowledge: () => [],
  inputDesc: '',
  modelTag: '腾讯混元 hunyuan-lite',
  defaultExpanded: true,
});

const expanded = ref(props.defaultExpanded);

const lawCount = computed(() => props.laws?.length ?? 0);
const caseCount = computed(() => props.cases?.length ?? 0);
const kbCount = computed(() => props.knowledge?.length ?? 0);

const summaryText = computed(() => {
  return `法规 ${lawCount.value} · 类案 ${caseCount.value} · 知识库 ${kbCount.value} · 大模型综合`;
});
</script>

<style scoped>
.ai-trace {
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f5f3ff 100%);
  padding: 0.85rem 1rem;
  margin-top: 1rem;
}

.trace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.left { display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap; }
.right { display: flex; align-items: center; gap: 0.4rem; color: #6b7280; font-size: 0.78rem; }

.trace-badge {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.trace-summary {
  font-size: 0.85rem;
  color: #4b5563;
}

.caret {
  display: inline-block;
  transition: transform 0.2s;
}
.caret.open { transform: rotate(180deg); }

.trace-body {
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px dashed rgba(99, 102, 241, 0.15);
}

.pipeline {
  display: flex;
  align-items: stretch;
  gap: 0;
  flex-wrap: wrap;
}

.node {
  flex: 1;
  min-width: 110px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 10px;
  padding: 0.55rem 0.7rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.78rem;
  color: #475569;
}

.node.filled {
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.4);
  color: #312e81;
}

.node.llm {
  background: linear-gradient(135deg, rgba(99,102,241,0.18), rgba(139,92,246,0.18));
  border-color: rgba(139, 92, 246, 0.5);
  color: #312e81;
  font-weight: 600;
}

.node-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #6366f1;
  color: #fff;
  font-size: 0.7rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.node-title { font-weight: 600; font-size: 0.78rem; }
.node-desc { font-size: 0.7rem; color: #6b7280; margin-top: 1px; }

.connector {
  width: 18px;
  height: 1.5px;
  background: linear-gradient(90deg, rgba(99,102,241,0.4), rgba(139,92,246,0.4));
  align-self: center;
  margin: 0 4px;
}

.legend {
  margin-top: 0.7rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.72rem;
  color: #6b7280;
  flex-wrap: wrap;
}
.legend .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.dot-filled { background: #6366f1; }
.dot-empty { background: rgba(148, 163, 184, 0.5); }
.model-name { margin-left: auto; font-weight: 600; color: #4338ca; }

@media (max-width: 768px) {
  .pipeline { flex-direction: column; }
  .connector { width: 1.5px; height: 14px; align-self: flex-start; margin-left: 14px; }
}
</style>
