<template>
  <div class="page-container fade-in">
    <!-- Header & Search -->
    <div class="card mb-6 p-6">
      <div class="flex justify-between items-center mb-4">
        <div>
          <h2 class="text-xl font-semibold text-strong m-0">合同台账与明细查询</h2>
          <p class="text-sm text-muted mt-1 m-0">对接后端分页列表，支持关键词、类型与状态筛选</p>
        </div>
        <button type="button" class="btn btn-primary px-4 py-2" @click="goDraft">
          <span class="icon mr-2">➕</span>新建合同
        </button>
      </div>

      <!-- Advanced Filter Bar -->
      <div class="filter-bar flex gap-4 pt-4 border-t items-end">
        <div class="form-group flex-1">
          <label class="filter-label">合同关键字</label>
          <input
            v-model.trim="keyword"
            type="text"
            class="form-input"
            placeholder="名称或编号…"
            @keyup.enter="applyFilters"
          />
        </div>
        <div class="form-group flex-1">
          <label class="filter-label">合同类型</label>
          <select v-model="contractType" class="form-input">
            <option value="">全部类型</option>
            <option v-for="t in contractTypes" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-group flex-1">
          <label class="filter-label">当前状态</label>
          <select v-model="status" class="form-input">
            <option value="">全部状态</option>
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="form-group">
          <button type="button" class="btn btn-secondary px-6" :disabled="loading" @click="resetFilters">重 置</button>
        </div>
        <div class="form-group">
          <button type="button" class="btn btn-primary px-6" :disabled="loading" @click="applyFilters">查 询</button>
        </div>
      </div>
    </div>

    <!-- Data Table Card -->
    <div class="card overflow-hidden">
      <div class="card-header pb-4 bg-app flex justify-between items-center">
        <div class="flex items-center gap-3 flex-wrap">
          <span class="text-sm font-medium text-main">
            共 {{ totalElements }} 条，第 {{ page + 1 }} / {{ totalPages || 1 }} 页
          </span>
          <span v-if="errorBanner" class="text-sm text-danger">{{ errorBanner }}</span>
        </div>
      </div>

      <div class="table-responsive" :class="{ 'opacity-60': loading }">
        <table class="data-table text-sm w-full">
          <thead>
            <tr>
              <th width="14%" class="text-left">合同编号</th>
              <th width="22%" class="text-left">合同名称</th>
              <th width="12%" class="text-left">类型</th>
              <th width="18%" class="text-left">相对方</th>
              <th width="12%" class="text-right">金额 (元)</th>
              <th width="12%" class="text-center">状态</th>
              <th width="10%" class="text-center">更新</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && rows.length === 0">
              <td colspan="7" class="text-center text-muted py-8">暂无数据</td>
            </tr>
            <tr v-for="row in rows" :key="row.id" class="table-row">
              <td class="font-mono text-muted">{{ row.contractNo }}</td>
              <td class="font-medium text-strong">
                <a href="javascript:void(0)" class="text-primary hover-underline" @click.prevent="showDetails(row)">{{ row.name }}</a>
              </td>
              <td>
                <span class="badge badge-normal">{{ row.contractType }}</span>
              </td>
              <td class="text-muted text-xs">{{ row.partyB }}</td>
              <td class="text-right font-mono">{{ formatAmount(row.amount) }}</td>
              <td class="text-center">
                <span class="status-indicator" :class="'status-' + statusTone(row.status)"></span>
                <span :class="'text-' + statusTone(row.status)">{{ statusLabel(row.status) }}</span>
                <select
                  class="form-input text-xs mt-2 status-select"
                  :value="row.status"
                  :disabled="statusBusyId === row.id"
                  @change="onStatusChange(row, ($event.target as HTMLSelectElement).value)"
                >
                  <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </td>
              <td class="text-center text-muted">{{ formatTime(row.updatedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="card-footer pt-4 border-t flex justify-end items-center text-sm flex-wrap gap-3">
        <span class="text-muted">每页 {{ size }} 条</span>
        <div class="pagination flex gap-1">
          <button
            type="button"
            class="page-btn"
            :class="{ disabled: loading || page <= 0 }"
            :disabled="loading || page <= 0"
            @click="goPage(page - 1)"
          >
            &lt;
          </button>
          <button type="button" class="page-btn active">{{ page + 1 }}</button>
          <button
            type="button"
            class="page-btn"
            :class="{ disabled: loading || page >= totalPages - 1 }"
            :disabled="loading || page >= totalPages - 1 || totalPages === 0"
            @click="goPage(page + 1)"
          >
            &gt;
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchContracts, updateContractStatus } from '@/shared/api/contracts'
import type { ContractItem, ContractStatus } from '@/shared/types/contracts'

const router = useRouter()

const loading = ref(false)
const statusBusyId = ref<number | null>(null)
const errorBanner = ref('')

const keyword = ref('')
const contractType = ref('')
const status = ref<'' | ContractStatus>('')

const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const rows = ref<ContractItem[]>([])

const contractTypes = [
  '采购合同',
  '工程合同',
  '服务合同',
  '保密/期权',
  '运维服务',
  '租赁合同',
  '技术开发',
]

const statusOptions: { value: ContractStatus; label: string }[] = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'UNDER_REVIEW', label: '审查中' },
  { value: 'SIGNED', label: '已签署' },
  { value: 'IN_PROGRESS', label: '执行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'TERMINATED', label: '已终止' },
]

const query = computed(() => ({
  page: page.value,
  size: size.value,
  keyword: keyword.value || undefined,
  type: contractType.value || undefined,
  status: (status.value || undefined) as ContractStatus | undefined,
}))

function statusLabel(s: ContractStatus): string {
  return statusOptions.find((o) => o.value === s)?.label ?? s
}

function statusTone(s: ContractStatus): 'success' | 'warning' | 'danger' | 'muted' {
  switch (s) {
    case 'DRAFT':
      return 'muted'
    case 'UNDER_REVIEW':
      return 'warning'
    case 'SIGNED':
    case 'IN_PROGRESS':
    case 'COMPLETED':
      return 'success'
    case 'TERMINATED':
      return 'danger'
    default:
      return 'muted'
  }
}

function formatAmount(amount: number): string {
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(amount)
}

function formatTime(iso: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function errMessage(e: unknown): string {
  if (e instanceof Error) return e.message
  return '操作失败，请重试'
}

async function loadList(): Promise<void> {
  errorBanner.value = ''
  loading.value = true
  try {
    const res = await fetchContracts(query.value)
    rows.value = res.content
    totalElements.value = res.totalElements
    totalPages.value = res.totalPages === 0 ? 1 : res.totalPages
  } catch (e: unknown) {
    errorBanner.value = errMessage(e)
    rows.value = []
    totalElements.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

function applyFilters(): void {
  page.value = 0
  void loadList()
}

function resetFilters(): void {
  keyword.value = ''
  contractType.value = ''
  status.value = ''
  page.value = 0
  void loadList()
}

function goPage(p: number): void {
  if (p < 0 || p > totalPages.value - 1) return
  page.value = p
  void loadList()
}

function goDraft(): void {
  router.push({ name: 'contractDraft' })
}

function showDetails(row: ContractItem): void {
  const lines = [
    `编号：${row.contractNo}`,
    `名称：${row.name}`,
    `类型：${row.contractType}`,
    `甲方：${row.partyA}`,
    `乙方：${row.partyB}`,
    `金额：${formatAmount(row.amount)}`,
    `状态：${statusLabel(row.status)}`,
    row.source ? `来源：${row.source}` : '',
  ].filter(Boolean)
  window.alert(lines.join('\n'))
}

async function onStatusChange(row: ContractItem, next: string): Promise<void> {
  const newStatus = next as ContractStatus
  if (newStatus === row.status) return
  errorBanner.value = ''
  statusBusyId.value = row.id
  try {
    await updateContractStatus(row.id, newStatus)
    await loadList()
  } catch (e: unknown) {
    errorBanner.value = errMessage(e)
    await loadList()
  } finally {
    statusBusyId.value = null
  }
}

onMounted(() => {
  void loadList()
})
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
.mt-2 { margin-top: 0.5rem; }
.mr-2 { margin-right: 0.5rem; }
.py-8 { padding-top: 2rem; padding-bottom: 2rem; }
.pt-4 { padding-top: 1rem; }
.pb-4 { padding-bottom: 1rem; }
.p-6 { padding: 1.5rem; }

.border-t { border-top: 1px solid var(--border-light); }
.bg-app { background-color: var(--bg-app); }
.overflow-hidden { overflow: hidden; }
.opacity-60 { opacity: 0.65; }

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
.flex-wrap { flex-wrap: wrap; }
.gap-1 { gap: 0.25rem; }
.gap-3 { gap: 0.75rem; }
.gap-4 { gap: 1rem; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.hover-underline:hover { text-decoration: underline; }

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

.status-select {
  max-width: 7.5rem;
  margin-left: auto;
  margin-right: auto;
  display: block;
}

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
