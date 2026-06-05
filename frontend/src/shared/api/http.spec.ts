import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/**
 * http.ts 的核心职责测试：
 *  1. api.get/post/put/del 统一拆解响应信封，返回 data.data；
 *  2. 并发相同 GET 做「在途去重」，只发一次真实请求；
 *  3. 响应拦截器把后端非 SUCCESS 码与各类 HTTP 状态映射为可读错误提示。
 *
 * 通过 mock 掉 axios 实例与 toast，隔离真实网络与 DOM。
 */

const { mockInstance, mockToast } = vi.hoisted(() => {
  return {
    mockInstance: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: { response: { use: vi.fn() } }
    },
    mockToast: vi.fn()
  };
});

vi.mock('axios', () => ({
  default: { create: () => mockInstance },
  AxiosError: class AxiosError extends Error {}
}));

vi.mock('@/shared/ui/toast', () => ({ toast: mockToast }));

// 在 mock 就绪后再导入被测模块，触发 axios.create 与拦截器注册。
import { api } from './http';

// 拦截器注册时传入的 (onFulfilled, onRejected)。
const [onFulfilled, onRejected] = mockInstance.interceptors.response.use.mock.calls[0] as [
  (resp: unknown) => unknown,
  (err: unknown) => Promise<never>
];

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('api 信封拆解', () => {
  it('get 返回 response.data.data', async () => {
    mockInstance.get.mockResolvedValue({ data: { code: 'SUCCESS', data: { id: 1 } } });
    await expect(api.get('/contracts/1')).resolves.toEqual({ id: 1 });
  });

  it('post 返回 response.data.data 并透传 body', async () => {
    mockInstance.post.mockResolvedValue({ data: { data: { ok: true } } });
    await expect(api.post('/contracts', { name: 'x' })).resolves.toEqual({ ok: true });
    expect(mockInstance.post).toHaveBeenCalledWith('/contracts', { name: 'x' }, undefined);
  });

  it('put / del 同样拆解信封', async () => {
    mockInstance.put.mockResolvedValue({ data: { data: 'put-ok' } });
    mockInstance.delete.mockResolvedValue({ data: { data: 'del-ok' } });
    await expect(api.put('/a')).resolves.toBe('put-ok');
    await expect(api.del('/a')).resolves.toBe('del-ok');
  });
});

describe('GET 在途去重', () => {
  it('并发相同 url+params 只发一次真实请求', async () => {
    let resolveFn: (v: unknown) => void = () => {};
    mockInstance.get.mockReturnValue(
      new Promise((resolve) => {
        resolveFn = resolve;
      })
    );

    const p1 = api.get('/contracts', { params: { page: 0 } });
    const p2 = api.get('/contracts', { params: { page: 0 } });

    expect(mockInstance.get).toHaveBeenCalledTimes(1);

    resolveFn({ data: { data: ['c1'] } });
    await expect(p1).resolves.toEqual(['c1']);
    await expect(p2).resolves.toEqual(['c1']);
  });

  it('不同 params 不去重', async () => {
    mockInstance.get.mockResolvedValue({ data: { data: [] } });
    await Promise.all([
      api.get('/contracts', { params: { page: 0 } }),
      api.get('/contracts', { params: { page: 1 } })
    ]);
    expect(mockInstance.get).toHaveBeenCalledTimes(2);
  });

  it('请求完成后从在途池移除，后续相同请求会重新发起', async () => {
    mockInstance.get.mockResolvedValue({ data: { data: 'first' } });
    await api.get('/health');
    await api.get('/health');
    expect(mockInstance.get).toHaveBeenCalledTimes(2);
  });
});

describe('响应拦截器 — 业务码', () => {
  it('code 非 SUCCESS 时 reject 并弹 toast', async () => {
    const resp = { data: { code: 'BIZ_ERROR', message: '合同已归档' } };
    await expect(onFulfilled(resp)).rejects.toThrow('合同已归档');
    expect(mockToast).toHaveBeenCalledWith('合同已归档', 'error');
  });

  it('code 为 SUCCESS 时原样放行', () => {
    const resp = { data: { code: 'SUCCESS', data: 1 } };
    expect(onFulfilled(resp)).toBe(resp);
    expect(mockToast).not.toHaveBeenCalled();
  });

  it('无 code 字段的响应原样放行', () => {
    const resp = { data: { anything: true } };
    expect(onFulfilled(resp)).toBe(resp);
  });
});

describe('响应拦截器 — HTTP 状态映射', () => {
  it.each([
    [401, '认证失败，请重新登录'],
    [403, '暂无权限访问该资源'],
    [404, '请求的资源不存在'],
    [500, '服务器错误，请稍后重试'],
    [503, '服务暂时不可用，请稍后重试']
  ])('状态 %i 映射为「%s」', async (status, message) => {
    await expect(onRejected({ response: { status, data: {} } })).rejects.toThrow(message as string);
    expect(mockToast).toHaveBeenCalledWith(message, 'error');
  });

  it('400/422 优先使用后端 message', async () => {
    await expect(
      onRejected({ response: { status: 400, data: { message: '名称不能为空' } } })
    ).rejects.toThrow('名称不能为空');
  });

  it('有请求无响应时提示网络错误', async () => {
    await expect(onRejected({ request: {} })).rejects.toThrow('网络连接失败，请检查网络设置');
  });

  it('请求构造阶段出错时回落到 error.message', async () => {
    await expect(onRejected({ message: '配置炸了' })).rejects.toThrow('配置炸了');
  });
});
