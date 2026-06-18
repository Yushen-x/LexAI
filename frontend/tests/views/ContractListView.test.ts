import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import ContractListView from '@/modules/contract/views/ContractListView.vue';
import {
  fetchContractStatistics,
  fetchContracts,
  getContract,
  updateContractStatus
} from '@/shared/api/contracts';
import { confirmAction } from '@/shared/ui/confirm';
import type { ContractItem, ContractStatus } from '@/shared/types/contracts';

const push = vi.fn();
const replace = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push, replace }),
  useRoute: () => ({ query: {} })
}));

vi.mock('@/shared/api/contracts', () => ({
  fetchContracts: vi.fn(),
  fetchContractStatistics: vi.fn(),
  getContract: vi.fn(),
  updateContractStatus: vi.fn()
}));

vi.mock('@/shared/ui/confirm', () => ({
  confirmAction: vi.fn()
}));

const fetchListMock = vi.mocked(fetchContracts);
const fetchStatsMock = vi.mocked(fetchContractStatistics);
const updateStatusMock = vi.mocked(updateContractStatus);
const getContractMock = vi.mocked(getContract);
const confirmMock = vi.mocked(confirmAction);

function contract(overrides: Partial<ContractItem> = {}): ContractItem {
  return {
    id: 1,
    contractNo: 'HT-001',
    name: '测试采购合同',
    contractType: '采购合同',
    partyA: '甲方',
    partyB: '乙方',
    amount: 1000,
    content: '正文',
    status: 'DRAFT',
    source: null,
    latestReview: null,
    createdAt: '2026-06-01T00:00:00Z',
    updatedAt: '2026-06-01T00:00:00Z',
    ...overrides
  };
}

function listResult(content: ContractItem[], totalPages = 1, totalElements = content.length) {
  return { content, totalElements, totalPages, page: 0, size: 10 };
}

function actionButton(wrapper: any, label: string) {
  return wrapper.findAll('button.action-link').find((b: any) => b.text().includes(label));
}

beforeEach(() => {
  push.mockReset();
  replace.mockReset();
  fetchListMock.mockReset();
  fetchStatsMock.mockReset();
  updateStatusMock.mockReset();
  getContractMock.mockReset();
  confirmMock.mockReset();
  confirmMock.mockResolvedValue(true);
  fetchStatsMock.mockResolvedValue({
    total: 1,
    statusDistribution: [{ status: 'DRAFT', count: 1 }],
    typeDistribution: [],
    monthlyTrend: []
  });
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('ContractListView', () => {
  it('渲染合同列表行', async () => {
    fetchListMock.mockResolvedValue(listResult([contract()]));
    const wrapper = mount(ContractListView);
    await flushPromises();
    expect(fetchListMock).toHaveBeenCalled();
    expect(wrapper.text()).toContain('测试采购合同');
  });

  it('草稿合同展示「提交审查」「终止」流转按钮', async () => {
    fetchListMock.mockResolvedValue(listResult([contract({ status: 'DRAFT' })]));
    const wrapper = mount(ContractListView);
    await flushPromises();
    expect(actionButton(wrapper, '提交审查')).toBeTruthy();
    expect(actionButton(wrapper, '终止')).toBeTruthy();
  });

  it('点击「提交审查」跳转到合同审查页', async () => {
    fetchListMock.mockResolvedValue(listResult([contract({ id: 42, status: 'DRAFT' })]));
    const wrapper = mount(ContractListView);
    await flushPromises();

    await actionButton(wrapper, '提交审查')!.trigger('click');
    expect(push).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'contractReview',
        query: { contractId: '42' }
      })
    );
  });

  it('点击「终止」二次确认通过后调用 updateContractStatus(TERMINATED)', async () => {
    const draft = contract({ id: 7, status: 'DRAFT' });
    fetchListMock.mockResolvedValue(listResult([draft]));
    updateStatusMock.mockResolvedValue({ ...draft, status: 'TERMINATED' as ContractStatus });

    const wrapper = mount(ContractListView);
    await flushPromises();

    await actionButton(wrapper, '终止')!.trigger('click');
    await flushPromises();
    expect(confirmMock).toHaveBeenCalled();
    expect(updateStatusMock).toHaveBeenCalledWith(7, 'TERMINATED');
  });

  it('终止确认弹窗被取消时不调用接口', async () => {
    confirmMock.mockResolvedValue(false);
    const draft = contract({ id: 7, status: 'DRAFT' });
    fetchListMock.mockResolvedValue(listResult([draft]));

    const wrapper = mount(ContractListView);
    await flushPromises();

    await actionButton(wrapper, '终止')!.trigger('click');
    await flushPromises();
    expect(confirmMock).toHaveBeenCalled();
    expect(updateStatusMock).not.toHaveBeenCalled();
  });

  it('已通过审查的合同可「标记已签署」', async () => {
    const reviewed = contract({
      id: 9,
      status: 'UNDER_REVIEW',
      latestReview: {
        summary: '',
        risks: [],
        missingClauses: [],
        reviewerOpinion: '',
        reviewDecision: 'APPROVED',
        reviewedAt: null
      }
    });
    fetchListMock.mockResolvedValue(listResult([reviewed]));
    const wrapper = mount(ContractListView);
    await flushPromises();
    expect(actionButton(wrapper, '标记已签署')).toBeTruthy();
  });

  it('分页：首页上一页禁用，点击下一页按新页码请求', async () => {
    fetchListMock.mockResolvedValue(listResult([contract()], 3, 25));
    const wrapper = mount(ContractListView);
    await flushPromises();

    const pageBtns = wrapper.findAll('.page-btn');
    const prev = pageBtns.find((b) => b.text().includes('<'))!;
    const next = pageBtns.find((b) => b.text().includes('>'))!;
    expect(prev.attributes('disabled')).toBeDefined();
    expect(next.attributes('disabled')).toBeUndefined();

    fetchListMock.mockClear();
    await next.trigger('click');
    await flushPromises();
    expect(fetchListMock).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }));
  });
});
