import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import { toast } from '@/shared/ui/toast';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const http = axios.create({
  baseURL,
  timeout: 60000
});

/** 后端统一响应信封：{ code, message, data }。 */
interface ApiEnvelope<T> {
  code?: string;
  message?: string;
  data: T;
}

/**
 * 同一时刻进行中的 GET 请求池，用于「在途去重」：
 * 多个组件并发请求相同 URL+参数时，复用同一个 Promise，避免重复网络往返。
 */
const inFlightGets = new Map<string, Promise<unknown>>();

function requestKey(url: string, params?: unknown): string {
  return params === undefined ? url : `${url}?${JSON.stringify(params)}`;
}

/**
 * 轻量 API 客户端：统一拆解响应信封（返回 `data.data`），消除各模块重复的 `.data.data` 样板；
 * GET 默认做在途去重。写操作（post/put/del）不去重，避免吞掉用户的重复提交意图。
 */
export const api = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const key = requestKey(url, config?.params);
    const existing = inFlightGets.get(key) as Promise<T> | undefined;
    if (existing) {
      return existing;
    }
    const request = http
      .get<ApiEnvelope<T>>(url, config)
      .then((response) => response.data.data)
      .finally(() => inFlightGets.delete(key));
    inFlightGets.set(key, request);
    return request;
  },
  async post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.post<ApiEnvelope<T>>(url, body, config);
    return response.data.data;
  },
  async put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.put<ApiEnvelope<T>>(url, body, config);
    return response.data.data;
  },
  async del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.delete<ApiEnvelope<T>>(url, config);
    return response.data.data;
  }
};

// Response interceptor for error handling
http.interceptors.response.use(
  (response) => {
    const body: any = response?.data;
    if (body && typeof body === 'object' && 'code' in body) {
      const code = body.code;
      if (code && code !== 'SUCCESS') {
        const msg = body.message || '请求失败，请重试';
        toast(msg, 'error');
        return Promise.reject(new Error(msg));
      }
    }
    return response;
  },
  (error: AxiosError) => {
    let errorMessage = '请求失败，请重试';

    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const data = error.response.data as any;

      switch (status) {
        case 400:
          errorMessage = data?.message || '请求参数错误';
          break;
        case 401:
          errorMessage = '认证失败，请重新登录';
          break;
        case 403:
          errorMessage = '暂无权限访问该资源';
          break;
        case 404:
          errorMessage = '请求的资源不存在';
          break;
        case 422:
          errorMessage = data?.message || '数据验证失败';
          break;
        case 500:
          errorMessage = '服务器错误，请稍后重试';
          break;
        case 503:
          errorMessage = '服务暂时不可用，请稍后重试';
          break;
        default:
          errorMessage = data?.message || `请求失败 (${status})`;
      }
    } else if (error.request) {
      // Request made but no response
      errorMessage = '网络连接失败，请检查网络设置';
    } else {
      // Error in request setup
      errorMessage = error.message || '请求配置出错';
    }

    console.error('API Error:', errorMessage);
    toast(errorMessage, 'error');
    
    return Promise.reject(new Error(errorMessage));
  }
);

