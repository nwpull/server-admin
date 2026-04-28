import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Monitor' },
      },
      {
        path: 'terminal',
        name: 'Terminal',
        component: () => import('@/views/Terminal.vue'),
        meta: { title: '终端', icon: 'Monitor' },
      },
      {
        path: 'files',
        name: 'FileManager',
        component: () => import('@/views/FileManager.vue'),
        meta: { title: '文件管理', icon: 'Folder' },
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/Monitor.vue'),
        meta: { title: '系统监控', icon: 'DataLine' },
      },
      {
        path: 'processes',
        name: 'Processes',
        component: () => import('@/views/Processes.vue'),
        meta: { title: '进程管理', icon: 'Cpu' },
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/Logs.vue'),
        meta: { title: '日志查看', icon: 'Document' },
      },
      {
        path: 'services',
        name: 'Services',
        component: () => import('@/views/Services.vue'),
        meta: { title: '服务管理', icon: 'Setting' },
      },
      {
        path: 'cron',
        name: 'CronJobs',
        component: () => import('@/views/CronJobs.vue'),
        meta: { title: '定时任务', icon: 'Timer' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '服务器管理', icon: 'Tools' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
