<template>
  <div v-if="items && items.length" class="rag-list">
    <div class="rag-list-header">
      <span class="rag-list-title">{{ titlePrefix }} · 共 {{ items.length }} 条</span>
      <span class="rag-list-hint">点击任意条目可展开完整原文，并可一键复制</span>
    </div>
    <ul>
      <li
        v-for="(item, idx) in items"
        :key="idx"
        class="rag-item"
        :class="{ open: openSet.has(idx), highlighted: highlightedIndex === idx }"
        :id="anchorId(idx)"
      >
        <div class="rag-item-row" @click="toggle(idx)">
          <span class="idx-chip" :class="kind">{{ chipPrefix }}{{ idx + 1 }}</span>
          <span class="rag-snippet">{{ snippet(item) }}</span>
          <span class="caret" :class="{ open: openSet.has(idx) }">▾</span>
        </div>
        <div v-show="openSet.has(idx)" class="rag-detail">
          <pre>{{ item }}</pre>
          <div class="rag-actions">
            <button type="button" class="btn-mini" @click.stop="copy(item, idx)">
              {{ copiedIdx === idx ? '已复制 ✓' : '一键复制原文' }}
            </button>
            <a
              v-if="searchUrl(item)"
              :href="searchUrl(item)"
              target="_blank"
              rel="noopener"
              class="btn-mini link"
              @click.stop
            >在得理搜索 ↗</a>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface Props {
  items: string[];
  /** 'law' | 'case' | 'kb' —— 决定 chip 颜色 */
  kind: 'law' | 'case' | 'kb';
  /** chip 前缀字母，如 L / C / K */
  chipPrefix: string;
  /** 标题前缀，如 “得理 · 法律法规” */
  titlePrefix: string;
  /** 外部高亮指定索引（从 0 开始），用于 citation 点击联动 */
  highlightedIndex?: number | null;
}

const props = defineProps<Props>();

const openSet = ref<Set<number>>(new Set());
const copiedIdx = ref<number | null>(null);

function toggle(i: number) {
  if (openSet.value.has(i)) {
    openSet.value.delete(i);
  } else {
    openSet.value.add(i);
  }
  openSet.value = new Set(openSet.value);
}

function snippet(text: string) {
  if (!text) return '';
  const oneLine = text.replace(/\s+/g, ' ').trim();
  return oneLine.length > 80 ? oneLine.slice(0, 80) + '...' : oneLine;
}

async function copy(text: string, idx: number) {
  try {
    await navigator.clipboard.writeText(text);
    copiedIdx.value = idx;
    setTimeout(() => {
      if (copiedIdx.value === idx) copiedIdx.value = null;
    }, 1600);
  } catch {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); } catch (_) { /* ignore */ }
    document.body.removeChild(ta);
    copiedIdx.value = idx;
    setTimeout(() => {
      if (copiedIdx.value === idx) copiedIdx.value = null;
    }, 1600);
  }
}

function searchUrl(text: string) {
  if (!text) return '';
  const oneLine = text.replace(/\s+/g, ' ').trim();
  // 取前 30 个字符作为搜索关键词，避免 URL 过长
  const kw = oneLine.slice(0, 30);
  return `https://www.delilegal.com/search?keywords=${encodeURIComponent(kw)}`;
}

function anchorId(i: number) {
  return `rag-${props.kind}-${i}`;
}

watch(() => props.highlightedIndex, (idx) => {
  if (idx === null || idx === undefined || idx < 0) return;
  openSet.value.add(idx);
  openSet.value = new Set(openSet.value);
  // 等下一帧再滚动 + 加临时高亮（CSS 类已绑定）
  setTimeout(() => {
    const el = document.getElementById(anchorId(idx));
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, 50);
});
</script>

<style scoped>
.rag-list {
  margin-top: 0.4rem;
}
.rag-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  color: #6b7280;
  margin-bottom: 0.4rem;
}
.rag-list-title { font-weight: 600; color: #475569; }
.rag-list-hint { font-style: italic; }

ul { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.4rem; }

.rag-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  transition: all 0.2s;
}
.rag-item.open { border-color: #c7d2fe; box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08); }
.rag-item.highlighted {
  border-color: #f59e0b;
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
  animation: pulseHi 1.2s ease-out 1;
}
@keyframes pulseHi {
  0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.5); }
  100% { box-shadow: 0 0 0 8px rgba(245, 158, 11, 0); }
}

.rag-item-row {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.55rem 0.7rem;
  cursor: pointer;
  user-select: none;
  min-width: 0;
}

.idx-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}
.idx-chip.law { background: #e0f2fe; color: #0369a1; }
.idx-chip.case { background: #fef3c7; color: #92400e; }
.idx-chip.kb { background: #ede9fe; color: #6d28d9; }

.rag-snippet {
  flex: 1;
  min-width: 0;
  font-size: 0.85rem;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  color: #94a3b8;
  font-size: 0.75rem;
  transition: transform 0.2s;
}
.caret.open { transform: rotate(180deg); }

.rag-detail {
  border-top: 1px dashed #e5e7eb;
  padding: 0.65rem 0.85rem;
  background: #f8fafc;
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
}
.rag-detail pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.83rem;
  color: #334155;
  margin: 0 0 0.55rem;
  font-family: inherit;
  line-height: 1.55;
}

.rag-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.btn-mini {
  padding: 0.3rem 0.7rem;
  border-radius: 6px;
  border: 1px solid #c7d2fe;
  background: #eef2ff;
  color: #4338ca;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  text-decoration: none;
}
.btn-mini:hover { background: #e0e7ff; }
.btn-mini.link { background: #ecfeff; border-color: #67e8f9; color: #0e7490; }
.btn-mini.link:hover { background: #cffafe; }
</style>
