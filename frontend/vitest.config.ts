import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';

// 测试专用配置：与 vite.config 共用 @ 别名，使用 jsdom 提供 DOM 环境，
// 以便对依赖 document 的工具（如 toast / clipboard）以及组件测试做断言。
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
    clearMocks: true,
    setupFiles: ['./tests/setup.ts'],
    // 同时收集 src 内联测试与 tests 目录下的测试
    include: ['src/**/*.{test,spec}.ts', 'tests/**/*.{test,spec}.ts'],
    coverage: {
      provider: 'v8',
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,vue}'],
      exclude: ['src/main.ts', 'src/env.d.ts', 'src/**/*.d.ts']
    }
  }
});
