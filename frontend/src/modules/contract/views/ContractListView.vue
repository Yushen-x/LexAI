<template>
  <div class="page-container fade-in">
    <!-- Header & Search -->
    <div class="card mb-6 p-6">
      <div class="flex justify-between items-center mb-4">
        <div>
          <h2 class="text-xl font-semibold text-strong m-0">合同台账与明细查询</h2>
          <p class="text-sm text-muted mt-1 m-0">All Contracts Management and Tracking</p>
        </div>
        <button class="btn btn-primary px-4 py-2">
          <span class="icon mr-2">➕</span>新建合同
        </button>
      </div>

      <!-- Advanced Filter Bar -->
      <div class="filter-bar flex gap-4 pt-4 border-t items-end">
        <div class="form-group flex-1">
          <label class="filter-label">合同关键字</label>
          <input type="text" class="form-input" placeholder="输入合同名称、编号搜寻..." />
        </div>
        <div class="form-group flex-1">
          <label class="filter-label">管理台账视角</label>
          <select class="form-input">
            <option value="ALL">全部合同</option>
            <option value="MINE">我起草的</option>
            <option value="DEPARTMENT">本部门的数据</option>
          </select>
        </div>
        <div class="form-group flex-1">
          <label class="filter-label">处理状态</label>
          <select class="form-input">
            <option value="">全部状态</option>
            <option value="0">起草中 / 草稿</option>
            <option value="1">审批流转中</option>
            <option value="2">已生效 / 归档</option>
            <option value="3">已驳回</option>
          </select>
        </div>
        <div class="form-group">
          <button class="btn btn-secondary px-6">重 置</button>
        </div>
        <div class="form-group">
          <button class="btn btn-primary px-6">查 询</button>
        </div>
      </div>
    </div>

    <!-- Data Table Card -->
    <div class="card overflow-hidden">
      <div class="card-header pb-4 bg-app flex justify-between items-center">
        <div class="flex items-center gap-3">
          <span class="text-sm font-medium text-main">共找到 12 条关联数据</span>
        </div>
        <button class="btn btn-icon btn-secondary text-sm">
          <span class="icon mr-1">📥</span> 导出本页报表
        </button>
      </div>
      
      <div class="table-responsive">
        <table class="data-table text-sm w-full">
          <thead>
            <tr>
              <th width="15%" class="text-left">合同编号</th>
              <th width="25%" class="text-left">合同名称</th>
              <th width="15%" class="text-left">合同类型</th>
              <th width="15%" class="text-right">涉及金额 (元)</th>
              <th width="10%" class="text-center">当前状态</th>
              <th width="15%" class="text-center">更新时间</th>
              <th width="10%" class="text-center">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in mockData" :key="row.id" class="table-row">
              <td class="font-mono text-muted">{{ row.no }}</td>
              <td class="font-medium text-strong">
                <a href="javascript:void(0)" class="text-primary hover-underline">{{ row.name }}</a>
              </td>
              <td>
                <span class="badge badge-normal">{{ row.typeStr }}</span>
              </td>
              <td class="text-right font-mono">{{ formatCurrency(row.amount) }}</td>
              <td class="text-center">
                <span class="status-indicator" :class="'status-' + row.statusColor"></span>
                <span :class="'text-' + row.statusColor">{{ row.statusStr }}</span>
              </td>
              <td class="text-center text-muted">{{ row.time }}</td>
              <td class="text-center">
                <button class="btn btn-link text-primary mr-2">详情</button>
                <button v-if="row.status === 0" class="btn btn-link text-primary">续编</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Pagination -->
      <div class="card-footer pt-4 border-t flex justify-end items-center text-sm">
        <span class="text-muted mr-4">当前显示第 1-10 条</span>
        <div class="pagination flex gap-1">
          <button class="page-btn disabled">&lt;</button>
          <button class="page-btn active">1</button>
          <button class="page-btn">2</button>
          <button class="page-btn">&gt;</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';

const mockData = reactive([
  { id: 1, no: 'CT-2026-A1X91', name: '2026年度云服务框架采购合作协议', typeStr: '采购合同', amount: 1550000, status: 1, statusStr: '流转中', statusColor: 'warning', time: '今天 10:24' },
  { id: 2, no: 'CT-2026-B9402', name: '新办公园区弱电工程施工合同', typeStr: '工程合同', amount: 890000, status: 2, statusStr: '已生效', statusColor: 'success', time: '昨天 15:30' },
  { id: 3, no: 'CT-2026-A1X44', name: '测试用-人事外包补充协议', typeStr: '服务合同', amount: 50000, status: 0, statusStr: '起草中', statusColor: 'muted', time: '2026-03-12' },
  { id: 4, no: 'CT-2026-C8221', name: '高管竞业禁止与保密期权协议', typeStr: 'NDA', amount: 0, status: 3, statusStr: '已驳回', statusColor: 'danger', time: '2026-03-10' },
  { id: 5, no: 'CT-2026-A7851', name: '核心机房第二季度软硬件代维合同', typeStr: '代维合同', amount: 245000, status: 2, statusStr: '已生效', statusColor: 'success', time: '2026-03-01' },
]);

const formatCurrency = (val: number) => {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(val);
};
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.mb-6 { margin-bottom: 1.5rem; }
.mb-4 { margin-bottom: 1rem; }
.mt-1 { margin-top: 0.25rem; }
.mr-2 { margin-right: 0.5rem; }
.mr-4 { margin-right: 1rem; }
.pt-4 { padding-top: 1rem; }
.pb-4 { padding-bottom: 1rem; }
.p-6 { padding: 1.5rem; }

.border-t { border-top: 1px solid var(--border-light); }
.bg-app { background-color: var(--bg-app); }
.overflow-hidden { overflow: hidden; }

.text-xl { font-size: 1.25rem; }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
.font-semibold { font-weight: 600; }
.font-medium { font-weight: 500; }
.font-mono { font-family: monospace; }
.m-0 { margin: 0; }
.w-full { width: 100%; }

.text-muted { color: var(--text-muted); }
.text-main { color: var(--text-main); }
.text-strong { color: var(--text-strong); }
.text-primary { color: var(--primary); }
.text-success { color: #16a34a; }
.text-warning { color: #ca8a04; }
.text-danger { color: #dc2626; }

.text-left { text-align: left; }
.text-center { text-align: center; }
.text-right { text-align: right; }

.flex { display: flex; }
.flex-1 { flex: 1; }
.gap-1 { gap: 0.25rem; }
.gap-3 { gap: 0.75rem; }
.gap-4 { gap: 1rem; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.hover-underline:hover { text-decoration: underline; }

/* Filter Bar */
.filter-label {
  display: block;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-app);
  color: var(--text-main);
  outline: none;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Data Table */
.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.data-table th {
  background-color: var(--bg-app);
  color: var(--text-muted);
  font-weight: 600;
  padding: 1rem 1.25rem;
  border-bottom: none;
  position: sticky;
  top: 0;
}

.data-table td {
  padding: 1.25rem;
  border-bottom: 1px solid rgba(0,0,0,0.04);
  vertical-align: middle;
}

.table-row {
  transition: background-color 0.3s ease;
}

.table-row:hover {
  background-color: rgba(0,0,0,0.02);
}

/* Status Indicator */
.status-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-success { background-color: #16a34a; }
.status-warning { background-color: #eab308; }
.status-danger { background-color: #ef4444; }
.status-muted { background-color: #94a3b8; }

.badge-normal {
  background: var(--border-light);
  color: var(--text-main);
}

.btn-link {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  font-weight: 500;
}
.btn-link:hover { text-decoration: underline; }

/* Pagination */
.page-btn {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  min-width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-main);
  transition: all 0.2s;
}

.page-btn:hover:not(.disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.page-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--bg-app);
}
</style>
