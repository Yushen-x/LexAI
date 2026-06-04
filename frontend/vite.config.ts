import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 将体积较大的第三方依赖拆为独立 vendor chunk，利用浏览器长缓存、
        // 避免业务代码改动导致整包失效
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-axios': ['axios'],
          'vendor-icons': ['lucide-vue-next']
        }
      }
    }
  }
});

