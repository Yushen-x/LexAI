import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

// 测试专用配置：与 vite.config 共用 @ 别名，使用 jsdom 提供 DOM 环境，
// 以便对依赖 document 的工具（如 toast）以及未来的组件测试做断言。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.ts'],
    clearMocks: true
  }
});
