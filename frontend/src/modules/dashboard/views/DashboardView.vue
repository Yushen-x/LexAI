<template>
  <div class="page-container fade-in">
    <p v-if="errorBanner" class="error-banner mb-6">{{ errorBanner }}</p>

    <!-- Welcome Banner Section -->
    <div class="welcome-banner mb-6 card flex justify-between items-center" style="padding: 1.5rem 2rem; border-left: 4px solid var(--primary); background: var(--bg-surface); color: var(--text-strong); box-shadow: var(--shadow-sm);">
      <div class="welcome-content">
        <h2 class="welcome-title" style="margin: 0 0 0.25rem 0; font-size: 1.5rem; color: var(--text-strong); font-weight: 700;">欢迎回来, {{ userName }}</h2>
        <p class="welcome-subtitle" style="margin: 0; color: var(--text-muted); font-size: 0.9375rem;">
          {{ overview?.positioning || 'LexAI 智慧合同管理系统已准备就绪' }}
        </p>
        <div v-if="overview?.projectName" class="user-meta mt-3 flex items-center">
          <span class="badge badge-primary mr-2">{{ overview.projectName }}</span>
          <span class="badge" style="background: var(--bg-app); color: var(--text-muted); border: 1px solid var(--border-light)">
            工作台
          </span>
        </div>
      </div>
      <div class="welcome-action">
        <button type="button" class="btn btn-primary" @click="goWorkflow">
          处理待办
        </button>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid mb-6">
      <div v-for="(stat, idx) in adminStats" :key="idx" class="stat-card">
        <div class="stat-icon-wrapper" :style="{ backgroundColor: stat.bgColor, color: stat.color }">
          <component :is="stat.icon" style="width: 28px; height: 28px; stroke-width: 2px;" />
        </div>
        <div class="stat-info">
          <h4 class="stat-label">{{ stat.title }}</h4>
          <span class="stat-value">{{ loadingStats ? '…' : stat.value }}</span>
        </div>
      </div>
    </div>

    <div class="grid-layout">
      <div class="left-col gap-6 flex-col flex">
        <div class="card">
          <div class="card-header pb-4 border-b">
            <h3 class="card-title">管理与快捷功能</h3>
          </div>
          <div class="quick-actions-grid pt-4">
            <button type="button" class="action-btn" @click="goContractList">
              <div class="icon-box" style="background: #eff6ff; color: #3b82f6;"><IconContract style="width: 26px; height: 26px; opacity: 0.9;" /></div>
              <span class="action-text">合同台账</span>
            </button>
            <button type="button" class="action-btn" @click="goWorkflow">
              <div class="icon-box" style="background: #fef3c7; color: #f59e0b;"><IconWorkflow style="width: 26px; height: 26px; opacity: 0.9;" /></div>
              <span class="action-text">待办任务</span>
            </button>
            <button type="button" class="action-btn" @click="goConsultation">
              <div class="icon-box" style="background: #f3e8ff; color: #8b5cf6;"><IconConsultation style="width: 26px; height: 26px; opacity: 0.9;" /></div>
              <span class="action-text">法律咨询</span>
            </button>
            <button type="button" class="action-btn" @click="goContractReview">
              <div class="icon-box" style="background: #d1fae5; color: #10b981;"><IconReview style="width: 26px; height: 26px; opacity: 0.9;" /></div>
              <span class="action-text">合同审查</span>
            </button>
          </div>
        </div>

        <div class="card">
          <div class="card-header pb-4 border-b">
            <h3 class="card-title">系统运行状态</h3>
          </div>
          <div class="status-list pt-4">
            <div v-for="(status, index) in systemStatus" :key="index" class="status-item">
              <div class="status-label">
                <span class="status-dot" :style="{ backgroundColor: status.color }"></span>
                {{ status.name }}
              </div>
              <div class="status-value">
                <span class="badge" :class="status.badgeClass">{{ status.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-col gap-6 flex-col flex">
        <div class="card">
          <div class="card-header pb-4 border-b flex justify-between items-center">
            <h3 class="card-title">待办任务</h3>
            <button
              type="button"
              class="text-primary text-sm cursor-pointer hover-underline btn-linkish"
              @click="goWorkflow"
            >
              查看更多
            </button>
          </div>
          <div class="todo-list pt-2">
            <div v-if="!loadingStats && todoTasks.length === 0" class="text-muted text-sm px-4 py-3">
              暂无待处理任务
            </div>
            <div v-for="task in todoTasks" :key="task.id" class="todo-item">
              <span class="badge badge-primary mr-3 flex-shrink-0">{{ typeShortLabel(task.type) }}</span>
              <div class="todo-content">
                <div class="todo-title">{{ task.title }}</div>
                <div class="todo-time">{{ formatRelative(task.createdAt) }}</div>
              </div>
              <button type="button" class="btn btn-secondary text-sm px-3 py-1 ml-2" @click="goWorkflow">
                去处理
              </button>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header pb-4 border-b">
            <h3 class="card-title">最近活动</h3>
          </div>
          <div class="activity-list pt-4">
            <div v-if="!loadingStats && recentActivities.length === 0" class="text-muted text-sm px-4 py-3">
              暂无近期任务记录
            </div>
            <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
              <div class="activity-icon" :style="{ backgroundColor: activity.bgColor, color: activity.color }">
                <component :is="activity.icon" style="width: 18px; height: 18px;" />
              </div>
              <div class="activity-content">
                <div class="activity-text">{{ activity.text }}</div>
                <div class="activity-time">{{ activity.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { 
  Files as IconContract, 
  ListTodo as IconWorkflow, 
  Layers as IconCapability, 
  History as IconHistory,
  MessageSquare as IconConsultation,
  BarChart2 as IconAnalysis,
  FileSearch as IconReview,
  FileEdit as IconDraft
} from 'lucide-vue-next';
import { fetchContracts } from '@/shared/api/contracts';
import { fetchHealth, fetchOverview } from '@/shared/api/legal';
import { fetchTasks } from '@/shared/api/tasks';
import type { PlatformOverview } from '@/shared/types/legal';
import type { TaskItem, WorkspaceTaskType } from '@/shared/types/tasks';

const router = useRouter();

const userName = ref('Admin');
const overview = ref<PlatformOverview | null>(null);
const loadingStats = ref(true);
const errorBanner = ref('');

const contractTotal = ref(0);
const pendingTaskCount = ref(0);
const allTaskCount = ref(0);
const capabilityCount = ref(0);

const healthUp = ref(true);

const pendingTasksList = ref<TaskItem[]>([]);
const allTasksSnapshot = ref<TaskItem[]>([]);

function errMessage(e: unknown): string {
  if (e instanceof Error) return e.message;
  return '加载失败';
}

function typeShortLabel(t: WorkspaceTaskType): string {
  const m: Record<WorkspaceTaskType, string> = {
    LEGAL_CONSULTATION: '咨询',
    CASE_ANALYSIS: '分析',
    CONTRACT_REVIEW: '审查',
    CONTRACT_DRAFT: '起草',
  };
  return m[t] ?? t;
}

function typeLabel(t: WorkspaceTaskType): string {
  const m: Record<WorkspaceTaskType, string> = {
    LEGAL_CONSULTATION: '法律咨询',
    CASE_ANALYSIS: '案件分析',
    CONTRACT_REVIEW: '合同审查',
    CONTRACT_DRAFT: '合同起草',
  };
  return m[t] ?? t;
}

function formatRelative(iso: string): string {
  if (!iso) return '—';
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return iso;
  const diff = Date.now() - t;
  const m = 60_000;
  const h = 60 * m;
  const d = 24 * h;
  if (diff < m) return '刚刚';
  if (diff < h) return `${Math.floor(diff / m)} 分钟前`;
  if (diff < d) return `${Math.floor(diff / h)} 小时前`;
  if (diff < 7 * d) return `${Math.floor(diff / d)} 天前`;
  return new Date(iso).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

const adminStats = computed(() => [
  {
    title: '合同总数',
    value: formatInt(contractTotal.value),
    icon: IconContract,
    color: '#3b82f6',
    bgColor: '#eff6ff',
  },
  {
    title: '待处理任务',
    value: formatInt(pendingTaskCount.value),
    icon: IconWorkflow,
    color: '#f59e0b',
    bgColor: '#fef3c7',
  },
  {
    title: '能力模块',
    value: formatInt(capabilityCount.value),
    icon: IconCapability,
    color: '#8b5cf6',
    bgColor: '#f3e8ff',
  },
  {
    title: '任务记录',
    value: formatInt(allTaskCount.value),
    icon: IconHistory,
    color: '#10b981',
    bgColor: '#d1fae5',
  },
]);

function formatInt(n: number): string {
  return new Intl.NumberFormat('zh-CN').format(n);
}

const systemStatus = computed(() => [
  {
    name: 'API 服务',
    value: healthUp.value ? '正常' : '异常',
    color: healthUp.value ? '#16a34a' : '#dc2626',
    badgeClass: healthUp.value ? 'badge-success' : 'badge-danger',
  },
  {
    name: '平台概览',
    value: overview.value ? '已加载' : '—',
    color: overview.value ? '#16a34a' : '#94a3b8',
    badgeClass: overview.value ? 'badge-success' : 'badge-warning',
  },
  {
    name: '数据存储',
    value: 'H2 演示库',
    color: '#16a34a',
    badgeClass: 'badge-success',
  },
  {
    name: 'AI 推理',
    value: 'Mock 网关',
    color: '#ca8a04',
    badgeClass: 'badge-warning',
  },
]);

const todoTasks = computed(() => pendingTasksList.value.slice(0, 5));

const recentActivities = computed(() => {
  const sorted = [...allTasksSnapshot.value].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
  const top = sorted.slice(0, 5);
  const colorByType: Record<WorkspaceTaskType, string> = {
    LEGAL_CONSULTATION: '#3b82f6',
    CASE_ANALYSIS: '#10b981',
    CONTRACT_REVIEW: '#f59e0b',
    CONTRACT_DRAFT: '#8b5cf6',
  };
  const iconByType: Record<WorkspaceTaskType, any> = {
    LEGAL_CONSULTATION: IconConsultation,
    CASE_ANALYSIS: IconAnalysis,
    CONTRACT_REVIEW: IconReview,
    CONTRACT_DRAFT: IconDraft,
  };
  return top.map((t) => ({
    text: `${typeLabel(t.type)}：${t.title}`,
    time: formatRelative(t.createdAt),
    icon: iconByType[t.type] || IconHistory,
    color: colorByType[t.type] || '#64748b',
    bgColor: (colorByType[t.type] || '#64748b') + '15',
  }));
});

function goContractList() {
  router.push({ name: 'contractList' });
}
function goWorkflow() {
  router.push({ name: 'workflowPending' });
}
function goConsultation() {
  router.push({ name: 'consultation' });
}
function goContractReview() {
  router.push({ name: 'contractReview' });
}

async function loadDashboard(): Promise<void> {
  errorBanner.value = '';
  loadingStats.value = true;
  try {
    const [ov, health, listResult, tasks] = await Promise.all([
      fetchOverview(),
      fetchHealth().catch(() => ({ status: 'DOWN' })),
      fetchContracts({ page: 0, size: 1 }),
      fetchTasks(),
    ]);
    overview.value = ov;
    capabilityCount.value = ov.capabilities?.length ?? 0;
    healthUp.value = health.status === 'UP';
    contractTotal.value = listResult.totalElements;
    allTasksSnapshot.value = tasks;
    const pending = tasks.filter((t) => t.status === 'PENDING');
    pendingTasksList.value = pending;
    pendingTaskCount.value = pending.length;
    allTaskCount.value = tasks.length;
  } catch (e: unknown) {
    errorBanner.value = errMessage(e);
    overview.value = null;
    healthUp.value = false;
    contractTotal.value = 0;
    pendingTaskCount.value = 0;
    allTaskCount.value = 0;
    capabilityCount.value = 0;
    pendingTasksList.value = [];
    allTasksSnapshot.value = [];
  } finally {
    loadingStats.value = false;
  }
}

onMounted(() => {
  void loadDashboard();
});
</script>

<style scoped>
.error-banner {
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
  font-size: 0.9rem;
}

.btn-linkish {
  border: none;
  background: none;
  font: inherit;
  padding: 0;
}

.page-container {
  max-width: 1400px;
  margin: 0 auto;
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
.mt-3 {
  margin-top: 0.75rem;
}
.mr-2 {
  margin-right: 0.5rem;
}
.mr-3 {
  margin-right: 0.75rem;
}
.ml-2 {
  margin-left: 0.5rem;
}
.pb-4 {
  padding-bottom: 1rem;
}
.pt-4 {
  padding-top: 1rem;
}
.pt-2 {
  padding-top: 0.5rem;
}
.px-3 {
  padding-left: 0.75rem;
  padding-right: 0.75rem;
}
.px-4 {
  padding-left: 1rem;
  padding-right: 1rem;
}
.py-1 {
  padding-top: 0.25rem;
  padding-bottom: 0.25rem;
}
.py-3 {
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
}
.border-b {
  border-bottom: 1px solid var(--border-light);
}
.text-sm {
  font-size: 0.875rem;
}
.text-xs {
  font-size: 0.75rem;
}
.text-muted {
  color: var(--text-muted);
}
.text-primary {
  color: var(--primary);
}
.gap-6 {
  gap: 1.5rem;
}
.flex {
  display: flex;
}
.flex-col {
  flex-direction: column;
}
.justify-between {
  justify-content: space-between;
}
.items-center {
  align-items: center;
}
.flex-shrink-0 {
  flex-shrink: 0;
}
.cursor-pointer {
  cursor: pointer;
}
.hover-underline:hover {
  text-decoration: underline;
}

/* Welcome Banner styling replaced by inline unified styles */

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
}

.stat-card {
  border: none !important;
  background: var(--bg-surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  padding: 2rem;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.stat-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1.5rem;
}

.stat-icon-text {
  font-size: 1.75rem;
  font-weight: 700;
}

.stat-label {
  margin: 0 0 0.25rem 0;
  font-size: 0.875rem;
  color: var(--text-muted);
  font-weight: 500;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-strong);
  line-height: 1;
}

.grid-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  align-items: start;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  padding: 1rem;
  border-radius: var(--radius-md);
  transition: background-color 0.2s;
  border: none;
  background: transparent;
  font: inherit;
  color: inherit;
}

.action-btn:hover {
  background-color: var(--bg-app);
}

.icon-box {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.25rem;
  font-weight: 700;
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.action-btn:hover .icon-box {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.bg-blue {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}
.bg-green {
  background: linear-gradient(135deg, #22c55e, #15803d);
}
.bg-orange {
  background: linear-gradient(135deg, #f59e0b, #b45309);
}
.bg-purple {
  background: linear-gradient(135deg, #a855f7, #6b21a8);
}

.action-text {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-main);
}

.status-list {
  display: flex;
  flex-direction: column;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid var(--border-light);
}

.status-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.status-label {
  display: flex;
  align-items: center;
  font-size: 0.875rem;
  color: var(--text-main);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 0.75rem;
}

.todo-item,
.activity-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  transition:
    background-color 0.2s,
    border-color 0.2s;
}

.todo-item:hover,
.activity-item:hover {
  background-color: var(--bg-app);
  border-color: var(--border-light);
}

.todo-content,
.activity-content {
  flex: 1;
  min-width: 0;
}

.todo-title,
.activity-text {
  font-size: 0.875rem;
  color: var(--text-strong);
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.todo-time,
.activity-time {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
  font-weight: 600;
  flex-shrink: 0;
}

@media (max-width: 1024px) {
  .stats-grid,
  .grid-layout {
    grid-template-columns: 1fr;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .welcome-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}
</style>
