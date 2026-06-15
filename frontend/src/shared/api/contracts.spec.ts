import { beforeEach, describe, expect, it, vi } from 'vitest';

// 仅验证各 API 函数对 url 与查询参数的拼装/清洗逻辑，底层 api 客户端被 mock。
const { mockApi } = vi.hoisted(() => ({
  mockApi: { get: vi.fn(), post: vi.fn(), put: vi.fn(), del: vi.fn() }
}));
vi.mock('@/shared/api/http', () => ({ api: mockApi }));

import {
  createContract,
  fetchContractStatistics,
  fetchContracts,
  getContract,
  updateContract,
  updateContractReview,
  updateContractStatus
} from './contracts';

beforeEach(() => {
  vi.clearAllMocks();
  mockApi.get.mockResolvedValue(undefined);
  mockApi.post.mockResolvedValue(undefined);
  mockApi.put.mockResolvedValue(undefined);
});

describe('fetchContracts 参数清洗', () => {
  it('空字符串过滤为 undefined，页码/页长落默认值', async () => {
    await fetchContracts({ keyword: '', status: undefined, type: '' });
    expect(mockApi.get).toHaveBeenCalledWith('/contracts', {
      params: { keyword: undefined, status: undefined, type: undefined, page: 0, size: 20 }
    });
  });

  it('透传非空筛选条件与自定义分页', async () => {
    await fetchContracts({ keyword: '采购', type: '采购合同', page: 2, size: 50 });
    expect(mockApi.get).toHaveBeenCalledWith('/contracts', {
      params: { keyword: '采购', status: undefined, type: '采购合同', page: 2, size: 50 }
    });
  });
});

describe('合同单项操作的 url 拼装', () => {
  it('getContract 命中详情路径', async () => {
    await getContract(7);
    expect(mockApi.get).toHaveBeenCalledWith('/contracts/7');
  });

  it('updateContractStatus 走 status 子资源', async () => {
    await updateContractStatus(7, 'ARCHIVED' as never);
    expect(mockApi.put).toHaveBeenCalledWith('/contracts/7/status', { status: 'ARCHIVED' });
  });

  it('updateContractReview 走 review 子资源并透传 payload', async () => {
    const payload = { reviewerOpinion: '通过', reviewDecision: 'APPROVED' as never };
    await updateContractReview(7, payload);
    expect(mockApi.put).toHaveBeenCalledWith('/contracts/7/review', payload);
  });

  it('createContract / updateContract 命中集合与详情路径', async () => {
    const payload = { name: 'x', contractType: '服务合同' };
    await createContract(payload);
    await updateContract(7, payload);
    expect(mockApi.post).toHaveBeenCalledWith('/contracts', payload);
    expect(mockApi.put).toHaveBeenCalledWith('/contracts/7', payload);
  });

  it('fetchContractStatistics 命中统计端点', async () => {
    await fetchContractStatistics();
    expect(mockApi.get).toHaveBeenCalledWith('/contracts/statistics');
  });
});
