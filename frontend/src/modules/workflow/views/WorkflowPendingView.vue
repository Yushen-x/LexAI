<template>
  <div class="page-container fade-in">
    <!-- Header -->
    <div class="header-section mb-6">
      <div class="flex justify-between items-end flex-wrap gap-3">
        <div>
          <h2 class="text-xl font-semibold text-strong m-0">工作流与待办任务</h2>
          <p class="text-sm text-muted mt-1 m-0">对接工作台任务 API，按状态筛选并更新处理进度</p>
        </div>
        <div class="stats-badge">
          当前待处理 <span class="text-primary font-bold">{{ pendingCount }}</span> 项
        </div>
      </div>
    </div>

    <p v-if="errorBanner" class="error-banner mb-4">{{ errorBanner }}</p>

    <!-- Layout Grid -->
    <div class="workflow-grid">
      <!-- Left: Filter & Categories -->
      <div class="sidebar-col">
        <div class="card mb-4 p-4">
          <h4 class="text-sm font-semibold mb-3 m-0">任务分类</h4>
          <ul class="nav-list text-sm">
            <li
              v-for="item in navItems"
              :key="item.key"
              class="nav-item"
              :class="{ active: activeNav === item.key }"
              @click="selectNav(item.key)"
            >
              <span class="icon mr-2">{{ item.icon }}</span>{{ item.label }}
              <span v-if="item.key === 'PENDING'" class="count-badge ml-auto">{{ pendingCount }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Right: Task List -->
      <div class="main-col flex-col flex gap-4">
        <div v-if="loading && tasks.length === 0" class="card p-5 text-muted text-sm">加载中…</div>
        <div v-if="!loading && tasks.length === 0" class="card p-5 text-muted text-sm">该分类下暂无任务</div>

        <div v-for="task in tasks" :key="task.id" class="card task-card p-5">
          <div class="flex justify-between items-start mb-3 flex-wrap gap-2">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="badge badge-normal">{{ typeLabel(task.type) }}</span>
              <h3 class="task-title m-0 font-medium">{{ task.title }}</h3>
            </div>
            <span class="badge" :class="statusBadgeClass(task.status)">
              {{ statusLabel(task.status) }}
            </span>
          </div>

          <div class="task-meta-grid mb-4 bg-app p-3 rounded-md">
            <div class="meta-item">
              <span class="meta-label">任务编号:</span>
              <span class="meta-value font-mono">{{ task.taskNo }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">关联 ID:</span>
              <span class="meta-value font-mono">{{ task.relatedId }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">发起人:</span>
              <span class="meta-value">{{ task.initiator }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间:</span>
              <span class="meta-value">{{ formatTime(task.createdAt) }}</span>
            </div>
          </div>

          <div class="flex justify-between items-center mt-2 border-t pt-3 flex-wrap gap-3">
            <div class="text-xs text-muted">记录 ID：<span class="font-mono">{{ task.id }}</span></div>
            <div class="flex gap-3 items-center flex-wrap">
              <label class="status-field text-xs text-muted flex items-center gap-2">
                <span>变更状态</span>
                <select
                  class="form-select"
                  :value="task.status"
                  :disabled="busyId === task.id"
                  @change="onStatusChange(task, ($event.target as HTMLSelectElement).value)"
                >
                  <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
              </label>
              <button type="button" class="btn btn-secondary text-sm" @click="showDetails(task)">详情</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchTasks, updateTaskStatus } from '@/shared/api/tasks';
import type { TaskItem, WorkspaceTaskStatus, WorkspaceTaskType } from '@/shared/types/tasks';

const loading = ref(false);
const busyId = ref<number | null>(null);
const errorBanner = ref('');
const tasks = ref<TaskItem[]>([]);
const pendingCount = ref(0);
const activeNav = ref<string>('PENDING');

const navItems = [
  { key: 'PENDING', label: '待处理', icon: '⏱️' },
  { key: 'IN_PROGRESS', label: '处理中', icon: '🔄' },
  { key: 'COMPLETED', label: '已完成', icon: '✅' },
  { key: 'REJECTED', label: '已驳回', icon: '⛔' },
  { key: 'ALL', label: '全部', icon: '📋' },
];

const statusOptions: { value: WorkspaceTaskStatus; label: string }[] = [
  { value: 'PENDING', label: '待处理' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' },
];

function typeLabel(t: WorkspaceTaskType): string {
  const m: Record<WorkspaceTaskType, string> = {
    LEGAL_CONSULTATION: '法律咨询',
    CASE_ANALYSIS: '案件分析',
    CONTRACT_REVIEW: '合同审查',
    CONTRACT_DRAFT: '合同起草',
  };
  return m[t] ?? t;
}

function statusLabel(s: WorkspaceTaskStatus): string {
  return statusOptions.find((o) => o.value === s)?.label ?? s;
}

function statusBadgeClass(s: WorkspaceTaskStatus): string {
  switch (s) {
    case 'PENDING':
      return 'badge-warning';
    case 'IN_PROGRESS':
      return 'badge-primary';
    case 'COMPLETED':
      return 'badge-success';
    case 'REJECTED':
      return 'badge-danger';
    default:
      return 'badge-normal';
  }
}

function formatTime(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function errMessage(e: unknown): string {
  if (e instanceof Error) return e.message;
  return '请求失败，请重试';
}

function currentFilter(): WorkspaceTaskStatus | undefined {
  if (activeNav.value === 'ALL') return undefined;
  return activeNav.value as WorkspaceTaskStatus;
}

async function refreshPendingCount(): Promise<void> {
  try {
    const list = await fetchTasks('PENDING');
    pendingCount.value = list.length;
  } catch {
    pendingCount.value = 0;
  }
}

async function loadList(): Promise<void> {
  errorBanner.value = '';
  loading.value = true;
  try {
    tasks.value = await fetchTasks(currentFilter());
    await refreshPendingCount();
  } catch (e: unknown) {
    errorBanner.value = errMessage(e);
    tasks.value = [];
  } finally {
    loading.value = false;
  }
}

function selectNav(key: string): void {
  activeNav.value = key;
  void loadList();
}

function showDetails(task: TaskItem): void {
  const lines = [
    `任务编号：${task.taskNo}`,
    `标题：${task.title}`,
    `类型：${typeLabel(task.type)}`,
    `关联 ID：${task.relatedId}`,
    `发起人：${task.initiator}`,
    `状态：${statusLabel(task.status)}`,
    `创建：${formatTime(task.createdAt)}`,
  ];
  window.alert(lines.join('\n'));
}

async function onStatusChange(task: TaskItem, next: string): Promise<void> {
  const newStatus = next as WorkspaceTaskStatus;
  if (newStatus === task.status) return;
  busyId.value = task.id;
  errorBanner.value = '';
  try {
    await updateTaskStatus(task.id, newStatus);
    await loadList();
  } catch (e: unknown) {
    errorBanner.value = errMessage(e);
    await loadList();
  } finally {
    busyId.value = null;
  }
}

onMounted(() => {
  void loadList();
});
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.error-banner {
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
  font-size: 0.9rem;
}

.fade-in {
  animation: fadeIn 0.4s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mb-6 {
  margin-bottom: 1.5rem;
}
.mb-4 {
  margin-bottom: 1rem;
}
.mb-3 {
  margin-bottom: 0.75rem;
}
.mt-1 {
  margin-top: 0.25rem;
}
.mt-2 {
  margin-top: 0.5rem;
}
.mr-2 {
  margin-right: 0.5rem;
}
.ml-auto {
  margin-left: auto;
}
.p-4 {
  padding: 1rem;
}
.p-5 {
  padding: 1.25rem;
}
.p-3 {
  padding: 0.75rem;
}
.pt-3 {
  padding-top: 0.75rem;
}

.border-t {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
.bg-app {
  background-color: var(--bg-app);
  border-radius: var(--radius-lg);
}
.rounded-md {
  border-radius: var(--radius-lg);
}

.text-xl {
  font-size: 1.25rem;
}
.text-sm {
  font-size: 0.875rem;
}
.text-xs {
  font-size: 0.75rem;
}
.font-semibold {
  font-weight: 600;
}
.font-medium {
  font-weight: 500;
}
.font-bold {
  font-weight: 700;
}
.font-mono {
  font-family: monospace;
}
.m-0 {
  margin: 0;
}

.text-muted {
  color: var(--text-muted);
}
.text-main {
  color: var(--text-main);
}
.text-strong {
  color: var(--text-strong);
}
.text-primary {
  color: var(--primary);
}

.flex {
  display: flex;
}
.flex-col {
  flex-direction: column;
}
.flex-wrap {
  flex-wrap: wrap;
}
.gap-2 {
  gap: 0.5rem;
}
.gap-3 {
  gap: 0.75rem;
}
.gap-4 {
  gap: 1rem;
}
.justify-between {
  justify-content: space-between;
}
.items-center {
  align-items: center;
}
.items-start {
  align-items: flex-start;
}
.items-end {
  align-items: flex-end;
}

.stats-badge {
  background: rgba(37, 99, 235, 0.1);
  color: var(--text-main);
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-size: 0.875rem;
  border: 1px solid rgba(37, 99, 235, 0.2);
}

.workflow-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 1.5rem;
  align-items: start;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.2s;
}

.nav-item:hover {
  background: var(--bg-app);
}

.nav-item.active {
  background: var(--primary);
  color: white;
  font-weight: 500;
}

.count-badge {
  background: white;
  color: var(--primary);
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.nav-item:not(.active) .count-badge {
  background: var(--border-light);
  color: var(--text-muted);
}

.nav-item.active .count-badge {
  background: rgba(255, 255, 255, 0.25);
  color: white;
}

.task-card {
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  border: none !important;
  border-radius: var(--radius-xl);
}

.task-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.task-title {
  color: var(--text-strong);
  font-size: 1.05rem;
}

.task-meta-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.meta-label {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.meta-value {
  color: var(--text-strong);
  font-size: 0.875rem;
}

.badge-normal {
  background: var(--border-light);
  color: var(--text-main);
  font-weight: 500;
}

.badge-warning {
  background: #fef9c3;
  color: #854d0e;
  border: 1px solid #eab308;
}

.badge-primary {
  background: rgba(37, 99, 235, 0.12);
  color: var(--primary);
  border: 1px solid rgba(37, 99, 235, 0.35);
}

.badge-success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #22c55e;
}

.badge-danger {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #f87171;
}

.form-select {
  font-size: 0.8rem;
  padding: 0.35rem 0.5rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
  background: var(--bg-app);
  color: var(--text-main);
  min-width: 6.5rem;
}

.status-field {
  white-space: nowrap;
}

@media (max-width: 768px) {
  .workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
