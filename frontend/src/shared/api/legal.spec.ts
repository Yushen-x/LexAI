import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockApi } = vi.hoisted(() => ({
  mockApi: { get: vi.fn(), post: vi.fn(), put: vi.fn(), del: vi.fn() }
}));
vi.mock('@/shared/api/http', () => ({ api: mockApi }));

import {
  fetchLegalSessionDetail,
  fetchLegalSessions,
  fetchRecentLegalSessions,
  submitConsultation
} from './legal';

beforeEach(() => {
  vi.clearAllMocks();
  mockApi.get.mockResolvedValue(undefined);
  mockApi.post.mockResolvedValue(undefined);
});

describe('fetchLegalSessions 分页与关键字处理', () => {
  it('使用默认分页，关键字缺省为 undefined', async () => {
    await fetchLegalSessions('CONSULTATION' as never);
    expect(mockApi.get).toHaveBeenCalledWith('/legal/sessions', {
      params: { type: 'CONSULTATION', page: 0, size: 20, keyword: undefined }
    });
  });

  it('关键字两端空白被 trim，纯空白回落 undefined', async () => {
    await fetchLegalSessions('CONSULTATION' as never, 1, 10, '  劳动合同  ');
    expect(mockApi.get).toHaveBeenCalledWith('/legal/sessions', {
      params: { type: 'CONSULTATION', page: 1, size: 10, keyword: '劳动合同' }
    });

    await fetchLegalSessions('CONSULTATION' as never, 0, 20, '   ');
    expect(mockApi.get).toHaveBeenLastCalledWith('/legal/sessions', {
      params: { type: 'CONSULTATION', page: 0, size: 20, keyword: undefined }
    });
  });
});

describe('legal 其他端点', () => {
  it('fetchRecentLegalSessions 默认 limit=5', async () => {
    await fetchRecentLegalSessions();
    expect(mockApi.get).toHaveBeenCalledWith('/legal/sessions/recent', { params: { limit: 5 } });
  });

  it('fetchLegalSessionDetail 命中详情路径', async () => {
    await fetchLegalSessionDetail(99);
    expect(mockApi.get).toHaveBeenCalledWith('/legal/sessions/99');
  });

  it('submitConsultation 透传 payload 到咨询端点', async () => {
    const payload = { question: '能否解除合同？' };
    await submitConsultation(payload as never);
    expect(mockApi.post).toHaveBeenCalledWith('/legal/consultation', payload);
  });
});
