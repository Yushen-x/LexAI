<template>
  <div class="page-container fade-in">
    <!-- Header -->
    <div class="header-section mb-6">
      <div class="flex justify-between items-end">
        <div>
          <h2 class="text-xl font-semibold text-strong m-0">工作流与待办任务</h2>
          <p class="text-sm text-muted mt-1 m-0">Workflow Approvals & Pending Tickets</p>
        </div>
        <div class="stats-badge">
          今天还有 <span class="text-primary font-bold">4</span> 项待审批任务
        </div>
      </div>
    </div>

    <!-- Layout Grid -->
    <div class="workflow-grid">
      <!-- Left: Filter & Categories -->
      <div class="sidebar-col">
        <div class="card mb-4 p-4">
          <h4 class="text-sm font-semibold mb-3">任务分类</h4>
          <ul class="nav-list text-sm">
            <li class="nav-item active">
              <span class="icon mr-2">⏱️</span>待我审批 
              <span class="count-badge ml-auto">4</span>
            </li>
            <li class="nav-item">
              <span class="icon mr-2">✅</span>我已处理
            </li>
            <li class="nav-item">
              <span class="icon mr-2">📤</span>我发起的
            </li>
            <li class="nav-item">
              <span class="icon mr-2">📋</span>抄送我的
            </li>
          </ul>
        </div>
      </div>

      <!-- Right: Task List -->
      <div class="main-col flex-col flex gap-4">
        <!-- Single Task Card -->
        <div v-for="task in mockTasks" :key="task.id" class="card task-card p-5">
          <div class="flex justify-between items-start mb-3">
            <div class="flex items-center gap-2">
              <span class="badge badge-normal">{{ task.type }}</span>
              <h3 class="task-title m-0 font-medium">{{ task.title }}</h3>
            </div>
            <span class="badge" :class="task.urgent ? 'badge-danger' : 'badge-warning'">
              {{ task.node }}
            </span>
          </div>
          
          <div class="task-meta-grid mb-4 bg-app p-3 rounded-md">
            <div class="meta-item">
              <span class="meta-label">合同编号:</span>
              <span class="meta-value font-mono">{{ task.no }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">发起人:</span>
              <span class="meta-value">{{ task.initiator }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">提交时间:</span>
              <span class="meta-value">{{ task.time }}</span>
            </div>
            <div class="meta-item" v-if="task.isJoint">
              <span class="badge badge-primary text-xs">并线会签区</span>
            </div>
          </div>

          <div class="flex justify-between items-center mt-2 border-t pt-3">
            <div class="text-xs text-muted">
              流水号：<span class="font-mono">{{ task.id }}</span>
            </div>
            <div class="flex gap-3">
              <button class="btn btn-link text-muted">包含 3 个附件</button>
              <button class="btn btn-secondary text-sm">查看源卷宗</button>
              <button class="btn btn-primary text-sm px-5">前往审批</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';

const mockTasks = reactive([
  { id: 'WF-770921', no: 'CT-2026-B9402', title: '新办公园区弱电工程施工合同', type: '工程建设', node: '法务合规复核', initiator: '项目部 - 张三', time: '10分钟前', urgent: true, isJoint: false },
  { id: 'WF-770918', no: 'CT-2026-A1X91', title: '2026年度云服务框架采购合作协议', type: '软件采购', node: '部门经理初审', initiator: '采购部 - 王五', time: '2小时前', urgent: false, isJoint: true },
  { id: 'WF-770855', no: 'CT-2026-A7851', title: '核心机房第二季度软硬件代维合同', type: '运维服务', node: '财务成本核算', initiator: '运维部 - 李四', time: '昨天 16:30', urgent: false, isJoint: false },
  { id: 'WF-770612', no: 'CT-2026-A1X44', title: '测试用-人事外包补充协议', type: '土建工程', node: '总经理终审', initiator: '人事部 - 赵六', time: '2026-03-12', urgent: false, isJoint: true },
]);
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.mb-6 { margin-bottom: 1.5rem; }
.mb-4 { margin-bottom: 1rem; }
.mb-3 { margin-bottom: 0.75rem; }
.mt-1 { margin-top: 0.25rem; }
.mt-2 { margin-top: 0.5rem; }
.mr-2 { margin-right: 0.5rem; }
.ml-auto { margin-left: auto; }
.p-4 { padding: 1rem; }
.p-5 { padding: 1.25rem; }
.p-3 { padding: 0.75rem; }
.pt-3 { padding-top: 0.75rem; }
.px-5 { padding-left: 1.25rem; padding-right: 1.25rem; }

.border-t { border-top: 1px solid rgba(0,0,0,0.06); }
.bg-app { background-color: var(--bg-app); border-radius: var(--radius-lg); padding: 1rem; }
.rounded-md { border-radius: var(--radius-lg); }

.text-xl { font-size: 1.25rem; }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.font-semibold { font-weight: 600; }
.font-medium { font-weight: 500; }
.font-bold { font-weight: 700; }
.font-mono { font-family: monospace; }
.m-0 { margin: 0; }

.text-muted { color: var(--text-muted); }
.text-main { color: var(--text-main); }
.text-strong { color: var(--text-strong); }
.text-primary { color: var(--primary); }

.flex { display: flex; }
.flex-col { flex-direction: column; }
.gap-2 { gap: 0.5rem; }
.gap-3 { gap: 0.75rem; }
.gap-4 { gap: 1rem; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.items-start { align-items: flex-start; }
.items-end { align-items: flex-end; }

/* Header Badge */
.stats-badge {
  background: rgba(37, 99, 235, 0.1);
  color: var(--text-main);
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-size: 0.875rem;
  border: 1px solid rgba(37, 99, 235, 0.2);
}

/* Layout Grid */
.workflow-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 1.5rem;
  align-items: start;
}

/* Sidebar Nav */
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

/* Task Card */
.task-card {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1);
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

/* Badges & Buttons */
.badge-normal {
  background: var(--border-light);
  color: var(--text-main);
  font-weight: 500;
}

.badge-danger {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #f87171;
}

.btn-link {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
}
.btn-link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
