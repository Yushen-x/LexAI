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
          <component :is="item.icon" class="nav-icon" />
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
import {
  LayoutDashboard,
  FileEdit,
  Files,
  ListTodo,
  MessageSquare,
  BarChart2,
  FileSearch
} from 'lucide-vue-next';

const route = useRoute();

const navItems = [
  { to: '/dashboard', title: '工作台', icon: LayoutDashboard },
  { to: '/contract-draft', title: '合同起草', icon: FileEdit },
  { to: '/contract-list', title: '合同台账', icon: Files },
  { to: '/workflow-pending', title: '待办任务', icon: ListTodo },
  { to: '/consultation', title: '法律咨询', icon: MessageSquare },
  { to: '/case-analysis', title: '案件分析', icon: BarChart2 },
  { to: '/contract-review', title: '合同审查', icon: FileSearch }
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
  background: var(--sidebar-bg-from);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  color: var(--sidebar-text);
  z-index: 20;
}

.sidebar-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--sidebar-border);
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: var(--text-strong);
}

.brand-link:hover {
  color: var(--primary);
  opacity: 1;
}

.brand-logo {
  width: 40px;
  height: 40px;
  background: #ffffff;
  color: var(--primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1.25rem;
  box-shadow: var(--shadow-sm);
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

.nav-icon {
  width: 18px;
  height: 18px;
  margin-right: 0.75rem;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.nav-link:hover {
  background-color: var(--sidebar-bg-hover);
  color: var(--sidebar-text);
}

.nav-link:hover .nav-icon {
  opacity: 1;
}

.nav-link-active {
  background: var(--bg-app);
  color: var(--primary);
  font-weight: 600;
}

.nav-link-active .nav-icon {
  opacity: 1;
  color: var(--primary);
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
  background-color: var(--primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--primary);
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
