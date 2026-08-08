<template>
  <aside class="history-panel card">
    <div class="history-header border-b pb-3 mb-3">
      <h3 class="history-title">{{ title }}</h3>
      <p class="text-xs text-muted m-0">{{ subtitle }}</p>
    </div>

    <div class="search-box mb-3">
      <input
        v-model="keywordInput"
        type="search"
        class="search-input"
        placeholder="搜索标题或会话号…"
        @keydown.enter="applySearch"
      />
      <button type="button" class="btn btn-secondary text-xs search-btn" @click="applySearch">
        搜索
      </button>
    </div>

    <div v-if="loading" class="text-sm text-muted py-4 text-center">加载历史中…</div>
    <div v-else-if="sessions.length === 0" class="text-sm text-muted py-4 text-center">
      {{ activeKeyword ? '未找到匹配的历史记录' : '暂无历史记录，提交后将自动保存' }}
    </div>
    <ul v-else class="history-list">
      <li
        v-for="item in sessions"
        :key="item.id"
        class="history-item"
        :class="{ active: item.id === activeId }"
      >
        <button type="button" class="history-btn" @click="onSelect(item.id)">
          <span class="history-item-title">{{ item.title }}</span>
          <span class="history-meta">
            <span>{{ formatRelative(item.createdAt) }}</span>
            <span v-if="item.confidence != null" class="confidence">
              {{ Math.round(item.confidence * 100) }}%
            </span>
          </span>
        </button>
      </li>
    </ul>

    <button
      v-if="totalElements > sessions.length"
      type="button"
      class="btn btn-secondary text-sm w-full mt-3"
      :disabled="loadingMore"
      @click="loadMore"
    >
      {{ loadingMore ? '加载中…' : `加载更多 (${sessions.length}/${totalElements})` }}
    </button>
  </aside>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { fetchLegalSessionDetail, fetchLegalSessions } from '@/shared/api/legal';
import type { LegalScenarioType, LegalSessionSummary } from '@/shared/types/legal';
import { formatRelativeTime } from '@/shared/utils/datetime';

const props = defineProps<{
  scenarioType: LegalScenarioType;
  title?: string;
  subtitle?: string;
  refreshKey?: number;
}>();

const emit = defineEmits<{
  restore: [detail: { inputPayload: string; outputPayload: string }];
}>();

const sessions = ref<LegalSessionSummary[]>([]);
const activeId = ref<number | null>(null);
const loading = ref(false);
const loadingMore = ref(false);
const page = ref(0);
const totalElements = ref(0);
const keywordInput = ref('');
const activeKeyword = ref('');

const title = props.title ?? '历史记录';
const subtitle = props.subtitle ?? '点击可恢复输入与结果';

async function loadSessions(reset = false): Promise<void> {
  if (reset) {
    page.value = 0;
    sessions.value = [];
  }
  const isFirst = page.value === 0;
  if (isFirst) loading.value = true;
  else loadingMore.value = true;
  try {
    const result = await fetchLegalSessions(
      props.scenarioType,
      page.value,
      10,
      activeKeyword.value || undefined
    );
    totalElements.value = result.totalElements;
    if (reset) {
      sessions.value = result.content;
    } else {
      sessions.value = [...sessions.value, ...result.content];
    }
  } catch (error) {
    console.error('加载历史失败:', error);
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
}

async function onSelect(id: number): Promise<void> {
  activeId.value = id;
  try {
    const detail = await fetchLegalSessionDetail(id);
    emit('restore', {
      inputPayload: detail.inputPayload,
      outputPayload: detail.outputPayload
    });
  } catch (error) {
    console.error('加载历史详情失败:', error);
  }
}

function loadMore(): void {
  page.value += 1;
  void loadSessions(false);
}

function applySearch(): void {
  activeKeyword.value = keywordInput.value.trim();
  void loadSessions(true);
}

// 相对时间逻辑统一来自 @/shared/utils/datetime
const formatRelative = formatRelativeTime;

function refresh(clearKeyword = false): void {
  if (clearKeyword) {
    keywordInput.value = '';
    activeKeyword.value = '';
  }
  void loadSessions(true);
}

watch(
  () => props.refreshKey,
  () => refresh()
);

onMounted(() => {
  refresh(true);
});

defineExpose({ refresh });
</script>

<style scoped>
.history-panel {
  padding: 1rem;
  height: fit-content;
  max-height: calc(100vh - 2rem);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.history-title {
  margin: 0;
  font-size: 0.95rem;
}

.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
}

.history-item {
  margin-bottom: 0.35rem;
}

.history-btn {
  width: 100%;
  text-align: left;
  border: 1px solid var(--border-light);
  background: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 0.65rem 0.75rem;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
  font: inherit;
  color: inherit;
}

.history-item.active .history-btn,
.history-btn:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.history-item-title {
  display: block;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 0.25rem;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: 0.7rem;
  color: var(--text-muted);
}

.confidence {
  color: var(--primary);
  font-weight: 600;
}

.w-full {
  width: 100%;
}

.border-b {
  border-bottom: 1px solid var(--border-light);
}

.pb-3 {
  padding-bottom: 0.75rem;
}

.mb-3 {
  margin-bottom: 0.75rem;
}

.mt-3 {
  margin-top: 0.75rem;
}

.text-xs {
  font-size: 0.75rem;
}

.text-sm {
  font-size: 0.875rem;
}

.text-muted {
  color: var(--text-muted);
}

.text-center {
  text-align: center;
}

.py-4 {
  padding-top: 1rem;
  padding-bottom: 1rem;
}

.m-0 {
  margin: 0;
}

.search-box {
  display: flex;
  gap: 0.35rem;
}

.search-input {
  flex: 1;
  min-width: 0;
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  font-size: 0.8125rem;
  background: var(--bg-surface);
  color: var(--text-main);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
}

.search-btn {
  flex-shrink: 0;
  padding: 0.45rem 0.65rem;
}

.text-xs {
  font-size: 0.75rem;
}
</style>
