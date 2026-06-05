import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockApi } = vi.hoisted(() => ({
  mockApi: { get: vi.fn(), post: vi.fn(), put: vi.fn(), del: vi.fn() }
}));
vi.mock('@/shared/api/http', () => ({ api: mockApi }));

import { fetchTasks, getTask, updateTaskStatus } from './tasks';

beforeEach(() => {
  vi.clearAllMocks();
  mockApi.get.mockResolvedValue(undefined);
  mockApi.put.mockResolvedValue(undefined);
});

describe('tasks api', () => {
  it('fetchTasks 无状态时传空参数对象', async () => {
    await fetchTasks();
    expect(mockApi.get).toHaveBeenCalledWith('/tasks', { params: {} });
  });

  it('fetchTasks 带状态时作为查询参数下发', async () => {
    await fetchTasks('PENDING' as never);
    expect(mockApi.get).toHaveBeenCalledWith('/tasks', { params: { status: 'PENDING' } });
  });

  it('getTask 命中详情路径', async () => {
    await getTask(12);
    expect(mockApi.get).toHaveBeenCalledWith('/tasks/12');
  });

  it('updateTaskStatus 走 status 子资源', async () => {
    await updateTaskStatus(12, 'DONE' as never);
    expect(mockApi.put).toHaveBeenCalledWith('/tasks/12/status', { status: 'DONE' });
  });
});
