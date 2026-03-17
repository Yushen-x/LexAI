<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="app-sidebar">
      <div class="sidebar-header">
        <RouterLink to="/" class="brand-link">
          <div class="brand-logo">L</div>
          <div class="brand-text">
            <span class="brand-title">LexAI</span>
            <span class="brand-subtitle">Enterprise Legal</span>
          </div>
        </RouterLink>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          active-class="nav-link-active"
        >
          {{ item.title }}
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="user-profile">
          <div class="avatar">U</div>
          <div class="user-info">
            <div class="user-name">User</div>
            <div class="user-role">Admin Workspace</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="app-main">
      <header class="app-header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <span class="badge badge-primary">Demo Environment</span>
        </div>
      </header>

      <main class="page-content">
        <RouterView v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const navItems = [
  {
    to: '/dashboard',
    title: '工作台 (Dashboard)'
  },
  {
    to: '/contract-draft',
    title: '合同起草 (AI Generate)'
  },
  {
    to: '/contract-list',
    title: '合同台账 (Contract List)'
  },
  {
    to: '/workflow-pending',
    title: '待办任务 (Workflow)'
  },
  {
    to: '/consultation',
    title: '法律咨询 (Consultation)'
  },
  {
    to: '/case-analysis',
    title: '案件分析 (Analysis)'
  },
  {
    to: '/contract-review',
    title: '合同审查 (Review)'
  }
];

const pageTitle = computed(() => String(route.meta.title ?? '概览'));
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background-color: var(--bg-app);
}

.app-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: linear-gradient(180deg, var(--sidebar-bg-from) 0%, var(--sidebar-bg-to) 100%);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  color: var(--sidebar-text);
  z-index: 20;
  box-shadow: 4px 0 24px rgba(0,0,0,0.1);
}

.sidebar-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--sidebar-border);
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: #fff;
}

.brand-link:hover {
  color: #fff;
  opacity: 0.9;
}

.brand-logo {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.25rem;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 1.25rem;
  font-weight: 700;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 0.75rem;
  color: var(--sidebar-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar-nav {
  flex-grow: 1;
  padding: 1.5rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  overflow-y: auto;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 0.875rem 1rem;
  color: var(--sidebar-text-muted);
  border-radius: var(--radius-lg);
  font-weight: 500;
  font-size: 0.9375rem;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  margin-bottom: 0.25rem;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--sidebar-text);
  transform: translateX(4px);
}

.nav-link-active {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.15) 0%, transparent 100%);
  border-left: 3px solid var(--primary);
  color: #fff;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
}

.nav-link-active:hover {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.25) 0%, transparent 100%);
}

.sidebar-footer {
  padding: 1.5rem;
  border-top: 1px solid var(--sidebar-border);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: var(--sidebar-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--sidebar-text);
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--sidebar-text);
}

.user-role {
  font-size: 0.75rem;
  color: var(--sidebar-text-muted);
}

.app-main {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-header {
  height: 72px;
  flex-shrink: 0;
  background-color: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  z-index: 10;
  position: sticky;
  top: 0;
}

.page-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-strong);
}

.page-content {
  flex-grow: 1;
  padding: 2rem;
  overflow-y: auto;
}

/* Page Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
