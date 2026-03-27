import axios, { AxiosError } from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const http = axios.create({
  baseURL,
  timeout: 15000
});

// Response interceptor for error handling
http.interceptors.response.use(
  response => response,
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

    // Show error toast/notification
    console.error('API Error:', errorMessage);
    
    // You can integrate with a toast notification library here
    // e.g., ElMessage.error(errorMessage);
    
    return Promise.reject(new Error(errorMessage));
  }
);

