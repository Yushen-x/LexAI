<template>
  <div class="page-container fade-in">
    <!-- Header & Search -->
    <div class="card mb-6 p-6">
      <div class="flex justify-between items-center mb-4">
        <div>
          <h2 class="text-xl font-semibold text-strong m-0">合同台账</h2>
          <p class="text-sm text-muted mt-1 m-0">
            合同主数据中心：所有合同的总账与生命周期入口，从这里进入起草、AI 审查与归档。
          </p>
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
                <div v-if="nextActions(row).length" class="action-links mt-2">
                  <button
                    v-for="action in nextActions(row)"
                    :key="action.target"
                    type="button"
                    class="action-link"
                    :class="action.tone"
                    :disabled="statusBusyId === row.id"
                    @click="action.handler(row)"
                  >
                    {{ action.label }}
                  </button>
                </div>
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

    <div v-if="selectedContract" class="card detail-card mt-6 p-6">
      <div class="flex justify-between items-start gap-4 mb-4">
        <div>
          <h3 class="text-xl font-semibold text-strong m-0">{{ selectedContract.name }}</h3>
          <p class="text-sm text-muted mt-1 m-0">
            {{ selectedContract.contractNo }} · {{ statusLabel(selectedContract.status) }} · {{ selectedContract.contractType }}
          </p>
        </div>
        <button type="button" class="btn btn-secondary px-4 py-2" @click="closeDetails">关闭详情</button>
      </div>

      <!-- 合同生命周期进度 + 下一步指引 -->
      <div class="lifecycle-card mb-4">
        <div class="lifecycle-track">
          <div
            v-for="(stage, idx) in lifecycleStages"
            :key="stage.key"
            class="lifecycle-step"
            :class="{
              'is-active': stage.key === currentStageKey,
              'is-done': isStageBefore(stage.key, currentStageKey)
            }"
          >
            <div class="lifecycle-dot">{{ idx + 1 }}</div>
            <div class="lifecycle-label">{{ stage.label }}</div>
          </div>
        </div>
        <div class="lifecycle-hint">
          <span class="lifecycle-hint__title">下一步：</span>
          <span class="lifecycle-hint__text">{{ nextStepHint }}</span>
        </div>
      </div>

      <div class="detail-grid mb-4">
        <div class="detail-item">
          <span class="detail-label">甲方</span>
          <span class="detail-value">{{ selectedContract.partyA || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">乙方</span>
          <span class="detail-value">{{ selectedContract.partyB || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">金额</span>
          <span class="detail-value">{{ formatAmount(selectedContract.amount) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">来源</span>
          <span class="detail-value">{{ selectedContract.source || '系统录入' }}</span>
        </div>
      </div>

      <div class="detail-actions mb-4 flex gap-3 flex-wrap">
        <button
          v-if="selectedContract.status === 'DRAFT'"
          type="button"
          class="btn btn-secondary px-4 py-2"
          @click="continueDraft(selectedContract.id)"
        >
          继续编辑
        </button>
        <button
          v-if="selectedContract.status === 'DRAFT' || selectedContract.status === 'UNDER_REVIEW'"
          type="button"
          class="btn btn-primary px-4 py-2"
          @click="openReview(selectedContract.id)"
        >
          {{ selectedContract.status === 'DRAFT' ? '提交 AI 审查' : '查看审查详情' }}
        </button>
        <button
          v-for="action in nextActions(selectedContract)"
          :key="action.target"
          type="button"
          class="btn px-4 py-2"
          :class="action.tone === 'success' ? 'btn-primary' : 'btn-secondary'"
          :disabled="statusBusyId === selectedContract.id"
          @click="action.handler(selectedContract)"
        >
          {{ action.label }}
        </button>
      </div>

      <div class="detail-section">
        <div class="detail-label mb-4">合同正文</div>
        <pre class="detail-content">{{ selectedContract.content || '当前合同暂无正文内容' }}</pre>
      </div>

      <div v-if="selectedContract.latestReview" class="detail-section">
        <div class="detail-label mb-4">最近一次审查结果</div>
        <div class="review-summary-card">
          <div class="review-summary-head">
            <span class="badge badge-warning">{{ reviewDecisionLabel(selectedContract.latestReview.reviewDecision) }}</span>
            <span class="text-sm text-muted">
              {{ selectedContract.latestReview.reviewedAt ? formatTime(selectedContract.latestReview.reviewedAt) : '未记录时间' }}
            </span>
          </div>
          <p class="review-summary-text">
            {{ selectedContract.latestReview.summary || '暂无 AI 审查摘要' }}
          </p>
          <div class="review-stats">
            <span>风险 {{ selectedContract.latestReview.risks.length }} 项</span>
            <span>缺失条款 {{ selectedContract.latestReview.missingClauses.length }} 项</span>
          </div>
          <p v-if="selectedContract.latestReview.reviewerOpinion" class="review-opinion">
            人工意见：{{ selectedContract.latestReview.reviewerOpinion }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchContracts, getContract, updateContractStatus } from '@/shared/api/contracts'
import { CONTRACT_TYPE_VALUES } from '@/shared/constants/contractTypes'
import type { ContractItem, ContractStatus } from '@/shared/types/contracts'

const router = useRouter()
const route = useRoute()

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
const selectedContract = ref<ContractItem | null>(null)

const contractTypes = CONTRACT_TYPE_VALUES

const statusOptions: { value: ContractStatus; label: string }[] = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'UNDER_REVIEW', label: '审查中' },
  { value: 'SIGNED', label: '已签署' },
  { value: 'IN_PROGRESS', label: '执行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'TERMINATED', label: '已终止' },
]

interface ContractAction {
  label: string
  target: ContractStatus
  tone: 'success' | 'warning' | 'danger' | 'muted'
  handler: (row: ContractItem) => void
}

function nextActions(row: ContractItem): ContractAction[] {
  const actions: ContractAction[] = []
  const s = row.status

  if (s === 'DRAFT') {
    actions.push({
      label: '提交审查',
      target: 'UNDER_REVIEW',
      tone: 'success',
      handler: (r) => openReview(r.id)
    })
  }

  if (s === 'UNDER_REVIEW') {
    const decision = row.latestReview?.reviewDecision
    if (decision === 'APPROVED') {
      actions.push({
        label: '标记已签署',
        target: 'SIGNED',
        tone: 'success',
        handler: (r) => doStatusChange(r, 'SIGNED')
      })
    }
  }

  if (s === 'SIGNED') {
    actions.push({
      label: '开始履约',
      target: 'IN_PROGRESS',
      tone: 'success',
      handler: (r) => doStatusChange(r, 'IN_PROGRESS')
    })
  }

  if (s === 'IN_PROGRESS') {
    actions.push({
      label: '标记完成',
      target: 'COMPLETED',
      tone: 'success',
      handler: (r) => doStatusChange(r, 'COMPLETED')
    })
  }

  if (s !== 'COMPLETED' && s !== 'TERMINATED') {
    actions.push({
      label: '终止',
      target: 'TERMINATED',
      tone: 'danger',
      handler: (r) => doStatusChange(r, 'TERMINATED')
    })
  }

  return actions
}

const lifecycleStages: { key: ContractStatus | 'TERMINATED'; label: string }[] = [
  { key: 'DRAFT', label: '草稿' },
  { key: 'UNDER_REVIEW', label: '审查中' },
  { key: 'SIGNED', label: '已签署' },
  { key: 'IN_PROGRESS', label: '执行中' },
  { key: 'COMPLETED', label: '已完成' }
]
const lifecycleOrder: ContractStatus[] = ['DRAFT', 'UNDER_REVIEW', 'SIGNED', 'IN_PROGRESS', 'COMPLETED']

const currentStageKey = computed<ContractStatus>(() => {
  return (selectedContract.value?.status ?? 'DRAFT') as ContractStatus
})

function isStageBefore(stage: ContractStatus, current: ContractStatus): boolean {
  return lifecycleOrder.indexOf(stage) < lifecycleOrder.indexOf(current)
}

const nextStepHint = computed<string>(() => {
  const contract = selectedContract.value
  if (!contract) return ''
  switch (contract.status) {
    case 'DRAFT':
      return '点击「继续编辑」补全正文，或直接「打开审查页」让 AI 给出风险报告。'
    case 'UNDER_REVIEW': {
      const decision = contract.latestReview?.reviewDecision
      if (decision === 'NEEDS_REVISION') {
        return 'AI 已提示存在需要修改的风险，请回到起草页修订后再次发起审查。'
      }
      if (decision === 'APPROVED') {
        return '审查已通过，可在台账中将状态更新为「已签署」。'
      }
      return '请在「打开审查页」中保存人工复核决策（通过 / 退回），完成后待办会自动闭环。'
    }
    case 'SIGNED':
      return '合同已签署，可推进到「执行中」并跟踪履约进展。'
    case 'IN_PROGRESS':
      return '合同正在履约，根据业务情况手动更新为「已完成」。'
    case 'COMPLETED':
      return '合同已归档完结，无需进一步操作。'
    case 'TERMINATED':
      return '合同已终止，仅作为历史记录留存。'
    default:
      return ''
  }
})

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
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function reviewDecisionLabel(decision: string): string {
  switch (decision) {
    case 'APPROVED':
      return '审查通过'
    case 'NEEDS_REVISION':
      return '退回修改'
    case 'PENDING_CONFIRMATION':
    default:
      return '待人工确认'
  }
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

    const routeKeyword = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
    const routeIntent = typeof route.query.intent === 'string' ? route.query.intent : ''
    const routeContractId = typeof route.query.contractId === 'string' ? Number(route.query.contractId) : Number.NaN

    if ((!Number.isInteger(routeContractId) || routeContractId <= 0) && routeKeyword) {
      const exactMatch = res.content.find(
        (item) => item.contractNo === routeKeyword || item.name === routeKeyword
      )
      if (exactMatch) {
        selectedContract.value = await getContract(exactMatch.id)
        if (routeIntent === 'review') {
          router.replace({
            name: 'contractReview',
            query: { contractId: String(exactMatch.id) }
          })
          return
        }
      }
    }

    if (selectedContract.value) {
      const refreshed = res.content.find((item) => item.id === selectedContract.value?.id)
      if (refreshed) {
        selectedContract.value = refreshed
      }
    }
  } catch (e: unknown) {
    errorBanner.value = errMessage(e)
    rows.value = []
    totalElements.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

async function syncRouteSelection(): Promise<void> {
  const rawContractId = route.query.contractId
  const contractId = typeof rawContractId === 'string' ? Number(rawContractId) : Number.NaN
  const routeKeyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''

  if (routeKeyword && routeKeyword !== keyword.value) {
    keyword.value = routeKeyword
  }

  if (!Number.isInteger(contractId) || contractId <= 0) {
    if (!routeKeyword) {
      selectedContract.value = null
    }
    return
  }

  if (selectedContract.value?.id === contractId) {
    return
  }

  try {
    selectedContract.value = await getContract(contractId)
  } catch (e: unknown) {
    errorBanner.value = errMessage(e)
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

async function showDetails(row: ContractItem): Promise<void> {
  selectedContract.value = await getContract(row.id)
  void router.replace({
    name: 'contractList',
    query: {
      ...route.query,
      contractId: String(row.id)
    }
  })
}

function closeDetails(): void {
  selectedContract.value = null
  const nextQuery = { ...route.query }
  delete nextQuery.contractId
  void router.replace({
    name: 'contractList',
    query: nextQuery
  })
}

function continueDraft(contractId: number): void {
  router.push({
    name: 'contractDraft',
    query: { contractId: String(contractId) }
  })
}

function openReview(contractId: number): void {
  router.push({
    name: 'contractReview',
    query: { contractId: String(contractId) }
  })
}

async function doStatusChange(row: ContractItem, target: ContractStatus): Promise<void> {
  if (target === row.status) return
  errorBanner.value = ''
  statusBusyId.value = row.id
  try {
    const updated = await updateContractStatus(row.id, target)
    rows.value = rows.value.map((item) => (item.id === updated.id ? updated : item))
    if (selectedContract.value?.id === updated.id) {
      selectedContract.value = updated
    }
    await loadList()
  } catch (e: unknown) {
    errorBanner.value = errMessage(e)
    await loadList()
  } finally {
    statusBusyId.value = null
  }
}

watch(
  () => route.query.contractId,
  () => {
    void syncRouteSelection()
  }
)

watch(
  () => route.query.keyword,
  (nextKeyword) => {
    const value = typeof nextKeyword === 'string' ? nextKeyword : ''
    if (value === keyword.value) {
      return
    }
    keyword.value = value
    page.value = 0
    void loadList()
  }
)

onMounted(async () => {
  await syncRouteSelection()
  await loadList()
  await syncRouteSelection()
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

.action-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  justify-content: center;
}

.action-link {
  font-size: 0.7rem;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: 1px solid var(--border-light);
  background: var(--bg-surface);
  color: var(--text-main);
  transition: all 0.2s;
  white-space: nowrap;
}

.action-link:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(37, 99, 235, 0.06);
}

.action-link.success {
  color: #16a34a;
  border-color: #bbf7d0;
}
.action-link.success:hover:not(:disabled) {
  background: #dcfce7;
  border-color: #16a34a;
}

.action-link.danger {
  color: #dc2626;
  border-color: #fecaca;
}
.action-link.danger:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #dc2626;
}

.action-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.detail-card {
  border: 1px solid var(--border-light);
}

.lifecycle-card {
  background: var(--bg-app);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 1rem 1.25rem;
}

.lifecycle-track {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  position: relative;
}

.lifecycle-track::before {
  content: '';
  position: absolute;
  top: 14px;
  left: 14px;
  right: 14px;
  height: 2px;
  background: var(--border-light);
  z-index: 0;
}

.lifecycle-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
  z-index: 1;
  color: var(--text-muted);
  font-size: 0.75rem;
  text-align: center;
}

.lifecycle-dot {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: white;
  border: 2px solid var(--border-light);
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.8rem;
  margin-bottom: 0.35rem;
}

.lifecycle-step.is-done .lifecycle-dot {
  background: var(--success, #16a34a);
  border-color: var(--success, #16a34a);
  color: white;
}

.lifecycle-step.is-done .lifecycle-label {
  color: var(--success, #16a34a);
}

.lifecycle-step.is-active .lifecycle-dot {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.15);
}

.lifecycle-step.is-active .lifecycle-label {
  color: var(--primary);
  font-weight: 600;
}

.lifecycle-hint {
  margin-top: 0.85rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--border-light);
  font-size: 0.85rem;
  color: var(--text-main);
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.lifecycle-hint__title {
  color: var(--primary);
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  background: var(--bg-app);
  border-radius: var(--radius-md);
}

.detail-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-value {
  color: var(--text-strong);
  font-size: 0.95rem;
}

.detail-section {
  margin-top: 1rem;
}

.detail-content {
  margin: 0;
  padding: 1rem;
  background: var(--bg-app);
  border-radius: var(--radius-md);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: var(--text-main);
  max-height: 420px;
  overflow: auto;
}

.review-summary-card {
  border: 1px solid var(--border-light);
  background: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 1rem;
}

.review-summary-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.review-summary-text,
.review-opinion {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--text-main);
}

.review-stats {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin: 0.75rem 0;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
