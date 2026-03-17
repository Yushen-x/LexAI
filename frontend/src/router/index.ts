import { createRouter, createWebHistory } from 'vue-router';
import AppShell from '@/shared/layout/AppShell.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/',
      component: AppShell,
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/modules/dashboard/views/DashboardView.vue'),
          meta: { title: '工作台' }
        },
        {
          path: 'consultation',
          name: 'consultation',
          component: () => import('@/modules/consultation/views/ConsultationView.vue'),
          meta: { title: '法律咨询' }
        },
        {
          path: 'case-analysis',
          name: 'caseAnalysis',
          component: () => import('@/modules/case-analysis/views/CaseAnalysisView.vue'),
          meta: { title: '案件分析' }
        },
        {
          path: 'contract-review',
          name: 'contractReview',
          component: () => import('@/modules/contract-review/views/ContractReviewView.vue'),
          meta: { title: '合同审查' }
        },
        {
          path: 'contract-draft',
          name: 'contractDraft',
          component: () => import('@/modules/contract/views/ContractDraftView.vue'),
          meta: { title: '合同智能起草' }
        },
        {
          path: 'contract-list',
          name: 'contractList',
          component: () => import('@/modules/contract/views/ContractListView.vue'),
          meta: { title: '合同台账' }
        },
        {
          path: 'workflow-pending',
          name: 'workflowPending',
          component: () => import('@/modules/workflow/views/WorkflowPendingView.vue'),
          meta: { title: '待办任务' }
        }
      ]
    }
  ]
});

export default router;
