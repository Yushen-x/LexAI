<template>
  <div class="page-container fade-in">
    <!-- Welcome Banner Section -->
    <div class="welcome-banner mb-6">
      <div class="welcome-content">
        <h2 class="welcome-title">欢迎回来, {{ userName }}</h2>
        <p class="welcome-subtitle">LexAI 智慧合同管理系统已准备就绪</p>
        <div class="user-meta mt-3">
          <span class="badge badge-primary mr-2">法务审核部</span>
          <span class="badge" style="background:rgba(255,255,255,0.2);color:#fff;border:1px solid rgba(255,255,255,0.3);">系统管理员</span>
        </div>
      </div>
      <div class="welcome-action">
        <button class="btn btn-secondary" style="background:rgba(255,255,255,0.1);color:#fff;border-color:rgba(255,255,255,0.2);">
          进入个人主页
        </button>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid mb-6">
      <div v-for="(stat, idx) in adminStats" :key="idx" class="stat-card">
        <div class="stat-icon-wrapper" :style="{ backgroundColor: stat.bgColor, color: stat.color }">
          <span class="stat-icon-text">{{ stat.icon }}</span>
        </div>
        <div class="stat-info">
          <h4 class="stat-label">{{ stat.title }}</h4>
          <span class="stat-value">{{ stat.value }}</span>
        </div>
      </div>
    </div>

    <div class="grid-layout">
      <!-- Left Column: System Status & Quick Actions -->
      <div class="left-col gap-6 flex-col flex">
        <!-- Quick Actions -->
        <div class="card">
          <div class="card-header pb-4 border-b">
            <h3 class="card-title">管理与快捷功能</h3>
          </div>
          <div class="quick-actions-grid pt-4">
            <div class="action-btn">
              <div class="icon-box bg-blue">U</div>
              <span class="action-text">用户管理</span>
            </div>
            <div class="action-btn">
              <div class="icon-box bg-green">O</div>
              <span class="action-text">组织架构</span>
            </div>
            <div class="action-btn">
              <div class="icon-box bg-orange">K</div>
              <span class="action-text">知识库</span>
            </div>
            <div class="action-btn">
              <div class="icon-box bg-purple">W</div>
              <span class="action-text">流程管理</span>
            </div>
          </div>
        </div>

        <!-- System Status -->
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

      <!-- Right Column: Recent Activities & To-Do -->
      <div class="right-col gap-6 flex-col flex">
        <!-- Todo Tasks -->
        <div class="card">
          <div class="card-header pb-4 border-b flex justify-between items-center">
            <h3 class="card-title">待办任务</h3>
            <span class="text-primary text-sm cursor-pointer hover-underline">查看更多</span>
          </div>
          <div class="todo-list pt-2">
            <div v-for="(task, index) in todoTasks" :key="index" class="todo-item">
              <span class="badge badge-primary mr-3 flex-shrink-0">审批</span>
              <div class="todo-content">
                <div class="todo-title">{{ task.title }}</div>
                <div class="todo-time">{{ task.time }}</div>
              </div>
              <button class="btn btn-secondary text-sm px-3 py-1 ml-2">去处理</button>
            </div>
          </div>
        </div>

        <!-- Recent Activities -->
        <div class="card">
          <div class="card-header pb-4 border-b">
            <h3 class="card-title">最近活动</h3>
          </div>
          <div class="activity-list pt-4">
            <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
              <div class="activity-icon" :style="{ backgroundColor: activity.color }">
                {{ activity.icon }}
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
import { ref, reactive } from 'vue';

const userName = ref('Admin');

// 模拟管理员统计数据
const adminStats = reactive([
  { title: '用户总数', value: '1,245', icon: 'U', color: '#2563eb', bgColor: '#eff6ff' },
  { title: '部门总数', value: '48', icon: 'D', color: '#16a34a', bgColor: '#dcfce7' },
  { title: '知识库文件', value: '3,892', icon: 'K', color: '#ca8a04', bgColor: '#fef08a' },
  { title: '合同总数', value: '12,506', icon: 'C', color: '#dc2626', bgColor: '#fee2e2' }
]);

const systemStatus = reactive([
  { name: '主数据库连接', value: '正常', color: '#16a34a', badgeClass: 'badge-success' },
  { name: 'AI 引擎与大模型节点', value: '运行中', color: '#16a34a', badgeClass: 'badge-success' },
  { name: '核心系统负载', value: '42%', color: '#ca8a04', badgeClass: 'badge-warning' },
  { name: '云端存储空间', value: '充足', color: '#16a34a', badgeClass: 'badge-success' }
]);

const todoTasks = reactive([
  { title: '关于某省政企定制化平台开发采购项目合同审批', time: '10 分钟前' },
  { title: '2026年度云服务框架采购合作协议补充条款复核', time: '1 小时前' },
  { title: '人事外包服务年度续约合同法务排查', time: '3 小时前' },
  { title: '新办公园区弱电工程施工节点确权意见书', time: '昨天 15:30' }
]);

const recentActivities = reactive([
  { text: '法务部 李四 创建了新合同初稿', time: '2分钟前', icon: 'C', color: '#2563eb' },
  { text: '财务部 王五 提交了合同付款计划审批', time: '15分钟前', icon: 'S', color: '#16a34a' },
  { text: '系统自动执行合规性抽查完成 (100份)', time: '2小时前', icon: 'A', color: '#64748b' },
  { text: '管理员 添加了新的合规模板至知识库', time: '3小时前', icon: 'K', color: '#ca8a04' }
]);
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.mb-6 { margin-bottom: 1.5rem; }
.mt-3 { margin-top: 0.75rem; }
.mr-2 { margin-right: 0.5rem; }
.mr-3 { margin-right: 0.75rem; }
.ml-2 { margin-left: 0.5rem; }
.pb-4 { padding-bottom: 1rem; }
.pt-4 { padding-top: 1rem; }
.pt-2 { padding-top: 0.5rem; }
.px-3 { padding-left: 0.75rem; padding-right: 0.75rem; }
.py-1 { padding-top: 0.25rem; padding-bottom: 0.25rem; }
.border-b { border-bottom: 1px solid var(--border-light); }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--primary); }
.gap-6 { gap: 1.5rem; }
.flex { display: flex; }
.flex-col { flex-direction: column; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.flex-shrink-0 { flex-shrink: 0; }
.cursor-pointer { cursor: pointer; }
.hover-underline:hover { text-decoration: underline; }

/* Welcome Banner */
.welcome-banner {
  background: linear-gradient(135deg, #1e3a8a 0%, #312e81 100%);
  border-radius: var(--radius-lg);
  padding: 2rem;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-md);
}

.welcome-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.75rem;
  color: white;
}

.welcome-subtitle {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 1rem;
}

/* Stats Grid */
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
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1);
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

/* Main Grid Layout */
.grid-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  align-items: start;
}

/* Quick Actions */
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
  transition: transform 0.2s;
}

.action-btn:hover .icon-box {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.bg-blue { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.bg-green { background: linear-gradient(135deg, #22c55e, #15803d); }
.bg-orange { background: linear-gradient(135deg, #f59e0b, #b45309); }
.bg-purple { background: linear-gradient(135deg, #a855f7, #6b21a8); }

.action-text {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-main);
}

/* System Status */
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

/* Todo & Activities */
.todo-item, .activity-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  transition: background-color 0.2s, border-color 0.2s;
}

.todo-item:hover, .activity-item:hover {
  background-color: var(--bg-app);
  border-color: var(--border-light);
}

.todo-content, .activity-content {
  flex: 1;
  min-width: 0;
}

.todo-title, .activity-text {
  font-size: 0.875rem;
  color: var(--text-strong);
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.todo-time, .activity-time {
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
  .stats-grid, .grid-layout {
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
